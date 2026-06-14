package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.ApiProject
import com.example.data.api.ApiTask
import com.example.ui.viewmodel.TaskTrackerViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.theme.*

@Composable
fun SearchableProjectSelect(
    options: List<ApiProject>,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    isDarkTheme: Boolean
) {
    var isOpen by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    val selectedLabel = options.find { it.name == value }?.name ?: placeholder
    val filtered = options.filter { it.name.contains(search, ignoreCase = true) }

    Box(modifier = Modifier.width(180.dp)) {
        // Main trigger button
        OutlinedCard(
            onClick = { isOpen = !isOpen },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (isDarkTheme) Slate950 else Slate50
            ),
            border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) White else Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false; search = "" },
            modifier = Modifier
                .width(180.dp)
                .background(if (isDarkTheme) Slate900 else White)
                .border(1.dp, if (isDarkTheme) Slate800 else Slate200, RoundedCornerShape(8.dp))
        ) {
            // Search Input inside Dropdown
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search...", style = MaterialTheme.typography.bodySmall, color = Slate500) },
                    modifier = Modifier.fillMaxWidth(),
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
            }

            DropdownMenuItem(
                text = { Text("All Projects", style = MaterialTheme.typography.bodyMedium, color = if (isDarkTheme) Slate300 else Slate700) },
                onClick = {
                    onChange("")
                    isOpen = false
                    search = ""
                }
            )

            filtered.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.name, style = MaterialTheme.typography.bodyMedium, color = if (isDarkTheme) Slate300 else Slate700) },
                    onClick = {
                        onChange(p.name)
                        isOpen = false
                        search = ""
                    }
                )
            }
        }
    }
}

