package eb.mainwindow

import eb.Eb
import eb.analysis.Analyzer
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.event.ActionEvent
import java.awt.event.WindowListener
import java.beans.EventHandler
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.ArrayList

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
import eb.subwindow.StudyOptionsWindow
import eb.utilities.Utilities
import eb.utilities.doNothing
import eb.utilities.isValidIdentifier
import eb.utilities.pluralize
import eb.utilities.log
import java.awt.event.KeyEvent.getExtendedKeyCodeForChar
import kotlin.system.exitProcess

// FUTURE PLANS:
// show history of card in side window!!! (want to know tough cases, so can adapt)
// enable pictures to be shown with cards
// better sorting of repeated cards [what does that mean?]
// ? Allow Eb to run WITHOUT taking up two taskbar slots?

// 2.1.6: Added overview panel so you can see when you are creating a duplicate card before you have fully created it
// 2.1.5: Solved bug where a failed review was not shown properly in the statistics window if it had been reviewed multiple times during a session.
// 2.1.4: Prioritize reviewing of already-reviewed cards (so only new cards skipped if needed). Sorting algorithm should work properly now
// 2.1.3: Solved problems when starting up on other computer, partly due to decimal point differences
// 2.1.2: Ensure that after merging cards in triple mode, the separate merging window is disposed of after it has fulfilled its purpose
// 2.1.1: Prioritize reviewing of known cards. Not yet prioritized for relative delay.
// 2.1.0: Ensure that 'delete this card' works properly with the 3-sided creation window. Also shows card merging more clearly.
// 2.0.9: Fix to make it impossible to open duplicate Eb instances (not sure how it works, though... Possibly a var? not optimized away by compiler).
// .....: ALSO allow multi-line-input to shift focus to the last card, so Ctrl-V Tab Enter should do the trick
// 2.0.8: Makes the 'duplicate card insertion' error more clear, and allows multiline input (copy-pasted text with newline)
// 2.0.7: creates log file so one can check score even if one forgot to write it down...
// 2.0.6. Extra feature: now properly inserts spacing around ,
// 2.0.5: Extra feature: three-sided cards. ALSO: better font
// 2.0.4. Bugfix: should accumulate percentages over one run, not reset score without a deckswap or quit
// 2.0.3. QoL improvement: show current score of reviewing process
// 2.0.2. QoL-improvement: show percentage of cards successfully remembered
// 2.0.1. Bugfix: was able to add cards with the same front

// The name of the program.
private const val PROGRAM_NAME = "Eb"

/**
 * The main window of Eb.
 *
 * @author Eric-Wubbo Lameijer
 */
class MainWindow : JFrame(PROGRAM_NAME), Listener {

    private val EB_STATUS_FILE = "eb_status.txt"
    private val REVIEW_PANEL_ID = "REVIEWING_PANEL"
    private val INFORMATION_PANEL_ID = "INFORMATION_PANEL"
    private val SUMMARIZING_PANEL_ID = "SUMMARIZING_PANEL"

    // The label that has to be shown if there is no card that needs to be reviewed currently, or if there is an error.
    // Is the alternative to the regular "reviewing" window, which should be active most of the time.
    private val messageLabel = JLabel()

    // the initial state of the main window
    private var state = MainWindowState.REACTIVE

