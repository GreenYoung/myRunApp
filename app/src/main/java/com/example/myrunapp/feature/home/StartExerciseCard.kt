package com.example.myrunapp.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.theme.MyRunAppTheme

private val StartGreen = Color(0xFF0BDA51)
private val StartCyan = Color(0xFF22D3EE)
private val StartCardDark = Color(0xFF111820)

@Composable
fun StartExerciseCard(
    onStartClick: () -> Unit,
    onOutdoorClick: () -> Unit,
    onTreadmillClick: () -> Unit,
    onFreeRunClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(198.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF151E26), StartCardDark),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
        ) {
            StartExerciseBackground(modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(11.dp, Alignment.CenterVertically)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "开始运动",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                RunButton(onClick = onStartClick)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StartModeItem(text = "户外跑步", mark = "OUT", onClick = onOutdoorClick, modifier = Modifier.weight(1f))
                    StartModeItem(text = "跑步机", mark = "IN", onClick = onTreadmillClick, modifier = Modifier.weight(1f))
                    StartModeItem(text = "自由跑", mark = "RUN", onClick = onFreeRunClick, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RunButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(88.dp)
            .background(StartGreen.copy(alpha = 0.15f), CircleShape)
            .padding(6.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(StartGreen, Color(0xFF84CC16)),
                    start = Offset.Zero,
                    end = Offset.Infinite
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "▶",
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "开始",
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StartModeItem(
    text: String,
    mark: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(42.dp)
            .background(Color(0x990C1218), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StartExerciseBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(
            color = StartGreen.copy(alpha = 0.08f),
            radius = size.width * 0.42f,
            center = Offset(size.width * 0.10f, size.height * 0.04f)
        )
        drawCircle(
            color = StartCyan.copy(alpha = 0.05f),
            radius = size.width * 0.52f,
            center = Offset(size.width * 0.95f, size.height * 0.48f)
        )

        val speedLine = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.70f)
            cubicTo(
                size.width * 0.26f,
                size.height * 0.56f,
                size.width * 0.58f,
                size.height * 0.82f,
                size.width * 0.94f,
                size.height * 0.58f
            )
        }
        drawPath(
            path = speedLine,
            color = StartGreen.copy(alpha = 0.16f),
            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
        )
        drawLine(
            color = StartCyan.copy(alpha = 0.18f),
            start = Offset(size.width * 0.62f, size.height * 0.30f),
            end = Offset(size.width * 0.90f, size.height * 0.18f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartExerciseCardPreview() {
    MyRunAppTheme(darkTheme = true) {
        StartExerciseCard(
            onStartClick = {},
            onOutdoorClick = {},
            onTreadmillClick = {},
            onFreeRunClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
