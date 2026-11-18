package com.example.myrunapp.feature.run

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.maps.model.CameraPosition
import com.example.myrunapp.core.log.AppLogger
import com.example.myrunapp.core.log.LogTags
import com.example.myrunapp.ui.theme.AppSecondaryText
import kotlin.math.roundToInt

private val TrackPreviewMapFallback = Color(0x990C1218)

@Composable
fun AMapTrackPreview(
    points: List<RunTrackPointUiModel>,
    mapDisplayType: RunMapDisplayType = RunMapDisplayType.Normal,
    modifier: Modifier = Modifier
) {
    val validPoints = remember(points.size, points.lastOrNull()?.recordedAt) {
        points.filterValidMapPoints()
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // MapView 自身状态（相机位置/地图类型）必须走标准 savedInstanceState，
    // 在 ON_PAUSE 抓取，旋转后 onCreate 还原。
    val mapState = rememberSaveable { Bundle() }
    val mapView = remember {
        runCatching {
            AMapLocationClient.updatePrivacyShow(context.applicationContext, true, true)
            AMapLocationClient.updatePrivacyAgree(context.applicationContext, true)
            MapView(context).apply {
                onCreate(mapState)
            }
        }.onFailure {
            AppLogger.e(LogTags.MAP, "create track preview MapView failed", it)
        }.getOrNull()
    }

    if (mapView == null) {
        MapUnavailableFallback(modifier = modifier)
        return
    }

    val trackRenderer = remember(mapView) { AMapStaticTrackRenderer() }
    val renderedSignature = remember(mapView) { arrayOfNulls<String>(1) }
    val cameraListenerAttached = remember(mapView) { booleanArrayOf(false) }
    val boundsPaddingPx = (36 * context.resources.displayMetrics.density).roundToInt()

    DisposableEffect(lifecycleOwner, mapView) {
        var destroyed = false
        fun destroyOnce() {
            if (!destroyed) {
                trackRenderer.reset()
                mapView.onDestroy()
                destroyed = true
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onSaveInstanceState(mapState)
                    mapView.onPause()
                }
                Lifecycle.Event.ON_DESTROY -> destroyOnce()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            destroyOnce()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            val map = view.map
            map.configureSportTrackUi(mapDisplayType)
            // 只在点集真正变化时重画：update 每次重组都会跑，而 drawSportTrack 会 clear()，
            // 无去重会和下面的相机回调互相触发，造成标记反复重建。
            val signature = trackSignature(validPoints)
            if (renderedSignature[0] != signature) {
                renderedSignature[0] = signature
                AppLogger.d(
                    LogTags.MAP,
                    "render track preview points=${validPoints.size} mapType=${mapDisplayType.label}"
                )
                trackRenderer.render(map, context, validPoints)
                view.post {
                    map.moveToSportTrack(validPoints, mapDisplayType, boundsPaddingPx)
                }
            }
            if (!cameraListenerAttached[0]) {
                cameraListenerAttached[0] = true
                map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
                    override fun onCameraChange(position: CameraPosition?) = Unit

                    override fun onCameraChangeFinish(position: CameraPosition?) {
                        trackRenderer.relayoutIfZoomChanged(map, context)
                    }
                })
            }
        }
    )
}

/** 点集签名：数量 + 首尾时间，够用来判断"要不要重画整条轨迹"。 */
private fun trackSignature(points: List<RunTrackPointUiModel>): String {
    return "${points.size}:${points.firstOrNull()?.recordedAt}:${points.lastOrNull()?.recordedAt}"
}

@Composable
private fun MapUnavailableFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TrackPreviewMapFallback),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "地图暂不可用",
            color = AppSecondaryText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
