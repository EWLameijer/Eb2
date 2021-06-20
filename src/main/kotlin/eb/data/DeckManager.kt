package eb.data

import eb.writer.CardConverter
import eb.eventhandling.BlackBoard
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.subwindow.studyoptions.StudyOptions
import eb.utilities.isValidIdentifier
import eb.utilities.log
import com.google.gson.GsonBuilder
import eb.Eb
import eb.Personalisation
import eb.popups.PopupUtils.closeOptionPane
import eb.subwindow.archivingsettings.ArchivingManager
import java.io.*
import java.nio.charset.Charset
import java.time.Instant
import javax.swing.JButton
import javax.swing.JOptionPane
import kotlin.RuntimeException


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

    // The name of the default deck
    private const val defaultDeckName = "default"

    private fun deckHasBeenLoaded() = decks.isNotEmpty()

    // Returns whether the deck has been initialized, even if it is only with the default deck.
    private fun ensureDeckExists() {
        if (!deckHasBeenLoaded()) {
            // No deck has been loaded yet - try to load the default deck,
            // or else create it.
            val nameOfLastReviewedDeck = Personalisation.nameOfLastDeck
            if (nameOfLastReviewedDeck != null && canLoadDeck(nameOfLastReviewedDeck))
                loadDeckGroup(nameOfLastReviewedDeck)
            else if (canLoadDeck(defaultDeckName)) loadDeckGroup(defaultDeckName)
            else createDeckWithName(defaultDeckName)
        }
        // postconditions: m_currentDeck cannot be null, but that is ensured by the Deck.createDeckWithName call,
        // which exits with an error if a deck cannot be created.
        require(deckHasBeenLoaded()) { "Deck.ensureDeckExists() error: there is no valid deck." }
    }


    fun loadDeckGroup(name: String) {
        require(canLoadDeck(name)) { "DeckManager.loadDeck() error: deck cannot be loaded. Was canLoadDeck called?" }
        save()
        val newMainDeck = loadDeck(name)
            ?: throw RuntimeException("DeckManager.loadDeck() error: the requested deck cannot be loaded.")
        newMainDeck.ebVersion = Eb.version
        loadTime = Instant.now()
        decks.clear()
        decks += newMainDeck
        decks[0].updateRecommendedStudyIntervalDurations()
        loadLinkedDecks(Personalisation.deckLinks[name])
        BlackBoard.post(Update(UpdateType.DECK_SWAPPED))
    }

    fun mergeWithDeck(name: String) {
        if (name == decks[0].name) showAttemptToMergeWithItselfMessage() // attempt to merge deck with itself
        else {
            val otherIndex = decks.map { it.name }.indexOf(name)
            if (otherIndex != -1) mergeWithLinkedDeck(otherIndex) // the other deck is already linked to this one.
            else mergeWithNonLinkedDeck(name)
        }
    }

    private fun mergeWithNonLinkedDeck(name: String) {
        require(canLoadDeck(name)) { "DeckManager.mergeWithDeck() error: deck cannot be loaded. Was canLoadDeck called?" }
        val otherDeck = loadDeck(name)
        val otherCards: List<Card> = otherDeck!!.cardCollection.getCards()
        otherCards.forEach { cardToAdd ->
            val cardWithDeckName = getCardWithDeckName(cardToAdd.front.contents)
            if (cardWithDeckName == null) {
                currentDeck().cardCollection.addCard(cardToAdd)
            } else {
                val (currentCard, currentCardDeck) = cardWithDeckName
                // two scenarios
                // in both scenarios: you have one card (reference) in the current setup;
                // 1: the base card is in the current deck
                if (currentCardDeck == currentDeck().name) possiblyReplaceOriginal(currentCard, cardToAdd)
                else possiblyReplaceCardInLinkedDeck(
                    currentCard,
                    currentCardDeck,
                    cardToAdd
                ) // the base card is in a linked deck
            }
        }
    }

    private fun mergeWithLinkedDeck(otherIndex: Int) {
        val otherCards: List<Card> = decks[otherIndex].cardCollection.getCards()
        otherCards.forEach { currentDeck().cardCollection.addCard(it) }
        Personalisation.unlink(decks[0].name, decks[otherIndex].name)
        decks.removeAt(otherIndex)
    }

    private fun showAttemptToMergeWithItselfMessage() {
        JOptionPane.showMessageDialog(
            null,
            "You cannot merge a deck with itself",
            "Can't merge deck with itself", JOptionPane.ERROR_MESSAGE
        )
    }

    private fun possiblyReplaceOriginal(
        currentVersion: Card,
        versionInDeckMergedIn: Card
    ) {
        val buttons = arrayOf(
            getMergeButton(currentVersion, versionInDeckMergedIn),
            getDeleteThisCardButton(currentVersion, versionInDeckMergedIn),
            getIgnoreNewCardButton()
        )

        val front = currentVersion.front.contents
        val currentBack = currentVersion.back
        val backOfMergeInCandidate = versionInDeckMergedIn.back
        JOptionPane.showOptionDialog(
            null,
            "A card with the front '$front' already exists; on the back is the text\n'$currentBack'\nreplace with\n'$backOfMergeInCandidate'?",
            "A card with this front already exists. What do you want to do?", 0,
            JOptionPane.QUESTION_MESSAGE, null, buttons, null
        )
    }

    private fun possiblyReplaceCardInLinkedDeck(
        currentVersion: Card,
        currentDeck: String,
        versionInDeckMergedIn: Card
    ) {
        val buttons = getHandleMergeDuplicateInLinkedDeckButtons(currentVersion, currentDeck, versionInDeckMergedIn)
        val front = currentVersion.front.contents
        val currentBack = currentVersion.back
        val backOfMergeInCandidate = versionInDeckMergedIn.back
        JOptionPane.showOptionDialog(
            null,
            "A card with the front '$front' already exists; on the back is the text\n'$currentBack'\nreplace with\n'$backOfMergeInCandidate'?",
            "A card with this front already exists. What do you want to do?", 0,
            JOptionPane.QUESTION_MESSAGE, null, buttons, null
        )
    }

    private fun getHandleMergeDuplicateInLinkedDeckButtons(
        currentVersion: Card,
        currentDeck: String,
        versionInDeckMergedIn: Card
    ) = arrayOf(
        getMergeToCurrentDeckButton(
            currentVersion,
            currentDeck,
            versionInDeckMergedIn
        ), // remove card from linked deck, move merged card to current deck
        getMergeToLinkedDeckButton(
            currentVersion,
            currentDeck,
            versionInDeckMergedIn
        ), // replace card in linked deck by merged card
        replaceCardAndMoveToCurrentDeckButton(currentVersion, currentDeck, versionInDeckMergedIn),
        replaceCardInLinkedDeckButton(currentVersion, currentDeck, versionInDeckMergedIn),
        getIgnoreNewCardButton() // ignore the new card
    )

    private fun replaceCardInLinkedDeckButton(
        currentVersion: Card,
        currentDeckName: String,
        versionInDeckMergedIn: Card
    ) =
        JButton("Replace card in linked deck").apply {
            addActionListener { replaceInLinkedDeck(currentVersion, currentDeckName, versionInDeckMergedIn) }
        }

    private fun replaceInLinkedDeck(currentVersion: Card, currentDeckName: String, versionInDeckMergedIn: Card) {
        val oldCollection = getDeckReference(currentDeckName).cardCollection
        oldCollection.removeCard(currentVersion)
        oldCollection.addCard(versionInDeckMergedIn)
        closeOptionPane()
    }

    private fun replaceCardAndMoveToCurrentDeckButton(
        currentVersion: Card,
        currentDeckName: String,
        versionInDeckMergedIn: Card
    ) =
        JButton("Move new card to current deck, delete card from linked deck").apply {
            addActionListener {
                replaceAndMoveToCurrentDeck(
                    currentVersion,
                    currentDeckName,
                    versionInDeckMergedIn
                )
            }
        }

    private fun replaceAndMoveToCurrentDeck(
        currentVersion: Card,
        currentDeckName: String,
        versionInDeckMergedIn: Card
    ) {
        val oldCollection = getDeckReference(currentDeckName).cardCollection
        oldCollection.removeCard(currentVersion)
        currentDeck().cardCollection.addCard(versionInDeckMergedIn)
        closeOptionPane()
    }

    private fun getMergeToLinkedDeckButton(
        currentVersion: Card,
        currentDeckName: String,
        versionInDeckMergedIn: Card
    ) =
        JButton("Merge backs of cards to linked deck").apply {
            addActionListener { mergeToLinkedDeck(currentVersion, currentDeckName, versionInDeckMergedIn) }
        }


    private fun getMergeToCurrentDeckButton(
        currentVersion: Card,
        currentDeckName: String,
        versionInDeckMergedIn: Card
    ) =
        JButton("Merge backs of cards to current deck").apply {
            addActionListener { mergeToCurrentDeck(currentVersion, currentDeckName, versionInDeckMergedIn) }
        }


    private fun mergeToCurrentDeck(currentVersion: Card, currentDeckName: String, versionInDeckMergedIn: Card) {
        val oldCollection = getDeckReference(currentDeckName).cardCollection
        val newCard = getMergedCard(currentVersion, versionInDeckMergedIn)
        oldCollection.removeCard(currentVersion)
        currentDeck().cardCollection.addCard(newCard)
        closeOptionPane()
    }

    private fun mergeToLinkedDeck(currentVersion: Card, currentDeckName: String, versionInDeckMergedIn: Card) {
        val linkedCollection = getDeckReference(currentDeckName).cardCollection
        val newCard = getMergedCard(currentVersion, versionInDeckMergedIn)
        linkedCollection.removeCard(currentVersion)
        linkedCollection.addCard(newCard)
        closeOptionPane()
    }

    private fun getDeckReference(currentDeckName: String): Deck {
        for (deck in decks) {
            if (deck.name == currentDeckName) return deck
        }
        throw RuntimeException("DeckManager.getDeckReference error: deck '$currentDeckName' is not a valid linked deck.")
    }

    private fun getIgnoreNewCardButton() =
        JButton("Ignore the other card").apply {
            addActionListener { closeOptionPane() }
        }


    private fun getDeleteThisCardButton(currentVersion: Card, versionInDeckMergedIn: Card) =
        JButton("Replace the current card").apply {
            addActionListener { replaceCurrentCard(currentVersion, versionInDeckMergedIn) }
        }

    private fun getMergeButton(currentVersion: Card, versionInDeckMergedIn: Card): JButton =
        JButton("Merge backs of cards").apply {
            addActionListener { mergeBacksAndReplaceOriginal(currentVersion, versionInDeckMergedIn) }
        }

    private fun replaceCurrentCard(currentVersion: Card, versionInDeckMergedIn: Card) {
        val cardCollection = currentDeck().cardCollection
        cardCollection.removeCard(currentVersion)
        cardCollection.addCard(Card(versionInDeckMergedIn.front, versionInDeckMergedIn.back))
        closeOptionPane()
    }

    private fun mergeBacksAndReplaceOriginal(originalCard: Card, mergedInCard: Card) {
        val cardCollection = currentDeck().cardCollection
        cardCollection.removeCard(originalCard)
        cardCollection.addCard(getMergedCard(originalCard, mergedInCard))
        closeOptionPane()
    }

    private fun getMergedCard(originalCard: Card, mergedInCard: Card) =
        Card(originalCard.front, "${originalCard.back}; ${mergedInCard.back}")

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
        decks[0].updateRecommendedStudyIntervalDurations()
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
        Personalisation.registerTimeOfNextReview()
        try {
            for (deck in decks) {
                if (deck.hasBeenModifiedSinceLoad()) {
                    val objectOutputStream = ObjectOutputStream(FileOutputStream(deck.fileHandle))
                    objectOutputStream.writeObject(deck)
                    deck.saveDeckToTextFiles()
                }
            }
        } catch (e: Exception) {
            // Something goes wrong with serializing the deck; so you cannot create the file.
            log("$e")
            throw RuntimeException("Deck.save() error: cannot write the new deck to disk.")
        }
    }

    fun setStudyOptions(studyOptions: StudyOptions) {
        ensureDeckExists()
        decks[0].studyOptions = studyOptions
        decks[0].studyOptions.modifiedSinceLoad = true
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

    fun getCardWithDeckName(frontText: String): Pair<Card, String>? {
        decks.forEach { deck ->
            val match = deck.cardCollection.getCardWithFront(frontText)
            if (match != null) return match to deck.name
        }
        return null
    }
}
