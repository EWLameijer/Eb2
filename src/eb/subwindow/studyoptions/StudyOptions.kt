package eb.subwindow.studyoptions

import eb.data.Review
import eb.subwindow.studyoptions.settinggroups.IntervalSettings
import eb.subwindow.studyoptions.settinggroups.TimerSettings
import eb.subwindow.studyoptions.settinggroups.OtherSettings

import java.io.Serializable
import java.util.Objects

import eb.utilities.TimeInterval
import eb.utilities.TimeUnit
import eb.utilities.Utilities
import java.time.Duration

/**
 * The StudyOptions class can store the learning settings that we want to use
 * for a particular deck. However, in some cases a StudyOptions object can exist
 * outside any particular deck (for example Eb's default options).
 *
 * @author Eric-Wubbo Lameijer
 */
class StudyOptions(
    var intervalSettings: IntervalSettings = IntervalSettings(),
    var timerSettings: TimerSettings = TimerSettings(),
    var otherSettings: OtherSettings = OtherSettings()
) : Serializable {

    override fun equals(other: Any?) = when {
        this === other -> true
        other == null -> false
        javaClass != other.javaClass -> false
        else -> {
            val otherOptions = other as StudyOptions
            intervalSettings == otherOptions.intervalSettings &&
                    timerSettings == otherOptions.timerSettings &&
                    otherSettings == otherOptions.otherSettings

        }
    }

    override fun hashCode() = Objects.hash(intervalSettings, timerSettings, otherSettings)

    companion object {
        // The serialization ID. Automatically generated, can be ignored.
        private const val serialVersionUID = -5967297039338080285L
    }
}
