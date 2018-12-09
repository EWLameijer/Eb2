package eb.mainwindow.reviewing

import java.awt.CardLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ComponentListener
import java.awt.event.KeyEvent
import java.beans.EventHandler

import eb.data.DeckManager
import eb.subwindow.CardEditingManager
import eb.utilities.ProgrammableAction
import javax.swing.*

/**
 * The panel used to review cards (shows front, on clicking "Show Answer" the answer is shown, after which the user
 * can confirm or deny that he/she knew the correct answer).
 *
 * @author Eric-Wubbo Lameijer
 */
class ReviewPanel : JPanel() {

    private val frontOfCardPanel = CardPanel()
    private val backOfCardPanel = CardPanel()
    private val situationalButtonPanel = JPanel()
    private val fixedButtonPanel = JPanel()

    init {
        this.isFocusable = true
        addComponentListener(EventHandler.create(ComponentListener::class.java, this,
                "requestFocusInWindow", null, "componentShown"))

        layout = GridBagLayout()
        val frontOfCardConstraints = GridBagConstraints()

        frontOfCardConstraints.gridx = 0
        frontOfCardConstraints.gridy = 0
        frontOfCardConstraints.gridwidth = 4
        frontOfCardConstraints.gridheight = 2
        frontOfCardConstraints.weightx = 4.0
        frontOfCardConstraints.weighty = 2.0
        frontOfCardConstraints.fill = GridBagConstraints.BOTH
        frontOfCardPanel.background = Color.PINK
        add(frontOfCardPanel, frontOfCardConstraints)

        val backOfCardConstraints = GridBagConstraints()
        backOfCardConstraints.gridx = 0
        backOfCardConstraints.gridy = 2
        backOfCardConstraints.gridwidth = 4
        backOfCardConstraints.gridheight = 2
        backOfCardConstraints.weightx = 4.0
        backOfCardConstraints.weighty = 2.0
        backOfCardConstraints.fill = GridBagConstraints.BOTH
        backOfCardPanel.background = Color.YELLOW
        add(backOfCardPanel, backOfCardConstraints)

        val buttonPanelForHiddenBack = JPanel()
        buttonPanelForHiddenBack.layout = FlowLayout()
        val showAnswerButton = JButton("Show Answer")
        showAnswerButton.mnemonic = KeyEvent.VK_S
        showAnswerButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('s'), "show answer")
        showAnswerButton.actionMap.put("show answer", ProgrammableAction { showAnswer() })
        showAnswerButton.addActionListener { showAnswer() }
        buttonPanelForHiddenBack.add(showAnswerButton)

