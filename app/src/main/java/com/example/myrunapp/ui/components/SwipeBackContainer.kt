package com.example.myrunapp.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun SwipeBackContainer(
    enabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val edgeWidthPx = with(density) { 32.dp.toPx() }
    val triggerDistancePx = with(density) { 80.dp.toPx() }
    val maxVerticalDistancePx = with(density) { 52.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (!enabled) {
                    Modifier
                } else {
                    Modifier.pointerInput(onBack) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (down.position.x < size.width - edgeWidthPx) {
                                return@awaitEachGesture
                            }

                            var pointerId: PointerId = down.id
                            var totalX = 0f
                            var totalY = 0f
                            var triggered = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) break

                                val delta = change.positionChange()
                                totalX += delta.x
                                totalY += delta.y

                                if (abs(totalY) > maxVerticalDistancePx) {
                                    break
                                }

                                if (totalX <= -triggerDistancePx && !triggered) {
                                    triggered = true
                                    change.consume()
                                    onBack()
                                    break
                                }
                            }
                        }
                    }
                }
            )
    ) {
        content()
    }
}
