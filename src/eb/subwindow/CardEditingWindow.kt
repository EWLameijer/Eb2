package eb.subwindow

import javax.swing.*
import javax.swing.event.ListSelectionEvent

import eb.data.DeckManager
import eb.eventhandling.DelegatingDocumentListener
import eb.utilities.Hint
import eb.utilities.Utilities
import java.awt.*


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
        efficientCardTextUpdate("", "")
    }

    private fun updateSideList() {
        val allCardTexts = DeckManager.currentDeck().getCardTexts()
        val allRelevantCardTexts =
            allCardTexts.filter { it.first.startsWith(cardFrontPane.text) && it.second.contains(cardBackPane.text) }
        // not using cardFronts.addAll since it does not work on JVM < 9
        val newCardFronts = DefaultListModel<String>()
        allRelevantCardTexts.map { it.first }.sorted().forEach {
            newCardFronts.addElement(it)
        }
        listBox.model = newCardFronts
        val existingCardWithThisFront = DeckManager.currentDeck().cardCollection.getCardWithFront(cardFrontPane.text)
        deleteButton.isEnabled = existingCardWithThisFront != null
    }

    // Create the panel to edit the front of the card, and make enter
    // and tab transfer focus to the panel for editing the back of the card.
    // Escape should cancel the card-creating process and close the
    // NewCardWindow
    private val cardFrontPane = JTextPane().apply {
        text = frontText
        document.addDocumentListener(cardTextListener)
        Utilities.makeTabAndEnterTransferFocus(this)
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
        addFocusListener(CleaningFocusListener())
    }

    override val cardPanes = listOf(cardFrontPane, cardBackPane)

    init {
        val operation = if (manager.inCardCreatingMode()) "add" else "edit"
        this.title = "${DeckManager.currentDeck().name}: $operation card"
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
        weightx = 0.5
        weighty = 1.0
        insets = Insets(0, 0, 5, 0)
        fill = GridBagConstraints.BOTH
    }

    private fun addListPanel() {
        listBox.addListSelectionListener(::copyCardFromList)
        updateSideList()
        val listPanel = JPanel().apply {
            add(listBox)
            minimumSize = Dimension(150, 200)
        }
        add(JScrollPane(listPanel), listPanelConstraints)
    }

    private fun copyCardFromList(e: ListSelectionEvent) {
        val list = e.source as JList<*>
        val newFrontText = list.selectedValue as String?
        if (newFrontText != null) {
            copiedCard = DeckManager.currentDeck().cardCollection.getCardWithFront(Hint(newFrontText))!!
            efficientCardTextUpdate(newFrontText, copiedCard!!.back)

        }
    }

    private fun efficientCardTextUpdate(newFrontText: String, newBackText: String) {
        cardFrontPane.document.removeDocumentListener(cardTextListener)
        cardBackPane.document.removeDocumentListener(cardTextListener)
        cardFrontPane.text = newFrontText
        cardBackPane.text = newBackText
        updateSideList()
        cardFrontPane.document.addDocumentListener(cardTextListener)
        cardBackPane.document.addDocumentListener(cardTextListener)
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
