package eb.utilities

import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.io.Serializable
import java.lang.IllegalArgumentException
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoField
import java.util.HashSet
import java.util.logging.Logger
import java.util.regex.Pattern
import javax.swing.JButton
import javax.swing.JComponent

import javax.swing.KeyStroke
import kotlin.math.abs

/**
 * Contains some tools/generic methods that are not application-domain specific
 * but are nevertheless useful.
 *
 * @author Eric-Wubbo Lameijer
 */

// A string that contains (visible) information. Later possibly a picture or a sound recording or a combination
class Hint(rawContents: String) : Comparable<Hint>, Serializable {

    val contents = if (rawContents.isBlank())
        throw IllegalArgumentException("A Hint object needs to contain visible data")
    else rawContents.trim()

    override fun compareTo(other: Hint) = contents.compareTo(other.contents)

    override fun toString() = contents

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hint

        if (contents != other.contents) return false

        return true
    }

    override fun hashCode(): Int {
        return contents.hashCode()
    }

    companion object {
        fun isValid(candidateContents: String) = candidateContents.isNotBlank()
        private const val serialVersionUID = -6526056675010032709L // to prevent updates from breaking Eb
    }
}

val doNothing = Unit

const val EMPTY_STRING = ""

val String.isValidIdentifier
    get() = this.isNotBlank()

fun log(text: String) = Logger.getGlobal().info(text)

fun Int.toLiteralChar(): Char = (this + '0'.toInt()).toChar()

// ensures 1, 2, 3 are printed as "01", "02" and "03" etc.
fun Int.asTwoDigitString(): String {
    val twoDigitFormat = "%02d"
    return twoDigitFormat.format(this)
}

fun getDateString(): String {
    val now = LocalDateTime.now()
    return (now[ChronoField.YEAR] % 100).asTwoDigitString() +
            now[ChronoField.MONTH_OF_YEAR].asTwoDigitString() +
            (now[ChronoField.DAY_OF_MONTH]).asTwoDigitString() +
            "_" +
            now[ChronoField.HOUR_OF_DAY].asTwoDigitString() +
            now[ChronoField.MINUTE_OF_HOUR].asTwoDigitString()
}

object Utilities {
    // Line separator that, unlike '\n', consistently works when producing output
    val EOL: String = System.getProperty("line.separator")

    fun toRegionalString(str: String) = str.replace('.', decimalSeparator)

    fun stringToDouble(string: String): Double? {
        // Get a numberFormat object. Note that the number it returns will be Long
        // if possible, otherwise a Double.
        val numberFormat = NumberFormat.getNumberInstance()
        val parsePosition = ParsePosition(0)
        val number = numberFormat.parse(string, parsePosition)
        return if (parsePosition.index == 0) null else number.toDouble()
    }

