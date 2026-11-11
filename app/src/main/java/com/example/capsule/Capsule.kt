package com.example.capsule

/**
 * A time capsule placed on the map.
 *
 * [x] and [y] are positions on the mock map, from 0 to 1 (left→right, top→bottom).
 * When the real map is added, swap them for latitude / longitude.
 */
data class Capsule(
    val id: String,
    val title: String,
    val author: String,
    val message: String,
    val x: Float,
    val y: Float,
    val unlockAtMillis: Long,
    var isSaved: Boolean = false,
) {
    fun isUnlocked(now: Long = System.currentTimeMillis()): Boolean = now >= unlockAtMillis
}
