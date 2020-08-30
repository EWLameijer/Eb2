package eb.mainwindow

import java.awt.CardLayout
import java.awt.Graphics
import java.awt.event.ComponentListener
import java.beans.EventHandler
import eb.data.DeckManager
import eb.data.Review
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

    private fun successStatistics(reviews: List<Review>, text: String) = buildString {
        append("$text<br>")
        val totalNumberOfReviews = reviews.size
        append("total: $totalNumberOfReviews <br>")
        val (correctReviews, incorrectReviews) = reviews.partition { it.wasSuccess }
        val numberOfCorrectReviews = correctReviews.size
        append("correctly answered: $numberOfCorrectReviews<br>")
        append("incorrectly answered: ${incorrectReviews.size}<br>")
        val percentageOfCorrectReviews = 100.0 * numberOfCorrectReviews / totalNumberOfReviews
        val percentageCorrectReviewsAsString = String.format("%.2f", percentageOfCorrectReviews)
        append("percentage of correct reviews: $percentageCorrectReviewsAsString%")
        append("<br><br>")
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        report.text = getReport()
        File("log.txt").writeText(report.text.replace("<br>", "\n").replace("<.*?>".toRegex(), ""))
        val cardLayout = buttonPanel.layout as CardLayout

        if (DeckManager.currentDeck().reviewableCardList().isEmpty()) {
            cardLayout.show(buttonPanel, REVIEWS_COMPLETED_MODE)
        } else {
            cardLayout.show(buttonPanel, STILL_REVIEWS_TODO_MODE)
        }
    }

    private fun getReport() = buildString {
        append("<html>")
        append("<b>Summary</b><br><br>")
        val allReviews = ReviewManager.reviewResults()
        append(successStatistics(allReviews, "Total reviews"))

        val firstTimeReviews = ReviewManager.getNewFirstReviews() // reviewedCards.filter { it.getReviews().size == it.getReviewsAfter(DeckManager.deckLoadTime()).size }
        val (previouslySucceededReviews, previouslyFailedReviews) = ReviewManager.getNonFirstReviews()
        append(successStatistics(previouslySucceededReviews, "Previously succeeded cards"))
        append(successStatistics(previouslyFailedReviews, "Previously failed cards"))
        append(successStatistics(firstTimeReviews, "New cards"))
        append("</html>")
    }

    companion object {
        private const val REVIEWS_COMPLETED_MODE = "reviews completed"
        private const val STILL_REVIEWS_TODO_MODE = "still reviews to do"
    }
}
