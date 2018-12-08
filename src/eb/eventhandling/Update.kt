package eb.eventhandling

import eb.utilities.Utilities

/**
 * Produces an update; the exact value of the update is given with the String
 * "contents". Note that contents can (so far) only be provided for program
 * state (Main window state) updates.
 *
 * @param updateType
 * @param contents
 */
class Update (val type: UpdateType, val contents: String = ""){
    init {
        require(type != UpdateType.PROGRAMSTATE_CHANGED || contents.isNotEmpty()) {
               "Update constructor error: must give second parameter when the program state changes."}
    }
}
