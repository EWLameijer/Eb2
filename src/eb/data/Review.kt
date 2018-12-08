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
class Review
/**
 * Constructor for Review objects.
 *
 * @param thinkingTime
 * the time the user needed to come up with his or her answer
 *
 * @param wasSuccess
 * whether the user knew the answer (true) or didn't (false)
 */
(private val m_thinkingTime: Duration, private val m_success: Boolean) : Serializable {
    /**
     * Returns the instant of the review
     *
     * @return the instant (the point in time) that the review was performed.
     */
    // preconditions: none. Instant exists if review exists
    // postconditions: none. Simple return of Instant
    val instant: Instant

    val thinkingTime = Utilities.durationToSeconds(m_thinkingTime)

    init {
        instant = Instant.now()
    }

    /**
     * Returns whether the review was a success.
     *
     * @return true if the review was a success, false if it wasn't.
     */
    fun wasSuccess(): Boolean {
        // preconditions: none. Review exists
        return m_success
        // postconditions: none. Simple return of boolean
    }

    override fun toString(): String {
        return "($instant,$m_success)"
    }

    companion object {
        private const val serialVersionUID = -3475131013697503513L
    }
}
