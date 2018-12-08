package eb.utilities.ui_elements

import javax.swing.JComboBox
import javax.swing.JLabel

import eb.utilities.isValidIdentifier

class LabelledComboBox(labelText: String, comboBoxContents: Array<String>) : InputPanel() {
    private val m_label: JLabel
    private val m_comboBox: JComboBox<String>

    val value: String
        get() = m_comboBox.selectedItem as String

    init {
        require(labelText.isValidIdentifier){
                "LabelledComboBox constructor error: the text for the label cannot be empty."}
        for (comboBoxElement in comboBoxContents) {
            require(comboBoxElement.isValidIdentifier) {
                    "LabelledComboBox constructor error: the text for a combo box element cannot be empty."}
        }
        m_label = JLabel(labelText)
        m_comboBox = JComboBox(comboBoxContents)
        m_comboBox.addActionListener { notifyOfChange() }
        add(m_label)
        add(m_comboBox)
    }

    override fun areContentsEqual(otherInputPanel: InputPanel): Boolean {
        if (this === otherInputPanel) {
            return true
        } else if (this.javaClass != otherInputPanel.javaClass) {
            return false
        } else {
            val otherLabelledComboBox = otherInputPanel as LabelledComboBox?
            return m_comboBox.selectedItem == otherLabelledComboBox!!.m_comboBox.selectedItem
        }
    }

    fun setTo(item: String) {
        m_comboBox.selectedItem = item
    }
}
