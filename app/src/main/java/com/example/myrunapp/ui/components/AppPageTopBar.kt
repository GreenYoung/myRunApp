package com.example.myrunapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val PageHorizontalPadding = 16.dp
val PageTopSpacing = 0.dp
val PageTopBarHeight = 44.dp

@Composable
fun AppPageTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PageTopBarHeight),
        contentAlignment = Alignment.Center
    ) {
        if (showBackButton && onBackClick != null) {
            AppBackButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
