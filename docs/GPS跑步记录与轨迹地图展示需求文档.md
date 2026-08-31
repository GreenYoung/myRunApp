# GPS 跑步记录与轨迹地图展示需求文档

## 1. 需求背景

当前 MyRunApp 已经具备：

```text
首页开始运动入口
运动详情页
运动记录新增 / 编辑 / 删除
运动目标设置
运动统计维度筛选
体重记录与体重趋势
```

目前运动记录主要依赖用户手动录入运动日期、距离、时长等数据。

后续希望在首页「开始运动」入口基础上，支持：

```text
前台 GPS 跑步记录
结束后轨迹地图展示
轨迹点与运动记录信息持久化
```

第一版目标是打通完整主流程，不做复杂后台能力。

## 2. 需求目标

用户点击首页「开始运动」后，可以进入 GPS 跑步记录页面。

用户在页面内点击开始后，APP 使用 GPS 持续记录跑步轨迹，并实时展示核心运动数据。

用户点击结束后，APP 停止 GPS 记录，并保存：

```text
运动记录信息
轨迹点数据
跑步会话信息
```

保存完成后，可以进入轨迹地图详情页查看本次跑步路线。

## 3. 第一版实现范围

第一版只实现：

```text
前台 GPS 跑步记录
跑步开始 / 结束
实时距离 / 时长 / 配速展示
结束后保存运动记录
保存 GPS 轨迹点
轨迹地图详情页展示路线
```

第一版不实现：

```text
锁屏后台持续记录
前台服务常驻通知
暂停 / 继续
语音播报
自动识别跑步状态
配速提醒
复杂轨迹纠偏
离线地图
社交分享
轨迹导出
```

## 4. 用户流程

### 4.1 从首页进入

入口：

```text
首页
↓
开始运动卡片
↓
点击 RUN / 户外跑步
↓
进入 GPS 跑步记录页
```

建议第一版：

```text
RUN
户外跑步
```

进入 GPS 跑步记录页。

```text
跑步机
自由跑
```

可以暂时继续进入运动详情页或打开手动记录入口，避免误导为 GPS 跑步。

### 4.2 跑步记录中

页面展示：

```text
当前距离
运动时长
平均配速
当前 GPS 状态
开始 / 结束按钮
```

流程：

```text
进入页面
↓
检查定位权限
↓
用户授权
↓
点击开始
↓
持续采集 GPS 点
↓
实时计算距离、时长、配速
↓
点击结束
↓
停止定位
↓
保存运动记录和轨迹
↓
进入轨迹详情页
```

### 4.3 查看轨迹地图

入口：

```text
跑步结束后自动进入
或
运动记录详情中点击有轨迹的记录
```

页面展示：

```text
地图轨迹
起点
终点
距离
时长
平均配速
消耗热量
日期
```

## 5. 页面设计

### 5.1 GPS 跑步记录页

页面建议：

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
│        GPS 信号良好           │
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

视觉要求：

- 深色运动风。
- 当前距离是页面最大视觉元素。
- 开始按钮使用绿色主按钮。
- 结束按钮使用危险色或深色危险按钮。
- GPS 状态使用轻量文本，不要抢主视觉。
- 顶部返回按钮复用现有统一返回按钮。

### 5.2 轨迹地图详情页

页面建议：

```text
┌──────────────────────────────┐
│ ◯‹          跑步轨迹          │
│                              │
│ ┌──────────────────────────┐ │
│ │                          │ │
│ │        地图 + 轨迹线       │ │
│ │                          │ │
│ └──────────────────────────┘ │
│                              │
│  5.20 km     33min           │
│  6'23"/km    356 kcal        │
└──────────────────────────────┘
```

地图要求：

- 展示跑步轨迹 Polyline。
- 展示起点 Marker。
- 展示终点 Marker。
- 自动缩放到完整轨迹范围。
- 没有轨迹点时展示空状态。

## 6. 权限要求

第一版需要定位权限：

```text
ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION
```

第一版暂不申请：

