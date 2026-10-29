package com.example.myrunapp.feature.run

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.Circle
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import kotlin.math.roundToInt

private val TrackGradientStart = Color(0xFF22C55E)
private val TrackGradientMiddle = Color(0xFF06B6D4)
private val TrackGradientEnd = Color(0xFFF97316)
private val TrackSlow = Color(0xFF22C55E)
private val TrackSteady = Color(0xFF06B6D4)
private val TrackFast = Color(0xFFF97316)
private val MarkerGreen = Color(0xFF22C55E)
private val MarkerRed = Color(0xFFEF4444)
private val MarkerWhite = Color(0xFFFFFFFF)
private const val RoadBearing = 0f
private const val TrackBoundsPadding = 58
private const val MaxRenderTrackPoints = 700
private const val RenderGapMinTimeMs = 12_000L
private const val RenderGapMinDistanceMeters = 45f
private const val RenderGapMaxDistanceMeters = 70f
private const val LivePolylineTailPoints = 200
private const val MarkerBitmapCacheBytes = 4 * 1024 * 1024

private val markerBitmapCache = object : LruCache<String, Bitmap>(MarkerBitmapCacheBytes) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

internal data class RunMapCameraStyle(
    val zoom: Float,
    val tilt: Float
)

internal data class RunMapLocation(
    val latLng: LatLng,
    val accuracyMeters: Float,
    val isRoadLevelReliable: Boolean,
    val isGpsLocation: Boolean
)

internal fun AMap.configureSportTrackUi(mapDisplayType: RunMapDisplayType = RunMapDisplayType.Normal) {
    setMapType(mapDisplayType.amapType)
    setTrafficEnabled(false)
    showBuildings(mapDisplayType == RunMapDisplayType.Normal)
    uiSettings.setScrollGesturesEnabled(true)
    uiSettings.setZoomGesturesEnabled(true)
    uiSettings.setTiltGesturesEnabled(false)
    uiSettings.setRotateGesturesEnabled(false)
    uiSettings.setZoomInByScreenCenter(true)
    uiSettings.setGestureScaleByMapCenter(false)
    uiSettings.setZoomControlsEnabled(false)
    uiSettings.setCompassEnabled(false)
    uiSettings.setMyLocationButtonEnabled(false)
    uiSettings.setScaleControlsEnabled(true)
}

internal fun AMap.drawSportTrack(
    context: Context,
    points: List<RunTrackPointUiModel>,
    endLabel: String = "END",
    showCurrentAsEnd: Boolean = false,
    splitRenderGaps: Boolean = false,
    currentLocation: RunMapLocation? = null
) {
    clear()
    if (points.isEmpty()) {
        currentLocation?.let { drawCurrentLocationMarker(context, it) }
        return
    }

    val sourceSegments = if (splitRenderGaps) points.splitByRenderGap() else listOf(points)
    val renderSegments = sourceSegments.map { it.simplifyForMapRender() }
    val displayPoints = renderSegments.flatten()
    val latLngs = displayPoints.map { it.toAmapLatLng() }
    addMarker(
        MarkerOptions()
            .position(latLngs.first())
            .title("GO")
            .anchor(0.5f, 0.5f)
            .zIndex(20f)
            .icon(BitmapDescriptorFactory.fromBitmap(routeLabelMarkerBitmap(context, "GO", MarkerGreen)))
    )

    if (latLngs.size >= 2) {
        drawSpeedGradientPolylines(renderSegments)
        addKilometerMarkers(context, points)
    }

    if (latLngs.size >= 2 || showCurrentAsEnd) {
        addMarker(
            MarkerOptions()
                .position(latLngs.last())
                .title(endLabel)
                .anchor(0.5f, 0.5f)
                .zIndex(21f)
                .icon(BitmapDescriptorFactory.fromBitmap(routeLabelMarkerBitmap(context, endLabel, MarkerRed)))
        )
    }

    currentLocation?.let { drawCurrentLocationMarker(context, it) }
}

internal fun AMap.moveToSportTrack(
    points: List<RunTrackPointUiModel>,
    mapDisplayType: RunMapDisplayType = RunMapDisplayType.Normal
) {
    if (points.isEmpty()) return

    val latLngs = points.map { it.toAmapLatLng() }
    if (latLngs.size == 1) {
        moveToRoadLevel(latLngs.first(), mapDisplayType)
        return
    }

    val bounds = LatLngBounds.Builder().apply {
        latLngs.forEach(::include)
    }.build()
    runCatching {
        moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, TrackBoundsPadding))
        if (mapDisplayType == RunMapDisplayType.Satellite) {
            moveToRoadLevel(latLngs.trackCenter(), mapDisplayType)
        }
    }
}

