package eb.subwindow.cardediting

import eb.data.DeckManager
import eb.popups.deleteCard
import eb.utilities.*
import eb.utilities.uiElements.UnfocusableButton
import java.awt.GridBagConstraints
import java.awt.Insets
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.DefaultListModel


abstract class GenericCardEditingWindow(protected val manager: CardEditingManager) : JFrame() {

    abstract fun clear()

    protected val listBox = JList(DefaultListModel<String>()).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        visibleRowCount = -1 // to keep all values visible
        isFocusable = false
        // don't set preferred size as it limits the number of items visible
    }

    // The button to cancel creating this card, and return to the calling window.
    private val cancelButton = UnfocusableButton("Cancel") { closeWindow() }

    private fun closeWindow() {
        manager.endEditing(this@GenericCardEditingWindow)
    }

    private val clearButton = UnfocusableButton("Clear") { clear() }

    protected val deleteButton = UnfocusableButton("Delete") { deleteCardWithCurrentFront() }



    private fun deleteCardWithCurrentFront() {
        val existingCardWithThisFront = DeckManager.currentDeck().cardCollection.getCardWithFront(cardPanes[0].text)
        deleteCard(this, existingCardWithThisFront!!)
        clear()
    }


    // The button to press that requests the current deck to check whether this
    // card is a valid/usable card (so no duplicate of an existing card, for
    // example) and if so, to add it.
    private val okButton = UnfocusableButton("Ok") { submitCandidateCardToDeck() }

    protected abstract fun submitCandidateCardToDeck()

    protected val enterKeyListener = SpecificKeyListener(KeyEvent.VK_ENTER) { okButton.doClick() }

    protected abstract val cardPanes: List<JTextPane>

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
        val newText = lines[0].cleanLayout()
        if (newText != pane.text) pane.text = newText
    }

    fun focusFront() = cardPanes[0].requestFocusInWindow()

    fun updateContents(vararg cardTexts: String) =
        cardTexts.forEachIndexed { index, contents -> cardPanes[index].text = contents }

    protected open fun addCopyButtonIfNeeded(panel: JPanel) {}

    protected fun addButtonPanel() {
        val buttonPane = JPanel().apply {
            add(cancelButton)
            add(deleteButton)
            addCopyButtonIfNeeded(this)
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
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel") //$NON-NLS-1$
        getRootPane().actionMap.put("Cancel", ProgrammableAction { cleanOrExit() })
        add(buttonPane, buttonPaneConstraints)
    }

    private fun cleanOrExit() {
        if (cardPanes.any { it.text.isNotBlank() }) clear() else closeWindow()
    }

    fun updateTitle() {
        title = "${DeckManager.currentDeck().name}: ${manager.getVerb()} card"
    }
}

