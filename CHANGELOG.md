# CHANGELOG

## 2026-11-17（P2）

- 轨迹点追加从 O(N²) 降为持久化列表追加：引入 `kotlinx-collections-immutable 0.3.8`，Service 里用 `trackPoints.toPersistentList().add(point)`；状态类型仍是 `List`，渲染器/保存/预览的调用点无需改动。
- 地图页 `remember` 的 key 从"列表对象"改为「点数 + 最后一点时间」，避免每个点都重跑一次 O(N) 过滤。
- 结束跑步保存改为按 500 条/批写入轨迹点（SQLite 单语句变量上限 999），长跑保存不再一次性构造上万个参数的语句。
- 统计详情缓存最近 6 个周期结果，并用 `remember(period, periodOffset)` 复用数据库 Flow：翻回已看过的周期直接命中缓存，不再闪 `0.00 km` 占位、也不在每次重组时新建订阅。
- 删除 6 个无引用文件（约 1900 行）：`ExerciseSummaryCard` / `WeightSummaryCard` / `StartExerciseCard` / `SportCardBackground` / `ExerciseRecordItemDemo` / `DialogStyleGallery`。
- 新增设计 token 入口 `ui/theme/Tokens.kt`（`AppColors` / `AppRadius` / `AppSpacing`）：新代码从这里取值，旧页面在改动时逐步替换。
- 新增 GitHub Actions CI（`.github/workflows/ci.yml`）：`chmod +x gradlew` 修执行位、删除本机 `org.gradle.java.home`、跑 `assembleDebug + testDebugUnitTest + lintDebug`。
- 新增 3 条 Compose UI 冒烟测试（首页三块卡片、统计周期标签、记录行 `449 kcal` 不被裁）；本机仅验证编译，运行需模拟器/真机。
- 修复 `gradle.properties` 的 `PropertyEscape` lint error（`D\:` 转义），`:app:lintDebug` 现在 0 error。

## 2026-11-17（续）

- 统一数值文本样式 `NumericText`（tnum + 零字距），替换 `ExerciseTabularNumbers` 与首页 `TabularNumbers`，避免继承主题 0.5sp 字距导致数字行超宽。
- 运动记录行元信息改为「左段可压缩 + 热量不可压缩」两段式：配速多一位数字时用省略号收尾，`kcal` 不再被静默裁掉；月卡头部与统计卡副标题补显式 `TextOverflow.Ellipsis`。
- 新增 `Modifier.appClickable`（白色 16% ripple），统一首页卡片、目标 chip、统计分段、记录行、体重趋势卡、体重记录行、统计翻页箭头、地图类型切换等点击反馈（此前裸 `Box + clickable` 在深色背景上 ripple 不可见）。
- 地图页状态改用 `rememberSaveable`（地图类型/跟随），`MapView` 走标准 `onSaveInstanceState` 并在 `ON_PAUSE` 抓取，旋转后可恢复；`postDelayed` 增加销毁保护；权限检查改用 `refreshPermissionNow()` 返回值，避免读到旧快照重复弹窗。
- 体重目标支持增重方向：`calculateTargetProgress` 按方向计算、`targetProgressText` 输出「还需减 / 还需增 / 已达成」，超额完成显示已达成；同日覆盖改为 `WeightOverwriteDialog` 二次确认，不再静默覆盖。
- 主题父类改为深色并设置同色 `windowBackground`（消除冷启动白屏），`enableEdgeToEdge` 显式使用浅色系统栏图标；`LocalLifecycleOwner` 迁移到 `lifecycle-runtime-compose`，消除 2 条弃用告警。
- 侧滑返回手势 `pointerInput` key 固定为 `Unit` 并用 `rememberUpdatedState` 取最新回调，避免滑动过程中输入检测被重启。
- 单测增至 59 条（体重目标新增 4 条：增重方向、减重剩余、双向超额已达成）。

## 2026-11-17（P0）

- 跑步暂停时停止 AMap 定位、恢复时重启客户端并跳过首个点，暂停期间不再每秒定位与耗电；恢复失败会自动回滚为"仍暂停"并提示，避免时长继续累加。
- 跑步计时改用 `SystemClock.elapsedRealtime()`（单调时钟）：改系统时间 / NTP 校时 / 跨时区不再影响时长；结束时间改为"起点 + 时长"，草稿恢复以已记录的时长续算，不需要改表结构。
- 计时逻辑抽成纯函数 `computeActiveDurationSeconds()` 并补 6 条单测（正常、暂停冻结、恢复扣除、草稿续算、时钟回拨、异常钳制）。
- 通知权限被拒不再阻断开始跑步（Android 13+ 前台服务不依赖通知权限），只在顶部状态条提示"息屏时可能看不到进度"。
- 跑步进行中新增「放弃本次跑步」入口（长按结束提示下方，低强调），点击弹 `DiscardActiveRunDialog` 二次确认，确认后丢弃数据并停止服务。
- 前台服务通知改用专用单色小图标 `drawable/ic_stat_run.xml`，不再用自适应启动图标。

