# MyRunApp 技术方案

## 1. 技术目标

MyRunApp 采用 Android 原生技术实现，当前目标是支持离线体重记录和运动记录。

核心原则：

- 本地单机可用。
- 无账号、无服务器、无云同步。
- 无广告 SDK、无健康平台 SDK。
- 体重记录和运动记录在架构、数据模型、页面状态上分离。
- 首页只做摘要聚合和页面入口，不承载复杂业务逻辑。
- UI 优先适配荣耀 Magic8，同时通过统一尺寸定义支持后续设备更换。

## 2. 当前项目基础

当前项目为 Android 工程：

```text
MyRunApp
└── app
```

已具备：

- Kotlin。
- Jetpack Compose。
- Material 3。
- Android Gradle Plugin。
- `minSdk = 24`。
- `targetSdk = 36`。
- `compileSdk = 36`。

## 3. 技术栈

开发语言：

```text
Kotlin
```

UI：

```text
Jetpack Compose
Material 3
```

本地数据库：

```text
Room
SQLite
```

异步和状态：

```text
Kotlin Coroutines
Flow
ViewModel
```

图表：

```text
Jetpack Compose Canvas 自绘折线图
```

选择 Canvas 的原因：

- 当前体重曲线只需要数据点、连线、弱网格、坐标和自适应 Y 轴。
- 避免过早引入维护状态不确定的第三方图表库。
- 后续图表复杂度上升时，可替换 `WeightChart` 的内部实现。

## 4. 依赖计划

需要新增：

```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:<version>")
implementation("androidx.room:room-runtime:<version>")
implementation("androidx.room:room-ktx:<version>")
ksp("androidx.room:room-compiler:<version>")
```

需要启用：

```kotlin
alias(libs.plugins.ksp)
```

说明：

- 具体版本以项目 `libs.versions.toml` 中可用版本为准。
- 当前阶段不引入 Hilt。
- 当前阶段不引入网络库。
- 当前阶段不引入广告、埋点、账号、云同步相关 SDK。

## 5. 架构方案

### 5.1 推荐多 Module 架构

V1.1 开始引入运动记录后，推荐使用轻量多 Module：

```text
MyRunApp
├── app
├── core:data
├── core:ui
├── feature:home
├── feature:weight
└── feature:exercise
```

模块职责：

- `app`：应用入口、主题装配、导航容器。
- `core:data`：Room 数据库、公共数据库配置、基础类型。
- `core:ui`：颜色、排版、尺寸、按钮、卡片等通用 UI。
- `feature:home`：首页双卡片 UI、首页摘要状态和点击跳转。
- `feature:weight`：体重记录、目标体重、体重曲线、体重详情页。
- `feature:exercise`：运动记录、运动统计、运动详情页。

依赖方向：

```text
app
├── feature:home
├── feature:weight
└── feature:exercise

feature:* -> core:ui
feature:* -> core:data
```

约束：

- `feature:weight` 和 `feature:exercise` 不互相依赖。
- `feature:home` 只依赖两个 feature 暴露的只读摘要接口。
- `core:data` 不承载页面状态。

### 5.2 过渡方案

如果先降低改造成本，可以保留单 `app` 模块，但包结构必须按 feature 分离：

```text
com.example.myrunapp
├── core
│   ├── data
│   └── ui
├── feature
│   ├── home
│   ├── weight
│   └── exercise
└── MainActivity.kt
```

无论是否拆 Gradle Module，都必须保证：

- Composable 只负责 UI 渲染和事件转发。
- 数据校验、保存、查询、图表范围计算不直接写在 Composable 内。
- 体重和运动使用独立 Entity、DAO、状态模型和页面逻辑。

## 6. 数据库设计

### 6.1 数据库版本

初始体重版本：

```text
version = 1
```

新增运动记录后：

```text
version = 2
```

后续修改表结构必须提供 Room Migration。

### 6.2 体重记录表

表名：

```text
weight_records
```

Entity：

```kotlin
@Entity(tableName = "weight_records")
data class WeightRecordEntity(
    @PrimaryKey val date: String,
    val weightKg: Double,
    val createdAt: Long,
    val updatedAt: Long
)
```

说明：

- `date` 使用 `yyyy-MM-dd`，作为主键。
- 一天只保留一条体重记录。
- ISO 日期字符串可直接按字典序排序。
- UI 层和 ViewModel 层可使用 `LocalDate` 处理日期。

