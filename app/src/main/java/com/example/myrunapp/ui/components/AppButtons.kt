package com.example.myrunapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.theme.Accent
import com.example.myrunapp.ui.theme.AppError
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.AppSurface

val AppButtonHeight = 52.dp
val AppButtonCornerRadius = 8.dp

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = AppButtonHeight
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(AppButtonCornerRadius),
        colors = ButtonDefaults.buttonColors(containerColor = Accent)
    ) {
        Text(
            text = text,
            color = Color(0xFF06130E),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = AppButtonHeight
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(AppButtonCornerRadius),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppSurface,
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, AppSecondaryText.copy(alpha = 0.35f))
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = AppButtonHeight
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(AppButtonCornerRadius),
        colors = ButtonDefaults.buttonColors(containerColor = AppError)
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AppBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SportBackButton(onClick = onClick, modifier = modifier)
}

@Composable
fun SportBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = Color(0xFF151E26).copy(alpha = 0.96f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "‹",
                color = Color(0xFF22C55E),
                fontWeight = FontWeight.Medium,
                fontSize = 28.sp,
                lineHeight = 28.sp
            )
        }
    }
}

@Composable
fun AppDialogButtonRow(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String,
    modifier: Modifier = Modifier
) {
    val dialogButtonHeight = 52.dp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppSecondaryButton(
            text = "取消",
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            height = dialogButtonHeight
        )
        AppPrimaryButton(
            text = confirmText,
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            height = dialogButtonHeight
        )
    }
}