        val buttonPanelForShownBack = JPanel()
        val rememberedButton = JButton("Remembered")
        rememberedButton.mnemonic = KeyEvent.VK_R
        rememberedButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('r'), "remembered")
        rememberedButton.actionMap.put("remembered", ProgrammableAction { registerAnswer(true) })
        rememberedButton.addActionListener { registerAnswer(true) }
        buttonPanelForShownBack.add(rememberedButton)
        val forgottenButton = JButton("Forgotten")
        forgottenButton.mnemonic = KeyEvent.VK_F
        forgottenButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('f'), "forgotten")
        forgottenButton.actionMap.put("forgotten", ProgrammableAction { registerAnswer(false) })
        forgottenButton.addActionListener { registerAnswer(false) }
        buttonPanelForShownBack.add(forgottenButton)

        // for buttons that depend on the situation, like when the back of the card
        // is shown or when it is not yet shown.
        val situationalButtonPanelConstraints = GridBagConstraints()
        situationalButtonPanelConstraints.gridx = 0
        situationalButtonPanelConstraints.gridy = 4
        situationalButtonPanelConstraints.gridwidth = 3
        situationalButtonPanelConstraints.gridheight = 1
        situationalButtonPanelConstraints.weightx = 3.0
        situationalButtonPanelConstraints.weighty = 1.0
        situationalButtonPanelConstraints.fill = GridBagConstraints.BOTH
        situationalButtonPanel.layout = CardLayout()
        situationalButtonPanel.add(buttonPanelForHiddenBack, HIDDEN_ANSWER)
        situationalButtonPanel.add(buttonPanelForShownBack, SHOWN_ANSWER)
        situationalButtonPanel.background = Color.GREEN
        add(situationalButtonPanel, situationalButtonPanelConstraints)

        val editButton = JButton("Edit card")
        editButton.mnemonic = KeyEvent.VK_E
        editButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('e'), "edit")
        editButton.actionMap.put("edit", ProgrammableAction { editCard() })
        editButton.addActionListener { editCard() }

        val deleteButton = JButton("Delete card")
        deleteButton.mnemonic = KeyEvent.VK_D
        deleteButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('d'), "delete")
        deleteButton.actionMap.put("delete", ProgrammableAction { deleteCard() })
        deleteButton.addActionListener { deleteCard() }

        // the fixed button panel contains buttons that need to be visible always
        val fixedButtonPanelConstraints = GridBagConstraints()
        fixedButtonPanelConstraints.gridx = 3
        fixedButtonPanelConstraints.gridy = 4
        fixedButtonPanelConstraints.gridwidth = 1
        fixedButtonPanelConstraints.gridheight = 1
        fixedButtonPanelConstraints.weightx = 1.0
        fixedButtonPanelConstraints.weighty = 1.0
        fixedButtonPanelConstraints.fill = GridBagConstraints.BOTH
        fixedButtonPanel.add(editButton)
        fixedButtonPanel.add(deleteButton)
        add(fixedButtonPanel, fixedButtonPanelConstraints)

        // panel, to be used in future to show successful/unsuccessful cards
        // for now hidden?
        val sidePanelConstraints = GridBagConstraints()
        sidePanelConstraints.gridx = 4
        sidePanelConstraints.gridy = 0
        sidePanelConstraints.gridwidth = 1
        sidePanelConstraints.gridheight = 5
        sidePanelConstraints.fill = GridBagConstraints.BOTH
        sidePanelConstraints.weightx = 1.0
        sidePanelConstraints.weighty = 5.0
        val sidePanel = JPanel()
        sidePanel.background = Color.RED
        add(sidePanel, sidePanelConstraints)
    }

    private fun editCard() = CardEditingManager(ReviewManager.currentCard())

    private fun deleteCard() {
        val choice = JOptionPane.showConfirmDialog(backOfCardPanel,
                "Delete this card?", "Delete this card?", JOptionPane.OK_CANCEL_OPTION)
        if (choice == JOptionPane.OK_OPTION)
            DeckManager.currentDeck().cardCollection.removeCard(ReviewManager.currentCard()!!)
    }

    private fun registerAnswer(wasRemembered: Boolean) {
        showPanel(HIDDEN_ANSWER)
        ReviewManager.wasRemembered(wasRemembered)
        repaint()
    }

    private fun showAnswer() {
        showPanel(SHOWN_ANSWER)
        ReviewManager.showAnswer()
        repaint()
    }

    private fun showPanel(panelName: String) {
        val cardLayout = situationalButtonPanel.layout as CardLayout
        cardLayout.show(situationalButtonPanel, panelName)
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        frontOfCardPanel.setText(ReviewManager.currentFront())
    }

    fun refresh() =  repaint()

    fun updatePanels(frontText: String, backText: String, showAnswer: Boolean) {
        frontOfCardPanel.setText(frontText)
        backOfCardPanel.setText(backText)
        showPanel(if (showAnswer) SHOWN_ANSWER else HIDDEN_ANSWER)
    }

    companion object {
        private const val HIDDEN_ANSWER = "HIDDEN_ANSWER"
        private const val SHOWN_ANSWER = "SHOWN_ANSWER"
    }
}
