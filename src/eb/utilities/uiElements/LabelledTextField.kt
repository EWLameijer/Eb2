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

    init {
        textField.preferredSize = Dimension(40, 20)
        textField.document = FixedSizeNumberDocument(textField, size, precision)
        textField.document.addDocumentListener(DelegatingDocumentListener {
            BlackBoard.post(Update(UpdateType.INPUTFIELD_CHANGED)) })
        textField.text = textFieldContents
        add(label)
        add(textField)
    }

    fun contents() : String = textField.text

    fun setContents(i: Int?) {
        textField.text = i?.toString() ?: "none"
    }

    fun setContents(d: Double) {
        textField.text = d.toString()
    }
}
