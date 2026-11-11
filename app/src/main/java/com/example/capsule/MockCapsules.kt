package com.example.capsule

import kotlin.math.hypot

/**
 * Fake data for the Explore Map UI. Nothing here is real yet:
 * no backend, no GPS. Replace with real data later.
 */
object MockCapsules {

    /** Where the "you are here" dot sits on the mock map (0..1). */
    const val USER_X = 0.46f
    const val USER_Y = 0.54f

    /** You have to be this close to a capsule to open it. */
    const val OPEN_RADIUS_MILES = 0.1

    /** How many miles the map's width represents, and its rough height:width ratio. */
    private const val MAP_WIDTH_MILES = 0.8
    private const val MAP_ASPECT = 1.3

    private const val HOUR = 60L * 60L * 1000L
    private const val DAY = 24L * HOUR

    private val now = System.currentTimeMillis()

    val all: List<Capsule> = listOf(
        Capsule(
            id = "c1",
            title = "Note to Future Freshmen",
            author = "Alex Carter",
            message = "Don't skip the 8am. Trust me.",
            x = 0.24f, y = 0.30f,
            unlockAtMillis = now - 20 * DAY,
        ),
        Capsule(
            id = "c2",
            title = "Finals Week Survival Kit",
            author = "Jordan Lee",
            message = "Coffee, snacks, and a playlist that got me through.",
            x = 0.70f, y = 0.13f,
            unlockAtMillis = now + 3 * DAY + 5 * HOUR,
            isSaved = true,
        ),
        Capsule(
            id = "c3",
            title = "First Snow on the Quad",
            author = "Priya Shah",
            message = "Everyone stopped walking to class just to look up.",
            x = 0.53f, y = 0.58f,
            unlockAtMillis = now - 2 * DAY,
        ),
        Capsule(
            id = "c4",
            title = "Our Band's First Gig",
            author = "Marcus Bell",
            message = "We played to six people and it was the best night ever.",
            x = 0.90f, y = 0.52f,
            unlockAtMillis = now + 45 * DAY,
        ),
        Capsule(
            id = "c5",
            title = "Class of 2027 Hopes",
            author = "Sam Rivera",
            message = "Open this at graduation. Did we make it?",
            x = 0.20f, y = 0.80f,
            unlockAtMillis = now + 200 * DAY,
            isSaved = true,
        ),
        Capsule(
            id = "c6",
            title = "Coffee Shop Confessions",
            author = "Taylor Nguyen",
            message = "This is where I wrote my first app.",
            x = 0.70f, y = 0.80f,
            unlockAtMillis = now - 3 * DAY,
        ),
        Capsule(
            id = "c7",
            title = "Library Basement Secret",
            author = "Chris Park",
            message = "Third shelf from the left. You'll see.",
            x = 0.37f, y = 0.49f,
            unlockAtMillis = now + 18 * HOUR,
        ),
    )

    /** Pretend distance from the user's dot to a capsule, in miles. */
    fun distanceMiles(capsule: Capsule): Double {
        val dx = (capsule.x - USER_X).toDouble()
        val dy = ((capsule.y - USER_Y) * MAP_ASPECT).toDouble()
        return hypot(dx, dy) * MAP_WIDTH_MILES
    }

    fun isNearby(capsule: Capsule): Boolean = distanceMiles(capsule) <= OPEN_RADIUS_MILES
}
