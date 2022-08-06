package bots

import EXPENSES_DB
import EXPENSES_TELEGRAM_BOT_TOKEN_KEY
import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.Validated
import calendar
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.handlers.TextHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.network.fold
import com.github.kotlintelegrambot.types.TelegramBotResult.Error.*
import currentMonth
import dotEnv
import io.ktor.util.date.*
import logger
import notionClient
import org.jraf.klibnotion.model.base.reference.DatabaseReference
import org.jraf.klibnotion.model.database.Database
import org.jraf.klibnotion.model.date.DateOrDateRange
import org.jraf.klibnotion.model.property.spec.SelectPropertySpec
import org.jraf.klibnotion.model.property.value.PropertyValueList
import regular_expenses.*
import removePunctuation
import requireVariable
import runBlockingIO
import org.jraf.klibnotion.model.date.Date as NotionDate

class ExpensesBot(private val expensesDb: Database) {
    private val expensesTypes: List<String> = expensesDb
        .propertySpecs
        .filterIsInstance<SelectPropertySpec>()
        .first {
            it.name == EXPENSES_TYPE_KEY
        }
        .options
        .map { it.name }

    private val botToken by lazy { dotEnv.requireVariable(EXPENSES_TELEGRAM_BOT_TOKEN_KEY) }
    private val expensesBuilders = mutableListOf<Expense.Builder>()

    private val bot: Bot = bot {
//        logLevel = LogLevel.All()
        token = botToken
        dispatch {
            addCallbackQueries()
            text {
                logger.info("Processing expense: $text")

                val (name: String, amount) = prepareData()
                    .fold(
                        fe = { invalidMessage ->
                            bot.sendMessage(
                                chatId = ChatId.fromId(message.chat.id),
                                text = invalidMessage
                            )
                            return@text
                        },
                        fa = { (name, quantity) ->
                            logger.info("Searching name: $name and quantity: $quantity")
                            name to quantity
                        }
                    )
                val currentExpenseBuilder = Expense
                    .Builder()
                    .addName(name)
                    .addAmount(amount.toDouble())
                createInlineKeyboard()
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Choose category for expense: $name",
                    replyMarkup = createInlineKeyboard()
                ).fold(
                    ifSuccess = {
                        currentExpenseBuilder.addMessageId(it.messageId)
                        expensesBuilders.add(currentExpenseBuilder)
                    },
                    ifError = {
                        when (it) {
                            is HttpError -> logger.error(it.description)
                            is TelegramApi -> logger.error(it.description)
                            is InvalidResponse -> logger.error(it.body?.errorDescription)
                            is Unknown -> logger.error(it.exception.message)
                        }
                    }
                )
            }
        }
    }

    private fun createInlineKeyboard(): InlineKeyboardMarkup {
        val buttons = expensesTypes
            .map { s ->
                InlineKeyboardButton.CallbackData(
                    text = s,
                    callbackData = s
                )
            }

        val (first, second, third, fourth) = buttons
        return InlineKeyboardMarkup.create(
            listOf(listOf(first, second)) + listOf(listOf(third, fourth)) + buttons
                .slice(4..buttons.lastIndex)
                .chunked(4)
        )
    }

    private fun Dispatcher.addCallbackQueries() {
        expensesTypes.forEach { type ->
            fun Bot.onError(chatId: Long) {
                sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "Не получилось создать запись о трате"
                )
            }

            callbackQuery(type) {
                val chatId: Long? = callbackQuery.message?.chat?.id
                val messageId: Long? = callbackQuery.message?.messageId
                if (chatId == null || messageId == null) {
                    logger.error("For some reason chatId or messageId is null")
                    return@callbackQuery
                }

                val expense: Expense = expensesBuilders
                    .first { it.getMessageId() == callbackQuery.message?.messageId }
                    .addType(type)
                    .build()
                runCatching {
                    createNotionPage(expense, type)
                }.fold(
                    onSuccess = {
                        bot.editMessageText(
                            chatId = ChatId.fromId(chatId),
                            messageId = messageId,
                            text = "Добавили запись в табличку по ${expense.name} с типом $type и суммой ${expense.amount}",
                            replyMarkup = null
                        ).fold(
                            response = {
                                println(it)
                            },
                            error = {
                                println(it)
                            }
                        )
                    },
                    onFailure = {
                        logger.error("Error fetching products DB ${it.message}")
                        bot.onError(chatId)
                    }
                )
            }
        }
    }

    fun startPolling() = runBlockingIO {
        bot.startPolling()
    }

    private fun createNotionPage(
        expense: Expense,
        type: String
    ) = runBlockingIO {
        notionClient.pages.createPage(
            parentDatabase = DatabaseReference(id = EXPENSES_DB),
            properties = PropertyValueList()
                .title(
                    idOrName = DESCRIPTION_KEY,
                    text = expense.name
                )
                .number(
                    idOrName = FINAL_AMOUNT_KEY,
                    number = expense.amount
                )
                .selectByName(
                    idOrName = EXPENSES_TYPE_KEY,
                    selectName = type
                )
                .selectByName(
                    idOrName = MONTH_KEY,
                    selectName = buildString {
                        val monthName = Month.from(currentMonth).name
                        append(monthName.first())
                        for (i in 1..monthName.lastIndex) {
                            append(monthName[i].lowercaseChar())
                        }
                    }
                )
                .date(
                    idOrName = PAYMENT_DATE_KEY,
                    date = DateOrDateRange(
                        start = NotionDate(
                            timestamp = calendar.time
                        )
                    )
                )
        )
    }

    private fun TextHandlerEnvironment.prepareData(): Validated<String, Pair<String, String>> = text
        .trim()
        .removePunctuation()
        .split(" ")
        .run {
            if (isEmpty() || size == 1) {
                Invalid("Неправильный формат записи. Указали имя и сумму через пробел?")
            } else {
                Valid(
                    take(lastIndex)
                        .reduce(operation = { acc, it -> "$acc $it " })
                        .trim() to last()
                )
            }
        }

    private class Expense private constructor(
        val name: String,
        val messageId: Long,
        val type: String,
        val amount: Double
    ) {

        class Builder {

            private var messageId: Long? = null
            private var name: String? = null
            private var amount: Double? = null
            private var type: String? = null

            fun getMessageId() = requireNotNull(messageId)

            fun addName(name: String) = apply {
                this.name = name
            }

            fun addMessageId(id: Long) = apply {
                this.messageId = id
            }

            fun addAmount(amount: Double) = apply {
                this.amount = amount
            }

            fun addType(type: String) = apply {
                this.type = type
            }

            fun build() = Expense(
                name = requireNotNull(name),
                messageId = requireNotNull(messageId),
                type = requireNotNull(type),
                amount = requireNotNull(amount),
            )
        }
    }
}