### 6.3 运动记录表

表名：

```text
exercise_records
```

Entity：

```kotlin
@Entity(tableName = "exercise_records")
data class ExerciseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val distanceKm: Double,
    val durationMinutes: Int,
    val caloriesKcal: Int,
    val createdAt: Long,
    val updatedAt: Long
)
```

说明：

- `date` 使用 `yyyy-MM-dd` 字符串。
- 使用自增 `id`，支持同一天多条运动记录。
- 后续如果增加运动类型，可追加 `exerciseType` 字段并提供 Migration。

### 6.4 目标体重

目标体重属于体重 feature，不应混入运动模块。

V1.1 可先预留目标体重数据源：

- 若暂不实现目标体重设置，首页体重卡片展示 `-`。
- 若实现目标体重设置，可使用本地配置表或单独 Entity 保存。

## 7. DAO 设计

### 7.1 WeightDao

```kotlin
@Dao
interface WeightDao {
    @Upsert
    suspend fun upsertWeight(record: WeightRecordEntity)

    @Query("SELECT * FROM weight_records ORDER BY date DESC LIMIT 1")
    fun observeLatestWeight(): Flow<WeightRecordEntity?>

    @Query("SELECT * FROM weight_records ORDER BY date ASC")
    fun observeAllWeights(): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeWeightsBetween(startDate: String, endDate: String): Flow<List<WeightRecordEntity>>
}
```

说明：

- `@Upsert` 实现同一天覆盖。
- V1.1 可继续订阅全部体重记录后在 Kotlin 层筛选区间。
- `observeWeightsBetween` 作为可选优化接口，不强制使用。

### 7.2 ExerciseDao

```kotlin
@Dao
interface ExerciseDao {
    @Insert
    suspend fun insertExercise(record: ExerciseRecordEntity)

    @Query("SELECT * FROM exercise_records ORDER BY date DESC, createdAt DESC")
    fun observeAllExercises(): Flow<List<ExerciseRecordEntity>>

    @Query("SELECT * FROM exercise_records WHERE date = :date ORDER BY createdAt DESC")
    fun observeExercisesByDate(date: String): Flow<List<ExerciseRecordEntity>>
}
```

说明：

- 运动记录同一天可以插入多条。
- V1.1 数据量较小，运动统计可先在 Kotlin 层聚合。
- 后续数据量增大时，再补充 SQL 聚合查询或索引优化。

### 7.3 Migration

从版本 1 升级到版本 2 时新增运动记录表：

```sql
CREATE TABLE IF NOT EXISTS exercise_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    date TEXT NOT NULL,
    distanceKm REAL NOT NULL,
    durationMinutes INTEGER NOT NULL,
    caloriesKcal INTEGER NOT NULL,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);
```

建议索引：

```sql
CREATE INDEX IF NOT EXISTS index_exercise_records_date ON exercise_records(date);
```

## 8. 核心模型

### 8.1 体重点

```kotlin
data class WeightPoint(
    val date: String,
    val weightKg: Double
)
```

### 8.2 体重曲线区间

```kotlin
enum class WeightChartPeriod {
    Last7Days,
    Last30Days,
    Last90Days,
    LastYear,
    All
}
```

默认值：

```kotlin
WeightChartPeriod.Last30Days
```

展示文案：

```text
7天 / 30天 / 90天 / 1年 / 全部
```

### 8.3 运动统计

```kotlin
data class ExerciseSummary(
    val distanceKm: Double,
    val durationMinutes: Int,
    val caloriesKcal: Int
)
```

```kotlin
enum class ExercisePeriod {
    Day,
    Week,
    Month,
    Year,
    All
}
```

统计规则：

- Day：`date == selectedDate`。
- Week：ISO 周，周一到周日。
- Month：同一年同一月。
- Year：同一年。
- All：全部记录。

## 9. 页面状态设计

### 9.1 HomeUiState

首页状态拆成两个卡片模型：

```kotlin
data class HomeUiState(
    val exerciseCard: ExerciseHomeCardUiState,
    val weightCard: WeightHomeCardUiState
)
```

运动卡片：

```kotlin
data class ExerciseHomeCardUiState(
    val historySummary: String?,
    val entryLabel: String = "进入记录",
    val hasRecords: Boolean
)
```

体重卡片：