```text
ACCESS_BACKGROUND_LOCATION
FOREGROUND_SERVICE_LOCATION
```

原因：

- 第一版只做前台 GPS 记录。
- 不做锁屏后台持续记录。
- 降低权限和系统适配复杂度。

权限交互：

```text
进入 GPS 跑步记录页
↓
检查定位权限
↓
未授权则请求权限
↓
授权后允许开始跑步
↓
拒绝后展示定位权限提示
```

拒绝权限时：

```text
无法记录 GPS 轨迹，请开启定位权限
```

## 7. 数据模型建议

### 7.1 跑步会话表

建议新增：

```kotlin
data class RunSessionEntity(
    val id: Long,
    val exerciseRecordId: Long?,
    val startTime: Long,
    val endTime: Long?,
    val durationSeconds: Long,
    val distanceKm: Double,
    val caloriesKcal: Int,
    val averagePaceSecondsPerKm: Int?,
    val createdAt: Long
)
```

说明：

- `exerciseRecordId` 用于关联最终生成的运动记录。
- 第一版结束跑步后再生成运动记录。
- 如果保存过程中失败，要避免出现孤立数据。

### 7.2 轨迹点表

建议新增：

```kotlin
data class RunTrackPointEntity(
    val id: Long,
    val sessionId: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val altitudeMeters: Double?,
    val speedMetersPerSecond: Float?,
    val recordedAt: Long
)
```

说明：

- 轨迹点通过 `sessionId` 关联跑步会话。
- 后续轨迹地图根据 sessionId 查询轨迹点。
- 轨迹点数量可能较多，写入时要注意性能。

### 7.3 运动记录关系

第一版结束跑步后，需要生成一条普通运动记录：

```text
ExerciseRecordEntity
```

字段映射：

```text
type = OUTDOOR_RUNNING
startTime = 跑步开始时间
durationSeconds = 跑步总时长
distanceKm = GPS 计算距离
caloriesKcal = 按现有热量逻辑估算
```

这样首页、运动详情页、统计视图可以继续复用现有运动记录逻辑。

## 8. 定位与距离计算

### 8.1 定位采集

建议使用 Android 原生定位能力或 Google FusedLocationProvider。

如果考虑国内设备兼容性，优先确认荣耀设备上可用性。

第一版建议：

```text
定位间隔：1 秒～3 秒
最小距离变化：3 米～5 米
定位精度过滤：accuracy <= 30 米
```

### 8.2 距离计算

每次收到新的有效 GPS 点：

```text
计算与上一个有效点之间的距离
↓
过滤异常漂移点
↓
累加到 totalDistanceMeters
```

异常点过滤建议：

```text
accuracy > 30m：丢弃
两点距离过大且速度异常：丢弃
时间间隔过小但距离过大：丢弃
```

第一版不做复杂轨迹纠偏，只做基础过滤。

### 8.3 配速计算

平均配速：

```text
durationSeconds / distanceKm
```

如果距离为 0：

```text
--'--"/km
```

展示格式复用当前项目已有：

```text
formatPace(...)
```

## 9. 地图 SDK 方案

国内设备建议优先：

```text
高德地图 SDK
```

原因：

- 国内可用性较好。
- 荣耀设备兼容性更稳定。
- 支持轨迹 Polyline、Marker、缩放到轨迹范围。

Compose 接入方式：

```text
AndroidView
↓
承载原生 MapView
```

需要配置：

```text
高德 Key
包名
签名 SHA1
AndroidManifest metadata
地图 SDK 依赖
```

第一版不要自己手绘地图底图。

如果暂时没有高德 Key，可以先实现轨迹数据保存和地图页空壳，地图 SDK 接入单独后置。

## 10. 架构建议

建议新增模块或包：

```text
feature/run
```

结构示例：

```text
feature/run/
 ├── RunTrackingScreen.kt
 ├── RunMapDetailScreen.kt
 ├── RunTrackingViewModel.kt
 ├── RunTrackingLogic.kt
 └── data/
     ├── RunSessionEntity.kt
     ├── RunTrackPointEntity.kt
     └── RunDao.kt
```

