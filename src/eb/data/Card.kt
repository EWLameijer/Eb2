package eb.data

import eb.utilities.Hint
import java.io.Serializable
import java.time.Instant

import eb.utilities.log

/**
 * The Card class represents a card, which has contents (front and back, or 'stimulus' and 'response', as well as a
 * history (number of repetitions and such).
 *
 * @author Eric-Wubbo Lameijer
 */
class Card(var front: Hint, var back: String) : Serializable {

    internal val creationInstant = Instant.now()
    private val reviews = mutableListOf<Review>()

    fun lastReview(): Review? = reviews.last()

    fun history() = reviews.joinToString(prefix = "[", postfix = "]")

    fun hasBeenReviewed() = reviews.size > 0

    // Debugging function, helps check that the reviews have proceeded correctly.
    // Reports all reviews of this card performed so far.
    private fun reportReviews() = reviews.forEach { log("${it.thinkingTime} ${it.wasSuccess}") }

    fun addReview(review: Review) {
        reviews.add(review)
        reportReviews()
    }

    // Return the current number of consecutively successful reviews (2 uninterrupted successful reviews, 0
    // successful reviews (after a failure), and so on...)
    fun streakSize() = reviews.takeLastWhile { it.wasSuccess }.size

    fun getReviews() = reviews.toList()

    fun getFrontAndBack(): Pair<String, String> = front.contents to back

    fun getReviewsAfter(instant: Instant) = reviews.filter { it.instant > instant }

    companion object {
        // the proper auto-generated serialVersionUID as cards should be serializable.
        private const val serialVersionUID = -2746012998758766327L
    }
}
