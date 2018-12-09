package eb.eventhandling

import java.util.ArrayList

/**
 * The BlackBoard class serves as a kind of blackboard for central
 * communication. Events are posted to the blackboards with a message string,
 * and the blackboard arranges that the listeners interested in that particular
 * kind of update are notified.
 *
 * @author Eric-Wubbo Lameijer
 */
object BlackBoard {

    // note that the more logical Map<UpdateType, HashSet<Listener> gives problems
    // if you iterate over the set, as calling respondToUpdate may modify the set
    private var listeners: Map<UpdateType, ArrayList<Listener>> =
            UpdateType.values().associate { it to ArrayList<Listener>() }

    fun post(update: Update) {
        listeners[update.type]?.forEach { it.respondToUpdate(update) }
    }

    fun register(listener: Listener, updateType: UpdateType) = listeners[updateType]!!.add(listener)
}
