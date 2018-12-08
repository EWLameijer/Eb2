package eb.utilities

import java.util.logging.Logger

import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.JTextComponent
import javax.swing.text.PlainDocument

/**
 * Helps create text fields that only accept numbers and have a certain maximum
 * size.
 *
 * from:
 * http://stackoverflow.com/questions/1313390/is-there-any-way-to-accept-only-
 * numeric-values-in-a-jtextfield
 *
 * @author Terraego
 * @author Eric-Wubbo Lameijer
 */
/**
 * Constructor.
 *
 * @param owner
 * the JTextComponent that owns this instance.
 * @param fixedSize
 * the maximum length of the resulting string.
 * @param sizeOfFractionalPart
 * the maximum length of the fractional part (for example, 2.45 has a
 * fractional part of length 2)
 */
class FixedSizeNumberDocument (
        private val owner: JTextComponent,
        private val fixedSize: Int,
        private val sizeOfFractionalPart: Int) : PlainDocument() {

    init {
        // preconditions:
        require(fixedSize > 0) {
            "FixedSizeNumberDocument constructor error: the number of characters allowed must be greater than zero."}
        require(sizeOfFractionalPart >= 0) {
                "FixedSizeNumberDocument constructor error: the number of digits in the fractional part cannot be negative."}
        if (sizeOfFractionalPart > 0) {
            require(fixedSize >= sizeOfFractionalPart + 2) {
                    """FixedSizeNumberDocument constructor error: the size allotted to a real
                     number must be at least two greater than the number of digits allotted to the fractional part."""}
        }
    }

    @Throws(BadLocationException::class)
    override
            /**
             * Inserts a new string into this document -unless the string is, for some
             * reason, invalid or would give other problems.
             */
    fun insertString(offs: Int, str: String?, a: AttributeSet?) {

        val originalText = owner.text

        // first test: if the original string is long enough, you cannot insert.
        if (originalText.length >= fixedSize) {
            owner.toolkit.beep()
            return
        }

        // what would the new text look like?
        // Note that (to my knowledge) we need to work with the
        // PlainDocument.insertString() function. This means that extra characters
        // must be removed from the inserted string itself, not from the end of the
        // original.
        val textToBeInserted = StringBuilder(str!!)
        val candidateLength = originalText.length + textToBeInserted.length
        if (candidateLength > fixedSize) {
            val numberOfCharactersToRemove = candidateLength - fixedSize
            val newInsertionLength = textToBeInserted.length - numberOfCharactersToRemove
            textToBeInserted.setLength(newInsertionLength)
            owner.toolkit.beep()
        }
        val candidateText = StringBuilder(originalText)
        candidateText.insert(offs, textToBeInserted)

        if (representsValidContents(candidateText.toString())) {
            super.insertString(offs, textToBeInserted.toString(), a)
        } else {
            owner.toolkit.beep()
            Logger.getGlobal().info("problems inserting $str")
        }
    }

    /**
     * Returns whether the contents of this FixedSizeNumberDocument should
     * represent an integer, as opposed to a fractional number.
     *
     * @return whether the contents of this FixedSizeNumberDocument should
     * represent an integer.
     */
    private fun contentsShouldRepresentInteger(): Boolean {
        // preconditions: none. Should work when the object exists.
        return sizeOfFractionalPart == 0
        // postconditions: none. Simple return of boolean.
    }

    /**
     * Returns whether this string would return valid text box contents, so either
     * the empty string (users must be able to clear the text box), or an integer
     * or fractional number.
     *
     * @param candidateText
     * the string to be checked for being a valid state of the text box.
     *
     * @return whether the candidate text would be valid contents for this text
     * box
     */
    private fun representsValidContents(candidateText: String) =  when {
        // preconditions: candidateText should not be null
        candidateText === EMPTY_STRING ->  true // after all, "" is a valid state for a text box.
        contentsShouldRepresentInteger() -> Utilities.representsInteger(candidateText, fixedSize)
        else -> Utilities.representsPositiveFractionalNumber(candidateText, sizeOfFractionalPart)
    }

    companion object {
        private const val serialVersionUID = 7355097701705745079L
    }
}