import io.ktor.application.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import org.jraf.klibnotion.model.base.reference.DatabaseReference
import org.jraf.klibnotion.model.page.Page
import org.jraf.klibnotion.model.property.sort.PropertySort
import org.jraf.klibnotion.model.property.value.DatePropertyValue
import org.jraf.klibnotion.model.property.value.PropertyValueList
import org.jraf.klibnotion.model.property.value.SelectPropertyValue
import org.jraf.klibnotion.model.property.value.TitlePropertyValue
import java.util.*

class UpdateRegularExpensesUseCase {

    companion object {
        private const val paymentDeadlineColumnName = "Payment Deadline"
        private const val DESCRIPTION_KEY = "Description"
        private const val FINANCIAL_YEAR_KEY = "Financial Year"
        private const val EXPENSES_TYPE_KEY = "Expenses Type"
        private const val MONTH_KEY = "Month"
        private const val FINAL_AMOUNT_KEY = "Final Amount ₽"
    }

    private val REGULAR_EXPENSES_DATABASE by lazy { dotEnv.requireVariable("REGULAR_EXPENSES_DATABASE") }
    private val EXPENSES_DATABASE by lazy { dotEnv.requireVariable("EXPENSES_DATABASE") }
    private val calendar: Calendar = Calendar.getInstance()
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH)

    context(PipelineContext<Unit, ApplicationCall>) suspend fun runCommand() {
        val regularExpenses = notionClient.databases.queryDatabase(
            REGULAR_EXPENSES_DATABASE,
            sort = PropertySort().ascending(paymentDeadlineColumnName)
        )
            .results
            .filter { page -> paymentMonthEqualsToCurrentMonth(page) }

        regularExpenses.forEach { page ->
            notionClient.pages.createPage(
                parentDatabase = DatabaseReference(id = EXPENSES_DATABASE),
                properties = PropertyValueList()
                    .title(
                        idOrName = DESCRIPTION_KEY,
                        text = page.requirePropertyValue(DESCRIPTION_KEY)
                    )
                    .number(
                        idOrName = FINAL_AMOUNT_KEY,
                        number = page.requirePropertyValue(FINAL_AMOUNT_KEY)
                    )
                    .selectByName(
                        idOrName = EXPENSES_TYPE_KEY,
                        selectName = page.requirePropertyValue(EXPENSES_TYPE_KEY)
                    )
                    .selectByName(
                        idOrName = MONTH_KEY,
                        selectName = page.requirePropertyValue(MONTH_KEY)
                    )
                    .selectByName(
                        idOrName = FINANCIAL_YEAR_KEY,
                        selectName = page.requirePropertyValue(FINANCIAL_YEAR_KEY)
                    )
            )
        }

        call.respondText("Suck my kiss")
    }

    private fun paymentMonthEqualsToCurrentMonth(page: Page): Boolean {
        val calendar: Calendar = page
            .propertyValues
            .first { it.name == paymentDeadlineColumnName }
            .let { paymentDeadlineProperty ->
                requireNotNull((paymentDeadlineProperty as DatePropertyValue).value)
            }
            .start
            .timestamp
            .toCalendar()

        return calendar.get(Calendar.MONTH) == currentMonth
    }

    private inline fun <reified T : Any> Page.requirePropertyValue(key: String): T {
        val property = propertyValues
            .first { propertyValue -> propertyValue.name == key }
        return when (property) {
            is TitlePropertyValue -> property.value.plainText as T
            is SelectPropertyValue -> property.value!!.name as T
            else -> property.value as T
        }
    }

    private fun Date.toCalendar(): Calendar = run {
        calendar.time = this
        calendar
    }
}