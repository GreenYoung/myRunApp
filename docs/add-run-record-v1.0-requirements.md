# 29. 第一版增加运动记录字段

点击“＋ 增加运动记录”后，弹窗只负责录入原始运动数据，不在弹窗中展示自动计算结果。

弹窗字段：

```text
运动日期
运动场景
里程
运动时长
坡度（仅跑步机场景显示）
```

其中只有以下字段为必填：

```text
运动场景
里程
运动时长
```

运动日期为可选字段；如果用户未填写，则默认使用当天日期。

坡度为可选字段，不参与必填校验。

推荐样式：

```text
┌──────────────────────────────┐
│ 运动日期                     │
│ [ 2026-08-21            📅 ] │
│                              │
│ 运动场景                     │
│ [ 户外 ]      [ 跑步机 ]     │
│                              │
│ 里程                         │
│ [ 5.20                  ] km │
│                              │
│ 运动时长                     │
│ [ 00 ] 时 [ 33 ] 分 [ 12 ] 秒│
│                              │
│ 坡度                         │
│ [ 3.0                   ] %  │
└──────────────────────────────┘
```

运动场景只支持：

```text
户外
跑步机
```

选择“户外”时隐藏坡度输入项。

选择“跑步机”时显示坡度输入项。

跑步机场景下，如果用户没有填写坡度，则保存时默认：

```text
坡度 = 0%
```

不要因为坡度为空阻止保存。

弹窗视觉上不要显示“增加运动记录”标题，不要显示“仅跑步机场景显示”等辅助说明文字，也不要在内容底部放置“取消 / 保存”文字按钮。整体保持紧凑、纯输入式布局。保存或关闭交互应复用当前项目已有的无文字操作方式；如果项目当前没有合适交互，则保持现有弹窗交互机制，不额外增加底部文字按钮。

不要在弹窗中展示：

```text
平均配速
预计消耗
计算体重
自动计算
```

这些数据全部在用户点击“保存”后自动计算并写入运动记录或派生数据，然后直接体现在运动列表和所有相关统计中。

配速不允许手工输入，按照：

```text
运动时长 / 里程
```

自动计算。

预计消耗热量不允许手工输入，优先使用“运动日期当天最近一条有效体重记录”作为计算体重；如果当天没有体重记录，则使用运动日期之前最近一条有效体重记录。

热量估算需要结合：

```text
体重
运动时长
运动场景
运动速度 / 配速
坡度（跑步机场景）
```

进行估算。

保存成功后，该条记录立即展示为：

```text
🏃 跑步机                         08-21
5.20 km     33:12     6'23"/km     356 kcal
──────────────────────────────────
```

自动计算出的配速和热量同时参与：

```text
首页今日消耗
首页本周消耗
首页本月消耗
运动详情总消耗
运动详情本月消耗
运动记录列表单次消耗
其他已有依赖里程、热量或运动记录的统计
```

所有统计必须来自同一份 ExerciseRecord 数据，不要另外维护一套重复的累计值。

# 30. 数据校验

必填字段只校验：

```text
运动场景
里程
运动时长
```

运动场景不能为空，并且只能是：

```text
户外
跑步机
```

里程：

```text
0 < distanceKm <= 500
```

运动时长：

必须：

```text
> 0
```

运动日期：

```text
可为空
```

为空时自动使用当天日期。

坡度：

```text
户外：不显示、不要求填写
跑步机：可填写，可为空
```

跑步机场景下坡度为空时按：

```text
0%
```

处理。

热量：

```text
>= 0
```

禁止保存：

```text
运动场景为空
里程 0 km
时长 0
```

非法输入时显示明确错误提示。

# 31. ExerciseRecord 数据结构

如果当前尚未创建运动数据库模型：

建议使用：

```kotlin
@Entity(tableName = "exercise_records")
data class ExerciseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: String,

    val startTime: Long,

    val durationSeconds: Long,

    val distanceKm: Double,

    val caloriesKcal: Int
)
```

第一版不要存：

```text
pace
```

因为配速可以通过：

```text
durationSeconds / distanceKm
```

动态计算。