internal fun AMap.moveToRoadLevel(
    point: LatLng,
    mapDisplayType: RunMapDisplayType = RunMapDisplayType.Normal
) {
    val cameraStyle = mapDisplayType.cameraStyle()
    moveCamera(
        CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder()
                .target(point)
                .zoom(cameraStyle.zoom)
                .tilt(cameraStyle.tilt)
                .bearing(RoadBearing)
                .build()
        )
    )
}

internal fun AMap.moveToCurrentLocation(
    location: RunMapLocation,
    mapDisplayType: RunMapDisplayType = RunMapDisplayType.Normal
) {
    moveToRoadLevel(location.latLng, mapDisplayType)
}

internal fun RunMapDisplayType.cameraStyle(): RunMapCameraStyle {
    return when (this) {
        RunMapDisplayType.Normal -> RunMapCameraStyle(zoom = 18.8f, tilt = 0f)
        RunMapDisplayType.Night -> RunMapCameraStyle(zoom = 18.5f, tilt = 0f)
        RunMapDisplayType.Satellite -> RunMapCameraStyle(zoom = 17.6f, tilt = 0f)
    }
}

private fun List<LatLng>.trackCenter(): LatLng {
    var minLatitude = first().latitude
    var maxLatitude = first().latitude
    var minLongitude = first().longitude
    var maxLongitude = first().longitude
    forEach { point ->
        minLatitude = minOf(minLatitude, point.latitude)
        maxLatitude = maxOf(maxLatitude, point.latitude)
        minLongitude = minOf(minLongitude, point.longitude)
        maxLongitude = maxOf(maxLongitude, point.longitude)
    }
    return LatLng(
        (minLatitude + maxLatitude) / 2.0,
        (minLongitude + maxLongitude) / 2.0
    )
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
            .width(17f)
            .colorValues(colorValues)
            .zIndex(10f)
    )
}

private fun AMap.drawSpeedGradientPolyline(points: List<RunTrackPointUiModel>) {
    addPolyline(buildSpeedPolylineOptions(points))
}

private fun buildSpeedPolylineOptions(points: List<RunTrackPointUiModel>): PolylineOptions {
    val latLngs = points.map { it.toAmapLatLng() }
    val colorValues = points.indices.map { index ->
        val speedMps = when {
            index == 0 && points.size > 1 -> segmentSpeedMps(points[0], points[1])
            index > 0 -> segmentSpeedMps(points[index - 1], points[index])
            else -> points[index].speedMetersPerSecond ?: 0f
        }
        speedToTrackColor(speedMps).toArgb()
    }
    return PolylineOptions()
        .addAll(latLngs)
        .width(17f)
        .colorValues(colorValues)
        .zIndex(10f)
}

private fun AMap.drawSpeedGradientPolylines(segments: List<List<RunTrackPointUiModel>>) {
    segments.forEach { segment ->
        if (segment.size >= 2) {
            drawSpeedGradientPolyline(segment)
        }
    }
}

private fun speedToTrackColor(speedMps: Float): Color {
    return when {
        speedMps <= 0f -> TrackGradientMiddle
        speedMps < 1.8f -> TrackSlow
        speedMps < 3.2f -> TrackSteady
        else -> TrackFast
    }
}

private fun segmentSpeedMps(
    from: RunTrackPointUiModel,
    to: RunTrackPointUiModel
): Float {
    to.speedMetersPerSecond?.let { if (it > 0f) return it }
    val deltaSeconds = deltaTimeMs(from, to) / 1000f
    if (deltaSeconds <= 0f) return 0f
    return distanceMetersBetween(from, to) / deltaSeconds
}

private fun List<RunTrackPointUiModel>.simplifyForMapRender(
    maxPoints: Int = MaxRenderTrackPoints
): List<RunTrackPointUiModel> {
    if (size <= maxPoints) return this
    val step = (size - 1).toFloat() / (maxPoints - 1).coerceAtLeast(1)
    return List(maxPoints) { index ->
        this[(index * step).roundToInt().coerceIn(indices)]
    }.distinctBy { it.recordedAt }
}

private fun List<RunTrackPointUiModel>.splitByRenderGap(): List<List<RunTrackPointUiModel>> {
    if (size < 2) return listOf(this)
    val segments = mutableListOf<MutableList<RunTrackPointUiModel>>()
    var currentSegment = mutableListOf(first())

    zipWithNext().forEach { (from, to) ->
        if (isRenderGap(from, to)) {
            segments += currentSegment
            currentSegment = mutableListOf(to)
        } else {
            currentSegment += to
        }
    }
    segments += currentSegment
    return segments
}

