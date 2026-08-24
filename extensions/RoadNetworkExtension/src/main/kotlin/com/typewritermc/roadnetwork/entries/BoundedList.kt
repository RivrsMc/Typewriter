package com.typewritermc.roadnetwork.entries

internal fun <T> MutableList<T>.addBounded(element: T, maximum: Int, dispose: (T) -> Unit) {
    require(maximum > 0) { "The maximum number of active path streams must be positive" }
    while (size >= maximum) {
        dispose(removeAt(0))
    }
    add(element)
}
