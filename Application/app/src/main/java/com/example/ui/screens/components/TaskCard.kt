package com.example.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.ApiTask
import com.example.ui.theme.*

@Composable
fun TaskCard(task: ApiTask, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    val statusColor = when (task.status.lowercase()) {
        "completed" -> StatusCompleted
        "inprogress" -> StatusInProgress
        else -> StatusPending
    }
    val statusBgColor = when (task.status.lowercase()) {
        "completed" -> StatusCompletedContainer
        "inprogress" -> StatusInProgressContainer
        else -> StatusPendingContainer
    }
    val priorityColor = when (task.priority.lowercase()) {
        "high"   -> PriorityHigh
        "medium" -> PriorityMedium
        else     -> PriorityLow
    }
    val priorityBgColor = when (task.priority.lowercase()) {
        "high"   -> PriorityHighContainer
        "medium" -> PriorityMediumContainer
        else     -> PriorityLowContainer
    }
    val statusLabel = when (task.status.lowercase()) {
        "inprogress" -> "In Progress"
        else -> task.status.replaceFirstChar { it.uppercase() }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundUser),
        border = BorderStroke(1.dp, CardBorderUser)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    task.projectName?.let { pn ->
                        Text(text = pn, style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(priorityBgColor, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = priorityColor
                        )
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .background(statusBgColor, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = statusColor
                    )
                }
                task.deadline?.let { dl ->
                    Text(text = "•", color = Slate500, style = MaterialTheme.typography.bodySmall)
                    Text(text = dl.take(10), color = Slate400, style = MaterialTheme.typography.bodySmall)
                }
                task.assignedTo?.let { at ->
                    Spacer(Modifier.weight(1f))
                    Text(text = at, color = Slate400, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
