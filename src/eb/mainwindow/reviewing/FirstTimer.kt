package eb.mainwindow.reviewing

import eb.utilities.doNothing
import java.time.Instant

/**
 * FirstTimer registers a time the first time it is activated (set). Subsequently calling 'press' does not change
 * its value. Is useful if something has to happen multiple times (like repainting) but only the instant of
 * first usage is important.
 *
 * @author Eric-Wubbo Lameijer
 */
internal class FirstTimer {
    private var firstInstant: Instant? = null

    // returns the instant stored in this object, throws an exception if someone tries to use a FirstTimer object
    // erroneously [I could have returned an Optional, but throwing exceptions helps find logic errors]
    fun instant() : Instant {
        require(firstInstant != null) {
            "FirstTimer.getInstant() error: attempt to use time object before any time has been registered."}
        return firstInstant!!
    }

    // press: if the FirstTimer object is not yet storing a time (Instant), it will store the current time
    fun press() = if (firstInstant == null) firstInstant = Instant.now() else doNothing

    // reset: empties the FirstTimer object, so it can be reused to store a new time point (Instant)
    fun reset() {
        firstInstant = null
    }
}