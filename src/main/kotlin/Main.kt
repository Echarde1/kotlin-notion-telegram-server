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
import org.jraf.klibnotion.client.Authentication
import org.jraf.klibnotion.client.ClientConfiguration
import org.jraf.klibnotion.client.NotionClient
import org.jraf.klibnotion.model.base.reference.DatabaseReference
import org.jraf.klibnotion.model.property.value.PropertyValueList
import kotlin.random.Random

private val dotEnv = dotenv {
    ignoreIfMissing = true
}
private val TELEGRAM_BOT_TOKEN = dotEnv.getVariable("TELEGRAM_BOT_TOKEN")
private val NOTION_TOKEN = dotEnv.getVariable("NOTION_TOKEN")
private val FEES_DATABASE_ID = dotEnv.getVariable("FEES_DATABASE_ID")

suspend fun main(args: Array<String>) {
    val port = System.getenv("PORT")?.toInt() ?: 23567
    embeddedServer(Netty, port = port) {
        configureRouting()
    }.start(wait = true)
    val telegramBotToken = TELEGRAM_BOT_TOKEN
    bot {
        token = telegramBotToken
        dispatch {
            text {
                bot.sendMessage(ChatId.fromId(message.chat.id), text = text)
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
            val feeAmount = Random.nextInt(50_000)
            val database = notionClient.databases.getDatabase(FEES_DATABASE_ID)
            notionClient.pages.createPage(
                parentDatabase = DatabaseReference(id = FEES_DATABASE_ID),
                properties = PropertyValueList()
                    .title("Взнос", "Новенький взнос")
                    .number("Пенсионный", feeAmount)
                    .number("Медицинский", feeAmount)
                    .number("Патент на полгода", feeAmount)
                    .number("Патент на год", feeAmount)
            )

            val updated = notionClient.databases.getDatabase(FEES_DATABASE_ID)
            call.respondText("Initial Value: $database \n\n\n Updated value: $updated")
        }
    }
}