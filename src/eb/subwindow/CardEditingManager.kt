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
class CardEditingManager (private var cardToBeModified : Card? = null) {
    init {
        if (cardToBeModified == null) activateCardCreationWindow()
        else activateCardEditingWindow(cardToBeModified!!)
    }

    private var cardEditingWindow: CardEditingWindow? = null

    private fun currentFront() =
            if (cardToBeModified == null) EMPTY_STRING
            else cardToBeModified!!.front.contents

    private fun closeOptionPane() = JOptionPane.getRootFrame().dispose()

    fun inCardCreatingMode() = cardToBeModified == null

    /**
     * Shows the card editing window; however, has a guard that prevents the same
     * card from being edited in two different windows.
     *
     * @param card
     * the card to be edited.
     */
    private fun activateCardEditingWindow(card: Card) {
        if (card !in c_cardsBeingEdited) {
            cardEditingWindow = CardEditingWindow.display(card.front.contents, card.back, this)
            c_cardsBeingEdited.add(card)
        }
    }

    private fun activateCardCreationWindow() {
        cardEditingWindow = CardEditingWindow.display("", "", this)
    }

    fun processProposedContents(frontText: String, backText: String) {
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

            // Case 2 of 3: the front of the card is new or the front is the same
            // as the old front (when editing). Add the card and be done with it.
            // (well, when adding cards one should not close the new card window)
            val frontHint = Hint(frontText)
            val currentCardWithThisFront = DeckManager.currentDeck().cardCollection.getCardWithFront(frontHint)
            if (frontText == currentFront() || currentCardWithThisFront == null) {
                submitCardContents(frontHint, backText)
            } else {

                // Case 3 of 3: there is a current (but different) card with this exact
                // same front. Resolve this conflict.
                handleCardBeingDuplicate(frontHint, backText, currentCardWithThisFront)
            }
        }
    }

    private fun handleCardBeingDuplicate(frontText: Hint, backText: String, duplicate: Card) {

        val reeditButton = JButton("Re-edit card")
        reeditButton.addActionListener { closeOptionPane() }

        val mergeButton = JButton("Merge backs of cards")
        mergeButton.addActionListener {
            val otherBack = duplicate.back
            val newBack = "$otherBack; $backText"
            closeOptionPane()
            cardEditingWindow!!.updateContents(frontText.contents, newBack)
            DeckManager.currentDeck().cardCollection.removeCard(duplicate)
        }
        val deleteThisButton = JButton("Delete this card")
        deleteThisButton.addActionListener {
            closeOptionPane()
            if (inCardCreatingMode()) {
                cardEditingWindow!!.updateContents("", "")
            } else {
                DeckManager.currentDeck().cardCollection.removeCard(cardToBeModified!!)
                endEditing()
            }
        }
        val deleteOtherButton = JButton("Delete the other card")
        deleteOtherButton.addActionListener {
            DeckManager.currentDeck().cardCollection.removeCard(duplicate)
            closeOptionPane()
            submitCardContents(frontText, backText)
        }
        val buttons = arrayOf<Any>(reeditButton, mergeButton, deleteThisButton, deleteOtherButton)
        JOptionPane.showOptionDialog(null,
                "A card with this front already exists; on the back is the text '"
                        + duplicate.back + "'",
                "A card with this front already exists. What do you want to do?", 0,
                JOptionPane.QUESTION_MESSAGE, null, buttons, null)
    }

    /**
     * Submits these contents to the deck, and closes the editing window if
     * appropriate.
     *
     * @param frontText
     * @param backText
     */
    private fun submitCardContents(frontText: Hint, backText: String) {
        if (inCardCreatingMode()) {
            val candidateCard = Card(frontText, backText)
            DeckManager.currentDeck().cardCollection.addCard(candidateCard)
            cardEditingWindow!!.updateContents("", "")
            cardEditingWindow!!.focusFront()
        } else {
            // in editing mode
            cardToBeModified!!.front = frontText
            cardToBeModified!!.back = backText
            endEditing()
        }
        BlackBoard.post(Update(UpdateType.CARD_CHANGED))
    }

    fun endEditing() {
        if (!inCardCreatingMode()) {
            c_cardsBeingEdited.remove(cardToBeModified)
        }
        cardEditingWindow!!.dispose()
    }

    companion object {

        // prevent a card from being edited in two windows at the same time.
        private val c_cardsBeingEdited = HashSet<Card>()
    }
}
