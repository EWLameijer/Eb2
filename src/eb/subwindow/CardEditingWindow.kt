package eb.subwindow

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.ListSelectionEvent

import eb.data.DeckManager
import eb.eventhandling.DelegatingDocumentListener
import eb.utilities.Hint
import eb.utilities.Utilities


/**
 * CardEditingWindow allows the user to add a new card to the deck, or to edit
 * an existing card.
 *
 * It is managed by a CardEditingManager object, which checks the returned
 * contents and opens/closes it.
 *
 * @author Eric-Wubbo Lameijer
 */
class CardEditingWindow(
    frontText: String,
    backText: String,
    manager: CardEditingManager,
    private val autokill: Boolean
) : GenericCardEditingWindow(manager) {

    private val cardTextListener = DelegatingDocumentListener {
        val sideListUpdate = Runnable {
            updateSideList()
        }
        SwingUtilities.invokeLater(sideListUpdate)
    }

    override fun clear() {
        cardFrontPane.text = ""
        cardBackPane.text = ""
    }

    private fun updateSideList() {
        cardFronts.clear()
        val allCardTexts = DeckManager.currentDeck().getCardTexts()
        val allRelevantCardTexts =
            allCardTexts.filter { it.first.startsWith(cardFrontPane.text) && it.second.contains(cardBackPane.text) }
        cardFronts.addAll(allRelevantCardTexts.map { it.first }.sorted())
    }

    // Create the panel to edit the front of the card, and make enter
    // and tab transfer focus to the panel for editing the back of the card.
    // Escape should cancel the card-creating process and close the
    // NewCardWindow
    private val cardFrontPane = JTextPane().apply {
        text = frontText
        document.addDocumentListener(cardTextListener)
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
        document.addDocumentListener(cardTextListener)
        Utilities.makeTabTransferFocus(this)
        addKeyListener(enterKeyListener)
        addKeyListener(escapeKeyListener)
        addFocusListener(CleaningFocusListener())
    }

    override val cardPanes = listOf(cardFrontPane, cardBackPane)

    init {
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
    override fun submitCandidateCardToDeck() {
        standardizeFields()
        val frontText = cardFrontPane.text
        val backText = cardBackPane.text

        manager.processProposedContents(frontText, backText, true, this)
        if (autokill) this.dispose()
    }

    internal fun init() {
        // now add the buttons to the window
        // Now create a nice (or at least acceptable-looking) layout.
        addCardPanel()
        addListPanel()
        addButtonPanel()

        // And finally set the general settings of the 'new card'-window.
        setSize(600, 400)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        isVisible = true
    }

    private fun addCardPanel() {
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
    }

    private val listPanelConstraints = GridBagConstraints().apply {
        gridx = 1
        gridy = 0
        weightx = 0.1
        weighty = 1.0
        insets = Insets(0, 0, 5, 0)
        fill = GridBagConstraints.BOTH
    }

    private fun addListPanel() {
        listBox.addListSelectionListener(::copyCardFromList)
        updateSideList()
        val listPanel = JPanel().apply {
            add(listBox)
            minimumSize = Dimension(100, 200)
        }
        add(listPanel, listPanelConstraints)
    }

    private fun copyCardFromList(e: ListSelectionEvent) {
        val list = e.source as JList<*>
        val newFrontText = list.selectedValue as String?
        if (newFrontText != null) {
            copiedCard = DeckManager.currentDeck().cardCollection.getCardWithFront(Hint(newFrontText))!!
            cardFrontPane.text = newFrontText
            cardBackPane.text = copiedCard!!.back
        }
    }

    companion object {
        /**
         * Shows the NewCardWindow. Is necessary to accommodate the nullness checker,
         * which requires separation of the constructor and setting/manipulating its
         * fields (of course, warnings could be suppressed, but programming around it
         * seemed more elegant).
         */
        internal fun display(
            frontText: String,
            backText: String,
            manager: CardEditingManager,
            autokill: Boolean
        ): CardEditingWindow {
            val newCardWindow = CardEditingWindow(frontText, backText, manager, autokill)
            newCardWindow.init()
            return newCardWindow
        }
    }
}
