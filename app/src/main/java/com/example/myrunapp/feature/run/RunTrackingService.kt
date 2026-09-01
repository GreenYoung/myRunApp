package com.example.myrunapp.feature.run

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.example.myrunapp.MainActivity
import com.example.myrunapp.R
import com.example.myrunapp.core.data.AppDatabase
import com.example.myrunapp.core.log.AppLogger
import com.example.myrunapp.core.log.LogTags
import com.example.myrunapp.feature.exercise.ExerciseType
import com.example.myrunapp.feature.exercise.estimateExerciseCalories
import com.example.myrunapp.feature.exercise.formatDistance
import com.example.myrunapp.feature.exercise.formatPace
import com.example.myrunapp.feature.exercise.data.ExerciseRecordEntity
import com.example.myrunapp.feature.run.data.RunSessionEntity
import com.example.myrunapp.feature.weight.todayIsoDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RunTrackingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val gpsTrackFilter = GpsTrackFilter()
    private lateinit var locationManager: LocationManager
    private var amapLocationClient: AMapLocationClient? = null
    private var timerJob: Job? = null
    private var isSaving = false
    private var startTime: Long = 0L
    private var totalDistanceMeters = 0.0
    private var trackingWeightKg = 70.0

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(applicationContext)
        AMapLocationClient.updatePrivacyShow(applicationContext, true, true)
        AMapLocationClient.updatePrivacyAgree(applicationContext, true)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        ensureNotificationChannel()
        AppLogger.i(LogTags.RUN, "RunTrackingService created")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLogger.i(
            LogTags.RUN,
            "RunTrackingService onStartCommand action=${intent?.action} flags=$flags startId=$startId " +
                "isTracking=${RunTrackingStateStore.state.value.isTracking}"
        )
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_FINISH -> finishTracking()
            ACTION_DISCARD -> discardTracking()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        AppLogger.i(LogTags.RUN, "RunTrackingService destroying isTracking=${RunTrackingStateStore.state.value.isTracking}")
        stopLocationUpdates()
        stopTimer()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        AppLogger.w(
            LogTags.RUN,
            "RunTrackingService onTaskRemoved isTracking=${RunTrackingStateStore.state.value.isTracking} " +
                "points=${RunTrackingStateStore.state.value.trackPoints.size}"
        )
        super.onTaskRemoved(rootIntent)
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        AppLogger.i(LogTags.RUN, "startTracking requested currentTracking=${RunTrackingStateStore.state.value.isTracking}")
        if (RunTrackingStateStore.state.value.isTracking) return
        if (!hasPreciseLocationPermission()) {
            AppLogger.w(LogTags.RUN, "startTracking blocked: missing precise location permission")
            RunTrackingStateStore.update {
                it.copy(
                    hasLocationPermission = false,
                    errorMessage = "需要精确位置权限才能准确记录运动轨迹"
                )
            }
            stopSelf()
            return
        }

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!isGpsEnabled && !isNetworkEnabled) {
            AppLogger.w(LogTags.GPS, "startTracking blocked: no enabled location provider")
            RunTrackingStateStore.update {
                it.copy(
                    gpsStatusText = "请开启系统定位",
                    errorMessage = "请开启系统定位后再开始跑步"
                )
            }
            stopSelf()
            return
        }
        AppLogger.i(LogTags.GPS, "startTracking source=AMap gpsEnabled=$isGpsEnabled networkEnabled=$isNetworkEnabled")

        startRunForeground()

        startTime = System.currentTimeMillis()
        totalDistanceMeters = 0.0
        trackingWeightKg = 70.0
        gpsTrackFilter.reset()
        RunTrackingStateStore.update {
            RunTrackingUiState(
                isTracking = true,
                hasLocationPermission = true,
                hasNotificationPermission = it.hasNotificationPermission,
                isServiceRunning = true,
                gpsStatusText = "等待 GPS 定位"
            )
        }
        loadTrackingWeight()
        startTimer()

        try {
            startAmapLocationUpdates()
            AppLogger.i(LogTags.GPS, "AMap location updates registered interval=1000ms gpsFirst=true cache=false offset=true")
        } catch (error: Throwable) {
            AppLogger.e(LogTags.GPS, "AMap location updates failed", error)
            RunTrackingStateStore.update {
                it.copy(
                    hasLocationPermission = false,
                    isTracking = false,
                    gpsStatusText = "定位权限不可用",
                    errorMessage = "需要精确位置权限才能准确记录运动轨迹"
                )
            }
            stopForegroundAndSelf()
        }
    }

    private fun finishTracking() {
        if (isSaving) return
        val current = RunTrackingStateStore.state.value
        AppLogger.i(LogTags.RUN, "finishTracking requested canSave=${current.canSave} duration=${current.durationSeconds}s distance=${current.distanceKm} points=${current.trackPoints.size}")
        if (!current.canSave || startTime <= 0L) {
            RunTrackingStateStore.update {
                it.copy(errorMessage = "本次跑步数据太少，无法保存")
            }
            return
        }

        isSaving = true
        stopLocationUpdates()
        stopTimer()
        RunTrackingStateStore.update { it.copy(isSaving = true) }

        serviceScope.launch {
            val sessionId = saveRun(current)
            isSaving = false
            RunTrackingStateStore.emitSavedSessionId(sessionId)
            RunTrackingStateStore.reset()
            stopForegroundAndSelf()
        }
    }

    private fun discardTracking() {
        AppLogger.i(LogTags.RUN, "discardTracking points=${RunTrackingStateStore.state.value.trackPoints.size}")
        stopLocationUpdates()
        stopTimer()
        isSaving = false
        startTime = 0L
        totalDistanceMeters = 0.0
        trackingWeightKg = 70.0
        gpsTrackFilter.reset()
        RunTrackingStateStore.reset()
        stopForegroundAndSelf()
    }

    private suspend fun saveRun(current: RunTrackingUiState): Long = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(applicationContext)
        val endTime = System.currentTimeMillis()
        val recordDate = todayIsoDate()
        val weightKg = database.weightDao().getLatestWeightOnOrBefore(recordDate)?.weightKg ?: 70.0
        val caloriesKcal = estimateExerciseCalories(
            weightKg = weightKg,
            type = ExerciseType.OUTDOOR_RUNNING,
            durationSeconds = current.durationSeconds,
            distanceKm = current.distanceKm,
            inclinePercent = 0.0
        )
        val exerciseRecordId = database.exerciseDao().insertExercise(
            ExerciseRecordEntity(
                type = ExerciseType.OUTDOOR_RUNNING.name,
                startTime = startTime,
                durationSeconds = current.durationSeconds,
                distanceKm = current.distanceKm,
                caloriesKcal = caloriesKcal
            )
        )
        val sessionId = database.runDao().insertSession(
            RunSessionEntity(
                exerciseRecordId = exerciseRecordId,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = current.durationSeconds,
                distanceKm = current.distanceKm,
                caloriesKcal = caloriesKcal,
                createdAt = endTime
            )
        )
        database.runDao().insertTrackPoints(current.trackPoints.map { it.toEntity(sessionId) })
        AppLogger.i(
            LogTags.DATABASE,
            "run saved sessionId=$sessionId exerciseRecordId=$exerciseRecordId duration=${current.durationSeconds}s distance=${current.distanceKm} points=${current.trackPoints.size}"
        )
        sessionId
    }

    private fun handleLocation(location: Location) {
        AppLogger.d(LogTags.GPS, "raw location ${location.toDebugText()}")
        when (val result = gpsTrackFilter.filter(location)) {
            is LocationFilterResult.Accepted -> {
                totalDistanceMeters += result.distanceFromPreviousMeters
                RunTrackingStateStore.update { current ->
                    val points = current.trackPoints + result.point
                    val distanceKm = totalDistanceMeters / 1000.0
                    AppLogger.d(
                        LogTags.TRACK,
                        "accepted point size=${points.size} delta=${"%.2f".format(result.distanceFromPreviousMeters)}m total=${"%.4f".format(distanceKm)}km " +
                            "accuracy=${result.point.accuracyMeters} coord=${result.point.coordinateSystem} lat=${result.point.latitude} lon=${result.point.longitude}"
                    )
                    current.copy(
                        gpsStatusText = result.point.accuracyMeters?.let { "GPS 精度 ${it.toInt()}m" } ?: "GPS 信号良好",
                        distanceKm = distanceKm,
                        averagePaceText = formatPace(current.durationSeconds, distanceKm),
                        caloriesKcal = calculateRunCalories(current.durationSeconds, distanceKm),
                        canSave = current.durationSeconds > 0L && distanceKm > 0.0 && points.size >= 2,
                        trackPoints = points,
                        errorMessage = null
                    )
                }
                updateNotification()
            }
            is LocationFilterResult.Pending -> {
                logRejectedLocation(location, result.reason)
                AppLogger.d(LogTags.TRACK, "pending point reason=${result.reason} ${location.toDebugText()}")
                RunTrackingStateStore.update { it.copy(gpsStatusText = "正在确认 GPS 连续性") }
            }
            is LocationFilterResult.Rejected -> {
                logRejectedLocation(location, result.reason)
                AppLogger.d(LogTags.TRACK, "rejected point reason=${result.reason} ${location.toDebugText()}")
                RunTrackingStateStore.update { it.copy(gpsStatusText = gpsRejectStatusText(result.reason)) }
            }
        }
    }

    private fun startTimer() {
        stopTimer()
        timerJob = serviceScope.launch {
            while (true) {
                delay(1_000L)
                val durationSeconds = durationSecondsSince(startTime)
                RunTrackingStateStore.update { current ->
                    current.copy(
                        durationSeconds = durationSeconds,
                        averagePaceText = formatPace(durationSeconds, current.distanceKm),
                        caloriesKcal = calculateRunCalories(durationSeconds, current.distanceKm),
                        canSave = durationSeconds > 0L && current.distanceKm > 0.0 && current.trackPoints.size >= 2
                    )
                }
                updateNotification()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun loadTrackingWeight() {
        serviceScope.launch(Dispatchers.IO) {
            val recordDate = todayIsoDate()
            val weightKg = AppDatabase.getInstance(applicationContext)
                .weightDao()
                .getLatestWeightOnOrBefore(recordDate)
                ?.weightKg ?: 70.0
            trackingWeightKg = weightKg
            RunTrackingStateStore.update { current ->
                current.copy(caloriesKcal = calculateRunCalories(current.durationSeconds, current.distanceKm))
            }
            AppLogger.d(LogTags.RUN, "tracking calories weight loaded weightKg=$weightKg")
        }
    }

    private fun calculateRunCalories(durationSeconds: Long, distanceKm: Double): Int {
        return estimateExerciseCalories(
            weightKg = trackingWeightKg,
            type = ExerciseType.OUTDOOR_RUNNING,
            durationSeconds = durationSeconds,
            distanceKm = distanceKm,
            inclinePercent = 0.0
        )
    }

    private fun stopLocationUpdates() {
        runCatching {
            AppLogger.i(LogTags.GPS, "disable AMap background location")
            amapLocationClient?.disableBackgroundLocation(false)
            amapLocationClient?.stopLocation()
            amapLocationClient?.onDestroy()
            amapLocationClient = null
            AppLogger.i(LogTags.GPS, "AMap location updates stopped")
        }.onFailure {
            AppLogger.e(LogTags.GPS, "stop AMap location updates failed", it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAmapLocationUpdates() {
        stopLocationUpdates()
        amapLocationClient = AMapLocationClient(applicationContext).apply {
            setLocationOption(
                AMapLocationClientOption()
                    .setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy)
                    .setInterval(1_000L)
                    .setOnceLocation(false)
                    .setOnceLocationLatest(false)
                    .setNeedAddress(false)
                    .setGpsFirst(true)
                    .setGpsFirstTimeout(5_000L)
                    .setLocationCacheEnable(false)
                    .setOffset(true)
            )
            setLocationListener { location ->
                handleAmapLocation(location)
            }
            AppLogger.i(LogTags.GPS, "enable AMap background location notificationId=$NOTIFICATION_ID")
            enableBackgroundLocation(NOTIFICATION_ID, buildNotification(RunTrackingStateStore.state.value))
            startLocation()
        }
    }

    private fun handleAmapLocation(location: AMapLocation?) {
        if (location == null) {
            AppLogger.w(LogTags.GPS, "AMap location callback is null")
            RunTrackingStateStore.update { it.copy(gpsStatusText = "等待 GPS 定位") }
            return
        }
        AppLogger.d(
            LogTags.GPS,
            "AMap raw lat=${location.latitude} lon=${location.longitude} accuracy=${location.accuracy} " +
                "type=${location.locationType} gps=${location.gpsAccuracyStatus} coordType=${location.coordType} " +
                "offset=${location.isOffset} usable=${location.isCoorCanUseInMap} error=${location.errorCode} info=${location.errorInfo}"
        )
        if (location.errorCode != AMapLocation.LOCATION_SUCCESS) {
            AppLogger.w(LogTags.GPS, "AMap location failed error=${location.errorCode} info=${location.errorInfo}")
            RunTrackingStateStore.update { it.copy(gpsStatusText = "GPS 定位中") }
            return
        }
        handleLocation(location)
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(RunTrackingStateStore.state.value))
    }

    private fun startRunForeground() {
        val notification = buildNotification(RunTrackingStateStore.state.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AppLogger.i(LogTags.RUN, "startForeground type=location notificationId=$NOTIFICATION_ID")
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            AppLogger.i(LogTags.RUN, "startForeground legacy notificationId=$NOTIFICATION_ID")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(state: RunTrackingUiState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("MyRun 正在记录跑步")
            .setContentText("${formatDistance(state.distanceKm)} km · ${formatRunClock(state.durationSeconds)} · ${state.averagePaceText}")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "跑步记录",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun stopForegroundAndSelf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun hasPreciseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun gpsRejectStatusText(reason: LocationRejectReason): String {
        return when (reason) {
            LocationRejectReason.NO_ACCURACY,
            LocationRejectReason.POOR_ACCURACY -> "定位成功，GPS 精度较弱"
            LocationRejectReason.STALE_LOCATION -> "已忽略过旧定位"
            LocationRejectReason.TOO_CLOSE -> "已过滤静止漂移"
            LocationRejectReason.TOO_FREQUENT -> "GPS 点过密，已忽略"
            LocationRejectReason.IMPOSSIBLE_SPEED,
            LocationRejectReason.LARGE_JUMP,
            LocationRejectReason.SUSPECT_SPIKE -> "已过滤异常跳点"
            LocationRejectReason.MOCK_LOCATION -> "已忽略模拟定位"
            LocationRejectReason.INVALID_TIME -> "已忽略异常定位时间"
        }
    }

    private fun logRejectedLocation(location: Location, reason: LocationRejectReason) {
        Log.d(
            "GpsTrackFilter",
            "GPS_REJECT reason=$reason accuracy=${if (location.hasAccuracy()) location.accuracy else null} " +
                "speed=${if (location.hasSpeed()) location.speed else null} time=${location.time}"
        )
    }

    private fun Location.toDebugText(): String {
        return "provider=$provider lat=$latitude lon=$longitude accuracy=${if (hasAccuracy()) accuracy else null} " +
            "speed=${if (hasSpeed()) speed else null} time=$time elapsed=$elapsedRealtimeNanos ageMs=${locationAgeMs()}"
    }

    companion object {
        private const val CHANNEL_ID = "run_tracking"
        private const val NOTIFICATION_ID = 3001
        private const val ACTION_START = "com.example.myrunapp.feature.run.START"
        private const val ACTION_FINISH = "com.example.myrunapp.feature.run.FINISH"
        private const val ACTION_DISCARD = "com.example.myrunapp.feature.run.DISCARD"

        fun startIntent(context: Context): Intent {
            return Intent(context, RunTrackingService::class.java).setAction(ACTION_START)
        }

        fun finishIntent(context: Context): Intent {
            return Intent(context, RunTrackingService::class.java).setAction(ACTION_FINISH)
        }

        fun discardIntent(context: Context): Intent {
            return Intent(context, RunTrackingService::class.java).setAction(ACTION_DISCARD)
        }
    }
}
