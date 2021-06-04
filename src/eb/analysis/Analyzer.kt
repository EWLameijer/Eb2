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

    private val fullPatternMap = mutableMapOf<String, PatternStatistics>()
    private val shortenedPatternMap = mutableMapOf<Int, PatternStatistics>()

    fun getRecommendationsMap(): Map<String, Duration?> {
        val cards = DeckManager.currentDeck().cardCollection
        val categoryMap = getCategoryMap(cards)
        val streakMap = getStreakMap(cards)
        println(fullPatternMap)
        println(shortenedPatternMap)
        return categoryMap.mapValues { (key, value) ->
            val streakLength = stringToStreakLength(key)
            getPossibleTimeRecommendation(key, value, streakMap[streakLength])
        }
    }

    private fun stringToStreakLength(category: String): Int = when {
        category.isEmpty() -> 0
        category.last() == 'T' -> category.takeLastWhile { it == 'T' }.length
        else -> category.takeLastWhile { it == 'F' }.length
    }

    class PatternStatistics {
        val successes = mutableListOf<Double>() // in minutes
        val failures = mutableListOf<Double>() // in minutes

        override fun toString() = "S$successes , F$failures"
    }



    // makes a map of which cards fall into a category/pattern (for example, all cards that started with a failed
    // review, then a successful review. NOTE! Since a card usually has multiple reviews, it will be referenced
    // by multiple categories, a 'FS' card will also occur in the ''  and 'F' categories. ALSO: this will select only
    // cards with at least one more review, so you can determine the success of a pattern by looking at the average
    // success rate of the patternlength-th review. So success rate of FS will be seen at 2nd review (0th=F, 1th=S)
    private fun getCategoryMap(cards: CardCollection): Map<String, List<Card>> {
        val categoryMap = mutableMapOf<String, List<Card>>()
        cards.getCards().forEach { card ->
            val reviews = card.getReviews()
            var currentPattern = ""
            while (currentPattern.length < reviews.size) { //
                categoryMap[currentPattern] = (categoryMap[currentPattern] ?: listOf()) + card
                val indexOfReviewToCheck = currentPattern.length
                val review = reviews[indexOfReviewToCheck]
                val waitingTime = card.waitingTimeBeforeRelevantReview(indexOfReviewToCheck).toMinutes().toDouble()

                if (!fullPatternMap.contains(currentPattern)) fullPatternMap[currentPattern] = PatternStatistics()

                if (review.wasSuccess) fullPatternMap[currentPattern]!!.successes += waitingTime
                else fullPatternMap[currentPattern]!!.failures += waitingTime

                val streakNumber = getStreakNumber(currentPattern)
                if (!shortenedPatternMap.contains(streakNumber)) shortenedPatternMap[streakNumber] = PatternStatistics()
                if (review.wasSuccess) shortenedPatternMap[streakNumber]!!.successes += waitingTime
                else shortenedPatternMap[streakNumber]!!.failures += waitingTime

                currentPattern += if (review.wasSuccess) 'S' else 'F'
            }
        }
        return categoryMap
    }

    private fun getStreakNumber(currentPattern: String): Int = when {
        currentPattern.isEmpty() -> 0
        currentPattern.endsWith('S') -> currentPattern.takeLastWhile { it == 'S' }.length
        else -> -currentPattern.takeLastWhile { it == 'F' }.length
    }

    // makes a map of the streak length (2=2 most recent reviews were successes)
    private fun getStreakMap(cards: CardCollection): Map<Int, List<Card>> {
        val streakMap = mutableMapOf<Int, List<Card>>()
        cards.getCards().forEach { card ->
            val reviews = card.getReviews()
            val streakLength = getStreakLength(reviews)
            streakMap[streakLength] = (streakMap[streakLength] ?: listOf()) + card
        }
        return streakMap
    }

    private fun getStreakLength(reviews: List<Review>): Int = when {
        reviews.isEmpty() -> 0
        reviews.last().wasSuccess -> reviews.takeLastWhile { it.wasSuccess }.size
        else -> reviews.takeLastWhile { !it.wasSuccess }.size
    }

    fun List<Long>.median(): Long? {
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
    private fun getPossibleTimeRecommendation(pattern: String, cards: List<Card>, streakCards: List<Card>?): Duration? {
        val reliabilityCutoff = 60 // less than 60 cards? Not reliable enough
        if (cards.size >= reliabilityCutoff) return Duration.ofMinutes(
            getCorrectedImprovedTime(
                pattern,
                cards
            ).toLong()
        ) else return null
        /*if (streakCards.size >= reliabilityCutoff) return Duration.ofMinutes()
        val correctedImprovedTime =

        return if (cards.size >= reliabilityCutoff)
             else null*/
    }

    private fun getCorrectedImprovedTime(pattern: String, cards: List<Card>): Double {
        val basicImprovedTime = improvedTime(pattern, cards)
        // start possible reviews 20% sooner as you probably won't be able to review immediately anyway
        val correctingForLaterReviewDiscount = 0.80
        return basicImprovedTime * correctingForLaterReviewDiscount
    }

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
        private val reviewingTimesInMin = cards.map { it.waitingTimeBeforeRelevantReview(patternLength).toMinutes() }
        private val avgReviewingTimeInMin = reviewingTimesInMin.average()
        private val numSuccesses = succeededCards.size

        // NOTE: prefer the median of the successful reviews; fall back to median of all reviews if there are no
        // successes.
        private val succeededMedianReviewTime =
            succeededCards.map { it.waitingTimeBeforeRelevantReview(patternLength).toMinutes() }.median()
        private val medianSuccessReviewTimeStr = succeededMedianReviewTime.toHourText()
        private val numFailures = failedCards.size
        private val medianFailureReviewTimeStr =
            failedCards.map { it.waitingTimeBeforeRelevantReview(patternLength).toMinutes() }.median().toHourText()
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
            succeededCards.map { it.waitingTimeBeforeRelevantReview(patternLength).toMinutes() }.median()!!
        else cards.map { it.waitingTimeBeforeRelevantReview(patternLength).toMinutes() }.median()!!
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