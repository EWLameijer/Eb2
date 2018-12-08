package eb.eventhandling

@FunctionalInterface
interface Listener {
    fun respondToUpdate(update: Update)

}
