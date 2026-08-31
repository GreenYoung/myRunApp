# GPS 跑步记录第一版无地图 SDK 实现提示词

请基于当前 Android 项目，实现第一版「前台 GPS 跑步记录 + 结束后轨迹展示」功能。

本版本明确不接入高德地图 SDK，不接入其他第三方地图 SDK。轨迹详情页先使用 Compose Canvas 自绘简化轨迹线，后续再单独接入高德地图。

## 1. 第一版目标

本次只实现：

```text
首页开始运动入口
↓
GPS 跑步记录页
↓
前台 GPS 记录
↓
点击结束
↓
保存运动记录 + 保存轨迹点
↓
轨迹详情页展示简化轨迹
```

第一版不实现：

```text
高德地图 SDK
百度地图 SDK
Google Maps
锁屏后台记录
前台服务通知
暂停 / 继续
语音播报
轨迹纠偏
实时地图
公里提醒
分享功能
```

## 2. 当前项目背景

当前项目已有：

```text
HomeScreen
StartExerciseCard
ExerciseDetailScreen
ExerciseRecordEntity
ExerciseDao
ExerciseViewModel
运动记录新增 / 编辑 / 删除
运动统计
首页运动卡片
Room + Flow
Jetpack Compose + Material 3
```

首页已经有「开始运动」卡片。

本次需要让首页开始运动入口进入新的 GPS 跑步记录流程。

## 3. 首页入口规则

点击首页开始运动卡片中的：

```text
RUN
户外跑步
```

进入：

```text
RunTrackingScreen
```

点击：

```text
跑步机
自由跑
```

第一版可以继续进入运动详情页或保留当前行为，不强行接 GPS。

不要改动已有手动新增运动记录弹窗。

## 4. 新增页面

建议新增：

```text
RunTrackingScreen
RunTrackDetailScreen
```

### 4.1 RunTrackingScreen

用于前台 GPS 跑步记录。

页面结构建议：

```text
┌──────────────────────────────┐
│ ◯‹          户外跑步          │
│                              │
│          0.00 km             │
│          当前距离             │
│                              │
│   00:00:00       --'--"/km   │
│    运动时长        平均配速    │
│                              │
│        等待 GPS 定位          │
│                              │
│        [ 开始跑步 ]           │
└──────────────────────────────┘
```

记录中：

```text
┌──────────────────────────────┐
│ ◯‹          户外跑步          │
│                              │
│          3.26 km             │
│          当前距离             │
│                              │
│   00:21:36       6'37"/km    │
│    运动时长        平均配速    │
│                              │
│        GPS 信号良好           │
│                              │
│        [ 结束跑步 ]           │
└──────────────────────────────┘
```

要求：

- 深色运动风。
- 当前距离是页面最大视觉元素。
- 开始按钮使用绿色主按钮。
- 结束按钮使用统一危险按钮。
- 返回按钮复用现有 `AppPageTopBar` / `AppBackButton`。
- 记录中点击返回时需要提示是否放弃本次记录。

### 4.2 RunTrackDetailScreen

用于展示结束后的跑步轨迹。

第一版不接地图 SDK，使用 Compose Canvas 自绘轨迹。

页面结构建议：

```text
┌──────────────────────────────┐
│ ◯‹          跑步轨迹          │
│                              │
│ ┌──────────────────────────┐ │
│ │                          │ │
│ │      Canvas 简化轨迹线     │ │
│ │   起点 ●          ● 终点   │ │
│ │                          │ │
│ └──────────────────────────┘ │
│                              │
│  5.20 km     33min           │
│  6'23"/km    356 kcal        │
└──────────────────────────────┘
```

Canvas 绘制要求：

- 根据经纬度范围归一化到绘制区域。
- 绘制绿色轨迹线。
- 绘制起点和终点标记。
- 没有轨迹点时展示空状态。
- 不显示真实地图底图。
- 明确这是轨迹预览，不是地图导航。

## 5. 定位权限

第一版只申请前台定位权限：

```text
ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION
```

不要申请：

```text
ACCESS_BACKGROUND_LOCATION
FOREGROUND_SERVICE_LOCATION
```

权限流程：

```text
进入 RunTrackingScreen
↓
检查定位权限
↓
未授权则请求权限
↓
授权后允许开始跑步
↓
拒绝后展示权限提示
```

拒绝权限提示：

```text
需要定位权限才能记录户外跑步轨迹
```

## 6. GPS 采集规则

第一版只做前台采集：

- 页面可见时采集定位。
- 用户点击结束后停止采集。
- 用户离开页面并确认放弃后停止采集。
- 不保证锁屏或切后台持续记录。

建议采集参数：

```text
定位间隔：1～3 秒
最小距离变化：3～5 米
有效精度：accuracy <= 30 米
```

如果使用 Android 原生 `LocationManager`：

- 优先使用 GPS_PROVIDER。
- 必要时兼容 NETWORK_PROVIDER。

如果项目已有定位封装，优先复用。

## 7. 距离计算规则

每次收到新定位点：

```text
1. 判断定位精度
2. 判断是否为第一个有效点
3. 计算与上一个有效点之间的距离
4. 过滤明显漂移点
5. 累加总距离
6. 保存到内存轨迹列表
```

基础过滤规则：

```text
accuracy > 30m：丢弃
两点时间差 <= 0：丢弃
瞬时速度 > 8 m/s：第一版可判定为异常点并丢弃
```

说明：

- 8 m/s 约等于 28.8 km/h，对普通跑步足够。
- 后续可根据实际使用调整。

距离计算可以使用：

```kotlin
Location.distanceBetween(...)
```

或等价的 Haversine 计算。

## 8. 运动数据计算

实时展示：

