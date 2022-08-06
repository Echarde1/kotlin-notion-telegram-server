package regular_expenses

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "ValCurs")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Currencies constructor(
    @get:JacksonXmlElementWrapper(useWrapping = false)
    @get:JacksonXmlProperty(localName = "Valute")
    val list: MutableList<Currency>
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Currency constructor(
        @get:JacksonXmlProperty(localName = "NumCode")
        val numCode: Int,
        @get:JacksonXmlProperty(localName = "CharCode")
        val charCode: String,
        @get:JacksonXmlProperty(localName = "Name")
        val name: String,
        @get:JacksonXmlProperty(localName = "Value")
        val value: String
    )
}
