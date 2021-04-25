package eb

import eb.data.DeckManager
import eb.subwindow.archivingsettings.ArchivingManager
import eb.utilities.log
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

object Personalisation {
    val latestArchivingDirLabel = "most_recently_used_archiving_directory: "
    val mostRecentDeckIdentifier = "most_recently_reviewed_deck: "

    val deckLinks = loadDeckLinks()
    val deckShortcuts = loadDeckShortcuts()
    var deckShortcutKeys = deckShortcuts.keys.sorted()

    fun saveEbStatus() {
        val lines = mutableListOf<String>()
        lines.add(mostRecentDeckIdentifier + DeckManager.currentDeck().name)
        lines.add(latestArchivingDirLabel + DeckManager.nameOfLastArchivingDirectory)
        (1..9).forEach {
            val deckName = deckShortcuts[it]
            if (deckName != null) lines.add("$it: $deckName")
        }
        ArchivingManager.deckDirectories.forEach { (deckName, deckDirectory) ->
            lines.add("@$deckName: $deckDirectory")
        }
        val usedDecks = mutableSetOf<String>()
        deckLinks.forEach { (deck, linkedDecks) ->
            if (deck !in usedDecks) {
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

    fun shortcutsHaveChanged() = deckShortcutKeys != deckShortcuts.keys.sorted()

    fun updateShortcuts() {
        deckShortcutKeys = deckShortcuts.keys.sorted()
    }

    private fun loadDeckShortcuts(): MutableMap<Int, String> {
        val statusFilePath = Paths.get(Eb.EB_STATUS_FILE)
        val shortCuts = mutableMapOf<Int, String>()
        try {
            val lines = Files.readAllLines(statusFilePath, Charset.forName("UTF-8"))
            lines.filter { it.isNotBlank() && it.trim().length > 2 }.forEach { line ->
                val startChar = line[0]
                if (startChar.isDigit() && line[1] == ':') {
                    val deckName = line.drop(2).trim()
                    if (deckName.isNotBlank()) shortCuts[startChar - '0'] = deckName
                }
            }
        } catch (e: IOException) {
            log("$e")
        }
        return shortCuts
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
                    println("Decknames: $deckNames")
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
        println(links)
        return links
    }

    fun deckShortcuts() = (1..9).joinToString("<br>") {
        val deckName = deckShortcuts[it]
        if (deckName != null) "Ctrl+$it: load deck '$deckName'" else ""
    }

    fun setNameOfLastReviewedDeck() {
        val statusFilePath = Paths.get(Eb.EB_STATUS_FILE)

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

    fun loadLatestArchivingDirectory() {
        val statusFilePath = Paths.get(Eb.EB_STATUS_FILE)
        try {
            val lines = Files.readAllLines(statusFilePath, Charset.forName("UTF-8"))
            val fileLine = lines.find { it.startsWith(latestArchivingDirLabel) }
            if (fileLine != null) {
                val lastArchivingDirName = fileLine.substring(latestArchivingDirLabel.length)
                DeckManager.nameOfLastArchivingDirectory = lastArchivingDirName
                println("Loading $lastArchivingDirName")
            }
        } catch (e: IOException) {
            log("$e")
        }
    }

}