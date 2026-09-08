package com.example.myrunapp.feature.run

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import kotlin.math.roundToInt

private val TrackHalo = Color(0xB3FFFFFF)
private val TrackGradientStart = Color(0xFF06B6D4)
private val TrackGradientMiddle = Color(0xFF22C55E)
private val TrackGradientEnd = Color(0xFFA3E635)
private val MarkerHalo = Color(0x73FFFFFF)
private val MarkerGreen = Color(0xFF22C55E)
private val MarkerRed = Color(0xFFEF4444)
private val MarkerWhite = Color(0xFFFFFFFF)
private const val RoadZoom = 18f
private const val TrackBoundsPadding = 44

internal data class RunMapLocation(
    val latLng: LatLng,
    val accuracyMeters: Float,
    val isRoadLevelReliable: Boolean,
    val isGpsLocation: Boolean
)

internal fun AMap.configureSportTrackUi(mapDisplayType: RunMapDisplayType = RunMapDisplayType.Normal) {
    setMapType(mapDisplayType.amapType)
    setTrafficEnabled(false)
    showBuildings(false)
    uiSettings.setScrollGesturesEnabled(true)
    uiSettings.setZoomGesturesEnabled(true)
    uiSettings.setTiltGesturesEnabled(false)
    uiSettings.setRotateGesturesEnabled(false)
    uiSettings.setZoomInByScreenCenter(true)
    uiSettings.setGestureScaleByMapCenter(false)
}

internal fun AMap.drawSportTrack(
    context: Context,
    points: List<RunTrackPointUiModel>,
    endLabel: String = "END",
    showCurrentAsEnd: Boolean = false,
    currentLocation: RunMapLocation? = null
) {
    clear()
    if (points.isEmpty()) {
        currentLocation?.let { drawCurrentLocationMarker(context, it) }
        return
    }

    val latLngs = points.map { it.toAmapLatLng() }
    addMarker(
        MarkerOptions()
            .position(latLngs.first())
            .title("GO")
            .anchor(0.5f, 0.5f)
            .zIndex(20f)
            .icon(BitmapDescriptorFactory.fromBitmap(createRouteLabelMarkerBitmap(context, "GO", MarkerGreen)))
    )

    if (latLngs.size >= 2) {
        drawGradientPolyline(latLngs)
        addKilometerMarkers(context, points)
    }

    if (latLngs.size >= 2 || showCurrentAsEnd) {
        addMarker(
            MarkerOptions()
                .position(latLngs.last())
                .title(endLabel)
                .anchor(0.5f, 0.5f)
                .zIndex(21f)
                .icon(BitmapDescriptorFactory.fromBitmap(createRouteLabelMarkerBitmap(context, endLabel, MarkerRed)))
        )
    }

    currentLocation?.let { drawCurrentLocationMarker(context, it) }
}

internal fun AMap.moveToSportTrack(points: List<RunTrackPointUiModel>) {
    if (points.isEmpty()) return

    val latLngs = points.map { it.toAmapLatLng() }
    if (latLngs.size == 1) {
        moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.first(), RoadZoom))
        return
    }

    val bounds = LatLngBounds.Builder().apply {
        latLngs.forEach(::include)
    }.build()
    runCatching {
        moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, TrackBoundsPadding))
    }
}

internal fun AMap.moveToRoadLevel(point: LatLng) {
    moveCamera(CameraUpdateFactory.newLatLngZoom(point, RoadZoom))
}

internal fun AMap.moveToCurrentLocation(location: RunMapLocation) {
    moveCamera(CameraUpdateFactory.newLatLngZoom(location.latLng, RoadZoom))
}

internal fun AMap.drawCurrentLocationMarker(context: Context, location: RunMapLocation) {
    addCircle(
        CircleOptions()
            .center(location.latLng)
            .radius(location.accuracyMeters.toDouble())
            .strokeWidth(1.5f)
            .strokeColor(MarkerGreen.copy(alpha = 0.22f).toArgb())
            .fillColor(MarkerGreen.copy(alpha = 0.06f).toArgb())
            .zIndex(4f)
    )
    addMarker(
        MarkerOptions()
            .position(location.latLng)
            .title("当前位置")
            .anchor(0.5f, 0.5f)
            .zIndex(30f)
            .icon(BitmapDescriptorFactory.fromBitmap(createCurrentLocationBitmap(context)))
    )
}

internal fun List<RunTrackPointUiModel>.filterValidMapPoints(): List<RunTrackPointUiModel> {
    return filter { point ->
        point.latitude in -90.0..90.0 &&
            point.longitude in -180.0..180.0 &&
            !(point.latitude == 0.0 && point.longitude == 0.0)
    }
}

