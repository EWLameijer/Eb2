package eb.mainwindow.reviewing

import eb.data.Card
import java.awt.CardLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ComponentListener
import java.awt.event.KeyEvent
import java.beans.EventHandler

import eb.data.DeckManager
import eb.data.Review
import eb.eventhandling.BlackBoard
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.mainwindow.MainWindowState
import eb.subwindow.CardEditingManager
import eb.utilities.Hint
import eb.utilities.ProgrammableAction
import eb.utilities.Utilities.createKeyPressSensitiveButton
import eb.utilities.asTwoDigitString
import java.time.Duration
import java.time.Instant
import java.time.Year
import java.time.ZoneOffset
import java.time.temporal.TemporalField
import java.util.Calendar.YEAR
import javax.swing.*

/**
 * The panel used to review cards (shows front, on clicking "Show Answer" the answer is shown, after which the user
 * can confirm or deny that he/she knew the correct answer).
 *
 * @author Eric-Wubbo Lameijer
 */
class ReviewPanel : JPanel() {

    private val frontOfCardPanel = CardPanel()
    private val backOfCardPanel = CardPanel()
    private val situationalButtonPanel = JPanel()
    private val fixedButtonPanel = JPanel()
    private val reviewHistoryArea = JTextArea("test")

    init {
        this.isFocusable = true
        addComponentListener(
            EventHandler.create(
                ComponentListener::class.java, this,
                "requestFocusInWindow", null, "componentShown"
            )
        )

        layout = GridBagLayout()

        initFrontOfCardPanel()
        initBackOfCardPanel()
        initSituationalPanel() // Show answer (if only front is shown), or Remembered/Forgotten (if back is shown too, so user can check his learning)
        initFixedButtonPanel()
        initSidePanel()
    }

    private fun initFixedButtonPanel() {
        // the fixed button panel contains buttons that need to be visible always
        val fixedButtonPanelConstraints = GridBagConstraints().apply {
            gridx = 3
            gridy = 4
            gridwidth = 1
            gridheight = 1
            weightx = 1.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH
        }
        fixedButtonPanel.add(createKeyPressSensitiveButton("Edit card", 'e') { editCard() })
        fixedButtonPanel.add(createKeyPressSensitiveButton("Delete card", 'd', ::deleteCard))
        fixedButtonPanel.add(createKeyPressSensitiveButton("View score", 'v', ::showScore))
        add(fixedButtonPanel, fixedButtonPanelConstraints)
    }

