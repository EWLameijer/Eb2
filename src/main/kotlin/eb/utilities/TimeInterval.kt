package eb.utilities

import java.io.Serializable
import java.util.Objects


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

    override fun equals(other: Any?) = when {
        this === other -> true
        other == null -> false
        javaClass != other.javaClass ->false
        else -> {
            val otherInterval = other as TimeInterval?
            Utilities.doublesEqualWithinThousands(scalar, otherInterval!!.scalar) && unit == otherInterval.unit
        }
    }

    override fun hashCode() = Objects.hash(scalar, unit)

    fun asDuration() = Utilities.multiplyDurationBy(unit.duration, scalar)

    companion object {
        private const val serialVersionUID = 4957903341568456588L
    }
}