避免重复数据。

# 32. Room DAO

建议至少提供：

```kotlin
insertExercise()

getAllExercises()

observeAllExercises()
```

按照：

```text
startTime DESC
```

排序。

后续统计可以基于全部 ExerciseRecord 计算。

如果当前首页 ExerciseSummaryCard 已经有对应数据结构：

优先复用，不要再创建第二套运动模型。

# 33. 总体统计计算

详情页需要 ViewModel 根据 ExerciseRecord 计算：

```text
totalDistanceKm
checkInDays
weeklyDistanceKm
monthlyDistanceKm
totalCaloriesKcal
monthlyCaloriesKcal
```

不要在 Composable 中进行统计。

# 34. UI State

建议：

```kotlin
data class ExerciseDetailUiState(
    val totalDistanceKm: Double = 0.0,
    val checkInDays: Int = 0,

    val weeklyDistanceKm: Double = 0.0,
    val monthlyDistanceKm: Double = 0.0,

    val totalCaloriesKcal: Int = 0,
    val monthlyCaloriesKcal: Int = 0,

    val records: List<ExerciseRecordUiModel> = emptyList()
)
```

建议 UI 层不要直接大量处理 Entity。

可以增加：

```kotlin
data class ExerciseRecordUiModel(
    val id: Long,
    val type: ExerciseType,
    val dateTimeText: String,
    val distanceText: String,
    val durationText: String,
    val paceText: String,
    val caloriesText: String
)
```

如果当前项目仍然很简单，也可以暂时直接使用 ExerciseRecord。

不要为了追求架构完整度增加过多层级。

# 35. 导航

现有首页：

```text
HomeScreen
```

点击：

```text
ExerciseSummaryCard
```

进入：

```text
ExerciseDetailScreen
```

新增 route：

```text
exerciseDetail
```

结构：

```text
HomeScreen
    ↓
ExerciseDetailScreen
```

详情页返回：

```text
←
```

回到首页。

首页 ExerciseSummaryCard 整个 Card 可点击。

不要额外增加：

```text
查看详情 >
```

保持首页简洁。

# 36. ExerciseSummaryCard 修改

当前首页 ExerciseSummaryCard 之前可能设置为：

```text
不可点击
```

本次需要改为可点击。

增加：

```kotlin
onClick: () -> Unit
```

例如：

```kotlin
ExerciseSummaryCard(
    uiState = exerciseState,
    onClick = {
        navController.navigate("exerciseDetail")
    }
)
```

需要有自然的 Material 点击反馈。

# 37. 新增运动后自动刷新

保存运动记录后：

```text
Room
 ↓
Flow
 ↓
ViewModel
 ↓
ExerciseDetailScreen
 ↓
ExerciseSummaryCard
```

首页和详情页数据都应该自动刷新。

用户不需要：

```text
下拉刷新
重新进入页面
手动刷新
```

# 38. 无运动记录状态

如果当前没有任何运动记录：

顶部数据显示：

```text
总里程
0.00 km

打卡天数
0 天

本周累计
0.00 km

本月累计
0.00 km

总消耗
0 kcal

本月消耗
0 kcal
```

列表区域不要显示假数据。

可以在中间显示：

```text
暂无运动记录

添加第一条运动记录，
开始记录你的运动数据
```

底部：

```text
＋ 增加运动记录
```

仍然正常显示。

# 39. 页面视觉规范

继续使用现有 APP Theme。

页面背景：

```text
深灰 / 近黑
```

例如：

```text
#0B0F14
```

Card：

```text
#111820
#151E26
```

强调色：

```text
#22C55E
```

PrimaryText：

```text
白色 / 接近白色
```

SecondaryText：

```text
灰色
```

热量可以继续使用首页已有：

```text
#FF654F
```

但运动记录列表里不需要给每个热量数字做强烈橙色。

建议：

```text
主要数据白色
运动类型绿色图标
日期灰色
Divider低透明度
```

整体更克制。

# 40. 不要过度卡片化

页面只需要顶部总体数据使用一个较大的 Card。

运动历史区域：

不要：

```text
一条运动一个 Card
```

