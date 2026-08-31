当前 Android APP 的 GPS 户外运动轨迹记录功能已经实现，但实际测试发现轨迹漂移和累计里程偏差较大。

请不要重写现有 GPS 运动功能，只针对：

**GPS 定位点过滤、轨迹连续性、异常跳点、累计里程准确性**

进行严格优化。

目标优先级：

```text
轨迹准确性 > 轨迹完整性 > 实时响应速度
```

宁可丢弃少量低质量 GPS 点，也不要让明显漂移点进入轨迹和累计里程。

---

# 1. 总体过滤流程

每一个新的 Location 到达后，不允许直接：

```text
加入轨迹
+
累计距离
```

必须依次经过：

```text
原始 Location
      ↓
基础合法性检查
      ↓
定位精度过滤
      ↓
定位时间过滤
      ↓
速度精度过滤
      ↓
位移过滤
      ↓
瞬时速度过滤
      ↓
异常跳点过滤
      ↓
轨迹连续性确认
      ↓
AcceptedLocation
      ↓
绘制轨迹 + 累计里程
```

任何一步失败：

```text
reject
```

该点不能：

* 绘制到轨迹
* 参与累计距离
* 参与平均配速
* 参与实时速度
* 参与热量相关距离统计

---

# 2. 必须使用 Precise Location

户外运动轨迹必须检查：

```text
ACCESS_FINE_LOCATION
```

如果用户只授予 Approximate Location：

不要把粗略定位用于正式运动轨迹和里程统计。

应提示：

```text
需要精确位置权限才能准确记录运动轨迹
```

Android 的 approximate location 误差可能非常大，不适合跑步轨迹统计。

---

# 3. 水平定位精度过滤

只接受：

```text
location.hasAccuracy() == true
```

并设置严格阈值：

```text
accuracy <= 20m
```

推荐分级：

```text
0 ～ 10m
优秀
直接接受

10 ～ 15m
良好
正常接受

15 ～ 20m
一般
仅在轨迹连续且速度合理时接受

> 20m
直接丢弃
```

不要使用：

```text
accuracy <= 50m
accuracy <= 100m
```

这种过于宽松的条件记录跑步轨迹。

建议常量：

```kotlin
MAX_ACCEPTED_ACCURACY_METERS = 20f
GOOD_ACCURACY_METERS = 15f
EXCELLENT_ACCURACY_METERS = 10f
```

如果实际测试户外 GPS 条件很好，可以后续考虑进一步收紧到：

```text
15m
```

但第一版建议先使用：

```text
20m硬阈值
15m推荐阈值
```

---

# 4. 首个 GPS 点不能立即使用

运动开始后：

不要收到第一个 Location 就立刻作为正式轨迹起点。

进入 GPS warm-up 状态。

建议：

至少连续获得：

```text
3 个合格定位点
```

且其中最新一个：

```text
accuracy <= 15m
```

才确认正式起点。

例如：

```text
开始运动

Location1 accuracy = 38m
丢弃

Location2 accuracy = 24m
丢弃

Location3 accuracy = 13m
候选

Location4 accuracy = 11m
候选

Location5 accuracy = 9m
候选

↓
正式开始轨迹
```

避免刚打开 GPS 时产生几十米甚至上百米的起始漂移。

---

# 5. 使用 elapsedRealtime 计算时间差

连续 Location 点之间的时间差：

不要优先使用：

```text
location.time
```

进行运动轨迹差分。

优先使用：

```kotlin
location.elapsedRealtimeNanos
```

计算：

```text
deltaTime
```

因为 elapsedRealtime 是单调递增时间，更适合判断连续定位点。

要求：

```text
new.elapsedRealtimeNanos >
previous.elapsedRealtimeNanos
```

否则直接丢弃。

---

# 6. 定位点最小时间间隔

不需要因为系统连续回调多个 Location 就全部接受。

正式轨迹点建议最小时间间隔：

```text
>= 1 秒
```

推荐：

```kotlin
MIN_LOCATION_INTERVAL_MS = 1000L
```

如果：

```text
deltaTime < 1秒
```

且两个位置差异很小：

直接忽略后一个点。

不要让极高频率 GPS 噪声增加轨迹抖动。

---

# 7. 过旧定位点过滤

收到 Location 时检查其 age。

如果定位点已经明显过旧：

```text
age > 5秒
```

直接丢弃。

建议：

```kotlin
MAX_LOCATION_AGE_MS = 5000L
```

不要将缓存中的 last location 直接加入正式运动轨迹。

正式轨迹必须以实时 location updates 为准。

---

# 8. 最小位移过滤

GPS 即使用户完全静止，也可能在几米范围内漂移。

因此不能：

```text
只要经纬度变化
就累计距离
```

设置最小有效位移。

建议：

```text
distance < 3m
```

