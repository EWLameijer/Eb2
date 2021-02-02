package eb.analysis

import eb.Eb
import eb.data.*
import eb.subwindow.StudyOptions
import eb.utilities.Utilities
import eb.utilities.getDateString
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

object Analyzer {
    fun getRecommendationsMap(): Map<String, Duration?> {
        val cards = DeckManager.currentDeck().cardCollection
        val categoryMap = getCategoryMap(cards)
        return categoryMap.mapValues { getPossibleTimeRecommendation(it.key, it.value) }
    }

    private fun getCategoryMap(cards: CardCollection): Map<String, List<Card>> {
        val categoryMap = mutableMapOf<String, List<Card>>()
        cards.getCards().forEach { card ->
            val reviews = card.getReviews()
            var currentPattern = ""
            while (currentPattern.length < reviews.size) {
                categoryMap[currentPattern] = (categoryMap[currentPattern] ?: listOf()) + card
                val indexOfReviewToCheck = currentPattern.length
                currentPattern += if (reviews[indexOfReviewToCheck].wasSuccess) 'S' else 'F'
            }
        }
        return categoryMap
    }

    private fun getPossibleTimeRecommendation(pattern: String, cards: List<Card>): Duration? {
        val patternLength = pattern.length
        val reliabilityCutoff = 60 // less than 60 cards? Not too reliable
        val totalCards = cards.size
        val numSucceededCards = cards.count { it.getReviews()[patternLength].wasSuccess }
        val reviewingTimes = cards.map { it.waitingTimeBeforeRelevantReview(patternLength) }
        val avgReviewingTime = reviewingTimes.average()
        val successPercentage = numSucceededCards * 100.0 / totalCards
        return if (cards.size >= reliabilityCutoff)
            Duration.ofMinutes(improvedTime(avgReviewingTime, successPercentage).toLong()) else null
    }

    private fun Card.waitingTimeBeforeRelevantReview(reviewIndex: Int) =
        Duration.between(reviewInstant(reviewIndex - 1), reviewInstant(reviewIndex)).toMinutes()

    private fun Card.reviewInstant(reviewIndex: Int): Instant =
        if (reviewIndex >= 0) getReviews()[reviewIndex].instant else creationInstant

    fun run() {
        val deckName = DeckManager.currentDeck().name
        val fileName = "log-$deckName-${getDateString()}.txt"
        val outputStreamWriter = OutputStreamWriter(FileOutputStream(fileName), "UTF-8")
        BufferedWriter(outputStreamWriter).use { writer ->
            writeAnalysisFile(writer)
        }
    }

    private fun writeAnalysisFile(writer: BufferedWriter) {
        val cards = DeckManager.currentDeck().cardCollection
        val categoryMap = getCategoryMap(cards)
        writer.apply {
            writeHeader(cards)

            // write the data per 'review history' (like SSF, ='success, success, failure)
            categoryMap.keys.sorted().forEach {
                write(PatternReporter(it, categoryMap[it]!!).report())
                write(Utilities.EOL)
            }
        }
    }

    private fun BufferedWriter.writeHeader(cards: CardCollection) {
        val numReviews = cards.getCards().sumBy { it.getReviews().size }
        val numCorrect = cards.getCards().sumBy { it.getReviews().count { review -> review.wasSuccess } }
        val numIncorrect = numReviews - numCorrect
        val successPercentage = 100.0 * numCorrect / numReviews
        write("Eb version ${Eb.VERSION_STRING}${Utilities.EOL}")
        write("Number of cards is: ${cards.getTotal()}${Utilities.EOL}")
        write("Number of reviews: $numReviews, success percentage ")
        write("%.1f".format(successPercentage))
        write("% ($numCorrect correct, $numIncorrect incorrect)${Utilities.EOL}")
        write(Utilities.EOL)
    }

    private fun getMedianValue(collection: List<Long>): Long? =
        if (collection.isEmpty()) null
        else collection.sorted()[collection.size / 2]

    class PatternReporter(private val pattern: String, cards: List<Card>) {
        private fun Int?.toHourText() = convertToHourText(this?.toLong())
        private fun Long?.toHourText() = convertToHourText(this)
        private fun convertToHourText(timeInMinutes: Long?) =
            if (timeInMinutes != null) (timeInMinutes / 60.0).roundToInt().toString() else "unknown"

        private val patternLength = pattern.length
        private val totalCards = cards.size
        private val succeededCards = cards.filter { it.getReviews()[patternLength].wasSuccess }
        private val failedCards = cards.filter { !it.getReviews()[patternLength].wasSuccess }
        private val reviewingTimes = cards.map { it.waitingTimeBeforeRelevantReview(patternLength) }
        private val avgReviewingTime = reviewingTimes.average()
        private val numSuccesses = succeededCards.size
        private val medianSuccessReviewTime =
            getMedianValue(succeededCards.map { it.waitingTimeBeforeRelevantReview(patternLength) }).toHourText()
        private val numFailures = failedCards.size
        private val medianFailureReviewTime =
            getMedianValue(failedCards.map { it.waitingTimeBeforeRelevantReview(patternLength) }).toHourText()
        private val successPercentage = succeededCards.size * 100.0 / totalCards
        private val betterIntervalDurationInMin = improvedTime(avgReviewingTime, successPercentage)
        private val betterIntervalDurationInH = betterIntervalDurationInMin.roundToInt().toHourText()

        fun report() = buildString {
            append("$pattern: $totalCards ")
            append("%.1f".format(successPercentage))
            append("% correct ")
            append("($numSuccesses successes, $numFailures failures) - ")
            append("average review time ${avgReviewingTime.roundToInt().toHourText()} h")
            append(", aiming for $betterIntervalDurationInH h; ")
            append("median review times $medianSuccessReviewTime h for successful reviews, ")
            append("$medianFailureReviewTime h for failed reviews.")
        }
    }

    private fun improvedTime(averageReviewingTime: Double, currentSuccessPercentage: Double): Double {
        val idealSuccessPercentage = DeckManager.currentDeck().studyOptions.idealSuccessPercentage
        val percentageDifference = currentSuccessPercentage - idealSuccessPercentage
        var workingDifference = percentageDifference
        var multiplicationFactor = 1.0
        if (workingDifference < 0) {
            if (workingDifference < -10.0) workingDifference = -10.0
            multiplicationFactor = (100 - 0.5 * workingDifference * workingDifference) * 0.01 // minimum 50/100 = 0.5
        } else if (workingDifference > 0) {
            if (workingDifference > 10.0) workingDifference = 10.0
            multiplicationFactor = (100 + 0.5 * workingDifference * workingDifference) * 0.01 // maximum 150/100 = 1.5
        }
        return averageReviewingTime * multiplicationFactor
    }
}