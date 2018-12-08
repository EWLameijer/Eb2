package eb.mainwindow

import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.WindowListener
import java.beans.EventHandler
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.ArrayList
import java.util.Optional
import java.util.logging.Logger

import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.Timer

import eb.data.Deck
import eb.data.DeckManager
import eb.eventhandling.BlackBoard
import eb.eventhandling.Listener
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.mainwindow.reviewing.ReviewManager
import eb.mainwindow.reviewing.ReviewPanel
import eb.subwindow.ArchivingSettingsWindow
import eb.subwindow.CardEditingManager
import eb.subwindow.StudyOptions
import eb.subwindow.StudyOptionsWindow
import eb.utilities.TimeInterval
import eb.utilities.Utilities
import eb.utilities.isValidIdentifier

/**
 * The main window of Eb.
 *
 * @author Eric-Wubbo Lameijer
 */
class MainWindow
/**
 * MainWindow constructor. Sets title of the window, and creates some widgets.
 */
internal constructor() : JFrame(PROGRAM_NAME), Listener {

    // The label that has to be shown if there is no card that needs to be
    // reviewed currently, or if there is an error. Is the alternative to
    // the regular "reviewing" window, which should be active most of the
    // time.
    private val m_messageLabel: JLabel

    // the initial state of the main window
    private var m_state = MainWindowState.REACTIVE

    // button the user can press to start reviewing. Only visible if the user for some reason
    // decides to not review cards yet (usually by having one rounds of review, and then
    // stopping the reviewing)
    private val m_startReviewingButton = JButton()

    // Contains the REVIEWING_PANEL and the INFORMATION_PANEL, using a CardLayout.
    private val m_modesContainer: JPanel

    // The reviewing panel
    private val m_reviewPanel: ReviewPanel = ReviewPanel()

    // To regularly update how long it is until the next reviewing session
    private var m_messageUpdater: Timer? = null

    /**
     * Returns the commands of the user interface as a string, which can be used
     * for example to instruct the user on Eb's use.
     *
     * @return the commands of the user interface
     */
    private// preconditions: none
    // postconditions: none
    val uiCommands: String
        get() = ("<br>Ctrl+N to add a card.<br>" + "Ctrl+Q to quit.<br>"
                + "Ctrl+K to create a deck.<br>" + "Ctrl+L to load a deck.<br>"
                + "Ctrl+T to view/edit the study options.<br>"
                + "Ctrl+R to view/edit the deck archiving options.<br>")

    /**
     * Returns a message about the size of the current deck.
     *
     * @return a message about the size of the current deck.
     */
    private// preconditions: none
    // postconditions: none
    val deckSizeMessage: String
        get() = ("The current deck contains "
                + DeckManager.currentDeck?.cards?.getTotal() + " cards.")

    /**
     * Returns text indicating how long it will be to the next review
     *
     * @return text indicating the time to the next review
     */
    internal// no cards
    val timeToNextReviewMessage: String
        get() {
            val message = StringBuilder()
            val currentDeck = DeckManager.currentDeck
            val numCards = currentDeck?.cards?.getTotal() ?: 0
            if (numCards > 0) {
                message.append("Time till next review: ")
                val timeUntilNextReviewAsDuration = currentDeck?.timeUntilNextReview()
                val timeUntilNextReviewAsText = Utilities
                        .durationToString(timeUntilNextReviewAsDuration!!)
                message.append(timeUntilNextReviewAsText)
                message.append("<br>")
                m_startReviewingButton.isVisible = timeUntilNextReviewAsDuration.isNegative
            } else {
                m_startReviewingButton.isVisible = false
            }
            return message.toString()
        }

    init {
        m_messageLabel = JLabel()
        m_modesContainer = JPanel()
        m_modesContainer.layout = CardLayout()
    }// preconditions: none

    /**
     * Updates the message label (the information inside the main window, like time to next review)
     */
    internal fun updateMessageLabel() {
        val message = StringBuilder()
        message.append("<html>")
        message.append(deckSizeMessage)
        message.append("<br>")
        message.append(timeToNextReviewMessage)
        message.append(uiCommands)
        message.append("</html>")
        m_messageLabel.text = message.toString()
    }

    /**
     * Updates the title of the window, which contains information like the number of cards in the deck
     */
    internal fun updateWindowTitle() {
        val currentDeck = DeckManager.currentDeck!!
        val numReviewingPoints = currentDeck.cards.getReviewingPoints()

        val numReviewableCards = currentDeck.reviewableCardList().size
        var title = ("Eb: " + currentDeck.name + " ("
                + Utilities.pluralText(numReviewableCards, "card")
                + " to be reviewed in total")
        if (m_state == MainWindowState.REVIEWING) {
            title += (", " + Utilities
                    .pluralText(ReviewManager.cardsToGoYet(), "card")
                    + " yet to be reviewed in the current session")
        }
        val numCards = currentDeck.cards.getTotal()
        title += (", " + Utilities.pluralText(numCards, "card") + " in deck, "
                + Utilities.pluralText(numReviewingPoints, "point") + ")")

        this.title = title
    }

    /**
     * Updates the text on the review button
     */
    internal fun updateReviewButtonText() {
        m_startReviewingButton.text = "Review now"
    }

    /**
     * Gives the message label its correct (possibly updated) value.
     */
    internal fun updateOnScreenInformation() {
        updateMessageLabel()
        updateWindowTitle()
        updateReviewButtonText()
    }

    internal fun showCorrectPanel() {
        when (m_state) {
            MainWindowState.REACTIVE -> showReactivePanel()
            MainWindowState.INFORMATIONAL -> showInformationPanel()
            MainWindowState.REVIEWING -> showReviewingPanel()
            MainWindowState.SUMMARIZING -> showSummarizingPanel()
            MainWindowState.WAITING_FOR_TIMER_START -> switchToPanel(TIMED_REVIEW_START_PANEL_ID)
        }

    }

    private fun mustReviewNow(): Boolean {
        // case 1: there are no cards in the deck - so nothing to review either
        if (DeckManager.currentDeck!!.cards.getTotal() == 0) {
            return false
        } else {
            val timeUntilNextReviewAsDuration = DeckManager.currentDeck!!.timeUntilNextReview()
            return timeUntilNextReviewAsDuration.isNegative
        }
    }

    /**
     * Opens the study options window, at which one can set the study options for
     * a deck (after which interval the first card should be studied, etc.)
     */
    private fun openStudyOptionsWindow() {
        // preconditions: none (this method will simply be called when the user
        // presses the correct button).

        StudyOptionsWindow.display()

        // postconditions: none (the user does not have to do anything with the
        // settings)
    }

    private fun createDeck() {
        do {
            val deckName = JOptionPane.showInputDialog(null,
                    "Please give name " + "for deck to be created")
            if (deckName == null) {
                // cancel button has been pressed
                return
            } else {
                if (!deckName.isValidIdentifier) {
                    JOptionPane.showMessageDialog(null, "Sorry, \"" + deckName
                            + "\" is not a valid name for a deck. Please choose another name.")
                } else if (Deck.Companion.getDeckFileHandle(deckName).exists()) {
                    JOptionPane.showMessageDialog(null, "Sorry, the deck \"" + deckName
                            + "\" already exists. Please choose another name.")
                } else {
                    // The deckname is valid!
                    m_messageUpdater!!.stop()
                    DeckManager.createDeckWithName(deckName)
                    // reset window
                    m_state = MainWindowState.REACTIVE
                    updateOnScreenInformation()
                    m_messageUpdater!!.start()

                    return
                }
            }
        } while (true)
    }

    /**
     * Performs the proper buildup of the window (after the construction has
     * initialized all components properly).
     */
    private fun init() {
        // add menu
        val fileMenu = JMenu("File")
        val createItem = JMenuItem("Create deck")
        createItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_K, ActionEvent.CTRL_MASK)
        createItem.addActionListener { createDeck() }
        fileMenu.add(createItem)
        val loadItem = JMenuItem("Load deck")
        loadItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK)
        loadItem.addActionListener { loadDeck() }
        fileMenu.add(loadItem)
        val restoreItem = JMenuItem("Restore from archive")
        restoreItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK)
        restoreItem.addActionListener { restoreDeck() }
        fileMenu.add(restoreItem)
        val quitItem = JMenuItem("Quit")
        quitItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK)
        quitItem.addActionListener { saveAndQuit() }
        fileMenu.add(quitItem)
        val deckManagementMenu = JMenu("Manage Deck")
        val addCardItem = JMenuItem("Add Card")
        addCardItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK)
        addCardItem.addActionListener { CardEditingManager() }
        deckManagementMenu.add(addCardItem)
        val studyOptionsItem = JMenuItem("Study Options")
        studyOptionsItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK)
        studyOptionsItem.addActionListener { openStudyOptionsWindow() }
        deckManagementMenu.add(studyOptionsItem)
        val archivingOptionsItem = JMenuItem(
                "Deck Archiving Options")
        archivingOptionsItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK)
        archivingOptionsItem.addActionListener { openDeckArchivingWindow() }
        deckManagementMenu.add(archivingOptionsItem)
        val mainMenuBar = JMenuBar()
        mainMenuBar.add(fileMenu)
        mainMenuBar.add(deckManagementMenu)
        jMenuBar = mainMenuBar

        // add message label (or show cards-to-be-reviewed)

        val informationPanel = createInformationPanel()
        m_modesContainer.add(informationPanel, INFORMATION_PANEL_ID)
        m_modesContainer.add(m_reviewPanel, REVIEW_PANEL_ID)
        ReviewManager.setPanel(m_reviewPanel)
        val summarizingPanel = SummarizingPanel()
        m_modesContainer.add(summarizingPanel, SUMMARIZING_PANEL_ID)
        val timedReviewStartPanel = TimedReviewStartPanel()
        m_modesContainer.add(timedReviewStartPanel, TIMED_REVIEW_START_PANEL_ID)
        add(m_modesContainer)

        setNameOfLastReviewedDeck()

        showCorrectPanel()

        // now show the window itself.
        setSize(1000, 700)
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        // Instead of using the WindowAdapter class, use the EventHandler utility
        // class to create a class with default noop methods, just overriding the
        // windowClosing.
        addWindowListener(EventHandler.create(WindowListener::class.java, this,
                "saveAndQuit", null, "windowClosing"))
        isVisible = true
        BlackBoard.register(this, UpdateType.PROGRAMSTATE_CHANGED)
        m_messageUpdater = Timer(100) { showCorrectPanel() }
        m_messageUpdater!!.start()
        updateOnScreenInformation()
    }

    private fun restoreDeck() {
        val chooser = JFileChooser()
        val result = chooser.showOpenDialog(this)
        if (result == JFileChooser.CANCEL_OPTION) {
            return
        } else {
            val selectedFile = chooser.selectedFile
            DeckManager.createDeckFromArchive(selectedFile)
        }
    }

    /**
     * Opens a/the Deck archiving options window
     */
    private fun openDeckArchivingWindow() {
        ArchivingSettingsWindow.display()
    }

    private fun setNameOfLastReviewedDeck() {
        val statusFilePath = Paths.get(EB_STATUS_FILE)
        val mostRecentDeckIdentifier = "most_recently_reviewed_deck: "
        val lines: List<String>
        try {
            lines = Files.readAllLines(statusFilePath, Charset.forName("UTF-8"))
            val fileLine = lines.stream()
                    .filter { e -> e.startsWith(mostRecentDeckIdentifier) }.findFirst()
            if (fileLine.isPresent) {
                val deckName = fileLine.get()
                        .substring(mostRecentDeckIdentifier.length)
                DeckManager.setNameOfLastReviewedDeck(deckName)
            }
        } catch (e: IOException) {
            // If input fails, set name to ""
            DeckManager.setNameOfLastReviewedDeck("")
            Logger.getGlobal().info(e.toString() + "")
        }

    }

    private fun loadDeck() {
        do {
            val deckName = JOptionPane.showInputDialog(null,
                    "Please give name for deck to be loaded")
                    ?: // Cancel button pressed
                    return
            if (canDeckBeLoaded(deckName)) {
                m_messageUpdater!!.stop()
                DeckManager.loadDeck(deckName)
                // reset window
                m_state = MainWindowState.REACTIVE
                updateOnScreenInformation()
                m_messageUpdater!!.start()
                return
            }
        } while (true)
    }

    private fun canDeckBeLoaded(deckName: String): Boolean {
        if (!deckName.isValidIdentifier) {
            JOptionPane.showMessageDialog(null, "Sorry, \"" + deckName
                    + "\" is not a valid name for a deck. Please choose another name.")
        } else if (!Deck.Companion.getDeckFileHandle(deckName).exists()) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, the deck \"$deckName\" does not exist yet.")
        } else {
            // we have a valid deck here
            if (DeckManager.canLoadDeck(deckName)) {
                // the only 'happy path' - otherwise false should be returned.
                return true
            } else {
                JOptionPane.showMessageDialog(null,
                        "An error occurred while loading the deck \"" + deckName
                                + "\". It may be an invalid file; possibly try restore it from an archive file?")
            }
        }
        return false
    }

    private fun createInformationPanel(): JPanel {
        val informationPanel = JPanel()
        informationPanel.layout = BorderLayout()
        informationPanel.add(m_messageLabel, BorderLayout.CENTER)
        m_startReviewingButton.isVisible = false
        m_startReviewingButton.addActionListener {
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, MainWindowState.REACTIVE.name))
        }
        informationPanel.add(m_startReviewingButton, BorderLayout.SOUTH)
        return informationPanel
    }

    /**
     * Saves the current deck and its status, and quits Eb.
     */
    fun saveAndQuit() {
        saveEbStatus()
        DeckManager.save()
        dispose()
        System.exit(0)
    }

    private fun saveEbStatus() {
        val lines = ArrayList<String>()
        lines.add("most_recently_reviewed_deck: " + DeckManager.currentDeck!!.name)
        val statusFilePath = Paths.get(EB_STATUS_FILE)
        try {
            Files.write(statusFilePath, lines, Charset.forName("UTF-8"))
        } catch (e: IOException) {
            Logger.getGlobal().info(e.toString() + "")
        }

    }

    private fun switchToPanel(panelId: String) {
        val cardLayout = m_modesContainer.layout as CardLayout
        cardLayout.show(m_modesContainer, panelId)
    }

    private fun showInformationPanel() {
        switchToPanel(INFORMATION_PANEL_ID)
        updateOnScreenInformation()
    }

    /**
     * Shows the reviewing panel
     */
    private fun showReviewingPanel() {
        if (m_state != MainWindowState.REVIEWING) {
            ReviewManager.start(m_reviewPanel)
            m_state = MainWindowState.REVIEWING
        }
        switchToPanel(REVIEW_PANEL_ID)
        updateOnScreenInformation()
    }

    /**
     * Shows the 'reactive' panel, which means the informational panel if no
     * reviews need to be conducted, and the reviewing panel when cards need to be
     * reviewed.
     */
    private fun showReactivePanel() {
        updateOnScreenInformation()
        if (mustReviewNow()) {
            showReviewingPanel()
        } else {
            showInformationPanel()
        }
    }

    override fun respondToUpdate(update: Update) {
        if (update.type == UpdateType.DECK_CHANGED) {
            showCorrectPanel()
        } else if (update.type == UpdateType.PROGRAMSTATE_CHANGED) {
            m_state = MainWindowState.valueOf(update.contents)
            m_reviewPanel.refresh() // there may be new cards to refresh
            updateOnScreenInformation()
            showCorrectPanel()
        } else if (update.type == UpdateType.DECK_SWAPPED) {
            val newState = if (mustReviewNow())
                MainWindowState.REVIEWING
            else
                MainWindowState.REACTIVE
            BlackBoard
                    .post(Update(UpdateType.PROGRAMSTATE_CHANGED, newState.name))
        }
    }

    private fun showSummarizingPanel() {
        switchToPanel(SUMMARIZING_PANEL_ID)
    }

    companion object {

        // Automatically generated ID for serialization (not used).
        private val serialVersionUID = 5327238918756780751L

        // The name of the program.
        private val PROGRAM_NAME = "Eb"

        // IDs of the various panels, necessary for adding them to the CardLayout of the main panel
        private val REVIEW_PANEL_ID = "REVIEWING_PANEL"
        private val INFORMATION_PANEL_ID = "INFORMATION_PANEL"
        private val SUMMARIZING_PANEL_ID = "SUMMARIZING_PANEL"
        private val TIMED_REVIEW_START_PANEL_ID = "TIMER_START_PANEL"

        // the name of the file that contains which deck has been consulted last
        private val EB_STATUS_FILE = "eb_status.txt"

        /**
         * Displays the main window. Necessary since the Checker framework dislikes
         * initializing values and doing things like 'add' in the same method.
         */
        fun display() {
            val mainWindow = MainWindow()
            mainWindow.init()
        }
    }
}
