package eb.subwindow.archivingsettings

import java.awt.event.KeyEvent

import eb.data.DeckManager
import eb.utilities.ProgrammableAction
import javax.swing.*

object ArchivingSettingsWindow : JFrame("Deck archiving settings") {

    private const val START_OF_LABEL = "Location for archive files: "
    private var archivingLocation = JLabel()
    private var changeLocationButton = JButton("Change location for archive files")

    fun display() {
        changeLocationButton.addActionListener { changeArchivingLocation() }
        archivingLocation.text = getLabelText()
        val box = Box.createHorizontalBox().apply {
            add(archivingLocation)
            add(Box.createHorizontalStrut(10))
            add(changeLocationButton)
        }
        add(box)
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel") //$NON-NLS-1$
        getRootPane().actionMap.put("Cancel", ProgrammableAction { this.dispose() })
        setSize(700, 400)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        isVisible = true
    }

    private fun getLabelText() : String = START_OF_LABEL + (DeckManager.archivingDirectoryName() ?: "[default]")

    private fun changeArchivingLocation() {
        val chooser = JFileChooser()
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        val result = chooser.showSaveDialog(this)
        if (result == JFileChooser.CANCEL_OPTION) {
            return
        } else {
            DeckManager.setArchivingDirectory(chooser.selectedFile)
            archivingLocation.text = getLabelText()
        }
    }
}