    /**
     * Transfer focus when the user presses the tab key, overriding default
     * behavior in components where tab adds a tab to the contents. After applying
     * this function to a component, TAB transfers focus to the next focusable
     * component, SHIFT+TAB transfers focus to the previous focusable component.
     *
     * @param component
     * the component to be patched
     */
    fun makeTabTransferFocus(component: Component) {
        var strokes: HashSet<KeyStroke> = HashSet(listOf(KeyStroke.getKeyStroke("pressed TAB")))
        component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, strokes)
        strokes = HashSet(listOf(KeyStroke.getKeyStroke("shift pressed TAB")))
        component.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, strokes)
    }

    /**
     * Transfers focus when the user presses the tab or enter keys, overriding
     * default behavior in components where pressing the tab key adds a tab and
     * pressing the enter key adds a newline to the contents.
     *
     * @param component
     * the component to be patched.
     */
    fun makeTabAndEnterTransferFocus(component: Component) {
        var strokes: MutableSet<KeyStroke> = HashSet(listOf(KeyStroke.getKeyStroke("pressed TAB")))
        strokes.addAll(listOf(KeyStroke.getKeyStroke("pressed ENTER")))
        component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, strokes)
        strokes = HashSet(listOf(KeyStroke.getKeyStroke("shift pressed TAB")))
        component.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, strokes)
    }

    /**
     * Converts a floating point number to a string with a maximum precision, but
     * does so in a display-friendly way, so that, if the precision is 2, for
     * example, not 10.00 is displayed, but 10.
     *
     * @param number
     * the number that is to be converted to a string.
     * @param maxPrecision
     * the maximum number of digits after the period, fewer may be
     * displayed if the last digits would be 0.
     */
    fun doubleToMaxPrecisionString(number: Double, maxPrecision: Int): String {
        // preconditions: maxPrecision should be 0 or greater
        require(maxPrecision >= 0) {
            "Utilities.doubleToMaxPrecisionString error: the given precision should be 0 or positive."
        }

        val numberFormatter = DecimalFormat()
        numberFormatter.isGroupingUsed = false
        numberFormatter.maximumFractionDigits = maxPrecision
        numberFormatter.roundingMode = RoundingMode.HALF_UP
        return numberFormatter.format(number)
    }

    // returns this locale's decimal separator.
    private val decimalSeparator = DecimalFormat().decimalFormatSymbols.decimalSeparator

    // returns whether the given string is fully filled with a valid integer (...-2,-1,0,1,2,...).
    // Note that this method does not accept leading or trailing whitespace, nor a '+' sign.
    fun representsInteger(string: String, maxSize: Int? = null) =
        if (maxSize != null && string.length > maxSize) false
        else Pattern.matches("-?\\d+", string)

    /**
     * Whether the given string is fully filled with a valid fractional
     * number of a given maximum precision (like -2.1, or 5.17 or 10, or
     * .12). Note that this method does not accept leading or trailing
     * whitespace, nor a '+' sign.
     *
     * @param string
     * the string to be tested
     * @param maxPrecision
     * the maximum precision (maximum number of digits) in the fractional part.
     */
    private fun representsFractionalNumber(string: String, maxPrecision: Int): Boolean {
        require(maxPrecision >= 0) {
            "Utilities.representsFractionalNumber() error: the maximum precision should be a positive number."
        }
        if (!string.isValidIdentifier) {
            return false
        }
        val decimalSeparatorAsRegex = if (decimalSeparator == '.') "\\." else decimalSeparator.toString()
        val fractionalNumberRegex = ("-?\\d*$decimalSeparatorAsRegex?\\d{0,$maxPrecision}")
        return Pattern.matches(fractionalNumberRegex, string)
    }

    /**
     * Whether a given string represents a positive fractional number.
     *
     * @param string
     * the string to be checked for being a positive fractional number
     * @param maxPrecision
     * the maximum number of digits after the decimal separator
     * @return whether this is a positive fractional/rational number (or a
     * positive integer, that is also formally a rational number)
     */
    fun representsPositiveFractionalNumber(string: String, maxPrecision: Int) =
        if (string.startsWith("-")) false
        else representsFractionalNumber(string, maxPrecision)

    fun stringToInt(string: String): Int? {
        // Get a numberFormat object. Note that the number it returns will be Long
        // if possible, otherwise a Double.
        val numberFormat = NumberFormat.getNumberInstance()
        val parsePosition = ParsePosition(0)
        val number = numberFormat.parse(string, parsePosition)
        return if (parsePosition.index == 0) null else number.toInt()
    }

    fun durationToString(duration: Duration) = buildString {
        var durationAsSeconds = duration.seconds
        var finalPrefix = ""
        if (durationAsSeconds < 0) {
            durationAsSeconds *= -1
            finalPrefix = "minus "
        }
        val seconds = durationAsSeconds % 60
        append(" seconds")
        insert(0, seconds)
        val durationAsMinutes = durationAsSeconds / 60
        if (durationAsMinutes > 0) {
            val minutes = durationAsMinutes % 60
            insert(0, "$minutes minutes and ")
            val durationAsHours = durationAsMinutes / 60
            if (durationAsHours > 0) {
                val hours = durationAsHours % 24
                insert(0, "$hours hours, ")
                val days = durationAsHours / 24
                if (days > 0) {
                    insert(0, "$days days, ")
                }
            }
        }
        insert(0, finalPrefix)
    }


    fun durationToSeconds(duration: Duration): Double {
        val nanoPart = duration.nano / 1_000_000_000.0
        val secondsPart = duration.seconds.toDouble()
        return secondsPart + nanoPart
    }

    fun multiplyDurationBy(baseDuration: Duration, multiplicationFactor: Double): Duration {
        // we work with things like 0.01 s. So two decimal places. Unfortunately,
        // we can only multiply by longs, not doubles.
        val hundredthBaseDuration = baseDuration.dividedBy(100)
        val scalarTimesHundred = (multiplicationFactor * 100.0).toLong()

        return hundredthBaseDuration.multipliedBy(scalarTimesHundred)
    }

    /**
     * returns whether two doubles are 'practically equal', their difference being
     * smaller than 1/1000th of the smallest number (or of the largest number, if
     * the smallest number happens to equal zero).
     *
     * @param d1
     * the first double to be compared
     * @param d2
     * the second double to be compared
     *
     * @return whether the two doubles are practically equal within 1/1000th of
     * the smallest number unequal to zero.
     */
    fun doublesEqualWithinThousands(d1: Double, d2: Double): Boolean {
        val smallestAllowedDifference = 0.001

        return when {
            java.lang.Double.doubleToLongBits(d1 - d2) == 0L -> true
            // Note that d2 cannot be 0.0 because otherwise the first if-statement
            // would already have returned.
            java.lang.Double.doubleToLongBits(d1) == 0L -> abs(d2) < smallestAllowedDifference
            java.lang.Double.doubleToLongBits(d2) == 0L -> abs(d1) < smallestAllowedDifference
            else -> {
                val largerAbsoluteNumber: Double
                val smallerAbsoluteNumber: Double
                if (abs(d1) > abs(d2)) {
                    largerAbsoluteNumber = d1
                    smallerAbsoluteNumber = d2
                } else {
                    largerAbsoluteNumber = d2
                    smallerAbsoluteNumber = d1
                }
                val ratio = (largerAbsoluteNumber - smallerAbsoluteNumber) / smallerAbsoluteNumber
                abs(ratio) < smallestAllowedDifference
            }
        }
    }

    // Utility function: gives the right version (singular or plural) for a noun given the number,
    // so 0 cards, 1 card, 2 cards etc.
    fun pluralize(word: String, number: Int) = word + if (number == 1) EMPTY_STRING else "s"

    private fun createKeyPressSensitiveButton(text: String, actionKey: KeyStroke, action: () -> Unit): JButton =
        JButton(text).apply {
            val actionOnKeyPressId = "actionOnKeyPress"
            mnemonic = KeyEvent.getExtendedKeyCodeForChar(actionKey.keyChar.toInt())
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(actionKey, actionOnKeyPressId)
            actionMap.put(actionOnKeyPressId, ProgrammableAction { action() })
            addActionListener { action() }
        }

    fun createKeyPressSensitiveButton(text: String, key: Char, action: () -> Unit): JButton =
        createKeyPressSensitiveButton(text, KeyStroke.getKeyStroke(key), action)

    fun createKeyPressSensitiveButton(text: String, key: String, action: () -> Unit): JButton =
        createKeyPressSensitiveButton(text, KeyStroke.getKeyStroke(key), action)
}

fun String.pluralize(number: Int) = "$number ${Utilities.pluralize(this, number)}"
