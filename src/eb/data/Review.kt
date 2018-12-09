package eb.data

import java.io.Serializable
import java.time.Duration
import java.time.Instant

import eb.utilities.Utilities

/**
 * A Review object stores relevant data about a review, like when it occurred,
 * how long it took, and of course the result. In future, it may also store data
 * like 'review type'.
 *
 * @author Eric-Wubbo Lameijer
 */
class Review(thinkingTime: Duration, val wasSuccess: Boolean) : Serializable {
    val instant : Instant = Instant.now()

    val thinkingTime = Utilities.durationToSeconds(thinkingTime)

    override fun toString() = "($instant, $wasSuccess)"

    companion object {
        private const val serialVersionUID = -3475131013697503513L
    }
}
