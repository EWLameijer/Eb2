package eb.data

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.Duration
import java.util.logging.Logger

import eb.disk_io.CardConverter
import eb.eventhandling.BlackBoard
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.subwindow.StudyOptions
import eb.utilities.Utilities
import eb.utilities.isValidIdentifier
import java.lang.RuntimeException

/**
 * The DeckManager class concerns itself with all the housekeeping (such as
 * interacting with the GUI) that the deck itself (which only concerns itself
 * with the logical content) should not need to bother about.
 *
 * @author Eric-Wubbo Lameijer
 */
object DeckManager {
    // The deck managed by the DeckManager.
    private var m_deck: Deck? = null

    // the name of the deck that has been reviewed previously
    // TODO: basically, is only important when starting up Eb; why does this need
    // to be a field?
    private var c_nameOfLastReviewedDeck = ""

    // The name of the default deck
    private val DEFAULT_DECKNAME = "default"

    /**
     * Returns whether a deck / the contents of a deck have been loaded.
     *
     * @return whether a deck has been loaded into this "deck-container"
     */
    private fun deckHasBeenLoaded() = m_deck != null

    /**
     * Returns whether the deck has been initialized, even if it is only with the
     * default deck.
     *
     * @return whether the deck has been initialized, meaning it can be used for
     * things like counting the number of cards in it.
    */
    private fun ensureDeckExists() {
        if (!deckHasBeenLoaded()) {
            // No deck has been loaded yet - try to load the default deck,
            // or else create it.
            if (canLoadDeck(nameOfLastDeck)) {
                loadDeck(nameOfLastDeck)
            } else {
                // If loading the deck failed, try to create it.
                // Note that createDeckWithName cannot return null; it will exit
                // with an error message instead.
                createDeckWithName(DEFAULT_DECKNAME)
            }
        }

         // postconditions: m_currentDeck cannot be null, but that is ensured
         // by the Deck.createDeckWithName call, which exits with an error if
         // a deck cannot be created.
        require(deckHasBeenLoaded()) {"Deck.ensureDeckExists() error: there is no valid deck."}
    }

    /**
     * Returns the name of the deck studied previously (ideal when starting a new
     * session of Eb).
     *
     * @return the name of the last deck studied
     */
    private val nameOfLastDeck: String
        get() = if (c_nameOfLastReviewedDeck.isEmpty()) {
            DEFAULT_DECKNAME
        } else {
            c_nameOfLastReviewedDeck
        }

    /**
     * Loads a deck from file.
     *
     * @param name
     * the name of the deck.
     * @return a boolean indicating whether the requested deck was successfully
     * loaded
     */
    fun loadDeck(name: String) {
        // checking preconditions
        require(canLoadDeck(name)){"Deck.loadDeck() error: deck cannot be loaded. Was canLoadDeck called?"}

        save()
        val deckFile = Deck.getDeckFileHandle(name)
        try {
            ObjectInputStream(FileInputStream(deckFile)).use {
                objInStream ->
                    val loadedDeck = objInStream.readObject() as Deck?
                    if (loadedDeck != null) {
                        m_deck = loadedDeck
                        BlackBoard.post(Update(UpdateType.DECK_SWAPPED))
                    } else {
                        throw RuntimeException("Deck.loadDeck() error: the requested deck cannot be loaded.")
                    }
            }
        } catch (e: Exception) {
            // something goes wrong with deserializing the deck; so
            // you also can't read the file
            Logger.getGlobal().info(e.toString() + "Deck.loadDeck() error: could not load deck from file")
        }
    }

    /**
     * Returns whether a deck with this name can be loaded (it exists and is of
     * the proper file format)
     *
     * @param deckName
     * the name of the deck can be loaded.
     * @return true if the deck can be loaded from disk, false if it cannot.
     */
    fun canLoadDeck(deckName: String): Boolean {
        // checking preconditions
        if (!deckName.isValidIdentifier) return false

        val deckFile = Deck.getDeckFileHandle(deckName)

        // case A: the file does not exist
        if (!deckFile.isFile) return false

        // so the file must exist. But does it contain a valid deck?
        try {
            ObjectInputStream(FileInputStream(deckFile)).use {
                objInStream ->
                    val loadedDeck = objInStream.readObject() as? Deck
                    return true
            }
        } catch (e: Exception) {
                // something goes wrong with deserializing the deck; so
                // you also can't read the file
            Logger.getGlobal().info(e.toString() + "Deck.loadDeck() error: could not load deck from file")
            return false
        }
    }