@Composable
fun FilterSelectDropdown(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onChange: (String) -> Unit,
    isDarkTheme: Boolean
) {
    var isOpen by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == value }?.second ?: label

    Box(modifier = Modifier.width(140.dp)) {
        OutlinedCard(
            onClick = { isOpen = !isOpen },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (isDarkTheme) Slate950 else Slate50
            ),
            border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) White else Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false },
            modifier = Modifier
                .width(140.dp)
                .background(if (isDarkTheme) Slate900 else White)
                .border(1.dp, if (isDarkTheme) Slate800 else Slate200, RoundedCornerShape(8.dp))
        ) {
            options.forEach { (valStr, displayLabel) ->
                DropdownMenuItem(
                    text = { Text(displayLabel, style = MaterialTheme.typography.bodyMedium, color = if (isDarkTheme) Slate300 else Slate700) },
                    onClick = {
                        onChange(valStr)
                        isOpen = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(
    viewModel: TaskTrackerViewModel,
    onTaskClick: (String) -> Unit,
    onNewTaskClick: () -> Unit
) {
    val tasks        by viewModel.tasks.collectAsState()
    val loading      by viewModel.tasksLoading.collectAsState()
    val authInfo     by viewModel.authInfo.collectAsState()
    val projects     by viewModel.projects.collectAsState()
    val isDarkTheme  by viewModel.themeIsDark.collectAsState()

    // Filters (Local states)
    var statusFilter   by remember { mutableStateOf<String?>(null) }
    var priorityFilter by remember { mutableStateOf<String?>(null) }
    var projectFilter  by remember { mutableStateOf("") }

    // Pagination
    var currentPage    by remember { mutableIntStateOf(1) }
    val PAGE_SIZE = 8

    // Fetch all tasks once (with large page size) to support clean local filtering
    LaunchedEffect(Unit) {
        viewModel.fetchTasks(page = 1, pageSize = 1000)
        viewModel.fetchProjects()
    }

    // Client-side filtering logic
    val filteredTasks = remember(tasks, statusFilter, priorityFilter, projectFilter) {
        tasks.filter { t ->
            val statusMatch = statusFilter == null || t.status.equals(statusFilter, ignoreCase = true)
            val priorityMatch = priorityFilter == null || t.priority.equals(priorityFilter, ignoreCase = true)
            val projectMatch = projectFilter.isEmpty() || t.projectName?.equals(projectFilter, ignoreCase = true) == true
            statusMatch && priorityMatch && projectMatch
        }
    }

    // Client-side pagination logic
    val totalFilteredPages = maxOf(1, (filteredTasks.size + PAGE_SIZE - 1) / PAGE_SIZE)
    val safePage = minOf(currentPage, totalFilteredPages)
    
    val paginatedTasks = remember(filteredTasks, safePage) {
        val startIndex = (safePage - 1) * PAGE_SIZE
        val endIndex = minOf(startIndex + PAGE_SIZE, filteredTasks.size)
        if (startIndex < filteredTasks.size) {
            filteredTasks.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    // Reset page on filter changes
    LaunchedEffect(statusFilter, priorityFilter, projectFilter) {
        currentPage = 1
    }

    Scaffold(
        containerColor = if (isDarkTheme) Slate950 else Slate50
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDarkTheme) White else Slate900
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${filteredTasks.size} task${if (filteredTasks.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400
                    )
                    if (authInfo?.isAdmin == true) {
                        Button(
                            onClick = onNewTaskClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue,
                                contentColor = White
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "New Task",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            // Card Container for Filters and List
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Slate900 else White
                ),
                border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Filters wrapping row using FlowRow
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        maxItemsInEachRow = Int.MAX_VALUE
                    ) {
                        // Project Selector
                        SearchableProjectSelect(
                            options = projects,
                            value = projectFilter,
                            onChange = { projectFilter = it },
                            placeholder = "Filter by Project",
                            isDarkTheme = isDarkTheme
                        )

                        // Status Selector
                        FilterSelectDropdown(
                            label = "All Statuses",
                            value = statusFilter ?: "",
                            options = listOf(
                                "" to "All Statuses",
                                "Pending" to "Pending",
                                "InProgress" to "In Progress",
                                "Completed" to "Completed"
                            ),
                            onChange = { statusFilter = it.ifEmpty { null } },
                            isDarkTheme = isDarkTheme
                        )

                        // Priority Selector
                        FilterSelectDropdown(
                            label = "All Priorities",
                            value = priorityFilter ?: "",
                            options = listOf(
                                "" to "All Priorities",
                                "Low" to "Low",
                                "Medium" to "Medium",
                                "High" to "High"
                            ),
                            onChange = { priorityFilter = it.ifEmpty { null } },
                            isDarkTheme = isDarkTheme
                        )

                        // Clear filters text button
                        if (statusFilter != null || priorityFilter != null || projectFilter.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    statusFilter = null
                                    priorityFilter = null
                                    projectFilter = ""
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text("Clear filters", color = Blue500, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            }
                        }
                    }

                    HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

                    // Tasks List
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (loading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AccentBlue)
                            }
                        } else if (paginatedTasks.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No tasks found.", color = Slate400, style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(paginatedTasks) { t ->
                                    TaskItemCard(
                                        t = t,
                                        isDarkTheme = isDarkTheme,
                                        authInfo = authInfo,
                                        onTaskClick = onTaskClick,
                                        onDelete = {
                                            viewModel.deleteTask(t.id) {
                                                viewModel.fetchTasks(page = 1, pageSize = 1000)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Pagination Footer
                    if (totalFilteredPages > 1) {
                        HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Page $safePage of $totalFilteredPages",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (currentPage > 1) currentPage-- },
                                    enabled = safePage > 1,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Previous Page",
                                        tint = if (safePage > 1) (if (isDarkTheme) White else Slate900) else Slate600
                                    )
                                }
                                IconButton(
                                    onClick = { if (currentPage < totalFilteredPages) currentPage++ },
                                    enabled = safePage < totalFilteredPages,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Next Page",
                                        tint = if (safePage < totalFilteredPages) (if (isDarkTheme) White else Slate900) else Slate600
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

@Composable
fun TaskItemCard(
    t: ApiTask,
    isDarkTheme: Boolean,
    authInfo: com.example.ui.viewmodel.AuthInfo?,
    onTaskClick: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick(t.id.toString()) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Slate900 else White
        ),
        border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Title & Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = t.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDarkTheme) White else Slate900,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                PriorityBadge(priority = t.priority)
            }

            // Row 2: Project name & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = t.projectName ?: "No Project",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = t.status)
            }

            HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

            // Row 3: Assigned To & Deadline & Delete Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Assignee
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val initials = t.assignedTo?.split(" ")
                        ?.filter { it.isNotEmpty() }
                        ?.take(2)
                        ?.joinToString("") { it.take(1).uppercase() } ?: "U"
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = if (isDarkTheme) Blue900.copy(alpha = 0.3f) else Blue100,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Blue400 else Blue700,
                                fontSize = 10.sp
                            )
                        )
                    }
                    Text(
                        text = t.assignedTo ?: "Unassigned",
                        color = if (isDarkTheme) Slate300 else Slate700,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Deadline & Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    t.deadline?.let { dl ->
                        Text(
                            text = dl.take(10),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }

                    if (authInfo?.isAdmin == true) {
                        var showConfirmDelete by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showConfirmDelete = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Task",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (showConfirmDelete) {
                            AlertDialog(
                                onDismissRequest = { showConfirmDelete = false },
                                title = { Text("Delete Task?") },
                                text = { Text("Are you sure you want to delete this task?") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            onDelete()
                                            showConfirmDelete = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Delete") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showConfirmDelete = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