必须使用：

```text
紧凑列表 + Divider
```

这是本次页面的重要设计要求。

# 41. 组件拆分

建议：

```text
ExerciseDetailScreen
│
├── ExerciseDetailTopBar
│
├── ExerciseOverviewCard
│   └── ExerciseOverviewMetric
│
├── ExerciseRecordList
│   └── ExerciseRecordItem
│
└── AddExerciseButton
```

弹窗：

```text
AddExerciseRecordDialog
```

不要将所有代码写进：

```text
ExerciseDetailScreen.kt
```

一个巨大 Composable 中。

# 42. 数据格式工具

建议复用或增加：

```kotlin
formatDistance()
formatCalories()
formatDuration()
formatPace()
formatExerciseDateTime()
```

例如：

```text
5.20 km
18,542 kcal
33:12
6'23"/km
08-21 07:30
```

不要在多个 Composable 中重复拼字符串。

# 43. 数据库 Migration

如果本次需要新增：

```text
exercise_records
```

Room 数据表：

必须正确升级数据库版本。

例如：

```text
version 1 → version 2
```

提供正确 Migration。

不要使用：

```kotlin
fallbackToDestructiveMigration()
```

导致用户已经保存的体重数据丢失。

这是重要要求。

# 44. 本次不要开发

暂时不要实现：

* GPS运动记录
* 地图
* GPS轨迹
* 自动识别跑步
* 蓝牙跑步机
* Health Connect
* 心率
* 步频
* 海拔
* 运动曲线
* 运动目标设置
* 周/月目标编辑
* 运动记录删除
* 运动记录修改
* 分享
* 社交
* 排行榜
* AI运动分析
* 云同步

当前只完成：

```text
运动详情展示
+
手工增加运动记录
```

# 45. 实现顺序

请按照以下顺序执行：

1. 阅读当前项目代码
2. 阅读 ExerciseSummaryCard
3. 阅读现有 Navigation
4. 阅读 Room Database
5. 判断当前是否已有 ExerciseRecord
6. 如已有则优先复用
7. 如没有则创建 ExerciseRecord
8. 增加必要 DAO
9. 正确增加 Room Migration
10. 创建 ExerciseDetailUiState
11. 实现总体统计计算
12. 新增 ExerciseDetailScreen
13. 实现顶部返回栏
14. 实现顶部 2列×3行总体数据 Card
15. 实现紧凑运动记录列表
16. 实现运动记录 Divider
17. 实现里程格式
18. 实现时长格式
19. 实现配速计算及格式
20. 实现热量格式
21. 实现底部固定“增加运动记录”按钮
22. 实现 AddExerciseRecordDialog
23. 实现运动记录保存
24. 修改 ExerciseSummaryCard 支持点击
25. 增加 exerciseDetail route
26. 实现 Home → ExerciseDetail 导航
27. 实现无数据状态
28. 编译项目
29. 修复编译错误
30. 检查数据库升级不会导致体重数据丢失
31. 检查新增运动记录后首页和详情自动刷新
32. 检查常见手机宽度
33. 输出最终修改文件列表和实现说明

# 46. 验收标准

最终必须满足：

* 点击首页运动卡片可以进入运动详情
* 左上角返回按钮可以回首页
* 顶部不显示“运动概览”
* 列表顶部不显示“运动记录”
* 顶部展示总里程
* 顶部展示打卡天数
* 顶部展示本周累计
* 顶部展示本月累计
* 顶部展示总消耗
* 顶部展示本月消耗
* 顶部 6 个指标固定 2列×3行
* 运动列表按照时间倒序
* 每条运动记录使用紧凑两行结构
* 第一行显示运动类型和日期时间
* 第二行显示里程、时长、配速、热量
* 每条记录之间使用 Divider
* 不给每条记录使用大型 Card
* 里程固定保留两位小数
* 配速自动计算
* 热量使用整数
* 底部固定显示“＋ 增加运动记录”
* 可以手工新增运动记录
* 新增记录后详情页自动刷新
* 新增记录后首页运动卡片自动刷新
* 无运动记录时页面正常显示
* 与现有体重模块 UI 风格统一
* 不影响已有体重数据
* Room 升级不能清空现有数据库
* 项目最终能够正常编译

