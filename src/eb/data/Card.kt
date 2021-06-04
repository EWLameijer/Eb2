package eb.data

import eb.utilities.*
import java.io.Serializable
import java.time.Instant

import java.time.Duration

/**
 * The Card class represents a card, which has contents (front and back, or 'stimulus' and 'response', as well as a
 * history (number of repetitions and such).
 *
 * @author Eric-Wubbo Lameijer
 */
class Card(var front: Hint, var back: String) : Serializable {

    internal val creationInstant = Instant.now()
    private val reviews = mutableListOf<Review>()

    fun lastReview(): Review? = if (reviews.isEmpty()) null else reviews.last()

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


    fun getReviews() = reviews.toList()

    fun getFrontAndBack(): Pair<String, String> = front.contents to back

    fun getReviewsAfter(instant: Instant) = reviews.filter { it.instant > instant }

    fun getMemoryTime() : Duration {
        var index = reviews.lastIndex
        var totalDuration = Duration.ofSeconds(0)
        while (index >= 0) {
            if (reviews[index].wasSuccess) {
                if (index > 0) { // there has been a previous review
                    val elapsedTime = Duration.between(reviews[index-1].instant, reviews[index].instant)
                    totalDuration += elapsedTime
                } else {
                    val elapsedTime = Duration.between(creationInstant, reviews[index].instant)
                    totalDuration += elapsedTime
                }
            } else break
            index--
        }
        return totalDuration
    }

    fun reviewHistoryText(): String {
        val result = StringBuilder()
        val createdDateTime = creationInstant.getDateTimeString()
        result.append("Card created: $createdDateTime\n")
        for (index in getReviews().indices) result.append(getReviewDataAsString(index))
        val hoursSinceLastView = getHoursSinceLastView()
        val hoursAndDays = toDayHourString(hoursSinceLastView)
        result.append("$hoursAndDays since last view")
        return result.toString()
    }

    private fun getHoursSinceLastView(): Long {
        val indexOfLastView = getReviews().size - 1 // -1 if no reviews have taken place
        val lastViewInstant = reviewInstant(indexOfLastView)
        return Duration.between(lastViewInstant, Instant.now()).toHours()
    }

    private fun getReviewDataAsString(index: Int) = buildString {
        val review = getReviews()[index]
        append("${index + 1}: ")
        val reviewDateTime = review.instant.getDateTimeString()
        append("$reviewDateTime ")
        append(if (review.wasSuccess) "S" else "F")
        val durationInHours = waitingTimeBeforeRelevantReview(index).toHours()
        val dayHourString = toDayHourString(durationInHours)
        append(" ($dayHourString)\n")
    }

    fun waitingTimeBeforeRelevantReview(reviewIndex: Int) : Duration =
        Duration.between(reviewInstant(reviewIndex - 1), reviewInstant(reviewIndex))

    private fun reviewInstant(reviewIndex: Int): Instant =
        if (reviewIndex >= 0) getReviews()[reviewIndex].instant else creationInstant

    companion object {
        // the proper auto-generated serialVersionUID as cards should be serializable.
        private const val serialVersionUID = -2746012998758766327L
    }
}

fun List<Review>.streakSize() = takeLastWhile { it.wasSuccess }.size
