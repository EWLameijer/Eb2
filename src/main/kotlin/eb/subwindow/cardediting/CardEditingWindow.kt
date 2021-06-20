package eb.subwindow.cardediting

import eb.data.DeckManager
import eb.eventhandling.DelegatingDocumentListener
import eb.utilities.Hint
import eb.utilities.Utilities
import eb.utilities.italicizeIf
import java.awt.*
import javax.swing.*
import javax.swing.event.ListSelectionEvent


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

    private val noCardSelectedPanelId = "No card selected"
    private val cardSelectedPanelId = "Card selected"
    private val reviewDataArea = JTextArea("undefined")
    private val listPanel = JPanel().apply {
        layout = CardLayout()
        add(listBox, noCardSelectedPanelId)
        add(reviewDataArea, cardSelectedPanelId)
    }

    private val cardTextListener = DelegatingDocumentListener {
        val sideListUpdate = Runnable {
            updateSideList()
        }
        SwingUtilities.invokeLater(sideListUpdate)
    }

    override fun clear() {
        efficientCardTextUpdate("", "", false)
        manager.setEditedCard(null)

        showSideList(noCardSelectedPanelId)
    }

    private fun showSideList(sidePanelId: String) {
        val cardLayout = listPanel.layout as CardLayout
        cardLayout.show(listPanel, sidePanelId)
    }

    private fun updateSideList() {
        val allCardTexts = DeckManager.getAllCardTexts()
        val mainDeckName = DeckManager.currentDeck().name
        val allRelevantCardTexts =
            allCardTexts.filter { it.front.startsWith(cardFrontPane.text) && it.back.contains(cardBackPane.text) }
        // not using cardFronts.addAll since it does not work on JVM < 9
        val newCardFronts = DefaultListModel<String>()
        allRelevantCardTexts.sortedBy { it.front }.forEach {
            val cardFront = it.front
            val displayedText = cardFront.italicizeIf(it.deckName != mainDeckName)
            newCardFronts.addElement(displayedText)
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
        showSideList(noCardSelectedPanelId)
        if (autokill) this.dispose()
    }

    internal fun init() {
        // now add the buttons to the window
        // Now create a nice (or at least acceptable-looking) layout.
        addCardPanel()
        addListPanel()
        addButtonPanel()

        // And finally set the general settings of the 'new card'-window.
        setSize(650, 400)
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
        weightx = 0.4
        weighty = 1.0
        insets = Insets(0, 0, 5, 0)
        fill = GridBagConstraints.BOTH
    }

    private fun addListPanel() {
        listBox.addListSelectionListener(::copyCardFromList)
        updateSideList()
        val scrollPane = JScrollPane(listPanel).apply {
            minimumSize = Dimension(175, 200)
        }
        add(scrollPane, listPanelConstraints)
    }

    private fun copyCardFromList(e: ListSelectionEvent) {
        val list = e.source as JList<*>
        val newFrontText = list.selectedValue as String?
        if (newFrontText != null) {
            // Okay. Need to get the basic card contents: front is not needed, but back and deck are.
            val cleanedFrontText = newFrontText.stripHtmlItalic()
            val copiedCardBase = DeckManager.getBaseCard(cleanedFrontText)
            if (copiedCardBase!!.deckName == DeckManager.currentDeck().name) {
                val copiedCard = DeckManager.currentDeck().cardCollection.getCardWithFront(Hint(cleanedFrontText))!!
                manager.setEditedCard(copiedCard)
            }
            efficientCardTextUpdate(
                cleanedFrontText,
                copiedCardBase.back,
                copiedCardBase.deckName != DeckManager.currentDeck().name
            )
            val (originalCard, deckName) = DeckManager.getCardWithDeckName(cleanedFrontText)!!
            val deckInfo = if (deckName != DeckManager.currentDeck().name) "Deck: $deckName\n" else ""
            reviewDataArea.text = deckInfo + originalCard.reviewHistoryText()
            showSideList(cardSelectedPanelId)
        }
    }

    private fun efficientCardTextUpdate(newFrontText: String, newBackText: String, italicMode: Boolean) {
        cardFrontPane.document.removeDocumentListener(cardTextListener)
        cardBackPane.document.removeDocumentListener(cardTextListener)

        cardFrontPane.italicizeFontIf(italicMode)
        cardBackPane.italicizeFontIf(italicMode)

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

private fun String.stripHtmlItalic(): String = removePrefix("<html><i>").removeSuffix("</i></html>")

private fun JTextPane.italicizeFontIf(italicMode: Boolean) {
    val targetFont = if (italicMode) Font.ITALIC else Font.PLAIN
    font = Font(font.name, targetFont, font.size)
}
