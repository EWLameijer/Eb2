package eb.utilities.uiElements

import java.awt.Dimension

import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

import eb.eventhandling.BlackBoard
import eb.eventhandling.DelegatingDocumentListener
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.utilities.FixedSizeNumberDocument

class LabelledTextField(labelText: String, textFieldContents: String, size: Int, precision: Int) : JPanel() {
    private val label = JLabel(labelText)
    private val textField = JTextField()

    var contents: String
        get() = textField.text
        set(text) {
            textField.text = text
        }

    init {
        textField.preferredSize = Dimension(40, 20)
        textField.document = FixedSizeNumberDocument(textField, size, precision)
        textField.document.addDocumentListener(DelegatingDocumentListener {
            BlackBoard.post(Update(UpdateType.INPUTFIELD_CHANGED)) })
        textField.text = textFieldContents
        add(label)
        add(textField)
    }

    fun setContents(i: Int?) {
        contents = i?.toString() ?: "none"
    }

    fun setContents(d: Double) {
        contents = d.toString()
    }
}
