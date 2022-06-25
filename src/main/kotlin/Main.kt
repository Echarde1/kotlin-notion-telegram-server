import io.github.cdimascio.dotenv.dotenv
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

private val dotEnv = dotenv {
    ignoreIfMissing = true
}
private val TELEGRAM_BOT_TOKEN =
    dotEnv.get("TELEGRAM_BOT_TOKEN") ?: System.getenv("TELEGRAM_BOT_TOKEN")
private val NOTION_TOKEN = dotEnv.get("NOTION_TOKEN") ?: System.getenv("NOTION_TOKEN")
private val FEES_DATABASE_ID = dotEnv.get("FEES_DATABASE_ID") ?: System.getenv("FEES_DATABASE_ID")

suspend fun main(args: Array<String>) {
    val port = System.getenv("PORT")?.toInt() ?: 23567
    embeddedServer(Netty, port = port) {
        configureRouting()
    }.start(wait = true)
//    val telegramBotToken = TELEGRAM_BOT_TOKEN
//    bot {
//        token = telegramBotToken
//        dispatch {
//            text {
//                bot.sendMessage(ChatId.fromId(message.chat.id), text = text)
//            }
//        }
//    }.startPolling()
}

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("$TELEGRAM_BOT_TOKEN $NOTION_TOKEN $FEES_DATABASE_ID")
        }
        /*get("/") {
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
        }*/
    }
}