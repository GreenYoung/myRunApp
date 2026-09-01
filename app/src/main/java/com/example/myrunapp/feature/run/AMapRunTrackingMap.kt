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
import androidx.compose.runtime.LaunchedEffect
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
private const val MaxMapDisplayAccuracyMeters = 80f
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
    var mapConfigured by remember { mutableStateOf(false) }
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

    LaunchedEffect(mapView, locationSource, hasLocationPermission, shouldFollowNativeLocation) {
        mapConfigured = false
        AppLogger.d(
            LogTags.MAP,
            "request tracking map async configure hasPermission=$hasLocationPermission followNative=$shouldFollowNativeLocation"
        )
        mapView.getMapAsyn { map ->
            map.configureSportTrackUi()
            map.configureNativeMyLocation(context, locationSource, hasLocationPermission)
            map.configureRoadLevelNativeLocationZoom(shouldFollowNativeLocation)
            mapConfigured = true
            AppLogger.d(
                LogTags.MAP,
                "tracking map configured hasPermission=$hasLocationPermission followNative=$shouldFollowNativeLocation"
            )
        }
    }

    LaunchedEffect(mapConfigured, locationSource, hasLocationPermission) {
        if (mapConfigured && hasLocationPermission) {
            AppLogger.d(LogTags.MAP, "start map location source after map configured")
            locationSource.start()
        }
    }

    LaunchedEffect(mapConfigured, finalRenderSignature) {
        AppLogger.d(
            LogTags.MAP,
            "track render requested signature=$finalRenderSignature points=${validPoints.size} isTracking=$isTracking " +
                "fallback=${fallbackMapLocation != null} mapConfigured=$mapConfigured"
        )
        if (!mapConfigured) {
            AppLogger.d(LogTags.MAP, "skip track render before map configured")
            return@LaunchedEffect
        }
        if (lastDrawnSignature[0] == finalRenderSignature) return@LaunchedEffect
        lastDrawnSignature[0] = finalRenderSignature
        mapView.getMapAsyn { map ->
            map.renderRunTrackingTrack(
                context = context,
                mapView = mapView,
                points = validPoints,
                isTracking = isTracking,
                fallbackMapLocation = fallbackMapLocation
            )
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = {}
    )
}

