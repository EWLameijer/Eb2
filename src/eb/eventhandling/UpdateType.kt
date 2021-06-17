package eb.eventhandling

enum class UpdateType {
    DECK_CHANGED, // card added/removed
    DECK_SWAPPED, // other deck loaded
    INPUTFIELD_CHANGED, // input field in one of the options windows changed contents
    PROGRAMSTATE_CHANGED // changing program state (from reviewing to summarizing, for example)
}
