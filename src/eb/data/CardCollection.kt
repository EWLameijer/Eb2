package eb.data

import java.io.Serializable
import java.io.Writer

import eb.disk_io.CardConverter
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

    private val cards = arrayListOf<Card>()

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
     * Returns an iterator to the collection, so for example the Deck can loop
     * over the individual cards.
     * NOTE: if this were a val instead of fun, it messes up the serialization of this class and therefore the saving
     * of the deck!
     */
    fun getIterator() = cards.iterator()

    /**
     * Write the cards of this collection, in alphabetical order (well, the order
     * imposed by the default character encoding) to a given writer.
     *
     * @param writer the writer to which the cards have to be written.
     * @param formatting how the card is transformed to text
     */
    fun writeCards(writer: Writer, formatting: (Card) -> String) {
        cards.sortedBy { it.front.contents }.forEach { CardConverter.writeLine(writer, formatting(it)) }
    }

    /**
     * Checks if a certain card can be added to the deck. In practice, this means
     * that the front is a valid identifier that is not already present in the
     * deck, and the back is not a null pointer.
     *
     * @param card
     * the candidate card to be added.
     *
     * @return whether the card can legally be added to the deck.
     */
    private fun canAddCard(card: Card) = getCardWithFront(card.front) == null

    /**
     * Adds a card to the deck. Note that one has to call canAddCard() beforehand.
     *
     * @param card
     * the card to add to the deck.
     */
    fun addCard(card: Card) {
        // preconditions: card must be 'addable'
        require(canAddCard(card)){
                """LogicalDeck.addCard() error: the card that is intended to be added is invalid. The 'canAddCard'
method has to be invoked first to check the possibility of the current method."""}

        cards.add(card)
        BlackBoard.post(Update(UpdateType.DECK_CHANGED))
    }

    /**
     * Returns an optional that contains the card with the given front text - if
     * such a card exists in the collection, or an empty optional if no card with
     * such front is present.
     *
     * @param frontText
     * the text on the front of the card that is sought
     * @return an optional that is empty if there is no card with the given front
     * in the current collection; and which is 'present' when such a card
     * actually exists.
     */
    fun getCardWithFront(frontText: Hint): Card? = cards.find { it.front == frontText }

    /**
     * Removes a given card from the collection.
     *
     * @param card
     * the card to be removed from the collection
     */
    fun removeCard(card: Card) {
        val collectionContainedCard = cards.remove(card)
        if (collectionContainedCard) {
            BlackBoard.post(Update(UpdateType.DECK_CHANGED))
        } else throw RuntimeException(
            "CardCollection.removeCard() error: the card cannot be removed, as it is not in the deck!")
    }

    companion object {
        // the proper auto-generated serialVersionUID as CardCollection should be serializable.
        private const val serialVersionUID = -6526056675010032709L
    }
}