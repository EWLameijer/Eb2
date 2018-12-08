package eb

import java.io.IOException
import java.net.ServerSocket

import javax.swing.JOptionPane

import eb.eventhandling.BlackBoard
import eb.eventhandling.UpdateType
import eb.mainwindow.MainWindow
import eb.mainwindow.reviewing.ReviewManager

/**
 * Runs Eb.
 *
 * @author Eric-Wubbo Lameijer
 */
object Eb {

    var VERSION_STRING = "1.3"

    private var errortype = "Access Error"
    var error = "The application is already running....."
    @JvmStatic
    fun main(args: Array<String>) {
        // Avoid multiple instances of Eb running at same time. From
        // http://stackoverflow.com/questions/19082265/how-to-ensure-only-one-instance-of-a-java-program-can-be-executed

        /*
		 * The method ManagementFactory.getRuntimeMXBean() returns an identifier
		 * with application PID in the Sun JVM, but each JVM may have you own
		 * implementation. So in a JVM, other than Sun, this code may not work., :(
		 */


        try {
            //creating object of server socket and bind to some port number
            val serverSocket = ServerSocket(14356)
            ////do not put common port number like 80 etc.
            ////Because they are already used by system
            // If exists another instance, show message and terminates the current
            // instance.
            // Otherwise starts application.

            /* getMonitoredVMs(runtimePid) */
            val reviewManager = ReviewManager.instance
            BlackBoard.register(reviewManager, UpdateType.DECK_SWAPPED)
            BlackBoard.register(reviewManager, UpdateType.CARD_CHANGED)
            BlackBoard.register(reviewManager, UpdateType.DECK_CHANGED)
            MainWindow.display()
        } catch (exc: IOException) {
            JOptionPane.showMessageDialog(null, error, errortype, JOptionPane.ERROR_MESSAGE)
            System.exit(0)
        }

    }
}
/**
 * Hide the implicit public constructor.
 */
