package eb.mainwindow

import eb.Personalisation
import eb.analysis.Analyzer
import eb.data.Deck
import eb.data.DeckManager
import eb.subwindow.archivingsettings.ArchivingSettingsWindow
import eb.subwindow.cardediting.CardEditingManager
import eb.subwindow.studyoptions.StudyOptionsWindow
import eb.utilities.isValidIdentifier
import eb.utilities.toLiteralChar
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class MainWindowMenu(private val actionExecutor: MainWindow) : JMenuBar() {

    init {
        val fileMenu = createFileManagementMenu()
        add(fileMenu)
        val deckManagementMenu = createDeckManagementMenu()
        add(deckManagementMenu)
    }

    private fun createFileManagementMenu(): JMenu {
        val fileMenu = JMenu("File")
        addBasicFileManagementItems(fileMenu)
        addDeckLoadingMenuItems(fileMenu)
        return fileMenu
    }

    private fun createDeckManagementMenu() = JMenu("Manage Deck").apply {
        add(createMenuItem("Add Card", 'n') { CardEditingManager(false) })
        add(createMenuItem("Add Card (triple mode)", 'o') { CardEditingManager(true) })
        add(createMenuItem("Study Options", 't', ::openStudyOptionsWindow))
        add(createMenuItem("Deck Archiving Options", 'r', ::openDeckArchivingWindow))
        add(createMenuItem("Analyze deck", 'z', ::analyzeDeck))
        add(createMenuItem("Standardize card texts", 's', ::standardizeCardTexts))
    }

    private fun createMenuItem(label: String, actionKey: Char, listener: () -> Unit) = JMenuItem(label).apply {
        accelerator =
            KeyStroke.getKeyStroke(KeyEvent.getExtendedKeyCodeForChar(actionKey.code), ActionEvent.CTRL_MASK)
        addActionListener { listener() }
    }

    private fun createAltMenuItem(label: String, actionKey: Char, listener: () -> Unit) = JMenuItem(label).apply {
        accelerator =
            KeyStroke.getKeyStroke(KeyEvent.getExtendedKeyCodeForChar(actionKey.code), ActionEvent.ALT_MASK)
        addActionListener { listener() }
    }

    // Opens the study options window, in which one can set the study options for
    // a deck (after which interval the first card should be studied, etc.)
    private fun openStudyOptionsWindow() = StudyOptionsWindow.display()

    private fun openDeckArchivingWindow() = ArchivingSettingsWindow.display()

    private fun analyzeDeck() = Analyzer.run()

    private fun standardizeCardTexts() = DeckManager.currentDeck().cardCollection.standardizeTexts()

    private fun addBasicFileManagementItems(fileMenu: JMenu) = fileMenu.apply {
        add(createMenuItem("Create deck", 'k', ::createDeck))
        add(createMenuItem("Load deck", 'l', ::loadDeck))
        add(createMenuItem("Restore from archive", 'h', ::restoreDeck))
        add(createMenuItem("Merge other deck into this one", 'm', ::mergeDeck))
        add(createMenuItem("Quit", 'q', actionExecutor::saveAndQuit))
    }

    private fun createDeck() {
        do {
            val deckName = getNameForNewDeck() ?: return // if null has been given, the cancel button has been pressed

            if (!deckName.isValidIdentifier) messageThatDeckNameIsInvalid(deckName)
            else if (Deck.getDeckFileHandle(deckName).exists()) messageThatDeckAlreadyExists(deckName)
            else {
                // The deckname is valid!
                actionExecutor.changeDeck { DeckManager.createDeckWithName(deckName) }
                return
            }
        } while (true) // user is allowed to try create decks until he/she succeeds or gives up
    }

    private fun messageThatDeckAlreadyExists(deckName: String) {
        JOptionPane.showMessageDialog(
            null,
            "Sorry, the deck \"$deckName\" already exists. Please choose another name."
        )
    }

    private fun messageThatDeckNameIsInvalid(deckName: String) {
        JOptionPane.showMessageDialog(
            null,
            "Sorry, \"$deckName\" is not a valid name for a deck. Please choose another name."
        )
    }

    private fun getNameForNewDeck() = JOptionPane.showInputDialog(
        null,
        "Please give name for deck to be created"
    )


    private fun loadDeck() {
        do {
            val deckName = JOptionPane.showInputDialog(
                null,
                "Please give name for deck to be loaded"
            )
                ?: // Cancel button pressed
                return
            if (actionExecutor.loadDeckIfPossible(deckName)) return
        } while (true)
    }

    private fun restoreDeck() {
        val chooser = JFileChooser(Personalisation.nameOfLastArchivingDirectory).apply {
            fileFilter = FileNameExtensionFilter("Archive files", "json")
        }
        val result = chooser.showOpenDialog(actionExecutor)
        if (result == JFileChooser.CANCEL_OPTION) {
            return
        } else {
            val selectedFile = chooser.selectedFile
            Personalisation.nameOfLastArchivingDirectory = selectedFile.parent
            DeckManager.createDeckFromArchive(selectedFile)
        }
    }

    private fun mergeDeck() {
        do {
            val deckName = JOptionPane.showInputDialog(
                null, "Please give name for deck to be merged into this one"
            )
                ?: // Cancel button pressed
                return
            if (actionExecutor.mergeDeckIfPossible(deckName)) return
        } while (true)
    }


    private fun addDeckLoadingMenuItems(fileMenu: JMenu) {
        fileMenu.addSeparator()
        fileMenu.add(createMenuItem("Manage deck-shortcuts", '0', actionExecutor::manageDeckShortcuts))
        (1..9).filter { Personalisation.shortcutsWithDeckData[it] != null }.forEach { digit ->
            val deckName = Personalisation.shortcutsWithDeckData[digit]!!.name
            fileMenu.add(
                createMenuItem(
                    "Load deck '$deckName'",
                    digit.toLiteralChar()
                ) { actionExecutor.loadDeckIfPossible(deckName) })
        }
        (10..Personalisation.MAX_ALT_SHORTCUTS).filter { Personalisation.shortcutsWithDeckData[it] != null }
            .forEach { rawIndex ->
                val deckName = Personalisation.shortcutsWithDeckData[rawIndex]!!.name
                val digit = rawIndex - 10 // deck 11 becomes Alt+1 etc.
                fileMenu.add(
                    createAltMenuItem(
                        "Load deck '$deckName'",
                        digit.toLiteralChar()
                    ) { actionExecutor.loadDeckIfPossible(deckName) })
            }
    }


}