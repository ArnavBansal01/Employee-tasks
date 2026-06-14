package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.ApiTask
import com.example.ui.screens.components.TaskCard
import com.example.ui.viewmodel.TaskTrackerViewModel
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: TaskTrackerViewModel,
    onTaskClick: (String) -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val loading by viewModel.tasksLoading.collectAsState()
    val authInfo by viewModel.authInfo.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchTasks(pageSize = 1000)
    }

    val total     = tasks.size
    val pending   = tasks.count { it.status.equals("Pending", ignoreCase = true) }
    val inProg    = tasks.count { it.status.equals("InProgress", ignoreCase = true) }
    val completed = tasks.count { it.status.equals("Completed", ignoreCase = true) }
    val recent    = remember(tasks) {
        tasks.sortedWith(
            compareByDescending<ApiTask> { viewModel.getTaskUpdateTime(it.id) }
                .thenByDescending { it.id }
        ).take(5)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400
                )
                Text(
                    text = authInfo?.name ?: "...",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (authInfo?.isAdmin == true) {
                    Surface(
                        color = AccentBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Admin",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentBlue
                        )
                    }
                }
            }
        }

        // Stat cards
        item {
            if (loading) {
                Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Total Tasks", total.toString(), Blue500, StatusInProgressContainer, Modifier.weight(1f))
                        StatCard("Pending", pending.toString(), StatusPending, StatusPendingContainer, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("In Progress", inProg.toString(), StatusInProgress, StatusInProgressContainer, Modifier.weight(1f))
                        StatCard("Completed", completed.toString(), StatusCompleted, StatusCompletedContainer, Modifier.weight(1f))
                    }
                }
            }
        }

        // Recent tasks header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT TASKS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = Slate300
                )
            }
        }

        if (recent.isEmpty() && !loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No tasks yet", color = Slate400)
                }
            }
        }

        items(recent) { task ->
            TaskCard(task = task, onClick = { onTaskClick(task.id.toString()) })
        }
    }
}

@Composable
fun StatCard(title: String, value: String, dotColor: Color, dotBgColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StatCardBackground),
        border = BorderStroke(1.dp, StatCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium, letterSpacing = 1.sp
                ),
                color = Slate400
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier.size(32.dp).background(dotBgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
                }
            }
        }
    }
}


// Extension used to get status color for ApiTask
fun ApiTask.statusColor() = when (status.lowercase()) {
    "completed" -> StatusCompleted
    "inprogress" -> StatusInProgress
    else -> StatusPending
}
