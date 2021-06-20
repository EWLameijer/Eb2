package eb

import eb.data.BaseDeckData
import eb.data.DeckManager
import eb.subwindow.archivingsettings.ArchivingManager
import eb.utilities.asTwoDigitString
import eb.utilities.log
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime

object Personalisation {
    const val MAX_ALT_SHORTCUTS = 19

    private const val latestArchivingDirLabel = "most_recently_used_archiving_directory: "
    private const val mostRecentDeckIdentifier = "most_recently_reviewed_deck: "
    val nameOfLastDeck = getNameOfLastReviewedDeck()
    var nameOfLastArchivingDirectory = loadLatestArchivingDirectory()

    private fun getNameOfLastReviewedDeck(): String? {
        val statusFilePath = Paths.get(Eb.EB_STATUS_FILE)

        try {
            val lines = Files.readAllLines(statusFilePath, Charset.forName("UTF-8"))
            val fileLine = lines.find { it.startsWith(mostRecentDeckIdentifier) }
            if (fileLine != null) {
                return fileLine.substring(mostRecentDeckIdentifier.length)
            }
        } catch (e: IOException) {
            log("$e")
        }

        return null
    }

    val deckLinks = loadDeckLinks()
    val shortcutsWithDeckData = loadDeckShortcutsAndReviewTimes()
    private var deckShortcutKeys = shortcutsWithDeckData.keys.sorted()

    fun saveEbStatus() {
        val lines = mutableListOf<String>()
        lines.add(mostRecentDeckIdentifier + DeckManager.currentDeck().name)
        lines.add(latestArchivingDirLabel + nameOfLastArchivingDirectory)
        lines += getShortcutLinesForFile()
        ArchivingManager.deckDirectories.forEach { (deckName, deckDirectory) ->
            lines.add("@$deckName: $deckDirectory")
        }
        val usedDecks = mutableSetOf<String>()
        deckLinks.forEach { (deck, linkedDecks) ->
            if (deck !in usedDecks && linkedDecks.isNotEmpty()) {
                usedDecks += deck
                linkedDecks.forEach { usedDecks += it }
                val linkedDecksStr = linkedDecks.joinToString("&")
                lines.add("&$deck&$linkedDecksStr")
            }
        }
        val statusFilePath = Paths.get(Eb.EB_STATUS_FILE)
        try {
            Files.write(statusFilePath, lines, Charset.forName("UTF-8"))
        } catch (e: IOException) {
            log("$e")
        }
    }

    private fun getShortcutLinesForFile(): List<String> {
        return (1..MAX_ALT_SHORTCUTS).filter {
            shortcutsWithDeckData[it] != null
        }.map {
            val deckData = shortcutsWithDeckData[it]
            val deckName = deckData?.name
            val nextReviewTime = deckData?.nextReview
            val nextReviewText = when {
                nextReviewTime != null -> " $nextReviewTime"
                else -> ""
            }
            "$it: $deckName$nextReviewText"
        }
    }

    fun shortcutsHaveChanged() = deckShortcutKeys != shortcutsWithDeckData.keys.sorted()

    fun updateShortcuts() {
        deckShortcutKeys = shortcutsWithDeckData.keys.sorted()
    }

    private fun loadDeckShortcutsAndReviewTimes(): MutableMap<Int, BaseDeckData> {
        val statusFilePath = Paths.get(Eb.EB_STATUS_FILE)
        val shortCuts = mutableMapOf<Int, BaseDeckData>()
        try {
            processShortcutLines(statusFilePath, shortCuts)
        } catch (e: IOException) {
            log("$e")
        }
        return shortCuts
    }

    private fun processShortcutLines(statusFilePath: Path, shortCuts: MutableMap<Int, BaseDeckData>) {
        val lines = Files.readAllLines(statusFilePath, Charset.forName("UTF-8"))
        lines.filter { it.isNotBlank() && it.trim().length > 2 }.forEach { line ->
            val possibleNumberMatch = getPossibleNumberMatch(line)
            if (possibleNumberMatch != null) {
                val (index, fileAndPossibleNextReview) = possibleNumberMatch
                shortCuts[index] = fileAndPossibleNextReview
            }
        }
    }

    private fun getPossibleNumberMatch(line: String): Pair<Int, BaseDeckData>? {
        val lineComponents = line.split(' ')
        if (lineComponents.size !in 2..3) return null
        val (possibleNumber, fileName) = lineComponents
        if (possibleNumber.last() != ':') return null
        val contents = possibleNumber.dropLast(1)
        if (contents.any { !it.isDigit() }) return null
        val nextReview = if (lineComponents.size == 3) LocalDateTime.parse(lineComponents[2]) else null
        return contents.toInt() to BaseDeckData(fileName, nextReview)
    }

