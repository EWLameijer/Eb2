package eb.analysis

import eb.data.CardCollection
import eb.data.DeckManager
import eb.data.Review
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

fun Number.toHours(): Duration = Duration.ofHours(this.toLong())

object Analyzer {
    fun run() {
        println("Starting deck analysis")
        val cards = DeckManager.currentDeck().cardCollection
        println("Number of cards: ${cards.getTotal()}")
        val (failures, successes) = analyzeCards(cards)
        val percentFirstSuccess = 100.0 * successes / (successes + failures)
        println("$successes successes in the first round, $failures failures, success percentage $percentFirstSuccess%")
        println("Ending deck analysis")
    }


    private fun analyzeCards(cards: CardCollection): Pair<Int, Int> {
        var successes = 0
        var failures = 0
        // 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597
        val borderTimes = listOf<Number>(0.5, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, 1000000.0).map { it.toHours() }.toTypedArray()
        val histogramFirstReview = HistogramMaker("Success rates for the first review", *borderTimes)
        val histogramAfterFailure = HistogramMaker("Success rates for the first review after failing the very first review", *borderTimes)
        val histogramAfterSuccess = BiHistogramMaker("Success rates at the second review as a function of the time of the first review", *borderTimes)
        val borderHours: Array<Int> = Array(24) { it + 1 }
        val histogramCreationTime = HistogramMaker("Success rate of first review as function of time of learning", *borderHours)

        val lastReviews = mutableListOf<Review>()

        cards.getCards().forEach {
            println("Datetime: ${LocalDateTime.ofInstant(it.creationInstant, ZoneId.of("Europe/Paris"))}")
            val creationTime = LocalDateTime.ofInstant(it.creationInstant, ZoneId.of("Europe/Paris")).hour
            println("Hour = $creationTime")
            //println(it.getReviews().size)
            val reviews = it.getReviews()
            if (reviews.isNotEmpty()) {
                lastReviews += reviews
                val firstReview = reviews[0]
                val creationTime = LocalDateTime.ofInstant(it.creationInstant, ZoneId.of("Europe/Paris")).hour
                histogramCreationTime.addDataPoint(creationTime, firstReview.wasSuccess)
                val timeBetweenCreationAndFirstReview = Duration.between(it.creationInstant, firstReview.instant)
                //println(timeBetweenCreationAndFirstReview)
                histogramFirstReview.addDataPoint(timeBetweenCreationAndFirstReview, firstReview.wasSuccess)
                if (!firstReview.wasSuccess) {
                    if (reviews.size > 1) {
                        val secondReview = reviews[1]
                        val timeBetweenFirstAndSecondReview = Duration.between(firstReview.instant, secondReview.instant)
                        histogramAfterFailure.addDataPoint(timeBetweenFirstAndSecondReview, secondReview.wasSuccess)
                    }
                } else { // firstReview WAS a success
                    if (reviews.size > 1) {
                        val secondReview = reviews[1]
                        val timeBetweenFirstAndSecondReview = Duration.between(firstReview.instant, secondReview.instant)
                        histogramAfterSuccess.addDataPoint(timeBetweenCreationAndFirstReview, timeBetweenFirstAndSecondReview, secondReview.wasSuccess)
                    }


                }
                if (firstReview.wasSuccess) successes++ else failures++
            }
        }


        histogramFirstReview.displayResults()
        histogramAfterFailure.displayResults()
        histogramAfterSuccess.displayResults()
        lastReviews.sortBy { it.instant }

        //lastReviews.forEach {println(it)}
        showAverage(100, lastReviews)
        showAverage(1000, lastReviews)
        showAverage(10000, lastReviews)
        showAverage(100000, lastReviews)
        histogramCreationTime.displayResults()
        return Pair(failures, successes)

    }
}

fun showAverage(n: Int, reviews: List<Review>) {
    val average = reviews.takeLast(n).map { if (it.wasSuccess) 1 else 0 }.average() * 100
    val averageAsString = String.format("%.2f", average)
    println("Of the last $n reviews, the average score was $averageAsString%")

}

class HistogramMaker<T : Comparable<T>>(private val description: String, vararg boxValues: T) {
    val boxes = boxValues.map { HistogramBox(it) }.toList()
    private var accumulatedSuccesses = 0
    private var accumulatedFailures = 0

    fun addDataPoint(value: T, result: Boolean) {
        if (result) accumulatedSuccesses++ else accumulatedFailures++
        boxes.first { it.canAssign(value, result) }
    }

    fun displayResults() {
        println(description)
        boxes.forEach { it.showResult() }
        println("-----")
        val percentFirstSuccess = 100.0 * accumulatedSuccesses / (accumulatedSuccesses + accumulatedFailures)
        println("On average: $accumulatedSuccesses successes, $accumulatedFailures failures, success percentage $percentFirstSuccess%")
    }
}

class BiHistogramMaker(private val description: String, vararg elapsedDuration: Duration) {
    val boxes: List<BiDurationTimeBox> = elapsedDuration.flatMap { firstDuration ->
        elapsedDuration.map { secondDuration -> BiDurationTimeBox(firstDuration, secondDuration) }
    }
    private var accumulatedSuccesses = 0
    private var accumulatedFailures = 0

    fun addDataPoint(firstDuration: Duration, secondDuration: Duration, result: Boolean) {
        if (result) accumulatedSuccesses++ else accumulatedFailures++
        boxes.first { it.canAssign(firstDuration, secondDuration, result) }
    }

    fun displayResults() {
        println(description)
        boxes.forEach { it.showResult() }
        println("-----")
        val percentFirstSuccess = 100.0 * accumulatedSuccesses / (accumulatedSuccesses + accumulatedFailures)
        println("On average: $accumulatedSuccesses successes, $accumulatedFailures failures, success percentage $percentFirstSuccess%")
    }
}

class HistogramBox<T : Comparable<T>>(private val maxValue: T) {
    private var successes = 0
    private var failures = 0

    fun canAssign(value: T, result: Boolean): Boolean {
        if (value <= maxValue) {
            if (result) successes++ else failures++
            return true
        }
        return false
    }

    fun showResult() {
        val percentFirstSuccess = 100.0 * successes / (successes + failures)
        println("Until $maxValue: $successes successes in the first round, $failures failures, success percentage $percentFirstSuccess%")
    }
}

class BiDurationTimeBox(private val firstMaxTime: Duration, private val secondMaxTime: Duration) {
    private var successes = 0
    private var failures = 0

    fun canAssign(firstDuration: Duration, secondDuration: Duration, result: Boolean): Boolean {
        if (firstDuration <= firstMaxTime && secondDuration <= secondMaxTime) {
            if (result) successes++ else failures++
            return true
        }
        return false
    }

    fun showResult() {
        val percentFirstSuccess = 100.0 * successes / (successes + failures)
        println("For before time $firstMaxTime and then $secondMaxTime: $successes successes in the first round, $failures failures, success percentage $percentFirstSuccess%")
    }
}