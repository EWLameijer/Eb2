package eb.subwindow

import eb.utilities.SpecificKeyListener
import eb.utilities.doNothing
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.Insets
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.DefaultListModel


abstract class GenericCardEditingWindow(protected val manager: CardEditingManager) : JFrame() {

    protected val cardFronts = DefaultListModel<String>()
    protected abstract fun clear()

    protected val listBox = JList(cardFronts).apply {
        fixedCellWidth = 100
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        visibleRowCount = -1 // to keep all values visible
        preferredSize = Dimension(100, 400)
    }

    // The button to cancel creating this card, and return to the calling window.
    protected val cancelButton = JButton("Cancel").apply {
        addActionListener { manager.endEditing(this@GenericCardEditingWindow) }
    }

    private val clearButton = JButton("Clear").apply {
        addActionListener { clear() }
    }

    // The button to press that requests the current deck to check whether this
    // card is a valid/usable card (so no duplicate of an existing card, for
    // example) and if so, to add it.
    protected val okButton = JButton("Ok").apply {
        addActionListener { submitCandidateCardToDeck() }
    }

    protected abstract fun submitCandidateCardToDeck()

    protected val escapeKeyListener = SpecificKeyListener(KeyEvent.VK_ESCAPE) { cancelButton.doClick() }
    protected val enterKeyListener = SpecificKeyListener(KeyEvent.VK_ENTER) { okButton.doClick() }

    protected abstract val cardPanes: List<JTextPane>

    //Listens for a specific key and consumes it (and performs the appropriate action) when it is pressed


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

    fun updateContents(vararg cardTexts: String) =
        cardTexts.forEachIndexed { index, contents -> cardPanes[index].text = contents }

    fun clearContents() = cardPanes.forEach { it.text = "" }

    protected fun addButtonPanel() {
        val buttonPane = JPanel().apply {
            add(cancelButton)
            add(clearButton)
            add(okButton)
        }
        val buttonPaneConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 1
            weightx = 0.0
            weighty = 0.0
            insets = Insets(10, 10, 10, 10)
        }
        add(buttonPane, buttonPaneConstraints)
    }

}