    private fun loadDeckLinks(): MutableMap<String, Set<String>> {
        val statusFilePath = Paths.get(Eb.EB_STATUS_FILE)
        val links = mutableMapOf<String, Set<String>>()
        try {
            val lines = Files.readAllLines(statusFilePath, Charset.forName("UTF-8"))
            lines.filter { it.isNotBlank() && it.trim().length > 2 }.forEach { line ->
                val startChar = line[0]
                if (startChar == '&') {
                    val deckNames = line.substring(1).split('&')
                    deckNames.forEach { currentDeckName ->
                        if (links[currentDeckName] == null) links[currentDeckName] = setOf()
                        deckNames.forEach { otherDeckName ->
                            if (otherDeckName != currentDeckName) links[currentDeckName] =
                                links[currentDeckName]!! + otherDeckName
                        }
                    }
                }
            }
        } catch (e: IOException) {
            log("$e")
        }
        return links
    }

    fun deckShortcuts() =
        (1..MAX_ALT_SHORTCUTS).map { it to shortcutsWithDeckData[it] }.filter { it.second?.name != null }
            .joinToString("<br>") { (index, deckData) ->
                val deckName = deckData!!.name
                val keyName = if (index < 10) "Ctrl" else "Alt"
                val shortCutDigit = if (index < 10) index else index - 10
                val nextReview = deckData.nextReview
                val (pre, post) = if (nextReview != null) {
                    if (nextReview < LocalDateTime.now()) "*" to ""
                    else "" to nicelyFormatFutureDate(nextReview)
                } else "" to ""
                "$pre$keyName+$shortCutDigit: load deck '$deckName'$post"
            }


    fun toStudy(): String {
        val decksToBeReviewed = (1..MAX_ALT_SHORTCUTS).map { shortcutsWithDeckData[it] }.filter {
            it?.name != null && it.nextReview != null && it.nextReview!! < LocalDateTime.now()
        }.joinToString { it!!.name }
        return if (decksToBeReviewed == "") "Reviewing of favorite decks finished for now!"
        else "Yet to review: [$decksToBeReviewed]"
    }

    private fun nicelyFormatFutureDate(nextReview: LocalDateTime): String {
        val today = LocalDateTime.now()
        val daysDifference = getDayDifference(today, nextReview)
        val hour = nextReview.hour.asTwoDigitString()
        val minute = nextReview.minute.asTwoDigitString()
        val hourMinute = "$hour:$minute"
        return " " + when {
            daysDifference == 0 -> "TODAY $hourMinute"
            daysDifference == 1 -> "TOMORROW $hourMinute"
            daysDifference < 366 -> "in $daysDifference days"
            else -> "in more than a year"
        }
    }

    private fun getDayDifference(earlierDate: LocalDateTime, laterDate: LocalDateTime): Int = when {
        earlierDate.year == laterDate.year -> laterDate.dayOfYear - earlierDate.dayOfYear
        laterDate.year == earlierDate.year + 1 -> laterDate.dayOfYear + lengthOfYear(earlierDate.year) - earlierDate.dayOfYear
        else -> 367
    }

    private fun lengthOfYear(year: Int): Int =
        if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 366
        else 365


    private fun loadLatestArchivingDirectory() : String? {
        val statusFilePath = Paths.get(Eb.EB_STATUS_FILE)
        try {
            val lines = Files.readAllLines(statusFilePath, Charset.forName("UTF-8"))
            val fileLine = lines.find { it.startsWith(latestArchivingDirLabel) }
            if (fileLine != null) {
                return fileLine.substring(latestArchivingDirLabel.length)
            }
        } catch (e: IOException) {
            log("$e")
        }
        return null
    }

    fun getShortcutIdOfDeck(soughtDeckName: String): Int? =
        shortcutsWithDeckData.filter { (_, deckData) -> deckData.name == soughtDeckName }.toList().firstOrNull()?.first

    fun registerTimeOfNextReview() {
        val currentDeck = DeckManager.currentDeck()
        val key = getShortcutIdOfDeck(currentDeck.name)
        if (key != null) shortcutsWithDeckData[key]!!.nextReview = currentDeck.timeOfNextReview()
    }

    fun updateTimeOfCurrentDeckReview() {
        val currentDeck = DeckManager.currentDeck()
        val currentDeckId = getShortcutIdOfDeck(currentDeck.name)
        if (currentDeckId != null)
            shortcutsWithDeckData[currentDeckId]!!.nextReview = LocalDateTime.now() + currentDeck.timeUntilNextReview()
    }

    fun unlink(firstDeck: String, secondDeck: String) {
        unlinkSecondFromFirst(firstDeck, secondDeck)
        unlinkSecondFromFirst(secondDeck, firstDeck)
    }

    private fun unlinkSecondFromFirst(firstDeck: String, secondDeck: String) {
        val currentlyLinkedDecks: Set<String> = deckLinks[firstDeck]!!
        val withSecondDeckRemoved = currentlyLinkedDecks - secondDeck
        deckLinks[firstDeck] = withSecondDeckRemoved
    }
}