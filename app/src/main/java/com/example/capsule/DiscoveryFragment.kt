package com.example.capsule

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

class DiscoveryFragment : Fragment() {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var generation = 0
    private var cancellation: CancellationSignal? = null
    private var timeout: Runnable? = null
    private lateinit var store: DiscoveryStore
    private var capsules = mutableListOf<DiscoveryCapsule>()
    private var candidates = emptyList<DiscoveryCapsule>()
    private var location: Location? = null
    private var busy = false
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private lateinit var searchButton: Button
    private lateinit var refreshButton: Button
    private var afterLocation: (() -> Unit)? = null
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (has(Manifest.permission.ACCESS_COARSE_LOCATION) || has(Manifest.permission.ACCESS_FINE_LOCATION)) locate()
        else { setBusy(false); status.text = "Location permission was denied. Tap Find nearby photos to try again." }
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        store = DiscoveryStore(requireContext())
        capsules = store.load()
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
        }
        root.addView(TextView(requireContext()).apply { text = "Commons discovery"; textSize = 25f })
        root.addView(TextView(requireContext()).apply {
            text = "Photos within 2 km • Select a photo to create a capsule. Demo capsules unlock after 1 minute and within 161 m. Locations are contributor-supplied; check the source before visiting."
        })
        status = TextView(requireContext()).apply { text = "Saved capsules: ${capsules.size}. Find photos to get started."; setPadding(0, 12, 0, 12) }
        root.addView(status)
        searchButton = Button(requireContext()).apply {
            text = "Find nearby photos"
            setOnClickListener { afterLocation = { search() }; requestLocation() }
        }
        root.addView(searchButton)
        refreshButton = Button(requireContext()).apply {
            text = "Refresh location / unlock status"
            setOnClickListener { afterLocation = { render() }; requestLocation() }
        }
        root.addView(refreshButton)
        list = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(requireContext()).apply { addView(list) },
            LinearLayout.LayoutParams(-1, 0, 1f))
        return root
    }
    override fun onViewCreated(view: View, state: Bundle?) { render() }
    private fun has(permission: String) = ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    private fun setBusy(value: Boolean) {
        busy = value
        searchButton.isEnabled = !value
        refreshButton.isEnabled = !value
    }
    private fun requestLocation() {
        setBusy(true)
        if (!has(Manifest.permission.ACCESS_COARSE_LOCATION) && !has(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else locate()
    }
    private fun locate() {
        val manager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            has(Manifest.permission.ACCESS_FINE_LOCATION) && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            setBusy(false); status.text = "Enable device location services, then retry."; return
        }
        status.text = "Getting your current location…"
        cancellation?.cancel()
        val signal = CancellationSignal()
        cancellation = signal
        val token = generation
        timeout = Runnable {
            signal.cancel()
            if (token == generation && view != null) { setBusy(false); status.text = "Location timed out. Move outdoors or set an emulator location, then retry." }
        }.also { main.postDelayed(it, 25000) }
        try {
            LocationManagerCompat.getCurrentLocation(manager, provider, signal,
                ContextCompat.getMainExecutor(requireContext())) { fix ->
                timeout?.let { main.removeCallbacks(it) }
                if (token != generation || view == null) return@getCurrentLocation
                setBusy(false)
                if (fix == null) { status.text = "No current location available. Please retry."; return@getCurrentLocation }
                location = fix
                status.text = "Location found (accuracy ±${fix.accuracy.toInt()} m)."
                afterLocation?.invoke()
            }
        } catch (error: SecurityException) {
            timeout?.let { main.removeCallbacks(it) }
            setBusy(false); status.text = "Location permission changed. Please retry."
        }
    }
    private fun search() {
        val fix = location ?: return
        setBusy(true)
        status.text = "Searching Commons and checking photo credits…"
        val token = generation
        worker.execute {
            val result = runCatching { CommonsClient.search(fix.latitude, fix.longitude) }
            main.post {
                if (token != generation || view == null) return@post
                setBusy(false)
                result.onSuccess {
                    candidates = it
                    status.text = if (it.isEmpty()) "No qualifying photos within 2 km. Try another area."
                        else "${it.size} photos with supported licenses. Review a source, then select a photo."
                    render()
                }.onFailure { status.text = "Search failed: ${it.message}. Tap Find nearby photos to retry." }
            }
        }
    }
    private fun text(value: String, size: Float = 16f) = TextView(requireContext()).apply { text = value; textSize = size }
    private fun plain(value: String) = HtmlCompat.fromHtml(value, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    private fun link(label: String, url: String, parent: LinearLayout) {
        parent.addView(Button(requireContext()).apply {
            text = label
            setOnClickListener {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    .onFailure { status.text = "No app is available to open this link." }
            }
        })
    }
    private fun distance(c: DiscoveryCapsule): Float? {
        val fix = location ?: return null
        return FloatArray(1).also { Location.distanceBetween(fix.latitude, fix.longitude, c.lat, c.lon, it) }[0]
    }
    private fun canOpen(c: DiscoveryCapsule): Boolean {
        val fix = location ?: return false
        val age = (android.os.SystemClock.elapsedRealtimeNanos() - fix.elapsedRealtimeNanos) / 1_000_000
        return age in 0..60000 && fix.hasAccuracy() && fix.accuracy <= 75 &&
            (distance(c) ?: Float.MAX_VALUE) + fix.accuracy <= 161 && System.currentTimeMillis() >= c.unlockAt
    }
    private fun render() {
        list.removeAllViews()
        list.addView(text("Your discovery capsules (${capsules.size})", 20f))
        capsules.forEach { card(it, true) }
        list.addView(text("Nearby photo candidates", 20f))
        candidates.filter { candidate -> capsules.none { it.id == candidate.id } }.forEach { card(it, false) }
    }
    private fun card(c: DiscoveryCapsule, saved: Boolean) {
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 24, 8, 24)
        }
        list.addView(box)
        box.addView(text(c.title, 19f))
        box.addView(text("Source coordinates: ${c.lat}, ${c.lon}" +
            (distance(c)?.let { " • ${it.toInt()} m away" } ?: "")))
        if (!saved || c.openedAt > 0) {
            val preview = ImageView(requireContext()).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = c.title
            }
            box.addView(preview, LinearLayout.LayoutParams(-1, 420))
            val token = generation
            worker.execute {
                val bitmap = runCatching {
                    val connection = CommonsClient.connection(c.image)
                    try {
                        if (connection.responseCode != 200) error("Image unavailable")
                        connection.inputStream.use { BitmapFactory.decodeStream(it) }
                    } finally { connection.disconnect() }
                }.getOrNull()
                main.post {
                    if (token == generation && view != null && preview.isAttachedToWindow) {
                        if (bitmap != null) preview.setImageBitmap(bitmap)
                        else preview.contentDescription = "Photo unavailable. Use View source."
                    }
                }
            }
        }
        box.addView(text("Photo: ${plain(c.artist)}\n${plain(c.credit)}\n${c.license}"))
        link("View source", c.source, box)
        link("View license", c.licenseUrl, box)
        box.addView(Button(requireContext()).apply {
            text = when {
                !saved -> "Create discovery capsule"
                c.openedAt > 0 -> "Opened • saved in history"
                else -> "Open capsule"
            }
            isEnabled = !saved || c.openedAt == 0L
            setOnClickListener {
                if (!saved) {
                    capsules.add(c.copy(unlockAt = System.currentTimeMillis() + 60000))
                    store.save(capsules)
                    status.text = "Capsule created. It unlocks in one minute; refresh location when ready."
                    render()
                } else if (canOpen(c)) {
                    c.openedAt = System.currentTimeMillis()
                    store.save(capsules)
                    status.text = "Capsule opened and saved to your local history."
                    render()
                } else {
                    status.text = "Wait for the unlock time and refresh your location. Opening needs a fresh, accurate fix within 161 m (including accuracy margin)."
                }
            }
        })
        if (saved) box.addView(text("Unlocks: ${DateFormat.getDateTimeInstance().format(Date(c.unlockAt))}"))
    }
    override fun onDestroyView() {
        generation++
        cancellation?.cancel()
        timeout?.let { main.removeCallbacks(it) }
        super.onDestroyView()
    }
    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}
