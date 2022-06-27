import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.handlers.TextHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import io.github.cdimascio.dotenv.dotenv
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jraf.klibnotion.client.Authentication
import org.jraf.klibnotion.client.ClientConfiguration
import org.jraf.klibnotion.client.NotionClient
import org.jraf.klibnotion.model.base.reference.DatabaseReference
import org.jraf.klibnotion.model.page.Page
import org.jraf.klibnotion.model.property.value.PropertyValueList
import org.jraf.klibnotion.model.property.value.TitlePropertyValue
import org.jraf.klibnotion.model.richtext.RichTextList
import org.slf4j.LoggerFactory

private val dotEnv = dotenv {
    ignoreIfMissing = true
}
val FEES_DATABASE_ID = dotEnv.requireVariable("FEES_DATABASE_ID")
val PRODUCTS_DATABASE_ID = dotEnv.requireVariable("PRODUCTS_DATABASE_ID")
val NEED_TO_BUY_DATABASE_ID = dotEnv.requireVariable("NEED_TO_BUY_DATABASE_ID")
private val TELEGRAM_BOT_TOKEN = dotEnv.requireVariable("TELEGRAM_BOT_TOKEN")
private val NOTION_TOKEN = dotEnv.requireVariable("NOTION_TOKEN")

val notionClient by lazy {
    NotionClient.newInstance(
        ClientConfiguration(
            Authentication(NOTION_TOKEN)
        )
    )
}
val logger by lazy { LoggerFactory.getLogger("Main") }
private val processProductUseCase by lazy { ProcessProductUseCase() }

suspend fun main(args: Array<String>): Unit = runBlocking {
    val port = System.getenv("PORT")?.toInt() ?: 23567
    val telegramBotToken = TELEGRAM_BOT_TOKEN
    bot {
        token = telegramBotToken
        dispatch {
            text {
                logger.info("Processing text: $text")
                runBlockingIO {
                    processProductUseCase.runCommand()
                }
            }
        }
    }.startPolling()
    embeddedServer(Netty, port = port) {
        configureRouting()
    }.start(wait = true)
}

fun Application.configureRouting() {
    routing {
        get("/") {
            difficultHomePage()
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.difficultHomePage() {
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

data class PageAndTitlePropertyValue(
    val page: Page, val titlePropertyValue: TitlePropertyValue
)

private suspend fun foo(): Int {
    var r = 0
    for (i in 1..100_000) {
        r += i * i
    }
    return r
}