默认视为 GPS 抖动：

```text
不累计距离
```

推荐常量：

```kotlin
MIN_DISTANCE_METERS = 3f
```

如果：

```text
distance < 3m
```

可以：

* 不累计距离
* 不增加正式轨迹点

这样可以明显降低用户原地停留时里程自动增长的问题。

---

# 9. 根据 accuracy 动态提高最小位移

不要始终固定使用 3m。

当 GPS 精度较差时，提高最小有效位移。

推荐：

```text
accuracy <= 10m
最小位移 = 3m

10m < accuracy <= 15m
最小位移 = 4m

15m < accuracy <= 20m
最小位移 = 5m
```

即：

GPS 越不稳定：

```text
越不轻易认为用户真的移动了
```

---

# 10. 瞬时速度过滤

计算：

```text
calculatedSpeed =
distance / deltaTime
```

同时如果：

```text
location.hasSpeed()
```

可以参考 GPS 提供的：

```text
location.speed
```

对于当前跑步/快走 APP：

采用严格的人体运动速度上限。

建议：

```text
MAX_RUNNING_SPEED = 8.5 m/s
```

约等于：

```text
30.6 km/h
```

如果连续两点计算得到：

```text
speed > 8.5 m/s
```

直接认为是 GPS 跳点。

不要加入轨迹。

这个上限已经明显高于普通用户跑步速度，因此不会影响正常运动，但能过滤：

```text
50km/h
80km/h
200km/h
```

这种 GPS 瞬移。

---

# 11. 极端跳点直接拒绝

增加硬位移限制。

例如：

```text
deltaTime <= 5秒
```

但：

```text
distance > 50m
```

直接拒绝。

建议：

```kotlin
MAX_JUMP_DISTANCE_METERS = 50f
MAX_JUMP_WINDOW_MS = 5000L
```

例如：

```text
2秒移动 83m
3秒移动 120m
```

这种点无论 accuracy 看起来多好：

都不进入轨迹。

---

# 12. Speed Accuracy 过滤

如果设备支持：

```kotlin
location.hasSpeedAccuracy()
```

则读取：

```kotlin
location.speedAccuracyMetersPerSecond
```

建议：

```text
speedAccuracy > 2.0 m/s
```

认为速度可信度较差。

此时：

不要仅依赖：

```text
location.speed
```

进行轨迹判断。

优先使用：

```text
distance / deltaTime
```

结合位置连续性判断。

如果：

```text
speedAccuracy <= 1.5m/s
```

可以认为速度数据质量较好。

---

# 13. 连续两点跳变不要立即接受

增加“三点确认”机制处理可疑点。

例如已有：

```text
A
```

收到：

```text
B
```

如果 B 与 A 的距离/方向明显异常：

不要马上接受 B。

暂存为：

```text
suspectLocation
```

等待下一个：

```text
C
```

如果：

```text
C 又回到 A 附近的正常运动方向
```

说明 B 是漂移点：

```text
丢弃 B
接受 C
```

例如：

```text
A --------正常方向-------- C
 \
  \
   \---- B（突然偏离马路50m）
```

这种情况下：

B 必须被过滤。

---

# 14. 三点几何异常过滤

针对：

```text
A → B → C
```

如果：

```text
A → B 很远
B → C 又很远
```

但：

```text
A → C 很近
```

说明 B 很可能是单点漂移。

例如：

```text
A → B = 35m
B → C = 37m
A → C = 5m
```

则：

```text
B = GPS异常点
```

删除 B。

这个规则对：

```text
轨迹突然出现尖刺
```

非常有效。

---

# 15. 方向突变过滤

如果设备有 bearing，并且当前运动速度足够高：

可以辅助判断方向变化。

不要在：

```text
速度很低
```

时使用 bearing，因为静止/慢速情况下方向噪声很大。

仅在：

```text
speed >= 1.5m/s
```

时考虑方向一致性。

如果：

```text
1～2秒内方向突然变化 > 120°
```

同时伴随：

```text
异常大位移
```

将该点标记为 suspect。

注意：

方向变化不能单独作为拒绝条件。

因为用户真的可能掉头。

必须与：

```text
distance
speed
accuracy
```

组合判断。

---

# 16. 静止状态严格过滤

如果用户：

```text
连续多个点 speed < 0.5m/s
```

认为可能处于：

```text
静止 / 等红灯 / 休息
```

此时必须提高位移过滤严格度。

建议：

```text
stationary minDistance = 5m
```

即用户静止时：

小于 5m 的 GPS 漂移完全不累计。

避免：

```text
站5分钟
GPS自己走了100m
```

---

# 17. 静止状态判定

建议满足：

连续：

```text
3～5个合格点
```

速度均：

```text
< 0.5m/s
```

进入：

```text
STATIONARY
```

