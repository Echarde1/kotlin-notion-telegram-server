package regular_expenses

import currentMonth
import io.ktor.util.date.*
import org.jraf.klibnotion.model.date.DateOrDateRange
import org.jraf.klibnotion.model.page.Page
import org.jraf.klibnotion.model.property.value.PropertyValueList
import relationsList
import java.util.*
import org.jraf.klibnotion.model.date.Date as NotionDate

abstract class AddRegularPaymentStrategy(
    protected val key: String
) {

    abstract fun runStrategy(
        page: Page,
        properties: PropertyValueList,
    ): PropertyValueList
}

class AddDescription(key: String) : AddRegularPaymentStrategy(key) {
    override fun runStrategy(
        page: Page,
        properties: PropertyValueList,
    ): PropertyValueList = properties.title(
        idOrName = key,
        text = page.requirePropertyValue(key)
    )
}

/**
 * Стратегия для создания новой записи с датой оплаты в таблице Regular Expenses
 */
class AddPaymentDateNewRegular(key: String) : AddRegularPaymentStrategy(key) {
    override fun runStrategy(
        page: Page,
        properties: PropertyValueList,
    ): PropertyValueList = properties.date(
        idOrName = key,
        date = DateOrDateRange(
            start = NotionDate(
                timestamp = page
                    .getCalendarFromDateOrDateRange(key)
                    .let { calendar ->
                        GregorianCalendar(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.DATE)
                        )
                    }
                    .time
            )
        )
    )
}

/**
 * Стратегия для создания страницы с Regular Expenses
 */
class AddPaymentDateToExpenses(key: String) : AddRegularPaymentStrategy(key) {
    override fun runStrategy(
        page: Page,
        properties: PropertyValueList,
    ): PropertyValueList = properties.date(
        idOrName = key,
        date = DateOrDateRange(
            start = NotionDate(
                timestamp = page
                    .getCalendarFromDateOrDateRange(key)
                    .let { calendar ->
                        GregorianCalendar(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DATE)
                        )
                    }
                    .time
            )
        )
    )
}

class AddMonth(key: String) : AddRegularPaymentStrategy(key) {
    override fun runStrategy(
        page: Page,
        properties: PropertyValueList,
    ): PropertyValueList = properties.selectByName(
        idOrName = key,
        selectName = buildString {
            val monthName = Month.from(currentMonth).name
            append(monthName.first())
            for (i in 1..monthName.lastIndex) {
                append(monthName[i].lowercaseChar())
            }
        }
    )
}

class AddNumber(key: String) : AddRegularPaymentStrategy(key) {
    override fun runStrategy(
        page: Page,
        properties: PropertyValueList,
    ): PropertyValueList = properties.number(
        idOrName = key,
        number = page.requirePropertyValue(key)
    )
}

class AddSelect(key: String) : AddRegularPaymentStrategy(key) {
    override fun runStrategy(
        page: Page,
        properties: PropertyValueList,
    ): PropertyValueList = properties.selectByName(
        idOrName = key,
        selectName = page.requirePropertyValue(key)
    )
}

class AddRelation(key: String) : AddRegularPaymentStrategy(key) {
    override fun runStrategy(
        page: Page,
        properties: PropertyValueList,
    ): PropertyValueList = properties.relationsList(
        idOrName = key,
        pageIds = page.requirePropertyValue(key)
    )
}
