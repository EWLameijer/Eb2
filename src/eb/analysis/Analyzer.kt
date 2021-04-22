package eb.analysis

import eb.Eb
import eb.data.*
import eb.utilities.Utilities
import eb.utilities.getDateString
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.lang.IllegalArgumentException
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

    fun List<Long>.median(): Long?  {
        val sortedList = sorted()
        return when {
            isEmpty() -> null
            size % 2 != 0 -> sortedList[(size - 1) / 2]
            else -> {
                val afterCenterIndex = size / 2 // list size 4 has elements 0, 1, 2, 3; 4/2=2
                val beforeCenterIndex = afterCenterIndex - 1
                (sortedList[beforeCenterIndex] + sortedList[afterCenterIndex]) / 2
            }
        }
    }

    /*
    Working with average times did not work very well; I suspect especially for the low intervals, the average is
    too much skewed by late reviews (after a weekend or so)
    20210219 (2.2.1) try setting the time to the MEDIAN of the SUCCEEDED reviews - 20% (to take delays in reviewing into
    account
     */
    private fun getPossibleTimeRecommendation(pattern: String, cards: List<Card>): Duration? {
        val reliabilityCutoff = 60 // less than 60 cards? Not reliable enough
        val correctedImprovedTime = getCorrectedImprovedTime(pattern, cards)

        return if (cards.size >= reliabilityCutoff)
            Duration.ofMinutes(correctedImprovedTime.toLong()) else null
    }

    private fun getCorrectedImprovedTime(pattern: String, cards: List<Card>): Double {
        val basicImprovedTime = improvedTime(pattern, cards)
        // start possible reviews 20% sooner as you probably won't be able to review immediately anyway
        val correctingForLaterReviewDiscount = 0.80
        return basicImprovedTime * correctingForLaterReviewDiscount
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

    class PatternReporter(private val pattern: String, cards: List<Card>) {
        private fun Int?.toHourText() = convertToHourText(this?.toLong())
        private fun Long?.toHourText() = convertToHourText(this)
        private fun convertToHourText(timeInMinutes: Long?) =
            if (timeInMinutes != null) (timeInMinutes / 60.0).roundToInt().toString() else "unknown"

        private val patternLength = pattern.length
        private val totalCards = cards.size
        private val succeededCards = cards.filter { it.getReviews()[patternLength].wasSuccess }
        private val failedCards = cards.filter { !it.getReviews()[patternLength].wasSuccess }
        private val reviewingTimesInMin = cards.map { it.waitingTimeBeforeRelevantReview(patternLength) }
        private val avgReviewingTimeInMin = reviewingTimesInMin.average()
        private val numSuccesses = succeededCards.size

        // NOTE: prefer the median of the successful reviews; fall back to median of all reviews if there are no
        // successes.
        private val succeededMedianReviewTime =
            succeededCards.map { it.waitingTimeBeforeRelevantReview(patternLength) }.median()
        private val medianSuccessReviewTimeStr = succeededMedianReviewTime.toHourText()
        private val numFailures = failedCards.size
        private val medianFailureReviewTimeStr =
            failedCards.map { it.waitingTimeBeforeRelevantReview(patternLength) }.median().toHourText()
        private val successPercentage = succeededCards.size * 100.0 / totalCards
        private val betterIntervalDurationInMin =
            getCorrectedImprovedTime(pattern, cards)
        private val betterIntervalDurationInH = betterIntervalDurationInMin.roundToInt().toHourText()

        fun report() = buildString {
            append("-$pattern: $totalCards ")
            append("%.1f".format(successPercentage))
            append("% correct ")
            append("($numSuccesses successes, $numFailures failures) - ")
            append("average review time ${avgReviewingTimeInMin.roundToInt().toHourText()} h")
            append(", aiming for $betterIntervalDurationInH h; ")
            append("median review times $medianSuccessReviewTimeStr h for successful reviews, ")
            append("$medianFailureReviewTimeStr h for failed reviews.")
        }
    }

    private fun getSuccessPercentage(pattern: String, cards: List<Card>): Double {
        val succeededCards = cards.filter { it.getReviews()[pattern.length].wasSuccess }
        return succeededCards.size * 100.0 / cards.size
    }

    private fun getReviewTimeToTweak(pattern: String, cards: List<Card>): Long {
        if (cards.isEmpty()) throw IllegalArgumentException()
        val patternLength = pattern.length
        val succeededCards = cards.filter { it.getReviews()[patternLength].wasSuccess }
        return if (succeededCards.isNotEmpty())
            succeededCards.map { it.waitingTimeBeforeRelevantReview(patternLength) }.median()!!
        else cards.map { it.waitingTimeBeforeRelevantReview(patternLength) }.median()!!
    }

    private fun improvedTime(pattern: String, cards: List<Card>): Double {
        val currentSuccessPercentage = getSuccessPercentage(pattern, cards)
        val idealSuccessPercentage = DeckManager.currentDeck().studyOptions.otherSettings.idealSuccessPercentage
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
        return getReviewTimeToTweak(pattern, cards) * multiplicationFactor
    }
}