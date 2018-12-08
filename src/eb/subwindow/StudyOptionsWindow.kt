package eb.subwindow

import java.awt.BorderLayout
import java.awt.Container
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
import eb.utilities.ui_elements.LabelledComboBox
import eb.utilities.ui_elements.LabelledTextField
import eb.utilities.ui_elements.TimeInputElement
import javax.swing.*

/**
 * The window in which the user can set how he/she wants to study; like which
 * time to take before the initial repetition, or what scheme repetitions will
 * have (every day, or with increasing intervals, or whatever).
 *
 * @author Eric-Wubbo Lameijer
 */
/**
 * @author Eric-Wubbo Lameijer
 */
class StudyOptionsWindow
/**
 * Creates a new Study Options window.
 */
private constructor() : JFrame(), Listener {

    // Button that closes this window, not saving any changes made.
    private val m_cancelButton: JButton

    // Button that restores the defaults to those of Eb.
    private val m_loadEbDefaultsButton: JButton

    // Button that reloads the current settings of the deck (undoing non-saved
    // changes).
    private val m_loadCurrentDeckSettingsButton: JButton

    // Button that sets the study settings of the deck to the values currently
    // displayed in this window.
    private val m_setToTheseValuesButton: JButton

    // Input element that allows users to view and set the interval between the
    // creation of the card and the first time it is put up for review.
    private val m_initialIntervalBox: TimeInputElement

    private val m_sizeOfReview: LabelledTextField

    private val m_timeToWaitAfterCorrectReview: TimeInputElement

    private val m_lengtheningFactor: LabelledTextField

    private val m_timeToWaitAfterIncorrectReview: TimeInputElement


    init {
        val studyOptions = DeckManager.currentDeck!!.studyOptions
        m_initialIntervalBox = TimeInputElement.createInstance(
                "Initial review after", studyOptions.initialInterval)
        m_sizeOfReview = LabelledTextField("number of cards per reviewing session",
                studyOptions.reviewSessionSize.toString(), 3, 0)
        m_timeToWaitAfterCorrectReview = TimeInputElement.createInstance(
                "Time to wait for re-reviewing remembered card:", studyOptions.rememberedInterval)
        m_lengtheningFactor = LabelledTextField(
                "after each successful review, increase review time by a factor",
                studyOptions.lengtheningFactor.toString(), 5, 2)
        m_timeToWaitAfterIncorrectReview = TimeInputElement.createInstance(
                "Time to wait for re-reviewing forgotten card:", studyOptions.forgottenInterval)

        m_cancelButton = JButton("Discard unsaved changes and close")
        m_loadEbDefaultsButton = JButton("Load Eb's default values")
        m_loadCurrentDeckSettingsButton = JButton("Load settings of current deck")
        m_setToTheseValuesButton = JButton("Set study parameters of this deck to these values")

    }// preconditions: none (default constructor...)

    /**
     * Updates the title of the frame in response to changes to indicate to the
     * user whether there are unsaved changes.
     */
    private fun updateFrame() {
        // preconditions: none. Is by definition only called when the object
        // has been constructed already.
        val guiStudyOptions = gatherUIDataIntoStudyOptionsObject()
        var title = "Study Options"
        val deckStudyOptions = DeckManager.currentDeck!!.studyOptions
        if (guiStudyOptions == deckStudyOptions) {
            title += " - no unsaved changes"
        } else {
            title += " - UNSAVED CHANGES"
        }
        setTitle(title)
        // postconditions: none. Simply changes the frame's title.
    }

    /**
     * Closes the frame, removing all its contents. NOTE: EVIL DUPLICATION. CAN I
     * AVOID THAT?
     */
    private fun close() {
        // preconditions: none
        this.dispose()
        // postconditions: none
    }

    private fun loadSettings(settings: StudyOptions) {
        m_initialIntervalBox.interval = settings.initialInterval
        m_sizeOfReview.setContents(settings.reviewSessionSize)
        m_timeToWaitAfterCorrectReview.interval = settings.rememberedInterval
        m_lengtheningFactor.setContents(settings.lengtheningFactor)
        m_timeToWaitAfterIncorrectReview.interval = settings.forgottenInterval
    }

    /**
     * Loads Eb's default values.
     */
    private fun loadEbDefaults() {
        // preconditions: none
        loadSettings(StudyOptions())
        // postconditions: none (should have worked)
    }

    /**
     * Loads the study options settings of the current deck.
     */
    private fun loadCurrentDeckSettings() {
        // preconditions: none
        loadSettings(DeckManager.currentDeck!!.studyOptions)
        // postconditions: none
    }

    /**
     * Collects the data from the GUI, and packages it nicely into a StudyOptions
     * object.
     *
     * @return a StudyOptions object reflecting the settings created in this
     * window's GUI.
     */
    private fun gatherUIDataIntoStudyOptionsObject(): StudyOptions {
        return StudyOptions(m_initialIntervalBox.interval!!,
                Utilities.stringToInt(m_sizeOfReview.contents),
                m_timeToWaitAfterCorrectReview.interval!!,
                m_timeToWaitAfterIncorrectReview.interval!!,
                Utilities.stringToDouble(m_lengtheningFactor.contents)!!)
    }

    /**
     * Saves the settings to the deck.
     */
    private fun saveSettingsToDeck() {
        val guiStudyOptions = gatherUIDataIntoStudyOptionsObject()
        DeckManager.setStudyOptions(guiStudyOptions)
        BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED,
                MainWindowState.REACTIVE.name))
        updateFrame() // Should be set to 'no unsaved changes' again.
    }

    /**
     * Initializes the study options window, performing those actions which are
     * only permissible (for a nullness checker) after the window has been
     * created.
     */
    internal fun init() {

        layout = BorderLayout()

        // first: make the buttons do something
        m_cancelButton.addActionListener { close() }
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel") //$NON-NLS-1$
        getRootPane().actionMap.put("Cancel",
                ProgrammableAction { close() })
        m_loadCurrentDeckSettingsButton
                .addActionListener { loadCurrentDeckSettings() }
        m_loadEbDefaultsButton.addActionListener { loadEbDefaults() }
        m_setToTheseValuesButton.addActionListener { saveSettingsToDeck() }
        BlackBoard.register(this, UpdateType.INPUTFIELD_CHANGED)

        // Then create two panels: one for setting the correct values for the study
        // options, and one to contain the reset/confirm/reload etc. buttons.
        val settingsPane = JPanel()
        val buttonsPane = JPanel()

        // Give the panes appropriate layouts
        settingsPane.layout = BorderLayout()

        buttonsPane.layout = GridLayout(2, 2)

        // now fill the panes
        val settingsBox = Box.createVerticalBox()
        settingsBox.add(m_initialIntervalBox)
        settingsBox.add(m_sizeOfReview)
        settingsBox.add(m_timeToWaitAfterCorrectReview)
        settingsBox.add(m_lengtheningFactor)
        settingsBox.add(m_timeToWaitAfterIncorrectReview)
        settingsPane.add(settingsBox, BorderLayout.NORTH)

        buttonsPane.add(m_cancelButton)
        buttonsPane.add(m_loadEbDefaultsButton)
        buttonsPane.add(m_loadCurrentDeckSettingsButton)
        buttonsPane.add(m_setToTheseValuesButton)

        add(settingsPane, BorderLayout.NORTH)
        add(buttonsPane, BorderLayout.SOUTH)

        setSize(700, 400)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        updateFrame()
        isVisible = true
    }

    override fun respondToUpdate(update: Update) {
        if (update.type == UpdateType.INPUTFIELD_CHANGED) {
            updateFrame()
        }
    }

    companion object {

        // Automatically generated serialVersionUID.
        private val serialVersionUID = -907266672997684012L

        /**
         * Displays the study options window. In order to pacify the nullness checker,
         * separates creation and display of the window.
         */
        fun display() {
            val studyOptionsWindow = StudyOptionsWindow()
            studyOptionsWindow.init()
            // postconditions: none
        }
    }
}
