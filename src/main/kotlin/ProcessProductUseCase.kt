import com.github.kotlintelegrambot.dispatcher.handlers.TextHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import org.jraf.klibnotion.model.base.reference.DatabaseReference
import org.jraf.klibnotion.model.property.value.PropertyValueList
import org.jraf.klibnotion.model.property.value.TitlePropertyValue
import org.jraf.klibnotion.model.richtext.RichTextList

class ProcessProductUseCase {
    context(TextHandlerEnvironment)
    suspend fun runCommand() {
        val (name: String, quantity) = text
            .removePunctuation()
            .split(" ")
            .run {
                if (isEmpty() || size == 1) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "Неправильный формат записи. Указали имя и количество через пробел?"
                    )
                    return
                } else {
                    take(lastIndex).reduce(operation = { acc, it -> "$acc $it " })
                        .trim() to last()
                }
            }
        logger.info("Searching name: $name and quantity: $quantity")
        logger.info("start fetching data from grocery DB")
        val groceryDb = notionClient.databases.queryDatabase(PRODUCTS_DATABASE_ID)
//        val needToBuyDb = notionClient.databases.queryDatabase(NEED_TO_BUY_DATABASE_ID)
        logger.info("fetched data from grocery DB")
        val resultText: String = groceryDb
            .results
            .map {
                PageAndTitlePropertyValue(
                    it, it.propertyValues.filterIsInstance<TitlePropertyValue>().first()
                )
            }
            .filter {
                it.titlePropertyValue.value.plainText?.removePunctuation().equals(
                    name,
                    ignoreCase = true
                )
            }
            .let { resultFromGrocery ->
                if (resultFromGrocery.isEmpty()) {
                    "Нет такого продукта в базе. Добавить в общую базу продуктов?"
                } else {
                    runCatching {
                        notionClient
                            .pages
                            .createPage(
                                parentDatabase = DatabaseReference(id = NEED_TO_BUY_DATABASE_ID),
                                properties = PropertyValueList()
                                    .title(
                                        "Name",
                                        richTextList = RichTextList()
                                            .pageMention(pageId = resultFromGrocery.first().page.id)
                                    )
                                    .number("Quantity", quantity.toInt())
                                    .relation(
                                        idOrName = "Products",
                                        resultFromGrocery.first().page.id
                                    )
                            )
                    }
                        .fold(
                            onSuccess = {
                                "Добавили (наверное): $text"
                            },
                            onFailure = {
                                logger.error(it.message)
                                "Чета ошибка при запросе в Ноушен"
                            }
                        )
                }
            }

        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = resultText
        )
    }

    private fun String.removePunctuation() = replace(Regex("[.,!?\\\\-]"), "")

}