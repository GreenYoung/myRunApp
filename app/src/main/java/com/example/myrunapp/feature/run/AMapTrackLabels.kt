package com.example.myrunapp.feature.run

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.amap.api.maps.AMap
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import kotlin.math.abs
import kotlin.math.roundToInt

internal val MarkerGreen = Color(0xFF22C55E)
internal val MarkerRed = Color(0xFFEF4444)
internal val MarkerWhite = Color(0xFFFFFFFF)

// 标记层级：起终点旗标必须在里程点之上，当前位置始终最顶
internal const val ZIndexKilometerDot = 29f
internal const val ZIndexKilometerText = 30f
internal const val ZIndexEndLabel = 40f
internal const val ZIndexStartLabel = 41f
internal const val ZIndexCurrentLocation = 45f

// 缩放变化超过该阈值才重新排版，避免相机跟随时反复重排
private const val ZoomRelayoutThreshold = 0.05f

private const val MarkerBitmapCacheBytes = 4 * 1024 * 1024
private const val FlagCapHeightDp = 20f
private const val FlagArrowDp = 6f
private const val FlagPaddingHorizontalDp = 7f
private const val FlagMinWidthDp = 26f
private const val FlagTextSizeSp = 9f

private val markerBitmapCache = object : LruCache<String, Bitmap>(MarkerBitmapCacheBytes) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

/** 一个待渲染的轨迹标签（真实经纬度 + 类型）。 */
internal data class TrackLabelSpec(
    val latLng: LatLng,
    val kind: TrackLabelKind,
    val kilometer: Int? = null
)

/**
 * 轨迹标签渲染器：负责"候选 → 布局 → 落图"，并持有已添加的 marker 以便缩放后重排。
 *
 * 起点/终点用旗标贴图（GO 向上展开、END 向下展开），所以同一点也不会互相遮挡；
 * 里程点按 [layoutTrackLabels] 的结果显示数字或降级成小圆点。
 */
internal class AMapTrackLabelRenderer(
    private val startText: String = "GO",
    private val endText: String = "END"
) {
    private val specs = mutableListOf<TrackLabelSpec>()
    private val markers = mutableListOf<Marker>()
    private var lastZoom = Float.NaN

    fun replaceAll(newSpecs: List<TrackLabelSpec>) {
        specs.clear()
        specs.addAll(newSpecs)
    }

    fun add(spec: TrackLabelSpec) {
        specs += spec
    }

    fun isEmpty(): Boolean = specs.isEmpty()

    /** 仅清理内部状态，不动地图（调用方通常已经 map.clear()）。 */
    fun reset() {
        markers.clear()
        specs.clear()
        lastZoom = Float.NaN
    }

    /** 终点标记（实时页结束跑步时才会出现）；位置变化返回 true。 */
    fun upsertEnd(latLng: LatLng): Boolean {
        val index = specs.indexOfFirst { it.kind == TrackLabelKind.END }
        if (index >= 0) {
            val existing = specs[index]
            if (existing.latLng.latitude == latLng.latitude && existing.latLng.longitude == latLng.longitude) {
                return false
            }
            specs[index] = existing.copy(latLng = latLng)
            return true
        }
        specs += TrackLabelSpec(latLng, TrackLabelKind.END)
        return true
    }

    /** 重新在地图上摆放全部标签（会先移除旧的 label marker）。 */
    fun draw(map: AMap, context: Context) {
        removeMarkers()
        if (specs.isEmpty()) return
        val zoom = runCatching { map.cameraPosition.zoom }.getOrNull()
        if (zoom != null) lastZoom = zoom

        val density = context.resources.displayMetrics.density
        val screenPoints = screenPoints(map)
        val placements = if (screenPoints == null) {
            // 地图还没布局好：退化成"全部显示"，保持旧行为，至少不会少东西
            specs.mapIndexed { index, spec ->
                TrackLabelPlacement(index, spec.kind, fallbackText(spec), isDot = false)
            }
        } else {
            val candidates = specs.mapIndexed { index, spec ->
                TrackLabelCandidate(
                    index = index,
                    kind = spec.kind,
                    x = screenPoints[index].x.toFloat(),
                    y = screenPoints[index].y.toFloat(),
                    kilometer = spec.kilometer
                )
            }
            layoutTrackLabels(candidates, density, startText, endText)
        }

        placements.forEach { placement ->
            val spec = specs[placement.index]
            markers += map.addMarker(buildMarkerOptions(context, spec, placement))
        }
    }

    /** 缩放变化时重排；返回是否真的重排过。纯平移不会改变标记间的相对距离，所以忽略平移。 */
    fun relayoutIfZoomChanged(map: AMap, context: Context): Boolean {
        val zoom = runCatching { map.cameraPosition.zoom }.getOrNull() ?: return false
        if (lastZoom.isNaN()) {
            lastZoom = zoom
            return false
        }
        if (abs(zoom - lastZoom) <= ZoomRelayoutThreshold) return false
        draw(map, context)
        return true
    }

    private fun removeMarkers() {
        markers.forEach { marker -> runCatching { marker.remove() } }
        markers.clear()
    }

    private fun screenPoints(map: AMap): List<android.graphics.Point>? = runCatching {
        val projection = map.projection ?: return null
        projection.visibleRegion ?: return null
        specs.map { spec -> projection.toScreenLocation(spec.latLng) }
    }.getOrNull()

    private fun fallbackText(spec: TrackLabelSpec): String = when (spec.kind) {
        TrackLabelKind.START -> startText
        TrackLabelKind.END -> endText
        TrackLabelKind.KILOMETER -> spec.kilometer?.toString().orEmpty()
    }

    private fun buildMarkerOptions(
        context: Context,
        spec: TrackLabelSpec,
        placement: TrackLabelPlacement
    ): MarkerOptions {
        val options = MarkerOptions()
            .position(spec.latLng)
            .title(placement.text)
        return when {
            placement.kind == TrackLabelKind.START -> options
                .anchor(0.5f, 1f)
                .zIndex(ZIndexStartLabel)
                .icon(
                    BitmapDescriptorFactory.fromBitmap(
                        flagMarkerBitmap(context, placement.text, MarkerGreen, arrowDown = true)
                    )
                )

            placement.kind == TrackLabelKind.END -> options
                .anchor(0.5f, 0f)
                .zIndex(ZIndexEndLabel)
                .icon(
                    BitmapDescriptorFactory.fromBitmap(
                        flagMarkerBitmap(context, placement.text, MarkerRed, arrowDown = false)
                    )
                )

            placement.isDot -> options
                .anchor(0.5f, 0.5f)
                .zIndex(ZIndexKilometerDot)
                .icon(BitmapDescriptorFactory.fromBitmap(kilometerDotBitmap(context)))

            else -> options
                .anchor(0.5f, 0.5f)
                .zIndex(ZIndexKilometerText)
                .icon(BitmapDescriptorFactory.fromBitmap(kilometerMarkerBitmap(context, placement.text)))
        }
    }
}

