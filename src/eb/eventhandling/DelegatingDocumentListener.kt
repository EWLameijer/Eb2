package eb.eventhandling

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class DelegatingDocumentListener(// the function that handles all update requests
        private val m_handler: () -> Unit) : DocumentListener {

    private fun processUpdate() {
        m_handler
    }

    override fun changedUpdate(arg0: DocumentEvent) {
        processUpdate()

    }

    override fun insertUpdate(arg0: DocumentEvent) {
        processUpdate()

    }

    override fun removeUpdate(arg0: DocumentEvent) {
        processUpdate()
    }

}
