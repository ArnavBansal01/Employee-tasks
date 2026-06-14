package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.api.ApiUser
import com.example.data.api.ApiTask
import com.example.data.api.CreateTaskRequest
import com.example.data.api.UpdateProjectRequest
import com.example.ui.viewmodel.TaskTrackerViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    viewModel: TaskTrackerViewModel,
    onTaskClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val project      by viewModel.selectedProject.collectAsState()
    val members      by viewModel.projectMembers.collectAsState()
    val projectTasks by viewModel.projectTasks.collectAsState()
    val authInfo     by viewModel.authInfo.collectAsState()
    val allUsers     by viewModel.users.collectAsState()
    val projectOp    by viewModel.projectOpState.collectAsState()
    val taskOpState  by viewModel.taskOpState.collectAsState()
    val isDarkTheme  by viewModel.themeIsDark.collectAsState()

    var showEditDialog    by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMemberDialog  by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }

    val id = projectId.toIntOrNull() ?: return

    LaunchedEffect(id) {
        viewModel.fetchProjectById(id)
        viewModel.fetchProjectMembers(id)
        viewModel.fetchProjectTasks(id)
        viewModel.fetchUsers() // Pre-load all users for membership search
    }

    LaunchedEffect(projectOp) {
        if (projectOp is UiState.Success) {
            showEditDialog   = false
            showMemberDialog = false
            viewModel.resetProjectOpState()
        }
    }

    LaunchedEffect(taskOpState) {
        if (taskOpState is UiState.Success) {
            showCreateTaskDialog = false
            viewModel.resetTaskOpState()
            viewModel.fetchProjectTasks(id) // Reload tasks
        }
    }

    if (project == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentBlue)
        }
        return
    }

    val isAdmin = authInfo?.isAdmin == true
    val formattedDeadline = project!!.deadline?.take(10) ?: "No deadline"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Slate900 else Slate50),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back link
        item {
            Row(
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Slate500,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Back to Projects",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Slate500
                )
            }
        }

        // Project Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Slate800 else White
                ),
                border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = project!!.name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isDarkTheme) White else Slate900
                            )
                            Text(
                                text = project!!.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate400
                            )
                        }
                        if (isAdmin) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { showEditDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Project", tint = Slate400)
                                }
                                IconButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Project", tint = Slate400)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = if (isDarkTheme) Slate700 else Slate100)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.DateRange, contentDescription = "Deadline", tint = Slate500, modifier = Modifier.size(16.dp))
                            Text(formattedDeadline, style = MaterialTheme.typography.bodySmall, color = Slate400)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.List, contentDescription = "Tasks", tint = Slate500, modifier = Modifier.size(16.dp))
                            Text("${projectTasks.size} tasks", style = MaterialTheme.typography.bodySmall, color = Slate400)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Group, contentDescription = "Members", tint = Slate500, modifier = Modifier.size(16.dp))
                            Text("${members.size} members", style = MaterialTheme.typography.bodySmall, color = Slate400)
                        }
                    }
                }
            }
        }

        // Members Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Slate800 else White
                ),
                border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Members",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDarkTheme) White else Slate900
                        )
                        if (isAdmin) {
                            Row(
                                modifier = Modifier
                                    .clickable { showMemberDialog = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Member", tint = Blue600, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Blue600)
                            }
                        }
                    }

                    HorizontalDivider(color = if (isDarkTheme) Slate700 else Slate100)

                    if (members.isEmpty()) {
                        Text("No members assigned.", color = Slate400, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            members.forEach { m ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Blue500.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = m.name.take(1).uppercase(),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Blue600
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = m.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = if (isDarkTheme) White else Slate900
                                            )
                                            Text(
                                                text = m.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Slate400
                                            )
                                        }
                                    }
                                    if (isAdmin) {
                                        IconButton(
                                            onClick = {
                                                val remainingIds = members.filter { it.idOrUserId != m.idOrUserId }.map { it.idOrUserId }
                                                viewModel.replaceProjectMembers(id, remainingIds) {}
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.PersonRemove, contentDescription = "Remove Member", tint = Slate400)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tasks Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Slate800 else White
                ),
                border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tasks",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDarkTheme) White else Slate900
                        )
                        if (isAdmin) {
                            Row(
                                modifier = Modifier
                                    .clickable { showCreateTaskDialog = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Blue600, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Task", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Blue600)
                            }
                        }
                    }

                    HorizontalDivider(color = if (isDarkTheme) Slate700 else Slate100)

                    if (projectTasks.isEmpty()) {
                        Text("No tasks created for this project yet.", color = Slate400, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        // Horizontal scrollable table layout to match website responsive table
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier.width(600.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Table Header
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("TITLE", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = Slate400, style = MaterialTheme.typography.labelSmall)
                                    Text("ASSIGNED TO", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Slate400, style = MaterialTheme.typography.labelSmall)
                                    Text("STATUS", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, color = Slate400, style = MaterialTheme.typography.labelSmall)
                                    Text("PRIORITY", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Slate400, style = MaterialTheme.typography.labelSmall)
                                    Text("DEADLINE", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, color = Slate400, style = MaterialTheme.typography.labelSmall)
                                }

                                HorizontalDivider(color = if (isDarkTheme) Slate700 else Slate100)

                                // Table Rows
                                projectTasks.forEach { t ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = t.title,
                                            modifier = Modifier
                                                .weight(2f)
                                                .clickable { onTaskClick(t.id.toString()) },
                                            color = Blue600,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = t.assignedTo ?: "Unassigned",
                                            modifier = Modifier.weight(1.5f),
                                            color = if (isDarkTheme) Slate300 else Slate700,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Box(modifier = Modifier.weight(1.2f)) {
                                            StatusBadge(status = t.status)
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            PriorityBadge(priority = t.priority)
                                        }
                                        Text(
                                            text = t.deadline?.take(10) ?: "-",
                                            modifier = Modifier.weight(1.2f),
                                            color = if (isDarkTheme) Slate300 else Slate700,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Add Project Member Modal
    if (showMemberDialog && isAdmin) {
        AddMemberDialog(
            members = members,
            allUsers = allUsers,
            viewModel = viewModel,
            projectId = id,
            onDismiss = { showMemberDialog = false }
        )
    }

    // 2. Edit Project Modal
    if (showEditDialog && isAdmin) {
        EditProjectDialog(
            project = project!!,
            viewModel = viewModel,
            onDismiss = { showEditDialog = false }
        )
    }

    // 3. Delete Project Modal
    if (showDeleteConfirm && isAdmin) {
        DeleteProjectDialog(
            project = project!!,
            viewModel = viewModel,
            onDismiss = { showDeleteConfirm = false },
            onDeleteSuccess = {
                showDeleteConfirm = false
                onBack()
            }
        )
    }

    // 4. Create Task Modal
    if (showCreateTaskDialog && isAdmin) {
        CreateTaskDialog(
            projectId = id,
            viewModel = viewModel,
            onDismiss = { showCreateTaskDialog = false }
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val statusLower = status.lowercase()
    val color = when (statusLower) {
        "completed" -> StatusCompleted
        "inprogress" -> StatusInProgress
        else -> StatusPending
    }
    val bgColor = when (statusLower) {
        "completed" -> StatusCompletedContainer
        "inprogress" -> StatusInProgressContainer
        else -> StatusPendingContainer
    }
    val label = when (statusLower) {
        "inprogress" -> "In Progress"
        else -> status.replaceFirstChar { it.uppercase() }
    }

    Surface(
        color = bgColor,
        shape = CircleShape
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = color
        )
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val priorityLower = priority.lowercase()
    val color = when (priorityLower) {
        "high" -> PriorityHigh
        "medium" -> PriorityMedium
        else -> PriorityLow
    }
    val bgColor = when (priorityLower) {
        "high" -> PriorityHighContainer
        "medium" -> PriorityMediumContainer
        else -> PriorityLowContainer
    }

    Surface(
        color = bgColor,
        shape = CircleShape
    ) {
        Text(
            text = priority.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
            color = color
        )
    }
}

@Composable
private fun AddMemberDialog(
    members: List<ApiUser>,
    allUsers: List<ApiUser>,
    viewModel: TaskTrackerViewModel,
    projectId: Int,
    onDismiss: () -> Unit
) {
    val projectOp by viewModel.projectOpState.collectAsState()
    val isDarkTheme by viewModel.themeIsDark.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val selectedUserIds = remember { mutableStateListOf<Int>() }

    val nonMembers = allUsers.filter { u -> !members.any { m -> m.idOrUserId == u.idOrUserId } }
    val filteredNonMembers = nonMembers.filter { u ->
        u.name.contains(searchQuery, ignoreCase = true) ||
        u.email.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Slate900 else White
            ),
            border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Project Member",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkTheme) White else Slate900
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or email...", color = Slate500) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDarkTheme) White else Slate900,
                        unfocusedTextColor = if (isDarkTheme) White else Slate900,
                        focusedBorderColor = Blue500,
                        unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                        focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                        unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                    )
                )

                // List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, if (isDarkTheme) Slate800 else Slate200, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (filteredNonMembers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (nonMembers.isEmpty()) "All users are already in this project." else "No users match your search.",
                                    color = Slate400,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        items(filteredNonMembers) { u ->
                            val isChecked = selectedUserIds.contains(u.idOrUserId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isChecked) Blue500.copy(alpha = 0.05f) else Color.Transparent)
                                    .clickable {
                                        if (isChecked) selectedUserIds.remove(u.idOrUserId) else selectedUserIds.add(u.idOrUserId)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked == true) selectedUserIds.add(u.idOrUserId) else selectedUserIds.remove(u.idOrUserId)
                                        }
                                    )
                                    Column {
                                        Text(u.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = if (isDarkTheme) White else Slate900)
                                        Text(u.email, style = MaterialTheme.typography.bodySmall, color = Slate400)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val submitLoading = projectOp is UiState.Loading
                    TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp), enabled = !submitLoading) {
                        Text("Cancel", color = if (isDarkTheme) Slate300 else Slate700)
                    }
                    Button(
                        onClick = {
                            viewModel.addProjectMembers(projectId, selectedUserIds.toList()) {
                                onDismiss()
                            }
                        },
                        enabled = selectedUserIds.isNotEmpty() && !submitLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        if (submitLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Adding...")
                        } else {
                            Text("Add Selected (${selectedUserIds.size})")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProjectDialog(
    project: com.example.data.api.ApiProject,
    viewModel: TaskTrackerViewModel,
    onDismiss: () -> Unit
) {
    val projectOp by viewModel.projectOpState.collectAsState()
    val isDarkTheme by viewModel.themeIsDark.collectAsState()
    val context = LocalContext.current

    var name by remember { mutableStateOf(project.name) }
    var description by remember { mutableStateOf(project.description) }
    var deadline by remember { mutableStateOf(project.deadline?.take(10) ?: "") }
    var submitError by remember { mutableStateOf("") }

    val datePickerDialog = remember {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                deadline = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    LaunchedEffect(projectOp) {
        if (projectOp is UiState.Error) {
            submitError = (projectOp as UiState.Error).message
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Slate900 else White),
            border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Project",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkTheme) White else Slate900
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

                if (submitError.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(submitError, modifier = Modifier.padding(12.dp), color = Color(0xFFB91C1C), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("PROJECT NAME", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDarkTheme) White else Slate900,
                                unfocusedTextColor = if (isDarkTheme) White else Slate900,
                                focusedBorderColor = Blue500,
                                unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                            )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("DESCRIPTION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDarkTheme) White else Slate900,
                                unfocusedTextColor = if (isDarkTheme) White else Slate900,
                                focusedBorderColor = Blue500,
                                unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                            )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("TARGET DEADLINE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                        Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
                            OutlinedTextField(
                                value = deadline,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                    disabledContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                    disabledTextColor = if (isDarkTheme) White else Slate900
                                ),
                                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Slate400) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    val submitLoading = projectOp is UiState.Loading
                    TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp), enabled = !submitLoading) {
                        Text("Cancel", color = if (isDarkTheme) Slate300 else Slate700)
                    }
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                submitError = "Project Name is required."
                                return@Button
                            }
                            viewModel.updateProject(project.id, UpdateProjectRequest(name, description, deadline.ifBlank { null })) {}
                        },
                        enabled = !submitLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        if (submitLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteProjectDialog(
    project: com.example.data.api.ApiProject,
    viewModel: TaskTrackerViewModel,
    onDismiss: () -> Unit,
    onDeleteSuccess: () -> Unit
) {
    val projectOp by viewModel.projectOpState.collectAsState()
    val isDarkTheme by viewModel.themeIsDark.collectAsState()
    var submitLoading by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Slate900 else White),
            border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFEF2F2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                }

                Text(
                    text = "Delete Project?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDarkTheme) White else Slate900
                )

                Text(
                    text = "Are you sure you want to delete this project? All associated tasks and assignments will be permanently removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (submitError.isNotEmpty()) {
                    Text(submitError, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !submitLoading
                    ) {
                        Text("Cancel", color = if (isDarkTheme) Slate300 else Slate700)
                    }

                    Button(
                        onClick = {
                            submitLoading = true
                            viewModel.deleteProject(project.id) {
                                submitLoading = false
                                onDeleteSuccess()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !submitLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        if (submitLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Deleting...")
                        } else {
                            Text("Delete Permanently")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskDialog(
    projectId: Int,
    viewModel: TaskTrackerViewModel,
    onDismiss: () -> Unit
) {
    val taskOpState by viewModel.taskOpState.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val projectMembers by viewModel.projectMembers.collectAsState()
    val isDarkTheme by viewModel.themeIsDark.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Pending") }
    var selectedPriority by remember { mutableStateOf("Medium") }
    var selectedProjectId by remember { mutableIntStateOf(projectId) }
    var selectedUserId by remember { mutableIntStateOf(0) }
    var deadline by remember { mutableStateOf("") }
    var submitError by remember { mutableStateOf("") }

    var showAssigneeSelectDialog by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    val datePickerDialog = remember {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                deadline = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    LaunchedEffect(Unit) {
        viewModel.fetchProjects()
    }

    LaunchedEffect(selectedProjectId) {
        if (selectedProjectId != 0) {
            viewModel.fetchProjectMembers(selectedProjectId)
            selectedUserId = 0
        }
    }

    LaunchedEffect(taskOpState) {
        if (taskOpState is UiState.Error) {
            submitError = (taskOpState as UiState.Error).message
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Slate900 else White),
            border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Task",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkTheme) White else Slate900
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

                if (submitError.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(submitError, modifier = Modifier.padding(12.dp), color = Color(0xFFB91C1C), style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Scrollable container for the fields
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("TASK TITLE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                placeholder = { Text("Enter task title", color = Slate500) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = if (isDarkTheme) White else Slate900,
                                    unfocusedTextColor = if (isDarkTheme) White else Slate900,
                                    focusedBorderColor = Blue500,
                                    unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                    focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                    unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                                )
                            )
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("DESCRIPTION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                placeholder = { Text("Enter task description", color = Slate500) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = if (isDarkTheme) White else Slate900,
                                    unfocusedTextColor = if (isDarkTheme) White else Slate900,
                                    focusedBorderColor = Blue500,
                                    unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                    focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                    unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                                )
                            )
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("PROJECT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                            var projectSearch by remember { mutableStateOf("") }
                            var projectMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val projName = projects.find { it.id == selectedProjectId }?.name ?: "Select Project"
                                OutlinedTextField(
                                    value = projName,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = if (isDarkTheme) White else Slate900,
                                        unfocusedTextColor = if (isDarkTheme) White else Slate900,
                                        focusedBorderColor = Blue500,
                                        unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                        focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                        unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { projectMenuExpanded = true }
                                )

                                DropdownMenu(
                                    expanded = projectMenuExpanded,
                                    onDismissRequest = { projectMenuExpanded = false; projectSearch = "" },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .background(if (isDarkTheme) Slate900 else White)
                                        .border(1.dp, if (isDarkTheme) Slate800 else Slate200, RoundedCornerShape(8.dp))
                                ) {
                                    OutlinedTextField(
                                        value = projectSearch,
                                        onValueChange = { projectSearch = it },
                                        placeholder = { Text("Search...", style = MaterialTheme.typography.bodySmall, color = Slate500) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(6.dp),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = if (isDarkTheme) White else Slate900,
                                            unfocusedTextColor = if (isDarkTheme) White else Slate900,
                                            focusedBorderColor = Blue500,
                                            unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                            focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                            unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                                        )
                                    )

                                    val filteredProjects = projects.filter { it.name.contains(projectSearch, ignoreCase = true) }
                                    filteredProjects.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text(p.name, color = if (isDarkTheme) White else Slate900) },
                                            onClick = {
                                                selectedProjectId = p.id
                                                projectMenuExpanded = false
                                                projectSearch = ""
                                            }
                                        )
                                    }
                                    if (filteredProjects.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No projects found", color = Slate500, style = MaterialTheme.typography.bodySmall) },
                                            onClick = {}
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val isAssigneeEnabled = selectedProjectId != 0
                            val userName = if (!isAssigneeEnabled) {
                                "Select a project first"
                            } else {
                                projectMembers.find { it.idOrUserId == selectedUserId }?.name ?: "Select Assignee"
                            }
                            Text("ASSIGNEE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isAssigneeEnabled) { showAssigneeSelectDialog = true }
                            ) {
                                OutlinedTextField(
                                    value = userName,
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                        disabledContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                        disabledTextColor = if (isAssigneeEnabled) (if (isDarkTheme) White else Slate900) else Slate500,
                                        disabledPlaceholderColor = Slate500
                                    ),
                                    trailingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = if (isAssigneeEnabled) Slate400 else Slate400.copy(alpha = 0.5f)) }
                                )
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("STATUS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                                OutlinedTextField(
                                    value = if (selectedStatus == "InProgress") "In Progress" else selectedStatus,
                                    onValueChange = {}, readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = if (isDarkTheme) White else Slate900,
                                        unfocusedTextColor = if (isDarkTheme) White else Slate900,
                                        focusedBorderColor = Blue500,
                                        unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                        focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                        unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                                    )
                                )
                                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                                    listOf("Pending", "InProgress", "Completed").forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(if (s == "InProgress") "In Progress" else s) },
                                            onClick = { selectedStatus = s; statusExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("PRIORITY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                            ExposedDropdownMenuBox(expanded = priorityExpanded, onExpandedChange = { priorityExpanded = it }) {
                                OutlinedTextField(
                                    value = selectedPriority, onValueChange = {}, readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(priorityExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = if (isDarkTheme) White else Slate900,
                                        unfocusedTextColor = if (isDarkTheme) White else Slate900,
                                        focusedBorderColor = Blue500,
                                        unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                        focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                        unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                                    )
                                )
                                ExposedDropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                                    listOf("Low", "Medium", "High").forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text(p) },
                                            onClick = { selectedPriority = p; priorityExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("TARGET DEADLINE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                            Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
                                OutlinedTextField(
                                    value = deadline,
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    placeholder = { Text("Select deadline date", color = Slate500) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                        disabledContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                        disabledTextColor = if (isDarkTheme) White else Slate900,
                                        disabledPlaceholderColor = Slate500
                                    ),
                                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Slate400) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    val submitLoading = taskOpState is UiState.Loading
                    TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp), enabled = !submitLoading) {
                        Text("Cancel", color = if (isDarkTheme) Slate300 else Slate700)
                    }
                    Button(
                        onClick = {
                            if (title.isBlank() || description.isBlank()) {
                                submitError = "Title and Description are required."
                                return@Button
                            }
                            viewModel.createTask(
                                CreateTaskRequest(
                                    title = title,
                                    description = description,
                                    status = selectedStatus,
                                    priority = selectedPriority,
                                    deadline = deadline.ifBlank { null },
                                    userId = selectedUserId,
                                    projectId = selectedProjectId
                                )
                            ) {}
                        },
                        enabled = !submitLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        if (submitLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Creating...")
                        } else {
                            Text("Create Task")
                        }
                    }
                }
            }
        }
    }

    if (showAssigneeSelectDialog) {
        AssigneeSelectDialog(
            members = projectMembers,
            selectedUserId = selectedUserId,
            isDarkTheme = isDarkTheme,
            onDismiss = { showAssigneeSelectDialog = false },
            onSelect = { userId ->
                selectedUserId = userId
                showAssigneeSelectDialog = false
            }
        )
    }
}

@Composable
private fun AssigneeSelectDialog(
    members: List<ApiUser>,
    selectedUserId: Int,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = members.filter { u ->
        u.name.contains(searchQuery, ignoreCase = true) ||
        u.email.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 450.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Slate900 else White),
            border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Assignee",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkTheme) White else Slate900
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search users...", color = Slate500) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDarkTheme) White else Slate900,
                        unfocusedTextColor = if (isDarkTheme) White else Slate900,
                        focusedBorderColor = Blue500,
                        unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                        focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                        unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                    )
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, if (isDarkTheme) Slate800 else Slate200, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (filteredUsers.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                Text("No users found", color = Slate400)
                            }
                        }
                    } else {
                        items(filteredUsers) { u ->
                            val isSelected = u.idOrUserId == selectedUserId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Blue500.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { onSelect(u.idOrUserId) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(u.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = if (isDarkTheme) White else Slate900)
                                    Text(u.email, style = MaterialTheme.typography.bodySmall, color = Slate400)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = Blue600, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
