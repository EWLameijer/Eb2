package eb.subwindow.studyoptions.settinggroups

import java.io.Serializable

private const val defaultTotalTimingMode = false

data class TimerSettings(var totalTimingMode: Boolean = defaultTotalTimingMode) : Serializable

