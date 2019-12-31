package eb.subwindow

import eb.utilities.doNothing
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JTextPane

abstract class GenericCardEditingWindow : JFrame() {

    // The button to cancel creating this card, and return to the calling window.
    protected val cancelButton = JButton("Cancel")

    // The button to press that requests the current deck to check whether this
    // card is a valid/usable card (so no duplicate of an existing card, for
    // example) and if so, to add it.
    protected val okButton = JButton("Ok")

    protected val escapeKeyListener = SpecificKeyListener(KeyEvent.VK_ESCAPE) { cancelButton.doClick() }
    protected val enterKeyListener = SpecificKeyListener(KeyEvent.VK_ENTER) { okButton.doClick() }

    protected abstract val cardPanes: List<JTextPane>

    //Listens for a specific key and consumes it (and performs the appropriate action) when it is pressed
    protected inner class SpecificKeyListener(private val keyCode: Int, val action: () -> Unit) : KeyListener {
        override fun keyPressed(e: KeyEvent) {
            if (e.keyCode == keyCode) {
                e.consume()
                action()
            }
        }

        // dummy method: we only need to respond to the pressing of the key, not to the release.
        override fun keyReleased(arg0: KeyEvent) = doNothing

        // dummy method: we only need to respond to the pressing of the key, not to it being typed
        override fun keyTyped(arg0: KeyEvent) = doNothing
    }

    internal inner class CleaningFocusListener : FocusListener {

        override fun focusGained(arg0: FocusEvent) = doNothing

        override fun focusLost(arg0: FocusEvent) = standardizeFields()
    }

    fun standardizeFields() = cardPanes.forEachIndexed { index, pane ->
        val lines = pane.text.split("\n")
        (1..2).forEach {
            val cardIndex = index + it
            if (lines.size > it && cardIndex <= cardPanes.lastIndex) {
                cardPanes[cardIndex].run {
                    text += lines[it]
                    requestFocusInWindow()
                }
            }
        }
        pane.text = lines[0]

        pane.text = pane.text.standardizeSeparator(' ', " ")
        pane.text = pane.text.standardizeSeparator(',', ", ")
    }

    private fun String.standardizeSeparator(separator: Char, whatItShouldLookLike: String): String {
        val words = this.split(separator).map { it.trim() }.filter { it != "" }
        return words.joinToString(separator = whatItShouldLookLike)
    }

    fun focusFront() = cardPanes[0].requestFocusInWindow()

    fun updateContents(vararg cardTexts: String) = cardTexts.forEachIndexed { index, contents -> cardPanes[index].text = contents }

    fun clearContents() = cardPanes.forEach { it.text = "" }

}