package eb.mainwindow

import java.awt.CardLayout
import java.awt.Graphics
import java.awt.event.ComponentListener
import java.beans.EventHandler

import com.sun.glass.events.KeyEvent

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
    private var buttonPanel: JPanel
    private var reviewsCompletedBPanel: JPanel
    private var stillReviewsToDoBPanel: JPanel

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
        reviewsCompletedBPanel = JPanel()
        reviewsCompletedBPanel.addComponentListener(EventHandler.create(ComponentListener::class.java, this,
                        "requestFocusInWindow", null, "componentShown"))
        makeButtonAndKeystrokeActivateRunnable(backToReactiveModeButton,
                KeyStroke.getKeyStroke("pressed ENTER"), "back to reactive mode"){ toReactiveMode() }
        reviewsCompletedBPanel.add(backToReactiveModeButton)

        stillReviewsToDoBPanel = JPanel()
        stillReviewsToDoBPanel.addComponentListener(EventHandler.create(ComponentListener::class.java, this,
                        "requestFocusInWindow", null, "componentShown"))

        backToReviewing.mnemonic = KeyEvent.VK_G
        backToReviewing.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('g'), "goToNextReview")
        backToReviewing.actionMap.put("goToNextReview", ProgrammableAction { backToReviewingMode() })
        backToReviewing.addActionListener { backToReviewingMode() }

        makeButtonAndKeystrokeActivateRunnable(backToReviewing,
                KeyStroke.getKeyStroke('G'), "back to reviewing") { backToReviewingMode() }
        stillReviewsToDoBPanel.add(backToReviewing)

        backToInformationModeButton.mnemonic = KeyEvent.VK_B
        makeButtonAndKeystrokeActivateRunnable(backToInformationModeButton,
                KeyStroke.getKeyStroke('B'), "back to information mode")
                { backToInformationMode() }
        stillReviewsToDoBPanel.add(backToInformationModeButton)

        buttonPanel = JPanel()
        buttonPanel.layout = CardLayout()
        buttonPanel.add(reviewsCompletedBPanel, REVIEWS_COMPLETED_MODE)
        buttonPanel.add(stillReviewsToDoBPanel, STILL_REVIEWS_TODO_MODE)
        add(report)
        add(buttonPanel)

    }

    private fun toReactiveMode() =
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, MainWindowState.REACTIVE.name))

    private fun optionalDoubleToString(d: Double?): String {
        return if (d is Double) {
            String.format("%.2f", d)
        } else {
            "not applicable"
        }
    }

    private fun List<Double>.averageOrNull() =
            if (this.isEmpty())  null
            else this.average()

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val allReviews = ReviewManager.reviewResults()
        val completedReviews = allReviews.filterNotNull()
        val text = StringBuilder()
        text.append("<html>")
        text.append("<b>Summary</b><br><br>")
        text.append("Cards reviewed<br>")
        val totalNumberOfReviews = completedReviews.size.toLong()
        val (correctReviews, incorrectReviews ) = completedReviews.partition { it.wasSuccess() }
        text.append("total: $totalNumberOfReviews <br>")
        text.append("correctly answered: ${correctReviews.size}<br>")
        text.append("incorrectly answered: ${incorrectReviews.size}<br>")
        text.append("<br><br>")
        text.append("time needed for answering<br>")
        val averageTime = completedReviews.map { it.thinkingTime }.averageOrNull()
        text.append("average time: ${optionalDoubleToString(averageTime)}<br>")

        val averageCorrectTime = correctReviews.map { it.thinkingTime }.averageOrNull()
        text.append("average time per correct card: ${optionalDoubleToString(averageCorrectTime)}<br>")
        val averageIncorrectTime = incorrectReviews.map { it.thinkingTime }.averageOrNull()
        text.append("average time per incorrect card: ${optionalDoubleToString(averageIncorrectTime)}<br>")
        text.append("</html>")
        report.text = text.toString()
        val cardLayout = buttonPanel.layout as CardLayout
        if (DeckManager.currentDeck!!.reviewableCardList().isEmpty()) {
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