internal fun RunTrackPointUiModel.toAmapLatLng(): LatLng {
    if (coordinateSystem == CoordinateSystem.GCJ02) {
        return LatLng(latitude, longitude)
    }
    val coordinate = wgs84ToGcj02(latitude, longitude)
    return LatLng(coordinate.latitude, coordinate.longitude)
}

private fun AMap.drawGradientPolyline(latLngs: List<LatLng>) {
    val colorValues = latLngs.indices.map { index ->
        val progress = index / (latLngs.lastIndex.toFloat()).coerceAtLeast(1f)
        when {
            progress < 0.55f -> lerpColor(TrackGradientStart, TrackGradientMiddle, progress / 0.55f)
            else -> lerpColor(TrackGradientMiddle, TrackGradientEnd, (progress - 0.55f) / 0.45f)
        }.toArgb()
    }
    addPolyline(
        PolylineOptions()
            .addAll(latLngs)
            .width(18f)
            .color(TrackHalo.toArgb())
            .zIndex(8f)
    )
    addPolyline(
        PolylineOptions()
            .addAll(latLngs)
            .width(12f)
            .colorValues(colorValues)
            .zIndex(10f)
    )
}

private fun AMap.addKilometerMarkers(
    context: Context,
    points: List<RunTrackPointUiModel>
) {
    var cumulativeMeters = 0f
    var nextKilometer = 1
    points.zipWithNext().forEach { (from, to) ->
        val segmentMeters = distanceMetersBetween(from, to)
        if (segmentMeters <= 0f) return@forEach

        while (cumulativeMeters + segmentMeters >= nextKilometer * 1000f) {
            val ratio = ((nextKilometer * 1000f) - cumulativeMeters) / segmentMeters
            val markerPoint = interpolatePoint(from, to, ratio.coerceIn(0f, 1f))
            addMarker(
                MarkerOptions()
                    .position(markerPoint.toAmapLatLng())
                    .title("${nextKilometer}km")
                    .anchor(0.5f, 0.5f)
                    .zIndex(22f)
                    .icon(BitmapDescriptorFactory.fromBitmap(createCircleMarkerBitmap(context, nextKilometer.toString())))
            )
            nextKilometer += 1
        }
        cumulativeMeters += segmentMeters
    }
}

private fun interpolatePoint(
    from: RunTrackPointUiModel,
    to: RunTrackPointUiModel,
    ratio: Float
): RunTrackPointUiModel {
    return from.copy(
        latitude = from.latitude + (to.latitude - from.latitude) * ratio,
        longitude = from.longitude + (to.longitude - from.longitude) * ratio,
        recordedAt = from.recordedAt + ((to.recordedAt - from.recordedAt) * ratio).toLong()
    )
}

private fun createRouteLabelMarkerBitmap(
    context: Context,
    text: String,
    background: Color
): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (32 * density).roundToInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MarkerHalo.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center - 0.5f * density, paint)

    paint.apply {
        color = background.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center - 3f * density, paint)

    drawCenteredText(canvas, text, MarkerWhite, 9.5f * density, Typeface.BOLD)
    return bitmap
}

private fun createCircleMarkerBitmap(context: Context, text: String): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (22 * density).roundToInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MarkerHalo.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center - 0.5f * density, paint)

    paint.apply {
        color = MarkerGreen.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center - 3f * density, paint)

    drawCenteredText(canvas, text, MarkerWhite, 8.5f * density, Typeface.BOLD)
    return bitmap
}

internal fun createCurrentLocationBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (30 * density).roundToInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MarkerGreen.copy(alpha = 0.14f).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center - density, paint)

    paint.apply {
        color = MarkerGreen.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, 6.5f * density, paint)

    paint.apply {
        color = MarkerWhite.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    canvas.drawCircle(center, center, 6.5f * density, paint)

    return bitmap
}

private fun drawCenteredText(
    canvas: Canvas,
    text: String,
    textColor: Color,
    textSizePx: Float,
    typefaceStyle: Int
) {
    val bounds = Rect()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.toArgb()
        textAlign = Paint.Align.CENTER
        textSize = textSizePx
        typeface = Typeface.create(Typeface.DEFAULT, typefaceStyle)
    }
    paint.getTextBounds(text, 0, text.length, bounds)
    val x = canvas.width / 2f
    val y = canvas.height / 2f - bounds.exactCenterY()
    canvas.drawText(text, x, y, paint)
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val clamped = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * clamped,
        green = start.green + (end.green - start.green) * clamped,
        blue = start.blue + (end.blue - start.blue) * clamped,
        alpha = start.alpha + (end.alpha - start.alpha) * clamped
    )
}
