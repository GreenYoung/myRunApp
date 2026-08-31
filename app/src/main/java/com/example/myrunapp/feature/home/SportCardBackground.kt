package com.example.myrunapp.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun GradientSportCardBackground(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val green = Color(0xFF22C55E)
        drawCircle(
            color = green.copy(alpha = 0.08f),
            radius = size.width * 0.48f,
            center = Offset(size.width * 0.18f, size.height * 0.03f)
        )
        drawCircle(
            color = green.copy(alpha = 0.06f),
            radius = size.width * 0.42f,
            center = Offset(size.width * 0.86f, size.height * 0.38f)
        )

        val firstWave = Path().apply {
            moveTo(-size.width * 0.08f, size.height * 0.38f)
            cubicTo(
                size.width * 0.18f,
                size.height * 0.24f,
                size.width * 0.48f,
                size.height * 0.48f,
                size.width * 1.08f,
                size.height * 0.26f
            )
        }
        val secondWave = Path().apply {
            moveTo(-size.width * 0.04f, size.height * 0.55f)
            cubicTo(
                size.width * 0.26f,
                size.height * 0.42f,
                size.width * 0.58f,
                size.height * 0.68f,
                size.width * 1.04f,
                size.height * 0.48f
            )
        }
        drawPath(
            path = firstWave,
            color = green.copy(alpha = 0.15f),
            style = Stroke(width = 18.dp.toPx())
        )
        drawPath(
            path = secondWave,
            color = green.copy(alpha = 0.08f),
            style = Stroke(width = 28.dp.toPx())
        )
    }
}
