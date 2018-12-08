package eb.utilities.ui_elements

import javax.swing.JPanel

import eb.eventhandling.BlackBoard
import eb.eventhandling.Update
import eb.eventhandling.UpdateType

abstract class InputPanel : JPanel() {
    protected fun notifyOfChange() {
        BlackBoard.post(Update(UpdateType.INPUTFIELD_CHANGED))
    }

    abstract fun areContentsEqual(otherInputPanel: InputPanel): Boolean

}
