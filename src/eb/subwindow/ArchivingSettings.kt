package eb.subwindow

import eb.utilities.EMPTY_STRING
import java.io.File
import java.io.Serializable

class ArchivingSettings : Serializable {
    private var m_archivingDirectory: File? = null

    val directoryName: String
        get() = if (m_archivingDirectory == null) {
            EMPTY_STRING
        } else {
            m_archivingDirectory!!.absolutePath
        }

    init {
        m_archivingDirectory = null
    }

    fun setDirectory(directory: File) {
        if (!directory.exists()) {
            directory.mkdir()
        }
        m_archivingDirectory = directory
    }

    companion object {

        /**
         *
         */
        private const val serialVersionUID = 1L

        val default: ArchivingSettings
            get() = ArchivingSettings()
    }

}
