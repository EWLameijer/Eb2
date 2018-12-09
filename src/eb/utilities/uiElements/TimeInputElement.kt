package eb.utilities.uiElements

import java.awt.Dimension

import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

import eb.eventhandling.BlackBoard
import eb.eventhandling.DelegatingDocumentListener
import eb.eventhandling.Update
import eb.eventhandling.UpdateType
import eb.utilities.*

/**
 * A TimeInputElement contains a textfield and combobox that allow the user to
 * input say "5.5 minutes" or "3 hours".
 *
 * @author Eric-Wubbo Lameijer
 */
class TimeInputElement(name: String, inputTimeInterval: TimeInterval) : JPanel() {
    private val label = JLabel(name)

    // Text field that sets the quantity (the '3' in 3 hours), the combo box
    // selects the unit (the hours in "3 hours").
    private val scalarField = JTextField()

    // Combo box used (in combination with a text field) to set the value for the
    // initial study interval. Contains the hours of "3 hours".
    private val unitComboBox = JComboBox<String>()

    // Returns the time interval encapsulated by this TimeInputElement.
    var interval: TimeInterval
        get() {
            val timeUnit = TimeUnit.parseUnit(unitComboBox.selectedItem!!.toString())!!
            val timeIntervalScalar = Utilities.stringToDouble(scalarField.text) ?: 0.01
            return TimeInterval(timeIntervalScalar, timeUnit)
        }
        set(timeInterval) {
            scalarField.text = Utilities.doubleToMaxPrecisionString(timeInterval.scalar, 2)
            unitComboBox.selectedItem = timeInterval.unit.userInterfaceName
            notifyDataFieldChangeListeners()
        }

    init {
        scalarField.document = FixedSizeNumberDocument(scalarField, 5, 2)
        scalarField.document.addDocumentListener( DelegatingDocumentListener { notifyDataFieldChangeListeners() })

        scalarField.preferredSize = Dimension(40, 20)
        unitComboBox.model = DefaultComboBoxModel<String>(TimeUnit.unitNames())
        unitComboBox.addActionListener { notifyDataFieldChangeListeners() }
        interval = inputTimeInterval
        add(label)
        add(scalarField)
        add(unitComboBox)
    }

    private fun notifyDataFieldChangeListeners() = BlackBoard.post(Update(UpdateType.INPUTFIELD_CHANGED))
}