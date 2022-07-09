import io.ktor.application.*
import io.ktor.response.*
import io.ktor.util.date.*
import io.ktor.util.pipeline.*
import org.jraf.klibnotion.model.base.reference.DatabaseReference
import org.jraf.klibnotion.model.date.DateOrDateRange
import org.jraf.klibnotion.model.page.Page
import org.jraf.klibnotion.model.property.sort.PropertySort
import org.jraf.klibnotion.model.property.value.DatePropertyValue
import org.jraf.klibnotion.model.property.value.PropertyValueList
import org.jraf.klibnotion.model.property.value.SelectPropertyValue
import org.jraf.klibnotion.model.property.value.TitlePropertyValue
import java.util.*
import org.jraf.klibnotion.model.date.Date as NotionDate

class UpdateRegularExpensesUseCase {

    companion object {
        private const val PAYMENT_DEADLINE_KEY = "Payment Deadline"
        private const val DESCRIPTION_KEY = "Description"
        private const val FINANCIAL_YEAR_KEY = "Financial Year"
        private const val EXPENSES_TYPE_KEY = "Expenses Type"
        private const val MONTH_KEY = "Month"
        private const val FINAL_AMOUNT_KEY = "Final Amount ₽"
        private const val AMOUNT_RUB_KEY = "Amount ₽"
        private const val AMOUNT_USD_KEY = "Amount $"
        private const val AMOUNT_EUR_KEY = "Amount €"
        private const val USD_RATE_RELATION = "USD Rate Relation"
        private const val EUR_RATE_RELATION = "EUR Rate Relation"
        private const val RELATED_TO_BALANCE = "Related to Balance"
        private const val MONEY_SOURCE = "Money Source"
        private val currentMonth = Calendar.getInstance().get(Calendar.MONTH)

        private val calendar: Calendar = Calendar.getInstance()
        private fun Date.toCalendar(): Calendar = run {
            calendar.time = this
            calendar
        }

        private inline fun <reified T> Page.requirePropertyValue(key: String): T {
            val property = propertyValues
                .first { propertyValue -> propertyValue.name == key }
            return when (property) {
                is TitlePropertyValue -> property.value.plainText as T
                is SelectPropertyValue -> property.value!!.name as T
                else -> property.value as T
            }
        }
    }

    private val REGULAR_EXPENSES_DATABASE by lazy { dotEnv.requireVariable("REGULAR_EXPENSES_DATABASE") }
    private val EXPENSES_DATABASE by lazy { dotEnv.requireVariable("EXPENSES_DATABASE") }

    context(PipelineContext<Unit, ApplicationCall>) suspend fun runCommand() {
        addNewRegularExpenses()
//        updateExpensesWithRegular()

        call.respondText("Suck my kiss")
    }

    private suspend fun addNewRegularExpenses() {
        val regularExpensesKeys = listOf(
            PAYMENT_DEADLINE_KEY,
            DESCRIPTION_KEY,
            FINANCIAL_YEAR_KEY,
            EXPENSES_TYPE_KEY,
            MONTH_KEY,
            FINAL_AMOUNT_KEY,
            AMOUNT_RUB_KEY,
            AMOUNT_USD_KEY,
            AMOUNT_EUR_KEY,
            USD_RATE_RELATION,
            EUR_RATE_RELATION,
            MONEY_SOURCE,
            RELATED_TO_BALANCE
        )
        val regularExpenses = notionClient.databases.queryDatabase(
            REGULAR_EXPENSES_DATABASE,
            sort = PropertySort().ascending(PAYMENT_DEADLINE_KEY)
        )
            .results
            .filter { page -> paymentMonthEqualsToGivenMonth(page, currentMonth - 1) }

        regularExpenses.forEach { page ->
            notionClient.pages.createPage(
                parentDatabase = DatabaseReference(id = REGULAR_EXPENSES_DATABASE),
                properties = PropertyValueList().also { properties ->
                    regularExpensesKeys.forEach { key ->
                        AddRegularExpensesProperty(page = page, key = key, properties = properties)
                            .invoke()
                    }
                }
            )
        }
    }

    private suspend fun updateExpensesWithRegular() {
        val regularExpenses = notionClient.databases.queryDatabase(
            REGULAR_EXPENSES_DATABASE,
            sort = PropertySort().ascending(PAYMENT_DEADLINE_KEY)
        )
            .results
            .filter { page -> paymentMonthEqualsToGivenMonth(page, currentMonth) }

        val page = regularExpenses.first()
        val properties = PropertyValueList()
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
        /*regularExpenses.forEach { page ->
            notionClient.pages.createPage(
                parentDatabase = DatabaseReference(id = EXPENSES_DATABASE),
                properties = properties
            )
        }*/
    }

    private fun paymentMonthEqualsToGivenMonth(page: Page, givenMonth: Int): Boolean {
        val calendar: Calendar = page
            .propertyValues
            .first { it.name == PAYMENT_DEADLINE_KEY }
            .let { paymentDeadlineProperty ->
                requireNotNull((paymentDeadlineProperty as DatePropertyValue).value)
            }
            .start
            .timestamp
            .toCalendar()

        return calendar.get(Calendar.MONTH) == givenMonth
    }

    private class AddRegularExpensesProperty(
        private val page: Page,
        private val key: String,
        private val properties: PropertyValueList,
    ) : () -> PropertyValueList {
        override fun invoke(): PropertyValueList = properties.run {
            when (key) {
                DESCRIPTION_KEY -> title(
                    idOrName = key,
                    text = page.requirePropertyValue(key)
                )
                PAYMENT_DEADLINE_KEY -> {
                    date(
                        idOrName = key,
                        date = DateOrDateRange(
                            start = NotionDate(
                                timestamp = page
                                    .requirePropertyValue<DateOrDateRange>(key)
                                    .start
                                    .timestamp
                                    .toCalendar()
                                    .let { calendar ->
                                        GregorianCalendar(
                                            2022,
                                            calendar.get(Calendar.MONTH) + 1,
                                            calendar.get(Calendar.DATE)
                                        )
                                    }
                                    .time
                            )
                        )
                    )
                }
                MONTH_KEY -> selectByName(
                    idOrName = MONTH_KEY,
                    selectName = buildString {
                        val monthName = Month.from(currentMonth).name
                        append(monthName.first())
                        for (i in 1..monthName.lastIndex) {
                            append(monthName[i].lowercaseChar())
                        }
                    }
                )
                FINAL_AMOUNT_KEY, AMOUNT_RUB_KEY, AMOUNT_USD_KEY, AMOUNT_EUR_KEY -> number(
                    idOrName = key,
                    number = page.requirePropertyValue(key)
                )
                MONEY_SOURCE, EXPENSES_TYPE_KEY, FINANCIAL_YEAR_KEY -> selectByName(
                    idOrName = key,
                    selectName = page.requirePropertyValue(key)
                )
                USD_RATE_RELATION, EUR_RATE_RELATION, RELATED_TO_BALANCE -> relationsList(
                    idOrName = key,
                    pageIds = page.requirePropertyValue(key)
                )
                else -> {
                    throw IllegalArgumentException("We do not add value with $key into Regular Expenses Database")
                }
            }
        }
    }
}