package bots

import GROCERIES_TELEGRAM_BOT_TOKEN_KEY
import ProcessProductUseCase
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.text
import dotEnv
import logger
import requireVariable
import runBlockingIO

class GroceriesBot {
    private val botToken by lazy { dotEnv.requireVariable(GROCERIES_TELEGRAM_BOT_TOKEN_KEY) }
    private val processProductUseCase by lazy { ProcessProductUseCase() }

    private val bot: Bot = bot {
//        logLevel = LogLevel.All()
        token = botToken
        dispatch {
            text {
                logger.info("Processing text: $text")
                runBlockingIO {
                    processProductUseCase.runCommand()
                }
            }
        }
    }

    fun startPolling() {
        bot.startPolling()
    }
}