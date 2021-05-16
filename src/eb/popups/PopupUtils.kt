package eb.popups

import javax.swing.JOptionPane

object PopupUtils {
    fun closeOptionPane() = JOptionPane.getRootFrame().dispose()
}