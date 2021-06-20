package eb.popups

import eb.Personalisation
import eb.data.BaseDeckData
import eb.data.Deck
import eb.data.DeckManager
import eb.popups.PopupUtils.closeOptionPane
import javax.swing.JButton
import javax.swing.JOptionPane

class DeckShortcutsPopup {
    private val shortcutsWithDeckData = Personalisation.shortcutsWithDeckData

    fun updateShortcuts() {
        val currentDeckName = DeckManager.currentDeck().name
        val currentDeckIndices = shortcutsWithDeckData.filterValues { it.name == currentDeckName }.keys
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

    private fun createCancelButton() =
        JButton("Cancel").apply {
            addActionListener { closeOptionPane() }
        }

    private fun createRemoveShortcutButton(currentDeckIndex: Int?) =
        JButton("Remove shortcut").apply {
            isEnabled = if (currentDeckIndex != null) {
                addActionListener {
                    shortcutsWithDeckData.remove(currentDeckIndex)
                    closeOptionPane()
                }
                true
            } else false
        }

    private fun createAddShortcutButton(currentDeckIndex: Int?, currentDeckName: String) =
        JButton("Add shortcut").apply {
            val currentDeckReviewTime = DeckManager.currentDeck().timeOfNextReview()
            isEnabled = if (currentDeckIndex == null) {
                val firstFreeIndex = getFirstFreeIndex(shortcutsWithDeckData)
                addActionListener {
                    shortcutsWithDeckData[firstFreeIndex] = BaseDeckData(currentDeckName,currentDeckReviewTime)
                    closeOptionPane()
                }
                true
            } else false
        }

    private fun getFirstFreeIndex(shortcuts: MutableMap<Int, BaseDeckData>) =
        (1..Personalisation.MAX_ALT_SHORTCUTS).first { shortcuts[it] == null }
}

