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
import eb.popups.DeckShortcutsPopup
import eb.subwindow.archivingsettings.ArchivingSettingsWindow
import eb.subwindow.cardediting.CardEditingManager
import eb.subwindow.studyoptions.StudyOptionsWindow
import eb.utilities.*
import java.awt.event.KeyEvent.getExtendedKeyCodeForChar
import kotlin.system.exitProcess


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

    private val deckShortcuts = loadDeckShortcuts()
    private var deckShortcutKeys = deckShortcuts.keys.sorted()

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
            append("<br>")
            append(deckShortcuts())
            append("</html>")
        }
    }

    // Updates the title of the window, which contains information like the number of cards in the deck
    private fun updateWindowTitle() {
        val currentDeck = DeckManager.currentDeck()
        val numReviewingPoints = currentDeck.cardCollection.getReviewingPoints()

        val numReviewableCards = currentDeck.reviewableCardList().size
        var title =
            ("Eb${Eb.VERSION_STRING}: ${currentDeck.name} (${"card".pluralize(numReviewableCards)} to be reviewed in total")
        if (state == MainWindowState.REVIEWING) {
            title += (", ${"card".pluralize(ReviewManager.cardsToGoYet())} yet to be reviewed in the current session")
        }
        val numCards = currentDeck.cardCollection.getTotal()
        title += (", ${"card".pluralize(numCards)} in deck, ${"point".pluralize(numReviewingPoints)})")

        this.title = title
    }

    // Gives the message label its correct (possibly updated) value.
    private fun updateOnScreenInformation() {
        updateMenuIfNeeded()
        updateMessageLabel()
        updateWindowTitle()
    }

    private fun updateMenuIfNeeded() {
        if (deckShortcutKeys != deckShortcuts.keys.sorted()) {
            deckShortcutKeys = deckShortcuts.keys.sorted()
            createMenu()
        }
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
            val deckName = JOptionPane.showInputDialog(
                null,
                "Please give name for deck to be created"
            )
            if (deckName == null) {
                // cancel button has been pressed
                return
            } else {
                if (!deckName.isValidIdentifier) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Sorry, \"$deckName\" is not a valid name for a deck. Please choose another name."
                    )
                } else if (Deck.getDeckFileHandle(deckName).exists()) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Sorry, the deck \"$deckName\" already exists. Please choose another name."
                    )
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
        createMenu()

        // add message label (or show cards-to-be-reviewed)
        modesContainer.add(createInformationPanel(), INFORMATION_PANEL_ID)
        modesContainer.add(reviewPanel, REVIEW_PANEL_ID)
        ReviewManager.setPanel(reviewPanel)
        modesContainer.add(SummarizingPanel(), SUMMARIZING_PANEL_ID)
        add(modesContainer)
        setNameOfLastReviewedDeck()
        showCorrectPanel()

        // now show the window itself.
        setSize(1000, 700)
        defaultCloseOperation = EXIT_ON_CLOSE

        // Instead of using the WindowAdapter class, use the EventHandler utility
        // class to create a class with default noop methods, just overriding the
        // windowClosing.
        addWindowListener(
            EventHandler.create(
                WindowListener::class.java, this,
                "saveAndQuit", null, "windowClosing"
            )
        )
        isVisible = true
        BlackBoard.register(this, UpdateType.PROGRAMSTATE_CHANGED)
        messageUpdater = Timer(100) { showCorrectPanel() }
        messageUpdater!!.start()
        updateOnScreenInformation()
    }

    private fun createMenu() {
        val mainMenuBar = JMenuBar()
        val fileMenu = createFileManagementMenu()
        mainMenuBar.add(fileMenu)
        val deckManagementMenu = createDeckManagementMenu()
        mainMenuBar.add(deckManagementMenu)
        jMenuBar = mainMenuBar
    }

    private fun createDeckManagementMenu(): JMenu {
        val deckManagementMenu = JMenu("Manage Deck")
        deckManagementMenu.add(createMenuItem("Add Card", 'n') { CardEditingManager(false) })
        deckManagementMenu.add(createMenuItem("Add Card (triple mode)", 'o') { CardEditingManager(true) })
        deckManagementMenu.add(createMenuItem("Study Options", 't', ::openStudyOptionsWindow))
        deckManagementMenu.add(createMenuItem("Deck Archiving Options", 'r', ::openDeckArchivingWindow))
        return deckManagementMenu
    }

    private fun createFileManagementMenu(): JMenu {
        val fileMenu = JMenu("File")
        addBasicFileManagementItems(fileMenu)
        addDeckLoadingMenuItems(fileMenu)
        return fileMenu
    }

    private fun addBasicFileManagementItems(fileMenu: JMenu) = fileMenu.apply {
        add(createMenuItem("Create deck", 'k', ::createDeck))
        add(createMenuItem("Load deck", 'l', ::loadDeck))
        add(createMenuItem("Restore from archive", 'h', ::restoreDeck))
        add(createMenuItem("Analyze deck", 'z', ::analyzeDeck))
        add(createMenuItem("Quit", 'q', ::saveAndQuit))
    }

    private fun addDeckLoadingMenuItems(fileMenu: JMenu) {
        fileMenu.addSeparator()
        fileMenu.add(createMenuItem("Manage deck-shortcuts",'0', ::manageDeckShortcuts))
        (1..9).filter { deckShortcuts[it] != null }.forEach { digit ->
            val deckName = deckShortcuts[digit]
            fileMenu.add(
                createMenuItem(
                    "Load deck '$deckName'",
                    digit.toLiteralChar()
                ) { loadDeckIfPossible(deckName!!) })
        }
    }

    private fun manageDeckShortcuts() =  DeckShortcutsPopup(deckShortcuts).updateShortcuts()

    private fun loadDeckShortcuts(): MutableMap<Int, String> {
        val statusFilePath = Paths.get(EB_STATUS_FILE)
        val shortCuts = mutableMapOf<Int, String>()
        try {
            val lines = Files.readAllLines(statusFilePath, Charset.forName("UTF-8"))
            lines.filter { it.isNotBlank() && it.trim().length > 2 }.forEach { line ->
                val startChar = line[0]
                if (startChar.isDigit() && line[1] == ':') {
                    val deckName = line.drop(2).trim()
                    if (deckName.isNotBlank()) shortCuts[startChar - '0'] = deckName
                }
            }
        } catch (e: IOException) {
            log("$e")
        }
        return shortCuts
    }

    private fun deckShortcuts() = (1..9).joinToString("<br>") {
        val deckName = deckShortcuts[it]
        if (deckName != null) "Ctrl+$it: load deck '$deckName'" else ""
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
        try {
            val lines = Files.readAllLines(statusFilePath, Charset.forName("UTF-8"))
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
            val deckName = JOptionPane.showInputDialog(
                null,
                "Please give name for deck to be loaded"
            )
                ?: // Cancel button pressed
                return
            if (loadDeckIfPossible(deckName)) return
        } while (true)
    }

    private fun loadDeckIfPossible(deckName: String): Boolean {
        if (canDeckBeLoaded(deckName)) {
            changeDeck { DeckManager.loadDeck(deckName) }
            return true
        }
        return false
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
            JOptionPane.showMessageDialog(
                null,
                "Sorry, \"$deckName\" is not a valid name for a deck. Please choose another name."
            )
        } else if (!Deck.getDeckFileHandle(deckName).exists()) {
            JOptionPane.showMessageDialog(
                null,
                "Sorry, the deck \"$deckName\" does not exist yet."
            )
        } else {
            // we have a valid deck here
            if (DeckManager.canLoadDeck(deckName)) {
                // the only 'happy path' - otherwise false should be returned.
                return true
            } else {
                JOptionPane.showMessageDialog(
                    null,
                    """An error occurred while loading the deck \"$deckName\". It may be an invalid file;
                            possibly try restore it from an archive file?"""
                )
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
        val lines = mutableListOf<String>()
        lines.add("most_recently_reviewed_deck: " + DeckManager.currentDeck().name)
        (1..9).forEach {
            val deckName = deckShortcuts[it]
            if (deckName != null) lines.add("$it: $deckName")
        }
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
}
