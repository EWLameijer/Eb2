package eb

import java.io.IOException
import java.net.ServerSocket
import javax.swing.JOptionPane
import java.net.*

import eb.mainwindow.MainWindow
import kotlin.system.exitProcess

/**
 * Runs Eb.
 *
 * @author Eric-Wubbo Lameijer
 */

object SingleInstance {
    private var ss: ServerSocket? = null

    fun alreadyRunning(): Boolean {
        try {
            ss = ServerSocket(65000, 10, InetAddress.getLocalHost()) // using private port 65000
        }
        catch (e: IOException) {
            // port already in use so an instance is already running
            return true
        }
        return false
    }

    fun close() {
        if (ss == null || ss?.isClosed() == true) return
        ss?.close()
    }
}


object Eb {

    const val VERSION_STRING = "1.3"
    private var ss: ServerSocket? = null

    @JvmStatic
    fun main(args: Array<String>) {
        // Avoid multiple instances of Eb running at same time. From
        // http://stackoverflow.com/questions/19082265/how-to-ensure-only-one-instance-of-a-java-program-can-be-executed
        try {
            // create object of server socket and bind to some port number
            // ServerSocket(65000, 10, InetAddress.getLocalHost()) // using private port 65000 // DOES NOT WORK
            ss = ServerSocket(65000, 10, InetAddress.getLocalHost()) // using private port 65000 // DOES NOT WORK
            // ServerSocket(14356, 10, InetAddress.getLocalHost())
            // do not put common port number like 80 etc. Because they are already used by system
            // If another instance exists, show message and terminates the current instance.
            // Otherwise starts application.

            MainWindow()

        } catch (exc: IOException) {
            JOptionPane.showMessageDialog(null, "The application is already running.....",
                    "Access Error", JOptionPane.ERROR_MESSAGE)
            exitProcess(0)
        }
    }
}
