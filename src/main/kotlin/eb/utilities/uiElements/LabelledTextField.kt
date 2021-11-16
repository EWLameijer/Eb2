package eb.utilities.uiElements

import java.awt.Dimension

import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

import eb.eventhandling.BlackBoard
import eb.eventhandling.DelegatingDocumentListener
import eb.eventhandling.Update
import eb.eventhandling.UpdateType

class LabelledTextField(labelText: String, textFieldContents: String, size: Int, precision: Int) : JPanel() {
    private val documentListener = DelegatingDocumentListener {
        BlackBoard.post(Update(UpdateType.INPUTFIELD_CHANGED))
    }

    private val label = JLabel(labelText)
    private val textField = JTextField().apply {
        preferredSize = Dimension(40, 20)
        document = FixedSizeNumberDocument(this, size, precision)
        // need to add text AFTER document, as the replaced document deletes the existing text
        text = textFieldContents
        document.addDocumentListener(documentListener)
    }

    init {
        add(label)
        add(textField)
    }

    fun contents(): String {
        return textField.text
    }

    fun setContents(i: Int?) {
        textField.text = i?.toString() ?: "none"
    }

    fun setContents(d: Double) {
        textField.text = d.toString()
    }

    fun deactivateListener() {
        textField.document.removeDocumentListener(documentListener)
    }

    fun activateListener() {
        textField.document.addDocumentListener(documentListener)
    }
}
