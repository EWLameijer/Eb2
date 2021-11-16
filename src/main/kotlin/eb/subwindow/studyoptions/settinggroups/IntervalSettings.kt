package eb.subwindow.studyoptions.settinggroups

import eb.data.Review
import eb.data.streakSize
import eb.data.TimeInterval
import eb.data.TimeUnit
import eb.utilities.Utilities
import java.io.Serializable
import java.time.Duration
import java.util.*
import kotlin.math.pow

private val defaultInitialInterval = TimeInterval(14.0, TimeUnit.HOUR)
private val defaultRememberedInterval = TimeInterval(3.0, TimeUnit.DAY)
private val defaultForgottenInterval = TimeInterval(14.0, TimeUnit.HOUR)
private const val defaultLengtheningFactor = 5.0

class IntervalSettings(
    // how long Eb waits after a card is created to do its first review
    var initialInterval: TimeInterval = defaultInitialInterval,
    var rememberedInterval: TimeInterval = defaultRememberedInterval,
    var forgottenInterval: TimeInterval = defaultForgottenInterval,
    var lengtheningFactor: Double = defaultLengtheningFactor
) : Serializable {
    override fun equals(other: Any?) = when {
        this === other -> true
        other == null -> false
        javaClass != other.javaClass -> false
        else -> {
            val otherOptions = other as IntervalSettings
            initialInterval == otherOptions.initialInterval &&
                    rememberedInterval == otherOptions.rememberedInterval &&
                    Utilities.doublesEqualWithinThousands(lengtheningFactor, otherOptions.lengtheningFactor) &&
                    forgottenInterval == otherOptions.forgottenInterval

        }
    }

    override fun hashCode() =
        Objects.hash(
            initialInterval,
            rememberedInterval,
            forgottenInterval,
            lengtheningFactor
        )


    fun calculateNextIntervalDuration(reviews: List<Review>): Duration =
        when (val lastReview = reviews.lastOrNull()) {
            null -> initialInterval.asDuration()
            else -> {
                if (lastReview.wasSuccess) getIntervalAfterSuccessfulReview(reviews)
                else forgottenInterval.asDuration()
            }
        }


    // Returns the time to wait for the next review (the previous review being a success).
    private fun getIntervalAfterSuccessfulReview(reviews: List<Review>): Duration {
        // the default wait time after a single successful review is given by the study options
        val waitTime = rememberedInterval.asDuration()

        // However, if previous reviews also have been successful, the wait time
        // should be longer (using exponential growth by default, though may want
        // to do something more sophisticated in the future).
        val streakLength = reviews.streakSize()
        val numberOfLengthenings = streakLength - 1 // 2 reviews = lengthen 1x.
        return Utilities.multiplyDurationBy(waitTime, lengtheningFactor.pow(numberOfLengthenings.toDouble()))
    }

    companion object {
        private const val serialVersionUID = -1
    }
}