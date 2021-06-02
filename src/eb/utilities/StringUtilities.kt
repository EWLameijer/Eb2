package eb.utilities

import java.lang.IllegalArgumentException

fun String.cleanLayout() =
    standardizeSeparator(' ', " ")
        .standardizeSeparator(',', ", ")
        .cleanDoubleQuotes()
        .cleanParentheses()
        .cleanSquareBrackets()
        .cleanAccolades()

private fun String.cleanParentheses() = cleanTextEnclosings('(', ')')
private fun String.cleanSquareBrackets() = cleanTextEnclosings('[', ']')
private fun String.cleanAccolades() = cleanTextEnclosings('{', '}')

private fun String.cleanTextEnclosings(startChar: Char, endChar: Char): String {
    val numOpeningParens = count { it == startChar }
    val numClosingParens = count { it == endChar }

    if (numOpeningParens == 0 || numOpeningParens != numClosingParens) return this
    var currentString = this
    var result = ""
    while (true) {
        val nextOpenParentPos = currentString.indexOf(startChar)
        if (nextOpenParentPos == -1) break
        val (beforeOpenParent, afterOpenParent) = currentString.splitAt(nextOpenParentPos)
        result = result.smartAdd(beforeOpenParent)
        result = result.smartAdd("$startChar")
        val (insideParens, rest) = analyzeParentheses(afterOpenParent, startChar, endChar)
        result = result.smartAdd(insideParens)
        result += "$endChar"
        currentString = rest
    }
    return result.smartAdd(currentString)
}

private fun String.splitAt(position: Int): Pair<String, String> {
    if (position >= length) throw IllegalArgumentException("String.splitAt cannot split beyond the end of the string")
    return substring(0, position) to substring(position + 1)
}

fun String.italicizeIf(condition: Boolean): String = if (condition) "<html><i>$this</i></html>" else this

private fun analyzeParentheses(text: String, startChar: Char, endChar: Char): Pair<String, String> {
    // input: something basically X[closing char]Y
    // output X, Y

    var currentString = text
    var result = ""
    while (true) {
        val firstOpeningParentPos = currentString.indexOf(startChar)
        val firstClosingParentPos = currentString.indexOf(endChar)
        if (firstOpeningParentPos == -1 || firstClosingParentPos < firstOpeningParentPos) {
            if (firstClosingParentPos == -1) return result to currentString
            val (beforeOpeningParent, afterOpeningParent) = currentString.splitAt(firstClosingParentPos)
            result = result.smartAdd(beforeOpeningParent)
            return result to afterOpeningParent
        }
        // apparently something like [...]?([...]?)[()]?[...]?)
        var (contentsBefore, contentsAfter) = currentString.splitAt(firstOpeningParentPos)
        result += contentsBefore
        result = result.smartAdd("$startChar")
        val (insideParentheses, afterParenthesis) = analyzeParentheses(contentsAfter, startChar, endChar)
        result = result.smartAdd(insideParentheses)
        result += endChar
        currentString = afterParenthesis
    }
}

private fun String.smartAdd(other: String): String {
    val firstPart = trim()
    val secondPart = other.trim()

    return when {
        firstPart == "" -> secondPart
        secondPart == "" -> firstPart
        shouldNotHaveSpaceInBetween(firstPart.last(), secondPart.first()) -> firstPart + secondPart
        else -> "$firstPart $secondPart"
    }
}

private fun shouldNotHaveSpaceInBetween(preChar: Char, postChar: Char): Boolean =
    (preChar.isOpeningChar()) ||
            (preChar.isClosingChar() && postChar.isPunctuationChar())

private fun Char.isPunctuationChar(): Boolean = this in listOf('.', ',', '!', '?', ':', ';')

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