完成以上需求后停止。

不要自行继续实现 GPS、地图、蓝牙跑步机或其他运动高级功能。

# 47. 运动日期日历选择器

本次只实现“运动日期”的日历选择功能，不修改其他已经完成的运动功能。

点击运动日期区域或右侧日历图标后，弹出一个深色风格的日历浮层。建议独立封装为：

```kotlin
ExerciseDatePicker
```

日历顶部显示当前浏览的“年 + 月”，例如：

```text
2026年8月
```

顶部左侧使用“‹”切换上一个月，右侧使用“›”切换下一个月，支持连续翻月，翻页时不关闭日历。

星期固定显示：

```text
日、一、二、三、四、五、六
```

日期必须按照真实年月和星期正确排列。

打开日历时，默认选中当前运动日期；如果尚未选择运动日期，则默认选择今天。点击某个日期后立即更新运动日期并自动关闭日历，不需要再次确认。

点击日历之外区域时直接关闭日历，并保持原来选择的日期不变。日历中不要出现“取消”“确定”“保存”“完成”等按钮。

日期视觉规则：

```text
Today：
- 使用绿色描边圆形标记
- 背景透明
- 强调色使用 #22C55E

Selected：
- 使用实心绿色圆形背景
- 日期文字使用高对比颜色
- 选中状态优先级高于 Today
- 如果选中日期就是今天，只显示选中状态

普通日期：
- 不使用背景
- 使用正常文字颜色
```

视觉要求：

```text
背景：#111820 / #151E26 一类深色
强调色：#22C55E
圆角：16dp～20dp
日期点击区域：至少 40dp × 40dp
不使用白色日历背景
不引入大型第三方日历库
```

# 48. 运动时长分钟滚轮选择器

本次只实现“运动时长”的分钟滚轮选择功能，不修改其他已经完成的运动记录功能。

运动记录表单中的运动时长不再使用“小时 / 分钟 / 秒”手动输入，改为点击运动时长区域后弹出分钟滚轮选择器。

表单展示：

```text
运动时长
[ 33 分钟                  › ]
```

未选择时展示：

```text
运动时长
[ 请选择                   › ]
```

点击整个运动时长区域后弹出选择器。

分钟选择器使用深色运动风，建议独立封装为：

```text
ExerciseDurationPicker
DurationWheel
DurationWheelItem
```

选择范围：

```text
1 ～ 300 分钟
```

每次递增：

```text
1 分钟
```

不循环滚动，不能小于 1，也不能大于 300。

滚轮交互：

```text
用户上下滑动数字列表
滚动停止后自动吸附到距离中心最近的整数分钟
中心位置代表最终选中的分钟
只允许选择整数分钟
```

如果当前已经选择时长，再次打开时滚轮定位到当前分钟；如果没有选择时长，默认定位到 30 分钟，但不自动认为用户已经选择完成。只有用户滚动停止或点击某个分钟后，才更新运动时长。

选择完成后不显示“取消 / 确定 / 保存 / 完成”等按钮。点击某个分钟或滚动停止完成选择后，立即更新：

```kotlin
selectedDurationMinutes
```

并关闭选择器。点击选择器之外区域直接关闭；如果没有完成新的选择，保持原来的运动时长不变。

视觉规则：

```text
选中项：
- 展示 “33 分钟”
- 字号明显大于上下未选中项
- 使用 #22C55E
- 字体 Medium / SemiBold

未选中项：
- 只展示数字
- 使用 SecondaryText
- 距离中心越远透明度越低

中心区域：
- 可使用上下两条低透明度 Divider
- 或轻微绿色透明背景
```

数据规则：

```kotlin
selectedDurationMinutes: Int?
durationSeconds = selectedDurationMinutes * 60L
```

运动时长仍然必填。没有选择时禁止保存，并提示：

```text
请选择运动时长
```

保存后的配速计算、热量估算、统计汇总继续使用现有 `durationSeconds`，不修改其它统计逻辑。
