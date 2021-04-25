package eb.subwindow.studyoptions.settinggroups

import eb.subwindow.studyoptions.StudyOptions
import eb.utilities.Utilities
import java.io.Serializable
import java.util.*

// the default maximum number of cards to be reviewed in a single reviewing session
private const val defaultReviewSessionSize = 20

// the default 'aimed for' percentage correct (if real reviews are above or below the average, reviewing
// times are adjusted)
private const val defaultSuccessTarget = 85.0

class OtherSettings(
    var reviewSessionSize: Int? = defaultReviewSessionSize, // if not specified, review all/infinite cards
    var idealSuccessPercentage: Double = defaultSuccessTarget
) : Serializable {
    override fun equals(other: Any?) = when {
        this === other -> true
        other == null -> false
        javaClass != other.javaClass -> false
        else -> {
            val otherOptions = other as OtherSettings
            reviewSessionSize == otherOptions.reviewSessionSize &&
                    Utilities.doublesEqualWithinThousands(
                        idealSuccessPercentage,
                        otherOptions.idealSuccessPercentage
                    )
        }
    }

    override fun hashCode() =
        Objects.hash(
            reviewSessionSize,
            idealSuccessPercentage
        )

    companion object {
        private const val serialVersionUID = -1
    }
}
