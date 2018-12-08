package eb.subwindow

import java.util.HashSet
import java.util.Optional

import javax.swing.JButton
import javax.swing.JOptionPane

import eb.data.Card
import eb.data.DeckManager
import eb.eventhandling.BlackBoard
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.utilities.Hint
import eb.utilities.Utilities

/**
 * CardEditingManager coordinates the flow of information from the window that
 * requests a card to be created/edited to the UI-element that actually does the
 * editing.
 *
 * @author Eric-Wubbo Lameijer
 */
class CardEditingManager (private var m_cardToBeModified : Card? = null) {
    init {
        if (m_cardToBeModified == null) activateCardCreationWindow()
        else activateCardEditingWindow(m_cardToBeModified)
    }

    private var m_cardEditingWindow: CardEditingWindow? = null

    private val currentFront: String
        get() = if (m_cardToBeModified == null) {
            ""
        } else {
            m_cardToBeModified!!.front.contents
        }

    /**
     * Stores which card is to me modified.
     *
     * @param card
     * the card to be edited
     */


    private fun closeOptionPane() {
        JOptionPane.getRootFrame().dispose()
    }

    fun inCardCreatingMode(): Boolean {
        return m_cardToBeModified == null

    }

    /**
     * Shows the card editing window; however, has a guard that prevents the same
     * card from being edited in two different windows.
     *
     * @param card
     * the card to be edited.
     */
    private fun activateCardEditingWindow(card: Card?) {
        if (!c_cardsBeingEdited.contains(card)) {
            m_cardEditingWindow = CardEditingWindow.display(card!!.front.contents, card.back, this)
        }
    }

    private fun activateCardCreationWindow() {
        m_cardEditingWindow = CardEditingWindow.display("", "", this)
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
            val currentCardWithThisFront = DeckManager.currentDeck!!.cards.getCardWithFront(frontHint)
            if (frontText == currentFront || currentCardWithThisFront == null) {
                submitCardContents(frontHint, backText)
            } else {

                // Case 3 of 3: there is a current (but different) card with this exact
                // same front. Resolve this conflict.
                handleCardBeingDuplicate(frontHint, backText, currentCardWithThisFront)
            }
        }
    }

    private fun handleCardBeingDuplicate(frontText: Hint, backText: String,
                                         duplicate: Card) {

        val reeditButton = JButton("Re-edit card")
        reeditButton.addActionListener { closeOptionPane() }

        val mergeButton = JButton("Merge backs of cards")
        mergeButton.addActionListener {
            val currentBack = backText
            val otherBack = duplicate.back
            val newBack = "$currentBack; $otherBack"
            closeOptionPane()
            m_cardEditingWindow!!.updateContents(frontText.contents, newBack)
            DeckManager.currentDeck!!.cards.removeCard(duplicate)

        }
        val deleteThisButton = JButton("Delete this card")
        deleteThisButton.addActionListener {
            closeOptionPane()
            if (inCardCreatingMode()) {
                m_cardEditingWindow!!.updateContents("", "")
            } else {
                DeckManager.currentDeck!!.cards.removeCard(m_cardToBeModified!!)
                endEditing()
            }
        }
        val deleteOtherButton = JButton("Delete the other card")
        deleteOtherButton.addActionListener {
            DeckManager.currentDeck!!.cards.removeCard(duplicate)
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
            DeckManager.currentDeck!!.cards.addCard(candidateCard)
            m_cardEditingWindow!!.updateContents("", "")
            m_cardEditingWindow!!.focusFront()
        } else {
            // in editing mode
            m_cardToBeModified!!.front = frontText
            m_cardToBeModified!!.back = backText
            c_cardsBeingEdited.remove(m_cardToBeModified!!)
            m_cardEditingWindow!!.dispose()
        }
        BlackBoard.post(Update(UpdateType.CARD_CHANGED))
    }

    fun endEditing() {
        if (!inCardCreatingMode()) {
            c_cardsBeingEdited.remove(m_cardToBeModified)
        }
        m_cardEditingWindow!!.dispose()
    }

    companion object {

        // prevent a card from being edited in two windows at the same time.
        private val c_cardsBeingEdited = HashSet<Card>()
    }

}