恢复条件：

连续：

```text
2个点
```

速度：

```text
>= 0.8m/s
```

再恢复：

```text
MOVING
```

使用滞回机制：

```text
进入静止 <0.5
恢复移动 >=0.8
```

避免状态频繁抖动。

---

# 18. 不要累计 accuracy 圆内的微小运动

如果：

```text
distance
```

明显小于两个定位点的不确定范围：

不应该立即认为是真实移动。

可以增加辅助条件：

```text
distance >= max(
    MIN_DISTANCE,
    min(currentAccuracy, previousAccuracy) * 0.25
)
```

但必须设置上限，避免 GPS accuracy=20m 时要求移动10m以上。

建议最终动态 threshold：

```text
3m ～ 5m
```

不要无限随 accuracy 放大。

---

# 19. GPS 精度突然恶化

如果：

```text
上一点 accuracy = 5m

下一点 accuracy = 19m
```

不要因为仍然小于20m就无条件接受。

对于：

```text
accuracy > 15m
```

的点：

必须同时满足：

```text
速度合理
位移合理
方向无明显异常
不是单点跳变
```

才允许接受。

---

# 20. accuracy 突然改善也不能直接信任跳点

例如：

```text
上一点 accuracy = 18m
新点 accuracy = 5m
```

但新点突然跳了：

```text
70m
```

不能因为 accuracy=5m 就接受。

所有点必须同时通过：

```text
accuracy
+
distance
+
time
+
speed
+
continuity
```

检查。

---

# 21. Mock Location

正式运动轨迹中检查：

```kotlin
location.isMock
```

兼容低版本可使用：

```text
LocationCompat.isMock(location)
```

如果检测到 mock location：

不要参与正式 GPS 运动轨迹和里程统计。

至少记录日志：

```text
LOCATION_REJECT_MOCK
```

---

# 22. 建议 LocationRequest

如果当前使用：

```text
FusedLocationProviderClient
```

户外运动时采用：

```text
Priority.PRIORITY_HIGH_ACCURACY
```

推荐：

```text
interval = 1000 ～ 2000ms
minUpdateInterval = 1000ms
minUpdateDistance = 0～2m
```

注意：

LocationRequest 本身不能代替业务过滤。

即使请求 high accuracy：

收到的每个 Location 仍然必须经过上述过滤器。

---

# 23. 原始轨迹与有效轨迹分开

强烈建议区分：

```text
rawLocations

acceptedLocations
```

调试阶段：

可以保留 raw location：

```text
lat
lng
accuracy
speed
timestamp
rejectReason
```

正式：

地图轨迹和运动里程只使用：

```text
acceptedLocations
```

不要让 rawLocations 直接参与用户运动统计。

---

# 24. 每个拒绝点记录原因

建议定义：

```kotlin
enum class LocationRejectReason {
    NO_ACCURACY,
    POOR_ACCURACY,
    STALE_LOCATION,
    INVALID_TIME,
    TOO_CLOSE,
    IMPOSSIBLE_SPEED,
    LARGE_JUMP,
    POOR_SPEED_ACCURACY,
    SUSPECT_SPIKE,
    MOCK_LOCATION
}
```

调试日志示例：

```text
GPS_REJECT
accuracy=34.2
distance=12.4
reason=POOR_ACCURACY
```

或者：

```text
GPS_REJECT
accuracy=8.5
distance=72.4
deltaTime=2.1s
speed=34.4m/s
reason=IMPOSSIBLE_SPEED
```

这样现场测试时才能知道轨迹为什么被过滤。

---

# 25. 建议严格参数汇总

第一版先使用下面这一组参数：

```text
最大定位精度：
20m

推荐良好精度：
15m

优秀精度：
10m

GPS起步：
连续3个合格点
至少一个 <= 15m

定位点最大年龄：
5秒

最小定位时间间隔：
1秒

最小有效位移：
3m

中等精度最小位移：
4m

较差精度最小位移：
5m

静止最小位移：
5m

人体运动最大合理速度：
8.5m/s
≈ 30.6km/h

5秒内最大合理跳变：
50m

速度精度良好：
<= 1.5m/s

速度精度较差：
> 2.0m/s

静止速度：
< 0.5m/s

恢复移动速度：
>= 0.8m/s
```

将这些参数统一放到：

```kotlin
GpsTrackFilterConfig
```

中。

不要散落魔法数字。

---

# 26. 推荐数据结构

建议：

