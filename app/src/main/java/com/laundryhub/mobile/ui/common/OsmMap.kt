package com.laundryhub.mobile.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** A point to draw on the map. */
data class MapPoint(val lat: Double, val lng: Double, val title: String)

/**
 * Free OpenStreetMap (osmdroid) inside Compose. No API key needed.
 * The camera centers on `center` (e.g. the driver), or on the first point.
 */
@Composable
fun OsmMap(points: List<MapPoint>, center: MapPoint?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.5)
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            map.overlays.clear()
            for (p in points) {
                val marker = Marker(map)
                marker.position = GeoPoint(p.lat, p.lng)
                marker.title = p.title
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map.overlays.add(marker)
            }
            val focus = center ?: points.firstOrNull()
            if (focus != null) map.controller.setCenter(GeoPoint(focus.lat, focus.lng))
            map.invalidate()
        },
    )
}
