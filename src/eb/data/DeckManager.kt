package eb.data

import eb.writer.CardConverter
import eb.eventhandling.BlackBoard
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.subwindow.studyoptions.StudyOptions
import eb.utilities.isValidIdentifier
import eb.utilities.log
import java.lang.RuntimeException
import com.google.gson.GsonBuilder
import eb.Personalisation
import eb.subwindow.archivingsettings.ArchivingManager
import java.io.*
import java.nio.charset.Charset
import java.time.Instant


/**
 * The DeckManager class concerns itself with all the housekeeping (such as interacting with the GUI) that the deck
 * itself (which only concerns itself with the logical content) should not need to bother about.
 *
 * @author Eric-Wubbo Lameijer
 */
object DeckManager {
    // The deck managed by the DeckManager is deck[0]. deck[1..n] are for ' subordinate' linked decks
    private val decks: MutableList<Deck> = mutableListOf()
    private var loadTime: Instant = Instant.now()
    fun deckLoadTime() = loadTime
    private var nameOfLastReviewedDeck = ""

    // The name of the default deck
    private const val DEFAULT_DECKNAME = "default"

    private fun deckHasBeenLoaded() = decks.isNotEmpty()

    // Returns whether the deck has been initialized, even if it is only with the default deck.
    private fun ensureDeckExists() {
        if (!deckHasBeenLoaded()) {
            // No deck has been loaded yet - try to load the default deck,
            // or else create it.
            if (canLoadDeck(nameOfLastDeck())) loadDeckGroup(nameOfLastDeck())
            else createDeckWithName(DEFAULT_DECKNAME)
        }
        // postconditions: m_currentDeck cannot be null, but that is ensured by the Deck.createDeckWithName call,
        // which exits with an error if a deck cannot be created.
        require(deckHasBeenLoaded()) { "Deck.ensureDeckExists() error: there is no valid deck." }
    }

    // Returns the name of the deck studied previously (ideal when starting a new session of Eb).
    private fun nameOfLastDeck() = if (nameOfLastReviewedDeck.isEmpty()) DEFAULT_DECKNAME else nameOfLastReviewedDeck

    fun loadDeckGroup(name: String) {
        require(canLoadDeck(name)) { "Deck.loadDeck() error: deck cannot be loaded. Was canLoadDeck called?" }
        save()
        val newMainDeck = loadDeck(name)
            ?: throw RuntimeException("DeckManager.loadDeck() error: the requested deck cannot be loaded.")
        println("Linked decks: ${Personalisation.deckLinks[name]}")

        loadTime = Instant.now()
        decks.clear()
        decks += newMainDeck
        decks[0].initRecommendedStudyIntervalDurations()
        loadLinkedDecks(Personalisation.deckLinks[name])
        BlackBoard.post(Update(UpdateType.DECK_SWAPPED))
    }

    private fun loadLinkedDecks(linkedDecks: Set<String>?) {
        linkedDecks?.forEach { decks += loadDeck(it)!! }
    }

    private fun loadDeck(name: String): Deck? {
        val deckFile = Deck.getDeckFileHandle(name)
        return try {
            val objectInputStream = ObjectInputStream(FileInputStream(deckFile))
            objectInputStream.readObject() as Deck?
        } catch (e: Exception) {
            // something goes wrong with deserializing the deck; so you also can't read the file
            log("$e DeckManager.loadDeck() error: could not load deck from file")
            null
        }
    }


    // Returns whether a deck with this name can be loaded (it exists and is of the proper file format)
    fun canLoadDeck(deckName: String): Boolean {
        if (!deckName.isValidIdentifier) return false

        val deckFile = Deck.getDeckFileHandle(deckName)

        // case A: the file does not exist
        if (!deckFile.isFile) return false

        // so the file must exist. But does it contain a valid deck?
        return try {
            val objectInputStream = ObjectInputStream(FileInputStream(deckFile))
            objectInputStream.readObject() as? Deck
            true
        } catch (e: Exception) {
            // something goes wrong with deserializing the deck; so you also can't read the file
            log("$e Deck.canLoadDeck() error: could not load deck from file")
            false
        }
    }

    fun createDeckWithName(name: String) {
        require(name.isValidIdentifier) {
            "Deck.createDeckWithName() error: name has to contain non-whitespace characters."
        }

        // Save the current deck to disk before creating the new deck
        save()
        decks.clear()
        decks.add(Deck(name))
        decks[0].initRecommendedStudyIntervalDurations()
        loadTime = Instant.now()
        // postconditions: the deck should exist (deck.save handles any errors occurring during saving the deck).
        require(deckHasBeenLoaded()) { "Deck.createDeckWithName() error: problem creating and/or writing the new deck." }

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
        val mainDeck = decks[0]
        try {
            val objectOutputStream = ObjectOutputStream(FileOutputStream(mainDeck.fileHandle))
            objectOutputStream.writeObject(mainDeck)
            mainDeck.saveDeckToTextFiles()
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
        decks[0].studyOptions = studyOptions
    }

    fun setNameOfLastReviewedDeck(nameOfLastReviewedDeck: String) {
        this.nameOfLastReviewedDeck = nameOfLastReviewedDeck
    }

    fun setArchivingDirectory(directory: File) {
        ensureDeckExists()
        ArchivingManager.setDirectory(decks[0].name, directory)
    }

    fun archivingDirectoryName(): String? {
        ensureDeckExists()
        return ArchivingManager.getDirectory(decks[0].name)
    }

    private fun createDeckFromJson(jsonFile: File) {
        // Save the current deck to disk before creating the new deck
        save()

        val builder = GsonBuilder()
        builder.setPrettyPrinting()

        val gson = builder.create()
        try {
            val fr = InputStreamReader(FileInputStream(jsonFile), Charset.forName("UTF-8"))

            val stringBuilder = StringBuilder()
            while (true) {
                val i = fr.read()
                if (i == -1) break
                stringBuilder.append(i.toChar())
            }
            val json = stringBuilder.toString()
            decks[0] = gson.fromJson(json, Deck::class.java)

            // postconditions: the deck should exist (deck.save handles any errors
            // occurring during saving the deck).
            require(deckHasBeenLoaded()) { "Deck.createDeckWithName() error: " + "problem creating and/or writing the new deck." }

            // The deck has been changed. So ensure depending GUI-elements know that.
            BlackBoard.post(Update(UpdateType.DECK_SWAPPED))
        } catch (e: IOException) {
            // some error. NOt sure how to handle it.
        }

    }

    // create deck from archive (text!) file (instead of regular loading via deserialization)
    fun createDeckFromArchive(selectedFile: File) {
        val fileName = selectedFile.name
        val sizeOfFileName = fileName.length
        val sizeOfEnd = "_DDMMYY_HHMM.txt".length
        val deckName = fileName.substring(0, sizeOfFileName - sizeOfEnd)
        createDeckWithName(deckName)
        ensureDeckExists()

        if (fileName.endsWith("json")) createDeckFromJson(selectedFile)
        else CardConverter.extractCardsFromArchiveFile(selectedFile)
    }

    fun currentDeck(): Deck {
        ensureDeckExists()
        return decks[0]
    }

    fun getAllCardTexts(): List<BaseCardData> = decks.flatMap { it.getCardTexts() }

    fun getBaseCard(frontText: String): BaseCardData? =
        decks.flatMap { it.getCardTexts() }.find { it.front == frontText }
}
