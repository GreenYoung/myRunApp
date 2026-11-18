package com.example.myrunapp.feature.run

import android.content.Context
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
private const val RoadBearing = 0f
internal const val TrackBoundsPadding = 58
private const val MaxRenderTrackPoints = 700
private const val RenderGapMinTimeMs = 12_000L
private const val RenderGapMinDistanceMeters = 45f
private const val RenderGapMaxDistanceMeters = 70f
private const val LivePolylineTailPoints = 200

// 鐎规矮缍呮稉顓熸焽婢跺嫮娈戦搹姘卞殠鏉╃偞甯撮敍姘辩叾閺夎法浼?+ 閾忔氨鍤庨敍灞炬绾喛銆冪粈?鏉╂瑦顔岄弰顖涘腹鐎规矮缍呯純?
private val TrackGapColor = Color(0xFF64748B)
private const val TrackGapAlpha = 0.85f
private const val TrackGapLineWidthPx = 7f

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

/**
 * 閸樺棗褰舵潪銊ㄦ姉濞撳弶鐓嬮崳顭掔窗娑撯偓濞嗏剝鈧呮暰鐎瑰矁寤烘潻閫涚瑢閺嶅洩顔囬敍灞借嫙閸︺劎缂夐弨鎯у綁閸栨牕鎮楅柌宥嗗笓閺嶅洩顔囬妴?
 *
 * 閺嶅洩顔囬敍鍫ｆ崳缂佸牏鍋ｉ弮妤佺垼閵嗕線鍣风粙瀣仯閿涘绮烘稉鈧禍銈囩舶 [AMapTrackLabelRenderer]閿?
 * 閻㈠崬鐣犻崑姘潌楠炴洜鈹栭梻鎾缉鐠佲晪绱濋柆鍨帳缂佹洖婀€/閹舵绻戞潪銊ㄦ姉娑撳﹤顦挎稉顏呯垼鐠佹澘褰旈崷銊ょ鐠ф灚鈧?
 */
internal class AMapStaticTrackRenderer {
    private val labels = AMapTrackLabelRenderer()

    fun render(
        map: AMap,
        context: Context,
        points: List<RunTrackPointUiModel>
    ) {
        map.clear()
        labels.reset()
        if (points.isEmpty()) return

        // 鐎规矮缍呮稉顓熸焽閿涘牊甯€閺?瀵彉淇婇崣鍑ょ礆婢跺嫪绗夐悽璇茬杽缁惧尅绱濋弨鍦暰閾忔氨鍤庢潻鐐村复閿涙俺寤烘潻鍦箙鐠ч攱娼甸弰顖濈箾缂侇厾娈戦敍?
        // 娴ｅ棙鐗卞蹇庣瑐閺勫海鈥樼悰銊ㄦ彧"鏉╂瑤绔村▓鐢垫畱閸ф劖鐖ｉ弰顖涘腹濞村娈?閿涘苯鐤勯弮鎯板缓鏉╃懓鎷伴崢鍡楀蕉鏉炪劏鎶楁穱婵囧瘮娑撯偓閼锋番鈧?
        val renderSegments = points.splitByRenderGap().map { it.simplifyForMapRender() }
        val displayPoints = renderSegments.flatten()
        val latLngs = displayPoints.map { it.toAmapLatLng() }
        val specs = mutableListOf(TrackLabelSpec(latLngs.first(), TrackLabelKind.START))
        if (latLngs.size >= 2) {
            map.drawSpeedGradientPolylines(renderSegments)
            map.drawGapConnectors(renderSegments)
            specs += kilometerLabelSpecs(points)
            // 缂佸牏鍋ｉ弮妤佺垼閸氭垳绗呯仦鏇炵磻閿涘苯鎷伴崥鎴滅瑐閻?GO 閺冩鐖ｆ禍鎺嶇瑝闁喗灏呴敍鍫濇儓鐠ч鍋ｉ崡宕囩矒閻愬湱娈戦悳顖滃殠閿?
            specs += TrackLabelSpec(latLngs.last(), TrackLabelKind.END)
        }
        labels.replaceAll(specs)
        labels.draw(map, context)
    }

    fun relayoutIfZoomChanged(map: AMap, context: Context): Boolean =
        labels.relayoutIfZoomChanged(map, context)

    fun reset() {
        labels.reset()
    }
}

internal fun AMap.moveToSportTrack(
    points: List<RunTrackPointUiModel>,
    mapDisplayType: RunMapDisplayType = RunMapDisplayType.Normal,
    paddingPx: Int = TrackBoundsPadding
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
        // padding 閸楁洑缍呴弰顖氬剼缁辩媴绱扮挧椋庣矒閻愯妫楅弽鍥︾窗娑撳﹣绗呯仦鏇炵磻閿涘瞼鏆€閻ф垝绗夋径鐔剁窗閹跺﹨鍒涙潏鍦畱閺冩鐖ｇ憗浣瑰竴
        moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx))
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
            .title("瑜版挸澧犳担宥囩枂")
            .anchor(0.5f, 0.5f)
            .zIndex(ZIndexCurrentLocation)
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

/** 閺傤厽銆傛径鍕暰娑撯偓閺壜ゆ珓缁惧尅绱濋幎濠傚閸氬簼琚卞▓浣冨缓鏉?閹?鐠ч攱娼甸敍灞肩稻娑撳秴鍟嬮崗鍛埂鐎圭偠寤烘潻骞库偓?*/
private fun buildGapConnectorOptions(
    from: RunTrackPointUiModel,
    to: RunTrackPointUiModel
): PolylineOptions {
    return PolylineOptions()
        .add(from.toAmapLatLng())
        .add(to.toAmapLatLng())
        .width(TrackGapLineWidthPx)
        .color(TrackGapColor.copy(alpha = TrackGapAlpha).toArgb())
        .setDottedLine(true)
        .setDottedLineType(PolylineOptions.DOTTEDLINE_TYPE_SQUARE)
        .zIndex(9f)
}

