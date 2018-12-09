package eb.data

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.logging.Logger

import eb.writer.CardConverter
import eb.eventhandling.BlackBoard
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.subwindow.StudyOptions
import eb.utilities.isValidIdentifier
import eb.utilities.log
import java.lang.RuntimeException

/**
 * The DeckManager class concerns itself with all the housekeeping (such as interacting with the GUI) that the deck
 * itself (which only concerns itself with the logical content) should not need to bother about.
 *
 * @author Eric-Wubbo Lameijer
 */
object DeckManager {
    // The deck managed by the DeckManager.
    private var deck: Deck? = null

    private var nameOfLastReviewedDeck = ""

    // The name of the default deck
    private const val DEFAULT_DECKNAME = "default"

    private fun deckHasBeenLoaded() = deck != null

    // Returns whether the deck has been initialized, even if it is only with the default deck.
    private fun ensureDeckExists() {
        if (!deckHasBeenLoaded()) {
            // No deck has been loaded yet - try to load the default deck,
            // or else create it.
            if (canLoadDeck(nameOfLastDeck())) loadDeck(nameOfLastDeck())
            else createDeckWithName(DEFAULT_DECKNAME)
        }
        // postconditions: m_currentDeck cannot be null, but that is ensured by the Deck.createDeckWithName call,
        // which exits with an error if a deck cannot be created.
        require(deckHasBeenLoaded()) {"Deck.ensureDeckExists() error: there is no valid deck."}
    }

    // Returns the name of the deck studied previously (ideal when starting a new session of Eb).
    private fun nameOfLastDeck() = if (nameOfLastReviewedDeck.isEmpty()) DEFAULT_DECKNAME else nameOfLastReviewedDeck

    fun loadDeck(name: String) {
        require(canLoadDeck(name)){"Deck.loadDeck() error: deck cannot be loaded. Was canLoadDeck called?"}

        save()
        val deckFile = Deck.getDeckFileHandle(name)
        try {
            val objectInputStream = ObjectInputStream(FileInputStream(deckFile))
            val loadedDeck = objectInputStream.readObject() as Deck?
            if (loadedDeck != null) {
                deck = loadedDeck
                BlackBoard.post(Update(UpdateType.DECK_SWAPPED))
            } else {
                throw RuntimeException("Deck.loadDeck() error: the requested deck cannot be loaded.")
            }
        } catch (e: Exception) {
            // something goes wrong with deserializing the deck; so you also can't read the file
            log("$e Deck.loadDeck() error: could not load deck from file")
        }
    }


    // Returns whether a deck with this name can be loaded (it exists and is of the proper file format)
    fun canLoadDeck(deckName: String): Boolean {
        if (!deckName.isValidIdentifier) return false

        val deckFile = Deck.getDeckFileHandle(deckName)

        // case A: the file does not exist
        if (!deckFile.isFile) return false

        // so the file must exist. But does it contain a valid deck?
        try {
            val objectInputStream = ObjectInputStream(FileInputStream(deckFile))
            objectInputStream.readObject() as? Deck
            return true
        } catch (e: Exception) {
                // something goes wrong with deserializing the deck; so you also can't read the file
            log("$e Deck.canLoadDeck() error: could not load deck from file")
            return false
        }
    }

    fun createDeckWithName(name: String) {
        require(name.isValidIdentifier) {
            "Deck.createDeckWithName() error: name has to contain non-whitespace characters." }

        // Save the current deck to disk before creating the new deck
        save()
        deck = Deck(name)

        // postconditions: the deck should exist (deck.save handles any errors occurring during saving the deck).
        require(deckHasBeenLoaded()){ "Deck.createDeckWithName() error: problem creating and/or writing the new deck." }

        // The deck has been changed. So ensure depending GUI-elements know that.
        reportDeckChangeEvent()
    }

    private fun reportDeckChangeEvent() = BlackBoard.post(Update(UpdateType.DECK_CHANGED))

    // saves the deck to disk
    fun save() {

        // First: check if there is a deck to be saved in the first place.
        if (!deckHasBeenLoaded()) {
            // If there is no deck, there is no necessity to save it...
            return
        }
        ensureDeckExists()
        try {
            val objectOutputStream = ObjectOutputStream(FileOutputStream(deck!!.fileHandle))
            objectOutputStream.writeObject(deck)
            deck!!.saveDeckToTextFiles()
        } catch (e: Exception) {
            // Something goes wrong with serializing the deck; so you cannot create the file.
            log("$e")
            throw RuntimeException("Deck.save() error: cannot write the new deck to disk.")
        }
        // postconditions: the save has to be a success! Which it is if no
        // exception occurred - in other words, if you get here.
    }

    fun setStudyOptions(studyOptions: StudyOptions) {
        ensureDeckExists()
        deck!!.studyOptions = studyOptions
    }

    fun setNameOfLastReviewedDeck(nameOfLastReviewedDeck: String) {
        this.nameOfLastReviewedDeck = nameOfLastReviewedDeck
    }

    fun setArchivingDirectory(directory: File) {
        ensureDeckExists()
        deck!!.archivingSettings.setDirectory(directory)
    }

    fun archivingDirectoryName(): String? {
        ensureDeckExists()
        return deck!!.archivingSettings.directoryName()
    }

    // create deck from archive (text!) file (instead of regular loading via deserialization)
    fun createDeckFromArchive(selectedFile: File) {
        val fileName = selectedFile.name
        val sizeOfFileName = fileName.length
        val sizeOfEnd = "_DDMMYY_HHMM.txt".length
        val deckName = fileName.substring(0, sizeOfFileName - sizeOfEnd)
        createDeckWithName(deckName)
        ensureDeckExists()

        CardConverter.extractCardsFromArchiveFile(selectedFile)
    }

    fun currentDeck(): Deck {
        ensureDeckExists()
        return deck!!
    }
}
