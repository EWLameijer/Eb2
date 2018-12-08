package eb.subwindow

import eb.utilities.Utilities
import java.lang.RuntimeException

val NORMAL_IDENTIFIER = "normal"
val TIMED_IDENTIFIER = "timed"


enum class TimedModus constructor(val modusName: String) {

    TRUE(TIMED_IDENTIFIER), FALSE(NORMAL_IDENTIFIER);


    companion object {

        fun stringToTimedModus(value: String): TimedModus? {
            if (value == TimedModusHelper.TIMED_IDENTIFIER) {
                return TimedModus.TRUE
            } else if (value == TimedModusHelper.NORMAL_IDENTIFIER) {
                return TimedModus.FALSE
            } else {
                throw RuntimeException("TimedModus.stringToTimedModus() error: I don't recognize modus'$value'")
            }
        }
    }

}
