package eb.mainwindow.panels

import eb.Eb
import eb.Personalisation
import eb.data.DeckManager
import eb.eventhandling.BlackBoard
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.mainwindow.MainWindowState
import eb.mainwindow.reviewing.ReviewManager
import eb.utilities.Utilities
import eb.utilities.pluralize
import java.awt.BorderLayout
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JMenuBar
import javax.swing.JPanel

class InformationPanel : JPanel() {
    private val messageLabel = JLabel()

    // button the user can press to start reviewing. Only visible if the user for some reason decides to not review
    // cards yet (usually by having one rounds of review, and then stopping the reviewing)
    private val startReviewingButton = JButton().apply {
        isVisible = false
        text = "Review now"
        addActionListener {
            ReviewManager.resetTimers()
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, MainWindowState.REACTIVE.name))
        }
    }

    init{
        layout = BorderLayout()
        add(messageLabel, BorderLayout.CENTER)
        add(startReviewingButton, BorderLayout.SOUTH)
    }

    private fun deckSizeMessage() =
        "The current deck contains ${"card".pluralize(DeckManager.currentDeck().cardCollection.getTotal())}."

    private fun totalReviewTimeMessage() =
        "Reviewing has taken a total time of ${Utilities.durationToString(DeckManager.currentDeck().totalStudyTime())}"

    //Returns text indicating how long it will be to the next review
    private fun timeToNextReviewMessage() = buildString {
        val currentDeck = DeckManager.currentDeck()
        val numCards = currentDeck.cardCollection.getTotal()
        if (numCards > 0) {
            append("Time till next review: ")
            val timeUntilNextReviewAsDuration = currentDeck.timeUntilNextReview()
            val timeUntilNextReviewAsText = Utilities.durationToString(timeUntilNextReviewAsDuration)
            append(timeUntilNextReviewAsText)
            val nextReviewInstant = LocalDateTime.now() + timeUntilNextReviewAsDuration
            val formatter = DateTimeFormatter.ofPattern(" (yyyy-MM-dd HH:mm:ss)")
            val formattedNextReviewInstant = nextReviewInstant.format(formatter)
            append(formattedNextReviewInstant)
            append("<br>")
            startReviewingButton.isVisible = timeUntilNextReviewAsDuration.isNegative
        } else {
            startReviewingButton.isVisible = false
        }
    }

    // Updates the message label (the information inside the main window, like time to next review)
    fun updateMessageLabel() {
        messageLabel.text = buildString {
            append("<html>")
            append(deckSizeMessage())
            append("<br>")
            append(totalReviewTimeMessage())
            append("<br>")
            append(timeToNextReviewMessage())
            append(uiCommands)
            append("<br>")
            append(Personalisation.deckShortcuts())
            append("</html>")
        }
    }

    //Returns the commands of the user interface as a string, which can be used to instruct the user on Eb's use.
    private val uiCommands = ("""<br>
            Ctrl+N to add a card.<br>
            Ctrl+Q to quit.<br>
            Ctrl+K to create a deck.<br>
            Ctrl+L to load a deck.<br>
            Ctrl+T to view/edit the study options.<br>
            Ctrl+R to view/edit the deck archiving options.<br>""".trimIndent())





}