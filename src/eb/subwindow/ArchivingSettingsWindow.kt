package eb.subwindow

import java.awt.Container
import java.awt.event.KeyEvent
import java.io.File

import eb.data.DeckManager
import eb.utilities.ProgrammableAction
import javax.swing.*

class ArchivingSettingsWindow internal constructor() : JFrame("Deck archiving settings") {

    internal var m_archivingLocation: JLabel
    internal var m_changeLocationButton: JButton

    init {
        val archivingDirectoryName = DeckManager.archivingDirectoryName
        val displayedDirectoryName: String
        if (archivingDirectoryName.isEmpty()) {
            displayedDirectoryName = "[default]"
        } else {
            displayedDirectoryName = archivingDirectoryName
        }
        m_archivingLocation = JLabel(
                "Location for archive files: $displayedDirectoryName")
        m_changeLocationButton = JButton("Change location for archive files")
        m_changeLocationButton.addActionListener { changeArchivingLocation() }
    }

    private fun changeArchivingLocation() {
        val chooser = JFileChooser()
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        val result = chooser.showSaveDialog(this)
        if (result == JFileChooser.CANCEL_OPTION) {
            return
        } else {
            val selectedDirectory = chooser.selectedFile
            DeckManager.setArchivingDirectory(selectedDirectory)
            m_archivingLocation.text = selectedDirectory.absolutePath
        }

    }

    private fun init() {
        val box = Box.createHorizontalBox()
        box.add(m_archivingLocation)
        box.add(Box.createHorizontalStrut(10))
        box.add(m_changeLocationButton)
        add(box)
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel") //$NON-NLS-1$
        getRootPane().actionMap.put("Cancel",
                ProgrammableAction { this.dispose() })
        setSize(700, 400)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        isVisible = true
    }

    companion object {

        fun display() {
            val archivingSettingsWindow = ArchivingSettingsWindow()
            archivingSettingsWindow.init()
        }
    }

}
