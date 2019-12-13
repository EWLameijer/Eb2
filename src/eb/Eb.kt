package eb

import java.io.IOException
import java.net.ServerSocket
import javax.swing.JOptionPane

import eb.mainwindow.MainWindow

/**
 * Runs Eb.
 *
 * @author Eric-Wubbo Lameijer
 */
object Eb {

    const val VERSION_STRING = "1.3"

    @JvmStatic
    fun main(args: Array<String>) {
        // Avoid multiple instances of Eb running at same time. From
        // http://stackoverflow.com/questions/19082265/how-to-ensure-only-one-instance-of-a-java-program-can-be-executed

        try {
            // create object of server socket and bind to some port number
            ServerSocket(14356)
            // do not put common port number like 80 etc. Because they are already used by system
            // If another instance exists, show message and terminates the current instance.
            // Otherwise starts application.

            MainWindow

        } catch (exc: IOException) {
            JOptionPane.showMessageDialog(null, "The application is already running.....",
                    "Access Error", JOptionPane.ERROR_MESSAGE)
            System.exit(0)
        }
    }
}
