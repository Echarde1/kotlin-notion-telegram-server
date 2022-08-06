import bjj.ClearTrainingListsInteractor
import bots.ExpensesBot
import bots.GroceriesBot
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.github.cdimascio.dotenv.dotenv
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.logging.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.runBlocking
import org.jraf.klibnotion.client.*
import org.jraf.klibnotion.model.page.Page
import org.jraf.klibnotion.model.property.value.CheckboxPropertyValue
import org.jraf.klibnotion.model.property.value.TitlePropertyValue
import org.jraf.klibnotion.model.richtext.RichTextList
import org.slf4j.LoggerFactory
import regular_expenses.UpdateCurrenciesRateUseCase
import regular_expenses.UpdateRegularExpensesUseCase

val dotEnv = dotenv {
    ignoreIfMissing = true
}
val NEED_TO_BUY_DATABASE by lazy { dotEnv.requireVariable(NEED_TO_BUY_DATABASE_KEY) }

private val NOTION_TOKEN by lazy { dotEnv.requireVariable(NOTION_TOKEN_KEY) }

val httpClient
    get() = HttpClient(CIO) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
val notionClient by lazy {
    NotionClient.newInstance(
        ClientConfiguration(
            authentication = Authentication(NOTION_TOKEN),
            httpConfiguration = HttpConfiguration(loggingLevel = HttpLoggingLevel.ALL)
        )
    )
}
val xmlMapper by lazy {
    XmlMapper().apply {
        registerModule(ParameterNamesModule())
        registerModule(Jdk8Module())
        registerModule(JavaTimeModule())
        registerKotlinModule()
    }
}
val logger by lazy { LoggerFactory.getLogger("Main") }
private val updateCurrenciesRateUseCase by lazy { UpdateCurrenciesRateUseCase() }
private val updateRegularExpensesUseCase by lazy { UpdateRegularExpensesUseCase() }
private val clearTrainingListsInteractor by lazy { ClearTrainingListsInteractor() }

suspend fun main(): Unit = runBlocking {
    val port = System.getenv("PORT")?.toInt() ?: 23567
    GroceriesBot().startPolling()
    ExpensesBot().startPolling()
    embeddedServer(Netty, port = port) {
        configureNotionRouting()
    }.start(wait = true)
}

fun Application.configureNotionRouting() {
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
        get("/update_currency_rate") {
            updateCurrenciesRateUseCase.runCommand()
        }
        get("/add_regular_expenses") {
            updateRegularExpensesUseCase.runCommand()
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.helloWorld() {
    call.respondText("Hello, World!")
}

private suspend fun PipelineContext<Unit, ApplicationCall>.clearNeedToBuy() {
    runCatching { notionClient.databases.queryDatabase(NEED_TO_BUY_DATABASE) }
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
