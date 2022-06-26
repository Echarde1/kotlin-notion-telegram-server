import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import io.github.cdimascio.dotenv.dotenv
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jraf.klibnotion.client.Authentication
import org.jraf.klibnotion.client.ClientConfiguration
import org.jraf.klibnotion.client.NotionClient
import org.jraf.klibnotion.model.base.reference.DatabaseReference
import org.jraf.klibnotion.model.page.Page
import org.jraf.klibnotion.model.property.value.PropertyValueList
import org.jraf.klibnotion.model.property.value.TitlePropertyValue
import org.jraf.klibnotion.model.richtext.RichTextList
import kotlin.random.Random

private val dotEnv = dotenv {
    ignoreIfMissing = true
}
private val TELEGRAM_BOT_TOKEN = dotEnv.getVariable("TELEGRAM_BOT_TOKEN")
private val NOTION_TOKEN = dotEnv.getVariable("NOTION_TOKEN")
private val FEES_DATABASE_ID = dotEnv.getVariable("FEES_DATABASE_ID")
private val PRODUCTS_DATABASE_ID = dotEnv.getVariable("PRODUCTS_DATABASE_ID")
private val NEED_TO_BUY_DATABASE_ID = dotEnv.getVariable("NEED_TO_BUY_DATABASE_ID")

val notionClient by lazy {
    NotionClient.newInstance(
        ClientConfiguration(
            Authentication(NOTION_TOKEN)
        )
    )
}

suspend fun main(args: Array<String>) {
    val port = System.getenv("PORT")?.toInt() ?: 23567
    /*embeddedServer(Netty, port = port) {
        configureRouting()
    }.start(wait = true)*/
    val telegramBotToken = TELEGRAM_BOT_TOKEN
    bot {
        token = telegramBotToken
        dispatch {
            text {
                runBlocking(Dispatchers.IO) {
                    val groceryDb = notionClient.databases.queryDatabase(PRODUCTS_DATABASE_ID)
                    val needToBuyDb = notionClient.databases.queryDatabase(NEED_TO_BUY_DATABASE_ID)
                    val (name: String, quantity) = text.split(" ")
                        .run {
                            take(lastIndex).reduce(operation = { acc, it -> "$acc $it " }).trim() to last()
                        }
                    val resultText: String = groceryDb
                        .results
                        .map {
                            PageAndTitlePropertyValue(
                                it, it.propertyValues.filterIsInstance<TitlePropertyValue>().first()
                            )
                        }
                        .filter {
                            it.titlePropertyValue.value.plainText.equals(
                                name,
                                ignoreCase = true
                            )
                        }
                        .let { resultFromGrocery ->
                            if (resultFromGrocery.isEmpty()) {
                                "Нет такого продукта в базе. Добавить в общую базу продуктов?"
                            } else {
                                notionClient
                                    .pages
                                    .createPage(
                                        parentDatabase = DatabaseReference(id = NEED_TO_BUY_DATABASE_ID),
                                        properties = PropertyValueList()
                                            .title(
                                                "Name",
                                                richTextList = RichTextList()
                                                    .pageMention(pageId = resultFromGrocery.first().page.id,)
                                            )
                                            .number("Quantity", quantity.toInt())
                                            .relation(
                                                idOrName = "Products",
                                                resultFromGrocery.first().page.id
                                            )
                                    )
                                "Добавили (наверное): $name $quantity"
                            }
                        }

                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = resultText
                    )
                }
            }
        }
    }.startPolling()
}

fun Application.configureRouting() {

    routing {
        get("/") {
            val notionClient = NotionClient.newInstance(
                ClientConfiguration(
                    Authentication(NOTION_TOKEN)
                )
            )
//            val groceryDb = notionClient.databases.getDatabase(PRODUCTS_DATABASE_ID)
            val groceryDb = notionClient.databases.queryDatabase(PRODUCTS_DATABASE_ID)
            val needToBuyDb = notionClient.databases.queryDatabase(NEED_TO_BUY_DATABASE_ID)
//            val database = notionClient.databases.getDatabaseList()

            groceryDb.results.map {
                PageAndTitlePropertyValue(
                    it, it.propertyValues.filterIsInstance<TitlePropertyValue>().first()
                )
            }

            needToBuyDb.results.forEach {
                println(it)
            }

            call.respondText("done")
        }
    }
}

data class PageAndTitlePropertyValue(
    val page: Page, val titlePropertyValue: TitlePropertyValue
)