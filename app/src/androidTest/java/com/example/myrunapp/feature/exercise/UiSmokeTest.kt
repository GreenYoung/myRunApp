package com.example.myrunapp.feature.exercise

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.myrunapp.feature.home.HomeScreen
import com.example.myrunapp.feature.weight.WeightUiState
import com.example.myrunapp.ui.theme.MyRunAppTheme
import org.junit.Rule
import org.junit.Test

/**
 * 三条 UI 冒烟测试，覆盖最容易回归的三个页面片段：
 * 首页三块卡片、统计详情周期标签、记录行元信息（kcal 不被裁掉）。
 *
 * 注意：这是 instrumentation 测试，需要模拟器/真机，不在 CI 里跑。
 */
class UiSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun home_showsThreeEntryCards() {
        composeRule.setContent {
            MyRunAppTheme(darkTheme = true) {
                HomeScreen(
                    weightUiState = WeightUiState(),
                    exerciseUiState = ExerciseUiState(),
                    onExerciseClick = {},
                    onStartOutdoorRunClick = {},
                    onWeightClick = {}
                )
            }
        }

        composeRule.onNodeWithText("今日运动").assertIsDisplayed()
        composeRule.onNodeWithText("今日体重").assertIsDisplayed()
        composeRule.onNodeWithText("本周目标").assertIsDisplayed()
    }

    @Test
    fun statsDetail_showsPeriodLabelAndRange() {
        composeRule.setContent {
            MyRunAppTheme(darkTheme = true) {
                ExerciseStatsDetailScreen(
                    uiState = ExerciseStatsDetailUiState(
                        period = ExerciseStatsPeriod.WEEK,
                        periodOffset = -1,
                        title = "周统计",
                        periodLabel = "上周 · 11.09-11.15",
                        rangeText = "11月9日 - 11月15日",
                        recordCount = 3,
                        canGoNext = true,
                        canGoPrevious = true
                    ),
                    onBack = {},
                    onPreviousPeriod = {},
                    onNextPeriod = {}
                )
            }
        }

        composeRule.onNodeWithText("上周 · 11.09-11.15").assertIsDisplayed()
        composeRule.onNodeWithText("累计里程 · 11月9日 - 11月15日").assertIsDisplayed()
    }

    @Test
    fun monthCard_recordRowKeepsCalories() {
        composeRule.setContent {
            MyRunAppTheme(darkTheme = true) {
                ExerciseMonthCard(
                    group = ExerciseMonthGroup(
                        year = 2026,
                        month = 11,
                        records = listOf(
                            ExerciseRecordUiModel(
                                id = 1L,
                                type = ExerciseType.TREADMILL,
                                startTime = 1_763_308_920_000L,
                                durationSeconds = 3_420L,
                                distanceKm = 5.2,
                                typeText = "室内",
                                dateTimeText = "2026-11-16",
                                distanceText = "5.20 km",
                                durationText = "57min",
                                paceText = "12'44\"/km",
                                caloriesText = "449 kcal",
                                inclinePercent = 0.0
                            )
                        ),
                        totalDistanceKm = 5.2,
                        totalDurationSeconds = 3_420L,
                        totalCaloriesKcal = 449
                    ),
                    monthlyGoalKm = 80.0,
                    onRecordClick = {},
                    onLongClick = {}
                )
            }
        }

        composeRule.onNodeWithText("449 kcal", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("室内").assertIsDisplayed()
    }
}
