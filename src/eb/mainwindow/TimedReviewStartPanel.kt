package eb.mainwindow

import javax.swing.JButton
import javax.swing.JPanel

import eb.eventhandling.BlackBoard
import eb.eventhandling.Update
import eb.eventhandling.UpdateType

class TimedReviewStartPanel internal constructor() : JPanel() {

    init {
        val startButton = JButton("Start reviewing")
        val postponeButton = JButton("Postpone reviewing")
        postponeButton.addActionListener {
            BlackBoard.post(Update(UpdateType.PROGRAMSTATE_CHANGED,
                    MainWindowState.INFORMATIONAL.name))
        }
        add(startButton)
        add(postponeButton)
    }

}
