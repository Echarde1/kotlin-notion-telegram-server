import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.text
import io.github.cdimascio.dotenv.dotenv
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.runBlocking
import org.jraf.klibnotion.client.*
import org.jraf.klibnotion.model.base.UuidString
import org.jraf.klibnotion.model.block.Block
import org.jraf.klibnotion.model.block.MutableBlockList
import org.jraf.klibnotion.model.block.ToDoBlock
import org.jraf.klibnotion.model.page.Page
import org.jraf.klibnotion.model.property.value.TitlePropertyValue
import org.jraf.klibnotion.model.richtext.RichTextList
import org.slf4j.LoggerFactory

val dotEnv = dotenv {
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
            authentication = Authentication(NOTION_TOKEN),
            httpConfiguration = HttpConfiguration(loggingLevel = HttpLoggingLevel.ALL)
        )
    )
}
val logger by lazy { LoggerFactory.getLogger("Main") }
private val processProductUseCase by lazy { ProcessProductUseCase() }
private val clearTrainingListsInteractor by lazy { ClearTrainingListsInteractor() }

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
            helloWorld()
        }
        get("/clear_gi") {
            clearTrainingListsInteractor.clearGi()
        }
        get("/clear_no_gi") {
            clearTrainingListsInteractor.clearNoGi()
        }
        get("/clear_need_to_buy") {
            clearNeedToBuy()
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.helloWorld() {
    call.respondText("Hello, World!")
}

private suspend fun PipelineContext<Unit, ApplicationCall>.clearNeedToBuy() {

    call.respondText("Cleared NeedToBuy DB")
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
