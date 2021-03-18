package eb.popups

import eb.data.DeckManager
import javax.swing.JButton
import javax.swing.JOptionPane

class DeckShortcutsPopup(private val shortcuts: MutableMap<Int, String>) {
    fun updateShortcuts() {
        val currentDeckName = DeckManager.currentDeck().name
        val currentDeckIndices = shortcuts.filterValues { it == currentDeckName }.keys
        val currentDeckIndex = if (currentDeckIndices.isEmpty()) null else currentDeckIndices.first()

        val cancelButton = createCancelButton()
        val removeShortcutButton = createRemoveShortcutButton(currentDeckIndex)
        val addShortcut = createAddShortcutButton(currentDeckIndex, currentDeckName)

        val buttons = arrayOf(cancelButton, removeShortcutButton, addShortcut)
        JOptionPane.showOptionDialog(
            null,
            "Do you want to create or remove a shortcut?",
            "Manage deck shortcuts", 0,
            JOptionPane.QUESTION_MESSAGE, null, buttons, null
        )
    }

    private fun createCancelButton() {
        JButton("Cancel").apply {
            addActionListener { closeOptionPane() }
        }
    }

    private fun createRemoveShortcutButton(currentDeckIndex: Int?) {
        JButton("Remove shortcut").apply {
            isEnabled = if (currentDeckIndex != null) {
                addActionListener {
                    shortcuts.remove(currentDeckIndex)
                    closeOptionPane()
                }
                true
            } else false
        }
    }

    private fun createAddShortcutButton(currentDeckIndex: Int?, currentDeckName: String) {
        JButton("Add shortcut").apply {
            isEnabled = if (currentDeckIndex == null) {
                val firstFreeIndex = getFirstFreeIndex(shortcuts)
                addActionListener {
                    shortcuts[firstFreeIndex] = currentDeckName
                    closeOptionPane()
                }
                true
            } else false
        }
    }

    private fun getFirstFreeIndex(shortcuts: MutableMap<Int, String>) =
        (1..9).first { shortcuts[it] == null }

    private fun closeOptionPane() = JOptionPane.getRootFrame().dispose()
}

