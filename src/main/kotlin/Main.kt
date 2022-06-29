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
import org.jraf.klibnotion.model.property.value.CheckboxPropertyValue
import org.jraf.klibnotion.model.property.value.TitlePropertyValue
import org.jraf.klibnotion.model.richtext.RichTextList
import org.slf4j.LoggerFactory
import java.util.function.BiPredicate

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
    runCatching { notionClient.databases.queryDatabase(NEED_TO_BUY_DATABASE_ID) }
        .fold(
            onSuccess = { pages ->
                pages
                    .results
                    .filter { page ->
                        page.propertyValues.contains {
                            it is CheckboxPropertyValue
                                    && it.name == "Bought"
                                    && it.value
                        }
                    }
            },
            onFailure = {
                logger.error("Error querying NeedToBuy DB ${it.message}")
                return@clearNeedToBuy
            }
        )
        .also { pages ->
            val items = pages.map { it.requireTitlePropertyValue() }
            logger.info("Fetched and filtered NeedToBuy. Items: $items")
        }
        .forEach {
            runCatching { notionClient.pages.setPageArchived(id = it.id, archived = true) }
                .fold(
                    onSuccess = {
                        logger.info("Archived page id: ${it.id} and name: ${it.requireTitlePropertyValue()}")
                    },
                    onFailure = {
                        logger.error("Error archiving page: ${it.message}")
                        return@clearNeedToBuy
                    }
                )
        }

    call.respondText("Cleared NeedToBuy DB")
}

data class PageAndTitlePropertyValue(
    val page: Page, val titlePropertyValue: TitlePropertyValue
)

private fun <T> List<T>.contains(predicate: (T) -> Boolean): Boolean {
    return find(predicate) != null
}

private fun Page.requireTitlePropertyValue(): String? {
    return propertyValues
        .first { it is TitlePropertyValue }
        .run { value as RichTextList }
        .plainText
}

private suspend fun foo(): Int {
    var r = 0
    for (i in 1..100_000) {
        r += i * i
    }
    return r
}
