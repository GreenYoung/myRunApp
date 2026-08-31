package com.example.myrunapp

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.example.myrunapp.ui.theme.MyRunAppTheme

private enum class AppDestination {
    Home,
    Exercise,
    Weight,
    WeightTrend,
    RunTracking,
    RunTrackDetail,
    ExerciseRecordDetail
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppLogger.init(applicationContext)
        AppLogger.i(LogTags.APP, "MainActivity created")

        val database = AppDatabase.getInstance(applicationContext)
        val targetWeightRepository = TargetWeightRepository(applicationContext)
        val exerciseGoalRepository = ExerciseGoalRepository(applicationContext)
        val weightViewModelFactory = WeightViewModelFactory(database.weightDao(), targetWeightRepository)
        val exerciseViewModelFactory = ExerciseViewModelFactory(
            database.exerciseDao(),
            database.runDao(),
            database.weightDao(),
            exerciseGoalRepository
        )
        val runTrackingViewModelFactory = RunTrackingViewModelFactory(
            applicationContext,
            database.runDao()
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
                        onBack = { destination = AppDestination.Home },
                        onRecordClick = { recordId ->
                            selectedExerciseRecordId = recordId
                            destination = AppDestination.ExerciseRecordDetail
                        }
                    )

                    AppDestination.Weight -> WeightRoute(
                        viewModel = weightViewModel,
                        onBack = { destination = AppDestination.Home },
                        onTrendClick = { destination = AppDestination.WeightTrend }
                    )

                    AppDestination.WeightTrend -> WeightTrendRoute(
                        viewModel = weightViewModel,
                        onBack = { destination = AppDestination.Weight }
                    )

                    AppDestination.RunTracking -> RunTrackingRoute(
                        viewModel = runTrackingViewModel,
                        onBack = { destination = AppDestination.Home },
                        onSaved = { sessionId ->
                            selectedRunSessionId = sessionId
                            destination = AppDestination.RunTrackDetail
                        }
                    )

                    AppDestination.RunTrackDetail -> RunTrackDetailRoute(
                        viewModel = runTrackingViewModel,
                        sessionId = selectedRunSessionId ?: 0L,
                        onBack = { destination = AppDestination.Exercise }
                    )

                    AppDestination.ExerciseRecordDetail -> ExerciseRecordDetailRoute(
                        viewModel = exerciseViewModel,
                        recordId = selectedExerciseRecordId ?: 0L,
                        onBack = { destination = AppDestination.Exercise }
                    )
                }
            }
        }
    }
}
