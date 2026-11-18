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
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
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
import com.example.myrunapp.feature.run.data.RunDraftDao
import com.example.myrunapp.feature.run.data.RunDraftEntity
import com.example.myrunapp.feature.run.data.RunSessionEntity
import com.example.myrunapp.feature.run.data.RunTrackPointEntity
import com.example.myrunapp.feature.weight.todayIsoDate
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val TrackGapLogMinTimeMs = 8_000L
private const val TrackGapLogMinDistanceMeters = 40f
private const val DraftPersistIntervalMs = 5_000L
private const val LocationSilenceWarnMs = 8_000L
private const val LocationSilenceRestartMs = 30_000L
private const val InitialLocationGraceMs = 30_000L
private const val LocationRestartCooldownMs = 20_000L
private const val LocationErrorRestartDelayMs = 2_000L
// 网络定位只用于地图蓝点展示，且精度必须好于该值；再差的点会让蓝点直接跳到几百米外
private const val NonGpsMapDisplayMaxAccuracyMeters = 15f

// AMap 错误码 4：网络连接异常/定位超时（info 常见 Location timeout#0402#pm100011）
private const val AmapErrorCodeConnectionFailure = 4

class RunTrackingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val gpsTrackFilter = GpsTrackFilter()
    private lateinit var locationManager: LocationManager
    private var amapLocationClient: AMapLocationClient? = null
    private var timerJob: Job? = null
    private var isSaving = false
    /** 起点墙上时间，只用于落库的 startTime 与草稿展示，不参与计时。 */
    private var startTime: Long = 0L

    /** 本次暂停开始的墙上时间，只写进草稿，不参与计时。 */
    private var pausedAtMs: Long = 0L

    /** 累计暂停的墙上时长，只写进草稿，不参与计时。 */
    private var totalPausedMs: Long = 0L

    /** 本段计时基准（elapsedRealtime），改系统时间不会影响。 */
    private var startElapsedMs: Long = 0L

    /** 本次暂停开始的 elapsed 时刻；0 表示当前未暂停。 */
    private var pausedAtElapsedMs: Long = 0L

    /** 本段累计暂停的 elapsed 时长。 */
    private var totalPausedElapsedMs: Long = 0L

    /** 草稿恢复时已经累计的时长。 */
    private var baseDurationMs: Long = 0L
    private var skipNextAcceptedPoint = false
    private var totalDistanceMeters = 0.0
    private var trackingWeightKg = 70.0
    private var lastDraftPersistAt = 0L
    private var lastGpsFixAtMs = 0L
    private var lastLocationRestartAtMs = 0L
    private var gpsFixSinceRestart = false
    private var locationLostNotified = false
    private var wakeLock: PowerManager.WakeLock? = null

    private val draftDao: RunDraftDao
        get() = AppDatabase.getInstance(applicationContext).runDraftDao()

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(applicationContext)
        AMapLocationClient.updatePrivacyShow(applicationContext, true, true)
        AMapLocationClient.updatePrivacyAgree(applicationContext, true)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        ensureNotificationChannel()
        markStaleTrackingStateIfNeeded()
        AppLogger.i(LogTags.RUN, "RunTrackingService created")
    }

    /**
     * Service 实例是新建的，但内存状态仍显示正在跑步，说明上一个 Service 实例已被系统销毁，
     * 此时不能相信内存状态（否则"开始跑步"会被 isTracking=true 永久拦截），把它转成可恢复状态。
     */
    private fun markStaleTrackingStateIfNeeded() {
        val current = RunTrackingStateStore.state.value
        if (!current.isTracking) return
        AppLogger.w(
            LogTags.RUN,
            "detected stale tracking state without running service duration=${current.durationSeconds}s distance=${current.distanceKm} points=${current.trackPoints.size}"
        )
        RunTrackingStateStore.update {
            it.copy(
                isTracking = false,
                isPaused = false,
                isServiceRunning = false,
                isSaving = false,
                hasRecoveredDraft = it.trackPoints.isNotEmpty() || it.distanceKm > 0.0,
                gpsStatusText = "跑步记录已中断",
                errorMessage = null
            )
        }
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
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_FINISH -> finishTracking()
            ACTION_DISCARD -> discardTracking()
            ACTION_CONTINUE_DRAFT -> continueDraftTracking()
            ACTION_SAVE_DRAFT -> saveDraftTracking()
            else -> {
                // 系统以 null intent 重启（粘性服务）时不会带来任何跑步状态，直接结束空服务。
                AppLogger.w(LogTags.RUN, "empty restart intent, stopping service action=${intent?.action}")
                markStaleTrackingStateIfNeeded()
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        val current = RunTrackingStateStore.state.value
        AppLogger.i(LogTags.RUN, "RunTrackingService destroying isTracking=${current.isTracking} isSaving=$isSaving")
        if (current.isTracking && !isSaving) {
            persistDraftBlocking()
            RunTrackingStateStore.update {
                it.copy(
                    isTracking = false,
                    isPaused = false,
                    isServiceRunning = false,
                    isSaving = false,
                    hasRecoveredDraft = it.trackPoints.isNotEmpty() || it.distanceKm > 0.0,
                    gpsStatusText = "跑步记录已中断",
                    errorMessage = null
                )
            }
        }
        stopLocationUpdates()
        stopTimer()
        releaseWakeLock()
        AppLogger.i(LogTags.RUN, "RunTrackingService cancelling service scope")
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        AppLogger.w(
            LogTags.RUN,
            "RunTrackingService onTaskRemoved isTracking=${RunTrackingStateStore.state.value.isTracking} " +
                "points=${RunTrackingStateStore.state.value.trackPoints.size}"
        )
        persistDraftSnapshot(force = true)
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
        acquireWakeLock()

        startTime = System.currentTimeMillis()
        pausedAtMs = 0L
        totalPausedMs = 0L
        startElapsedMs = SystemClock.elapsedRealtime()
        pausedAtElapsedMs = 0L
        totalPausedElapsedMs = 0L
        baseDurationMs = 0L
        skipNextAcceptedPoint = false
        totalDistanceMeters = 0.0
        trackingWeightKg = 70.0
        gpsTrackFilter.reset()
        resetLocationHealthWatch()
        RunTrackingStateStore.update {
            RunTrackingUiState(
                isTracking = true,
                isPaused = false,
                hasLocationPermission = true,
                hasNotificationPermission = it.hasNotificationPermission,
                isServiceRunning = true,
                gpsStatusText = "正在校准起点"
            )
        }
        loadTrackingWeight()
        resetDraftForNewRun()
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
            clearDraftAsync()
            stopForegroundAndSelf()
        }
    }

    private fun pauseTracking() {
        val current = RunTrackingStateStore.state.value
        AppLogger.i(
            LogTags.RUN,
            "pauseTracking requested isTracking=${current.isTracking} isPaused=${current.isPaused} duration=${current.durationSeconds}s distance=${current.distanceKm} points=${current.trackPoints.size}"
        )
        if (!current.isTracking || current.isPaused || isSaving) return

        pausedAtMs = System.currentTimeMillis()
        pausedAtElapsedMs = SystemClock.elapsedRealtime()
        // 暂停即停定位：不再每秒回调、也不再耗电；恢复时重新校准起点（skipNextAcceptedPoint）
        stopLocationUpdates()
        RunTrackingStateStore.update {
            it.copy(
                isPaused = true,
                gpsStatusText = "跑步已暂停",
                errorMessage = null
            )
        }
        releaseWakeLock()
        persistDraftSnapshot(force = true)
        updateNotification()
        AppLogger.i(LogTags.RUN, "pauseTracking applied pausedAtMs=$pausedAtMs")
    }

    private fun resumeTracking() {
        val current = RunTrackingStateStore.state.value
        AppLogger.i(
            LogTags.RUN,
            "resumeTracking requested isTracking=${current.isTracking} isPaused=${current.isPaused} duration=${current.durationSeconds}s distance=${current.distanceKm} points=${current.trackPoints.size}"
        )
        if (!current.isTracking || !current.isPaused || isSaving) return

        val now = System.currentTimeMillis()
        if (pausedAtMs > 0L) {
            totalPausedMs += (now - pausedAtMs).coerceAtLeast(0L)
        }
        pausedAtMs = 0L
        val elapsedNow = SystemClock.elapsedRealtime()
        val pauseElapsedMs = if (pausedAtElapsedMs > 0L) {
            (elapsedNow - pausedAtElapsedMs).coerceAtLeast(0L)
        } else {
            0L
        }
        totalPausedElapsedMs += pauseElapsedMs
        pausedAtElapsedMs = 0L
        skipNextAcceptedPoint = true
        gpsTrackFilter.reset()
        resetLocationHealthWatch()
        acquireWakeLock()
        RunTrackingStateStore.update {
            it.copy(
                isPaused = false,
                gpsStatusText = "重新定位中",
                errorMessage = null
            )
        }
        // 暂停期间定位是关掉的，恢复时必须重启客户端；失败则回滚成"仍然暂停"，
        // 避免用户以为在跑、实际没有定位、时长却继续累加。
        val restartError = runCatching { startAmapLocationUpdates() }.exceptionOrNull()
        if (restartError != null) {
            AppLogger.e(LogTags.GPS, "resume AMap location failed", restartError)
            totalPausedElapsedMs = (totalPausedElapsedMs - pauseElapsedMs).coerceAtLeast(0L)
            pausedAtElapsedMs = elapsedNow
            pausedAtMs = now
            RunTrackingStateStore.update {
                it.copy(
                    isPaused = true,
                    gpsStatusText = "定位恢复失败",
                    errorMessage = "定位恢复失败，请重试或结束本次跑步"
                )
            }
            updateNotification()
            return
        }
        persistDraftSnapshot(force = true)
        updateNotification()
        AppLogger.i(LogTags.RUN, "resumeTracking applied totalPausedMs=$totalPausedMs skipNextAcceptedPoint=true")
    }

    private fun finishTracking() {
        if (isSaving) return
        val current = RunTrackingStateStore.state.value
        AppLogger.i(LogTags.RUN, "finishTracking requested canSave=${current.canSave} duration=${current.durationSeconds}s distance=${current.distanceKm} points=${current.trackPoints.size}")
        if (!current.canSave || startTime <= 0L) {
            val message = "本次跑步数据太少，无法保存"
            RunTrackingStateStore.update {
                it.copy(errorMessage = message)
            }
            RunTrackingStateStore.publishSaveResult(RunSaveResult.Failure(message))
            return
        }

        isSaving = true
        stopLocationUpdates()
        stopTimer()
        releaseWakeLock()
        RunTrackingStateStore.update { it.copy(isSaving = true) }

        serviceScope.launch {
            try {
                val sessionId = saveRun(current)
                isSaving = false
                startTime = 0L
                withContext(Dispatchers.IO) { runCatching { clearDraft() } }
                RunTrackingStateStore.update { it.copy(isSaving = false) }
                RunTrackingStateStore.publishSaveResult(RunSaveResult.Success(sessionId))
                RunTrackingStateStore.reset()
                stopForegroundAndSelf()
            } catch (error: Throwable) {
                AppLogger.e(LogTags.DATABASE, "save run failed", error)
                isSaving = false
                val message = "保存失败，请重试"
                RunTrackingStateStore.update {
                    it.copy(isSaving = false, errorMessage = message)
                }
                RunTrackingStateStore.publishSaveResult(RunSaveResult.Failure(message))
            }
        }
    }

    private fun discardTracking() {
        AppLogger.i(LogTags.RUN, "discardTracking points=${RunTrackingStateStore.state.value.trackPoints.size}")
        stopLocationUpdates()
        stopTimer()
        isSaving = false
        startTime = 0L
        pausedAtMs = 0L
        totalPausedMs = 0L
        startElapsedMs = 0L
        pausedAtElapsedMs = 0L
        totalPausedElapsedMs = 0L
        baseDurationMs = 0L
        skipNextAcceptedPoint = false
        totalDistanceMeters = 0.0
        trackingWeightKg = 70.0
        gpsTrackFilter.reset()
        clearDraftAsync()
        RunTrackingStateStore.reset()
        stopForegroundAndSelf()
    }

    private fun continueDraftTracking() {
        if (RunTrackingStateStore.state.value.isTracking) return
        if (!hasPreciseLocationPermission()) {
            AppLogger.w(LogTags.RUN, "continue draft blocked: missing precise location permission")
            RunTrackingStateStore.update {
                it.copy(
                    hasLocationPermission = false,
                    errorMessage = "需要精确位置权限才能准确记录运动轨迹"
                )
            }
            stopSelf()
            return
        }

        startRunForeground()
        acquireWakeLock()
        serviceScope.launch {
            val dao = draftDao
            val draft = withContext(Dispatchers.IO) { dao.getDraft() }
            val points = withContext(Dispatchers.IO) { dao.getPoints() }
            if (draft == null || draft.startTime <= 0L) {
                AppLogger.w(LogTags.RUN, "continue draft blocked: no persisted draft")
                RunTrackingStateStore.update {
                    it.copy(
                        hasRecoveredDraft = false,
                        errorMessage = "没有可恢复的跑步记录"
                    )
                }
                stopForegroundAndSelf()
                return@launch
            }

            startTime = draft.startTime
            // 计时以草稿里已记录的时长重新起算：保存到恢复之间的空档天然不计入，
            // 也不再依赖墙上时钟，恢复后改系统时间同样不影响。
            baseDurationMs = draft.durationSeconds * 1000L
            startElapsedMs = SystemClock.elapsedRealtime()
            pausedAtMs = 0L
            totalPausedMs = 0L
            pausedAtElapsedMs = 0L
            totalPausedElapsedMs = 0L
            skipNextAcceptedPoint = true
            totalDistanceMeters = draft.totalDistanceMeters
            trackingWeightKg = draft.weightKg
            lastDraftPersistAt = 0L
            gpsTrackFilter.reset()
            resetLocationHealthWatch()

            val durationSeconds = activeDurationSeconds()
            val distanceKm = totalDistanceMeters / 1000.0
            RunTrackingStateStore.update {
                RunTrackingUiState(
                    isTracking = true,
                    isPaused = false,
                    hasLocationPermission = true,
                    hasNotificationPermission = it.hasNotificationPermission,
                    isServiceRunning = true,
                    gpsStatusText = "重新定位中",
                    distanceKm = distanceKm,
                    durationSeconds = durationSeconds,
                    averagePaceText = formatPace(durationSeconds, distanceKm),
                    caloriesKcal = calculateRunCalories(durationSeconds, distanceKm),
                    canSave = durationSeconds > 0L && distanceKm > 0.0 && points.size >= 2,
                    trackPoints = points.map { point -> point.toRunTrackPointUiModel() }
                )
            }
            AppLogger.i(
                LogTags.RUN,
                "draft restored startTime=$startTime duration=${durationSeconds}s distance=${distanceKm}km points=${points.size}"
            )
            updateNotification()
            startTimer()
            try {
                startAmapLocationUpdates()
            } catch (error: Throwable) {
                AppLogger.e(LogTags.GPS, "AMap location updates failed on draft restore", error)
                RunTrackingStateStore.update {
                    it.copy(
                        isTracking = false,
                        isPaused = false,
                        isServiceRunning = false,
                        hasLocationPermission = false,
                        hasRecoveredDraft = it.trackPoints.isNotEmpty() || it.distanceKm > 0.0,
                        gpsStatusText = "跑步记录已中断",
                        errorMessage = "需要精确位置权限才能准确记录运动轨迹"
                    )
                }
                stopForegroundAndSelf()
                return@launch
            }
            persistDraftSnapshot(force = true)
        }
    }

    private fun saveDraftTracking() {
        if (isSaving) return
        isSaving = true
        RunTrackingStateStore.update { it.copy(isSaving = true) }
        serviceScope.launch {
            try {
                val dao = draftDao
                val draft = withContext(Dispatchers.IO) { dao.getDraft() }
                val points = withContext(Dispatchers.IO) { dao.getPoints() }
                if (draft == null || draft.startTime <= 0L || draft.durationSeconds <= 0L ||
                    draft.totalDistanceMeters <= 0.0 || points.size < 2
                ) {
                    val message = "本次跑步数据太少，无法保存"
                    AppLogger.w(LogTags.RUN, "save draft blocked: insufficient data")
                    isSaving = false
                    RunTrackingStateStore.update { it.copy(isSaving = false, errorMessage = message) }
                    RunTrackingStateStore.publishSaveResult(RunSaveResult.Failure(message))
                    return@launch
                }

                val database = AppDatabase.getInstance(applicationContext)
                val weightKg = withContext(Dispatchers.IO) {
                    database.weightDao().getLatestWeightOnOrBefore(todayIsoDate())?.weightKg
                } ?: draft.weightKg
                val distanceKm = draft.totalDistanceMeters / 1000.0
                val caloriesKcal = estimateExerciseCalories(
                    weightKg = weightKg,
                    type = ExerciseType.OUTDOOR_RUNNING,
                    durationSeconds = draft.durationSeconds,
                    distanceKm = distanceKm,
                    inclinePercent = 0.0
                )
                val endTime = maxOf(draft.updatedAt, points.last().recordedAt)
                val sessionId = saveRunData(
                    startTime = draft.startTime,
                    endTime = endTime,
                    durationSeconds = draft.durationSeconds,
                    distanceKm = distanceKm,
                    caloriesKcal = caloriesKcal,
                    points = points.map { it.toRunTrackPointUiModel().toEntity(0L) }
                )
                withContext(Dispatchers.IO) { runCatching { clearDraft() } }
                isSaving = false
                RunTrackingStateStore.publishSaveResult(RunSaveResult.Success(sessionId))
                RunTrackingStateStore.reset()
                stopForegroundAndSelf()
            } catch (error: Throwable) {
                AppLogger.e(LogTags.DATABASE, "save recovered draft failed", error)
                isSaving = false
                val message = "保存失败，请重试"
                RunTrackingStateStore.update { it.copy(isSaving = false, errorMessage = message) }
                RunTrackingStateStore.publishSaveResult(RunSaveResult.Failure(message))
            }
        }
    }

    private fun resetDraftForNewRun() {
        val snapshot = buildDraftSnapshot(System.currentTimeMillis()) ?: return
        serviceScope.launch(Dispatchers.IO) {
            runCatching {
                clearDraft()
                draftDao.upsertDraft(snapshot)
            }.onFailure { AppLogger.e(LogTags.DATABASE, "reset run draft failed", it) }
        }
    }

    private fun appendDraftPoint(point: RunTrackPointUiModel) {
        serviceScope.launch(Dispatchers.IO) {
            runCatching { draftDao.insertPoint(point.toDraftEntity()) }
                .onFailure { AppLogger.e(LogTags.DATABASE, "persist draft point failed", it) }
        }
    }

    private fun persistDraftSnapshot(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastDraftPersistAt < DraftPersistIntervalMs) return
        val snapshot = buildDraftSnapshot(now) ?: return
        lastDraftPersistAt = now
        serviceScope.launch(Dispatchers.IO) {
            runCatching { draftDao.upsertDraft(snapshot) }
                .onFailure { AppLogger.e(LogTags.DATABASE, "persist run draft failed", it) }
        }
    }

    private fun buildDraftSnapshot(now: Long): RunDraftEntity? {
        val current = RunTrackingStateStore.state.value
        if (!current.isTracking || startTime <= 0L) return null
        return RunDraftEntity(
            startTime = startTime,
            pausedAtMs = pausedAtMs,
            totalPausedMs = totalPausedMs,
            durationSeconds = current.durationSeconds,
            totalDistanceMeters = totalDistanceMeters,
            caloriesKcal = current.caloriesKcal,
            weightKg = trackingWeightKg,
            updatedAt = now
        )
    }

    private fun persistDraftBlocking() {
        val snapshot = buildDraftSnapshot(System.currentTimeMillis()) ?: return
        runCatching {
            runBlocking(Dispatchers.IO) { draftDao.upsertDraft(snapshot) }
        }.onFailure { AppLogger.e(LogTags.DATABASE, "persist run draft on destroy failed", it) }
    }

    private fun clearDraftAsync() {
        serviceScope.launch(Dispatchers.IO) {
            runCatching { clearDraft() }
                .onFailure { AppLogger.e(LogTags.DATABASE, "clear run draft failed", it) }
        }
    }

    private suspend fun clearDraft() {
        draftDao.clearPoints()
        draftDao.clearDraft()
    }

    private suspend fun saveRun(current: RunTrackingUiState): Long = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(applicationContext)
        // 用"起点 + 计时时长"算结束时间：墙上时钟被改过也不会出现 endTime < startTime
        val endTime = startTime + current.durationSeconds * 1000L
        val recordDate = todayIsoDate()
        val weightKg = database.weightDao().getLatestWeightOnOrBefore(recordDate)?.weightKg ?: 70.0
        val caloriesKcal = estimateExerciseCalories(
            weightKg = weightKg,
            type = ExerciseType.OUTDOOR_RUNNING,
            durationSeconds = current.durationSeconds,
            distanceKm = current.distanceKm,
            inclinePercent = 0.0
        )
        saveRunData(
            startTime = startTime,
            endTime = endTime,
            durationSeconds = current.durationSeconds,
            distanceKm = current.distanceKm,
            caloriesKcal = caloriesKcal,
            points = current.trackPoints.map { it.toEntity(0L) }
        )
    }

    private suspend fun saveRunData(
        startTime: Long,
        endTime: Long,
        durationSeconds: Long,
        distanceKm: Double,
        caloriesKcal: Int,
        points: List<RunTrackPointEntity>
    ): Long = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(applicationContext)
        // 三张表必须一起成功，否则会留下没有会话/轨迹的运动记录。
        database.withTransaction {
            val exerciseRecordId = database.exerciseDao().insertExercise(
                ExerciseRecordEntity(
                    type = ExerciseType.OUTDOOR_RUNNING.name,
                    startTime = startTime,
                    durationSeconds = durationSeconds,
                    distanceKm = distanceKm,
                    caloriesKcal = caloriesKcal
                )
            )
            val newSessionId = database.runDao().insertSession(
                RunSessionEntity(
                    exerciseRecordId = exerciseRecordId,
                    startTime = startTime,
                    endTime = endTime,
                    durationSeconds = durationSeconds,
                    distanceKm = distanceKm,
                    caloriesKcal = caloriesKcal,
                    createdAt = endTime
                )
            )
            // 分批写入：SQLite 单语句变量上限 999，长跑（上万点）一次性构造会占大量内存
            points.chunked(500).forEach { chunk ->
                database.runDao().insertTrackPoints(chunk.map { it.copy(sessionId = newSessionId) })
            }
            AppLogger.i(
                LogTags.DATABASE,
                "run saved sessionId=$newSessionId exerciseRecordId=$exerciseRecordId duration=${durationSeconds}s " +
                    "distance=$distanceKm points=${points.size}"
            )
            newSessionId
        }
    }

    private fun handleLocation(location: Location) {
        AppLogger.d(LogTags.GPS, "raw location ${location.toDebugText()}")
        if (RunTrackingStateStore.state.value.isPaused) {
            AppLogger.d(LogTags.TRACK, "location ignored because run is paused ${location.toDebugText()}")
            return
        }
        when (val result = gpsTrackFilter.filter(location)) {
            is LocationFilterResult.Accepted -> {
                if (skipNextAcceptedPoint) {
                    skipNextAcceptedPoint = false
                    RunTrackingStateStore.update {
                        it.copy(
                            gpsStatusText = result.point.accuracyMeters?.let { accuracy -> "GPS 精度 ${accuracy.toInt()}m" } ?: "GPS 信号良好",
                            errorMessage = null
                        )
                    }
                    AppLogger.i(
                        LogTags.TRACK,
                        "resume baseline accepted and skipped lat=${result.point.latitude} lon=${result.point.longitude} accuracy=${result.point.accuracyMeters}"
                    )
                    return
                }
                totalDistanceMeters += result.distanceFromPreviousMeters
                RunTrackingStateStore.update { current ->
                    current.trackPoints.lastOrNull()?.let { previousPoint ->
                        logAcceptedTrackGapIfNeeded(
                            previousPoint = previousPoint,
                            currentPoint = result.point,
                            acceptedDistanceMeters = result.distanceFromPreviousMeters
                        )
                    }
                    // 持久化列表追加是 O(log32 n)，不会像 list + element 那样每点复制整表；
                    // 状态里仍用 List 类型，调用方（渲染器/保存/预览）无需改动。
                    val points = current.trackPoints.toPersistentList().add(result.point)
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
                appendDraftPoint(result.point)
                persistDraftSnapshot()
            }
            is LocationFilterResult.Pending -> {
                logRejectedLocation(location, result.reason)
                AppLogger.d(LogTags.TRACK, "pending point reason=${result.reason} ${location.toDebugText()}")
                RunTrackingStateStore.update { it.copy(gpsStatusText = gpsPendingStatusText(result.reason)) }
            }
            is LocationFilterResult.Rejected -> {
                logRejectedLocation(location, result.reason)
                AppLogger.d(LogTags.TRACK, "rejected point reason=${result.reason} ${location.toDebugText()}")
                RunTrackingStateStore.update { it.copy(gpsStatusText = gpsRejectStatusText(result.reason)) }
            }
        }
    }

    private fun logAcceptedTrackGapIfNeeded(
        previousPoint: RunTrackPointUiModel,
        currentPoint: RunTrackPointUiModel,
        acceptedDistanceMeters: Float
    ) {
        val deltaMs = deltaTimeMs(previousPoint, currentPoint)
        if (deltaMs < TrackGapLogMinTimeMs && acceptedDistanceMeters < TrackGapLogMinDistanceMeters) return

        val deltaSeconds = deltaMs / 1000f
        val speedMps = if (deltaSeconds > 0f) acceptedDistanceMeters / deltaSeconds else 0f
        AppLogger.w(
            LogTags.TRACK,
            "TRACK_GAP_ACCEPTED deltaTime=${deltaMs}ms deltaDistance=${"%.2f".format(acceptedDistanceMeters)}m " +
                "speed=${"%.2f".format(speedMps)}mps previousLat=${previousPoint.latitude} previousLon=${previousPoint.longitude} " +
                "currentLat=${currentPoint.latitude} currentLon=${currentPoint.longitude} previousAccuracy=${previousPoint.accuracyMeters} " +
                "currentAccuracy=${currentPoint.accuracyMeters} previousSpeed=${previousPoint.speedMetersPerSecond} " +
                "currentSpeed=${currentPoint.speedMetersPerSecond} currentCoord=${currentPoint.coordinateSystem}"
        )
    }

    private fun startTimer() {
        stopTimer()
        timerJob = serviceScope.launch {
            while (true) {
                delay(1_000L)
                monitorLocationHealth()
                RunTrackingStateStore.update { current ->
                    if (current.isPaused) {
                        return@update current
                    }
                    val durationSeconds = activeDurationSeconds()
                    current.copy(
                        durationSeconds = durationSeconds,
                        averagePaceText = formatPace(durationSeconds, current.distanceKm),
                        caloriesKcal = calculateRunCalories(durationSeconds, current.distanceKm),
                        canSave = durationSeconds > 0L && current.distanceKm > 0.0 && current.trackPoints.size >= 2
                    )
                }
                updateNotification()
                persistDraftSnapshot()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun resetLocationHealthWatch() {
        lastGpsFixAtMs = System.currentTimeMillis()
        lastLocationRestartAtMs = 0L
        gpsFixSinceRestart = false
        locationLostNotified = false
    }

    /**
     * AMap 偶尔会长时间（实测约 160s）不再回调任何定位，直到 SDK 自己报 error=4 超时才恢复。
     * 这里用 1 秒定时器做看门狗：先提示用户，再主动重建定位客户端，避免把短暂丢星拖成几分钟的轨迹空洞。
     *
     * 注意：只以 GPS 结果计时。WiFi/基站定位每几秒就来一条，如果拿它续命，GPS 掉星时看门狗永远不会重启客户端
     * （2026-10-30 17:06 那次就是这样，WiFi 点持续到 44s，客户端一次都没重启）。
     */
    private fun monitorLocationHealth() {
        val current = RunTrackingStateStore.state.value
        if (!current.isTracking || current.isPaused) {
            lastGpsFixAtMs = System.currentTimeMillis()
            return
        }
        val now = System.currentTimeMillis()
        val silenceMs = now - lastGpsFixAtMs
        val restartThresholdMs = if (gpsFixSinceRestart) LocationSilenceRestartMs else InitialLocationGraceMs
        when {
            silenceMs >= restartThresholdMs -> {
                if (!locationLostNotified) {
                    locationLostNotified = true
                    AppLogger.w(
                        LogTags.GPS,
                        "no GPS fix for ${silenceMs}ms (fixSinceRestart=$gpsFixSinceRestart), restarting AMap client"
                    )
                }
                RunTrackingStateStore.update {
                    it.copy(gpsStatusText = if (gpsFixSinceRestart) "GPS 信号丢失，正在重新定位" else "GPS 信号弱，正在重新定位")
                }
                restartAmapLocation(reason = "no_gps_${silenceMs}ms")
            }
            silenceMs >= LocationSilenceWarnMs && gpsFixSinceRestart -> {
                if (!locationLostNotified) {
                    locationLostNotified = true
                    AppLogger.w(LogTags.GPS, "no GPS fix for ${silenceMs}ms, waiting for recovery")
                }
                RunTrackingStateStore.update { it.copy(gpsStatusText = "GPS 信号弱，等待定位") }
            }
        }
    }

    private fun restartAmapLocation(reason: String) {
        val current = RunTrackingStateStore.state.value
        if (!current.isTracking || current.isPaused || isSaving) return
        val now = System.currentTimeMillis()
        val sinceLastRestartMs = now - lastLocationRestartAtMs
        if (lastLocationRestartAtMs > 0L && sinceLastRestartMs < LocationRestartCooldownMs) {
            AppLogger.d(LogTags.GPS, "skip AMap restart reason=$reason cooldownRemaining=${LocationRestartCooldownMs - sinceLastRestartMs}ms")
            return
        }
        lastLocationRestartAtMs = now
        lastGpsFixAtMs = now
        gpsFixSinceRestart = false
        gpsTrackFilter.beginRecovery()
        AppLogger.w(LogTags.GPS, "restarting AMap location client reason=$reason")
        runCatching { startAmapLocationUpdates() }
            .onFailure { error ->
                AppLogger.e(LogTags.GPS, "restart AMap location client failed reason=$reason", error)
            }
    }

    private fun markGpsFixReceived() {
        val now = System.currentTimeMillis()
        if (locationLostNotified) {
            locationLostNotified = false
            AppLogger.i(LogTags.GPS, "GPS fix recovered silence=${now - lastGpsFixAtMs}ms")
        }
        lastGpsFixAtMs = now
        gpsFixSinceRestart = true
    }

    /**
     * 只按单调时钟计时：墙上时钟只决定 startTime（落库用），
     * 用户改系统时间 / NTP 校时 / 跨时区都不会让时长跳变。
     */
    private fun activeDurationSeconds(): Long = computeActiveDurationSeconds(
        baseDurationMs = baseDurationMs,
        startElapsedMs = startElapsedMs,
        totalPausedElapsedMs = totalPausedElapsedMs,
        pausedAtElapsedMs = pausedAtElapsedMs,
        nowElapsedMs = SystemClock.elapsedRealtime()
    )

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
            if (location.errorCode == AmapErrorCodeConnectionFailure) {
                RunTrackingStateStore.update { it.copy(gpsStatusText = "GPS 定位异常，正在重新定位") }
                serviceScope.launch {
                    delay(LocationErrorRestartDelayMs)
                    restartAmapLocation(reason = "amap_error_${location.errorCode}")
                }
            } else {
                RunTrackingStateStore.update { it.copy(gpsStatusText = "GPS 定位中") }
            }
            return
        }
        val isGpsLocation = location.locationType == AMapLocation.LOCATION_TYPE_GPS
        if (isGpsLocation) {
            markGpsFixReceived()
        }
        publishMapLocation(location)
        handleLocation(location)
    }

    /**
     * 把定位结果发布到共享状态，地图页据此更新原生定位蓝点，避免地图端再开一个定位客户端。
     * 坐标已是 GCJ-02（客户端设置了 setOffset(true)），可直接用于高德地图。
     * 网络定位只有精度足够好时才更新蓝点，否则宁可让蓝点停在最后一次 GPS 位置，也不要在图上跳 100 多米。
     */
    private fun publishMapLocation(location: AMapLocation) {
        if (location.accuracy <= 0f || location.accuracy > RunMapMaxDisplayAccuracyMeters) return
        val isGpsLocation = location.locationType == AMapLocation.LOCATION_TYPE_GPS
        if (!isGpsLocation && location.accuracy > NonGpsMapDisplayMaxAccuracyMeters) {
            AppLogger.d(
                LogTags.GPS,
                "skip non-gps map location type=${location.locationType} accuracy=${location.accuracy}"
            )
            return
        }
        RunTrackingStateStore.update {
            it.copy(
                mapLocation = RunMapLocationSnapshot(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy,
                    isRoadLevelReliable = location.accuracy <= RunMapRoadLevelAccuracyMeters &&
                        isGpsLocation &&
                        location.gpsAccuracyStatus != AMapLocation.GPS_ACCURACY_BAD,
                    isGpsLocation = isGpsLocation,
                    recordedAt = location.time
                )
            )
        }
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
        val title = if (state.isPaused) "MyRun 跑步已暂停" else "MyRun 正在记录跑步"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_run)
            .setContentTitle(title)
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
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    /**
     * 前台服务本身不会阻止 CPU 休眠。屏幕关闭后 CPU 挂起会同时冻结定位回调和 1s 计时器，
     * 表现为轨迹长时间中断（日志里一段时间内 GPS、地图、watchdog 全部静默）。
     * 因此记录期间持有 PARTIAL_WAKE_LOCK，暂停/结束/销毁时释放。
     */
    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        val existing = wakeLock
        if (existing?.isHeld == true) return
        val lock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .apply {
                setReferenceCounted(false)
                acquire()
            }
        wakeLock = lock
        AppLogger.i(LogTags.RUN, "wake lock acquired tag=$WAKE_LOCK_TAG")
    }

    private fun releaseWakeLock() {
        val lock = wakeLock ?: return
        wakeLock = null
        if (lock.isHeld) {
            lock.release()
            AppLogger.i(LogTags.RUN, "wake lock released")
        }
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
            LocationRejectReason.START_ANCHOR_CALIBRATING,
            LocationRejectReason.START_ANCHOR_UNSTABLE -> "正在校准起点"
            LocationRejectReason.EARLY_TRACK_GUARD -> "已过滤起步异常跳点"
            LocationRejectReason.RECOVERY_CALIBRATING,
            LocationRejectReason.RECOVERY_GUARD -> "已过滤恢复期异常点"
            LocationRejectReason.IMPOSSIBLE_SPEED,
            LocationRejectReason.LARGE_JUMP,
            LocationRejectReason.SUSPECT_SPIKE -> "已过滤异常跳点"
            LocationRejectReason.MOCK_LOCATION -> "已忽略模拟定位"
            LocationRejectReason.NON_GPS_LOCATION -> "已忽略网络定位，等待 GPS"
            LocationRejectReason.INVALID_TIME -> "已忽略异常定位时间"
        }
    }

    private fun gpsPendingStatusText(reason: LocationRejectReason): String {
        return when (reason) {
            LocationRejectReason.START_ANCHOR_CALIBRATING -> "正在校准起点"
            LocationRejectReason.START_ANCHOR_UNSTABLE -> "起点定位不稳定"
            LocationRejectReason.EARLY_TRACK_GUARD -> "正在确认起步轨迹"
            LocationRejectReason.RECOVERY_CALIBRATING -> "正在重新校准定位"
            LocationRejectReason.RECOVERY_GUARD -> "正在确认 GPS 连续性"
            else -> "正在确认 GPS 连续性"
        }
    }

    private fun logRejectedLocation(location: Location, reason: LocationRejectReason) {
        AppLogger.d(
            LogTags.GPS,
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
        private const val WAKE_LOCK_TAG = "MyRunApp:RunTracking"
        private const val ACTION_START = "com.example.myrunapp.feature.run.START"
        private const val ACTION_PAUSE = "com.example.myrunapp.feature.run.PAUSE"
        private const val ACTION_RESUME = "com.example.myrunapp.feature.run.RESUME"
        private const val ACTION_FINISH = "com.example.myrunapp.feature.run.FINISH"
        private const val ACTION_DISCARD = "com.example.myrunapp.feature.run.DISCARD"
        private const val ACTION_CONTINUE_DRAFT = "com.example.myrunapp.feature.run.CONTINUE_DRAFT"
        private const val ACTION_SAVE_DRAFT = "com.example.myrunapp.feature.run.SAVE_DRAFT"

        fun startIntent(context: Context): Intent {
            return Intent(context, RunTrackingService::class.java).setAction(ACTION_START)
        }

        fun finishIntent(context: Context): Intent {
            return Intent(context, RunTrackingService::class.java).setAction(ACTION_FINISH)
        }

        fun pauseIntent(context: Context): Intent {
            return Intent(context, RunTrackingService::class.java).setAction(ACTION_PAUSE)
        }

        fun resumeIntent(context: Context): Intent {
            return Intent(context, RunTrackingService::class.java).setAction(ACTION_RESUME)
        }

        fun discardIntent(context: Context): Intent {
            return Intent(context, RunTrackingService::class.java).setAction(ACTION_DISCARD)
        }

        fun continueDraftIntent(context: Context): Intent {
            return Intent(context, RunTrackingService::class.java).setAction(ACTION_CONTINUE_DRAFT)
        }

        fun saveDraftIntent(context: Context): Intent {
            return Intent(context, RunTrackingService::class.java).setAction(ACTION_SAVE_DRAFT)
        }
    }
}