```kotlin
data class GpsTrackFilterConfig(
    val maxAccuracyMeters: Float = 20f,
    val goodAccuracyMeters: Float = 15f,
    val excellentAccuracyMeters: Float = 10f,

    val maxLocationAgeMs: Long = 5000L,
    val minIntervalMs: Long = 1000L,

    val minDistanceMeters: Float = 3f,
    val mediumAccuracyMinDistanceMeters: Float = 4f,
    val poorAccuracyMinDistanceMeters: Float = 5f,
    val stationaryMinDistanceMeters: Float = 5f,

    val maxRunningSpeedMps: Float = 8.5f,

    val maxJumpDistanceMeters: Float = 50f,
    val maxJumpWindowMs: Long = 5000L,

    val goodSpeedAccuracyMps: Float = 1.5f,
    val poorSpeedAccuracyMps: Float = 2.0f,

    val stationarySpeedMps: Float = 0.5f,
    val movingResumeSpeedMps: Float = 0.8f
)
```

---

# 27. 建议过滤器独立封装

不要把所有判断写进：

```text
LocationCallback
```

建议独立：

```kotlin
class GpsTrackFilter
```

例如：

```kotlin
sealed interface LocationFilterResult {

    data class Accepted(
        val location: Location
    ) : LocationFilterResult

    data class Rejected(
        val location: Location,
        val reason: LocationRejectReason
    ) : LocationFilterResult

    data class Pending(
        val location: Location
    ) : LocationFilterResult
}
```

调用结构：

```text
FusedLocationProvider
        ↓
GpsTrackFilter
        ↓
Accepted
        ↓
TrackRecorder
        ↓
地图 + 距离统计
```

---

# 28. 累计里程计算

累计距离只允许使用：

```text
acceptedLocations
```

例如：

```text
accepted A
accepted B
accepted C
```

累计：

```text
distance(A,B)
+
distance(B,C)
```

Rejected Location：

绝对不能参与累计距离。

Pending Location：

在确认之前也不能参与累计。

---

# 29. 配速同步修正

实时平均配速：

不要根据 raw GPS distance 计算。

必须使用：

```text
filteredTotalDistance
```

配合真实运动时间。

因此 GPS 漂移点被过滤后：

```text
总里程
平均配速
运动详情
首页运动统计
```

应该全部同步使用过滤后的距离。

禁止：

```text
地图使用过滤距离
但统计使用原始距离
```

造成数据不一致。

---

# 30. 不做过度平滑

本次重点是：

```text
异常点过滤
```

而不是把真实路线强行变成平滑曲线。

不要直接使用非常强的：

```text
移动平均
Bezier曲线
大量坐标平滑
```

修改用户真实轨迹。

例如用户真的：

```text
转弯
掉头
绕弯
```

轨迹应该保留。

优先过滤：

```text
明显异常点
```

而不是“让地图看起来漂亮”。

---

# 31. 测试场景

至少验证以下场景：

### 正常直线跑步

预期：

```text
轨迹连续
无明显漏点
距离误差较小
```

### 原地静止5分钟

预期：

```text
累计里程基本不增长
```

### 等红灯

预期：

```text
GPS轻微漂移不增加明显距离
```

### 高楼附近

预期：

```text
20m以上低精度点大量被过滤
```

### 单个GPS跳点

例如：

```text
正常路线
突然跳到街对面80m
下一秒又回来
```

预期：

```text
异常点完全不显示
距离不增加
```

### 正常急转弯

预期：

```text
不因为方向变化而误删真实轨迹
```

### 用户掉头

预期：

```text
正常保留
```

### GPS刚启动

预期：

```text
不立即使用第一个粗糙定位点
```

---

# 32. 调试统计

一次运动结束后 Debug 日志输出：

```text
Raw GPS points: 1250

Accepted points: 802

Rejected:
POOR_ACCURACY: 128
TOO_CLOSE: 214
IMPOSSIBLE_SPEED: 4
SUSPECT_SPIKE: 2
STALE_LOCATION: 0

Raw distance:
5.67 km

Filtered distance:
5.21 km
```

便于实际户外测试后继续调整阈值。

不要把这些 Debug 信息展示给普通用户。

---

# 33. 验收标准

最终必须满足：

* accuracy > 20m 的点不进入正式轨迹
* GPS启动阶段不会立即使用粗定位
* 静止时小范围漂移基本不累计距离
* 异常高速跳点被过滤
* 数十米瞬移点被过滤
* 单点尖刺能够通过三点连续性识别
* 正常转弯不会因为方向变化被误删
* 所有距离统计只使用过滤后的轨迹
* 地图轨迹和累计里程使用同一份 acceptedLocations
* 配速基于过滤后的距离
* 每个被拒绝点都有明确 reject reason
* 所有阈值统一放在 GpsTrackFilterConfig
* 不在 LocationCallback 中堆积大量过滤业务代码
* 不破坏现有 GPS 开始、暂停、继续、结束运动逻辑
* 不修改现有运动详情 UI
* 项目最终正常编译

本次只优化 GPS 轨迹过滤和距离准确性，不自行扩展其他 GPS 功能。