private fun AMap.renderRunTrackingTrack(
    context: android.content.Context,
    mapView: MapView,
    points: List<RunTrackPointUiModel>,
    isTracking: Boolean,
    fallbackMapLocation: RunMapLocation?
) {
    configureSportTrackUi()
    AppLogger.d(
        LogTags.MAP,
        "track render executed points=${points.size} isTracking=$isTracking fallback=${fallbackMapLocation != null}"
    )
    drawSportTrack(
        context = context,
        points = points,
        endLabel = if (isTracking) "LIVE" else "END",
        showCurrentAsEnd = isTracking,
        currentLocation = fallbackMapLocation
    )
    mapView.post {
        val latestPoint = points.lastOrNull()?.toAmapLatLng()
        val latestTrackPoint = points.lastOrNull()
        when {
            latestPoint != null && latestTrackPoint.isReliableForRoadLevel() -> {
                AppLogger.d(
                    LogTags.MAP,
                    "move camera to latest reliable track point points=${points.size} zoom=$RoadLevelZoom lat=${latestPoint.latitude} lon=${latestPoint.longitude}"
                )
                moveCamera(CameraUpdateFactory.newLatLngZoom(latestPoint, RoadLevelZoom))
            }
            latestPoint != null -> {
                AppLogger.d(
                    LogTags.MAP,
                    "move camera to latest weak track point points=${points.size} accuracy=${latestTrackPoint?.accuracyMeters} " +
                        "zoom=$RoadLevelZoom lat=${latestPoint.latitude} lon=${latestPoint.longitude}"
                )
                moveCamera(CameraUpdateFactory.newLatLngZoom(latestPoint, RoadLevelZoom))
            }
            fallbackMapLocation != null && points.size < 2 -> {
                AppLogger.d(
                    LogTags.MAP,
                    "move camera to fallback location roadReliable=${fallbackMapLocation.isRoadLevelReliable} " +
                        "accuracy=${fallbackMapLocation.accuracyMeters} zoom=$RoadLevelZoom " +
                        "lat=${fallbackMapLocation.latLng.latitude} lon=${fallbackMapLocation.latLng.longitude}"
                )
                moveToCurrentLocation(fallbackMapLocation)
                mapView.postDelayed(
                    { moveToCurrentLocation(fallbackMapLocation) },
                    350L
                )
            }
        }
    }
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
        AppLogger.i(LogTags.MAP, "RunMapLocationSource activate isTracking=$isTracking clientExists=${client != null}")
        this.listener = listener
        start()
    }

    fun start() {
        if (client != null) {
            AppLogger.d(LogTags.MAP, "RunMapLocationSource start skipped existing client")
            return
        }

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
                    AppLogger.d(
                        LogTags.MAP,
                        "RunMapLocationSource callback null=${location == null} " +
                            "lat=${location?.latitude} lon=${location?.longitude} accuracy=${location?.accuracy} " +
                            "type=${location?.locationType} gps=${location?.gpsAccuracyStatus} error=${location?.errorCode} info=${location?.errorInfo}"
                    )
                    if (location != null && shouldDispatchForMapDisplay(location)) {
                        lastDispatchedLocation = location
                        this@RunMapLocationSource.listener?.onLocationChanged(location)
                        onLocationChanged(location)
                        logAcceptedMapLocation(location)
                    }
                }
                AppLogger.i(
                    LogTags.MAP,
                    "RunMapLocationSource startLocation interval=${if (isTracking) 1_000L else 2_500L} gpsFirst=true maxDisplayAccuracy=$MaxMapDisplayAccuracyMeters"
                )
                startLocation()
            }
        }.onFailure {
            AppLogger.e(LogTags.MAP, "RunMapLocationSource start failed", it)
        }.getOrNull()
    }

    override fun deactivate() {
        AppLogger.i(LogTags.MAP, "RunMapLocationSource deactivate")
        listener = null
        client?.stopLocation()
    }

    fun destroy() {
        AppLogger.i(LogTags.MAP, "RunMapLocationSource destroy")
        client?.onDestroy()
        client = null
    }

    private fun shouldDispatchForMapDisplay(location: AMapLocation): Boolean {
        if (!location.isDisplayableNativeMapLocation()) return false

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

private fun AMapLocation.isDisplayableNativeMapLocation(): Boolean {
    if (errorCode != AMapLocation.LOCATION_SUCCESS) {
        logRejectedMapLocation("error=$errorCode info=$errorInfo")
        return false
    }
    if (accuracy <= 0f) {
        logRejectedMapLocation("poor_accuracy accuracy=$accuracy type=$locationType coordType=$coordType gps=$gpsAccuracyStatus")
        return false
    }
    if (accuracy > MaxMapDisplayAccuracyMeters) {
        logRejectedMapLocation("too_weak_for_map_display accuracy=$accuracy max=$MaxMapDisplayAccuracyMeters type=$locationType coordType=$coordType gps=$gpsAccuracyStatus")
        return false
    }
    if (accuracy > RoadLevelAccuracyMeters) {
        logWeakMapLocation(location = this)
    }
    return true
}

private fun logAcceptedMapLocation(location: AMapLocation) {
    AppLogger.d(
        LogTags.MAP,
        "AMAP_NATIVE_ACCEPT lat=${location.latitude} lon=${location.longitude} accuracy=${location.accuracy} " +
            "type=${location.locationType} gps=${location.gpsAccuracyStatus} coordType=${location.coordType} " +
            "offset=${location.isOffset} usable=${location.isCoorCanUseInMap} roadReliable=${location.isReliableForRoadLevelDisplay()}"
    )
}

private fun logWeakMapLocation(location: AMapLocation) {
    AppLogger.d(
        LogTags.MAP,
        "AMAP_MAP_DISPLAY_WEAK accuracy=${location.accuracy} max=$MaxMapDisplayAccuracyMeters type=${location.locationType} " +
            "gps=${location.gpsAccuracyStatus} usable=${location.isCoorCanUseInMap}"
    )
}

private fun logRejectedMapLocation(reason: String) {
    AppLogger.d(LogTags.MAP, "AMAP_REJECT $reason")
}

private fun AMapLocation.isReliableForRoadLevelDisplay(): Boolean {
    return accuracy > 0f &&
        accuracy <= RoadLevelAccuracyMeters &&
        locationType == AMapLocation.LOCATION_TYPE_GPS &&
        gpsAccuracyStatus != AMapLocation.GPS_ACCURACY_BAD
}

private fun RunTrackPointUiModel?.isReliableForRoadLevel(): Boolean {
    val accuracy = this?.accuracyMeters ?: return false
    return accuracy in 0f..RoadLevelAccuracyMeters
}

private fun AMapLocation.toFallbackRunMapLocation(): RunMapLocation? {
    if (accuracy <= 0f || accuracy > MaxMapDisplayAccuracyMeters) return null
    val isGpsLocation = locationType == AMapLocation.LOCATION_TYPE_GPS
    return RunMapLocation(
        latLng = com.amap.api.maps.model.LatLng(latitude, longitude),
        accuracyMeters = accuracy,
        isRoadLevelReliable = isReliableForRoadLevelDisplay(),
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
    return accuracy > 0f && accuracy <= RoadLevelAccuracyMeters
}