    private fun showScore() {
        BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, MainWindowState.SUMMARIZING.name))
    }

    private fun initSituationalPanel() {
        // for buttons that depend on the situation, like when the back of the card
        // is shown or when it is not yet shown.
        val buttonPanelForHiddenBack = JPanel().apply {
            layout = FlowLayout()
            add(createKeyPressSensitiveButton("Show Answer", 's', ::showAnswer))
        }

        val buttonPanelForShownBack = JPanel().apply {
            add(createKeyPressSensitiveButton("Remembered", 'r') { registerAnswer(true) })
            add(createKeyPressSensitiveButton("Forgotten", 'f') { registerAnswer(false) })
        }
        val situationalButtonPanelConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 4
            gridwidth = 3
            gridheight = 1
            weightx = 3.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH
        }
        situationalButtonPanel.layout = CardLayout()
        situationalButtonPanel.add(buttonPanelForHiddenBack, HIDDEN_ANSWER)
        situationalButtonPanel.add(buttonPanelForShownBack, SHOWN_ANSWER)
        situationalButtonPanel.background = Color.GREEN
        add(situationalButtonPanel, situationalButtonPanelConstraints)
    }

    private fun initBackOfCardPanel() {
        val backOfCardConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 2
            gridwidth = 4
            gridheight = 2
            weightx = 4.0
            weighty = 2.0
            fill = GridBagConstraints.BOTH
        }
        backOfCardPanel.background = Color.YELLOW
        add(backOfCardPanel, backOfCardConstraints)
    }

    private fun initFrontOfCardPanel() {
        val frontOfCardConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            gridwidth = 4
            gridheight = 2
            weightx = 4.0
            weighty = 2.0
            fill = GridBagConstraints.BOTH
        }
        frontOfCardPanel.background = Color.PINK
        add(frontOfCardPanel, frontOfCardConstraints)
    }

    private fun initSidePanel() {
        // panel, to be used in future to show successful/unsuccessful cards
        // for now hidden?
        val sidePanelConstraints = GridBagConstraints().apply {
            gridx = 4
            gridy = 0
            gridwidth = 1
            gridheight = 5
            fill = GridBagConstraints.BOTH
            weightx = 1.0
            weighty = 5.0
        }
        val sidePanel = JPanel()

        sidePanel.add(reviewHistoryArea)
        sidePanel.background = Color.RED
        add(sidePanel, sidePanelConstraints)
    }

    private fun editCard() = CardEditingManager(false, ReviewManager.currentCard())

    private fun deleteCard() {
        val choice = JOptionPane.showConfirmDialog(
            backOfCardPanel,
            "Delete this card?", "Delete this card?", JOptionPane.OK_CANCEL_OPTION
        )
        if (choice == JOptionPane.OK_OPTION)
            DeckManager.currentDeck().cardCollection.removeCard(ReviewManager.currentCard()!!)
    }

    private fun registerAnswer(wasRemembered: Boolean) {
        showPanel(HIDDEN_ANSWER)
        ReviewManager.wasRemembered(wasRemembered)
        repaint()
    }

    private fun showAnswer() {
        showPanel(SHOWN_ANSWER)
        ReviewManager.showAnswer()
        repaint()
    }

    private fun showPanel(panelName: String) {
        val cardLayout = situationalButtonPanel.layout as CardLayout
        cardLayout.show(situationalButtonPanel, panelName)
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        frontOfCardPanel.setText(ReviewManager.currentFront())
    }

    fun refresh() = repaint()

    fun updatePanels(frontText: String, backText: String, showAnswer: Boolean) {
        frontOfCardPanel.setText(frontText)
        backOfCardPanel.setText(backText)
        showPanel(if (showAnswer) SHOWN_ANSWER else HIDDEN_ANSWER)
        updateSidePanel(frontText, showAnswer)

    }

    private fun Instant.getDateTimeString(): String {
        val zonedDateTime = this.atZone(ZoneOffset.UTC)
        val year = zonedDateTime.year
        val month = zonedDateTime.monthValue.asTwoDigitString()
        val day = zonedDateTime.dayOfMonth.asTwoDigitString()
        val hour = zonedDateTime.hour.asTwoDigitString()
        val minute = zonedDateTime.minute.asTwoDigitString()
        return "$year-$month-$day $hour:$minute"
    }

    private fun Card.waitingTimeBeforeRelevantReview(reviewIndex: Int) =
        Duration.between(reviewInstant(reviewIndex - 1), reviewInstant(reviewIndex)).toHours()

    private fun Card.reviewInstant(reviewIndex: Int): Instant =
        if (reviewIndex >= 0) getReviews()[reviewIndex].instant else creationInstant

    private fun updateSidePanel(frontText: String, showAnswer: Boolean) {
        reviewHistoryArea.isVisible = showAnswer
        val card = DeckManager.currentDeck().cardCollection.getCardWithFront(Hint(frontText))!!
        reviewHistoryArea.text = buildString {
            val createdDateTime = card.creationInstant.getDateTimeString()
            append("Card created: $createdDateTime\n")
            for (index in card.getReviews().indices)
                append(getReviewDataAsString(index, card))
            val hoursSinceLastView = getHoursSinceLastView(card)
            val hoursAndDays = toDayHourString(hoursSinceLastView)
            append("$hoursAndDays since last view")
        }
    }

    private fun getHoursSinceLastView(card: Card): Long {
        val indexOfLastView = card.getReviews().size - 1 // -1 if no reviews have taken place
        val lastViewInstant = card.reviewInstant(indexOfLastView)
        return Duration.between(lastViewInstant, Instant.now()).toHours()
    }

    private fun toDayHourString(durationInHours: Long) : String {
        val durationDays = durationInHours / 24
        val durationHours = durationInHours % 24
        return "$durationDays d, $durationHours h"
    }

    private fun getReviewDataAsString(index: Int, card: Card) = buildString {
        val review = card.getReviews()[index]
        append("${index + 1}: ")
        val reviewDateTime = review.instant.getDateTimeString()
        append("$reviewDateTime ")
        append(if (review.wasSuccess) "S" else "F")
        val durationInHours = card.waitingTimeBeforeRelevantReview(index)
        val dayHourString = toDayHourString(durationInHours)
        append(" ($dayHourString)\n")
    }

    companion object {
        private const val HIDDEN_ANSWER = "HIDDEN_ANSWER"
        private const val SHOWN_ANSWER = "SHOWN_ANSWER"
    }
}
