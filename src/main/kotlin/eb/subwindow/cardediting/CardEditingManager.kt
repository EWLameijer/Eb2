package eb.subwindow.cardediting

import eb.data.BaseCardData
import java.util.HashSet

import javax.swing.JButton
import javax.swing.JOptionPane

import eb.data.Card
import eb.data.DeckManager
import eb.eventhandling.BlackBoard
import eb.eventhandling.Listener
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.utilities.EMPTY_STRING
import eb.utilities.Hint

/**
 * CardEditingManager coordinates the flow of information from the window that requests a card to be created/edited
 * to the UI-element that actually does the editing.
 *
 * @author Eric-Wubbo Lameijer
 */
class CardEditingManager(private val tripleModus: Boolean = false, private var card: Card? = null) : Listener {

    private val cardEditingWindow: GenericCardEditingWindow? = when (card) {
        null -> if (tripleModus) ThreeSidedCardWindow.display(this) else CardEditingWindow.display("", "", this, false)
        !in c_cardsBeingEdited -> CardEditingWindow.display(card!!.front.contents, card!!.back, this, true)
        else -> null
    }

    private fun currentFront() =
        if (card == null) EMPTY_STRING
        else card!!.front.contents

    private fun currentBack() =
        if (card == null) EMPTY_STRING
        else card!!.back

    init {
        BlackBoard.register(this, UpdateType.DECK_SWAPPED)
    }

    private fun closeOptionPane() = JOptionPane.getRootFrame().dispose()

    fun inCardCreatingMode() = card == null
    private fun inCardEditingMode() = !inCardCreatingMode()

    fun processProposedContents(
        frontText: String,
        backText: String,
        shouldClearCardWindow: Boolean,
        callingWindow: GenericCardEditingWindow
    ) {
        // Case 1 of 3: the front is empty; this won't be a valid card.
        if (!Hint.isValid(frontText)) {
            handleEmptyFront(backText, callingWindow)
        } else if (isIdenticalToCurrentCard(frontText, backText)) { // unchanged card
            if (shouldClearCardWindow) callingWindow.clear()
        } else {
            val frontHint = Hint(frontText)
            val baseCardWithThisFront = DeckManager.getBaseCard(frontText)
            if (baseCardWithThisFront == null) {
                handlePossibleFrontReplacement(frontHint, backText, shouldClearCardWindow, callingWindow)
            } else {
                // Case 3 of 3: there is a current (but different) card with the same front. Resolve this conflict.
                handleCardBeingDuplicate(frontHint, backText, baseCardWithThisFront, callingWindow)
                if (shouldClearCardWindow) callingWindow.clear()
            }
        }
    }

    private fun handlePossibleFrontReplacement(
        frontHint: Hint,
        backText: String,
        shouldClearCardWindow: Boolean,
        callingWindow: GenericCardEditingWindow
    ) {
        if (inCardEditingMode()) { // are you trying to replace the card/front?
            val originalFront = card!!.front.contents
            val originalBack = card!!.back
            val newFront = frontHint.contents
            val buttons = getFrontChangeButtons(frontHint, backText, shouldClearCardWindow, callingWindow)
            JOptionPane.showOptionDialog(
                null,
                """Replace the card
                           '$originalFront' / '$originalBack' with
                           '$newFront' / '$backText'?""",
                "Are you sure you want to update the current card?", 0,
                JOptionPane.QUESTION_MESSAGE, null, buttons, null
            )
        } else { // in card creation mode
            submitCardContents(frontHint, backText, shouldClearCardWindow, callingWindow)
        }
    }

    private fun getFrontChangeButtons(
        frontHint: Hint,
        backText: String,
        shouldClearCardWindow: Boolean,
        callingWindow: GenericCardEditingWindow
    ): Array<JButton> {
        val replaceButton = JButton("Replace card").apply {
            addActionListener {
                DeckManager.currentDeck().cardCollection.removeCard(card!!)
                submitCardContents(frontHint, backText, shouldClearCardWindow, callingWindow)
                closeOptionPane()
            }
        }
        val keepBothButton = JButton("Keep both cards").apply {
            addActionListener {
                submitCardContents(frontHint, backText, shouldClearCardWindow, callingWindow)
                closeOptionPane()
            }
        }
        val cancelCardSubmissionButton = JButton("Cancel this submission").apply {
            addActionListener {
                closeOptionPane()
            }
        }
        return arrayOf(replaceButton, keepBothButton, cancelCardSubmissionButton)
    }

    private fun isIdenticalToCurrentCard(frontText: String, backText: String) =
        frontText == currentFront() && backText == currentBack()

    private fun handleEmptyFront(backText: String, callingWindow: GenericCardEditingWindow) {
        // if back is empty, then this is just a hasty return. Is okay.
        if (backText.isEmpty()) {
            endEditing(callingWindow)
        } else {
            // back is filled: so there is an error
            val verb = getVerb()
            JOptionPane.showMessageDialog(
                null,
                "Cannot $verb card: the front of a card cannot be blank.",
                "Cannot $verb card", JOptionPane.ERROR_MESSAGE
            )
        }
    }

