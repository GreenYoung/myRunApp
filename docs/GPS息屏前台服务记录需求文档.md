# GPS 息屏前台服务记录需求文档

## 1. 需求背景

当前 MyRunApp 已经完成第一版 GPS 跑步记录能力：

```text
首页 RUN / 户外跑步入口
前台 GPS 跑步记录页
实时距离 / 时长 / 平均配速
点击结束保存运动记录
保存 GPS 轨迹点
结束后 Canvas 简化轨迹展示
```

当前第一版只适合 APP 在前台时记录。

后续希望支持：

```text
息屏后继续记录 GPS 轨迹
切后台后继续记录 GPS 轨迹
通知栏显示跑步记录状态
回到 APP 后继续展示实时跑步数据
```

本需求文档只规划「前台服务 Foreground Service 后台记录」能力。

本阶段暂不接入高德地图 SDK。

## 2. 目标

用户点击开始跑步后，APP 启动前台服务持续记录 GPS。

即使用户：

```text
息屏
锁屏
切到后台
短时间切换到其他 APP
```

跑步记录仍然继续。

用户重新打开 APP 后，可以看到当前跑步状态仍在继续，并展示最新：

```text
距离
时长
平均配速
GPS 状态
```

用户点击结束后：

```text
停止前台服务
停止定位
保存运动记录
保存轨迹点
进入轨迹详情页
```

## 3. 本阶段实现范围

本阶段只实现：

```text
前台服务持续定位
息屏 / 切后台继续记录
通知栏显示跑步状态
APP 回前台后同步记录状态
点击结束保存运动记录和轨迹点
点击放弃停止服务并丢弃数据
```

本阶段不实现：

```text
高德地图 SDK
实时地图
后台被系统杀死后的完整恢复
断点续跑
暂停 / 继续
语音播报
公里提醒
轨迹纠偏
运动自动识别
云同步
分享功能
```

## 4. 用户流程

### 4.1 开始跑步

```text
首页
↓
点击 RUN / 户外跑步
↓
进入 RunTrackingScreen
↓
检查定位权限和通知权限
↓
点击开始跑步
↓
启动 RunTrackingService
↓
服务开始采集 GPS
↓
页面展示实时数据
```

### 4.2 息屏或切后台

```text
跑步记录中
↓
用户息屏 / 锁屏 / 切后台
↓
前台服务继续运行
↓
通知栏持续显示跑步状态
↓
GPS 轨迹继续采集
```

通知栏建议显示：

```text
MyRun 正在记录跑步
3.26 km · 00:21:36 · 6'37"/km
```

### 4.3 回到 APP

```text
用户重新打开 APP
↓
RunTrackingScreen 重新绑定或观察服务状态
↓
页面继续显示当前距离、时长、配速
```

### 4.4 结束跑步

```text
点击结束跑步
↓
停止前台服务
↓
停止 GPS 定位
↓
保存 ExerciseRecordEntity
↓
保存 RunSessionEntity
↓
保存 RunTrackPointEntity
↓
进入 RunTrackDetailScreen
```

### 4.5 放弃跑步

```text
记录中点击返回
↓
弹出确认放弃
↓
确认放弃
↓
停止前台服务
↓
停止定位
↓
清空本次内存轨迹
↓
返回首页
```

## 5. 权限要求

### 5.1 必需权限

需要在 AndroidManifest 中增加或确认：

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
```

Android 13 及以上还需要：

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 5.2 后台定位权限说明

如果使用前台服务并且有持续通知，第一阶段可以先不申请：

```xml
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

原因：

- 本阶段目标是前台服务记录。
- 前台服务带通知时，系统允许持续定位。
- 后台定位权限审核和用户授权成本更高。

后续如果发现荣耀 MagicOS 息屏后仍限制定位，再单独评估是否增加后台定位权限。

### 5.3 权限交互

进入跑步记录页时检查：

```text
定位权限
通知权限 Android 13+
系统定位开关
```

如果缺少定位权限：

```text
需要定位权限才能记录户外跑步轨迹
```

如果缺少通知权限：

```text
需要通知权限才能在息屏时保持跑步记录
```

如果系统定位关闭：

```text
请开启系统定位后再开始跑步
```

## 6. 前台服务设计

建议新增：

```text
RunTrackingService
```

职责：

```text
启动 / 停止 GPS 定位
维护跑步中状态
维护内存轨迹点
计算距离、时长、配速
更新通知栏
对外暴露实时状态 Flow / State
结束时保存数据
```

前台服务启动：

```text
ContextCompat.startForegroundService(...)
↓
Service.onStartCommand(...)
↓
startForeground(notificationId, notification)
↓
requestLocationUpdates(...)
```

前台服务停止：

```text
removeLocationUpdates(...)
stopForeground(...)
stopSelf()
```

## 7. 状态管理建议

当前第一版 GPS 记录逻辑主要在：

```text
RunTrackingViewModel
```

息屏记录版本建议将核心记录状态下沉到：

```text
RunTrackingService
或
RunTrackingRepository
```

推荐结构：

```text
RunTrackingScreen
   ↓ observe
RunTrackingViewModel
   ↓ observe / command
RunTrackingRepository
   ↓ bind
RunTrackingService
   ↓
LocationManager
```

或者第一版简化：

