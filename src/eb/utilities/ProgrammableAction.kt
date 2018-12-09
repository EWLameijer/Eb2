package eb.utilities

import java.awt.event.ActionEvent

import javax.swing.AbstractAction

// for creating something with an Action interface; existing classes with an ActionInterface are generally
// so specific that using them is hard and/or misleading,
// Action { ... } and AbstractAction{ ... } don't work as you cannot instantiate an interface or an abstract class.
// { e: ActionEvent -> showAnswer() } as AbstractAction also doesn't work
class ProgrammableAction(private val m_action: () -> Unit) : AbstractAction() {
    override fun actionPerformed(ae: ActionEvent) = m_action()
}