package eb.popups

import eb.data.Card
import eb.data.DeckManager
import java.awt.Component
import javax.swing.JOptionPane


fun deleteCard(parent: Component, card: Card) {
    val choice = JOptionPane.showConfirmDialog(
        parent,
        "Delete this card?", "Delete this card?", JOptionPane.OK_CANCEL_OPTION
    )
    if (choice == JOptionPane.OK_OPTION) {
        DeckManager.currentDeck().cardCollection.removeCard(card)
    }
}