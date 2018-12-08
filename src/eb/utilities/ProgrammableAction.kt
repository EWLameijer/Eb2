package eb.utilities

import java.awt.event.ActionEvent

import javax.swing.AbstractAction

class ProgrammableAction(@field:Transient private val m_action: () -> Unit) : AbstractAction() {

    override fun actionPerformed(ae: ActionEvent) {
        m_action()
    }
}