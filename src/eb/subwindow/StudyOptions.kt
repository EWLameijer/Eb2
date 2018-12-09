package eb.subwindow

import java.io.Serializable
import java.util.Objects

import eb.utilities.TimeInterval
import eb.utilities.TimeUnit
import eb.utilities.Utilities

/**
 * The StudyOptions class can store the learning settings that we want to use
 * for a particular deck. However, in some cases a StudyOptions object can exist
 * outside any particular deck (for example Eb's default options).
 *
 * @author Eric-Wubbo Lameijer
 */
class StudyOptions(
        var initialInterval: TimeInterval = DEFAULT_INITIAL_INTERVAL,
        var reviewSessionSize: Int? = DEFAULT_REVIEW_SESSION_SIZE,
        var rememberedInterval: TimeInterval = DEFAULT_REMEMBERED_INTERVAL,
        var forgottenInterval: TimeInterval = DEFAULT_FORGOTTEN_INTERVAL,
        var lengtheningFactor: Double = DEFAULT_LENGTHENING_FACTOR) : Serializable {

    // The interval between creation of a card and when it should first be
    // reviewed. Can be set by the user.
    /**
     * Returns the interval that a newly created card has to wait before it should
     * be reviewed (after all, reviewing a card 2 seconds after making it probably
     * won't teach you much, as it is still too fresh in your memory).
     *
     * @return the interval a newly created card has to wait before it can be
     * reviewed for the first time
     */
    // preconditions: none. The constructor and setters should have taken care
    // that the interval is valid.
    // postconditions: none. This is a simple getter method that should not
    // change anything.

    override fun equals(other: Any?) = when {
        this === other -> true
        other == null -> false
        javaClass != other.javaClass -> false
        else -> {
            val otherOptions = other as StudyOptions?
            (initialInterval == otherOptions!!.initialInterval &&
                    reviewSessionSize == otherOptions.reviewSessionSize &&
                    rememberedInterval == otherOptions.rememberedInterval &&
                    Utilities.doublesEqualWithinThousands(lengtheningFactor, otherOptions.lengtheningFactor) &&
                    forgottenInterval == otherOptions.forgottenInterval)
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(initialInterval, reviewSessionSize, rememberedInterval, forgottenInterval)
    }

    companion object {

        // The serialization ID. Automatically generated, can be ignored.
        private const val serialVersionUID = -5967297039338080285L

        // The default initial interval.
        private val DEFAULT_INITIAL_INTERVAL = TimeInterval(10.0, TimeUnit.MINUTE)
        private val DEFAULT_REMEMBERED_INTERVAL = TimeInterval(1.0, TimeUnit.DAY)
        private val DEFAULT_FORGOTTEN_INTERVAL = TimeInterval(1.0, TimeUnit.HOUR)

        // the default number of cards to be reviewed in a single reviewing session
        private const val DEFAULT_REVIEW_SESSION_SIZE = 20
        private const val DEFAULT_LENGTHENING_FACTOR = 5.0
    }
}
