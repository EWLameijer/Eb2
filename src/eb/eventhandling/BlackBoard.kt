package eb.eventhandling

import java.util.ArrayList
import java.util.HashMap

import eb.utilities.Utilities

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
    internal var c_listeners: MutableMap<UpdateType, ArrayList<Listener>> = HashMap()

    fun post(update: Update) {
        val listeners = c_listeners[update.type]
        if (listeners != null) {
            for (index in listeners.indices) {
                listeners[index].respondToUpdate(update)
            }
        }
    }

    fun register(listener: Listener, updateType: UpdateType) {
        if (!c_listeners.containsKey(updateType)) {
            c_listeners[updateType] = ArrayList()
        }
        c_listeners[updateType]!!.add(listener)
    }

    fun unRegister(listener: Listener, updateType: UpdateType) {
        c_listeners[updateType]!!.remove(listener)
    }

    fun unRegister(listener: Listener) {
        for (key in c_listeners.keys) {
            c_listeners[key]!!.remove(listener)
        }

    }
}
