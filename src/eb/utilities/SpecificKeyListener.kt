package eb.utilities

import java.awt.event.KeyEvent
import java.awt.event.KeyListener

class SpecificKeyListener(private val keyCode: Int, val action: () -> Unit) : KeyListener {
    override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == keyCode) {
            e.consume()
            action()
        }
    }

    // dummy method: we only need to respond to the pressing of the key, not to the release.
    override fun keyReleased(arg0: KeyEvent) = doNothing

    // dummy method: we only need to respond to the pressing of the key, not to it being typed
    override fun keyTyped(arg0: KeyEvent) = doNothing
}