private fun flagMarkerBitmap(
    context: Context,
    text: String,
    background: Color,
    arrowDown: Boolean
): Bitmap {
    return cachedMarkerBitmap(
        "flag:$text:${background.toArgb()}:$arrowDown:${densityKey(context)}"
    ) {
        createFlagMarkerBitmap(context, text, background, arrowDown)
    }
}

private fun createFlagMarkerBitmap(
    context: Context,
    text: String,
    background: Color,
    arrowDown: Boolean
): Bitmap {
    val density = context.resources.displayMetrics.density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MarkerWhite.toArgb()
        textAlign = Paint.Align.CENTER
        textSize = FlagTextSizeSp * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val capHeight = FlagCapHeightDp * density
    val arrow = FlagArrowDp * density
    val width = (textPaint.measureText(text) + FlagPaddingHorizontalDp * 2 * density)
        .coerceAtLeast(FlagMinWidthDp * density)
        .roundToInt()
        .coerceAtLeast(1)
    val height = (capHeight + arrow).roundToInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = background.toArgb()
        style = Paint.Style.FILL
    }
    // arrowDown = 起点旗标：箭头在下方、指向起点；否则终点旗标：箭头在上方、指向终点
    val capTop = if (arrowDown) 0f else arrow
    canvas.drawRoundRect(
        0f,
        capTop,
        width.toFloat(),
        capTop + capHeight,
        capHeight / 2f,
        capHeight / 2f,
        fill
    )
    val halfArrowWidth = 4f * density
    val arrowPath = Path().apply {
        if (arrowDown) {
            moveTo(width / 2f - halfArrowWidth, capTop + capHeight - 0.5f)
            lineTo(width / 2f + halfArrowWidth, capTop + capHeight - 0.5f)
            lineTo(width / 2f, capTop + capHeight + arrow)
        } else {
            moveTo(width / 2f - halfArrowWidth, capTop + 0.5f)
            lineTo(width / 2f + halfArrowWidth, capTop + 0.5f)
            lineTo(width / 2f, capTop - arrow)
        }
        close()
    }
    canvas.drawPath(arrowPath, fill)

    val metrics = textPaint.fontMetrics
    val baseline = capTop + capHeight / 2f - (metrics.ascent + metrics.descent) / 2f
    canvas.drawText(text, width / 2f, baseline, textPaint)
    return bitmap
}

private fun kilometerMarkerBitmap(context: Context, text: String): Bitmap {
    return cachedMarkerBitmap("km:$text:${densityKey(context)}") {
        createCircleMarkerBitmap(context, text)
    }
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

private fun kilometerDotBitmap(context: Context): Bitmap {
    return cachedMarkerBitmap("km-dot:${densityKey(context)}") {
        createKilometerDotBitmap(context)
    }
}

private fun createKilometerDotBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (7 * density).roundToInt().coerceAtLeast(3)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MarkerGreen.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center - 1f * density, paint)

    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MarkerWhite.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    canvas.drawCircle(center, center, center - 1f * density, ring)
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

private fun cachedMarkerBitmap(key: String, create: () -> Bitmap): Bitmap {
    markerBitmapCache.get(key)?.let { return it }
    return create().also { markerBitmapCache.put(key, it) }
}

private fun densityKey(context: Context): Int {
    return (context.resources.displayMetrics.density * 100).roundToInt()
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
