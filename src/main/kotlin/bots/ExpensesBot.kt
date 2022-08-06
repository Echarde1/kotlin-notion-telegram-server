package bots

import EXPENSES_DATABASE_KEY
import EXPENSES_TELEGRAM_BOT_TOKEN_KEY
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.network.fold
import dotEnv
import logger
import notionClient
import org.jraf.klibnotion.model.property.spec.SelectPropertySpec
import regular_expenses.EXPENSES_TYPE_KEY
import requireVariable
import runBlockingIO

class ExpensesBot {
    private val botToken by lazy { dotEnv.requireVariable(EXPENSES_TELEGRAM_BOT_TOKEN_KEY) }
    private val EXPENSES_DB by lazy { dotEnv.requireVariable(EXPENSES_DATABASE_KEY) }
    private var currentMessageIdWithMarkup = 0L

    private val bot: Bot = bot {
//        logLevel = LogLevel.All()
        token = botToken
        dispatch {
            callbackQuery("5") {
                val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "Введите свой `email` от Шереметьево"
                )
            }
            callbackQuery("4") {
                val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "Введите свой `email` от Шереметьево"
                )
            }
            callbackQuery("1") {
                val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                bot.editMessageReplyMarkup(
                    chatId = ChatId.fromId(chatId),
                    messageId = currentMessageIdWithMarkup,
                    replyMarkup = InlineKeyboardMarkup.create(
                        listOf(
                            InlineKeyboardButton.CallbackData(text = "a", "1"),
                            InlineKeyboardButton.CallbackData(text = "b", "2"),
                            InlineKeyboardButton.CallbackData(text = "c", "3"),
                            InlineKeyboardButton.CallbackData(text = "d", "4"),
                            InlineKeyboardButton.CallbackData(text = "e", "5"),
                            InlineKeyboardButton.CallbackData(text = "f", "6"),
                            InlineKeyboardButton.CallbackData(text = "g", "7"),
                            InlineKeyboardButton.CallbackData(text = "h", "8"),
                            InlineKeyboardButton.CallbackData(text = "i", "9"),
                            InlineKeyboardButton.CallbackData(text = "j", "10"),
                            InlineKeyboardButton.CallbackData(text = "k", "11"),
                            InlineKeyboardButton.CallbackData(text = "l", "12"),
                            InlineKeyboardButton.CallbackData(text = "m", "13"),
                            InlineKeyboardButton.CallbackData(text = "n", "14"),
                        )
                    )
                ).fold(
                    response = {
                        println(it)
                    },
                    error = {
                        logger.error(it.exception?.message)
                    }
                )
            }
            callbackQuery("2") {
                val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "Труляля",
                    replyMarkup = InlineKeyboardMarkup.create(
                        listOf(
                            InlineKeyboardButton.CallbackData(text = "a", "1"),
                            InlineKeyboardButton.CallbackData(text = "b", "2"),
                            InlineKeyboardButton.CallbackData(text = "c", "3"),
                            InlineKeyboardButton.CallbackData(text = "d", "4"),
                            InlineKeyboardButton.CallbackData(text = "e", "5"),
                            InlineKeyboardButton.CallbackData(text = "f", "6"),
                            InlineKeyboardButton.CallbackData(text = "g", "7"),
                            InlineKeyboardButton.CallbackData(text = "h", "8"),
                            InlineKeyboardButton.CallbackData(text = "i", "9"),
                            InlineKeyboardButton.CallbackData(text = "j", "10"),
                            InlineKeyboardButton.CallbackData(text = "k", "11"),
                            InlineKeyboardButton.CallbackData(text = "l", "12"),
                            InlineKeyboardButton.CallbackData(text = "m", "13"),
                            InlineKeyboardButton.CallbackData(text = "n", "14"),
                        )
                    )
                )
            }
            text {
                logger.info("Processing text: $text")

                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = text,
                    replyMarkup = InlineKeyboardMarkup.create(
                        listOf(
                            InlineKeyboardButton.CallbackData(text = "1", "1"),
                            InlineKeyboardButton.CallbackData(text = "2", "2"),
                            InlineKeyboardButton.CallbackData(text = "3", "3"),
                            InlineKeyboardButton.CallbackData(text = "4", "4"),
                            InlineKeyboardButton.CallbackData(text = "5", "5"),
                            InlineKeyboardButton.CallbackData(text = "6", "6"),
                            InlineKeyboardButton.CallbackData(text = "7", "7"),
                            InlineKeyboardButton.CallbackData(text = "8", "8"),
                            InlineKeyboardButton.CallbackData(text = "9", "9"),
                            InlineKeyboardButton.CallbackData(text = "10", "10"),
                            InlineKeyboardButton.CallbackData(text = "10", "11"),
                            InlineKeyboardButton.CallbackData(text = "10", "12"),
                            InlineKeyboardButton.CallbackData(text = "10", "13"),
                            InlineKeyboardButton.CallbackData(text = "10", "14"),
                        )
                    )
                ).fold(
                    ifSuccess = { currentMessageIdWithMarkup = it.messageId },
                    ifError = { }
                )
            }
        }
    }

    fun startPolling() = runBlockingIO {
//        bot.startPolling()
        val foo = notionClient
            .databases
            .getDatabase(EXPENSES_DB)
            .propertySpecs
            .filterIsInstance<SelectPropertySpec>()
            .first {
                it.name == EXPENSES_TYPE_KEY
            }
            .options
        println(foo)
    }
}