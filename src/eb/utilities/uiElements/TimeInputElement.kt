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
class TimeInputElement(name: String, timeInterval: TimeInterval) : JPanel() {
    /* @param name
    * the name of the interval, which is shown on the label
    * @param timeInterval
    * the time interval to be displayed, like "3 hour(s)".
    // Label indicating the identity of the interval (for example "interval
    // before new card is first shown)*/
    private val label: JLabel

    // Text field that sets the quantity (the '3' in 3 hours), the combo box
    // selects the unit (the hours in "3 hours").
    private val scalarField: JTextField

    // Combo box used (in combination with a text field) to set the value for the
    // initial study interval. Contains the hours of "3 hours".
    private val unitComboBox: JComboBox<String>

    /**
     * Returns the time interval encapsulated by this TimeInputElement.
     *
     * @return the time interval encapsulated by this TimeInputElement
     */
    /**
     * Sets the interval (displayed in the text field and combo box) to the given
     * time interval.
     *
     * the time interval that should be displayed by this
     * TimeInputElement (text field and combo box).
     */
    // preconditions: timeInterval should not be null
    /*
		 * Logger.getGlobal().info(
		 * Utilities.doubleToMaxPrecisionString(timeInterval.getScalar(), 2));
		 */// postconditions: none. I assume that all goes well.
    var interval: TimeInterval?
        get() {
            val timeUnit = TimeUnit.parseUnit(unitComboBox.selectedItem!!.toString())
            require(timeUnit != null) {
                    "TimeInterval.getInterval() error: the time unit is wrong for some reason."}

            val parsedNumber = Utilities.stringToDouble(scalarField.text)
            val timeIntervalScalar = parsedNumber ?: 0.01
            return TimeInterval(timeIntervalScalar, timeUnit)
        }
        set(timeInterval) {
            require(timeInterval != null) {
                    "TimeInputElement.setInterval error: the provided time interval may not be null."}
            scalarField.text = Utilities.doubleToMaxPrecisionString(timeInterval.scalar, 2)
            unitComboBox.selectedItem = timeInterval.unit.userInterfaceName
            notifyDataFieldChangeListeners()
        }

    init {
        require(name.isValidIdentifier) { "TimeInputElement constructor error: the name should be a valid identifier" }

        label = JLabel(name)
        scalarField = JTextField()

        scalarField.document = FixedSizeNumberDocument(scalarField, 5, 2)
        scalarField.document.addDocumentListener( DelegatingDocumentListener { notifyDataFieldChangeListeners() })

        scalarField.preferredSize = Dimension(40, 20)
        unitComboBox = JComboBox()
        unitComboBox.model = DefaultComboBoxModel<String>(TimeUnit.unitNames())
        unitComboBox.addActionListener { notifyDataFieldChangeListeners() }
        interval = timeInterval
        add(label)
        add(scalarField)
        add(unitComboBox)

    }// preconditions: name should be valid; the timeInterval will be checked by setInterval.


    private fun notifyDataFieldChangeListeners() = BlackBoard.post(Update(UpdateType.INPUTFIELD_CHANGED))


    companion object {
        private const val serialVersionUID = 4711963487097757055L
    }
}