```text
距离 distanceKm
时长 durationSeconds
平均配速 averagePace
GPS 状态 gpsStatusText
```

平均配速：

```text
durationSeconds / distanceKm
```

距离为 0 时：

```text
--'--"/km
```

展示格式复用当前项目已有：

```text
formatDistance
formatDuration
formatPace
formatCalories
```

热量估算复用当前已有逻辑：

```text
estimateExerciseCalories(...)
```

## 9. 数据库设计

需要新增跑步会话和轨迹点存储。

建议新增包：

```text
feature/run/data
```

### 9.1 RunSessionEntity

```kotlin
@Entity(tableName = "run_sessions")
data class RunSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseRecordId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceKm: Double,
    val caloriesKcal: Int,
    val createdAt: Long
)
```

### 9.2 RunTrackPointEntity

```kotlin
@Entity(tableName = "run_track_points")
data class RunTrackPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val altitudeMeters: Double?,
    val speedMetersPerSecond: Float?,
    val recordedAt: Long
)
```

### 9.3 RunDao

建议提供：

```kotlin
@Insert
suspend fun insertSession(session: RunSessionEntity): Long

@Insert
suspend fun insertTrackPoints(points: List<RunTrackPointEntity>)

@Query("SELECT * FROM run_sessions WHERE id = :sessionId")
fun observeSession(sessionId: Long): Flow<RunSessionEntity?>

@Query("SELECT * FROM run_track_points WHERE sessionId = :sessionId ORDER BY recordedAt ASC")
fun observeTrackPoints(sessionId: Long): Flow<List<RunTrackPointEntity>>
```

## 10. 保存规则

点击结束后：

```text
1. 停止定位
2. 计算最终距离、时长、配速、热量
3. 插入 ExerciseRecordEntity
4. 插入 RunSessionEntity
5. 插入 RunTrackPointEntity 列表
6. 保存成功后进入 RunTrackDetailScreen
```

ExerciseRecordEntity 映射：

```text
type = OUTDOOR_RUNNING
startTime = startTime
durationSeconds = durationSeconds
distanceKm = distanceKm
caloriesKcal = caloriesKcal
```

保存校验：

```text
durationSeconds > 0
distanceKm > 0
有效轨迹点 >= 2
```

如果不满足：

```text
提示：本次跑步数据太少，无法保存
```

## 11. 导航建议

如果当前项目仍使用简单枚举导航，可新增：

```text
RunTracking
RunTrackDetail
```

并在 `MainActivity` 中保存：

```kotlin
selectedRunSessionId: Long?
```

流程：

```text
HomeScreen
↓
RunTrackingScreen
↓ 保存成功回调 sessionId
RunTrackDetailScreen(sessionId)
```

返回规则：

- RunTrackingScreen 未开始时返回首页。
- RunTrackingScreen 记录中返回，需要确认放弃。
- RunTrackDetailScreen 返回运动详情页或首页，第一版可返回运动详情页。

## 12. 架构建议

建议新增：

```text
feature/run/
 ├── RunTrackingScreen.kt
 ├── RunTrackDetailScreen.kt
 ├── RunTrackingViewModel.kt
 ├── RunModels.kt
 ├── RunLogic.kt
 └── data/
     ├── RunDao.kt
     ├── RunSessionEntity.kt
     └── RunTrackPointEntity.kt
```

ViewModel 负责：

- 权限状态。
- GPS 采集状态。
- 跑步计时。
- 轨迹点内存管理。
- 距离和配速计算。
- 保存运动记录和轨迹。

Composable 只负责渲染和触发事件。

## 13. UI State 建议

```kotlin
data class RunTrackingUiState(
    val isTracking: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val gpsStatusText: String = "等待定位",
    val distanceKm: Double = 0.0,
    val durationSeconds: Long = 0L,
    val averagePaceText: String = "--'--\"/km",
    val canSave: Boolean = false,
    val errorMessage: String? = null
)
```

```kotlin
data class RunTrackPointUiModel(
    val latitude: Double,
    val longitude: Double,
    val recordedAt: Long
)
```

```kotlin
data class RunTrackDetailUiState(
    val sessionId: Long,
    val distanceKm: Double,
    val durationSeconds: Long,
    val paceText: String,
    val caloriesKcal: Int,
    val points: List<RunTrackPointUiModel>
)
```

## 14. AndroidManifest

需要增加：

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

不要添加后台定位权限。

## 15. 测试与验证

编译验证：

```text
testDebugUnitTest
```

真机验证重点：

- 首次进入跑步页会请求定位权限。
- 拒绝权限时不会崩溃。
- 授权后可以开始跑步。
- GPS 状态能更新。
- 距离、时长、配速能实时变化。
- 点击结束能停止定位。
- 跑步记录能保存到运动记录列表。
- 轨迹详情页能显示简化轨迹。
- 返回页面不会继续采集定位。

## 16. 验收标准

第一版完成后必须满足：

- 首页 RUN / 户外跑步可以进入 GPS 跑步记录页。
- 只做前台 GPS 记录。
- 不接入高德地图 SDK。
- 不申请后台定位权限。
- 跑步中展示距离、时长、平均配速。
- 点击结束停止跑步。
- 结束后保存运动记录。
- 结束后保存轨迹点。
- 结束后进入轨迹详情页。
- 轨迹详情页使用 Canvas 显示简化轨迹线。
- 原有手动运动记录功能不受影响。
- 原有首页运动卡片统计可以自动包含 GPS 跑步生成的运动记录。

## 17. 明确后续再做

以下内容后续单独实现：

```text
高德地图 SDK 真实地图
后台持续跑步记录
前台服务通知
暂停 / 继续
轨迹纠偏
实时地图跟随
历史轨迹列表入口
```
