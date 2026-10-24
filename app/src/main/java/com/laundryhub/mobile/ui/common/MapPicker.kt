package com.laundryhub.mobile.ui.common

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.laundryhub.mobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.camera.CameraPosition as MlCameraPosition
import org.maplibre.android.camera.CameraUpdateFactory as MlCameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MlLatLng
import org.maplibre.android.maps.MapView as MlMapView

/** Well-known places, so a location can be set quickly (also handy in class demos). */
val PHNOM_PENH_PLACES = listOf(
    Place("PNC (Sen Sok)", 11.5652, 104.8836),
    Place("Toul Kork", 11.5760, 104.8930),
    Place("BKK1", 11.5564, 104.9282),
    Place("Koh Pich (Diamond Island)", 11.5560, 104.9340),
    Place("Central Market", 11.5695, 104.9210),
    Place("Royal Palace", 11.5636, 104.9310),
    Place("Russian Market", 11.5470, 104.9170),
    Place("AEON Mall Sen Sok", 11.6025, 104.8900),
    Place("Chbar Ampov", 11.5420, 104.9450),
)

private val PHNOM_PENH_CENTER = Place("Phnom Penh", 11.5564, 104.9282)

/** Google Maps when a key is set in local.properties, otherwise free MapLibre + OpenFreeMap. */
private val USE_GOOGLE_MAPS = BuildConfig.MAPS_API_KEY.isNotBlank()

/** A point the map should fly to; a new instance each time, so the same place can be chosen twice. */
private class MapJump(val lat: Double, val lng: Double)

private fun metersBetween(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Float {
    val out = FloatArray(1)
    Location.distanceBetween(aLat, aLng, bLat, bLng, out)
    return out[0]
}

/**
 * Full-screen map to choose a location, like ride apps: drag the map (or tap it) under a fixed pin,
 * search a place by name, jump to a well-known place or to the phone's GPS, then save.
 */
@Composable
fun MapPickerDialog(title: String, start: Place?, onDismiss: () -> Unit, onPick: (Place) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val first = start ?: PHNOM_PENH_CENTER
    var centerLat by remember { mutableStateOf(first.lat) }
    var centerLng by remember { mutableStateOf(first.lng) }
    var jump by remember { mutableStateOf<MapJump?>(null) }
    // Name of the place we jumped to (search / quick place), kept until the map moves away from it
    var named by remember { mutableStateOf(start?.takeIf { it.name.isNotBlank() }) }
    var address by remember { mutableStateOf(start?.name ?: "") }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }

    fun moveTo(place: Place) {
        named = place
        address = place.name   // right away: the camera may not move (already there), so no lookup would run
        hint = null
        jump = MapJump(place.lat, place.lng)
    }

    val onCenter: (Double, Double) -> Unit = { lat, lng -> centerLat = lat; centerLng = lng }

    // Street address under the pin, looked up after the map stops moving
    LaunchedEffect(centerLat, centerLng) {
        val n = named
        if (n != null && metersBetween(n.lat, n.lng, centerLat, centerLng) < 30) {
            address = n.name
            return@LaunchedEffect
        }
        named = null
        address = ""
        delay(600)
        address = withContext(Dispatchers.IO) { describeLocation(context, centerLat, centerLng) }
    }

    // decorFitsSystemWindows = false: full screen, and systemBarsPadding() keeps the Save button above the nav bar
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.systemBarsPadding()) {
                // ---- top bar + search ----
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) { Text("← Back") }
                        Title(title)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it; hint = null },
                            label = { Text("Search a place") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(enabled = query.isNotBlank() && !searching, onClick = {
                            searching = true
                            scope.launch {
                                val place = withContext(Dispatchers.IO) { findPlace(context, query.trim()) }
                                searching = false
                                if (place != null) moveTo(place) else hint = "No place found for \"${query.trim()}\""
                            }
                        }) { Text(if (searching) "…" else "Search") }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PHNOM_PENH_PLACES) { place ->
                            OutlinedButton(onClick = { moveTo(place) }) { Text(place.name) }
                        }
                    }
                }

                // ---- map with a fixed pin in the middle ----
                // clipToBounds: the map view would otherwise draw over the rows above it
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                    if (USE_GOOGLE_MAPS) GooglePickerMap(first, jump, onCenter) else MapLibrePickerMap(first, jump, onCenter)
                    // The pin's tip sits on the map center
                    Text("📍", fontSize = 40.sp, modifier = Modifier.align(Alignment.Center).offset(y = (-22).dp))
                    SmallFloatingActionButton(
                        onClick = {
                            if (!hasLocationPermission(context)) {
                                hint = "Allow location access to use your GPS"
                            } else scope.launch {
                                val loc = currentLocation(context)
                                if (loc == null) {
                                    hint = "No GPS position yet. Try again in a moment."
                                } else {
                                    val name = withContext(Dispatchers.IO) { describeLocation(context, loc.latitude, loc.longitude) }
                                    moveTo(Place(name, loc.latitude, loc.longitude))
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    ) { Text("🎯") }
                }

                // ---- chosen place + save ----
                // Extra bottom space: dialogs don't always get the nav-bar inset, which hid the Save button
                Column(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    hint?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    MutedText("Drag the map or tap it to move the pin")
                    Text(address.ifBlank { "Finding address…" }, fontWeight = FontWeight.SemiBold)
                    MutedText("%.5f, %.5f".format(centerLat, centerLng))
                    BigButton("Save this location") {
                        val name = address.ifBlank { "%.5f, %.5f".format(centerLat, centerLng) }
                        onPick(Place(name, centerLat, centerLng))
                    }
                }
            }
        }
    }
}

