package eb.utilities.ui_elements

import java.awt.Dimension
import java.util.Optional

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
class TimeInputElement
/**
 * Constructs the TimeInputElement, given a name and a TimeInterval (which can
 * contain something like "3 hour(s)".
 *
 * @param name
 * the name of the interval, which is shown on the label
 * @param timeInterval
 * the time interval to be displayed, like "3 hour(s)".
 */
private constructor(name: String, timeInterval: TimeInterval) : JPanel() {

    // Label indicating the identity of the interval (for example "interval
    // before new card is first shown)
    private val m_label: JLabel

    // Text field that sets the quantity (the '3' in 3 hours), the combo box
    // selects the unit (the hours in "3 hours").
    private val m_scalarField: JTextField

    // Combo box used (in combination with a text field) to set the value for the
    // initial study interval. Contains the hours of "3 hours".
    private val m_unitComboBox: JComboBox<String>

    /**
     * Returns the time interval encapsulated by this TimeInputElement.
     *
     * @return the time interval encapsulated by this TimeInputElement
     */
    /**
     * Sets the interval (displayed in the text field and combo box) to the given
     * time interval.
     *
     * @param timeInterval
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
            val timeUnit = TimeUnit.parseUnit(m_unitComboBox.selectedItem!!.toString())
            require(timeUnit.isPresent) {
                    "TimeInterval.getInterval() error: the time unit is wrong for some reason."}

            val parsedNumber = Utilities.stringToDouble(m_scalarField.text)
            val timeIntervalScalar = parsedNumber ?: 0.01
            return TimeInterval(timeIntervalScalar, timeUnit.get())
        }
        set(timeInterval) {
            require(timeInterval != null) {
                    "TimeInputElement.setInterval error: the provided time interval may not be null."}
            m_scalarField.text = Utilities.doubleToMaxPrecisionString(timeInterval.scalar, 2)
            m_unitComboBox.selectedItem = timeInterval.unit.userInterfaceName
            notifyDataFieldChangeListeners()
        }

    init {

        require(name.isValidIdentifier) {
                "TimeInputElement constructor error: the name should be a valid identifier"}

        m_label = JLabel(name)
        m_scalarField = JTextField()

        m_scalarField.document = FixedSizeNumberDocument(m_scalarField, 5, 2)
        m_scalarField.document.addDocumentListener( DelegatingDocumentListener { notifyDataFieldChangeListeners() })

        m_scalarField.preferredSize = Dimension(40, 20)
        m_unitComboBox = JComboBox()
        m_unitComboBox.model = DefaultComboBoxModel<String>(TimeUnit.unitNames)
        m_unitComboBox.addActionListener { notifyDataFieldChangeListeners() }
        interval = timeInterval
    }// preconditions: name should be valid; the timeInterval will be checked
    // by setInterval.

    /**
     * Initializes the TimeInputElement - this needs to be separate from the
     * constructor, or else the nullness checker will complain.
     */
    private fun init() {
        add(m_label)
        add(m_scalarField)
        add(m_unitComboBox)
    }

    private fun notifyDataFieldChangeListeners() {
        BlackBoard.post(Update(UpdateType.INPUTFIELD_CHANGED))
    }

    companion object {

        private val serialVersionUID = 4711963487097757055L

        /**
         * Factory method to create a TimeInputElement, which is basically a small
         * JPanel that can be used to display a time. Factory method needed to handle
         * the separation between object construction and initialization 'stimulated'
         * by the nullness checker.
         *
         * @param name
         * the name of the interval, which is shown on the label
         * @param timeInterval
         * the time interval to be displayed, like "3 hour(s)".
         */
        fun createInstance(name: String, timeInterval: TimeInterval): TimeInputElement {
            val timeInputElement = TimeInputElement(name, timeInterval)
            timeInputElement.init()
            return timeInputElement
        }
    }
}