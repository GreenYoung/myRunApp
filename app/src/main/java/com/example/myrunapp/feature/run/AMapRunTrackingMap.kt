package com.example.myrunapp.feature.run

import android.location.Location
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.LocationSource
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MyLocationStyle
import com.example.myrunapp.core.log.AppLogger
import com.example.myrunapp.core.log.LogTags
import com.example.myrunapp.ui.theme.AppSecondaryText
import kotlin.math.abs
import kotlin.math.roundToLong

private val RunTrackingMapFallback = Color(0xFF111820)
private const val RoadLevelZoom = 18f
private const val RoadLevelAccuracyMeters = 30f
private const val MaxNativeLocationAccuracyMeters = 80f
private const val StationaryDriftDistanceMeters = 8f
private const val StationaryAccuracyDeltaMeters = 6f

@Composable
fun AMapRunTrackingMap(
    points: List<RunTrackPointUiModel>,
    isTracking: Boolean,
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val validPoints = remember(points) { points.filterValidRunMapPoints() }
    val trackSignature = remember(validPoints) {
        validPoints.lastOrNull()?.let { point ->
            31 * validPoints.size + point.recordedAt
        } ?: 0L
    }
    val renderSignature = 31 * (31 * trackSignature + if (isTracking) 1L else 0L) +
        if (hasLocationPermission) 1L else 0L
    val lastDrawnSignature = remember { longArrayOf(Long.MIN_VALUE) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var fallbackMapLocation by remember { mutableStateOf<RunMapLocation?>(null) }
    val fallbackLocationSignature = fallbackMapLocation?.let { location ->
        31 * (location.latLng.latitude * 1_000_000).roundToLong() +
            (location.latLng.longitude * 1_000_000).roundToLong()
    } ?: 0L
    val finalRenderSignature = 31 * renderSignature + fallbackLocationSignature
    val shouldFollowNativeLocation = validPoints.size < 2
    val locationSource = remember(context, isTracking) {
        RunMapLocationSource(
            appContext = context.applicationContext,
            isTracking = isTracking
        ) { location ->
            fallbackMapLocation = location.toFallbackRunMapLocation()
        }
    }
    val mapView = remember {
        runCatching {
            AMapLocationClient.updatePrivacyShow(context.applicationContext, true, true)
            AMapLocationClient.updatePrivacyAgree(context.applicationContext, true)
            MapView(context).apply { onCreate(Bundle()) }
        }.onFailure {
            AppLogger.e(LogTags.MAP, "create run tracking MapView failed", it)
        }.getOrNull()
    }

    if (mapView == null) {
        RunMapUnavailableFallback(modifier = modifier)
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

    DisposableEffect(locationSource) {
        onDispose {
            locationSource.deactivate()
            locationSource.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            if (lastDrawnSignature[0] == finalRenderSignature) return@AndroidView
            lastDrawnSignature[0] = finalRenderSignature
            view.getMapAsyn { map ->
                map.configureSportTrackUi()
                AppLogger.d(
                    LogTags.MAP,
                    "render tracking map points=${validPoints.size} isTracking=$isTracking hasPermission=$hasLocationPermission fallback=${fallbackMapLocation != null}"
                )
                map.drawSportTrack(
                    context = context,
                    points = validPoints,
                    endLabel = if (isTracking) "LIVE" else "END",
                    showCurrentAsEnd = isTracking
                )
                map.configureNativeMyLocation(context, locationSource, hasLocationPermission)
                map.configureRoadLevelNativeLocationZoom(shouldFollowNativeLocation)
                view.post {
                    val latestPoint = validPoints.lastOrNull()?.toAmapLatLng()
                    val latestTrackPoint = validPoints.lastOrNull()
                    when {
                        validPoints.size >= 2 && latestPoint != null && latestTrackPoint.isReliableForRoadLevel() -> {
                            AppLogger.d(LogTags.MAP, "move camera to latest track point zoom=$RoadLevelZoom lat=${latestPoint.latitude} lon=${latestPoint.longitude}")
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latestPoint, RoadLevelZoom))
                        }
                        fallbackMapLocation != null && validPoints.size < 2 -> {
                            AppLogger.d(
                                LogTags.MAP,
                                "move camera to fallback location zoom=$RoadLevelZoom lat=${fallbackMapLocation!!.latLng.latitude} lon=${fallbackMapLocation!!.latLng.longitude}"
                            )
                            map.moveToCurrentLocation(fallbackMapLocation!!)
                            view.postDelayed(
                                { map.moveToCurrentLocation(fallbackMapLocation!!) },
                                350L
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun RunMapUnavailableFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunTrackingMapFallback),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "地图暂不可用",
            color = AppSecondaryText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun List<RunTrackPointUiModel>.filterValidRunMapPoints(): List<RunTrackPointUiModel> {
    return filterValidMapPoints()
}

private fun AMap.configureNativeMyLocation(
    context: android.content.Context,
    locationSource: RunMapLocationSource,
    hasLocationPermission: Boolean
) {
    if (!hasLocationPermission) {
        setMyLocationEnabled(false)
        return
    }
    setLocationSource(locationSource)
    setMyLocationStyle(
        MyLocationStyle()
            .myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW)
            .showMyLocation(true)
            .anchor(0.5f, 0.5f)
            .radiusFillColor(Color(0xFF22C55E).copy(alpha = 0.06f).toArgb())
            .strokeColor(Color(0xFF22C55E).copy(alpha = 0.22f).toArgb())
            .strokeWidth(1.5f)
            .myLocationIcon(BitmapDescriptorFactory.fromBitmap(createCurrentLocationBitmap(context)))
    )
    setMyLocationEnabled(true)
}

private fun AMap.configureRoadLevelNativeLocationZoom(shouldFollowNativeLocation: Boolean) {
    if (!shouldFollowNativeLocation) {
        setOnMyLocationChangeListener(null)
        return
    }
    setOnMyLocationChangeListener { location ->
        if (location.isUsableForRoadLevelZoom()) {
            val point = LatLng(location.latitude, location.longitude)
            moveCamera(CameraUpdateFactory.newLatLngZoom(point, RoadLevelZoom))
        }
    }
}

private class RunMapLocationSource(
    private val appContext: android.content.Context,
    private val isTracking: Boolean,
    private val onLocationChanged: (AMapLocation) -> Unit
) : LocationSource {
    private var listener: LocationSource.OnLocationChangedListener? = null
    private var client: AMapLocationClient? = null
    private var lastDispatchedLocation: AMapLocation? = null

    override fun activate(listener: LocationSource.OnLocationChangedListener) {
        this.listener = listener
        if (client != null) return

        AMapLocationClient.updatePrivacyShow(appContext, true, true)
        AMapLocationClient.updatePrivacyAgree(appContext, true)
        client = runCatching {
            AMapLocationClient(appContext).apply {
                setLocationOption(
                    AMapLocationClientOption()
                        .setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy)
                        .setInterval(if (isTracking) 1_000L else 2_500L)
                        .setOnceLocation(false)
                        .setOnceLocationLatest(false)
                        .setNeedAddress(false)
                        .setGpsFirst(true)
                        .setGpsFirstTimeout(5_000L)
                        .setLocationCacheEnable(false)
                        .setOffset(true)
                )
        setLocationListener { location ->
                    if (location != null && shouldDispatch(location)) {
                        lastDispatchedLocation = location
                        listener.onLocationChanged(location)
                        onLocationChanged(location)
                        logAcceptedMapLocation(location)
                    }
                }
                startLocation()
            }
        }.getOrNull()
    }

    override fun deactivate() {
        listener = null
        client?.stopLocation()
    }

    fun destroy() {
        client?.onDestroy()
        client = null
    }

    private fun shouldDispatch(location: AMapLocation): Boolean {
        if (!location.isSuccessfulNativeMapLocation()) return false

        val previous = lastDispatchedLocation ?: return true
        val distanceMeters = distanceMetersBetween(previous, location)
        val accuracyDelta = abs(location.accuracy - previous.accuracy)

        if (!isTracking &&
            distanceMeters < StationaryDriftDistanceMeters &&
            accuracyDelta < StationaryAccuracyDeltaMeters
        ) {
            logRejectedMapLocation("stationary_drift distance=$distanceMeters accuracyDelta=$accuracyDelta accuracy=${location.accuracy}")
            return false
        }

        return true
    }
}

private fun AMapLocation.isSuccessfulNativeMapLocation(): Boolean {
    if (errorCode != AMapLocation.LOCATION_SUCCESS) {
        logRejectedMapLocation("error=$errorCode info=$errorInfo")
        return false
    }
    if (accuracy <= 0f) {
        logRejectedMapLocation("poor_accuracy accuracy=$accuracy type=$locationType coordType=$coordType gps=$gpsAccuracyStatus")
        return false
    }
    if (accuracy > MaxNativeLocationAccuracyMeters) {
        logRejectedMapLocation("weak_accuracy accuracy=$accuracy type=$locationType coordType=$coordType gps=$gpsAccuracyStatus")
        return false
    }
    return true
}

private fun logAcceptedMapLocation(location: AMapLocation) {
    AppLogger.d(
        LogTags.MAP,
        "AMAP_NATIVE_ACCEPT lat=${location.latitude} lon=${location.longitude} accuracy=${location.accuracy} " +
            "type=${location.locationType} gps=${location.gpsAccuracyStatus} coordType=${location.coordType} " +
            "offset=${location.isOffset} usable=${location.isCoorCanUseInMap}"
    )
}

private fun logRejectedMapLocation(reason: String) {
    AppLogger.d(LogTags.MAP, "AMAP_REJECT $reason")
}

private fun RunTrackPointUiModel?.isReliableForRoadLevel(): Boolean {
    val accuracy = this?.accuracyMeters ?: return false
    return accuracy in 0f..RoadLevelAccuracyMeters
}

private fun AMapLocation.toFallbackRunMapLocation(): RunMapLocation? {
    if (accuracy <= 0f || accuracy > MaxNativeLocationAccuracyMeters) return null
    val isGpsLocation = locationType == AMapLocation.LOCATION_TYPE_GPS
    return RunMapLocation(
        latLng = com.amap.api.maps.model.LatLng(latitude, longitude),
        accuracyMeters = accuracy,
        isRoadLevelReliable = isGpsLocation &&
            accuracy <= RoadLevelAccuracyMeters &&
            gpsAccuracyStatus != AMapLocation.GPS_ACCURACY_BAD,
        isGpsLocation = isGpsLocation
    )
}

private fun distanceMetersBetween(from: Location, to: Location): Float {
    val result = FloatArray(1)
    Location.distanceBetween(
        from.latitude,
        from.longitude,
        to.latitude,
        to.longitude,
        result
    )
    return result[0]
}

private fun Location?.isUsableForRoadLevelZoom(): Boolean {
    if (this == null) return false
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
    if (latitude == 0.0 && longitude == 0.0) return false
    if (!hasAccuracy()) return true
    return accuracy > 0f && accuracy <= MaxNativeLocationAccuracyMeters
}
