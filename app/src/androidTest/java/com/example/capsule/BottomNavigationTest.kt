package com.example.capsule

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomNavigationTest {
    @Test
    fun tabsSwitchAndPreserveCreationDraft() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val tabs = listOf(
                R.id.nav_home to HomeFragment::class.java,
                R.id.nav_profile to ProfileFragment::class.java,
                R.id.nav_map to MapFragment::class.java,
                R.id.nav_create to CreateFragment::class.java
            )
            for ((id, fragmentClass) in tabs) {
                onView(withId(id)).perform(click())
                scenario.onActivity { activity ->
                    val visible = activity.supportFragmentManager.fragments.filter { !it.isHidden }
                    assertEquals(1, visible.size)
                    assertEquals(fragmentClass, visible.single().javaClass)
                }
            }
            onView(withId(R.id.nav_profile)).perform(click())
            onView(withId(R.id.userName)).check(matches(isDisplayed()))
            onView(withId(R.id.userName)).perform(replaceText("Test profile"), closeSoftKeyboard())
            onView(withId(R.id.nav_create)).perform(click())
            onView(withId(R.id.capsule_name)).perform(replaceText("Campus memory"), closeSoftKeyboard())
            onView(withId(R.id.nav_map)).perform(click())
            onView(withId(R.id.nav_create)).perform(click())
            onView(withId(R.id.capsule_name)).check(matches(withText("Campus memory")))
            scenario.recreate()
            onView(withId(R.id.capsule_name)).check(matches(withText("Campus memory")))
            scenario.onActivity { activity ->
                assertEquals(R.id.nav_create, activity.findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId)
                assertEquals(1, activity.supportFragmentManager.fragments.count { !it.isHidden })
            }
            onView(withId(R.id.nav_profile)).perform(click())
            onView(withId(R.id.userName)).check(matches(withText("Test profile")))
        }
    }
}
