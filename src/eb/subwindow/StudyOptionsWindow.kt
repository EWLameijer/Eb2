package eb.subwindow

import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.event.KeyEvent

import eb.data.DeckManager
import eb.eventhandling.BlackBoard
import eb.eventhandling.Listener
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.mainwindow.MainWindowState
import eb.utilities.ProgrammableAction
import eb.utilities.Utilities
import eb.utilities.doNothing
import eb.utilities.uiElements.LabelledTextField
import eb.utilities.uiElements.TimeInputElement
import javax.swing.*

/**
 * The window in which the user can set how he/she wants to study; like which
 * time to take before the initial repetition, or what scheme repetitions will
 * have (every day, or with increasing intervals, or whatever).
 *
 * @author Eric-Wubbo Lameijer
 */
class StudyOptionsWindow : JFrame(), Listener {

    // Button that closes this window, not saving any changes made.
    private val cancelButton = JButton("Discard unsaved changes and close")

    // Button that restores the defaults to those of Eb.
    private val loadEbDefaultsButton = JButton("Load Eb's default values")

    // Button that reloads the current settings of the deck (undoing non-saved
    // changes).
    private val loadCurrentDeckSettingsButton = JButton("Load settings of current deck")

    // Button that sets the study settings of the deck to the values currently
    // displayed in this window.
    private val setToTheseValuesButton = JButton("Set study parameters of this deck to these values")

    // Input element that allows users to view and set the interval between the
    // creation of the card and the first time it is put up for review.
    private val initialIntervalBox: TimeInputElement

    private val sizeOfReview: LabelledTextField

    private val timeToWaitAfterCorrectReview: TimeInputElement

    private val lengtheningFactor: LabelledTextField

    private val targetedSuccessPercentage: LabelledTextField

    private val timeToWaitAfterIncorrectReview: TimeInputElement

    init {
        val studyOptions = DeckManager.currentDeck().studyOptions
        initialIntervalBox = TimeInputElement("Initial review after", studyOptions.initialInterval)
        sizeOfReview = LabelledTextField(
            "number of cards per reviewing session",
            studyOptions.reviewSessionSize.toString(), 3, 0
        )
        timeToWaitAfterCorrectReview = TimeInputElement(
            "Time to wait for re-reviewing remembered card:", studyOptions.rememberedInterval
        )
        lengtheningFactor = LabelledTextField(
            "after each successful review, increase review time by a factor",
            Utilities.toRegionalString(studyOptions.lengtheningFactor.toString()), 5, 2
        )
        timeToWaitAfterIncorrectReview = TimeInputElement(
            "Time to wait for re-reviewing forgotten card:", studyOptions.forgottenInterval
        )
        targetedSuccessPercentage = LabelledTextField(
            "Strive for this percentage successful reviews (between 80% and 90% likely best)",
            Utilities.toRegionalString(studyOptions.idealSuccessPercentage.toString()), 5, 2
        )
    }

    // Updates the title of the frame in response to changes to indicate to the user whether there are unsaved changes.
    private fun updateFrame() {
        val guiStudyOptions = gatherUIDataIntoStudyOptionsObject()
        val deckStudyOptions = DeckManager.currentDeck().studyOptions
        val title = "Study Options" +
                if (guiStudyOptions == deckStudyOptions) " - no unsaved changes"
                else " - UNSAVED CHANGES"

        setTitle(title)
    }

    private fun loadSettings(settings: StudyOptions) {
        initialIntervalBox.interval = settings.initialInterval
        sizeOfReview.setContents(settings.reviewSessionSize)
        timeToWaitAfterCorrectReview.interval = settings.rememberedInterval
        lengtheningFactor.setContents(settings.lengtheningFactor)
        timeToWaitAfterIncorrectReview.interval = settings.forgottenInterval
        targetedSuccessPercentage.setContents(settings.idealSuccessPercentage)
    }

    private fun loadEbDefaults() = loadSettings(StudyOptions())

    private fun loadCurrentDeckSettings() = loadSettings(DeckManager.currentDeck().studyOptions)

    //  Collects the data from the GUI, and packages it nicely into a StudyOptions object.
    private fun gatherUIDataIntoStudyOptionsObject() = StudyOptions(
        initialIntervalBox.interval,
        Utilities.stringToInt(sizeOfReview.contents()),
        timeToWaitAfterCorrectReview.interval,
        timeToWaitAfterIncorrectReview.interval,
        Utilities.stringToDouble(lengtheningFactor.contents())!!,
        Utilities.stringToDouble(targetedSuccessPercentage.contents())!!
    )

    private fun saveSettingsToDeck() {
        DeckManager.setStudyOptions(gatherUIDataIntoStudyOptionsObject())
        BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, MainWindowState.REACTIVE.name))
        updateFrame() // Should be set to 'no unsaved changes' again.
    }

    // Initializes the study options window, performing those actions which are only permissible (for a
    // nullness checker) after the window has been created.
    internal fun init() {
        layout = BorderLayout()

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel") //$NON-NLS-1$
        getRootPane().actionMap.put("Cancel", ProgrammableAction { dispose() })
        bindActionsToButtons()
        BlackBoard.register(this, UpdateType.INPUTFIELD_CHANGED)

        // Then create two panels: one for setting the correct values for the study
        // options, and one to contain the reset/confirm/reload etc. buttons.
        val settingsPane = initSettingsPane()
        add(settingsPane, BorderLayout.NORTH)
        val buttonsPane = initButtonsPane()
        add(buttonsPane, BorderLayout.SOUTH)

        setSize(700, 400)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        updateFrame()
        isVisible = true
    }

    private fun bindActionsToButtons() {
        cancelButton.addActionListener { dispose() }
        loadCurrentDeckSettingsButton.addActionListener { loadCurrentDeckSettings() }
        loadEbDefaultsButton.addActionListener { loadEbDefaults() }
        setToTheseValuesButton.addActionListener { saveSettingsToDeck() }
    }

    private fun initSettingsPane(): JPanel {
        val settingsPane = JPanel().apply {
            layout = BorderLayout()
        }

        // now fill the panes
        val settingsBox = Box.createVerticalBox().apply {
            add(initialIntervalBox)
            add(sizeOfReview)
            add(timeToWaitAfterCorrectReview)
            add(lengtheningFactor)
            add(timeToWaitAfterIncorrectReview)
            add(targetedSuccessPercentage)
        }
        settingsPane.add(settingsBox, BorderLayout.NORTH)
        return settingsPane
    }

    private fun initButtonsPane(): JPanel {
        val buttonsPane = JPanel().apply {
            add(cancelButton)
            add(loadEbDefaultsButton)
            add(loadCurrentDeckSettingsButton)
            add(setToTheseValuesButton)

            layout = GridLayout(2, 2)
        }
        return buttonsPane
    }

    override fun respondToUpdate(update: Update) =
        if (update.type == UpdateType.INPUTFIELD_CHANGED) updateFrame()
        else doNothing

    companion object {
        // Displays the study options window. In order to pacify the nullness checker,
        // separates creation and display of the window.
        fun display() = StudyOptionsWindow().init()
    }
}


