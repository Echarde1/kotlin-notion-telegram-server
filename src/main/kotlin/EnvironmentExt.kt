import io.github.cdimascio.dotenv.Dotenv

fun Dotenv.getVariable(key: String): String = get(key) ?: System.getenv(key)