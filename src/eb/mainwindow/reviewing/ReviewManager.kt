package eb.mainwindow.reviewing

import java.time.Duration
import java.util.ArrayList
import java.util.Collections
import java.util.logging.Logger

import eb.data.Card
import eb.data.Deck
import eb.data.DeckManager
import eb.data.Review
import eb.eventhandling.BlackBoard
import eb.eventhandling.Listener
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.mainwindow.MainWindowState
import eb.utilities.Hint
import eb.utilities.EMPTY_STRING


/**
 * Manages the review session much like Deck manages the LogicalDeck: there can
 * only be one review at a time
 *
 * @author Eric-Wubbo Lameijer
 */
object ReviewManager : Listener {

    private var m_reviewPanel: ReviewPanel? = null
    private var m_currentDeck: Deck? = null
    private var m_cardsToBeReviewed = mutableListOf<Card>()

    // m_counter stores the index of the card in the m_cardsToBeReviewed list that should be reviewed next.
    private var m_counter: Int = 0

    // m_startTimer is activated when the card is shown
    private val m_startTimer = FirstTimer()

    // m_stopTimer is activated when the user presses the button to show the answer.
    private val m_stopTimer = FirstTimer()

    // Should the answer (back of the card) be shown to the user? 'No'/false when the user is trying to recall the answer,
    // 'Yes'/true when the user needs to check the answer.
    private var m_showAnswer: Boolean = false

    fun reviewResults(): List<Review?> {
        ensureReviewSessionIsValid()
        return m_cardsToBeReviewed.map{ it.lastReview()}
    }

    fun currentCard(): Card? =
            if (m_cardsToBeReviewed.isEmpty() || m_counter >= m_cardsToBeReviewed.size) null
            else m_cardsToBeReviewed[m_counter]

    fun currentFront(): String {
        ensureReviewSessionIsValid()
        return currentCard()?.front?.contents ?: EMPTY_STRING
    }

    private fun currentBack() = currentCard()?.back ?: EMPTY_STRING

    /**
     * start starts the reviewing process,
     *
     * @param reviewPanel
     */
    // TODO:
    fun start(reviewPanel: ReviewPanel?) {
        if (reviewPanel != null) {
            m_reviewPanel = reviewPanel
            m_currentDeck = DeckManager.contents
        }
        initializeReviewSession()
    }

    private fun ensureReviewSessionIsValid() {
        if (m_currentDeck !== DeckManager.contents) {
            m_currentDeck = DeckManager.contents
            initializeReviewSession()
        }
    }

    fun wasRemembered(wasRemembered: Boolean) {
        ensureReviewSessionIsValid()
        val duration = Duration.between(m_startTimer.instant!!,
                m_stopTimer.instant)
        val duration_in_s = duration.nano / 1000_000_000.0 + duration.seconds
        Logger.getGlobal().info(m_counter.toString() + " " + duration_in_s)
        val review = Review(duration, wasRemembered)
        currentCard()!!.addReview(review)
        moveToNextReviewOrEnd()
    }

    override fun respondToUpdate(update: Update) {
        if (update.type == UpdateType.CARD_CHANGED) {
            updatePanels()
        } else if (update.type == UpdateType.DECK_CHANGED) {
            // It can be that the current card has been deleted, OR another card has
            // been deleted.
            // initializeReviewSession();
            updateCollection()
        } else if (update.type == UpdateType.DECK_SWAPPED) {
            initializeReviewSession()
            // cleanUp();
        }
    }

    fun showAnswer() {
        ensureReviewSessionIsValid()
        m_stopTimer.press()
        m_showAnswer = true
        updatePanels()
    }

    /**
     * Updates the panels
     */
    fun updatePanels() {
        if (activeCardExists()) {
            val currentBack = if (m_showAnswer) currentBack() else ""
            m_reviewPanel!!.updatePanels(currentFront(), currentBack, m_showAnswer)
        }
    }

