package eb.subwindow

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets

import eb.data.DeckManager
import eb.utilities.EMPTY_STRING
import eb.utilities.Utilities
import javax.swing.*

/**
 * ThreeSidedCardWindow allows the user to add a new card to the deck, with
 * a writing part (like kanji, like 明日), a pronounciation part (like kana, like あした
 * and a meaning part (like 'tomorrow'), which will result in 4 new cards being made
 *
 * It is managed by a CardEditingManager object, which checks the returned
 * contents and opens/closes it.
 *
 * @author Eric-Wubbo Lameijer
 */
class ThreeSidedCardWindow(manager: CardEditingManager) : GenericCardEditingWindow(manager) {

    private val cardTopPane = JTextPane()

    private val cardMiddlePane = JTextPane()

    private val cardBottomPane = JTextPane()

    // The button to cancel creating this card, and return to the calling window.

    override val cardPanes = listOf(cardTopPane, cardMiddlePane, cardBottomPane)

    init {
        // preconditions: none (we can assume the user clicked the appropriate
        // button, and even otherwise there is not a big problem)
        val operation = if (manager.inCardCreatingMode()) "add" else "edit"
        this.title = "${DeckManager.currentDeck().name}: $operation card"

        // Create the panel to edit the front of the card, and make enter
        // and tab transfer focus to the panel for editing the back of the card.
        // Escape should cancel the card-creating process and close the
        // NewCardWindow

        cardPanes.forEach {
            it.text = EMPTY_STRING
            it.addKeyListener(escapeKeyListener)
            it.addFocusListener(CleaningFocusListener())
            if (it != cardBottomPane) {
                Utilities.makeTabAndEnterTransferFocus(it)

            } else { // for the bottom pane
                Utilities.makeTabTransferFocus(it)
                it.addKeyListener(enterKeyListener)
            }
        }

        // we just want tab to cycle from the front to the back of the card,
        // and vice versa, and not hit the buttons
        cancelButton.isFocusable = false
        okButton.isFocusable = false

        // postconditions: none. The window exists and should henceforth handle
        // its own business using the appropriate GUI elements.
    }

    //Listens for a specific key and consumes it (and performs the appropriate action) when it is pressed


    /**
     * Converts the current contents of the NewCardWindow into a card (with front
     * and back as defined by the contents of the front and back panels) and
     * submit it to the current deck. The card may or may not be accepted,
     * depending on whether the front is valid, and not a duplicate of another
     * front.
     */
    override fun submitCandidateCardToDeck() {
        // preconditions: none: this is a button-press-response function,
        // and should therefore always activate when the associated button
        // (in this case the OK button) is pressed.

        standardizeFields()
        val writingText = cardTopPane.text
        val pronunciationText = cardMiddlePane.text
        val meaningText = cardBottomPane.text

        if (writingText != EMPTY_STRING) { // regular card, like 明日 / あした / tomorrow
            manager.processProposedContents("$writingText [m]", meaningText, false, this)
            manager.processProposedContents("$writingText [p]", pronunciationText, false, this)
        }
        val writingTextToBeAdded = if (writingText == "") "" else " ($writingText)"
        manager.processProposedContents(pronunciationText, "$meaningText$writingTextToBeAdded", false, this)
        manager.processProposedContents(meaningText, "$pronunciationText$writingTextToBeAdded", true, this)

        // postconditions: If adding succeeded, the front and back should
        // be blank again, if it didn't, they should be the same as they were
        // before (so nothing changed). Since the logic of the postcondition
        // would be as complex as the logic of the function itself, it's kind
        // of double and I skip it here.
    }

    internal fun init() {
        // now add the buttons to the window
        val buttonPane = JPanel(). apply {
            add(cancelButton)
            add(okButton)
        }

        // Now create a nice (or at least acceptable-looking) layout.
        val upperPanel = Box.createVerticalBox()
        cardPanes.forEach {
            upperPanel.add(Box.createVerticalStrut(10))
            upperPanel.add(it)
        }
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
        internal fun display(manager: CardEditingManager): ThreeSidedCardWindow {
            val newCardWindow = ThreeSidedCardWindow(manager)
            newCardWindow.init()
            return newCardWindow
        }
    }
}
