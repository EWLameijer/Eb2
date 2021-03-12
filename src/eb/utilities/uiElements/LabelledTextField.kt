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
    private val textField = JTextField().apply {
        preferredSize = Dimension(40, 20)
        document = FixedSizeNumberDocument(this, size, precision)
        // need to add text AFTER document, as the replaced document deletes the existing text
        text = textFieldContents
        document.addDocumentListener(DelegatingDocumentListener {
            BlackBoard.post(Update(UpdateType.INPUTFIELD_CHANGED)) })

    }

    init {
        println("Of label '$labelText' '$this' setting contents to '$textFieldContents'")
        println("Contents becoming '${textField.text}'")
        add(label)
        add(textField)
    }

    fun contents() : String {
        println("Of label '${label.text}' '$this' get contents of '${textField.text}'")
        return textField.text
    }

    fun setContents(i: Int?) {
        println("Of label '${label.text}' '$this' setting contents to '$i'")
        textField.text = i?.toString() ?: "none"
    }

    fun setContents(d: Double) {
        println("Of label '${label.text}' '$this' setting contents to '$d'")
        textField.text = d.toString()
    }
}
