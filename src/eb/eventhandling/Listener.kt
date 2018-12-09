package eb.eventhandling

interface Listener {
    fun respondToUpdate(update: Update)
}
