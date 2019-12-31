package eb.subwindow

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets

import eb.data.DeckManager
import eb.utilities.Utilities
import javax.swing.*

/**
 * CardEditingWindow allows the user to add a new card to the deck, or to edit
 * an existing card.
 *
 * It is managed by a CardEditingManager object, which checks the returned
 * contents and opens/closes it.
 *
 * @author Eric-Wubbo Lameijer
 */
class CardEditingWindow(frontText: String, backText: String, private val manager: CardEditingManager) : GenericCardEditingWindow() {

    // Create the panel to edit the front of the card, and make enter
    // and tab transfer focus to the panel for editing the back of the card.
    // Escape should cancel the card-creating process and close the
    // NewCardWindow
    private val cardFrontPane = JTextPane().apply {
        text = frontText
        Utilities.makeTabAndEnterTransferFocus(this)
        addKeyListener(escapeKeyListener)
        addFocusListener(CleaningFocusListener())
    }

    // Now create the panel to edit the back of the card; make tab transfer
    // focus to the front (for editing the front again), escape should (like
    // for the front panel) again cancel editing and close the NewCardWindow.
    // Pressing the Enter key, however, should try save the card instead of
    // transferring the focus back to the front-TextArea.
    private val cardBackPane = JTextPane().apply {
        text = backText
        Utilities.makeTabTransferFocus(this)
        addKeyListener(enterKeyListener)
        addKeyListener(escapeKeyListener)
        addFocusListener(CleaningFocusListener())
    }

    override val cardPanes = listOf(cardFrontPane, cardBackPane)

    init {
        // preconditions: none (we can assume the user clicked the appropriate
        // button, and even otherwise there is not a big problem)
        val operation = if (manager.inCardCreatingMode()) "add" else "edit"
        this.title = "${DeckManager.currentDeck().name}: $operation card"

        // we just want tab to cycle from the front to the back of the card,
        // and vice versa, and not hit the buttons
        cancelButton.isFocusable = false
        okButton.isFocusable = false
    }

    /**
     * Converts the current contents of the NewCardWindow into a card (with front
     * and back as defined by the contents of the front and back panels) and
     * submit it to the current deck. The card may or may not be accepted,
     * depending on whether the front is valid, and not a duplicate of another
     * front.
     */
    private fun submitCandidateCardToDeck() {
        // preconditions: none: this is a button-press-response function,
        // and should therefore always activate when the associated button
        // (in this case the OK button) is pressed.

        standardizeFields()
        val frontText = cardFrontPane.text
        val backText = cardBackPane.text

        manager.processProposedContents(frontText, backText, true, this)

        // postconditions: If adding succeeded, the front and back should
        // be blank again, if it didn't, they should be the same as they were
        // before (so nothing changed). Since the logic of the postcondition
        // would be as complex as the logic of the function itself, it's kind
        // of double and I skip it here.
    }

    internal fun init() {
        cancelButton.addActionListener { manager.endEditing() }
        okButton.addActionListener { submitCandidateCardToDeck() }

        // now add the buttons to the window
        val buttonPane = JPanel()
        buttonPane.add(cancelButton)
        buttonPane.add(okButton)

        // Now create a nice (or at least acceptable-looking) layout.
        val upperPanel = JSplitPane(JSplitPane.VERTICAL_SPLIT, JScrollPane(cardFrontPane), JScrollPane(cardBackPane))
        upperPanel.resizeWeight = 0.5
        layout = GridBagLayout()
        val frontConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            weighty = 1.0
            insets = Insets(0, 0, 5, 0)
            fill = GridBagConstraints.BOTH
        }
        add(upperPanel, frontConstraints)

        val buttonPaneConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 1
            weightx = 0.0
            weighty = 0.0
            insets = Insets(10, 10, 10, 10)
        }
        add(buttonPane, buttonPaneConstraints)

        // And finally set the general settings of the 'new card'-window.
        setSize(400, 400)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        isVisible = true
    }

    companion object {
        /**
         * Shows the NewCardWindow. Is necessary to accommodate the nullness checker,
         * which requires separation of the constructor and setting/manipulating its
         * fields (of course, warnings could be suppressed, but programming around it
         * seemed more elegant).
         */
        internal fun display(frontText: String, backText: String, manager: CardEditingManager): CardEditingWindow {
            val newCardWindow = CardEditingWindow(frontText, backText, manager)
            newCardWindow.init()
            return newCardWindow
        }
    }
}