private fun AMap.drawGapConnectors(segments: List<List<RunTrackPointUiModel>>) {
    segments.zipWithNext().forEach { (fromSegment, toSegment) ->
        val from = fromSegment.lastOrNull() ?: return@forEach
        val to = toSegment.firstOrNull() ?: return@forEach
        addPolyline(buildGapConnectorOptions(from, to))
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

/** 閹稿鐤拋陇绐涚粋鑽ょ暬閸戠儤澧嶉張澶嬫殻閸忣剟鍣烽弽鍥唶閻愮櫢绱欓崣顏嗙暬閸ф劖鐖ｉ敍灞炬Ц閸氾附妯夌粈鐑樻瀮鐎涙ぞ姘︾紒娆愮垼缁涙儳绔风仦鈧崘鍐茬暰閿涘鈧?*/
internal fun kilometerLabelSpecs(points: List<RunTrackPointUiModel>): List<TrackLabelSpec> {
    val specs = mutableListOf<TrackLabelSpec>()
    var cumulativeMeters = 0f
    var nextKilometer = 1
    points.zipWithNext().forEach { (from, to) ->
        val segmentMeters = distanceMetersBetween(from, to)
        if (segmentMeters <= 0f) return@forEach

        while (cumulativeMeters + segmentMeters >= nextKilometer * 1000f) {
            val ratio = ((nextKilometer * 1000f) - cumulativeMeters) / segmentMeters
            val markerPoint = interpolatePoint(from, to, ratio.coerceIn(0f, 1f))
            specs += TrackLabelSpec(
                latLng = markerPoint.toAmapLatLng(),
                kind = TrackLabelKind.KILOMETER,
                kilometer = nextKilometer
            )
            nextKilometer += 1
        }
        cumulativeMeters += segmentMeters
    }
    return specs
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
 * 实时轨迹渲染器：已完成片段、里程标签与起点标签跨帧保留，
 * 每帧只更新尾段折线、终点标签（跑完才出现）和当前位置点。
 */
internal class AMapLiveTrackRenderer {
    private var map: AMap? = null
    private var renderedCount = 0
    private var lastRenderedTime = Long.MIN_VALUE
    private var lastPoint: RunTrackPointUiModel? = null

    private var tailPoints = mutableListOf<RunTrackPointUiModel>()
    private var tailPolyline: Polyline? = null

    private var currentMarker: Marker? = null
    private var currentCircle: Circle? = null

    private val labels = AMapTrackLabelRenderer()

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

        var labelsDirty = false
        val startIndex: Int
        if (renderedCount == 0) {
            labels.add(TrackLabelSpec(points.first().toAmapLatLng(), TrackLabelKind.START))
            labelsDirty = true
            lastPoint = points.first()
            tailPoints = mutableListOf(points.first())
            startIndex = 1
        } else {
            startIndex = renderedCount
        }

        if (appendPoints(targetMap, points, startIndex)) {
            labelsDirty = true
        }
        renderedCount = points.size
        lastRenderedTime = points.last().recordedAt
        // 追踪中不画终点标签：当前位置的绿点就代表"我在这"。
        // 旧实现的红色 LIVE 标签 zIndex(21) 低于当前位置点(30)，本来就基本看不见。
        if (!isTracking && labels.upsertEnd(points.last().toAmapLatLng())) {
            labelsDirty = true
        }
        if (labelsDirty) {
            labels.draw(targetMap, context)
        }
        updateCurrentLocation(targetMap, context, currentLocation)
    }

    /** 缩放变化后重排里程标签，避免放大后仍停留在聚合结果。 */
    fun relayoutIfZoomChanged(map: AMap, context: Context): Boolean =
        labels.relayoutIfZoomChanged(map, context)

    private fun appendPoints(
        map: AMap,
        points: List<RunTrackPointUiModel>,
        startIndex: Int
    ): Boolean {
        var addedKilometers = false
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
            if (segmentMeters > 0f && appendKilometers(from, point, segmentMeters)) {
                addedKilometers = true
            }
            if (isRenderGap(from, point)) {
                commitTail()
                map.addPolyline(buildGapConnectorOptions(from, point))
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
        return addedKilometers
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

    private fun appendKilometers(
        from: RunTrackPointUiModel,
        to: RunTrackPointUiModel,
        segmentMeters: Float
    ): Boolean {
        var added = false
        while (cumulativeMeters + segmentMeters >= nextKilometer * 1000f) {
            val ratio = ((nextKilometer * 1000f) - cumulativeMeters) / segmentMeters
            val markerPoint = interpolatePoint(from, to, ratio.coerceIn(0f, 1f))
            labels.add(
                TrackLabelSpec(
                    latLng = markerPoint.toAmapLatLng(),
                    kind = TrackLabelKind.KILOMETER,
                    kilometer = nextKilometer
                )
            )
            added = true
            nextKilometer += 1
        }
        cumulativeMeters += segmentMeters
        return added
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
                    .zIndex(ZIndexCurrentLocation)
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
        labels.reset()
        renderedCount = 0
        lastRenderedTime = Long.MIN_VALUE
        lastPoint = null
        tailPoints = mutableListOf()
        tailPolyline = null
        currentMarker = null
        currentCircle = null
        cumulativeMeters = 0f
        nextKilometer = 1
    }
}
