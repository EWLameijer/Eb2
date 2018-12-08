package eb.data

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.Serializable
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoField
import java.util.logging.Logger

import eb.Eb
import eb.disk_io.CardConverter
import eb.subwindow.ArchivingSettings
import eb.subwindow.StudyOptions
import eb.utilities.EMPTY_STRING
import eb.utilities.Utilities
import eb.utilities.isValidIdentifier
import java.lang.RuntimeException

/**
 * Contains the properties belonging to the 'pure' deck itself, like its name
 * and contents [not the mess of dealing with the GUI, which is the
 * responsibility of the Deck class]
 *
 * @author Eric-Wubbo Lameijer
 */
class Deck(val name: String) : Serializable {
    init {
        // preconditions
        require(name.isValidIdentifier) { "LogicalDeck constructor error: deck has a bad name." }
        // postconditions: none. The deck should have been constructed,
        // everything should work
    }// code

    // Note that while intuitively a deck is just a collection of cards, in Eb a
    // deck also has settings, which are per convenience also part of the deck (or
    // deck file)
    /**
     * Returns the collection of cards that this deck possesses.
     *
     * @return the CardCollection associated with this deck.
     */
    val cards = CardCollection()

    var studyOptions = StudyOptions()

    /**
     * Returns the handle (File object) to the file in which this deck is stored.
     *
     * @return the handle (File object) to the file which stores this deck
     */
    internal val fileHandle = Deck.getDeckFileHandle(name)

    /**
     * Returns the archiving settings of this deck.
     *
     * @return the archiving settings of this deck
     */
    internal val archivingSettings: ArchivingSettings = ArchivingSettings()

    /**
     * Returns a list of all the cards which should be reviewed at the current
     * moment and study settings.
     *
     * @return a list of all the cards which should be reviewed, given the current
     * card collection and study settings.
     */
    fun reviewableCardList() : MutableList<Card> {
        val reviewableCards = ArrayList<Card>()
        val cardIterator = cards.getIterator()
        while (cardIterator.hasNext()) {
            val currentCard = cardIterator.next()
            if (getTimeUntilNextReview(currentCard).isNegative) {
                reviewableCards.add(currentCard)
            }
        }
        return reviewableCards
    }

    /**
     * Returns the time that the user has to wait to the next review.
     *
     * @return how long it will be until the next review.
     */
    fun timeUntilNextReview(): Duration {
        require(cards.getTotal() > 0) {
                "LogicalDeck.getTimeUntilNextReview()) error: the time till next review is undefined for an empty deck."}
        val cardIterator = cards.getIterator()

        val firstCard = cardIterator.next()
        var minimumTimeUntilNextReview = getTimeUntilNextReview(firstCard)
        while (cardIterator.hasNext()) {
            val card = cardIterator.next()
            val timeUntilThisCardIsReviewed = getTimeUntilNextReview(card)

            if (timeUntilThisCardIsReviewed < minimumTimeUntilNextReview) {
                minimumTimeUntilNextReview = timeUntilThisCardIsReviewed
            }
        }
        return minimumTimeUntilNextReview
    }



    /**
     * Saves the deck to a text file (helpful for recovery), though it deletes all
     * repetition data...
     */
    fun saveDeckToTextfiles() {
        // Phase 1: get proper filename for deck
        val now = LocalDateTime.now()
        val nameOfArchivingDirectory = archivingSettings.directoryName
        val textFileDirectory = if (nameOfArchivingDirectory.isEmpty())
            EMPTY_STRING
        else
            nameOfArchivingDirectory + File.separator
        val twoDigitFormat = "%02d" // format numbers as 01, 02...99

        val baseFileName = (textFileDirectory + name + "_"
                + String.format(twoDigitFormat, now.get(ChronoField.DAY_OF_MONTH))
                + String.format(twoDigitFormat, now.get(ChronoField.MONTH_OF_YEAR))
                + String.format(twoDigitFormat, now.get(ChronoField.YEAR) % 100) + "_"
                + String.format(twoDigitFormat, now.get(ChronoField.HOUR_OF_DAY))
                + String.format(twoDigitFormat, now.get(ChronoField.MINUTE_OF_HOUR)))

        val textFileName = "$baseFileName.txt"
        val reviewFileName = baseFileName + "_reviews.txt"

        createTextFile(textFileName) { CardConverter.cardToLine(it) }
        createTextFile(reviewFileName) { CardConverter.reviewHistoryToLine(it) }
    }