    fun getVerb() = if (inCardCreatingMode()) "add" else "edit"

    private fun handleCardBeingDuplicate(
        frontText: Hint,
        backText: String,
        duplicateBaseCard: BaseCardData,
        callingWindow: GenericCardEditingWindow
    ) {
        if (duplicateBaseCard.deckName != DeckManager.currentDeck().name) {
            val verb = getVerb()
            JOptionPane.showMessageDialog(
                null,
                "Cannot $verb card: the card is already present in a linked deck.",
                "Cannot $verb card", JOptionPane.ERROR_MESSAGE
            )
        } else {
            val duplicate = DeckManager.currentDeck().cardCollection.getCardWithFront(frontText)!!
            if (card != null && card!!.front == frontText) {
                // while I first made a menu here, it is more convenient to assume, like with normal editing,
                // that the user simply wants to change the back of the card.
                DeckManager.currentDeck().cardCollection.removeCard(duplicate)
                submitCardContents(frontText, backText, true, callingWindow)
            } else {
                showDuplicateFrontPopup(duplicate, backText, frontText, callingWindow)
            }
        }
    }

    private fun showDuplicateFrontPopup(
        duplicate: Card,
        backText: String,
        frontText: Hint,
        callingWindow: GenericCardEditingWindow
    ) {
        val reeditButton = JButton("Re-edit card").apply {
            addActionListener { closeOptionPane() }
        }
        val mergeButton = JButton("Merge backs of cards").apply {
            addActionListener { mergeBacks(duplicate, backText, frontText) }
        }
        val deleteThisButton = JButton("Delete this card").apply {
            addActionListener { deleteCurrentCard(callingWindow) }
        }
        val deleteOtherButton = JButton("Delete the other card").apply {
            addActionListener { deleteOtherCard(duplicate, frontText, backText, callingWindow) }
        }
        val buttons = arrayOf(reeditButton, mergeButton, deleteThisButton, deleteOtherButton)
        JOptionPane.showOptionDialog(
            null,
            "A card with the front '$frontText' already exists; on the back is the text\n'${duplicate.back}'\nreplace with\n'$backText'?",
            "A card with this front already exists. What do you want to do?", 0,
            JOptionPane.QUESTION_MESSAGE, null, buttons, null
        )
    }

    private fun deleteOtherCard(
        duplicate: Card,
        frontText: Hint,
        backText: String,
        callingWindow: GenericCardEditingWindow
    ) {
        DeckManager.currentDeck().cardCollection.removeCard(duplicate)
        closeOptionPane()
        submitCardContents(frontText, backText, true, callingWindow)
    }

    private fun deleteCurrentCard(callingWindow: GenericCardEditingWindow) {
        closeOptionPane()
        if (inCardCreatingMode()) {
            if (callingWindow !is ThreeSidedCardWindow) cardEditingWindow!!.clear()
        } else {
            DeckManager.currentDeck().cardCollection.removeCard(card!!)
            endEditing(callingWindow)
        }
    }

    private fun mergeBacks(duplicate: Card, backText: String, frontText: Hint) {
        val otherBack = duplicate.back
        val newBack = "$otherBack; $backText"
        closeOptionPane()
        if (!tripleModus) {
            cardEditingWindow!!.updateContents(frontText.contents, newBack)
        } else { // Create a new normal card window
            CardEditingWindow.display(frontText.contents, newBack, this, true)
        }
        DeckManager.currentDeck().cardCollection.removeCard(duplicate)
    }

    // Submits these contents to the deck, and closes the editing window if appropriate.
    private fun submitCardContents(
        frontText: Hint,
        backText: String,
        shouldClearCardWindow: Boolean,
        callingWindow: GenericCardEditingWindow
    ) {
        val candidateCard = Card(frontText, backText)
        DeckManager.currentDeck().cardCollection.addCard(candidateCard)
        if (shouldClearCardWindow) callingWindow.clear()
        callingWindow.focusFront()
        BlackBoard.post(Update(UpdateType.DECK_CHANGED))
    }

    fun endEditing(window: GenericCardEditingWindow) {
        if (!inCardCreatingMode()) {
            c_cardsBeingEdited.remove(card)
        }
        window.dispose()
    }

    companion object {
        // prevent a card from being edited in two windows at the same time.
        private val c_cardsBeingEdited = HashSet<Card>()
    }

    override fun respondToUpdate(update: Update) {
        if (update.type == UpdateType.DECK_SWAPPED && cardEditingWindow != null) {
            endEditing(cardEditingWindow)
        }
    }

    // so side list selection can update the card to be edited
    fun setEditedCard(selectedCard: Card?) {
        card = selectedCard
        cardEditingWindow?.updateTitle()
    }
}
