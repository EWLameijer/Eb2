package eb.subwindow

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

import eb.data.DeckManager
import eb.utilities.Utilities
import eb.utilities.doNothing
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
open class CardEditingWindow(frontText: String, backText: String, private val m_manager: CardEditingManager) : JFrame() {

    // Allows the creation/editing of the content on the front of the card.
    private val cardFrontPane: JTextPane

    // Allows the creation/editing of the contents of the back of the card.
    private val cardBackPane: JTextPane

    // The button to cancel creating this card, and return to the calling window.
    private val cancelButton: JButton

    // The button to press that requests the current deck to check whether this
    // card is a valid/usable card (so no duplicate of an existing card, for
    // example) and if so, to add it.
    private val okButton: JButton

    init {
        // preconditions: none (we can assume the user clicked the appropriate
        // button, and even otherwise there is not a big problem)
        val operation = if (m_manager.inCardCreatingMode()) "add" else "edit"
        this.title = "${DeckManager.currentDeck().name}: $operation card"
        cancelButton = JButton("Cancel")
        okButton = JButton("Ok")

        // Create the panel to edit the front of the card, and make enter
        // and tab transfer focus to the panel for editing the back of the card.
        // Escape should cancel the card-creating process and close the
        // NewCardWindow
        cardFrontPane = JTextPane()
        cardFrontPane.text = frontText
        Utilities.makeTabAndEnterTransferFocus(cardFrontPane)
        val escapeKeyListener = SpecificKeyListener(KeyEvent.VK_ESCAPE) { cancelButton.doClick() }
        val enterKeyListener = SpecificKeyListener(KeyEvent.VK_ENTER) { okButton.doClick() }

        cardFrontPane.addKeyListener(escapeKeyListener)
        cardFrontPane.addFocusListener(CleaningFocusListener())

        // Now create the panel to edit the back of the card; make tab transfer
        // focus to the front (for editing the front again), escape should (like
        // for the front panel) again cancel editing and close the NewCardWindow.
        // Pressing the Enter key, however, should try save the card instead of
        // transferring the focus back to the front-TextArea.
        cardBackPane = JTextPane()
        cardBackPane.text = backText

        Utilities.makeTabTransferFocus(cardBackPane)
        cardBackPane.addKeyListener(enterKeyListener)
        cardBackPane.addKeyListener(escapeKeyListener)
        cardBackPane.addFocusListener(CleaningFocusListener())

        // we just want tab to cycle from the front to the back of the card,
        // and vice versa, and not hit the buttons
        cancelButton.isFocusable = false
        okButton.isFocusable = false

        // postconditions: none. The window exists and should henceforth handle
        // its own business using the appropriate GUI elements.
    }

    //Listens for a specific key and consumes it (and performs the appropriate action) when it is pressed
    internal inner class SpecificKeyListener(private val keyCode: Int, val action: () -> Unit) : KeyListener {

        /**
         * If the user presses the escape key, dispose of the candidate card and
         * close the 'New Card' window (same as if the user clicks the Cancel
         * button).
         */
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

        override fun focusLost(arg0: FocusEvent) =  trimFields()
    }

    fun trimFields() {
        cardFrontPane.text = cardFrontPane.text.trim { it <= ' ' }
        cardBackPane.text = cardBackPane.text.trim { it <= ' ' }
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

        trimFields()
        val frontText = cardFrontPane.text
        val backText = cardBackPane.text

        m_manager.processProposedContents(frontText, backText)

        // postconditions: If adding succeeded, the front and back should
        // be blank again, if it didn't, they should be the same as they were
        // before (so nothing changed). Since the logic of the postcondition
        // would be as complex as the logic of the function itself, it's kind
        // of double and I skip it here.
    }

    /**
     * Initializes the components of the NewCardWindow.
     */
    internal open fun init() {
        cancelButton.addActionListener { m_manager.endEditing() }
        okButton.addActionListener { submitCandidateCardToDeck() }

        // now add the buttons to the window
        val buttonPane = JPanel()
        buttonPane.add(cancelButton)
        buttonPane.add(okButton)

        // Now create a nice (or at least acceptable-looking) layout.
        val upperPanel = JSplitPane(JSplitPane.VERTICAL_SPLIT,
                JScrollPane(cardFrontPane), JScrollPane(cardBackPane))
        upperPanel.resizeWeight = 0.5
        layout = GridBagLayout()
        val frontConstraints = GridBagConstraints()
        frontConstraints.gridx = 0
        frontConstraints.gridy = 0
        frontConstraints.weightx = 1.0
        frontConstraints.weighty = 1.0
        frontConstraints.insets = Insets(0, 0, 5, 0)
        frontConstraints.fill = GridBagConstraints.BOTH
        add(upperPanel, frontConstraints)

        val buttonPaneConstraints = GridBagConstraints()
        buttonPaneConstraints.gridx = 0
        buttonPaneConstraints.gridy = 1
        buttonPaneConstraints.weightx = 0.0
        buttonPaneConstraints.weighty = 0.0
        buttonPaneConstraints.insets = Insets(10, 10, 10, 10)
        add(buttonPane, buttonPaneConstraints)

        // And finally set the general settings of the 'new card'-window.
        setSize(400, 400)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        isVisible = true
    }

    fun updateContents(frontText: String, backText: String) {
        cardFrontPane.text = frontText
        cardBackPane.text = backText
    }

    fun focusFront() = cardFrontPane.requestFocusInWindow()

    companion object {
        // Default serialization ID (not used).
        private const val serialVersionUID = 3419171802910744055L

        /**
         * Shows the NewCardWindow. Is necessary to accommodate the nullness checker,
         * which requires separation of the constructor and setting/manipulating its
         * fields (of course, warnings could be suppressed, but programming around it
         * seemed more elegant).
         */
        internal fun display(frontText: String, backText: String,
                             manager: CardEditingManager): CardEditingWindow {
            val newCardWindow = CardEditingWindow(frontText,
                    backText, manager)
            newCardWindow.init()
            return newCardWindow
        }
    }
}
