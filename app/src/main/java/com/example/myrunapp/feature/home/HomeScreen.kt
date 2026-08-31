package com.example.myrunapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.myrunapp.feature.exercise.ExerciseUiState
import com.example.myrunapp.feature.weight.WeightUiState
import com.example.myrunapp.ui.components.PageHorizontalPadding
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.MyRunAppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    weightUiState: WeightUiState,
    exerciseUiState: ExerciseUiState,
    onExerciseClick: () -> Unit,
    onStartOutdoorRunClick: () -> Unit,
    onWeightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = PageHorizontalPadding,
                top = PageTopSpacing,
                end = PageHorizontalPadding,
                bottom = 28.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HomeHeader()
        StartExerciseCard(
            onStartClick = onStartOutdoorRunClick,
            onOutdoorClick = onStartOutdoorRunClick,
            onTreadmillClick = onExerciseClick,
            onFreeRunClick = onExerciseClick
        )
        ExerciseSummaryCard(
            uiState = exerciseUiState.summary,
            onClick = onExerciseClick
        )
        WeightSummaryCard(
            uiState = weightUiState.weightCard,
            onClick = onWeightClick
        )
    }
}

@Composable
private fun HomeHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "每日运动",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = todayMonthDayText(),
                    color = AppSecondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "今天也动起来",
                color = AppSecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun todayMonthDayText(): String {
    return SimpleDateFormat("MM月dd日", Locale.CHINA).format(Calendar.getInstance().time)
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
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