建议数据流：

```text
Location Provider
   ↓
RunTrackingViewModel
   ↓
RunTrackingUiState
   ↓
RunTrackingScreen
```

结束保存：

```text
RunTrackingViewModel
   ↓
ExerciseDao.insertExercise
   ↓
RunDao.insertSession / insertTrackPoints
   ↓
RunMapDetailScreen
```

## 11. UI State 建议

```kotlin
data class RunTrackingUiState(
    val isTracking: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val gpsStatusText: String = "等待定位",
    val distanceKm: Double = 0.0,
    val durationSeconds: Long = 0L,
    val averagePaceText: String = "--'--\"/km",
    val trackPoints: List<RunTrackPointUiModel> = emptyList(),
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

## 12. 导航建议

新增页面：

```text
RunTracking
RunMapDetail
```

首页入口：

```text
StartExerciseCard RUN / 户外跑步
↓
RunTracking
```

跑步结束后：

```text
RunTracking
↓
RunMapDetail(sessionId)
```

如果当前项目仍使用简单枚举导航，需要考虑如何传递 `sessionId`。

可选方案：

```text
MainActivity 保存 selectedRunSessionId 状态
```

后续如果页面增多，再考虑正式接 Navigation Compose。

## 13. 保存规则

点击结束时：

```text
1. 停止定位采集
2. 计算最终距离、时长、配速、热量
3. 插入 ExerciseRecordEntity
4. 插入 RunSessionEntity
5. 插入 RunTrackPointEntity 列表
6. 跳转轨迹地图详情页
```

边界规则：

- 距离过短时提示确认，例如小于 0.05 km。
- 时长过短时提示确认，例如小于 30 秒。
- 没有有效 GPS 点时不保存轨迹。
- 如果用户返回页面，需要确认是否放弃本次记录。

第一版可以先做基础校验：

```text
距离 > 0
时长 > 0
至少 2 个有效轨迹点
```

## 14. 风险点

主要风险：

```text
定位权限适配
荣耀设备定位稳定性
GPS 漂移导致距离不准
地图 SDK Key 和签名配置
地图 SDK 与 Compose 生命周期
轨迹点写入性能
页面退出后定位是否正确停止
```

第一版必须重点验证：

- 进入跑步页不会崩溃。
- 拒绝权限有清晰提示。
- 点击开始后能收到定位点。
- 点击结束后定位停止。
- 运动记录能保存。
- 轨迹点能保存。
- 地图页能显示轨迹。

## 15. 推荐实现步骤

建议分阶段执行：

```text
阶段一：新增 RunSession / RunTrackPoint 数据结构和 Dao
阶段二：新增 RunTrackingScreen 静态 UI
阶段三：接入定位权限请求
阶段四：实现前台 GPS 采集和距离计算
阶段五：结束跑步后生成 ExerciseRecordEntity
阶段六：保存 RunSession 和 RunTrackPoint
阶段七：新增 RunMapDetailScreen
阶段八：接入高德地图展示轨迹
阶段九：真机验证
```

## 16. 验收标准

第一版完成后应满足：

- 首页点击开始运动可以进入 GPS 跑步记录页。
- 用户可以点击开始跑步。
- APP 可以在前台持续记录 GPS 轨迹。
- 页面实时展示距离、时长、平均配速。
- 用户点击结束后停止定位。
- 结束后生成一条运动记录。
- 轨迹点和跑步会话被保存。
- 结束后可以查看轨迹地图。
- 地图显示起点、终点和路线。
- 不影响原有手动新增运动记录功能。
- 不影响运动统计和首页运动卡片统计。

## 17. 后续增强方向

第一版完成后再考虑：

```text
暂停 / 继续
锁屏后台记录
前台服务通知
语音播报
实时地图
轨迹纠偏
分段配速
公里提醒
历史轨迹列表
轨迹分享
```

这些能力不要混入第一版，避免范围失控。
