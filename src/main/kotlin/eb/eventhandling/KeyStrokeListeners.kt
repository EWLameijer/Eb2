package eb.eventhandling

import eb.utilities.ProgrammableAction
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.KeyStroke

fun JFrame.createKeyListener(keyEvent: Int, action: () -> Unit) {
    rootPane.createKeyListener(KeyStroke.getKeyStroke(keyEvent, 0), action)
}

fun JComponent.createKeyListener(keyStroke: KeyStroke, action: () -> Unit) {
    val eventId = "Pressed$keyStroke"
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, eventId)
    actionMap.put(eventId, ProgrammableAction(action))
}