private fun isRenderGap(
    from: RunTrackPointUiModel,
    to: RunTrackPointUiModel
): Boolean {
    val deltaMs = deltaTimeMs(from, to)
    val distanceMeters = distanceMetersBetween(from, to)
    return (deltaMs >= RenderGapMinTimeMs && distanceMeters >= RenderGapMinDistanceMeters) ||
        distanceMeters >= RenderGapMaxDistanceMeters
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
                    .icon(BitmapDescriptorFactory.fromBitmap(kilometerMarkerBitmap(context, nextKilometer.toString())))
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

private fun routeLabelMarkerBitmap(context: Context, text: String, background: Color): Bitmap {
    return cachedMarkerBitmap("route:$text:${background.toArgb()}:${densityKey(context)}") {
        createRouteLabelMarkerBitmap(context, text, background)
    }
}

private fun kilometerMarkerBitmap(context: Context, text: String): Bitmap {
    return cachedMarkerBitmap("km:$text:${densityKey(context)}") {
        createCircleMarkerBitmap(context, text)
    }
}

private fun cachedMarkerBitmap(key: String, create: () -> Bitmap): Bitmap {
    markerBitmapCache.get(key)?.let { return it }
    return create().also { markerBitmapCache.put(key, it) }
}

private fun densityKey(context: Context): Int {
    return (context.resources.displayMetrics.density * 100).roundToInt()
}

private fun createRouteLabelMarkerBitmap(
    context: Context,
    text: String,
    background: Color
): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (28 * density).roundToInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = background.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center - 0.5f * density, paint)

    drawCenteredText(canvas, text, MarkerWhite, 8.5f * density, Typeface.BOLD)
    return bitmap
}

private fun createCircleMarkerBitmap(context: Context, text: String): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (18 * density).roundToInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MarkerGreen.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center - 0.5f * density, paint)

    drawCenteredText(canvas, text, MarkerWhite, 7.5f * density, Typeface.BOLD)
    return bitmap
}

internal fun createCurrentLocationBitmap(context: Context): Bitmap {
    return cachedMarkerBitmap("current:${densityKey(context)}") {
        buildCurrentLocationBitmap(context)
    }
}