    // button the user can press to start reviewing. Only visible if the user for some reason decides to not review
    // cards yet (usually by having one rounds of review, and then stopping the reviewing)
    private val startReviewingButton = JButton().apply {
        isVisible = false
        addActionListener {
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, MainWindowState.REACTIVE.name))
        }
    }

    // Contains the REVIEWING_PANEL and the INFORMATION_PANEL, using a CardLayout.
    private val modesContainer = JPanel()

    // The reviewing panel
    private val reviewPanel: ReviewPanel = ReviewPanel()

    // To regularly update how long it is until the next reviewing session
    private var messageUpdater: Timer? = null

    //Returns the commands of the user interface as a string, which can be used to instruct the user on Eb's use.
    private val uiCommands = ("""<br>
            Ctrl+N to add a card.<br>
            Ctrl+Q to quit.<br>
            Ctrl+K to create a deck.<br>
            Ctrl+L to load a deck.<br>
            Ctrl+T to view/edit the study options.<br>
            Ctrl+R to view/edit the deck archiving options.<br>""".trimIndent())

    init {
        modesContainer.layout = CardLayout()
        startReviewingButton.text = "Review now"
        init()
    }

    private fun deckSizeMessage() =
            "The current deck contains ${"card".pluralize(DeckManager.currentDeck().cardCollection.getTotal())}."

    //Returns text indicating how long it will be to the next review
    private fun timeToNextReviewMessage() = buildString {
        val currentDeck = DeckManager.currentDeck()
        val numCards = currentDeck.cardCollection.getTotal()
        if (numCards > 0) {
            append("Time till next review: ")
            val timeUntilNextReviewAsDuration = currentDeck.timeUntilNextReview()
            val timeUntilNextReviewAsText = Utilities.durationToString(timeUntilNextReviewAsDuration)
            append(timeUntilNextReviewAsText)
            append("<br>")
            startReviewingButton.isVisible = timeUntilNextReviewAsDuration.isNegative
        } else {
            startReviewingButton.isVisible = false
        }
    }

    // Updates the message label (the information inside the main window, like time to next review)
    private fun updateMessageLabel() {
        messageLabel.text = buildString {
            append("<html>")
            append(deckSizeMessage())
            append("<br>")
            append(timeToNextReviewMessage())
            append(uiCommands)
            append("</html>")
        }
    }

    // Updates the title of the window, which contains information like the number of cards in the deck
    private fun updateWindowTitle() {
        val currentDeck = DeckManager.currentDeck()
        val numReviewingPoints = currentDeck.cardCollection.getReviewingPoints()

        val numReviewableCards = currentDeck.reviewableCardList().size
        var title = ("Eb${Eb.VERSION_STRING}: ${currentDeck.name} (${"card".pluralize(numReviewableCards)} to be reviewed in total")
        if (state == MainWindowState.REVIEWING) {
            title += (", ${"card".pluralize(ReviewManager.cardsToGoYet())} yet to be reviewed in the current session")
        }
        val numCards = currentDeck.cardCollection.getTotal()
        title += (", ${"card".pluralize(numCards)} in deck, ${"point".pluralize(numReviewingPoints)})")

        this.title = title
    }

    // Gives the message label its correct (possibly updated) value.
    private fun updateOnScreenInformation() {
        updateMessageLabel()
        updateWindowTitle()
    }

    private fun showCorrectPanel() =
            when (state) {
                MainWindowState.REACTIVE -> showReactivePanel()
                MainWindowState.INFORMATIONAL -> showInformationPanel()
                MainWindowState.REVIEWING -> showReviewingPanel()
                MainWindowState.SUMMARIZING -> showSummarizingPanel()
            }

    private fun mustReviewNow() =
            if (DeckManager.currentDeck().cardCollection.getTotal() == 0) false
            else DeckManager.currentDeck().timeUntilNextReview().isNegative

    // Opens the study options window, in which one can set the study options for
    // a deck (after which interval the first card should be studied, etc.)
    private fun openStudyOptionsWindow() = StudyOptionsWindow.display()

    private fun createDeck() {
        do {
            val deckName = JOptionPane.showInputDialog(null,
                    "Please give name for deck to be created")
            if (deckName == null) {
                // cancel button has been pressed
                return
            } else {
                if (!deckName.isValidIdentifier) {
                    JOptionPane.showMessageDialog(null,
                            "Sorry, \"$deckName\" is not a valid name for a deck. Please choose another name.")
                } else if (Deck.getDeckFileHandle(deckName).exists()) {
                    JOptionPane.showMessageDialog(null,
                            "Sorry, the deck \"$deckName\" already exists. Please choose another name.")
                } else {
                    // The deckname is valid!
                    changeDeck { DeckManager.createDeckWithName(deckName) }
                    return
                }
            }
        } while (true) // user is allowed to try create decks until he/she succeeds or gives up
    }

    private fun createMenuItem(label: String, actionKey: Char, listener: () -> Unit) = JMenuItem(label).apply {
        accelerator = KeyStroke.getKeyStroke(getExtendedKeyCodeForChar(actionKey.toInt()), ActionEvent.CTRL_MASK)
        addActionListener { listener() }
    }


    //Performs the proper buildup of the window (after the construction has initialized all components properly).
    private fun init() {
        // add menu
        val fileMenu = JMenu("File")
        fileMenu.add(createMenuItem("Create deck", 'k', ::createDeck))
        fileMenu.add(createMenuItem("Load deck", 'l', ::loadDeck))
        fileMenu.add(createMenuItem("Restore from archive", 'h', ::restoreDeck))
        fileMenu.add(createMenuItem("Analyze deck", 'z', ::analyzeDeck))
        fileMenu.add(createMenuItem("Quit", 'q', ::saveAndQuit))

        val deckManagementMenu = JMenu("Manage Deck")
        deckManagementMenu.add(createMenuItem("Add Card", 'n') { CardEditingManager(false) })
        deckManagementMenu.add(createMenuItem("Add Card (triple mode)", 'o') { CardEditingManager(true) })
        deckManagementMenu.add(createMenuItem("Study Options", 't', ::openStudyOptionsWindow))
        deckManagementMenu.add(createMenuItem("Deck Archiving Options", 'r', ::openDeckArchivingWindow))
        val mainMenuBar = JMenuBar()
        mainMenuBar.add(fileMenu)
        mainMenuBar.add(deckManagementMenu)
        jMenuBar = mainMenuBar

        // add message label (or show cards-to-be-reviewed)
        val informationPanel = createInformationPanel()
        modesContainer.add(informationPanel, INFORMATION_PANEL_ID)
        modesContainer.add(reviewPanel, REVIEW_PANEL_ID)
        ReviewManager.setPanel(reviewPanel)
        val summarizingPanel = SummarizingPanel()
        modesContainer.add(summarizingPanel, SUMMARIZING_PANEL_ID)
        add(modesContainer)
        setNameOfLastReviewedDeck()
        showCorrectPanel()

        // now show the window itself.
        setSize(1000, 700)
        defaultCloseOperation = EXIT_ON_CLOSE

        // Instead of using the WindowAdapter class, use the EventHandler utility
        // class to create a class with default noop methods, just overriding the
        // windowClosing.
        addWindowListener(EventHandler.create(WindowListener::class.java, this,
                "saveAndQuit", null, "windowClosing"))
        isVisible = true
        BlackBoard.register(this, UpdateType.PROGRAMSTATE_CHANGED)
        messageUpdater = Timer(100) { showCorrectPanel() }
        messageUpdater!!.start()
        updateOnScreenInformation()
    }

    private fun analyzeDeck() {
        Analyzer.run()
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

    private fun openDeckArchivingWindow() = ArchivingSettingsWindow.display()

    private fun setNameOfLastReviewedDeck() {
        val statusFilePath = Paths.get(EB_STATUS_FILE)
        val mostRecentDeckIdentifier = "most_recently_reviewed_deck: "
        val lines: List<String>
        try {
            lines = Files.readAllLines(statusFilePath, Charset.forName("UTF-8"))
            val fileLine = lines.find { it.startsWith(mostRecentDeckIdentifier) }
            if (fileLine != null) {
                val deckName = fileLine.substring(mostRecentDeckIdentifier.length)
                DeckManager.setNameOfLastReviewedDeck(deckName)
            }
        } catch (e: IOException) {
            // If input fails, set name to ""
            DeckManager.setNameOfLastReviewedDeck("")
            log("$e")
        }
    }

    private fun loadDeck() {
        do {
            val deckName = JOptionPane.showInputDialog(null,
                    "Please give name for deck to be loaded")
                    ?: // Cancel button pressed
                    return
            if (canDeckBeLoaded(deckName)) {
                changeDeck { DeckManager.loadDeck(deckName) }
                return
            }
        } while (true)
    }

    private fun changeDeck(deckProducer: () -> Unit) {
        messageUpdater!!.stop()
        deckProducer()
        // reset window
        state = MainWindowState.REACTIVE
        updateOnScreenInformation()
        messageUpdater!!.start()
    }

    private fun canDeckBeLoaded(deckName: String): Boolean {
        if (!deckName.isValidIdentifier) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, \"$deckName\" is not a valid name for a deck. Please choose another name.")
        } else if (!Deck.getDeckFileHandle(deckName).exists()) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, the deck \"$deckName\" does not exist yet.")
        } else {
            // we have a valid deck here
            if (DeckManager.canLoadDeck(deckName)) {
                // the only 'happy path' - otherwise false should be returned.
                return true
            } else {
                JOptionPane.showMessageDialog(null,
                        """An error occurred while loading the deck \"$deckName\". It may be an invalid file;
                            possibly try restore it from an archive file?""")
            }
        }
        return false
    }

    private fun createInformationPanel() = JPanel().apply {
        layout = BorderLayout()
        add(messageLabel, BorderLayout.CENTER)
        add(startReviewingButton, BorderLayout.SOUTH)
    }

    // Saves the current deck and its status, and quits Eb.
    // NOTE: CANNOT BE MADE PRIVATE DESPITE COMPILER COMPLAINING DUE TO addWindowListener CALL
    fun saveAndQuit() {
        saveEbStatus()
        DeckManager.save()
        dispose()
        exitProcess(0)
    }

    private fun saveEbStatus() {
        val lines = ArrayList<String>()
        lines.add("most_recently_reviewed_deck: " + DeckManager.currentDeck().name)
        val statusFilePath = Paths.get(EB_STATUS_FILE)
        try {
            Files.write(statusFilePath, lines, Charset.forName("UTF-8"))
        } catch (e: IOException) {
            log("$e")
        }

    }

    private fun switchToPanel(panelId: String) {
        val cardLayout = modesContainer.layout as CardLayout
        cardLayout.show(modesContainer, panelId)
    }

    private fun showInformationPanel() {
        switchToPanel(INFORMATION_PANEL_ID)
        updateOnScreenInformation()
    }

    // Shows the reviewing panel
    private fun showReviewingPanel() {
        if (state != MainWindowState.REVIEWING) {
            ReviewManager.start(reviewPanel)
            state = MainWindowState.REVIEWING
        }
        switchToPanel(REVIEW_PANEL_ID)
        updateOnScreenInformation()
    }

    // Shows the 'reactive' panel, which means the informational panel if no reviews need to be conducted,
    // and the reviewing panel when cards need to be reviewed.
    private fun showReactivePanel() {
        updateOnScreenInformation()
        if (mustReviewNow()) {
            showReviewingPanel()
        } else {
            showInformationPanel()
        }
    }

    override fun respondToUpdate(update: Update) = when (update.type) {
        UpdateType.DECK_CHANGED -> showCorrectPanel()
        UpdateType.PROGRAMSTATE_CHANGED -> {
            state = MainWindowState.valueOf(update.contents)
            reviewPanel.refresh() // there may be new cards to refresh
            updateOnScreenInformation()
            showCorrectPanel()
        }
        UpdateType.DECK_SWAPPED -> {
            val newState =
                    if (mustReviewNow()) MainWindowState.REVIEWING
                    else MainWindowState.REACTIVE
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED, newState.name))
        }
        else -> doNothing
    }

    private fun showSummarizingPanel() = switchToPanel(SUMMARIZING_PANEL_ID)


    // IDs of the various panels, necessary for adding them to the CardLayout of the main panel


    // the name of the file that contains which deck has been consulted last

}
