package eb.utilities.uiElements

import java.awt.event.ActionListener
import javax.swing.JButton

class UnfocusableButton (text: String, action: ActionListener) : JButton(text) {
    init {
        addActionListener(action)
        isFocusable = false
    }
}