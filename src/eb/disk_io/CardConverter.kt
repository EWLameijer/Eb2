package eb.disk_io

import java.io.File
import java.io.IOException
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.function.Function
import java.util.logging.Logger

import eb.data.Card
import eb.data.DeckManager
import eb.utilities.Hint
import eb.utilities.Utilities
import eb.utilities.log
import java.lang.RuntimeException

/**
 * The CardConverter class wraps converting a card to a line for output (or disk
 * IO) and converting a line to the appropriate card.
 *
 * @author Eric-Wubbo Lameijer
 */
object CardConverter {

    // note that the best separator in this case consists of characters that
    // cannot be part of the 'regular' text of a card; tab characters are
    // perfect for that, for when the user presses TAB, instead of a tab character
    // being added, the cursor just jumps to the other side of the card .
    private val SEPARATOR_REGEX = "\\t\\t"

    private val SEPARATOR = "\t\t"

    /**
     * Uses a line to create a new card (or rather, a new Card object).
     *
     * @param line
     * the line used to create the Card object. Needs to contain a
     * (non-empty) string to fill the front of the card, followed by the
     * separator string, followed by the string that makes up the back of
     * the card.
     * @return the card
     */
    private fun lineToCard(line: String): Card {
        val strings = line.split(SEPARATOR_REGEX.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (strings.size != 2 || !Hint.isValid(strings[0])) {
            throw RuntimeException("CardConverter.lineToCard() error: the input string is invalid.")
        }
        return Card(Hint(strings[0]), strings[1])
    }

    /**
     * The cardToLine method transforms the data of a card to a line, for purposes
     * of saving it to a human-readable file (that is also easy to restore data
     * from).
     *
     * @param card
     * the card to be written into a line of text.
     * @return a newline-terminated String containing the front and back of the
     * card, as text.
    */
    fun cardToLine(card: Card) = card.front.contents + SEPARATOR + card.back + Utilities.EOL

    /**
     * The reviewHistoryToLine method transforms the review data of a card to a
     * line, for purposes of saving it to a format that is easier to restore from
     * than Java's standard 'blobs'. (Note that I could also use GoogleProto here,
     * but my Java skills are not yet sufficient to handle that level of added
     * complexity)
     *
     * @param card
     * the card of which the review history must be saved.
     * @return a newline-terminated String containing the front and review history
     * of the card.
    */
    fun reviewHistoryToLine(card: Card) = card.front.contents + SEPARATOR + card.history() + Utilities.EOL

    /**
     * Writes a single card, converted to a line, to the given writer.
     *
     * @param writer
     * the writer to write the card to
     * @param card
     * the card to write to the writer
    */
    fun writeLine(writer: Writer, cardAsText: String) {
        try {
            writer.write(cardAsText)
        } catch (e: IOException) {
            log(e.toString() )
        }
    }

    fun extractCardsFromArchiveFile(selectedFile: File) {
        try {
            val lines = Files.readAllLines(selectedFile.toPath(),
                    Charset.forName("UTF-8"))

            // find out which line contains the first card (skip version data and
            // such for now)
            var currentLine = 0
            while (currentLine < lines.size && lines[currentLine] != SEPARATOR) {
                currentLine++
            }
            // skip the separator line
            currentLine++
            // read in the cards
            while (currentLine < lines.size) {
                val newCard = CardConverter.lineToCard(lines[currentLine])
                DeckManager.currentDeck!!.cards.addCard(newCard)
                currentLine++
            }
        } catch (e: IOException) {
            Logger.getGlobal().info(e.toString() + "")
            e.printStackTrace()
        }
    }
}


