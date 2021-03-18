package eb.utilities

fun String.standardizeSeparator(separator: Char, whatItShouldLookLike: String): String {
    val words = this.split(separator).map { it.trim() }.filter { it != "" }
    return words.joinToString(separator = whatItShouldLookLike)
}

fun String.cleanDoubleQuotes(): String {
    var currentPartIsQuote = false
    var currentPartStart = 0
    var result = ""
    for (index in indices) {
        if (this[index] == '"') {
            result += cleanedQuotePart(substring(currentPartStart, index), currentPartIsQuote) + '"'
            currentPartIsQuote = !currentPartIsQuote
            currentPartStart = index + 1
        }
    }
    return (result + cleanedQuotePart(substring(currentPartStart), currentPartIsQuote)).trim()
}

private fun cleanedQuotePart(text: String, isQuote: Boolean): String =
    if (isQuote) {
        text.trim()
    } else {
        val trimmedText = text.trim()
        spaceBetweenEndQuoteAndNonClosingChar(trimmedText) + trimmedText +
                spaceBetweenNonOpeningCharAndStartQuote(trimmedText)
    }

private fun spaceBetweenNonOpeningCharAndStartQuote(text: String) =
    spaceIf(text) { !it.last().isOpeningChar() }

private fun spaceBetweenEndQuoteAndNonClosingChar(text: String) =
    spaceIf(text) { !it.first().isClosingChar() }

private fun spaceIf(text: String, condition: (String) -> Boolean) =
    if (text.isNotBlank() && condition(text)) " " else ""

private fun Char.isOpeningChar(): Boolean = when (this) {
    '(', '[', '\'', '{' -> true
    else -> false
}

private fun Char.isClosingChar(): Boolean = when (this) {
    ')', ']', '\'', '}' -> true
    else -> false
}
