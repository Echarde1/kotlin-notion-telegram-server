package regular_expenses

import EXPENSES_DATABASE_KEY
import REGULAR_EXPENSES_DATABASE_KEY
import currentMonth
import dotEnv
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import notionClient
import org.jraf.klibnotion.model.base.reference.DatabaseReference
import org.jraf.klibnotion.model.page.Page
import org.jraf.klibnotion.model.property.sort.PropertySort
import org.jraf.klibnotion.model.property.value.DatePropertyValue
import org.jraf.klibnotion.model.property.value.PropertyValueList
import requireVariable
import java.util.*

class UpdateRegularExpensesUseCase {

    private val REGULAR_EXPENSES_DATABASE by lazy { dotEnv.requireVariable(REGULAR_EXPENSES_DATABASE_KEY) }
    private val EXPENSES_DATABASE by lazy { dotEnv.requireVariable(EXPENSES_DATABASE_KEY) }

    context(PipelineContext<Unit, ApplicationCall>) suspend fun runCommand() {
        addNewMonthRegularExpenses()
        fillExpensesWithRegular()

        call.respondText("Suck my kiss")
    }

    private suspend fun addNewMonthRegularExpenses() {
        val regularExpenses by lazy {
            listOf(
                AddPaymentDateNewRegular(PAYMENT_DATE_KEY),
                AddDescription(DESCRIPTION_KEY),
                AddMonth(MONTH_KEY),
                AddSelect(FINANCIAL_YEAR_KEY),
                AddSelect(EXPENSES_TYPE_KEY),
                AddSelect(MONEY_SOURCE),
                AddNumber(FINAL_AMOUNT_KEY),
                AddNumber(AMOUNT_RUB_KEY),
                AddNumber(AMOUNT_USD_KEY),
                AddNumber(AMOUNT_EUR_KEY),
                AddRelation(USD_RATE_RELATION),
                AddRelation(EUR_RATE_RELATION),
                AddRelation(RELATED_TO_BALANCE),
            )
        }
        val regularExpensesPages = notionClient.databases.queryDatabase(
            REGULAR_EXPENSES_DATABASE,
            sort = PropertySort().ascending(PAYMENT_DATE_KEY)
        )
            .results
            .filter { page -> paymentMonthEqualsToGivenMonth(page, currentMonth - 1) }

        regularExpensesPages.forEach { page ->
            notionClient.pages.createPage(
                parentDatabase = DatabaseReference(id = REGULAR_EXPENSES_DATABASE),
                properties = PropertyValueList().also { properties ->
                    regularExpenses.forEach { expense ->
                        expense.runStrategy(page, properties)
                    }
                }
            )
        }
    }

    private suspend fun fillExpensesWithRegular() {
        val expenses by lazy {
            listOf(
                AddDescription(DESCRIPTION_KEY),
                AddSelect(FINANCIAL_YEAR_KEY),
                AddSelect(EXPENSES_TYPE_KEY),
                AddMonth(MONTH_KEY),
                AddNumber(FINAL_AMOUNT_KEY),
                AddRelation(RELATED_TO_BALANCE),
                AddPaymentDateToExpenses(PAYMENT_DATE_KEY)
            )
        }
        val regularExpenses = notionClient.databases.queryDatabase(
            REGULAR_EXPENSES_DATABASE,
            sort = PropertySort().ascending(PAYMENT_DATE_KEY)
        )
            .results
            .filter { page -> paymentMonthEqualsToGivenMonth(page, currentMonth) }

        regularExpenses.forEach { page ->
            notionClient.pages.createPage(
                parentDatabase = DatabaseReference(id = EXPENSES_DATABASE),
                properties = PropertyValueList().also { properties ->
                    expenses.forEach { expense ->
                        expense.runStrategy(page, properties)
                    }
                }
            )
        }
    }

    private fun paymentMonthEqualsToGivenMonth(page: Page, givenMonth: Int): Boolean {
        val calendar: Calendar = page
            .propertyValues
            .first { it.name == PAYMENT_DATE_KEY }
            .let { paymentDeadlineProperty ->
                requireNotNull((paymentDeadlineProperty as DatePropertyValue).value)
            }
            .start
            .timestamp
            .toCalendar()

        return calendar.get(Calendar.MONTH) == givenMonth
    }
}