    /**
     * Creates a deck with name "name".
     *
     * @param name
     * the name of the deck to be created
     */
    fun createDeckWithName(name: String) {
        // checking preconditions
        require(name.isValidIdentifier) {
            "Deck.createDeckWithName() error: name has to contain non-whitespace characters." }
        // Save the current deck to disk before creating the new deck
        save()
        m_deck = Deck(name)

        // postconditions: the deck should exist (deck.save handles any errors occurring during saving the deck).
        require(deckHasBeenLoaded()){ "Deck.createDeckWithName() error: problem creating and/or writing the new deck." }

        // The deck has been changed. So ensure depending GUI-elements know that.
        reportDeckChangeEvent()
    }

    /**
     * After the deck has been swapped, ensure anything not handled by the
     * GUI-element activating the deck swap itself is performed.
     */
    private fun reportDeckChangeEvent() {
        // A new review session is needed.
        BlackBoard.post(Update(UpdateType.DECK_CHANGED))
    }

    /**
     * Saves the deck to disk.
     */
    fun save() {
        // Preconditions: none (well, if the deck does not exist, you don't have to
        // do anything).

        // First: check if there is a deck to be saved in the first place.
        if (!deckHasBeenLoaded()) {
                // If there is no deck, there is no necessity to save it...
            return
        }
        ensureDeckExists()
        try {
            ObjectOutputStream(FileOutputStream(m_deck!!.fileHandle)).use {
                objOutStream ->
                    objOutStream.writeObject(m_deck)
                    m_deck!!.saveDeckToTextfiles()
            }
        } catch (e: Exception) {
            // Something goes wrong with serializing the deck; so
            // you cannot create the file.
            Logger.getGlobal().info(e.toString() + "")
            throw RuntimeException("Deck.save() error: cannot write the new deck to disk.")
        }

        // postconditions: the save has to be a success! Which it is if no
        // exception occurred - in other words, if you get here.
    }

    /**
     * Sets the study options of the current deck to a new value.
     *
     * @param studyOptions
     * the new study options
     */
    fun setStudyOptions(studyOptions: StudyOptions) {
        // preconditions: outside ensuring that there is a deck, preconditions
        // should be handled by the relevant method in the logical deck
        ensureDeckExists()
        m_deck!!.studyOptions = studyOptions
        // postconditions: handled by callee.
    }

    val rememberedCardInterval: Duration
        get() {
            ensureDeckExists()
            return m_deck!!.studyOptions.rememberedInterval.asDuration()
        }

    val lengtheningFactor: Double
        get() {
            ensureDeckExists()
            return m_deck!!.studyOptions.lengtheningFactor
        }

    fun setNameOfLastReviewedDeck(nameOfLastReviewedDeck: String) {
        c_nameOfLastReviewedDeck = nameOfLastReviewedDeck
    }

    val contents: Deck?
        get() {
            ensureDeckExists()
            return m_deck
        }

    fun setArchivingDirectory(directory: File) {
        ensureDeckExists()
        m_deck!!.archivingSettings.setDirectory(directory)
    }

    val archivingDirectoryName: String
        get() {
            ensureDeckExists()
            return m_deck!!.archivingSettings.directoryName
        }

    /**
     * Creates a deck based on an archive file.
     *
     * @param selectedFile
     * the archive file (text file) to base the new deck on.
     */
    fun createDeckFromArchive(selectedFile: File) {
        val fileName = selectedFile.name
        val sizeOfFileName = fileName.length
        val sizeOfEnd = "_DDMMYY_HHMM.txt".length
        val deckName = fileName.substring(0, sizeOfFileName - sizeOfEnd)
        createDeckWithName(deckName)
        ensureDeckExists()

        CardConverter.extractCardsFromArchiveFile(selectedFile)
    }

    /**
     * Returns the current deck (loads the default deck or creates a deck if none
     * exists yet)
     *
     * @return the current deck.
     */
    val currentDeck: Deck?
        get() {
            ensureDeckExists()
            return m_deck
        }

}
