package eb.mainwindow

import eb.data.Card
import java.awt.CardLayout
import java.awt.Graphics
import java.awt.event.ComponentListener
import java.beans.EventHandler
import eb.data.DeckManager
import eb.eventhandling.BlackBoard
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.mainwindow.reviewing.ReviewManager
import eb.utilities.Utilities.createKeyPressSensitiveButton
import java.io.File
import javax.swing.*

class SummarizingPanel internal constructor() : JPanel() {
    private var report = JLabel()
    private var buttonPanel = JPanel()
    private var reviewsCompletedPanel = JPanel()
    private var stillReviewsToDoPanel = JPanel()

    private fun backToInformationMode() =
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, MainWindowState.INFORMATIONAL.name))

    private fun backToReviewingMode() =
            // REACTIVE ensures that a new review session is created.
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, MainWindowState.REACTIVE.name))

    init {
        this.addComponentListener(EventHandler.create(ComponentListener::class.java, this,
                "requestFocusInWindow", null, "componentShown"))

        reviewsCompletedPanel.addComponentListener(EventHandler.create(ComponentListener::class.java, this,
                "requestFocusInWindow", null, "componentShown"))

        reviewsCompletedPanel.add(createKeyPressSensitiveButton("Back to information screen", "pressed ENTER") { toReactiveMode() })

        stillReviewsToDoPanel.addComponentListener(EventHandler.create(ComponentListener::class.java, this,
                "requestFocusInWindow", null, "componentShown"))

        stillReviewsToDoPanel.add(createKeyPressSensitiveButton("Go to next round of reviews", 'g') { backToReviewingMode() })
        stillReviewsToDoPanel.add(createKeyPressSensitiveButton("Back to information screen", 'b') { backToInformationMode() })

        buttonPanel.layout = CardLayout()
        buttonPanel.add(reviewsCompletedPanel, REVIEWS_COMPLETED_MODE)
        buttonPanel.add(stillReviewsToDoPanel, STILL_REVIEWS_TODO_MODE)
        add(report)
        add(buttonPanel)
    }

    private fun toReactiveMode() =
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, MainWindowState.REACTIVE.name))

    private fun optionalDoubleToString(d: Double?) =
            if (d is Double) String.format("%.2f", d)
            else "not applicable"

    private fun List<Double>.averageOrNull() =
            if (this.isEmpty()) null
            else this.average()



    private fun successStatistics(cards: List<Card>, text: String) = buildString {
        val completedReviews = cards.map{ it.getReviews().last()}
        val totalNumberOfReviews = completedReviews.size.toLong()
        val (correctReviews, incorrectReviews) = completedReviews.partition { it.wasSuccess }

        append("$text<br>")
        append("total: $totalNumberOfReviews <br>")
        append("correctly answered: ${correctReviews.size}<br>")
        append("incorrectly answered: ${incorrectReviews.size}<br>")
        val percentageOfCorrectReviews = 100.0 * correctReviews.size / totalNumberOfReviews
        val percentageCorrectReviewsAsString = String.format("%.2f", percentageOfCorrectReviews)
        append("percentage of correct reviews: $percentageCorrectReviewsAsString%")
        append("<br><br>")
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        report.text = buildString {
            append("<html>")
            append("<b>Summary</b><br><br>")
            append("Cards reviewed<br>")
            val allReviews = ReviewManager.reviewResults()
            val completedReviews = allReviews.filterNotNull()
            val totalNumberOfReviews = completedReviews.size.toLong()
            val (correctReviews, incorrectReviews) = completedReviews.partition { it.wasSuccess }
            append("total: $totalNumberOfReviews <br>")
            append("correctly answered: ${correctReviews.size}<br>")
            append("incorrectly answered: ${incorrectReviews.size}<br>")
            val percentageOfCorrectReviews = 100.0 * correctReviews.size / totalNumberOfReviews
            val percentageCorrectReviewsAsString = String.format("%.2f", percentageOfCorrectReviews)
            append("percentage of correct reviews: $percentageCorrectReviewsAsString%")
            append("<br><br>")
            val reviewedCards = ReviewManager.reviewedCards()
            val (newlyReviewedCards, repeatReviewedCards) = reviewedCards.partition { it.getReviews().size == 1 }
            val (previouslySucceededCards, previouslyFailedCards) = repeatReviewedCards.partition { it.preLastReview().wasSuccess }
            append(successStatistics(previouslySucceededCards, "Previously succeeded cards"))
            append(successStatistics(previouslyFailedCards, "Previously failed cards"))
            append(successStatistics(newlyReviewedCards, "New cards"))

            append("time needed for answering<br>")
            val averageTime = completedReviews.map { it.thinkingTime }.averageOrNull()
            append("average time: ${optionalDoubleToString(averageTime)}<br>")
            val averageCorrectTime = correctReviews.map { it.thinkingTime }.averageOrNull()
            append("average time per correct card: ${optionalDoubleToString(averageCorrectTime)}<br>")
            val averageIncorrectTime = incorrectReviews.map { it.thinkingTime }.averageOrNull()
            append("average time per incorrect card: ${optionalDoubleToString(averageIncorrectTime)}<br>")
            append("</html>")
        }
        File("log.txt").writeText(report.text.replace("<br>", "\n").replace("<.*?>".toRegex(), ""))
        val cardLayout = buttonPanel.layout as CardLayout

        if (DeckManager.currentDeck().reviewableCardList().isEmpty()) {
            cardLayout.show(buttonPanel, REVIEWS_COMPLETED_MODE)
        } else {
            cardLayout.show(buttonPanel, STILL_REVIEWS_TODO_MODE)
        }
    }

    companion object {
        private const val REVIEWS_COMPLETED_MODE = "reviews completed"
        private const val STILL_REVIEWS_TODO_MODE = "still reviews to do"
    }
}