```kotlin
data class WeightHomeCardUiState(
    val targetWeightKg: Double?,
    val todayWeightKg: Double?,
    val checkInCount: Int?,
    val hasAnyWeightRecord: Boolean
)
```

展示转换：

- `historySummary == null` 时，运动历史记录展示 `-`。
- `targetWeightKg == null` 时，目标体重展示 `-`。
- `todayWeightKg == null` 时，今天体重展示 `-`。
- `checkInCount == null` 或无任何体重记录时，打卡次数展示 `-`。
- 有体重记录时，打卡次数为有记录的自然日数量。

### 9.2 WeightUiState

```kotlin
data class WeightUiState(
    val latestWeightKg: Double?,
    val latestDate: String?,
    val targetWeightKg: Double?,
    val visibleRecords: List<WeightPoint>,
    val selectedPeriod: WeightChartPeriod,
    val isRecordDialogVisible: Boolean,
    val inputDate: String,
    val inputWeight: String,
    val inputError: String?
)
```

事件：

```kotlin
fun onWeightChartPeriodSelected(period: WeightChartPeriod)
fun onSaveWeight()
fun onRecordDialogVisibleChange(visible: Boolean)
```

区间切换时：

- 只更新 `selectedPeriod` 和 `visibleRecords`。
- 不修改数据库。
- 不影响首页体重卡片。

### 9.3 ExerciseUiState

```kotlin
data class ExerciseUiState(
    val selectedPeriod: ExercisePeriod,
    val summary: ExerciseSummary?,
    val records: List<ExerciseRecordItemUiState>,
    val isRecordDialogVisible: Boolean,
    val inputDate: String,
    val inputDistanceKm: String,
    val inputDurationMinutes: String,
    val inputCaloriesKcal: String,
    val inputError: String?
)
```

事件：

```kotlin
fun onExercisePeriodSelected(period: ExercisePeriod)
fun onSaveExercise()
fun onRecordDialogVisibleChange(visible: Boolean)
```

## 10. 数据流

首页：

```text
Weight feature summary
Exercise feature summary
          ↓
HomeViewModel
          ↓
HomeUiState
          ↓
HomeScreen
```

体重记录：

```text
WeightScreen
   ↓
WeightViewModel
   ↓
WeightDao
   ↓
Room
   ↓
Flow
   ↓
WeightUiState
```

运动记录：

```text
ExerciseScreen
   ↓
ExerciseViewModel
   ↓
ExerciseDao
   ↓
Room
   ↓
Flow
   ↓
ExerciseUiState
```

首页聚合原则：

- 首页不保存运动记录或体重记录。
- 首页不计算复杂图表数据。
- 首页只订阅摘要数据并处理点击跳转。

## 11. 导航设计

建议使用 Navigation Compose。

路由：

```kotlin
object Routes {
    const val Home = "home"
    const val ExerciseDetail = "exercise_detail"
    const val WeightDetail = "weight_detail"
}
```

首页点击事件：

```kotlin
fun onExerciseCardClick()
fun onWeightCardClick()
```

导航目标：

- `onExerciseCardClick()`：进入运动详情页。
- `onWeightCardClick()`：进入体重详情页。

要求：

- 运动卡片主体区域可点击。
- “进入记录”点击区域可点击，目标与卡片主体一致。
- 体重卡片主体区域可点击。
- 无记录状态不禁用点击。

## 12. UI 实现

### 12.1 主题

使用 Material 3 自定义主题统一管理颜色、字体、形状和尺寸。

建议颜色：

```text
Background        #0F1115
Surface           #1A1D24
PrimaryText       #F7F8FA
SecondaryText     #8D93A1
Accent            #34D399
Grid              #2A2F3A
Error             #FF6B6B
```

说明：

- 强调色用于关键数字、曲线、选中态和主要操作。
- 不使用复杂渐变。
- 不复刻第三方品牌视觉资产。

### 12.2 首页组件

```kotlin
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onExerciseCardClick: () -> Unit,
    onWeightCardClick: () -> Unit
)
```

