package eb.data

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.Serializable
import java.time.Duration
import java.time.Instant

import eb.Eb
import eb.writer.CardConverter
import eb.subwindow.studyoptions.StudyOptions
import eb.utilities.EMPTY_STRING
import eb.utilities.Utilities
import eb.utilities.Utilities.EOL
import eb.utilities.isValidIdentifier
import java.time.temporal.Temporal

import com.google.gson.GsonBuilder
import eb.analysis.Analyzer
import eb.subwindow.archivingsettings.ArchivingManager
import eb.utilities.getDateString
import kotlin.math.pow


/**
 * Contains the properties belonging to the 'pure' deck itself, like its name and contents. Not the part of
 * dealing with the GUI, which is the responsibility of the DeckManager class]
 *
 * @author Eric-Wubbo Lameijer
 */
class Deck(val name: String) : Serializable {
    init {
        require(name.isValidIdentifier) { "LogicalDeck constructor error: deck has a bad name." }
    }

    val cardCollection = CardCollection()

    // Note that while intuitively a deck is just a collection of cards, in Eb a deck also has settings,
    // which are per convenience also part of the deck (or deck file)
    var studyOptions = StudyOptions()

    //Returns the handle (File object) to the file in which this deck is stored.
    internal val fileHandle = getDeckFileHandle(name)

    // Returns a list of all the cards which should be reviewed at the current moment and study settings.
    fun reviewableCardList(): List<Card> =
        cardCollection.getCards().filter { getTimeUntilNextReview(it).isNegative }

    fun timeUntilNextReview(): Duration {
        require(cardCollection.getTotal() > 0) {
            "LogicalDeck.getTimeUntilNextReview()) error: the time till next review is undefined for an empty deck."
        }
        return cardCollection.getCards().map { getTimeUntilNextReview(it) }.min()!!
    }


    //Saves the deck to a text file (helpful for recovery), though it deletes all repetition data...
    fun saveDeckToTextFiles() {

        val nameOfArchivingDirectory = ArchivingManager.getDirectory(name)
        val textFileDirectory =
            if (nameOfArchivingDirectory == null) EMPTY_STRING
            else nameOfArchivingDirectory + File.separator


        val baseFileName = textFileDirectory + name + "_" + getDateString()
        val textFileName = "$baseFileName.txt"
        val reviewFileName = "${baseFileName}_reviews.txt"
        val jsonFileName = "$baseFileName.json"

        createTextFile(textFileName) { CardConverter.cardToLine(it) }
        createTextFile(reviewFileName) { CardConverter.reviewHistoryToLine(it) }
        createJsonFile(jsonFileName)
    }

    private fun createTextFile(fileName: String, formatter: (Card) -> String) {
        val outputStreamWriter = OutputStreamWriter(FileOutputStream(fileName), "UTF-8")
        BufferedWriter(outputStreamWriter).use { writer ->
            // write the header
            writer.write("Eb version ${Eb.VERSION_STRING}$EOL")
            writer.write("Number of cards is: ${cardCollection.getTotal()}$EOL")
            writer.write(HEADER_BODY_SEPARATOR + EOL)

            // write the card data
            cardCollection.writeCards(writer, formatter)
        }
    }

    private fun createJsonFile(fileName: String) {
        val gson = GsonBuilder().create()
        val output = gson.toJson(this) + EOL
        BufferedWriter(OutputStreamWriter(FileOutputStream(fileName), "UTF-8")).use {
            it.write(output)
        }
    }

    fun getRipenessFactor(card: Card) = getTimeUntilNextReview(card).seconds.toDouble()

    // Returns the time till the next review of the given card. The time can be negative, as that information can help
    // deprioritize 'over-ripe' cards which likely have to be learned anew anyway.
    private fun getTimeUntilNextReview(card: Card): Duration =
        Duration.between(Instant.now(), getNextReviewInstant(card))

    private fun calculateIntervalDurationFromUserSettings(card: Card): Duration = when (val lastReview = card.lastReview()) {
        null -> studyOptions.initialInterval.asDuration()
        else -> {
            if (lastReview.wasSuccess) getIntervalAfterSuccessfulReview(card)
            else studyOptions.forgottenInterval.asDuration()
        }
    }

    private fun getPlannedIntervalDuration(card: Card): Duration {
        val reviewPattern: String =
            card.getReviews().map { if (it.wasSuccess) 'S' else 'F' }.joinToString(separator = "")
        return recommendationsMap[reviewPattern] // calculate wait time from optimized settings
            // else: not enough data to determine best settings; use user-provided defaults
            ?: calculateIntervalDurationFromUserSettings(card)
    }

    private fun getNextReviewInstant(card: Card): Temporal {
        val startOfCountingTime = if (card.hasBeenReviewed()) card.lastReview()!!.instant else card.creationInstant
        val waitTime = getPlannedIntervalDuration(card)

        return waitTime.addTo(startOfCountingTime)
    }

    // Returns the time to wait for the next review (the previous review being a success).
    private fun getIntervalAfterSuccessfulReview(card: Card): Duration {
        // the default wait time after a single successful review is given by the study options
        val waitTime = studyOptions.rememberedInterval.asDuration()

        // However, if previous reviews also have been successful, the wait time
        // should be longer (using exponential growth by default, though may want
        // to do something more sophisticated in the future).
        val lengtheningFactor = studyOptions.lengtheningFactor
        val streakLength = card.streakSize()
        val numberOfLengthenings = streakLength - 1 // 2 reviews = lengthen 1x.
        return Utilities.multiplyDurationBy(waitTime, lengtheningFactor.pow(numberOfLengthenings.toDouble()))
    }

    fun getCardTexts(): List<Pair<String, String>> = cardCollection.getCardTexts()
    private lateinit var recommendationsMap: Map<String, Duration?>

    fun initRecommendations() {
        recommendationsMap = Analyzer.getRecommendationsMap()
    }

    companion object {
        // Automatically generated ID for serialization.
        private const val serialVersionUID = 8271837223354295531L

        // The file extension of a deck.
        private const val DECKFILE_EXTENSION = ".deck"

        // when writing the deck to a text file.
        private const val HEADER_BODY_SEPARATOR = "\t\t"

        // Returns the File object representing a deck with name "deckName".
        fun getDeckFileHandle(deckName: String): File {
            require(deckName.isValidIdentifier) { "LogicalDeck.getDeckFileHandle() error: deck name is invalid." }
            val deckFileName = deckName + DECKFILE_EXTENSION
            return File(deckFileName)
        }
    }
}
