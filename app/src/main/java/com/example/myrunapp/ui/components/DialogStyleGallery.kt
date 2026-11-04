package com.example.myrunapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myrunapp.ui.dialogs.AddExerciseRecordFields
import com.example.myrunapp.ui.dialogs.ExerciseGoalFields
import com.example.myrunapp.ui.dialogs.TargetWeightFields
import com.example.myrunapp.ui.dialogs.WeightInputFields
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.MyRunAppTheme

private val GalleryButtonHeight = 48.dp
private val GalleryButtonCorner = 14.dp

/**
 * 弹窗样式总览：所有弹窗叠在一页里，方便在 Android Studio 预览面板一次看完。
 *
 * 表单类弹窗在「弹窗样式总览」，确认类弹窗在「确认类弹窗」。
 */
@Preview(name = "弹窗样式总览", showBackground = true, widthDp = 400, heightDp = 1900)
@Composable
private fun DialogStyleGalleryPreview() {
    MyRunAppTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(horizontal = 26.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            AppDialogCard(
                title = "运动目标",
                onDismiss = {},
                confirmText = "保存",
                onConfirm = {}
            ) {
                ExerciseGoalFields(
                    weeklyGoal = "20",
                    monthlyGoal = "80",
                    error = null,
                    onWeeklyGoalChange = {},
                    onMonthlyGoalChange = {}
                )
            }

            AppDialogCard(
                title = "添加运动记录",
                onDismiss = {},
                confirmText = "保存",
                onConfirm = {}
            ) {
                AddExerciseRecordFields(
                    date = "2026-11-04",
                    distanceKm = "5.20",
                    inclinePercent = "0",
                    durationLabel = "33 分钟",
                    error = null,
                    isTreadmill = false,
                    onDateClick = {},
                    onDistanceChange = {},
                    onInclineChange = {},
                    onDurationClick = {}
                )
            }

            AppDialogCard(
                title = "记录体重",
                onDismiss = {},
                confirmText = "保存",
                onConfirm = {}
            ) {
                WeightInputFields(
                    weight = "72.5",
                    date = "2026-11-04",
                    error = null,
                    onWeightChange = {},
                    onDateClick = {}
                )
            }

            AppDialogCard(
                title = "设置目标体重",
                onDismiss = {},
                confirmText = "保存",
                onConfirm = {}
            ) {
                TargetWeightFields(
                    value = "70.0",
                    heightCm = "175",
                    error = null,
                    onValueChange = {},
                    onHeightCmChange = {}
                )
            }
        }
    }
}

/** 带错误提示 + 跑步机（里程/坡度并排）的样子。 */
@Preview(name = "弹窗样式总览（含错误提示）", showBackground = true, widthDp = 400, heightDp = 1900)
@Composable
private fun DialogStyleGalleryErrorPreview() {
    MyRunAppTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(horizontal = 26.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            AppDialogCard(
                title = "运动目标",
                onDismiss = {},
                confirmText = "保存",
                onConfirm = {}
            ) {
                ExerciseGoalFields(
                    weeklyGoal = "",
                    monthlyGoal = "",
                    error = "月目标需要大于等于周目标",
                    onWeeklyGoalChange = {},
                    onMonthlyGoalChange = {}
                )
            }

            AppDialogCard(
                title = "添加运动记录",
                onDismiss = {},
                confirmText = "保存",
                onConfirm = {}
            ) {
                AddExerciseRecordFields(
                    date = "2026-11-04",
                    distanceKm = "",
                    inclinePercent = "0",
                    durationLabel = null,
                    error = "请填写有效里程（0.01～100 km）",
                    isTreadmill = true,
                    onDateClick = {},
                    onDistanceChange = {},
                    onInclineChange = {},
                    onDurationClick = {}
                )
            }

            AppDialogCard(
                title = "记录体重",
                onDismiss = {},
                confirmText = "保存",
                onConfirm = {}
            ) {
                WeightInputFields(
                    weight = "",
                    date = "2026-11-04",
                    error = "请输入 20.0～300.0 之间的体重",
                    onWeightChange = {},
                    onDateClick = {}
                )
            }
        }
    }
}

/** 确认类弹窗：记录操作（三按钮）、删除确认、后台定位、放弃记录。 */
@Preview(name = "确认类弹窗", showBackground = true, widthDp = 400, heightDp = 1500)
@Composable
private fun DialogConfirmGalleryPreview() {
    MyRunAppTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(horizontal = 26.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            AppDialogCard(
                title = "记录操作",
                onDismiss = {},
                confirmText = "编辑",
                onConfirm = {},
                actions = {
                    AppSecondaryButton(
                        text = "取消",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        height = GalleryButtonHeight,
                        cornerRadius = GalleryButtonCorner
                    )
                    AppPrimaryButton(
                        text = "编辑",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        height = GalleryButtonHeight,
                        cornerRadius = GalleryButtonCorner
                    )
                    AppDangerButton(
                        text = "删除",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        height = GalleryButtonHeight,
                        cornerRadius = GalleryButtonCorner
                    )
                }
            ) {
                AppDialogMessage("2026-11-04 08:12 · 跑步机 · 5.20 km")
            }

            AppDialogCard(
                title = "删除运动记录",
                onDismiss = {},
                confirmText = "删除",
                onConfirm = {},
                actions = {
                    AppSecondaryButton(
                        text = "取消",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        height = GalleryButtonHeight,
                        cornerRadius = GalleryButtonCorner
                    )
                    AppDangerButton(
                        text = "删除",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        height = GalleryButtonHeight,
                        cornerRadius = GalleryButtonCorner
                    )
                }
            ) {
                AppDialogMessage("确认删除这条运动记录？\n2026-11-04 08:12 · 跑步机 · 5.20 km")
            }

            AppDialogCard(
                title = "开启后台持续定位",
                onDismiss = {},
                confirmText = "去设置",
                onConfirm = {},
                actions = {
                    AppSecondaryButton(
                        text = "暂不",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        height = GalleryButtonHeight,
                        cornerRadius = GalleryButtonCorner
                    )
                    AppPrimaryButton(
                        text = "去设置",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        height = GalleryButtonHeight,
                        cornerRadius = GalleryButtonCorner
                    )
                }
            ) {
                AppDialogMessage("锁屏后系统可能冻结定位和计时，导致轨迹中断。建议把 MyRun 加入电池优化白名单，保证跑步全程持续记录。")
            }

            AppDialogCard(
                title = "放弃未完成的跑步记录？",
                onDismiss = {},
                confirmText = "放弃",
                onConfirm = {},
                actions = {
                    AppSecondaryButton(
                        text = "保留记录",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        height = GalleryButtonHeight,
                        cornerRadius = GalleryButtonCorner
                    )
                    AppDangerButton(
                        text = "放弃",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        height = GalleryButtonHeight,
                        cornerRadius = GalleryButtonCorner
                    )
                }
            ) {
                AppDialogMessage("未完成的跑步记录将被删除，且无法恢复。")
            }
        }
    }
}
