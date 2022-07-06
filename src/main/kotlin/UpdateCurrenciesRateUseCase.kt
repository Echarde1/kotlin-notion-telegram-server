import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import org.jraf.klibnotion.model.property.value.NumberPropertyValue
import org.jraf.klibnotion.model.property.value.PropertyValueList
import org.jraf.klibnotion.model.property.value.TitlePropertyValue
import org.jraf.klibnotion.model.richtext.RichTextList

class UpdateCurrenciesRateUseCase {
    private val RUB_TO_USD_PAGE by lazy { dotEnv.requireVariable("RUB_TO_USD_PAGE") }
    private val RUB_TO_EUR_PAGE by lazy { dotEnv.requireVariable("RUB_TO_EUR_PAGE") }

    context(PipelineContext<Unit, ApplicationCall>) suspend fun runCommand() {
        val (dollar, eur) = httpClient
            .get<String>("https://www.cbr.ru/scripts/XML_daily.asp")
            .let { response ->
                xmlMapper.readValue<Currencies>(response).list
            }
            .let { currencies ->
                Pair(
                    first = currencies.first { it.numCode == 840 || it.charCode == "USD" },
                    second = currencies.first { it.numCode == 978 || it.charCode == "EUR" }
                )
            }

        updateCurrency(RUB_TO_USD_PAGE, dollar)
        updateCurrency(RUB_TO_EUR_PAGE, eur)
        call.respondText("Foo")
    }

    private suspend fun updateCurrency(pageId: String, currency: Currencies.Currency) = notionClient.pages.run {
        val page = getPage(pageId)
        val title = page.propertyValues.first { it is TitlePropertyValue }

        updatePage(
            id = pageId,
            properties = PropertyValueList()
                .title(
                    idOrName = title.name,
                    richTextList = RichTextList()
                        .text(text = requireNotNull((title.value as RichTextList).plainText))
                )
                .number(
                    idOrName = page.propertyValues.first { it is NumberPropertyValue }.name,
                    number = currency.value.replace(',', '.').toFloat()
                )
        )
    }
}