package eb.subwindow

import eb.utilities.EMPTY_STRING
import java.io.File
import java.io.Serializable

class ArchivingSettings : Serializable {

    private var archivingDirectory: File? = null

    fun directoryName() : String? = archivingDirectory?.absolutePath

    fun setDirectory(directory: File) {
        if (!directory.exists()) {
            directory.mkdir()
        }
        archivingDirectory = directory
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
