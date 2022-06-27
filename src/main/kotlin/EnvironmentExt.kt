import io.github.cdimascio.dotenv.Dotenv

fun Dotenv.requireVariable(key: String): String = get(key) ?: requireNotNull(System.getenv(key)) {
    "Variable with key: $key not found"
}