```kotlin
@Composable
fun ExerciseHomeCard(
    state: ExerciseHomeCardUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun WeightHomeCard(
    state: WeightHomeCardUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

布局：

- 竖屏下两个卡片上下排列。
- 荣耀 Magic8 竖屏首屏优先完整展示两个卡片。
- 横屏下可左右排列或紧凑上下排列。
- 卡片高度从统一尺寸定义读取。
- 内容超出时允许换行或增加高度，不允许文字重叠。

### 12.3 体重图表组件

```kotlin
@Composable
fun WeightChart(
    records: List<WeightPoint>,
    modifier: Modifier = Modifier
)
```

职责：

- 绘制弱网格线。
- 绘制 Y 轴刻度文字。
- 绘制日期标签。
- 绘制折线。
- 绘制数据点。
- 突出当前区间最后一个点。
- 处理空数据和单数据点。

区间切换控件由页面层负责：

```text
7天 | 30天 | 90天 | 1年 | 全部
```

### 12.4 运动详情页

推荐结构：

```text
运动

日 / 周 / 月 / 年 / 全部

5.20 km
32 分钟
310 kcal

[新增运动记录]

运动记录列表
2026-08-20  5.2 km  32分钟  310 kcal
```

要求：

- 周期切换使用分段控件或 Tab。
- 核心距离使用页面最大字号。
- 时长和热量作为次级核心指标并列展示。
- 列表使用 `LazyColumn`。
- 新增记录可使用弹窗。

## 13. 体重曲线计算

### 13.1 区间筛选

```kotlin
fun filterWeightRecordsByPeriod(
    records: List<WeightRecordEntity>,
    period: WeightChartPeriod,
    today: LocalDate
): List<WeightRecordEntity>
```

规则：

- `Last7Days`：保留 `today.minusDays(6)` 到 `today` 的记录。
- `Last30Days`：保留 `today.minusDays(29)` 到 `today` 的记录。
- `Last90Days`：保留 `today.minusDays(89)` 到 `today` 的记录。
- `LastYear`：保留 `today.minusDays(364)` 到 `today` 的记录。
- `All`：不筛选。

说明：

- 区间包含当天。
- 最近区间不展示未来日期记录。
- 全部区间展示所有记录；如后续禁止录入未来日期，可调整输入校验。

### 13.2 Y 轴范围

```kotlin
fun calculateWeightAxisRange(records: List<WeightPoint>): ClosedFloatingPointRange<Double>
```

规则：

- 无数据时返回默认范围 `60.0～80.0`，UI 显示空状态。
- 有数据时取最小值和最大值。
- 上下至少预留 `1.0kg`。
- 最小跨度不少于 `2.0kg`。
- 轴范围可按 `0.5kg` 或 `1.0kg` 对齐。

## 14. 输入校验

### 14.1 体重校验

保存前校验：

- 日期不能为空。
- 日期必须是合法 `yyyy-MM-dd`。
- 体重不能为空。
- 体重必须是数字。
- 体重范围为 `30.0～300.0 kg`。
- 体重最多一位小数。

### 14.2 运动校验

保存前校验：

- 日期不能为空。
- 日期必须是合法 `yyyy-MM-dd`。
- 距离必须是数字，范围 `0.1～200.0 km`。
- 距离最多两位小数。
- 时长必须是正整数，范围 `1～1440 分钟`。
- 热量必须是正整数，范围 `1～10000 kcal`。

输入框建议：

- 体重和距离使用数字小数键盘。
- 时长和热量使用数字键盘。
- 保存时统一校验，不依赖键盘限制作为唯一校验。

## 15. 屏幕适配方案

### 15.1 主验证设备

当前首要验证设备：

- 荣耀 Magic8（用户原始描述为“荣耀 MIGIC8”）。

### 15.2 适配原则

- 不以物理像素或单一设备截图反推固定尺寸。
- 使用 `dp`、`sp`、`WindowInsets`、`BoxWithConstraints`、`LazyColumn` 等响应式能力。
- 组件尺寸、间距、圆角、图表高度、底部栏高度、按钮高度统一沉淀到 `core:ui` 或 `ui/theme`。
- 荣耀 Magic8 作为主验收设备，其他设备通过窗口宽度分类和滚动容器保持可用。

建议尺寸定义：

```kotlin
data class AppDimens(
    val screenHorizontalPadding: Dp,
    val sectionSpacing: Dp,
    val cardPadding: Dp,
    val primaryButtonHeight: Dp,
    val bottomNavigationHeight: Dp,
    val chartHeightCompact: Dp,
    val chartHeightRegular: Dp,
    val statCardMinHeight: Dp
)
```

建议断点：

```text
Compact: width < 600dp
Medium: 600dp <= width < 840dp
Expanded: width >= 840dp
```

V1.1 主要按 `Compact` 手机布局实现，预留 `Medium` 和 `Expanded` 尺寸入口。

### 15.3 横竖屏

实现原则：

- 不锁死 Activity 方向，除非后续产品明确只支持竖屏。
- 横竖屏变化时使用 ViewModel 保持当前曲线区间和录入状态。
- 图表尺寸由约束和统一尺寸定义计算。
- 竖屏为主要体验，横屏为增强展示。

方向判断：

```kotlin
val configuration = LocalConfiguration.current
val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
```

竖屏图表高度建议：

```kotlin
val portraitChartHeight = when {
    availableHeight < 640.dp -> 180.dp
    availableHeight < 760.dp -> 220.dp
    else -> 260.dp
}
```

说明：

- 最终值应沉淀到 `AppDimens` 或图表尺寸策略中。
- 竖屏图表高度不应无限放大，避免挤压首页核心内容。
- 横屏可采用左右布局：左侧信息，右侧曲线。

横竖屏切换必须保持：

- 体重曲线 `selectedPeriod`。
- 当前曲线数据。
- 最新体重和最新日期。
- 录入弹窗是否打开。
- 已输入但未保存的内容。

### 15.4 系统 Insets 和软键盘

要求：

- 根布局处理状态栏、导航栏和手势区域 Insets。
- 列表底部预留底部导航和新增按钮空间。
- 录入弹窗使用 `imePadding()` 或等效处理，保证软键盘弹起后保存按钮可见。

## 16. App 图标资源

V1.1 需要替换 Android 默认图标，使用自定义运动健康风格图标。

资源要求：

- 提供 Android Adaptive Icon。
- 提供前景层和背景层资源。
- 保留 `mipmap-anydpi-v26` 自适应图标配置。
- 同步提供必要的 `mipmap` 密度资源或使用矢量/生成资源导出。

建议资源位置：

```text
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
app/src/main/res/drawable/ic_launcher_foreground.xml
app/src/main/res/drawable/ic_launcher_background.xml
```

如果使用生成位图图标，应导出：

```text
mipmap-mdpi
mipmap-hdpi
mipmap-xhdpi
mipmap-xxhdpi
mipmap-xxxhdpi
```

验收：

- 桌面图标不再显示 Android 默认图标。
- 圆形、圆角矩形等启动器裁切下主体不被裁掉。
- 荣耀 Magic8 桌面上图标清晰、有运动健康识别度。
- 深色和浅色壁纸下图标边界清楚。

## 17. 构建与打包脚本

项目需要提供两个独立打包脚本：

- Debug 打包脚本。
- Release 打包脚本。

建议脚本位置：

```text
scripts/build_debug.ps1
scripts/build_release.ps1
```

如果需要兼容 macOS 或 Linux，可额外提供：

```text
scripts/build_debug.sh
scripts/build_release.sh
```

Debug 脚本职责：

- 执行 Debug 构建。
- 生成可安装的 Debug APK。
- 保留调试信息，方便本地安装和问题排查。
- 构建失败时返回非 0 状态码。

建议命令：

```powershell
.\gradlew.bat :app:assembleDebug
```

Release 脚本职责：

- 执行 Release 构建。
- 生成 Release APK 或 AAB。
- 使用正式签名配置。
- 构建失败时返回非 0 状态码。
- 不在脚本中硬编码敏感签名密码。

建议命令：

```powershell
.\gradlew.bat :app:assembleRelease
```

签名配置要求：

- Release 签名信息通过 `local.properties`、环境变量或未入库的 Gradle 配置读取。
- 不提交 keystore 文件。
- 不提交签名密码。
- Debug 使用默认 debug 签名即可。

产物要求：

- Debug 产物输出路径清晰，可用于本地安装测试。
- Release 产物输出路径清晰，可用于正式验收或分发。
- 脚本执行前不清理用户未提交代码。
- 脚本只做构建打包，不做上传、发布、安装等额外动作，除非后续需求明确。

## 18. 开发顺序

建议顺序：

1. 添加 Room、KSP、ViewModel Compose、Navigation Compose 等依赖。
2. 确认是否拆多 Module；若暂不拆，先按 feature 分包。
3. 创建体重记录 Entity、DAO、Database。
4. 创建运动记录 Entity、DAO、Migration。
5. 实现体重输入校验和保存。
6. 实现运动输入校验和保存。
7. 实现首页双卡片状态和 UI。
8. 实现首页点击进入体重详情页、运动详情页。
9. 实现体重曲线、时间区间筛选和 Y 轴计算。
10. 实现运动日、周、月、年、全部统计。
11. 补充 App 自定义图标资源。
12. 补充 Debug 和 Release 打包脚本。
13. 完成荣耀 Magic8 竖屏、横屏和大字体验证。
14. 补充单元测试和基础 UI 验证。

每完成一个阶段，应保证项目可以独立编译。

## 19. 测试计划

### 19.1 体重测试

- 体重范围校验。
- 一位小数校验。
- 日期格式校验。
- 同一天记录覆盖。
- 多天记录按日期升序查询。
- 最新记录按日期倒序获取。
- 7 天、30 天、90 天、1 年、全部区间筛选。
- 区间切换不改变首页体重卡片。
- 空数据、单点、同体重数据的 Y 轴范围计算。

### 19.2 运动测试

- 运动距离输入校验。
- 运动时长输入校验。
- 热量输入校验。
- 同一天多条运动记录统计。
- 日累计统计。
- ISO 周累计统计。
- 月累计统计。
- 年累计统计。
- 全部累计统计。

### 19.3 首页测试

- 无运动记录时，运动历史记录展示 `-`。
- 无体重记录时，目标体重、今天体重、打卡次数展示 `-`。
- 有当天体重时，今天体重展示一位小数。
- 有多天体重时，打卡次数等于有记录的自然日数量。
- 点击运动卡片进入运动详情页。
- 点击运动卡片内“进入记录”进入运动详情页。
- 点击体重卡片进入体重详情页。

### 19.4 UI 与适配测试

- 荣耀 Magic8 竖屏下首页两个卡片完整、内容不重叠。
- 荣耀 Magic8 竖屏下体重曲线比例协调、可读。
- 荣耀 Magic8 横屏下体重曲线重排正常、日期标签不重叠。
- 大字体模式下核心数据不截断。
- 软键盘弹起时录入弹窗按钮仍可点击。
- 底部导航和系统手势区域不遮挡页面内容。
- 桌面图标不再显示默认 Android 图标。

### 19.5 打包脚本测试

- Debug 打包脚本可以成功生成 Debug APK。
- Release 打包脚本可以成功生成 Release APK 或 AAB。
- Release 打包脚本不依赖入库的明文签名密码。
- 构建失败时脚本返回非 0 状态码。
- 两个脚本产物路径清晰。

## 20. 风险与处理

| 风险 | 影响 | 处理 |
| --- | --- | --- |
| 运动和体重逻辑混在一起 | 后续维护困难 | 按 feature 拆分页面、状态、数据模型 |
| 过早多 Module 增加改造成本 | 影响交付速度 | 可先单模块 feature 分包，确认后再拆 Gradle Module |
| 周统计边界不统一 | 用户看到的累计不符合预期 | 明确使用 ISO 周，周一到周日 |
| 首页聚合逻辑膨胀 | 首页 ViewModel 复杂 | 体重和运动各自提供只读摘要，首页只做聚合 |
| 图表横竖屏状态丢失 | 用户体验中断 | 区间和输入状态放在 ViewModel |
| 模仿悦跑圈导致合规风险 | 品牌和视觉风险 | 只参考运动数据气质，不复制素材、Logo、页面和文案 |
| Release 签名信息误提交 | 安全风险 | 使用本地配置或环境变量，不提交 keystore 和密码 |

## 21. 技术验收标准

- 项目可编译通过。
- 无网络权限依赖。
- Room 可保存和读取体重记录。
- Room 可保存和读取运动记录。
- 体重记录同一天保存会覆盖原记录。
- 运动记录同一天可保存多条。
- 首页展示运动记录卡片和体重卡片。
- 首页无记录状态展示 `-`。
- 点击首页卡片可进入对应详情页。
- 体重曲线支持 7 天、30 天、90 天、1 年、全部区间。
- 体重曲线横竖屏切换后状态保持。
- 运动日、周、月、年、全部统计结果准确。
- 数据库升级不破坏已有体重记录。
- App 图标不是 Android 默认图标。
- UI 未使用第三方品牌素材或复制第三方页面。
- 提供 Debug 打包脚本和 Release 打包脚本。
- Debug 脚本可生成可安装 Debug APK。
- Release 脚本可生成正式 Release 产物，且不提交敏感签名信息。