## 2026-11-17

- 移除首页「户外跑 / 室内跑」两张快捷卡片，入口统一收敛到「今日运动」卡片（卡片内「开始运动」进入跑步页，点击卡片进入运动页）。
- 修复运动统计详情页周期 pill 翻页后退化的问题：原来翻到上一期只显示「周 / 月 / 年」，现改为「相对词 · 区间」（如「上周 · 11.09-11.15」「3个月前 · 2026.08」），本期仍显示「本周 / 本月 / 今年」。
- 新增 `ExerciseStatsDetailUiState.periodLabel` 与 `statsPeriodLabel()`，周期标签统一在数据层生成，Composable 只负责渲染。
- 优化周/月/年统计区间文案：`2026-11-16 至 2026-11-17` 改为 `11月16日 - 11月17日`，起止同一天只写一天，跨年周两端带年份，年统计整年显示 `2025年`、当年显示 `2026年（至 11月17日）`。
- 统计周期无运动记录时显示「该周期暂无运动记录」，避免 `0.00 km` 被误认为数据未加载。
- 运动列表的室内跑记录缩略图由 emoji 改为彩色跑步插画：新增 `drawable-nodpi/ic_indoor_run.png`（192×192、透明底，按预乘 alpha 缩放避免透明区颜色渗透）与 `IndoorRunThumbnail` 组件；容器与户外轨迹缩略图一致，容器内只放图形，类型仍由行右侧胶囊标注；室外无轨迹记录保留原徽章。
- 修复轨迹地图上 GO/END 与里程标记互相遮挡：起终点改为旗标贴图，GO 用 `anchor(0.5, 1.0)` 向上展开、END 用 `anchor(0.5, 0.0)` 向下展开，起点即终点也不会重叠；层级调整为 GO > END > 里程点 > 轨迹线。
- 新增屏幕空间标记布局 `TrackLabelLayout`：里程点按「起终点 > 里程点（编号小的优先）」避让，同簇只保留最大编号、其余降级为小圆点，撞上起终点时整簇降级；阈值按 dp 换算，随缩放重排。
- 新增 `AMapTrackLabelRenderer` 统一"候选 → 布局 → 落图"，历史页 `AMapStaticTrackRenderer` 与实时页 `AMapLiveTrackRenderer` 共用；缩放变化（纯平移不算）后重排标记。
- `AMapTrackPreview` 增加点集签名去重，避免 `AndroidView.update` 每次重组都 `clear()` 重画整条轨迹（也是接入相机重排的前置条件）。
- 实时页追踪中不再绘制 LIVE 标签：它原来的 zIndex(21) 低于当前位置点(30)，实际一直被压住；跑完后才出现 END 旗标。
- 标记位图缓存统一到 `AMapTrackLabels.kt`，新增旗标、里程圆点贴图缓存；`moveToSportTrack` 的 bounds 留白改为按密度计算，避免贴边旗标被裁。
- 补充 9 条标记布局单测（同点起终点、同点多公里聚合成最大编号、与起终点间距、密度换算、下标保持）。
- 修复运动记录列表行里「时长 · 配速 · 热量」被截断：该行 `Text` 未设置字距，继承了 `MaterialTheme.bodyLarge` 的 `letterSpacing = 0.5.sp`，28 个字符多占约 14dp，配速从 `8'44"` 变成 `12'44"` 时末尾 `kcal` 就被裁掉；这行显式改成 `letterSpacing = 0.sp`。
- 运动详情页顶部卡片里的「周统计 / 月统计 / 年统计」增加可点击渲染：三段改成三块 4% 白底的圆角块、按 6dp 间隙分隔（不用分割线），段高从 34dp 提到 38dp，按下时底色加深到 10%。这三段是进入统计详情页的入口而不是 tab，所以刻意不做选中态（深色卡片上默认 ripple 取 `LocalContentColor`、实际不可见，改用按下态背景给反馈）。
- 补充 14 条统计文案单测（周期标签 6 条 + 区间文案 8 条），`ExerciseLogicTest` 共 17 条用例全部通过。

## 2026-08-21

