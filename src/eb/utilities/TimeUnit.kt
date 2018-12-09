package eb.utilities

import java.time.Duration
import java.util.Vector

/**
 * TimeUnit provides the time units, for use in for example combo boxes so that
 * users can select time intervals for reviewing.
 *
 * Returns the name for the unit in a form that is suitable for the user
 * interface (so not MINUTE, which the .toString() would produce, but
 * "minute(s)".
 *
 * @author Eric-Wubbo Lameijer
 */
enum class TimeUnit ( val userInterfaceName: String, val duration: Duration)
{
    SECOND("second(s)", Duration.ofSeconds(1)),
    MINUTE("minute(s)", Duration.ofMinutes(1)),
    HOUR("hour(s)", Duration.ofHours(1)),
    DAY("day(s)", Duration.ofDays(1)),
    WEEK("week(s)", Duration.ofDays(7)),
    MONTH("month(s)", Duration.ofMinutes(43830)), // 365.25 days a year, divided by 12 months
    YEAR("year(s)", Duration.ofHours(8766));

    companion object {
        // Returns the names of all time units (like "week(s)") as a String array.
        //
        // Note: despite SonarLint's warnings, using Vector here because the client,
        // the DefaultComboBoxModel constructor, requires it.
        fun unitNames(): Vector<String> {
            val unitNames = Vector<String>()
            TimeUnit.values().forEach { unitNames.add(it.userInterfaceName) }
            return unitNames
        }

        // Converts the given string into the appropriate TimeUnit.
        //
        //@param unitAsString : the unit as a string (like "second(s)") that needs to be converted
        //to the proper unit, SECOND
        fun parseUnit(unitAsString: String): TimeUnit? =
                TimeUnit.values().find{ it.userInterfaceName == unitAsString }
    }
}