    private fun createTextFile(fileName: String, outputter: (Card) -> String) {
        // Phase 2: write the deck itself
        try {
            val fn = fileName
            val fos = FileOutputStream(fn)
            val osw = OutputStreamWriter(fos, "UTF-8")
            val bw = BufferedWriter(osw)

                // Phase 2a: write the header.
                bw.write("Eb version ${Eb.VERSION_STRING}${Utilities.EOL}")
                bw.write("Number of cards is: ${cards.getTotal()}${Utilities.EOL}")
                bw.write(HEADER_BODY_SEPARATOR + Utilities.EOL)

                // Phase 2b: write the card data
                cards.writeCards(bw, outputter)


        } catch (e: Exception) {
            Logger.getGlobal().info(e.toString() + "")
            throw RuntimeException("Deck.saveDeckToTextfile() error: cannot save text copy of deck.")
        }
    }

    /**
     * Returns the time till the next review of the given card. The time can be
     * negative, as that information can help deprioritize 'over-ripe' cards which
     * likely have to be learned anew anyway.
     *
     * @return the time till the next planned review of this card. Can be
     * negative.
     */
    fun getTimeUntilNextReview(card: Card): Duration {
        // case 1: the card has never been reviewed yet. So take the creation
        // instant and add the user-specified initial interval.
        if (!card.hasBeenReviewed()) {
            return Duration.between(Instant.now(), studyOptions.initialInterval
                    .asDuration().addTo(card.creationInstant))
        } else {
            // other cases: there have been previous reviews.
            val lastReview = card.lastReview()!!
            val lastReviewInstant = lastReview.instant
            val waitTime: Duration
            waitTime = if (lastReview.wasSuccess())
                getIntervalAfterSuccessfulReview(card)
                else studyOptions.forgottenInterval.asDuration()

            val officialReviewTime = waitTime.addTo(lastReviewInstant)
            return Duration.between(Instant.now(), officialReviewTime)
        }
    }

    /**
     * Returns the time to wait for the next review (the previous review being a
     * success).
     *
     * @return the time to wait for the next review
     */
    private fun getIntervalAfterSuccessfulReview(card: Card): Duration {
        // the default wait time after a single successful review is given by the
        // study options
        var waitTime = studyOptions.rememberedInterval.asDuration()

        // However, if previous reviews also have been successful, the wait time
        // should be longer (using exponential growth by default, though may want
        // to do something more sophisticated in the future).
        val lengtheningFactor = DeckManager.lengtheningFactor
        val streakLength = card.streakSize()
        val numberOfLengthenings = streakLength - 1 // 2 reviews = lengthen 1x.
        for (lengtheningIndex in 0 until numberOfLengthenings) {
            waitTime = Utilities.multiplyDurationBy(waitTime, lengtheningFactor)
        }
        return waitTime
    }

    companion object {

        // Automatically generated ID for serialization.
        private const val serialVersionUID = 8271837223354295531L

        // The file extension of a deck.
        private const val DECKFILE_EXTENSION = ".deck"

        // when writing the deck to a text file.
        private const val HEADER_BODY_SEPARATOR = "\t\t"

        /**
         * Returns the File object representing a deck with name "deckName".
         *
         * @param deckName
         * the name of the deck
         * @return the File object belonging to this deck.
         */
        fun getDeckFileHandle(deckName: String): File {
            require(deckName.isValidIdentifier){ "LogicalDeck.getDeckFileHandle() error: deck name is invalid." }
            val deckFileName = deckName + DECKFILE_EXTENSION
            return File(deckFileName)
        }
    }

}