- 重构首页体重展示卡片为方案 F 层叠渐变风格，突出最新体重、最新日期、较上次变化和累计变化，不在首页卡片展示体重曲线。
- 新增 `WeightCardUiState`，由体重 ViewModel 计算首页卡片所需的最新体重、上次变化和累计变化，首页 Composable 仅负责渲染。
- 新增独立 `WeightSummaryCard` 组件，拆分头部、主数值、变化区域和 Canvas 层叠渐变背景，并支持无数据空状态。
- 重构体重详情页为深色运动风数据页，包含最新体重、目标进度、累计变化、目标体重、本周变化、趋势图、最近 5 条记录和底部操作区。
- 新增 `WeightDetailUiState` 和 `WeightRange`，由 ViewModel 计算详情页数据、7/30/90 天/全部范围筛选、最近记录变化和目标完成进度。
- 新增 DataStore Preferences 目标体重存储，支持目标体重设置弹窗和详情页自动刷新。
- 新增首页 `ExerciseSummaryCard` 运动打卡卡片，复用方案 F 深色层叠渐变背景，展示今日/本周/本月里程、目标完成度、总里程、打卡天数和热量统计。
- 新增 `ExerciseSummaryUiState`，当前正式运行使用默认空运动数据，Preview 使用示例数据，不提前引入运动数据库或运动详情功能。
- 抽取 `GradientSportCardBackground` 作为首页体重卡片和运动卡片共享背景组件，并将首页改为体重卡片在上、运动卡片在下的可滚动布局。
- 新增运动记录 Room 表、DAO、`ExerciseViewModel` 和非破坏性数据库迁移，支持手工新增运动记录并通过 Flow 自动刷新首页运动卡片和运动详情页。
- 新增 `ExerciseDetailScreen`，点击首页运动卡片进入，展示 2 列 × 3 行总体数据、倒序紧凑运动记录列表、无数据状态和底部固定“+ 增加运动记录”按钮。
- 新增运动记录输入弹窗，支持跑步机/户外、运动日期、里程、时长和跑步机坡度录入，并自动计算列表配速展示。
- 优化运动记录保存逻辑：运动日期可为空并默认当天，热量不再手工录入，保存时根据运动日期最近有效体重、时长、速度、场景和坡度自动估算。

## 2026-08-20

- 新增体重记录 APP V1.0 需求文档。
- 新增体重记录 APP V1.0 技术方案。
- 补充 V1.1 运动记录扩展需求，包含每日运动记录、日周月年全部统计、首页核心数据总览和悦跑圈风格参考边界。
- 补充 V1.1 运动记录扩展技术方案，包含运动与体重记录分离、多 Module 架构建议、运动数据模型、统计规则和测试计划。
- 补充 V1.1 屏幕适配要求，以荣耀 Magic8 为当前主验证设备，同时要求 UI 控件尺寸通过统一尺寸定义支持后续设备更换。
- 补充 V1.1 体重曲线时间区间查看能力，支持 7 天、30 天、90 天、1 年、全部区间筛选。
- 补充 V1.1 App 自定义图标要求，明确不能使用 Android 默认图标，需要年轻健康、突出运动主题的图标资源。
- 补充 V1.1 体重曲线横竖屏适配要求，明确竖屏展示比例、横屏重排和方向切换状态保持。
- 补充 V1.1 首页双卡片方案，首页展示运动记录卡片和体重卡片，并支持点击进入对应详情页面。
- 补充构建打包要求，明确提供 Debug 和 Release 两个独立打包脚本。
- 实现 V1.0 体重记录功能，包括 Room 本地保存、同日覆盖、首页展示、录入弹窗、输入校验和体重曲线。
- 修复本地构建环境：Gradle Wrapper 切换到可访问的 Gradle 8.13 镜像，并配置 Gradle 使用 Android Studio 自带 JDK。
- 调整首页为运动记录和体重记录双卡片入口；运动记录详情暂以占位页面承接，体重卡片展示目标体重、今日体重和打卡次数。
- 优化体重录入：支持当天快捷选择、默认当前年份、月份和日期横向滑动选择，并支持体重录入到小数点后两位。
- 替换 App 启动图标为年轻健康、突出运动主题的自定义自适应图标资源。
- 修复首页卡片点击后页面目标状态被重置的问题，体重卡片可正常进入体重详情页。
- 调整 APK 输出命名为 `myRun-v版本号.apk`，版本号统一由 Gradle 配置项维护。
- 优化 App 按钮视觉风格，新增统一主按钮、次按钮和返回按钮组件，并替换体重页、运动页返回入口。
- 优化体重录入日期选择，改为点击日期弹出深色风格日历，支持上下翻月、当天标记和选中日期标记。
- 放大体重曲线展示区域并减少图表边距，曲线仅标注首次记录和最后一次记录的体重。
- 支持点击体重曲线进入横屏全屏查看模式，关闭后恢复原页面方向。
