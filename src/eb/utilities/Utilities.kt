package eb.utilities

import java.awt.Component
import java.awt.KeyboardFocusManager
import java.io.Serializable
import java.lang.IllegalArgumentException
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition
import java.time.Duration
import java.util.Arrays
import java.util.HashSet
import java.util.Optional
import java.util.logging.Logger
import java.util.regex.Pattern

import javax.swing.KeyStroke

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

    override fun compareTo(other: Hint): Int {
        return contents.compareTo(other.contents)
    }

    override fun toString() = contents

    companion object {
        fun isValid(candidateContents: String) = !candidateContents.isBlank()
    }
}

const val EMPTY_STRING = ""

val String.isValidIdentifier
    get() = !this.isNullOrBlank()

fun log(text: String) = Logger.getGlobal().info(text)


object Utilities {
    // Line separator that, unlike \n, consistently works when displaying
    // output
    val EOL = System.getProperty("line.separator")


    /**
     * Parses a string to a double. Returns Optional.empty() if the number cannot
     * be parsed.
     *
     * @param string
     * the string to be parsed to a double.
     *
     * @return an Optional<Double> that contains a double value, if the string
     * could be parsed to one.
    </Double> */
    fun stringToDouble(string: String): Double? {
        // Get a numberFormat object. Note that the number it returns will be Long
        // if possible, otherwise a Double.
        val numberFormat = NumberFormat.getNumberInstance()
        val parsePosition = ParsePosition(0)
        val number = numberFormat.parse(string, parsePosition)
        return if (parsePosition.index == 0) null else number.toDouble()
    }
    /**
     * If condition is false, exit the program while writing the specified error
     * message to standard error output.
     *
     * @param condition
     * the condition which needs to be true if the program is to be
     * allowed to continue.
     * @param errorMessage
     * the error message being sent to the standard error output if the
     * condition is false
     */


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
        // preconditions: none
        var strokes: Set<KeyStroke> = HashSet(
                Arrays.asList(KeyStroke.getKeyStroke("pressed TAB")))
        component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                strokes)
        strokes = HashSet(
                Arrays.asList(KeyStroke.getKeyStroke("shift pressed TAB")))
        component.setFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, strokes)
        // postconditions: none
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
        // preconditions: none
        var strokes: MutableSet<KeyStroke> = HashSet(
                Arrays.asList(KeyStroke.getKeyStroke("pressed TAB")))
        strokes.addAll(Arrays.asList(KeyStroke.getKeyStroke("pressed ENTER")))
        component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                strokes)
        strokes = HashSet(
                Arrays.asList(KeyStroke.getKeyStroke("shift pressed TAB")))
        component.setFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, strokes)
        // postconditions: none
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
     *
     * @return the number, with given maximum precision, in String format.
     */
    fun doubleToMaxPrecisionString(number: Double,
                                   maxPrecision: Int): String {
        // preconditions: maxPrecision should be 0 or greater
        require(maxPrecision >= 0) {
                "Utilities.doubleToMaxPrecisionString error: the given precision should be 0 or positive."}

        // 1. Build the format String
        val numberFormatter = DecimalFormat()
        numberFormatter.isGroupingUsed = false
        numberFormatter.maximumFractionDigits = maxPrecision
        numberFormatter.roundingMode = RoundingMode.HALF_UP
        val result = numberFormatter.format(number)

        return result

        // postconditions: none. Should simply return the String, and I trust that
        // that works.
    }

    /**
     * CPPRCC Is this character the decimal separator of the current locale?
     *
     * @param ch
     * the character to be tested as being this locale's decimal
     * separator
     *
     * @return whether the character is this locale's decimal separator
     */
    fun isDecimalSeparator(ch: Char): Boolean {
        // preconditions: none. the character cannot be null, for example.
        return ch == decimalSeparator
        // postconditions: none. Simple return of boolean.
    }

    /**
     * Returns the decimal separator of this locale.
     *
     * @return this locale's decimal separator.
     */
    val decimalSeparator = DecimalFormat().decimalFormatSymbols.decimalSeparator

    /**
     * Whether the given string is fully filled with a valid integer
     * (...-2,-1,0,1,2,...). Note that this method does not accept leading or
     * trailing whitespace, nor a '+' sign.
     *
     * @param string
     * the string to be tested
     * @return whether the string is a string representation of an integer.
     */
    fun representsInteger(string: String): Boolean {
        // preconditions: string should not be null or empty.
        return Pattern.matches("-?\\d+", string)
        // postconditions: none: simple return of boolean.
    }

    /**
     * Whether the given string is fully filled with a valid integer
     * (...-2,-1,0,1,2,...). Note that this method does not accept leading or
     * trailing whitespace, nor a '+' sign.
     *
     * @param string
     * the string to be tested
     * @return whether the string is a string representation of an integer.
     */
    fun representsInteger(string: String, maxSize: Int): Boolean {
        // preconditions: string should not be null or empty.
        return if (string.length > maxSize) {
            false
        } else {
            representsInteger(string)
        }
        // postconditions: none: simple return of boolean.
    }

    /**
     * @@@CPPRC Whether the given string is fully filled with a valid fractional
     * number of a given maximum precision (like -2.1, or 5.17 or 10, or
     * .12). Note that this method does not accept leading or trailing
     * whitespace, nor a '+' sign.
     *
     * @param string
     * the string to be tested
     * @param maxPrecision
     * the maximum precision (maximum number of digits) in the fractional
     * part.
     *
     * @return whether the string is a string representation of a fractional
     * number.
     */
    fun representsFractionalNumber(string: String,
                                   maxPrecision: Int): Boolean {
        // preconditions: string should not be null or empty, and maxPrecision
        // should be positive
        require(maxPrecision >= 0) {
            "Utilities.representsFractionalNumber() error: the maximum precision should be a positive number."}
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
    fun representsPositiveFractionalNumber(string: String,
                                           maxPrecision: Int): Boolean {
        // preconditions handled by wrapped representsFractionalNumber
        return if (string.startsWith("-")) {
            false
        } else representsFractionalNumber(string, maxPrecision)
// postconditions: none; simple return of boolean
    }


    /**
     * Parses a string to a double. Returns Optional.empty() if the number cannot
     * be parsed.
     *
     * @param string
     * the string to be parsed to a double.
     *
     * @return an Optional<Double> that contains a double value, if the string
     * could be parsed to one.
    </Double> */
    fun stringToInt(string: String): Int? {
        // Get a numberFormat object. Note that the number it returns will be Long
        // if possible, otherwise a Double.
        val numberFormat = NumberFormat.getNumberInstance()
        val parsePosition = ParsePosition(0)
        val number = numberFormat.parse(string, parsePosition)
        return if (parsePosition.index == 0) null else  number.toInt()

        // postconditions: none. All possible cases should have been handled by the
        // Optional.
    }

    fun durationToString(duration: Duration): String {
        var durationAsSeconds = duration.seconds
        var finalPrefix = ""
        if (durationAsSeconds < 0) {
            durationAsSeconds *= -1
            finalPrefix = "minus "
        }
        val seconds = durationAsSeconds % 60
        val output = StringBuilder(" seconds")
        output.insert(0, seconds)
        val durationAsMinutes = durationAsSeconds / 60
        if (durationAsMinutes > 0) {
            val minutes = durationAsMinutes % 60
            output.insert(0, minutes.toString() + " minutes and ")
            val durationAsHours = durationAsMinutes / 60
            if (durationAsHours > 0) {
                val hours = durationAsHours % 24
                output.insert(0, hours.toString() + " hours, ")
                val days = durationAsHours / 24
                if (days > 0) {
                    output.insert(0, days.toString() + " days, ")
                }
            }
        }
        output.insert(0, finalPrefix)
        return output.toString()
    }

    fun durationToSeconds(duration: Duration): Double {
        val nanoPart = duration.nano / 1_000_000_000.0
        val secondsPart = duration.seconds.toDouble()
        return secondsPart + nanoPart
    }

    fun multiplyDurationBy(baseDuration: Duration,
                           multiplicationFactor: Double): Duration {
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
        if (java.lang.Double.doubleToLongBits(d1 - d2) == 0L) {
            return true
        }

        if (java.lang.Double.doubleToLongBits(d1) == 0L) {
            // Note that d2 cannot be 0.0 because otherwise the first if-statement
            // would already have returned.
            return Math.abs(d2) < smallestAllowedDifference
        } else if (java.lang.Double.doubleToLongBits(d2) == 0L) {
            return Math.abs(d1) < smallestAllowedDifference
        } else {
            val largerAbsoluteNumber: Double
            val smallerAbsoluteNumber: Double
            if (Math.abs(d1) > Math.abs(d2)) {
                largerAbsoluteNumber = d1
                smallerAbsoluteNumber = d2
            } else {
                largerAbsoluteNumber = d2
                smallerAbsoluteNumber = d1
            }
            val ratio = (largerAbsoluteNumber - smallerAbsoluteNumber) / smallerAbsoluteNumber

            return Math.abs(ratio) < smallestAllowedDifference
        }
    }

    /**
     * Utility function: gives the right version (singular or plural) for a noun
     * given the number, so 0 cards, 1 card, 2 cards etc.
     *
     * @param word
     * the word that may need to be pluralized.
     * @param number
     * the number of items.
     * @return the word in singular or plural form, whatever is appropriate.
     */
    fun pluralize(word: String, number: Int): String {
        return if (number == 1) {
            word
        } else {
            word + "s"
        }
    }

    /**
     * Produces a nicely formatted count of the number, for example (3, "point")
     * is converted into "3 points".
     *
     * @param number
     * the number to be formatted.
     * @param word
     * the "unit" in which the number is expressed (like point)
     * @return a nicely formatted string like "1 dog" or "2 cats"
     */
    fun pluralText(number: Int, word: String): String {
        return number.toString() + " " + pluralize(word, number)
    }
}

