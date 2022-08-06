fun String.removePunctuation() = replace(Regex("[,!?\\\\-]"), "")
