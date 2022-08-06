package regular_expenses

import calendar
import org.jraf.klibnotion.model.date.DateOrDateRange
import org.jraf.klibnotion.model.page.Page
import org.jraf.klibnotion.model.property.value.SelectPropertyValue
import org.jraf.klibnotion.model.property.value.TitlePropertyValue
import java.util.*

fun Date.toCalendar(): Calendar = run {
    calendar.time = this
    calendar
}

inline fun <reified T> Page.requirePropertyValue(key: String): T {
    val property = propertyValues
        .first { propertyValue -> propertyValue.name == key }
    return when (property) {
        is TitlePropertyValue -> property.value.plainText as T
        is SelectPropertyValue -> property.value!!.name as T
        else -> property.value as T
    }
}

fun Page.getCalendarFromDateOrDateRange(propertyKey: String): Calendar =
    requirePropertyValue<DateOrDateRange>(propertyKey)
        .start
        .timestamp
        .toCalendar()