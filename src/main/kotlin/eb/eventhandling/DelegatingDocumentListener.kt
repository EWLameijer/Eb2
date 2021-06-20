package eb.eventhandling

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

// the function that handles all update requests
class DelegatingDocumentListener(private val handler: () -> Unit) : DocumentListener {

    private fun processUpdate() = handler()

    override fun changedUpdate(arg0: DocumentEvent) = processUpdate()

    override fun insertUpdate(arg0: DocumentEvent) = processUpdate()

    override fun removeUpdate(arg0: DocumentEvent) = processUpdate()
}