/** "Pick on map" button: opens [MapPickerDialog] and gives back the saved place. */
@Composable
fun PickOnMapButton(title: String, start: Place?, onPick: (Place) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }) { Text("🗺 Pick on map") }
    if (open) {
        MapPickerDialog(title = title, start = start, onDismiss = { open = false }, onPick = { place ->
            open = false
            onPick(place)
        })
    }
}

/** Google Maps: smooth vector map. Reports the center once the camera stops moving. */
@Composable
private fun GooglePickerMap(start: Place, jump: MapJump?, onCenter: (Double, Double) -> Unit) {
    val scope = rememberCoroutineScope()
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(start.lat, start.lng), 16f)
    }
    LaunchedEffect(jump) {
        jump?.let { camera.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.lat, it.lng), 16f), 700) }
    }
    LaunchedEffect(camera.isMoving) {
        if (!camera.isMoving) camera.position.target.let { onCenter(it.latitude, it.longitude) }
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = camera,
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, mapToolbarEnabled = false),
        onMapClick = { scope.launch { camera.animate(CameraUpdateFactory.newLatLng(it), 400) } },
    )
}

/** Free OpenFreeMap vector style: no account, no API key. */
private const val OPENFREEMAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

/** MapLibre: smooth vector map with free OpenFreeMap tiles. Reports the center once the camera stops. */
@Composable
private fun MapLibrePickerMap(start: Place, jump: MapJump?, onCenter: (Double, Double) -> Unit) {
    val context = LocalContext.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val mapView = remember {
        MapLibre.getInstance(context)
        MlMapView(context).apply {
            onCreate(null)
            getMapAsync { m ->
                m.setStyle(OPENFREEMAP_STYLE)
                m.cameraPosition = MlCameraPosition.Builder().target(MlLatLng(start.lat, start.lng)).zoom(15.5).build()
                m.uiSettings.isTiltGesturesEnabled = false
                m.uiSettings.isRotateGesturesEnabled = false
                m.addOnCameraIdleListener {
                    val t = m.cameraPosition.target ?: return@addOnCameraIdleListener
                    onCenter(t.latitude, t.longitude)
                }
                // Tap anywhere: move the pin there
                m.addOnMapClickListener { p ->
                    m.animateCamera(MlCameraUpdateFactory.newLatLng(p), 400)
                    true
                }
                map = m
            }
        }
    }
    DisposableEffect(Unit) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    LaunchedEffect(jump, map) {
        val m = map ?: return@LaunchedEffect
        jump?.let { m.animateCamera(MlCameraUpdateFactory.newLatLngZoom(MlLatLng(it.lat, it.lng), 16.0), 700) }
    }
    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
}
