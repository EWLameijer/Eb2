package eb.mainwindow

import eb.Eb
import eb.Eb.EB_STATUS_FILE
import eb.Personalisation
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
import eb.mainwindow.panels.InformationPanel
import eb.mainwindow.panels.SummarizingPanel
import eb.mainwindow.reviewing.ReviewManager
import eb.mainwindow.reviewing.ReviewPanel
import eb.subwindow.cardediting.CardEditingManager

import eb.popups.DeckShortcutsPopup
import eb.subwindow.archivingsettings.ArchivingSettingsWindow
import eb.subwindow.studyoptions.StudyOptionsWindow
import eb.utilities.*
import java.awt.event.KeyEvent.getExtendedKeyCodeForChar
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess


// The name of the program.
private const val PROGRAM_NAME = "Eb"

/**
 * The main window of Eb.
 *
 * @author Eric-Wubbo Lameijer
 */
class MainWindow : JFrame(PROGRAM_NAME), Listener {
    private val REVIEW_PANEL_ID = "REVIEWING_PANEL"
    private val INFORMATION_PANEL_ID = "INFORMATION_PANEL"
    private val SUMMARIZING_PANEL_ID = "SUMMARIZING_PANEL"

    // The label that has to be shown if there is no card that needs to be reviewed currently, or if there is an error.
    // Is the alternative to the regular "reviewing" window, which should be active most of the time.



    // the initial state of the main window
    private var state = MainWindowState.INFORMATIONAL

    // Contains the REVIEWING_PANEL and the INFORMATION_PANEL, using a CardLayout.
    private val modesContainer = JPanel()

    private val informationPanel = InformationPanel()

    // The reviewing panel
    private val reviewPanel: ReviewPanel = ReviewPanel()

    // To regularly update how long it is until the next reviewing session
    private var messageUpdater: Timer? = null



    init {
        modesContainer.layout = CardLayout()
        init()
    }

    // Gives the message label its correct (possibly updated) value.
    private fun updateOnScreenInformation() {
        updateMenuIfNeeded()
        informationPanel.updateMessageLabel()
        updateWindowTitle()
    }

    private fun createMenu() {
        val mainMenuBar = JMenuBar()
        val fileMenu = createFileManagementMenu()
        mainMenuBar.add(fileMenu)
        val deckManagementMenu = createDeckManagementMenu()
        mainMenuBar.add(deckManagementMenu)
        jMenuBar = mainMenuBar
    }

    private fun updateMenuIfNeeded() {
        if (Personalisation.shortcutsHaveChanged()) {
            Personalisation.updateShortcuts()
            createMenu()
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
        modesContainer.add(informationPanel, INFORMATION_PANEL_ID)
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
        if (!DeckManager.currentDeck().studyOptions.timerSettings.totalTimingMode) showReactivePanel()
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
        fileMenu.add(createMenuItem("Manage deck-shortcuts", '0', ::manageDeckShortcuts))
        (1..9).filter { Personalisation.deckShortcuts[it] != null }.forEach { digit ->
            val deckName = Personalisation.deckShortcuts[digit]
            fileMenu.add(
                createMenuItem(
                    "Load deck '$deckName'",
                    digit.toLiteralChar()
                ) { loadDeckIfPossible(deckName!!) })
        }
    }

    private fun manageDeckShortcuts() = DeckShortcutsPopup(Personalisation.deckShortcuts).updateShortcuts()

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
            changeDeck { DeckManager.loadDeckGroup(deckName) }
            return true
        }
        return false
    }

    private fun changeDeck(deckProducer: () -> Unit) {
        messageUpdater!!.stop()
        deckProducer()
        // reset window
        state = if (DeckManager.currentDeck().studyOptions.timerSettings.totalTimingMode) MainWindowState.INFORMATIONAL else MainWindowState.REACTIVE
        ReviewManager.resetTimers()
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



    // Saves the current deck and its status, and quits Eb.
// NOTE: CANNOT BE MADE PRIVATE DESPITE COMPILER COMPLAINING DUE TO addWindowListener CALL
    fun saveAndQuit() {
        Personalisation.saveEbStatus()
        DeckManager.save()
        dispose()
        exitProcess(0)
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
