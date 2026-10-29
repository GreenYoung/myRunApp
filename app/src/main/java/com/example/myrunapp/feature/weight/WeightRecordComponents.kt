package com.example.myrunapp.feature.weight

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.components.AppDangerButton
import com.example.myrunapp.ui.components.AppPrimaryButton
import com.example.myrunapp.ui.components.AppSecondaryButton
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.AppSurface

private val RecordRowMinHeight = 38.dp

@Composable
internal fun RecentWeightRecords(
    records: List<WeightRecordItem>,
    onEditRecord: (WeightRecordItem) -> Unit,
    onDeleteRecord: (WeightRecordItem) -> Unit
) {
    var actionRecord by remember { mutableStateOf<WeightRecordItem?>(null) }
    var pendingDeleteRecord by remember { mutableStateOf<WeightRecordItem?>(null) }
    var expandedDate by remember { mutableStateOf<String?>(null) }

    DetailCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "体重记录",
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold
            )
            if (records.isEmpty()) {
                Text("暂无记录", color = AppSecondaryText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                return@Column
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                items(records, key = { it.date }) { record ->
                    WeightRecordRow(
                        record = record,
                        expanded = expandedDate == record.date,
                        onClick = {
                            expandedDate = if (expandedDate == record.date) null else record.date
                        },
                        onLongClick = { actionRecord = record },
                        onEdit = { onEditRecord(record) },
                        onDelete = { pendingDeleteRecord = record }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f), thickness = 1.dp)
                }
            }
        }
    }

    actionRecord?.let { record ->
        WeightRecordActionDialog(
            record = record,
            onDismiss = { actionRecord = null },
            onEdit = {
                actionRecord = null
                onEditRecord(record)
            },
            onDelete = {
                actionRecord = null
                pendingDeleteRecord = record
            }
        )
    }

    pendingDeleteRecord?.let { record ->
        DeleteWeightRecordDialog(
            record = record,
            onDismiss = { pendingDeleteRecord = null },
            onDelete = {
                onDeleteRecord(record)
                pendingDeleteRecord = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeightRecordRow(
    record: WeightRecordItem,
    expanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .defaultMinSize(minHeight = RecordRowMinHeight)
            .padding(vertical = 0.dp),
        verticalArrangement = if (expanded) Arrangement.spacedBy(6.dp) else Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = record.date,
                modifier = Modifier.weight(1.45f),
                color = AppSecondaryText,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = "${formatWeight(record.weightKg)} kg",
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = record.previousChange.toArrowChangeText(unit = false),
                modifier = Modifier.weight(0.85f),
                color = record.previousChange.toChangeColor(),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        if (expanded) {
            WeightRecordExpandedContent(onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
private fun WeightRecordExpandedContent(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppSecondaryButton(
                text = "编辑",
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                height = 42.dp
            )
            AppDangerButton(
                text = "删除",
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeightRecordActionDialog(
    record: WeightRecordItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("记录操作", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "${record.date} · ${formatWeight(record.weightKg)} kg",
                color = AppSecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppSecondaryButton(text = "取消", onClick = onDismiss, modifier = Modifier.weight(1f), height = 52.dp)
                AppPrimaryButton(text = "编辑", onClick = onEdit, modifier = Modifier.weight(1f), height = 52.dp)
                AppDangerButton(text = "删除", onClick = onDelete, modifier = Modifier.weight(1f))
            }
        }
    )
}

@Composable
private fun DeleteWeightRecordDialog(
    record: WeightRecordItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("删除体重记录", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "${record.date} · ${formatWeight(record.weightKg)} kg",
                color = AppSecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppSecondaryButton(text = "取消", onClick = onDismiss, modifier = Modifier.weight(1f), height = 52.dp)
                AppDangerButton(text = "删除", onClick = onDelete, modifier = Modifier.weight(1f))
            }
        }
    )
}
