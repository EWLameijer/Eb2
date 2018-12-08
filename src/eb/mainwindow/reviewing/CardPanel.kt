package eb.mainwindow.reviewing

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font

import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.text.AbstractDocument
import javax.swing.text.BoxView
import javax.swing.text.ComponentView
import javax.swing.text.Element
import javax.swing.text.IconView
import javax.swing.text.LabelView
import javax.swing.text.ParagraphView
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import javax.swing.text.StyledEditorKit
import javax.swing.text.View
import javax.swing.text.ViewFactory

import eb.utilities.Utilities
import java.lang.RuntimeException

/**
 * A CardPanel shows a side of a card (either the front or the back). It is
 * basically a graphical display used during the review process, unlike the
 * TextAreas used for input.
 *
 * @author Eric-Wubbo Lameijer
 */
class CardPanel : JPanel() {

    // the area on which the text is to be displayed
    private val m_textPane: JTextPane

    /**
     * CardPanel constructor.
     */
    init {
        layout = BorderLayout()
        m_textPane = JTextPane()
        m_textPane.isEditable = false
        m_textPane.font = Font("Arial", Font.PLAIN, 30)
        m_textPane.border = BorderFactory.createLineBorder(Color.black)

        // from
        // http://stackoverflow.com/questions/3213045/centering-text-in-a-jtextarea-or-jtextpane-horizontal-text-alignment

        m_textPane.editorKit = MyEditorKit()
        val doc = m_textPane.styledDocument
        val center = SimpleAttributeSet()
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER)
        doc.setParagraphAttributes(0, doc.length, center, false)
        add(JScrollPane(m_textPane), BorderLayout.CENTER)
    }

    /**
     * Sets the text to be displayed in this panel. Can be an empty string (if the
     * panel must yet remain empty).
     *
     * @param text
     * the text to display in this panel.
     */
    internal fun setText(text: String?) {
        require(text != null) { "CardPanel.setText() error: the text cannot be null." }
        m_textPane.text = text
        repaint()
    }

}

internal class MyEditorKit : StyledEditorKit() {

    override fun getViewFactory(): ViewFactory {
        return StyledViewFactory()
    }

    internal class StyledViewFactory : ViewFactory {

        override fun create(elem: Element): View {
            val kind = elem.name
            var view: View? = null
            if (kind != null) {
                view = when (kind) {
                    AbstractDocument.ContentElementName ->  LabelView(elem)
                    AbstractDocument.ParagraphElementName -> ParagraphView(elem)
                    AbstractDocument.SectionElementName -> CenteredBoxView(elem, View.Y_AXIS)
                    StyleConstants.ComponentElementName -> ComponentView(elem)
                    StyleConstants.IconElementName -> IconView(elem)
                    else -> throw RuntimeException( "StyledViewFactory.create error: unknown element name")
                }
            }
            if (view == null) {
                view = LabelView(elem)
            }
            return view
        }

    }
}

internal class CenteredBoxView(elem: Element, axis: Int) : BoxView(elem, axis) {

    override fun layoutMajorAxis(targetSpan: Int, axis: Int, offsets: IntArray,
                                 spans: IntArray) {

        super.layoutMajorAxis(targetSpan, axis, offsets, spans)
        var textBlockHeight = 0

        for (i in spans.indices) {

            textBlockHeight = spans[i]
        }
        if (textBlockHeight * offsets.size < targetSpan) {
            val offset = (targetSpan - textBlockHeight) / 2
            for (i in offsets.indices) {
                offsets[i] += offset
            }
        }
    }
}
