package eb.data

import java.io.Serializable
import java.io.Writer

import eb.writer.CardConverter
import eb.eventhandling.BlackBoard
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.utilities.Hint
import java.lang.RuntimeException

/**
 * CardCollection contains a collection of cards, which forms the content of the
 * deck (so what is stored, what should be learned, not how it should be learned
 * or the file system logistics).
 *
 * Note that while Cards themselves are considered unique/distinctive if they
 * have either different fronts or different backs, a CardCollection has the
 * additional requirement that all fronts should be unique in the collection (so
 * the number of cards equals the the size of the set of fronts) and there
 * should not be any "empty" fronts.
 *
 * @author Eric-Wubbo Lameijer
 */
class CardCollection : Serializable {

    private var cards = arrayListOf<Card>()

    fun getCards() = cards.toList()

    fun getCardTexts(): List<Pair<String, String>> = cards.map { it.getFrontAndBack() }

    fun getTotal() = cards.size

    /**
     * Returns the number of reviewing points of a deck, being the sum of the
     * latest "success streaks" of all cards in the deck. For example a fresh deck
     * will have 0 points, a 100 card deck where each card has has 2 successful
     * reviews 200 points, failing a review would bring that back to 2x99=198
     * points, and so on.
     */
    fun getReviewingPoints() = cards.map { it.streakSize() }.sum()

    /**
     * Write the cards of this collection, in alphabetical order (well, the order
     * imposed by the default character encoding) to a given writer.
     *
     * @param writer the writer to which the cards have to be written.
     * @param formatting how the card is transformed to text
     */
    fun writeCards(writer: Writer, formatting: (Card) -> String) =
        cards.sortedBy { it.front }.forEach { CardConverter.writeLine(writer, formatting(it)) }

    // Checks whether a certain card can be added to the deck. In practice, this means that the front is not already
    // present in the deck.
    private fun canAddCard(card: Card) = getCardWithFront(card.front) == null

    fun addCard(card: Card) {
        // preconditions: card must be 'addable' (cannot be a duplicate of a card already present)
        require(canAddCard(card)) {
            """LogicalDeck.addCard() error: the card that is intended to be added is invalid. The 'canAddCard'
method has to be invoked first to check the possibility of the current method."""
        }

        cards.add(card)
        BlackBoard.post(Update(UpdateType.DECK_CHANGED))
    }

    // Returns the card with the given front text - if such a card exists in the collection, otherwise returns null.
    fun getCardWithFront(frontText: Hint): Card? = cards.find { it.front == frontText }

    fun removeCard(card: Card) {
        val collectionContainedCard = cards.remove(card)
        if (collectionContainedCard)
            BlackBoard.post(Update(UpdateType.DECK_CHANGED))
        else
            throw RuntimeException("CardCollection.removeCard() error: the card cannot be removed, as it is not in the deck!")
    }

    companion object {
        // the proper auto-generated serialVersionUID as CardCollection should be serializable.
        private const val serialVersionUID = -6526056675010032709L
    }
}
