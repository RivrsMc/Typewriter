package com.typewritermc.core.utils.point

data class World(
    val name: String,
) {
    companion object {
        val Empty = World("")
    }
}