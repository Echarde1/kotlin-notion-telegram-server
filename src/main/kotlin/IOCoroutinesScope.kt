import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

class IOCoroutinesScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            logger.error("Ошибка в корутине: ${throwable.message}")
        }
}

fun <T> runBlockingIO(block: suspend CoroutineScope.() -> T) = runBlocking(
    context = IOCoroutinesScope().coroutineContext,
    block = block
)