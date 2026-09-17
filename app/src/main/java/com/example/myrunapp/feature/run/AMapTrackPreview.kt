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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapView
import com.example.myrunapp.core.log.AppLogger
import com.example.myrunapp.core.log.LogTags
import com.example.myrunapp.ui.theme.AppSecondaryText

private val TrackPreviewMapFallback = Color(0x990C1218)

@Composable
fun AMapTrackPreview(
    points: List<RunTrackPointUiModel>,
    mapDisplayType: RunMapDisplayType = RunMapDisplayType.Normal,
    modifier: Modifier = Modifier
) {
    val validPoints = remember(points) { points.filterValidMapPoints() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        runCatching {
            AMapLocationClient.updatePrivacyShow(context.applicationContext, true, true)
            AMapLocationClient.updatePrivacyAgree(context.applicationContext, true)
            MapView(context).apply {
                onCreate(Bundle())
            }
        }.onFailure {
            AppLogger.e(LogTags.MAP, "create track preview MapView failed", it)
        }.getOrNull()
    }

    if (mapView == null) {
        MapUnavailableFallback(modifier = modifier)
        return
    }

    DisposableEffect(lifecycleOwner, mapView) {
        var destroyed = false
        fun destroyOnce() {
            if (!destroyed) {
                mapView.onDestroy()
                destroyed = true
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
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
            AppLogger.d(
                LogTags.MAP,
                "render track preview points=${validPoints.size} mapType=${mapDisplayType.label}"
            )
            map.drawSportTrack(
                context = context,
                points = validPoints,
                endLabel = "END"
            )
            view.post {
                map.moveToSportTrack(validPoints, mapDisplayType)
            }
        }
    )
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
