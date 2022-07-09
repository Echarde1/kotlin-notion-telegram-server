import org.jraf.klibnotion.model.property.value.PropertyValueList

fun PropertyValueList.relationsList(idOrName: String, pageIds: List<String>): PropertyValueList =
    relation(
        idOrName = idOrName,
        *pageIds.toTypedArray()
    )