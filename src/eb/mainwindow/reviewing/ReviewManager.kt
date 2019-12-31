package eb.mainwindow.reviewing

import java.time.Duration
import java.util.ArrayList

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
import eb.utilities.doNothing
import eb.utilities.log
import kotlin.math.min

/**
 * Manages the review session much like Deck manages the LogicalDeck: there can only be one review at a time
 *
 * @author Eric-Wubbo Lameijer
 */
object ReviewManager : Listener {
    init {
        BlackBoard.register(this, UpdateType.DECK_SWAPPED)
        BlackBoard.register(this, UpdateType.CARD_CHANGED)
        BlackBoard.register(this, UpdateType.DECK_CHANGED)
    }

    private var reviewPanel: ReviewPanel? = null
    private var currentDeck: Deck? = null
    private var cardsToBeReviewed = mutableListOf<Card>()
    private var cardsReviewed = mutableListOf<Card>()

    // counter stores the index of the card in the cardsToBeReviewed list that should be reviewed next.
    private var counter: Int = 0

    // startTimer is activated when the card is shown
    private val startTimer = FirstTimer()

    // stopTimer is activated when the user presses the button to show the answer.
    private val stopTimer = FirstTimer()

    // Should the answer (back of the card) be shown to the user? 'No'/false when the user is trying to recall the answer,
    // 'Yes'/true when the user needs to check the answer.
    private var showAnswer: Boolean = false

    fun reviewResults(): List<Review?> {
        ensureReviewSessionIsValid()
        return cardsReviewed.map { it.lastReview() }
    }

    fun reviewedCards(): List<Card> {
        ensureReviewSessionIsValid()
        return cardsReviewed.toList()
    }

    fun currentCard(): Card? =
            if (cardsToBeReviewed.isEmpty() || counter >= cardsToBeReviewed.size) null
            else cardsToBeReviewed[counter]

    fun currentFront(): String {
        ensureReviewSessionIsValid()
        return currentCard()?.front?.contents ?: EMPTY_STRING
    }

    private fun currentBack() = currentCard()?.back ?: EMPTY_STRING

    // start the reviewing process,
    fun start(inputReviewPanel: ReviewPanel?) {
        if (inputReviewPanel != null) {
            reviewPanel = inputReviewPanel
            currentDeck = DeckManager.currentDeck()
        }
        continueReviewSession()
    }

    private fun ensureReviewSessionIsValid() {
        if (currentDeck != DeckManager.currentDeck()) {
            currentDeck = DeckManager.currentDeck()
            initializeReviewSession()
        }
    }

    fun wasRemembered(wasRemembered: Boolean) {
        ensureReviewSessionIsValid()
        val duration = Duration.between(startTimer.instant(), stopTimer.instant())
        val durationInSeconds = duration.nano / 1000_000_000.0 + duration.seconds
        log("$counter $durationInSeconds")
        currentCard()!!.addReview(Review(duration, wasRemembered))
        cardsReviewed.add(currentCard()!!)
        moveToNextReviewOrEnd()
    }

    override fun respondToUpdate(update: Update) = when (update.type) {
        UpdateType.CARD_CHANGED -> updatePanels()
        UpdateType.DECK_CHANGED -> updateCollection() // It can be that the current card (or another) has been deleted
        UpdateType.DECK_SWAPPED -> initializeReviewSession()
        else -> doNothing
    }

    fun showAnswer() {
        ensureReviewSessionIsValid()
        stopTimer.press()
        showAnswer = true
        updatePanels()
    }

    private fun updatePanels() {
        if (activeCardExists()) {
            val currentBack = if (showAnswer) currentBack() else EMPTY_STRING
            reviewPanel!!.updatePanels(currentFront(), currentBack, showAnswer)
        }
    }

    private fun initializeReviewSession() {
        cardsReviewed = mutableListOf() // don't carry old reviews with you.
        continueReviewSession()
    }

    private fun continueReviewSession() {
        val currentDeck = DeckManager.currentDeck()

        val maxNumReviews = currentDeck.studyOptions.reviewSessionSize
        val reviewableCards = currentDeck.reviewableCardList().toMutableList()
        val totalNumberOfReviewableCards = reviewableCards.size
        log("Number of reviewable cards is $totalNumberOfReviewableCards")
        val numCardsToBeReviewed =
                if (maxNumReviews == null) totalNumberOfReviewableCards
                else min(maxNumReviews, totalNumberOfReviewableCards)
        // now, for best effect, those cards which have expired more recently should
        // be rehearsed first, as other cards probably need to be relearned anyway,
        // and we should try to contain the damage.
        val (newlyReviewedCards, repeatReviewedCards) = reviewableCards.partition { it.getReviews().isEmpty() }
        val (previouslySucceededCards, previouslyFailedCards) = repeatReviewedCards.partition { it.lastReview()!!.wasSuccess }
        val sortedPrevSuccCards = previouslySucceededCards.sortedBy { currentDeck.getTimeUntilNextReview(it) }
        val sortedPrevFailedCards = previouslyFailedCards.sortedBy { currentDeck.getTimeUntilNextReview(it) }
        val sortedNewCards = newlyReviewedCards.sortedBy { currentDeck.getTimeUntilNextReview(it) }
        val prioritizedReviewList = sortedPrevSuccCards + sortedPrevFailedCards + sortedNewCards

                // get the first n for the review
        cardsToBeReviewed = ArrayList(prioritizedReviewList.subList(0, numCardsToBeReviewed))
        cardsToBeReviewed.shuffle()

        counter = 0
        startCardReview()
    }

    private fun startCardReview() {
        showAnswer = false
        startTimer.reset()
        stopTimer.reset()
        startTimer.press()
        updatePanels()
    }

    private fun activeCardExists() = counter < cardsToBeReviewed.size

    private fun moveToNextReviewOrEnd() {
        if (hasNextCard()) {
            counter++
            startCardReview()
        } else {
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, MainWindowState.SUMMARIZING.name))
        }
    }

    // is there a next card to study?
    private fun hasNextCard() = counter < cardsToBeReviewed.lastIndex

    // The number of cards that still need to be reviewed in this session
    fun cardsToGoYet(): Int {
        ensureReviewSessionIsValid()
        return cardsToBeReviewed.size - counter
    }

    // If cards are added to (or, more importantly, removed from) the deck, ensure
    // that the card also disappears from the list of cards to be reviewed
    private fun updateCollection() {
        if (cardsToBeReviewed.isEmpty()) {
            updatePanels()
            return
        }
        val deletingCurrentCard = !deckContainsCardWithThisFront(cardsToBeReviewed[counter].front)
        val deletedIndices = cardsToBeReviewed.withIndex().filter { !deckContainsCardWithThisFront(cardsToBeReviewed[it.index].front) }.map { it.index }
        cardsToBeReviewed = cardsToBeReviewed.filter { deckContainsCardWithThisFront(it.front) }.toMutableList()
        deletedIndices.forEach { if (it <= counter) counter-- }

        if (deletingCurrentCard) {
            moveToNextReviewOrEnd()
        } else {
            updatePanels()
        }
    }

    // Returns whether this deck contains a card with this front. This sounds a lot like asking whether the deck
    // contains a card, but since by definition each card in a deck has a unique front,
    // just checking fronts simplifies things.
    private fun deckContainsCardWithThisFront(front: Hint) =
            DeckManager.currentDeck().cardCollection.getCardWithFront(front) != null

    // Allows the GUI to initialize the panel that displays the reviews
    fun setPanel(inputReviewPanel: ReviewPanel) {
        reviewPanel = inputReviewPanel
    }
}
