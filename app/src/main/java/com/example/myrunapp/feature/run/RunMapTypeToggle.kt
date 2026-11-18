package com.example.myrunapp.feature.run

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MapToggleBackground = Color(0x99111820)
private val MapToggleAccent = Color(0xFF22C55E)

@Composable
fun RunMapTypeToggle(
    mapDisplayType: RunMapDisplayType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 用可点击 Surface：ripple 会按圆角裁剪，比在外层套 clickable 更干净
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MapToggleBackground,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = mapDisplayType.label,
            color = MapToggleAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
        )
    }
}
