import arrow.core.Invalid
import arrow.core.Option
import arrow.core.Valid
import arrow.core.Validated
import com.github.kotlintelegrambot.dispatcher.handlers.TextHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import org.jraf.klibnotion.model.base.reference.DatabaseReference
import org.jraf.klibnotion.model.page.Page
import org.jraf.klibnotion.model.pagination.ResultPage
import org.jraf.klibnotion.model.property.value.CheckboxPropertyValue
import org.jraf.klibnotion.model.property.value.PropertyValueList
import org.jraf.klibnotion.model.property.value.TitlePropertyValue
import org.jraf.klibnotion.model.richtext.PageMentionRichText
import org.jraf.klibnotion.model.richtext.RichTextList

class ProcessProductUseCase {
    context(TextHandlerEnvironment) suspend fun runCommand() {
        val (name: String, quantity) = prepareData()
            .fold(
                fe = { invalidMessage ->
                    produceMessage(invalidMessage)
                    return@runCommand
                },
                fa = { (name, quantity) ->
                    logger.info("Searching name: $name and quantity: $quantity")
                    name to quantity
                }
            )
        logger.info("start fetching data from products DB")
        val productsDb = runCatching { notionClient.databases.queryDatabase(PRODUCTS_DATABASE) }
            .fold(
                onSuccess = {
                    logger.info("fetched data from products DB")
                    it
                },
                onFailure = {
                    logger.error("Error fetching products DB ${it.message}")
                    produceMessage("Не получилось подтянуть базу Products")
                    return@runCommand
                }
            )
        logger.info("start fetching data from needToBuy DB")
        val needToBuyDb =
            runCatching { notionClient.databases.queryDatabase(NEED_TO_BUY_DATABASE) }
                .fold(
                    onSuccess = {
                        logger.info("fetched data from needToBuy DB")
                        it
                    },
                    onFailure = {
                        logger.error("Error fetching needToBuy DB. ${it.message}")
                        produceMessage("Не получилось подтянуть базу Products")
                        return@runCommand
                    }
                )

        val resultText = productsDb
            .results
            .mapToPageAndTitle()
            .filterEqualsOrChecked(name)
            .processResultFromGrocery(quantity, needToBuyDb)

        produceMessage(resultText)
    }

    private fun TextHandlerEnvironment.prepareData(): Validated<String, Pair<String, String>> = text
        .trim()
        .removePunctuation()
        .split(" ")
        .run {
            if (isEmpty() || size == 1) {
                Invalid("Неправильный формат записи. Указали имя и количество через пробел?")
            } else {
                Valid(
                    take(lastIndex)
                        .reduce(operation = { acc, it -> "$acc $it " })
                        .trim() to last()
                )
            }
        }

    private fun List<Page>.mapToPageAndTitle() = map {
        PageAndTitlePropertyValue(
            it, it.propertyValues.filterIsInstance<TitlePropertyValue>().first()
        )
    }

    private fun List<PageAndTitlePropertyValue>.filterEqualsOrChecked(name: String) = filter {
        it.titlePropertyValue.value.plainText?.trim()?.removePunctuation().equals(
            name,
            ignoreCase = true
        )
    }

    context(TextHandlerEnvironment) private suspend fun List<PageAndTitlePropertyValue>.processResultFromGrocery(
        quantity: String,
        needToBuyDb: ResultPage<Page>
    ) = let { resultFromProducts ->
        if (resultFromProducts.isEmpty()) {
            "Нет такого продукта в базе. Добавить в общую базу продуктов?"
        } else {
            runCatching {
                createNeedToBuyObject(needToBuyDb, resultFromProducts, quantity)
            }.fold(
                onSuccess = { "Добавили (наверное): $text" },
                onFailure = {
                    logger.error(it.message)
                    "Чета ошибка при запросе в Ноушен"
                }
            )
        }
    }

    private suspend fun createNeedToBuyObject(
        needToBuyDb: ResultPage<Page>,
        resultFromProducts: List<PageAndTitlePropertyValue>,
        quantity: String
    ) {
        val pages = notionClient.pages
        val quantityKey = "Quantity"
        val productPageId = resultFromProducts.first().page.id
        val (resultQuantity: Double, command) = Option.fromNullable(needToBuyDb
            .results
            .mapToPageAndTitle()
            .find { (page, title) ->
                val isItemAlreadyBought = page
                    .propertyValues
                    .first {
                        it.name == "Bought"
                    }
                    .let { it as CheckboxPropertyValue }
                    .value
                val shouldBuyItem = isItemAlreadyBought.not()
                val isPageIdEqualsToProductPageId = (title.value.richTextList.first() as PageMentionRichText).pageId == productPageId

                shouldBuyItem && isPageIdEqualsToProductPageId
            }
        ).fold(
            ifEmpty = {
                quantity.toDouble() to { properties: PropertyValueList ->
                    runBlockingIO {
                        pages.createPage(
                            parentDatabase = DatabaseReference(id = NEED_TO_BUY_DATABASE),
                            properties = properties
                        )
                    }
                }
            },
            ifSome = { (page, _) ->
                val value = page
                    .propertyValues
                    .first { it.name == quantityKey }
                    .value!!.toString().toDouble() + quantity.toDouble()
                val command = { properties: PropertyValueList ->
                    runBlockingIO {
                        pages
                            .updatePage(
                                id = page.id,
                                properties = properties
                            )
                    }
                }
                value to command
            }
        )

        command.invoke(
            PropertyValueList()
                .title(
                    "Name",
                    richTextList = RichTextList()
                        .pageMention(pageId = productPageId)
                )
                .number(quantityKey, resultQuantity)
                .relation(
                    idOrName = "Products",
                    productPageId
                )
        )
    }

    private fun String.removePunctuation() = replace(Regex("[,!?\\\\-]"), "")

    private fun TextHandlerEnvironment.produceMessage(textToSend: String) {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = textToSend
        )
    }
}