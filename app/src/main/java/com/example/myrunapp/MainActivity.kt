package com.example.myrunapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.myrunapp.core.data.AppDatabase
import com.example.myrunapp.core.log.AppLogger
import com.example.myrunapp.core.log.LogTags
import com.example.myrunapp.feature.exercise.ExerciseRoute
import com.example.myrunapp.feature.exercise.ExerciseRecordDetailRoute
import com.example.myrunapp.feature.exercise.ExerciseStatsDetailRoute
import com.example.myrunapp.feature.exercise.ExerciseStatsPeriod
import com.example.myrunapp.feature.exercise.ExerciseViewModel
import com.example.myrunapp.feature.exercise.ExerciseViewModelFactory
import com.example.myrunapp.feature.exercise.data.ExerciseGoalRepository
import com.example.myrunapp.feature.home.HomeScreen
import com.example.myrunapp.feature.run.RunTrackDetailRoute
import com.example.myrunapp.feature.run.RunTrackingRoute
import com.example.myrunapp.feature.run.RunTrackingViewModel
import com.example.myrunapp.feature.run.RunTrackingViewModelFactory
import com.example.myrunapp.feature.weight.data.TargetWeightRepository
import com.example.myrunapp.feature.weight.WeightRoute
import com.example.myrunapp.feature.weight.WeightTrendRoute
import com.example.myrunapp.feature.weight.WeightViewModel
import com.example.myrunapp.feature.weight.WeightViewModelFactory
import com.example.myrunapp.ui.components.SwipeBackContainer
import com.example.myrunapp.ui.theme.MyRunAppTheme

private enum class AppDestination {
    Home,
    Exercise,
    Weight,
    WeightTrend,
    RunTracking,
    RunTrackDetail,
    ExerciseRecordDetail,
    ExerciseStatsDetail
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App 固定深色 UI：系统栏统一用"浅色图标"，否则系统处于浅色模式时
        // 状态栏/导航栏会画成深色图标，落在深色背景上看不见。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        AppLogger.init(applicationContext)
        AppLogger.i(LogTags.APP, "MainActivity created")

        val database = AppDatabase.getInstance(applicationContext)
        val targetWeightRepository = TargetWeightRepository(applicationContext)
        val exerciseGoalRepository = ExerciseGoalRepository(applicationContext)
        val weightViewModelFactory = WeightViewModelFactory(
            database,
            database.weightDao(),
            database.exerciseDao(),
            targetWeightRepository
        )
        val exerciseViewModelFactory = ExerciseViewModelFactory(
            database,
            database.exerciseDao(),
            database.runDao(),
            database.weightDao(),
            exerciseGoalRepository
        )
        val runTrackingViewModelFactory = RunTrackingViewModelFactory(
            applicationContext,
            database.runDao(),
            database.runDraftDao()
        )

        setContent {
            MyRunAppTheme(darkTheme = true) {
                val weightViewModel: WeightViewModel = viewModel(factory = weightViewModelFactory)
                val weightUiState by weightViewModel.uiState.collectAsState()
                val exerciseViewModel: ExerciseViewModel = viewModel(factory = exerciseViewModelFactory)
                val exerciseUiState by exerciseViewModel.uiState.collectAsState()
                val runTrackingViewModel: RunTrackingViewModel = viewModel(factory = runTrackingViewModelFactory)
                var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }
                var selectedRunSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
                var selectedExerciseRecordId by rememberSaveable { mutableStateOf<Long?>(null) }
                var selectedExerciseStatsPeriod by rememberSaveable { mutableStateOf(ExerciseStatsPeriod.WEEK) }
                val swipeBackEnabled = destination in setOf(
                    AppDestination.Exercise,
                    AppDestination.Weight,
                    AppDestination.WeightTrend,
                    AppDestination.ExerciseRecordDetail,
                    AppDestination.ExerciseStatsDetail
                )
                fun navigateBack() {
                    destination = when (destination) {
                        AppDestination.Exercise,
                        AppDestination.Weight,
                        AppDestination.RunTracking -> AppDestination.Home
                        AppDestination.WeightTrend -> AppDestination.Weight
                        AppDestination.RunTrackDetail,
                        AppDestination.ExerciseRecordDetail,
                        AppDestination.ExerciseStatsDetail -> AppDestination.Exercise
                        AppDestination.Home -> AppDestination.Home
                    }
                }

                BackHandler(enabled = destination != AppDestination.Home) {
                    navigateBack()
                }

                SwipeBackContainer(
                    enabled = swipeBackEnabled,
                    onBack = { navigateBack() }
                ) {
                    when (destination) {
                        AppDestination.Home -> HomeScreen(
                            weightUiState = weightUiState,
                            exerciseUiState = exerciseUiState,
                            onExerciseClick = { destination = AppDestination.Exercise },
                            onStartOutdoorRunClick = { destination = AppDestination.RunTracking },
                            onWeightClick = { destination = AppDestination.Weight }
                        )

                        AppDestination.Exercise -> ExerciseRoute(
                            viewModel = exerciseViewModel,
                            onBack = { navigateBack() },
                            onRecordClick = { recordId ->
                                selectedExerciseRecordId = recordId
                                destination = AppDestination.ExerciseRecordDetail
                            },
                            onStatsPeriodClick = { period ->
                                selectedExerciseStatsPeriod = period
                                destination = AppDestination.ExerciseStatsDetail
                            }
                        )

                        AppDestination.Weight -> WeightRoute(
                            viewModel = weightViewModel,
                            onBack = { navigateBack() },
                            onTrendClick = { destination = AppDestination.WeightTrend }
                        )

                        AppDestination.WeightTrend -> WeightTrendRoute(
                            viewModel = weightViewModel,
                            onBack = { navigateBack() }
                        )

                        AppDestination.RunTracking -> RunTrackingRoute(
                            viewModel = runTrackingViewModel,
                            onBack = { navigateBack() },
                            onSaved = { sessionId ->
                                selectedRunSessionId = sessionId
                                destination = AppDestination.RunTrackDetail
                            }
                        )

                        AppDestination.RunTrackDetail -> RunTrackDetailRoute(
                            viewModel = runTrackingViewModel,
                            sessionId = selectedRunSessionId ?: 0L,
                            onBack = { navigateBack() }
                        )

                        AppDestination.ExerciseRecordDetail -> ExerciseRecordDetailRoute(
                            viewModel = exerciseViewModel,
                            recordId = selectedExerciseRecordId ?: 0L,
                            onBack = { navigateBack() }
                        )

                        AppDestination.ExerciseStatsDetail -> ExerciseStatsDetailRoute(
                            viewModel = exerciseViewModel,
                            period = selectedExerciseStatsPeriod,
                            onBack = { navigateBack() }
                        )
                    }
                }
            }
        }
    }
}
