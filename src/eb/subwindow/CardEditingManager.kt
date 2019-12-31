package eb.subwindow

import java.util.HashSet

import javax.swing.JButton
import javax.swing.JOptionPane

import eb.data.Card
import eb.data.DeckManager
import eb.eventhandling.BlackBoard
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
class CardEditingManager(private val tripleModus: Boolean = false, private var card: Card? = null) {

    private val cardEditingWindow: GenericCardEditingWindow? = when (card) {
        null -> if (tripleModus) ThreeSidedCardWindow.display(this) else CardEditingWindow.display("", "", this)
        !in c_cardsBeingEdited -> CardEditingWindow.display(card!!.front.contents, card!!.back, this)
        else -> null
    }

    private fun currentFront() =
            if (card == null) EMPTY_STRING
            else card!!.front.contents

    private fun closeOptionPane() = JOptionPane.getRootFrame().dispose()

    fun inCardCreatingMode() = card == null

    fun processProposedContents(frontText: String, backText: String, shouldClearCardWindow: Boolean, callingWindow: GenericCardEditingWindow) {
        // Case 1 of 3: there are empty fields. Or at least: the front is empty.
        // Investigate the exact problem.
        if (!Hint.isValid(frontText)) {
            // if back is empty, then this is just a hasty return. Is okay.
            if (backText.isEmpty()) {
                endEditing()
            } else {
                // back is filled: so there is an error
                val verb = if (inCardCreatingMode()) "add" else "modify"
                JOptionPane.showMessageDialog(null,
                        "Cannot $verb card: the front of a card cannot be blank.",
                        "Cannot $verb card", JOptionPane.ERROR_MESSAGE)
            }
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

    private fun handleCardBeingDuplicate(frontText: Hint, backText: String, duplicate: Card, callingWindow: GenericCardEditingWindow) {
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
        JOptionPane.showOptionDialog(null,
                "A card with the front '$frontText' already exists; on the back is the text '${duplicate.back}', replace with '$backText'?",
                "A card with this front already exists. What do you want to do?", 0,
                JOptionPane.QUESTION_MESSAGE, null, buttons, null)
    }

    private fun deleteOtherCard(duplicate: Card, frontText: Hint, backText: String, callingWindow: GenericCardEditingWindow) {
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
            endEditing()
        }
    }

    private fun mergeBacks(duplicate: Card, backText: String, frontText: Hint) {
        val otherBack = duplicate.back
        val newBack = "$otherBack; $backText"
        closeOptionPane()
        if (!tripleModus) {
            cardEditingWindow!!.updateContents(frontText.contents, newBack)
        } else {
            CardEditingWindow.display(frontText.contents, newBack, this)
        }
        DeckManager.currentDeck().cardCollection.removeCard(duplicate)
    }

    // Submits these contents to the deck, and closes the editing window if appropriate.
    private fun submitCardContents(frontText: Hint, backText: String, shouldClearCardWindow: Boolean, callingWindow: GenericCardEditingWindow) {
        if (inCardCreatingMode()) {
            val candidateCard = Card(frontText, backText)
            DeckManager.currentDeck().cardCollection.addCard(candidateCard)
            if (shouldClearCardWindow) callingWindow.clearContents()
            callingWindow.focusFront()
        } else {
            // in editing mode
            card!!.front = frontText
            card!!.back = backText
            endEditing()
        }
        BlackBoard.post(Update(UpdateType.CARD_CHANGED))
    }

    fun endEditing() {
        if (!inCardCreatingMode()) {
            c_cardsBeingEdited.remove(card)
        }
        cardEditingWindow!!.dispose()
    }

    companion object {
        // prevent a card from being edited in two windows at the same time.
        private val c_cardsBeingEdited = HashSet<Card>()
    }
}
