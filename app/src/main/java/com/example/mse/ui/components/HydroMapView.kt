package com.example.mse.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.example.mse.model.GeoPoint
import com.example.mse.model.HydroObject
import com.example.mse.model.HydroObjectType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import android.graphics.Color as AColor

private val UKRAINE_CENTER = LatLng(48.6, 31.0)

private fun letterOf(type: HydroObjectType) = when (type) {
    HydroObjectType.HPP -> "Г"
    HydroObjectType.PSPP -> "А"
    HydroObjectType.SMALL_HPP -> "м"
}

private fun markerIcon(context: Context, item: HydroObject, selected: Boolean): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val radius = (if (selected) 19 else 13) * density
    val size = (radius * 2 + 6 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = size / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = if (selected) AColor.parseColor("#102A43") else AColor.WHITE
    canvas.drawCircle(c, c, radius + 2 * density, paint)
    paint.color = statusColor(item.status).toArgb()
    canvas.drawCircle(c, c, radius, paint)
    paint.color = AColor.WHITE
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textSize = radius * 1.1f
    canvas.drawText(letterOf(item.type), c, c - (paint.descent() + paint.ascent()) / 2, paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun userIcon(context: Context): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val size = (34 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val c = size / 2f
    paint.color = AColor.argb(70, 25, 118, 210)
    canvas.drawCircle(c, c, c, paint)
    paint.color = AColor.WHITE
    canvas.drawCircle(c, c, 9 * density, paint)
    paint.color = AColor.rgb(25, 118, 210)
    canvas.drawCircle(c, c, 7 * density, paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Composable
fun HydroMapView(
    objects: List<HydroObject>,
    selectedId: String?,
    userPosition: GeoPoint?,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    centerOnUserTick: Int = 0
) {
    val context = LocalContext.current
    // BitmapDescriptorFactory працює лише після ініціалізації Maps SDK.
    remember { MapsInitializer.initialize(context.applicationContext) }
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(UKRAINE_CENTER, 5.2f) }

    LaunchedEffect(selectedId) {
        val target = objects.firstOrNull { it.id == selectedId } ?: return@LaunchedEffect
        val zoom = maxOf(camera.position.zoom, 8f)
        camera.animate(CameraUpdateFactory.newLatLngZoom(LatLng(target.lat, target.lon), zoom), 700)
    }

    LaunchedEffect(centerOnUserTick) {
        val p = userPosition ?: return@LaunchedEffect
        if (centerOnUserTick > 0) camera.animate(CameraUpdateFactory.newLatLngZoom(LatLng(p.lat, p.lon), 9f), 700)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false)
    ) {
        objects.forEach { item ->
            val selected = item.id == selectedId
            val state = remember(item.id) { MarkerState(LatLng(item.lat, item.lon)) }
            LaunchedEffect(selected) { if (selected) state.showInfoWindow() }
            Marker(
                state = state,
                title = item.name,
                snippet = "${item.type.title} · ${item.capacityMw.toInt()} МВт · ${item.status.title}",
                icon = remember(item.status, item.type, selected) { markerIcon(context, item, selected) },
                anchor = Offset(0.5f, 0.5f),
                zIndex = if (selected) 1f else 0f,
                onClick = {
                    onMarkerClick(item.id)
                    false
                }
            )
        }
        userPosition?.let { p ->
            Marker(
                state = remember(p) { MarkerState(LatLng(p.lat, p.lon)) },
                title = "Ваше місцезнаходження",
                icon = remember { userIcon(context) },
                anchor = Offset(0.5f, 0.5f),
                zIndex = 2f
            )
        }
    }
}
