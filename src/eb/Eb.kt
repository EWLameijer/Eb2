package eb

import java.io.IOException
import java.net.ServerSocket
import javax.swing.JOptionPane
import java.net.*

import eb.mainwindow.MainWindow
import kotlin.system.exitProcess

// FUTURE PLANS:
// ?Possibly faster adaptation (set lower limit on #rep needed, or intelligent add?)
// enable pictures to be shown with cards
// better sorting of repeated cards [what does that mean?]
// ? Allow Eb to run WITHOUT taking up two taskbar slots?

// 2.6.4: Now also saves deck if only the studyoptions have been changed.
// 2.6.3: Can now merge decks; also, escape now properly returns the edit window sidebar to front list mode
// 2.6.2: Small correction to make card-front-side list appear when OK button is pressed, and generalize reviewing times also if the last streak length has over cutoff events.
// 2.6.1: Now shows history info when a card is selected
// 2.6.0: Now changes weird format '"' (like from Excel) into normal double quotes
// 2.5.9: Now updates next review time in information screen after a review session
// 2.5.8: Nicer format for reviewing times in information window
// 2.5.7: Indicates which decks need to be reviewed now
// 2.5.6: Will now also show shortcut name in title (handy for my notes)
// 2.5.5: now will also show contents of cards from linked deck.
// 2.5.4: now should update reviewing times when returning to information screen, instead of only on reloading
// 2.5.3: increased shortcut capacity to 19 decks
// 2.5.2: now also indicates tomorrow next to today for next review
// 2.5.1: solve bug that timer also transfers to non-timed deck in some circumstances.
// 2.5.0: solve bug that also decks after a 'limited time review' deck take over the limited timer.
// 2.4.8: updating the timer function with years, and a bit of quality-of-life-improvement with archiving directory remembering
// 2.4.7: Adding an indication of total study value
// 2.4.6: With functioning (though optional) timer settings
// 2.4.5: Bugfix solving problems loading decks of which cards had not yet been reviewed
// 2.4.4: Small bugfix (could not create new deck anymore at first!). Internally, some refactoring.
// 2.4.3: Now also displays WHEN the next review will take place (so the date and time)
// 2.4.2: Now correctly turns ") ," into "), "
// 2.4.1: Make it easier to move decks between computers without having to reset all archiving directories.
// 2.4.0: improved formatting of (),[] and {} in card texts
// 2.3.9: added '-' to front of patterns in analysis file, making them easier to search
// 2.3.8: Small bugfix allowing default settings to be loaded again into options pane
// 2.3.7: Enables the creation of deck shortcuts to load much-used decks easily
// 2.3.6: Now also allows deletion of cards in edit window.
// 2.3.5: In the edit window, tab does not rotate to the clear button and the side list anymore, making editing more convenient
// 2.3.4: If the edit window has contents, escape clears it. If the edit window is empty, it is closed.
// 2.3.3: Automatically close edit windows when swapping a deck.
// 2.3.2: Made life a bit easier when modifying a card selected via the side list
// 2.3.1.DEBUG: removed small error in the quote correction based on testing
// 2.3.0.DEBUG: slightly better auto-correction of quotes, also with their interaction with ) and such...
// 2.2.9.DEBUG: now auto-corrects quote formats (so a" b  " -> a "b")
// 2.2.8.DEBUG Try check why Arabic Eb can take such a long time...
// 2.2.8: Make Eb run under Java 8 as well, and make list of terms on side of edit window scrollable.
// 2.2.7: Make it easier to compare card backs while replacing by putting them below each other
// 2.2.6: Now also reports how much time has passed since the last view (being the last review, or card creation if no review has yet taken place)
// 2.2.5: Now makes the review history visible after review, so one can see how well the study of this particular card has been going
// 2.2.4: Solved small annoyances: that Escape key did not always work anymore when editing, and that clicking OK on a loaded (but unmodified) card showed the default dialogue of 'do you want to overwrite this'
// 2.2.3: Solving bugs discovered in 2.2.2 (should sort a list before trying to get median!)
// 2.2.2: Solving bugs discovered in 2.2.1 (don't fail adjusting time when there are no successful reviews)
// 2.2.1: Making the reviews follow median instead of average times, as especially on short time scales the average was much higher than the median.
// 2.2.0: Changing/adjusting a card loaded from the menu is now more convenient: two options instead of one. Also solved two bugs/exceptions.
// 2.1.9: Now also adds an option in the study options menu to set the desired success percentage.
// 2.1.8: Added the ability to tweak the reviewing times automatically based on successes and failures.
// 2.1.7: Added 'Clear' button to edit panel, as well as refining the search that you can also search for substrings in the bottom card.
// 2.1.6: Added overview panel so you can see when you are creating a duplicate card before you have fully created it
// 2.1.5: Solved bug where a failed review was not shown properly in the statistics window if it had been reviewed multiple times during a session.
// 2.1.4: Prioritize reviewing of already-reviewed cards (so only new cards skipped if needed). Sorting algorithm should work properly now
// 2.1.3: Solved problems when starting up on other computer, partly due to decimal point differences
// 2.1.2: Ensure that after merging cards in triple mode, the separate merging window is disposed of after it has fulfilled its purpose
// 2.1.1: Prioritize reviewing of known cards. Not yet prioritized for relative delay.
// 2.1.0: Ensure that 'delete this card' works properly with the 3-sided creation window. Also shows card merging more clearly.
// 2.0.9: Fix to make it impossible to open duplicate Eb instances (not sure how it works, though... Possibly a var? not optimized away by compiler).
// .....: ALSO allow multi-line-input to shift focus to the last card, so Ctrl-V Tab Enter should do the trick
// 2.0.8: Makes the 'duplicate card insertion' error more clear, and allows multiline input (copy-pasted text with newline)
// 2.0.7: creates log file so one can check score even if one forgot to write it down...
// 2.0.6. Extra feature: now properly inserts spacing around ,
// 2.0.5: Extra feature: three-sided cards. ALSO: better font
// 2.0.4. Bugfix: should accumulate percentages over one run, not reset score without a deckswap or quit
// 2.0.3. QoL improvement: show current score of reviewing process
// 2.0.2. QoL-improvement: show percentage of cards successfully remembered
// 2.0.1. Bugfix: was able to add cards with the same front

/**
 * Runs Eb.
 * @author Eric-Wubbo Lameijer
 */

object Eb {
    const val version = 264
    val VERSION_STRING = versionToString()

    private fun versionToString(): Any {
        val major = version / 100
        val minPlusSub= version - major * 100
        val minor = minPlusSub / 10
        val sub = minPlusSub - minor * 10
        return "$major.$minor.$sub"
    }

    const val EB_STATUS_FILE = "eb_status.txt"
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
