package eb.utilities.uiElements

import javax.swing.JLabel
import javax.swing.JPanel

import eb.eventhandling.BlackBoard
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import javax.swing.JCheckBox

class LabelledBoolField(labelText: String, value: Boolean) : JPanel() {

    private val label = JLabel(labelText)
    private val boolField = JCheckBox("", value)

    init {
        add(label)
        add(boolField)
        boolField.addActionListener {
            BlackBoard.post(Update(UpdateType.INPUTFIELD_CHANGED))
        }
    }

    fun contents(): Boolean {
        return boolField.isSelected
    }

    fun setContents(shouldBeSelected: Boolean) {
        boolField.isSelected = shouldBeSelected
    }
}
