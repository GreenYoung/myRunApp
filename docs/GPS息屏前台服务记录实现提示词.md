# GPS 息屏前台服务记录实现提示词

请基于当前 Android 项目，实现「GPS 息屏前台服务记录」能力。

本次只实现前台服务后台/息屏持续记录，不接入高德地图 SDK，不新增暂停/继续，不扩展其他 GPS 功能。

## 1. 当前背景

当前项目已经完成：

```text
首页 StartExerciseCard RUN / 户外跑步入口
RunTrackingScreen 前台 GPS 跑步记录页
RunTrackDetailScreen Canvas 简化轨迹详情页
RunTrackingViewModel
RunSessionEntity
RunTrackPointEntity
RunDao
ExerciseRecordEntity 自动生成
GPS 轨迹过滤 GpsTrackFilter
GpsTrackFilterConfig
LocationRejectReason
```

当前 GPS 距离过滤已经优化过，实测与悦跑圈接近：

```text
悦跑圈：1.19 km
MyRunApp：1.23 km
```

因此本次不要重写 GPS 距离过滤算法。

必须继续复用：

```text
GpsTrackFilter
GpsTrackFilterConfig
LocationFilterResult
LocationRejectReason
```

本次重点是把 GPS 采集和跑步状态从页面 / ViewModel 生命周期中拆出来，迁移到前台服务，保证息屏和切后台后仍然可以继续记录。

## 2. 本次目标

用户点击开始跑步后：

```text
启动 RunTrackingService
↓
服务进入前台服务模式
↓
通知栏显示正在记录跑步
↓
服务持续采集 GPS
↓
息屏 / 锁屏 / 切后台后仍继续记录
```

用户回到 APP 后：

```text
RunTrackingScreen 显示服务中的实时状态
距离 / 时长 / 平均配速 / GPS 状态继续更新
```

用户点击结束后：

```text
停止定位
停止前台服务
保存 ExerciseRecordEntity
保存 RunSessionEntity
保存 RunTrackPointEntity
进入 RunTrackDetailScreen
```

用户点击放弃后：

```text
停止定位
停止前台服务
清空本次轨迹
不保存运动记录
返回首页
```

## 3. 本次实现范围

只实现：

```text
RunTrackingService
前台服务通知
息屏 / 切后台持续 GPS 记录
RunTrackingViewModel 与服务状态同步
开始跑步启动服务
结束跑步停止服务并保存
放弃跑步停止服务且不保存
Android 13+ 通知权限处理
Android 14 foregroundServiceType=location 适配
```

不要实现：

```text
高德地图 SDK
百度地图 SDK
Google Maps
暂停 / 继续
通知栏结束按钮
通知栏暂停按钮
语音播报
公里提醒
轨迹纠偏
运动自动识别
后台被系统杀死后的完整恢复
断点续跑
云同步
分享功能
```

## 4. 权限要求

AndroidManifest 需要确认或新增：

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Service 声明：

```xml
<service
    android:name=".feature.run.RunTrackingService"
    android:exported="false"
    android:foregroundServiceType="location" />
```

本次不要申请：