    private fun initializeReviewSession() {
        val currentDeck = DeckManager.currentDeck!!
        val maxNumReviews = currentDeck.studyOptions.reviewSessionSize
        val reviewableCards = currentDeck.reviewableCardList().toMutableList()
        val totalNumberOfReviewableCards = reviewableCards.size
        Logger.getGlobal().info("Number of reviewable cards is $totalNumberOfReviewableCards")
        val numCardsToBeReviewed =
                if (maxNumReviews == null) totalNumberOfReviewableCards
                else Math.min(maxNumReviews, totalNumberOfReviewableCards)
        // now, for best effect, those cards which have expired more recently should
        // be rehearsed first, as other cards probably need to be relearned anyway,
        // and we should try to contain the damage.
        reviewableCards.sortBy { currentDeck.getTimeUntilNextReview(it) }
        // get the first n for the review
        m_cardsToBeReviewed = ArrayList( reviewableCards.subList(0, numCardsToBeReviewed))
        m_cardsToBeReviewed.shuffle()

        m_counter = 0
        startCardReview()
    }

    private fun startCardReview() {
        m_showAnswer = false
        m_startTimer.reset()
        m_stopTimer.reset()
        m_startTimer.press()
        updatePanels()
    }

    private fun activeCardExists(): Boolean {
        return m_counter < m_cardsToBeReviewed.size
    }

    private fun moveToNextReviewOrEnd() {
        if (hasNextCard()) {
            m_counter++
            startCardReview()
        } else {
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, MainWindowState.SUMMARIZING.name))
        }
    }

    /**
     * Returns the index of the last card in the session.
     *
     * @return the index of the last card in the session.
     */
    private fun indexOfLastCard(): Int {
        return m_cardsToBeReviewed.size - 1
    }

    /**
     * Returns whether there is a next card to study
     *
     * @return whether there is a next card to study.
     */
    private fun hasNextCard(): Boolean {
        return m_counter + 1 <= indexOfLastCard()
    }

    /**
     * Returns the number of cards that still need to be reviewed in this session
     *
     * @return the number of cards that still must be reviewed in this session.
     */
    fun cardsToGoYet(): Int {
        ensureReviewSessionIsValid()
        return m_cardsToBeReviewed.size - m_counter
    }

    /**
     * If cards are added to (or, more importantly, removed from) the deck, ensure
     * that the card also disappears from the list of cards to be reviewed
     */
    private fun updateCollection() {
        var deletingCurrentCard = !deckContainsCardWithThisFront(m_cardsToBeReviewed[m_counter].front)
        val deletedIndices = m_cardsToBeReviewed.withIndex().
                filter{ !deckContainsCardWithThisFront(m_cardsToBeReviewed[it.index].front)}.map { it.index }
        m_cardsToBeReviewed = m_cardsToBeReviewed.filter { deckContainsCardWithThisFront(it.front)}.toMutableList()
        deletedIndices.forEach{ if (it <= m_counter) m_counter-- }

        if (deletingCurrentCard) {
            moveToNextReviewOrEnd()
        } else {
            updatePanels()
        }
    }

    /**
     * Returns whether this deck contains a card with this front. This sounds a
     * lot like whether the deck contains a card, but since by definition each
     * card in a deck has a unique front, just checking fronts simplifies things.
     *
     *
     * @param front
     * the front which may or may not be present on a card in the deck.
     * @return whether the deck contains a card with the given front.
     */
    private fun deckContainsCardWithThisFront(front: Hint) =
            DeckManager.currentDeck!!.cards.getCardWithFront(front) != null

    /**
     * Allows the GUI to initialize the panel that displays the reviews
     *
     * @param reviewPanel
     * the name of the panel in which the reviews are performed.
     */
    fun setPanel(reviewPanel: ReviewPanel) {
        m_reviewPanel = reviewPanel
    }
}
