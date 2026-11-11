package com.example.capsule

import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.capsule.databinding.FragmentMapBinding

/**
 * Explore Map tab: search bar, All / Unlocked / Locked filter, the map, and a
 * preview card for the tapped capsule. Uses [MockCapsules] until the real data exists.
 */
class MapFragment : Fragment(R.layout.fragment_map) {

    private enum class Filter { ALL, UNLOCKED, LOCKED }

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val capsules = MockCapsules.all
    private var query = ""
    private var filter = Filter.ALL
    private var selected: Capsule? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapBinding.bind(view)

        binding.mapView.onCapsuleSelected = { showCapsule(it) }

        binding.searchInput.doAfterTextChanged {
            query = it?.toString().orEmpty().trim()
            applyFilters()
        }

        binding.filterChips.setOnCheckedStateChangeListener { _, ids ->
            filter = when (ids.firstOrNull()) {
                R.id.chipUnlocked -> Filter.UNLOCKED
                R.id.chipLocked -> Filter.LOCKED
                else -> Filter.ALL
            }
            applyFilters()
        }

        binding.closeButton.setOnClickListener { showCapsule(null) }

        binding.saveButton.setOnClickListener {
            selected?.let {
                it.isSaved = !it.isSaved
                bindSaveButton(it)
            }
        }

        binding.openButton.setOnClickListener {
            selected?.let {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.map_open_coming_soon, it.title),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        applyFilters()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyFilters() {
        val visible = capsules.filter { c ->
            val matchesQuery = query.isEmpty() ||
                c.title.contains(query, ignoreCase = true) ||
                c.author.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                Filter.ALL -> true
                Filter.UNLOCKED -> c.isUnlocked()
                Filter.LOCKED -> !c.isUnlocked()
            }
            matchesQuery && matchesFilter
        }

        binding.mapView.setCapsules(visible)
        binding.capsuleCount.text = when (visible.size) {
            0 -> getString(R.string.map_count_none)
            1 -> getString(R.string.map_count_one)
            else -> getString(R.string.map_count_many, visible.size)
        }

        val current = selected
        if (current != null && current !in visible) showCapsule(null)
    }

    private fun showCapsule(capsule: Capsule?) {
        selected = capsule
        binding.mapView.selectedId = capsule?.id
        TransitionManager.beginDelayedTransition(binding.mapContainer)

        if (capsule == null) {
            binding.capsuleCard.isVisible = false
            return
        }
        binding.capsuleCard.isVisible = true

        val now = System.currentTimeMillis()
        val unlocked = capsule.isUnlocked(now)
        val nearby = MockCapsules.isNearby(capsule)
        val context = requireContext()

        // Locked / Unlocked pill
        val pillBg = if (unlocked) R.color.status_unlocked_bg else R.color.status_locked_bg
        val pillFg = if (unlocked) R.color.status_unlocked_fg else R.color.status_locked_fg
        binding.statusPill.apply {
            setText(if (unlocked) R.string.map_status_unlocked else R.string.map_status_locked)
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (unlocked) R.drawable.ic_lock_open_small else R.drawable.ic_lock_small, 0, 0, 0,
            )
            backgroundTintList = ContextCompat.getColorStateList(context, pillBg)
            compoundDrawableTintList = ContextCompat.getColorStateList(context, pillFg)
            setTextColor(ContextCompat.getColor(context, pillFg))
        }

        binding.capsuleTitle.text = capsule.title
        binding.capsuleAuthor.text = getString(R.string.map_left_by, capsule.author)
        binding.capsuleDistance.text = CapsuleFormat.distance(MockCapsules.distanceMiles(capsule))
        binding.capsuleUnlock.text = CapsuleFormat.unlockStatus(capsule, now)

        // What the user still needs to do before they can open it
        binding.capsuleHint.text = when {
            unlocked && nearby -> getString(R.string.map_hint_ready)
            unlocked -> getString(R.string.map_hint_too_far)
            nearby -> getString(R.string.map_hint_locked_nearby, CapsuleFormat.dateTime(capsule.unlockAtMillis))
            else -> getString(R.string.map_hint_locked_far, CapsuleFormat.date(capsule.unlockAtMillis))
        }
        binding.openButton.isEnabled = unlocked && nearby
        binding.openButton.setText(
            when {
                unlocked && nearby -> R.string.map_action_open
                unlocked -> R.string.map_action_too_far
                else -> R.string.map_action_locked
            },
        )

        bindSaveButton(capsule)
    }

    private fun bindSaveButton(capsule: Capsule) {
        binding.saveButton.setText(if (capsule.isSaved) R.string.map_action_saved else R.string.map_action_save)
        binding.saveButton.setIconResource(
            if (capsule.isSaved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border,
        )
    }
}
