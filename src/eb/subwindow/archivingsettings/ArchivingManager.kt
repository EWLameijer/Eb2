package eb.subwindow.archivingsettings

import eb.Eb
import eb.utilities.log
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

object ArchivingManager {
    val deckDirectories = loadDeckDirectories()

    fun getDirectory(deckName: String) : String? = deckDirectories[deckName]

    fun setDirectory(deckName: String, directory: File) {
        if (!directory.exists()) {
            directory.mkdir()
        }
        deckDirectories[deckName] = directory.absolutePath
    }

    private fun loadDeckDirectories(): MutableMap<String, String> {
        val statusFilePath = Paths.get(Eb.EB_STATUS_FILE)
        val directoryMap = mutableMapOf<String, String>()
        try {
            val lines = Files.readAllLines(statusFilePath, Charset.forName("UTF-8"))
            lines.filter { it.isNotBlank() && it.trim().length > 2 }.forEach { line ->
                val startChar = line[0]
                if (startChar=='@') {
                    val (rawDeckName, rawDeckPath) = line.split(": ")
                    val deckName = rawDeckName.drop(1) // remove '@'
                    val deckPath = rawDeckPath.trim()
                    if (deckName.isNotBlank())  directoryMap[deckName] = deckPath
                }
            }
        } catch (e: IOException) {
            log("$e")
        }
        return directoryMap
    }
}