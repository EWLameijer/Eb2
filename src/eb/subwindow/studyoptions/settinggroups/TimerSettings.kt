package eb.subwindow.studyoptions.settinggroups

import eb.utilities.TimeInterval
import eb.utilities.TimeUnit
import eb.utilities.Utilities
import java.io.Serializable
import java.util.*

private const val defaultTotalTimingMode = false
private const val defaultLimitReviewTime = false
private val defaultFrontStudyTimeLimit = TimeInterval(5.0, TimeUnit.SECOND)
private val defaultWholeStudyTimeLimit = TimeInterval(5.0, TimeUnit.SECOND)

class TimerSettings(
    var totalTimingMode: Boolean = defaultTotalTimingMode,
    var limitReviewTime: Boolean = defaultLimitReviewTime,
    var frontStudyTimeLimit: TimeInterval = defaultFrontStudyTimeLimit,
    var wholeStudyTimeLimit: TimeInterval = defaultWholeStudyTimeLimit
) : Serializable {
    override fun equals(other: Any?) = when {
        this === other -> true
        other == null -> false
        javaClass != other.javaClass -> false
        else -> {
            val otherSettings = other as TimerSettings
            totalTimingMode == otherSettings.totalTimingMode &&
                    limitReviewTime == otherSettings.limitReviewTime &&
                    frontStudyTimeLimit == otherSettings.frontStudyTimeLimit &&
                    wholeStudyTimeLimit == otherSettings.wholeStudyTimeLimit
        }
    }

    override fun hashCode() =
        Objects.hash(
            totalTimingMode,
            limitReviewTime,
            frontStudyTimeLimit,
            wholeStudyTimeLimit
        )
}

