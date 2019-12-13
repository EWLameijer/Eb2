package eb.mainwindow

import java.awt.CardLayout
import java.awt.Graphics
import java.awt.event.ComponentListener
import java.beans.EventHandler

import java.awt.event.KeyEvent

import eb.data.DeckManager
import eb.eventhandling.BlackBoard
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.mainwindow.reviewing.ReviewManager
import eb.utilities.ProgrammableAction
import javax.swing.*

class SummarizingPanel internal constructor() : JPanel() {
    private var report = JLabel()
    private var backToInformationModeButton = JButton("Back to information screen")
    private var backToReactiveModeButton = JButton("Back to information screen")
    private var backToReviewing = JButton("Go to next round of reviews")
    private var buttonPanel = JPanel()
    private var reviewsCompletedPanel = JPanel()
    private var stillReviewsToDoPanel = JPanel()

    private fun makeKeystrokeActivateRunnable(
            button: JButton, keyStroke: KeyStroke, actionName: String, runnable: () -> Unit) {

        button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName)
        button.actionMap.put(actionName, ProgrammableAction(runnable))
    }

    private fun makeButtonAndKeystrokeActivateRunnable(
            button: JButton, keyStroke: KeyStroke, actionName: String, runnable: () -> Unit) {
        button.addActionListener{ runnable() }
        makeKeystrokeActivateRunnable(button, keyStroke, actionName, runnable)
    }

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
        makeButtonAndKeystrokeActivateRunnable(backToReactiveModeButton,
                KeyStroke.getKeyStroke("pressed ENTER"), "back to reactive mode"){ toReactiveMode() }
        reviewsCompletedPanel.add(backToReactiveModeButton)

        backToReviewing.mnemonic = KeyEvent.VK_G
        backToReviewing.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('g'), "goToNextReview")
        backToReviewing.actionMap.put("goToNextReview", ProgrammableAction { backToReviewingMode() })
        backToReviewing.addActionListener { backToReviewingMode() }
        stillReviewsToDoPanel.addComponentListener(EventHandler.create(ComponentListener::class.java, this,
                "requestFocusInWindow", null, "componentShown"))
        makeButtonAndKeystrokeActivateRunnable(backToReviewing,
                KeyStroke.getKeyStroke('G'), "back to reviewing") { backToReviewingMode() }
        stillReviewsToDoPanel.add(backToReviewing)

        backToInformationModeButton.mnemonic = KeyEvent.VK_B
        makeButtonAndKeystrokeActivateRunnable(backToInformationModeButton,
                KeyStroke.getKeyStroke('b'), "back to information mode")
                { backToInformationMode() }
        stillReviewsToDoPanel.add(backToInformationModeButton)

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

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        report.text  = buildString {
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
            append("time needed for answering<br>")
            val averageTime = completedReviews.map { it.thinkingTime }.averageOrNull()
            append("average time: ${optionalDoubleToString(averageTime)}<br>")
            val averageCorrectTime = correctReviews.map { it.thinkingTime }.averageOrNull()
            append("average time per correct card: ${optionalDoubleToString(averageCorrectTime)}<br>")
            val averageIncorrectTime = incorrectReviews.map { it.thinkingTime }.averageOrNull()
            append("average time per incorrect card: ${optionalDoubleToString(averageIncorrectTime)}<br>")
            append("</html>")
        }
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
