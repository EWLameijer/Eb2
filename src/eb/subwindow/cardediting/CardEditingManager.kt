package eb.subwindow.cardediting

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
        !in c_cardsBeingEdited -> CardEditingWindow.display(card!!.front.contents, card!!.back, this, false)
        else -> null
    }

    private fun currentFront() =
        if (card == null) EMPTY_STRING
        else card!!.front.contents

    init {
        BlackBoard.register(this, UpdateType.DECK_SWAPPED)
    }

    private fun closeOptionPane() = JOptionPane.getRootFrame().dispose()

    fun inCardCreatingMode() = card == null

    fun processProposedContents(
        frontText: String,
        backText: String,
        shouldClearCardWindow: Boolean,
        callingWindow: GenericCardEditingWindow
    ) {
        // Case 1 of 3: there are empty fields. Or at least: the front is empty.
        if (!Hint.isValid(frontText)) {
            handleEmptyFront(backText, callingWindow)
        } else {
            // front text is not empty. Now, this can either be problematic or not.
            // Case 2 of 3: the front of the card is new or the front is the same as the old front (when editing).
            //  Add the card and be done with it (well, when adding cards one should not close the new card window).
            val frontHint = Hint(frontText)
            val currentCardWithThisFront = DeckManager.currentDeck().cardCollection.getCardWithFront(frontHint)
            if (frontText == currentFront() || currentCardWithThisFront == null) {
                submitCardContents(frontHint, backText, shouldClearCardWindow, callingWindow)
            } else {
                // Case 3 of 3: there is a current (but different) card with the same front. Resolve this conflict.
                handleCardBeingDuplicate(frontHint, backText, currentCardWithThisFront, callingWindow)
                if (shouldClearCardWindow) callingWindow.clearContents()
            }
        }
    }

    private fun handleEmptyFront(backText: String, callingWindow: GenericCardEditingWindow) {
        // if back is empty, then this is just a hasty return. Is okay.
        if (backText.isEmpty()) {
            endEditing(callingWindow)
        } else {
            // back is filled: so there is an error
            val verb = if (inCardCreatingMode()) "add" else "modify"
            JOptionPane.showMessageDialog(
                null,
                "Cannot $verb card: the front of a card cannot be blank.",
                "Cannot $verb card", JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun handleCardBeingDuplicate(
        frontText: Hint,
        backText: String,
        duplicate: Card,
        callingWindow: GenericCardEditingWindow
    ) {
        val cardCopiedFromSideList = callingWindow.copiedCard
        if (cardCopiedFromSideList != null && cardCopiedFromSideList.front == frontText) {
            // while I first made a menu here, it is more convenient to assume, like with normal editing,
            // that the user simply wants to change the back of the card.
            deleteOtherCard(duplicate, frontText, backText, callingWindow)
        } else {
            showDuplicateFrontPopup(duplicate, backText, frontText, callingWindow)
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
            if (callingWindow !is ThreeSidedCardWindow) cardEditingWindow!!.clearContents()
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
        if (inCardCreatingMode()) {
            val candidateCard = Card(frontText, backText)
            DeckManager.currentDeck().cardCollection.addCard(candidateCard)
            if (shouldClearCardWindow) callingWindow.clearContents()
            callingWindow.focusFront()
        } else {
            // in editing mode
            card!!.front = frontText
            card!!.back = backText
            endEditing(callingWindow)
        }
        BlackBoard.post(Update(UpdateType.CARD_CHANGED))
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
}
