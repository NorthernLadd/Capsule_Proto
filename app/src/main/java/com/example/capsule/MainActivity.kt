package com.example.capsule

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.example.capsule.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        if (savedInstanceState == null) {
            showFragment(DiscoveryFragment())
        }
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showFragment(DiscoveryFragment())
                R.id.nav_profile -> showFragment(ProfileFragment())
                R.id.nav_map -> showFragment(DiscoveryFragment())
                R.id.nav_create -> showFragment(CreateFragment())
                else -> return@setOnItemSelectedListener false
            }
            true
        }
        binding.bottomNav.setOnItemReselectedListener { /* Keep the current page. */ }
    }

    private fun showFragment(fragment: Fragment) {
        val tag = fragment.javaClass.name
        val destination = supportFragmentManager.findFragmentByTag(tag) ?: fragment
        val transaction = supportFragmentManager.beginTransaction()
        supportFragmentManager.fragments.filter { it != destination }.forEach {
            transaction.hide(it).setMaxLifecycle(it, Lifecycle.State.STARTED)
        }
        if (destination.isAdded) {
            transaction.show(destination)
        } else {
            transaction.add(R.id.fragmentContainer, destination, tag)
        }
        transaction.setMaxLifecycle(destination, Lifecycle.State.RESUMED).commitNow()
    }
}
