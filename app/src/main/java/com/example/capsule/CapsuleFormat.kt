package com.example.capsule

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Text helpers for showing capsule info. */
object CapsuleFormat {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("MMM d 'at' h:mm a", Locale.US)

    fun distance(miles: Double): String =
        if (miles < 0.1) "${(miles * 5280).roundToInt()} ft away"
        else String.format(Locale.US, "%.1f mi away", miles)

    fun date(millis: Long): String = dateFormat.format(Date(millis))

    fun dateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    /** "Unlocked Oct 2, 2026" or "Unlocks in 3d 5h". */
    fun unlockStatus(capsule: Capsule, now: Long = System.currentTimeMillis()): String =
        if (capsule.isUnlocked(now)) "Unlocked ${date(capsule.unlockAtMillis)}"
        else "Unlocks in ${timeRemaining(capsule.unlockAtMillis - now)}"

    /** Short countdown like "3d 5h", "18h 12m" or "45m". */
    fun timeRemaining(millis: Long): String {
        val totalMinutes = (millis / 60_000L).coerceAtLeast(0L)
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
