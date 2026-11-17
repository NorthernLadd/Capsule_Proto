package com.example.capsule

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommonsPrototypeTest {
    @Test fun liveCommonsSearchCreatesPersistedCapsuleWithoutResettingUnlock() {
        val results = CommonsClient.search(40.7829, -73.9654)
        assertTrue("Expected qualifying Commons photos near Central Park", results.isNotEmpty())
        val selected = results.first()
        assertTrue(selected.image.startsWith("https://"))
        assertTrue(selected.artist.isNotBlank())
        assertTrue(selected.licenseUrl.startsWith("https://creativecommons.org/"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = DiscoveryStore(context)
        val original = store.load()
        try {
            val opened = selected.copy(openedAt = 123456L)
            store.save(listOf(opened))
            val restored = DiscoveryStore(context).load().single()
            assertEquals(opened, restored)
            assertEquals(selected.unlockAt, restored.unlockAt)
            assertEquals(123456L, restored.openedAt)
        } finally { store.save(original) }
    }
}
