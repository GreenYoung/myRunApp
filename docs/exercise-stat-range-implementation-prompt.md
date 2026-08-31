# 运动详情统计维度筛选 实现提示词

请基于当前 Android 项目，实现运动详情页的统计维度筛选功能。

本次只实现：

```text
周 / 月 / 年 / 全部 筛选视图
对应统计数据展示
```

不要修改已经完成的运动记录新增、编辑、删除、运动目标、日期选择器、时长选择器、体重模块、首页卡片等无关功能。

## 1. 当前背景

当前项目已有：

```text
ExerciseDetailScreen
ExerciseSummaryCard
ExerciseRecordEntity
ExerciseDao
ExerciseViewModel
Room + Flow 自动刷新
运动记录新增
运动记录编辑
运动记录删除
运动目标设置
```

当前运动详情页顶部已经展示部分汇总数据，例如：

```text
总里程
打卡天数
本周累计
本月累计
总消耗
本月消耗
```

后续需要改为支持按统计维度查看。

## 2. 目标

运动详情页增加筛选视图：

```text
周
月
年
全部
```

用户切换不同维度后，顶部统计区域展示该维度下的：

```text
累计里程
累计消耗
打卡天数
平均配速
单次最长距离
```

运动记录列表可以继续展示全部历史记录，除非实现时确认更适合跟随筛选范围过滤。优先不要改变列表已有行为，避免扩大范围。

## 3. 统计范围定义

周：

```text
当前自然周
周一 00:00:00 到当前时间
```

月：

```text
当前自然月
本月 1 日 00:00:00 到当前时间
```

年：

```text
当前自然年
1 月 1 日 00:00:00 到当前时间
```

全部：

```text
全部运动记录
```

如果项目已有周起始定义，优先复用当前项目逻辑，避免出现首页和详情页统计口径不一致。

## 4. 统计字段

每个筛选维度下都展示：

```text
累计里程
累计消耗
打卡天数
平均配速
单次最长距离
```

### 4.1 累计里程

计算方式：

```text
筛选范围内所有记录 distanceKm 求和
```

展示格式：

```text
xx.xx km
```

固定保留两位小数。

### 4.2 累计消耗

计算方式：

```text
筛选范围内所有记录 caloriesKcal 求和
```

展示格式：

```text
xxxx kcal
```

整数展示。

### 4.3 打卡天数

计算方式：

```text
筛选范围内不同日期数量
```

同一天多条运动记录只算 1 天。

展示格式：

```text
x 天
```

### 4.4 平均配速

计算方式：

```text
筛选范围内总运动时长 / 筛选范围内总里程
```

不要简单平均每条记录的配速。

如果筛选范围内总里程为 0：

```text
--'--"/km
```

否则展示：

```text
x'xx"/km
```

### 4.5 单次最长距离

计算方式：

```text
筛选范围内 max(distanceKm)
```

如果无记录：

```text
0.00 km
```

否则固定两位小数。

## 5. UI 建议

在运动详情页顶部 `ExerciseOverviewCard` 上方或卡片内部顶部增加筛选控件。

建议使用分段控件样式：

```text
[ 周 ] [ 月 ] [ 年 ] [ 全部 ]
```

视觉要求：

```text
深色背景
绿色选中态
未选中使用次级文字
圆角与当前 APP 风格一致
高度紧凑
不要使用系统默认 TabRow 的突兀样式
```

切换筛选项时：

```text
顶部统计数据立即更新
不要重新加载页面
不要影响已有返回逻辑
不要影响底部按钮
```

## 6. 数据结构建议

可以新增：

```kotlin
enum class ExerciseStatsRange {
    WEEK,
    MONTH,
    YEAR,
    ALL
}
```

可以新增：

```kotlin
data class ExerciseRangeStatsUiState(
    val range: ExerciseStatsRange = ExerciseStatsRange.WEEK,
    val totalDistanceKm: Double = 0.0,
    val totalCaloriesKcal: Int = 0,
    val checkInDays: Int = 0,
    val averagePaceText: String = "--'--\"/km",
    val longestDistanceKm: Double = 0.0
)
```

也可以复用和扩展当前 `ExerciseDetailUiState`，但要避免字段语义混乱。

## 7. ViewModel 要求

筛选状态由 ViewModel 管理。

建议：

```kotlin
selectedStatsRange: ExerciseStatsRange
onStatsRangeChange(range: ExerciseStatsRange)
```

统计计算在 ViewModel / 纯函数中完成。

Composable 不直接遍历 Entity 计算统计。

数据流保持：

```text
Room
↓
Flow
↓
ViewModel
↓
ExerciseDetailScreen
```

## 8. 纯函数建议

建议在 `ExerciseLogic.kt` 中新增或扩展：

```kotlin
buildExerciseRangeStats(
    records: List<ExerciseRecordEntity>,
    range: ExerciseStatsRange
): ExerciseRangeStatsUiState
```

或者类似命名。

要求：

```text
便于单元测试
不依赖 Composable
不依赖 Android UI
```

## 9. 空数据状态

当筛选范围内没有运动记录时：

```text
累计里程：0.00 km
累计消耗：0 kcal
打卡天数：0 天
平均配速：--'--"/km
单次最长距离：0.00 km
```

UI 不应崩溃，也不要显示 NaN、Infinity 或空白。

## 10. 与现有首页目标的关系

首页 `ExerciseSummaryCard` 已经有：

```text
本周累计
本月累计
周目标
月目标
圆环进度
```

本次不要修改首页展示结构。

如果复用了统计函数，需要确保首页周/月累计和目标进度口径不变。

## 11. 本次不要实现

不要顺带实现：

```text
自定义日期范围
上一周 / 下一周切换
上一月 / 下一月切换
年份选择
图表
排行
地图
GPS
Health Connect
导出报表
```

## 12. 实现步骤

请按以下顺序执行：

```text
1. 阅读当前 ExerciseModels.kt
2. 阅读 ExerciseLogic.kt
3. 阅读 ExerciseViewModel.kt
4. 阅读 ExerciseScreen.kt 中 ExerciseOverviewCard
5. 新增 ExerciseStatsRange
6. 新增或扩展 Range Stats UI State
7. 在 ExerciseLogic.kt 中实现按周/月/年/全部过滤和统计计算
8. 在 ExerciseViewModel 中保存 selectedStatsRange
9. 增加 onStatsRangeChange
10. 将 range stats 传给 ExerciseDetailScreen
11. 实现分段筛选控件
12. 改造 ExerciseOverviewCard 展示五项统计
13. 保持运动记录列表现有展示逻辑
14. 编译项目
15. 修复编译错误
16. 检查新增/编辑/删除运动记录后统计自动刷新
```

## 13. 验收标准

最终必须满足：

```text
运动详情页可以切换 周 / 月 / 年 / 全部
每个范围展示累计里程
每个范围展示累计消耗
每个范围展示打卡天数
每个范围展示平均配速
每个范围展示单次最长距离
平均配速使用总时长 / 总里程计算
没有记录时展示合理默认值
切换范围后数据立即更新
新增运动记录后当前范围统计自动刷新
编辑运动记录后当前范围统计自动刷新
删除运动记录后当前范围统计自动刷新
不影响首页运动卡片现有结构
不影响运动目标设置
不影响运动记录新增、编辑、删除
不影响体重模块
项目最终正常编译
```
