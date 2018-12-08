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

import eb.data.Card
import eb.data.DeckManager
import eb.subwindow.CardEditingManager
import eb.utilities.Hint
import eb.utilities.ProgrammableAction
import javax.swing.*

/**
 * The panel used to review cards (shows front, on clicking "Show Answer" the
 * answer is shown, after which the user can confirm or deny that he/she knew
 * the correct answer).
 *
 * @author Eric-Wubbo Lameijer
 */
class ReviewPanel : JPanel() {

    private val m_frontOfCardPanel: CardPanel
    private val m_backOfCardPanel: CardPanel
    private val m_situationalButtonPanel: JPanel
    private val m_fixedButtonPanel: JPanel

    private fun currentCard() =
            if (ReviewManager.instance.currentFront() != EMPTY_FRONT)
                DeckManager.currentDeck!!.cards.getCardWithFront(Hint(ReviewManager.instance.currentFront()))
            else null


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
        m_frontOfCardPanel = CardPanel()
        m_frontOfCardPanel.background = Color.PINK

        add(m_frontOfCardPanel, frontOfCardConstraints)
        val backOfCardConstraints = GridBagConstraints()
        backOfCardConstraints.gridx = 0
        backOfCardConstraints.gridy = 2
        backOfCardConstraints.gridwidth = 4
        backOfCardConstraints.gridheight = 2
        backOfCardConstraints.weightx = 4.0
        backOfCardConstraints.weighty = 2.0
        backOfCardConstraints.fill = GridBagConstraints.BOTH
        m_backOfCardPanel = CardPanel()
        m_backOfCardPanel.background = Color.YELLOW
        add(m_backOfCardPanel, backOfCardConstraints)

        val buttonPanelForHiddenBack = JPanel()
        buttonPanelForHiddenBack.layout = FlowLayout()
        val showAnswerButton = JButton("Show Answer")
        showAnswerButton.mnemonic = KeyEvent.VK_S
        showAnswerButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('s'), "show answer")
        showAnswerButton.actionMap.put("show answer",
                ProgrammableAction { showAnswer() })
        showAnswerButton.addActionListener { showAnswer() }
        buttonPanelForHiddenBack.add(showAnswerButton)

        val buttonPanelForShownBack = JPanel()
        val rememberedButton = JButton("Remembered")
        rememberedButton.mnemonic = KeyEvent.VK_R
        rememberedButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('r'), "remembered")
        rememberedButton.actionMap.put("remembered",
                ProgrammableAction { remembered(true) })
        rememberedButton.addActionListener { remembered(true) }
        buttonPanelForShownBack.add(rememberedButton)
        val forgottenButton = JButton("Forgotten")
        forgottenButton.mnemonic = KeyEvent.VK_F
        forgottenButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('f'), "forgotten")
        forgottenButton.actionMap.put("forgotten",
                ProgrammableAction { remembered(false) })

        forgottenButton.addActionListener { remembered(false) }
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
        m_situationalButtonPanel = JPanel()
        m_situationalButtonPanel.layout = CardLayout()
        m_situationalButtonPanel.add(buttonPanelForHiddenBack, HIDDEN_ANSWER)
        m_situationalButtonPanel.add(buttonPanelForShownBack, SHOWN_ANSWER)
        m_situationalButtonPanel.background = Color.GREEN
        add(m_situationalButtonPanel, situationalButtonPanelConstraints)

        val editButton = JButton("Edit card")
        editButton.mnemonic = KeyEvent.VK_E
        editButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('e'), "edit")
        editButton.actionMap.put("edit",
                ProgrammableAction { editCard() })
        editButton.addActionListener { editCard() }

        val deleteButton = JButton("Delete card")
        deleteButton.mnemonic = KeyEvent.VK_D
        deleteButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('d'), "delete")
        deleteButton.actionMap.put("delete",
                ProgrammableAction { deleteCard() })
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
        m_fixedButtonPanel = JPanel()
        m_fixedButtonPanel.add(editButton)
        m_fixedButtonPanel.add(deleteButton)
        add(m_fixedButtonPanel, fixedButtonPanelConstraints)

        // panel, to be used in future to show successful/unsuccessful cards.
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

    private fun editCard() {
        val currentCard = currentCard()
        CardEditingManager(currentCard)
    }

    private fun deleteCard() {
        val choice = JOptionPane.showConfirmDialog(m_backOfCardPanel,
                "Delete this card?", "Delete this card?", JOptionPane.OK_CANCEL_OPTION)
        if (choice == JOptionPane.OK_OPTION) {
            DeckManager.currentDeck!!.cards.removeCard(currentCard()!!)
        }
    }

    private fun remembered(wasRemembered: Boolean) {
        showPanel(HIDDEN_ANSWER)
        ReviewManager.instance.wasRemembered(wasRemembered)
        repaint()
    }

    private fun showAnswer() {
        showPanel(SHOWN_ANSWER)
        ReviewManager.instance.showAnswer()
        repaint()
    }

    private fun showPanel(panelName: String) {
        val cardLayout = m_situationalButtonPanel.layout as CardLayout
        cardLayout.show(m_situationalButtonPanel, panelName)
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        m_frontOfCardPanel.setText(ReviewManager.instance.currentFront())
    }

    fun refresh() {
        repaint()
    }

    fun updatePanels(frontText: String, backText: String,
                     showAnswer: Boolean) {
        m_frontOfCardPanel.setText(frontText)
        m_backOfCardPanel.setText(backText)
        showPanel(if (showAnswer) SHOWN_ANSWER else HIDDEN_ANSWER)

    }

    companion object {

        private const val HIDDEN_ANSWER = "HIDDEN_ANSWER"
        private const val SHOWN_ANSWER = "SHOWN_ANSWER"
        private const val EMPTY_FRONT = ""
    }

}