private fun buildCurrentLocationBitmap(context: Context): Bitmap {
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

/**
 * Incremental renderer for the live tracking map. Completed polyline chunks,
 * kilometer markers and the start marker are kept across updates; only the
 * current tail chunk, the end marker and the current-location marker change.
 */
internal class AMapLiveTrackRenderer {
    private var map: AMap? = null
    private var renderedCount = 0
    private var lastRenderedTime = Long.MIN_VALUE
    private var lastPoint: RunTrackPointUiModel? = null
    private var endLabel: String? = null

    private var tailPoints = mutableListOf<RunTrackPointUiModel>()
    private var tailPolyline: Polyline? = null

    private var endMarker: Marker? = null
    private var currentMarker: Marker? = null
    private var currentCircle: Circle? = null

    private var cumulativeMeters = 0f
    private var nextKilometer = 1

    fun render(
        map: AMap,
        context: Context,
        points: List<RunTrackPointUiModel>,
        isTracking: Boolean,
        currentLocation: RunMapLocation?
    ) {
        if (this.map !== map) {
            resetState()
            this.map = map
        }
        val canAppend = points.size >= renderedCount &&
            (renderedCount == 0 || points[renderedCount - 1].recordedAt == lastRenderedTime)
        if (!canAppend) {
            resetState()
        }
        val targetMap = this.map ?: return
        if (points.isEmpty()) {
            updateCurrentLocation(targetMap, context, currentLocation)
            return
        }

        val startIndex: Int
        if (renderedCount == 0) {
            targetMap.addMarker(
                MarkerOptions()
                    .position(points.first().toAmapLatLng())
                    .title("GO")
                    .anchor(0.5f, 0.5f)
                    .zIndex(20f)
                    .icon(BitmapDescriptorFactory.fromBitmap(routeLabelMarkerBitmap(context, "GO", MarkerGreen)))
            )
            lastPoint = points.first()
            tailPoints = mutableListOf(points.first())
            startIndex = 1
        } else {
            startIndex = renderedCount
        }

        appendPoints(targetMap, context, points, startIndex)
        renderedCount = points.size
        lastRenderedTime = points.last().recordedAt
        updateEndMarker(targetMap, context, points.last(), isTracking)
        updateCurrentLocation(targetMap, context, currentLocation)
    }

    private fun appendPoints(
        map: AMap,
        context: Context,
        points: List<RunTrackPointUiModel>,
        startIndex: Int
    ) {
        var index = startIndex
        while (index < points.size) {
            val point = points[index]
            val from = lastPoint
            if (from == null) {
                lastPoint = point
                tailPoints = mutableListOf(point)
                index += 1
                continue
            }

            val segmentMeters = distanceMetersBetween(from, point)
            if (segmentMeters > 0f) {
                appendKilometerMarkers(map, context, from, point, segmentMeters)
            }
            if (isRenderGap(from, point)) {
                commitTail()
                tailPoints = mutableListOf(point)
            } else {
                tailPoints.add(point)
            }

            lastPoint = point
            if (tailPoints.size >= LivePolylineTailPoints) {
                drawTail(map)
                commitTail()
                tailPoints = mutableListOf(point)
            }
            index += 1
        }
        drawTail(map)
    }

    private fun drawTail(map: AMap) {
        tailPolyline?.remove()
        tailPolyline = null
        if (tailPoints.size >= 2) {
            tailPolyline = map.addPolyline(buildSpeedPolylineOptions(tailPoints))
        }
    }

    private fun commitTail() {
        tailPolyline = null
        tailPoints = mutableListOf()
    }

    private fun appendKilometerMarkers(
        map: AMap,
        context: Context,
        from: RunTrackPointUiModel,
        to: RunTrackPointUiModel,
        segmentMeters: Float
    ) {
        while (cumulativeMeters + segmentMeters >= nextKilometer * 1000f) {
            val ratio = ((nextKilometer * 1000f) - cumulativeMeters) / segmentMeters
            val markerPoint = interpolatePoint(from, to, ratio.coerceIn(0f, 1f))
            map.addMarker(
                MarkerOptions()
                    .position(markerPoint.toAmapLatLng())
                    .title("${nextKilometer}km")
                    .anchor(0.5f, 0.5f)
                    .zIndex(22f)
                    .icon(BitmapDescriptorFactory.fromBitmap(kilometerMarkerBitmap(context, nextKilometer.toString())))
            )
            nextKilometer += 1
        }
        cumulativeMeters += segmentMeters
    }

    private fun updateEndMarker(
        map: AMap,
        context: Context,
        point: RunTrackPointUiModel,
        isTracking: Boolean
    ) {
        val label = if (isTracking) "LIVE" else "END"
        val latLng = point.toAmapLatLng()
        val marker = endMarker
        if (marker == null) {
            endLabel = label
            endMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(label)
                    .anchor(0.5f, 0.5f)
                    .zIndex(21f)
                    .icon(BitmapDescriptorFactory.fromBitmap(routeLabelMarkerBitmap(context, label, MarkerRed)))
            )
            return
        }
        marker.setPosition(latLng)
        if (endLabel != label) {
            endLabel = label
            marker.setTitle(label)
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(routeLabelMarkerBitmap(context, label, MarkerRed)))
        }
    }

    private fun updateCurrentLocation(
        map: AMap,
        context: Context,
        location: RunMapLocation?
    ) {
        val marker = currentMarker
        val circle = currentCircle
        if (location == null) {
            marker?.isVisible = false
            circle?.isVisible = false
            return
        }
        if (marker == null || circle == null) {
            currentCircle = map.addCircle(
                CircleOptions()
                    .center(location.latLng)
                    .radius(location.accuracyMeters.toDouble())
                    .strokeWidth(1.5f)
                    .strokeColor(MarkerGreen.copy(alpha = 0.22f).toArgb())
                    .fillColor(MarkerGreen.copy(alpha = 0.06f).toArgb())
                    .zIndex(4f)
            )
            currentMarker = map.addMarker(
                MarkerOptions()
                    .position(location.latLng)
                    .title("当前位置")
                    .anchor(0.5f, 0.5f)
                    .zIndex(30f)
                    .icon(BitmapDescriptorFactory.fromBitmap(createCurrentLocationBitmap(context)))
            )
            return
        }
        marker.isVisible = true
        circle.isVisible = true
        marker.setPosition(location.latLng)
        circle.setCenter(location.latLng)
        circle.setRadius(location.accuracyMeters.toDouble())
    }

    private fun resetState() {
        runCatching { map?.clear() }
        renderedCount = 0
        lastRenderedTime = Long.MIN_VALUE
        lastPoint = null
        endLabel = null
        tailPoints = mutableListOf()
        tailPolyline = null
        endMarker = null
        currentMarker = null
        currentCircle = null
        cumulativeMeters = 0f
        nextKilometer = 1
    }
}