```xml
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

原因：

- 本阶段使用前台服务 + 常驻通知。
- 暂不处理后台定位权限审核和授权复杂度。
- 如果荣耀 MagicOS 后续仍中断，再单独评估后台定位权限。

## 5. 通知权限

Android 13+ 需要请求：

```text
POST_NOTIFICATIONS
```

建议在进入 RunTrackingScreen 时检查。

如果缺少通知权限：

```text
提示：需要通知权限才能在息屏时保持跑步记录
```

如果用户拒绝通知权限：

- 不要崩溃。
- 可以禁止开始后台记录。
- 或提示用户开启通知权限后再开始。

第一版建议：

```text
定位权限 + 通知权限都满足后，才允许开始跑步
```

## 6. RunTrackingService 设计

新增：

```kotlin
class RunTrackingService : Service()
```

职责：

```text
启动前台通知
请求 LocationManager 定位
持有 GpsTrackFilter
维护跑步状态
维护 accepted track points
累计 filtered distance
计算 duration / pace
更新通知
对外暴露状态流
结束时保存数据
放弃时清空数据
```

建议提供命令：

```text
ACTION_START
ACTION_FINISH
ACTION_DISCARD
```

也可以通过 Binder 调用方法，但第一版建议使用清晰的 Service command + 单例状态流。

## 7. 状态流设计

建议新增一个运行时状态 holder，例如：

```kotlin
object RunTrackingStateStore {
    val state: StateFlow<RunTrackingUiState>
}
```

或者由 `RunTrackingService` companion object 暴露：

```kotlin
val trackingState: StateFlow<RunTrackingUiState>
```

要求：

- `RunTrackingScreen` 销毁后，状态仍然存在。
- `RunTrackingViewModel` 销毁后，服务仍然继续。
- APP 回到前台时，ViewModel 能重新观察当前状态。

注意：

不要让 `RunTrackingViewModel.onCleared()` 再停止 GPS。

只有以下动作可以停止服务：

```text
结束跑步
放弃跑步
服务异常自停
```

## 8. ViewModel 改造要求

当前 `RunTrackingViewModel` 直接持有：

```text
LocationManager
LocationListener
GpsTrackFilter
timerJob
trackPoints
totalDistanceMeters
```

本次需要迁移：

```text
LocationManager
LocationListener
GpsTrackFilter
timerJob
trackPoints
totalDistanceMeters
```

到：

```text
RunTrackingService
```

ViewModel 改为：

```text
检查权限
向 Service 发送开始 / 结束 / 放弃命令
观察服务状态
观察保存成功事件
提供 RunTrackDetailScreen 数据
```

不要在 ViewModel 销毁时停止定位。

## 9. GPS 过滤要求

服务中每个 Location 到达后，必须继续走：

```text
GpsTrackFilter.filter(location)
```

只有：

```text
LocationFilterResult.Accepted
```

才允许：

```text
加入 trackPoints
累计 totalDistanceMeters
更新 averagePace
保存到最终轨迹
```

Rejected / Pending：

- 不进入轨迹。
- 不累计距离。
- 可以更新 GPS 状态。
- 保留 debug 日志。

不要回退到旧逻辑：

```text
accuracy <= 30m
speed <= 8m/s
```

## 10. 通知栏设计

通知渠道：

```text
channelId = run_tracking
channelName = 跑步记录
```

通知内容：

```text
标题：MyRun 正在记录跑步
内容：3.26 km · 00:21:36 · 6'37"/km
```

通知要求：

- 常驻。
- 点击通知回到 MainActivity / RunTrackingScreen。
- 距离、时长、配速更新时同步刷新通知。
- 不需要通知栏操作按钮。

更新频率建议：

```text
1 秒更新一次时长
GPS 点接受后更新距离
```

如果频繁更新通知导致性能问题，可以降低通知更新频率到：

```text
2～5 秒
```

但页面状态仍可每秒更新。

## 11. 保存规则

点击结束：

```text
1. Service 停止 LocationManager 更新
2. 停止 timer
3. 校验数据
4. 估算热量
5. 插入 ExerciseRecordEntity
6. 插入 RunSessionEntity
7. 插入 RunTrackPointEntity
8. 发出保存成功 sessionId
9. 停止前台服务
10. 页面跳转 RunTrackDetailScreen
```

保存校验：

```text
durationSeconds > 0
distanceKm > 0
accepted trackPoints >= 2
```

不满足时：

```text
本次跑步数据太少，无法保存
```

避免重复保存：

- 点击结束后应进入 saving 状态。
- 保存过程中禁用重复点击。
- Service 端需要防重入。

## 12. 放弃规则

记录中点击返回：

```text
弹出确认放弃
```

确认后：

```text
发送 DISCARD 命令
停止定位
停止前台服务
清空内存轨迹
不保存 ExerciseRecordEntity
不保存 RunSessionEntity
不保存 RunTrackPointEntity
返回首页
```

## 13. UI 要求

RunTrackingScreen 当前 UI 可以保持现状，不做大改。

只需要增加轻量状态：

```text
息屏记录中
通知栏保持运行
```

不要抢当前距离主视觉。

权限提示：

```text
需要精确位置权限才能准确记录运动轨迹
需要通知权限才能在息屏时保持跑步记录
请开启系统定位后再开始跑步
```

## 14. 导航要求

保持当前简单导航方式即可。

现有流程：

```text
Home
↓
RunTracking
↓ 保存成功 sessionId
RunTrackDetail
```

本次继续沿用。

如果通知点击打开 APP，第一版可先回到首页或当前 Activity。

建议尽量回到：

```text
RunTrackingScreen
```

但不要为此引入复杂 Navigation Compose 重构。

## 15. Android 版本适配

重点：

```text
Android 8+：必须使用 startForegroundService
Android 13+：通知权限 POST_NOTIFICATIONS
Android 14+：foregroundServiceType location
```

启动服务时：

```text
ContextCompat.startForegroundService(context, intent)
```

Service 内尽快调用：

```text
startForeground(...)
```

避免系统抛出前台服务启动超时异常。

## 16. 荣耀 MAGIC8 验证重点

开发完成后需要真机验证：

```text
开始跑步后通知栏是否显示
息屏 5 分钟后距离是否继续增长
锁屏状态通知是否常驻
切后台使用其他 APP 后 GPS 是否继续
回到 MyRunApp 后页面数据是否同步
点击结束是否保存轨迹
点击放弃是否停止服务且不保存
省电模式下是否被限制
```

如果荣耀系统仍限制后台定位，后续再单独规划：

```text
电池优化白名单提示
后台运行权限提示
ACCESS_BACKGROUND_LOCATION
```

本次不做。

## 17. 禁止改动范围

本次不要修改：

```text
高德地图 SDK
Canvas 轨迹详情页展示逻辑
运动详情页 UI
首页运动卡片 UI
体重模块
运动目标
手动新增运动记录弹窗
GPS 过滤参数，除非编译必须调整
数据库表结构，除非前台服务保存确实需要
```

现有 `RunSessionEntity`、`RunTrackPointEntity`、`RunDao` 应优先复用。

## 18. 推荐实现顺序

请按以下顺序实现：

```text
1. 检查当前 RunTrackingViewModel / RunTrackingScreen / RunLogic
2. 新增 RunTrackingService
3. 新增通知渠道和通知构建逻辑
4. Manifest 增加前台服务权限和 service 声明
5. 将定位采集、计时、GpsTrackFilter 从 ViewModel 迁移到 Service
6. 建立 Service 状态流
7. ViewModel 观察 Service 状态
8. ViewModel 发送 start / finish / discard 命令
9. RunTrackingScreen 增加通知权限检查
10. 点击开始启动前台服务
11. 点击结束保存并停止服务
12. 点击放弃停止服务且不保存
13. 编译项目
14. 修复编译错误
15. 给出最终修改文件列表和验证结果
```

## 19. 验收标准

最终必须满足：

- 点击开始跑步后启动前台服务。
- 通知栏显示 MyRun 正在记录跑步。
- APP 切后台后 GPS 继续记录。
- 手机息屏后 GPS 继续记录。
- 回到 APP 后页面继续显示最新距离、时长、配速。
- 点击结束后停止服务和定位。
- 点击结束后保存运动记录和轨迹点。
- 点击放弃后停止服务且不保存数据。
- 距离、配速、轨迹继续使用 `GpsTrackFilter` 过滤后的 accepted points。
- 不接入高德地图 SDK。
- 不影响已有手动运动记录功能。
- 不影响已有 Canvas 轨迹详情页。
- 项目编译通过。

## 20. 最终输出要求

实现完成后，请输出：

```text
修改文件列表
核心实现说明
权限和真机验证注意事项
编译结果
```

如果无法完成真机验证，需要明确说明：

```text
已完成编译验证，息屏 GPS 需要荣耀 MAGIC8 真机实测
```
