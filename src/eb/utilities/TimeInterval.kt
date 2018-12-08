package eb.utilities

import java.io.Serializable
import java.time.Duration
import java.util.Objects

import org.checkerframework.checker.nullness.qual.EnsuresNonNull

/**
 * The TimeInterval class stores a time interval, for example "3.5 hours". It
 * stores the scalar ("3.5") and the unit ("hours") separately. (note that
 * "3.5 hours" is a quantity, "hours" is the unit. Not sure how 3.5 would be
 * called here, calling it a scalar is the best I can think of right now).
 *
 * @author Eric-Wubbo Lameijer
 */
class TimeInterval(var scalar: Double = 0.0, var unit: TimeUnit) : Serializable {
    init {
        require(scalar >= 0) { "TimeInterval.setTo() error: negative time intervals are not permitted."}
    }

    /**
     * Copy constructor, useful for initializing a new TimeInterval object with an
     * old one, without being dependent of the original object staying the same.
     *
     * @param intervalToBeCopied
     * the interval of which the values must be copied to the new
     * instance of TimeInterval
     */
    constructor(intervalToBeCopied: TimeInterval) : this (intervalToBeCopied.scalar, intervalToBeCopied.unit)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        } else if (other == null) {
            return false
        } else if (javaClass != other.javaClass) {
            return false
        } else {
            val otherInterval = other as TimeInterval?
            return Utilities.doublesEqualWithinThousands(scalar,
                    otherInterval!!.scalar) && unit == otherInterval.unit
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(scalar, unit)
    }

    fun asDuration(): Duration {
        val unitDuration = unit.duration
        return Utilities.multiplyDurationBy(unitDuration, scalar)
    }

    companion object {
        private val serialVersionUID = 4957903341568456588L;
    }
}