```text
RunTrackingService 持有单例状态流
RunTrackingViewModel 观察 Service 状态流
```

要求：

- 页面销毁不应导致 GPS 停止。
- ViewModel 销毁不应导致 GPS 停止。
- 只有点击结束或放弃时才停止服务。

## 8. UI 状态

建议继续复用或扩展：

```kotlin
data class RunTrackingUiState(
    val isTracking: Boolean,
    val hasLocationPermission: Boolean,
    val hasNotificationPermission: Boolean,
    val gpsStatusText: String,
    val distanceKm: Double,
    val durationSeconds: Long,
    val averagePaceText: String,
    val canSave: Boolean,
    val errorMessage: String?
)
```

新增状态：

```kotlin
val isServiceRunning: Boolean
val isScreenOffSupported: Boolean
```

UI 展示建议：

```text
息屏记录中
通知栏保持运行
```

作为轻量提示，不要抢当前距离的主视觉。

## 9. 通知栏设计

通知要求：

- 必须常驻。
- 不可静默到用户完全感知不到。
- 点击通知回到 RunTrackingScreen。
- 内容实时更新。

通知内容建议：

```text
标题：MyRun 正在记录跑步
内容：3.26 km · 00:21:36 · 6'37"/km
```

通知操作按钮第一版可以不做。

后续可增加：

```text
结束
暂停
继续
```

但本阶段不实现。

## 10. 定位采集规则

继续沿用第一版基础规则：

```text
定位间隔：1～3 秒
最小距离变化：3～5 米
有效精度：accuracy <= 30 米
瞬时速度 > 8 m/s 视为异常点
```

服务中每次收到定位点：

```text
1. 判断精度
2. 判断时间差
3. 计算两点距离
4. 判断异常速度
5. 累加距离
6. 更新状态流
7. 更新通知
```

## 11. 数据保存

点击结束时：

```text
1. 服务停止定位
2. 计算最终距离、时长、配速、热量
3. 插入 ExerciseRecordEntity
4. 插入 RunSessionEntity
5. 插入 RunTrackPointEntity
6. 返回 sessionId
7. 页面跳转 RunTrackDetailScreen
```

保存校验：

```text
durationSeconds > 0
distanceKm > 0
有效轨迹点 >= 2
```

不满足时：

```text
本次跑步数据太少，无法保存
```

## 12. Android 版本适配

重点适配：

```text
Android 8+
Android 10+
Android 13+
Android 14+
```

注意点：

- Android 8+ 后台服务必须使用前台服务。
- Android 13+ 需要通知权限。
- Android 14+ 前台服务类型需要声明 location。
- 荣耀 MagicOS 可能有额外后台限制。

Service 声明建议：

```xml
<service
    android:name=".feature.run.RunTrackingService"
    android:exported="false"
    android:foregroundServiceType="location" />
```

## 13. 荣耀 MagicOS 注意事项

当前目标设备：

```text
荣耀 MAGIC8
```

需要真机验证：

```text
息屏 5 分钟后 GPS 是否继续更新
锁屏状态通知是否常驻
切后台后距离是否继续增长
省电模式下是否受限
系统是否提示高耗电
```

如果出现定位中断，后续可能需要增加：

```text
电池优化白名单提示
后台运行权限提示
后台定位权限
```

这些不建议第一步直接强加。

## 14. 推荐实现步骤

建议分阶段执行：

```text
阶段一：新增 RunTrackingService 和通知渠道
阶段二：将定位采集从 ViewModel 下沉到 Service
阶段三：实现 Service 内状态流
阶段四：ViewModel 观察 Service 状态并转成 UI State
阶段五：开始跑步时启动前台服务
阶段六：结束 / 放弃时停止前台服务
阶段七：点击结束后保存运动记录和轨迹
阶段八：通知栏实时显示距离、时长、配速
阶段九：真机息屏 / 锁屏 / 后台验证
```

## 15. 验收标准

完成后必须满足：

- 点击开始跑步后启动前台服务。
- 通知栏显示正在记录跑步。
- APP 切后台后 GPS 继续记录。
- 手机息屏后 GPS 继续记录。
- 回到 APP 后页面继续显示最新距离、时长、配速。
- 点击结束后停止服务和定位。
- 点击结束后保存运动记录和轨迹点。
- 点击放弃后停止服务且不保存数据。
- 不接入高德地图 SDK。
- 不影响已有手动运动记录功能。
- 不影响已有轨迹 Canvas 展示。

## 16. 风险点

主要风险：

```text
Android 前台服务权限适配
Android 13 通知权限
Android 14 foregroundServiceType 限制
荣耀 MagicOS 后台策略
ViewModel 与 Service 状态同步复杂度
页面退出后服务生命周期管理
通知实时更新频率
长时间轨迹点内存占用
```

需要重点避免：

- 页面销毁导致跑步停止。
- 服务停止后通知仍残留。
- 点击结束后重复保存。
- 定位权限拒绝后崩溃。
- 系统定位关闭时进入假记录状态。

## 17. 后续增强

本阶段完成后，再考虑：

```text
暂停 / 继续
通知栏结束按钮
通知栏暂停按钮
后台定位权限
高德地图 SDK
实时地图跟随
轨迹纠偏
公里提醒
语音播报
```

以上增强不纳入本阶段。
