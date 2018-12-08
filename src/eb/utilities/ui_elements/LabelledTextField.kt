package eb.utilities.ui_elements

import java.awt.Dimension

import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

import eb.eventhandling.BlackBoard
import eb.eventhandling.DelegatingDocumentListener
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.utilities.FixedSizeNumberDocument

class LabelledTextField : JPanel {
    private val m_label: JLabel
    private val m_textField: JTextField

    var contents: String
        get() = m_textField.text
        set(text) {
            m_textField.text = text
        }

    constructor(labelText: String, textFieldContents: String, size: Int,
                precision: Int) {
        m_label = JLabel(labelText)
        m_textField = JTextField()
        m_textField.preferredSize = Dimension(40, 20)
        m_textField.document = FixedSizeNumberDocument(m_textField, size, precision)
        m_textField.document.addDocumentListener(DelegatingDocumentListener {
            BlackBoard.post(Update(UpdateType.INPUTFIELD_CHANGED)) })
        m_textField.text = textFieldContents
        add(m_label)
        add(m_textField)
    }

    /**
     * Constructor for making a non-numeric text field (that is, a text field that
     * contains an ordinary string)
     *
     * @param labelText
     * the text of the label
     */
    constructor(labelText: String) {
        m_label = JLabel(labelText)
        m_textField = JTextField("EMPTY")
        m_textField.maximumSize = Dimension(Integer.MAX_VALUE, 25)
        add(m_label)
        add(m_textField)
    }

    fun setContents(i: Int?) {
        contents = i?.toString() ?: "none"
    }

    fun setContents(d: Double) {
        contents = d.toString()
    }
}
