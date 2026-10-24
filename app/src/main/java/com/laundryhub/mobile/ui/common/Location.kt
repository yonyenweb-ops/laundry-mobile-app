package com.laundryhub.mobile.ui.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** Last known GPS/network position, or null. */
@SuppressLint("MissingPermission")
fun lastKnownLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { lm.isProviderEnabled(it) }
        .mapNotNull { lm.getLastKnownLocation(it) }
        .maxByOrNull { it.time }
}

/** Asks for a fresh position (GPS first, then network); falls back to the last known one. */
@SuppressLint("MissingPermission")
suspend fun currentLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .firstOrNull { lm.isProviderEnabled(it) } ?: return lastKnownLocation(context)
    val fresh = withTimeoutOrNull(10_000L) {
        suspendCancellableCoroutine<Location?> { cont ->
            val cancel = CancellationSignal()
            cont.invokeOnCancellation { cancel.cancel() }
            LocationManagerCompat.getCurrentLocation(lm, provider, cancel, ContextCompat.getMainExecutor(context)) {
                if (cont.isActive) cont.resume(it)
            }
        }
    }
    return fresh ?: lastKnownLocation(context)
}

/** A Google "Plus Code" such as "HV8M+487" - not a readable address. */
private val PLUS_CODE = Regex("""^[23456789CFGHJMPQRVWX]{4,8}\+[23456789CFGHJMPQRVWX]{2,3}(,\s*)?""")

/** Street address for a GPS point (Geocoder), or the coordinates when no address is found. */
@Suppress("DEPRECATION")
fun describeLocation(context: Context, lat: Double, lng: Double): String {
    val line = runCatching {
        if (!Geocoder.isPresent()) return@runCatching null
        val a = Geocoder(context).getFromLocation(lat, lng, 1)?.firstOrNull() ?: return@runCatching null
        val full = a.getAddressLine(0) ?: return@runCatching null
        if (!PLUS_CODE.containsMatchIn(full)) return@runCatching full
        // No street name here: use the area names instead of the Plus Code, e.g. "Tuek Thla, Sen Sok, Phnom Penh"
        listOfNotNull(a.subLocality, a.subAdminArea, a.locality ?: a.adminArea)
            .filter { it.isNotBlank() }.distinct()
            .joinToString(", ")
            .ifBlank { full.replace(PLUS_CODE, "") }
    }.getOrNull()
    return line?.takeIf { it.isNotBlank() } ?: "My location (%.5f, %.5f)".format(lat, lng)
}

/** A named point on the map. */
data class Place(val name: String, val lat: Double, val lng: Double)

/** Searches a place by name (Geocoder), biased to Cambodia. Null when nothing is found. */
@Suppress("DEPRECATION")
fun findPlace(context: Context, query: String): Place? = runCatching {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context)
    val hit = geocoder.getFromLocationName("$query, Phnom Penh, Cambodia", 1)?.firstOrNull()
        ?: geocoder.getFromLocationName(query, 1)?.firstOrNull()
    hit?.let { Place(it.getAddressLine(0) ?: query, it.latitude, it.longitude) }
}.getOrNull()

/**
 * Starts live location updates (driver app, FR-08). Returns a function that stops them.
 * In the emulator: "..." (Extended controls) > Location to set or move the position.
 */
@SuppressLint("MissingPermission")
fun startLocationUpdates(context: Context, onLocation: (Double, Double) -> Unit): () -> Unit {
    if (!hasLocationPermission(context)) return {}
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val listener = LocationListener { loc -> onLocation(loc.latitude, loc.longitude) }
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { lm.isProviderEnabled(it) }
    for (p in providers) {
        lm.requestLocationUpdates(p, 5_000L, 5f, listener, Looper.getMainLooper())
    }
    lastKnownLocation(context)?.let { onLocation(it.latitude, it.longitude) }
    return { lm.removeUpdates(listener) }
}

/**
 * "Use my current location" button: asks permission if needed, then gives lat/lng.
 * [onAddress] (optional) also gets the street address, looked up in the background.
 */
@Composable
fun UseMyLocationButton(
    onLocation: (Double, Double) -> Unit,
    onFail: (String) -> Unit,
    onAddress: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun useLastLocation() {
        scope.launch {
            val loc = currentLocation(context)
            if (loc == null) {
                onFail("No GPS position yet. Try again in a moment.")
                return@launch
            }
            onLocation(loc.latitude, loc.longitude)
            if (onAddress != null) {
                onAddress(withContext(Dispatchers.IO) { describeLocation(context, loc.latitude, loc.longitude) })
            }
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) useLastLocation() else onFail("Location permission was denied")
    }
    OutlinedButton(onClick = {
        if (hasLocationPermission(context)) {
            useLastLocation()
        } else {
            launcher.launch(LOCATION_PERMISSIONS)
        }
    }) { Text("📍 Use my current location") }
}
