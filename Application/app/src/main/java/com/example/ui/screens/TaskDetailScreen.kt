package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.api.UpdateTaskRequest
import com.example.ui.viewmodel.TaskTrackerViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    viewModel: TaskTrackerViewModel,
    onBack: () -> Unit
) {
    val task        by viewModel.selectedTask.collectAsState()
    val authInfo    by viewModel.authInfo.collectAsState()
    val taskOpState by viewModel.taskOpState.collectAsState()
    val isDarkTheme by viewModel.themeIsDark.collectAsState()
    val context     = LocalContext.current

    // Local state for Quick-edit status
    var selectedStatus by remember { mutableStateOf("") }

    // Status / Success indicators
    var saved   by remember { mutableStateOf(false) }
    var saving  by remember { mutableStateOf(false) }
    var error   by remember { mutableStateOf("") }

    // Admin full edit modal state
    var isEditModalOpen by remember { mutableStateOf(false) }
    var editTitle       by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editPriority    by remember { mutableStateOf("") }
    var editDeadline    by remember { mutableStateOf("") }

    // Load data
    LaunchedEffect(taskId) {
        viewModel.fetchTaskById(taskId.toInt())
        viewModel.resetTaskOpState()
    }

    // Set initial status selection once task is fetched
    LaunchedEffect(task) {
        task?.let { t ->
            selectedStatus = t.status
            error = ""
        }
    }

    // Handle background status/edit success
    LaunchedEffect(taskOpState) {
        when (taskOpState) {
            is UiState.Success -> {
                if (saving) {
                    saved = true
                    saving = false
                }
                viewModel.resetTaskOpState()
            }
            is UiState.Error -> {
                error = (taskOpState as UiState.Error).message
                saving = false
            }
            is UiState.Loading -> {
                // Keep saving true
            }
            else -> {}
        }
    }

    // Auto dismiss success toast
    LaunchedEffect(saved) {
        if (saved) {
            kotlinx.coroutines.delay(2000)
            saved = false
        }
    }

    if (task == null) {
        Box(Modifier.fillMaxSize().background(if (isDarkTheme) Slate950 else Slate50), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentBlue)
        }
        return
    }

    val currentTask = task!!
    val isAdmin = authInfo?.isAdmin == true

    Scaffold(
        containerColor = if (isDarkTheme) Slate950 else Slate50
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Back navigation
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
                    text = "Back to Tasks",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Slate500
                )
            }

            // Task Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Slate900 else White
                ),
                border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    
                    // ── Header Block ───────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = currentTask.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isDarkTheme) White else Slate900
                            )
                            Text(
                                text = currentTask.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate400,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PriorityBadge(priority = currentTask.priority)

                            if (isAdmin) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Edit details pencil icon
                                    IconButton(
                                        onClick = {
                                            editTitle = currentTask.title
                                            editDescription = currentTask.description
                                            editPriority = currentTask.priority
                                            editDeadline = currentTask.deadline?.take(10) ?: ""
                                            isEditModalOpen = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Details",
                                            tint = Slate400,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete trash icon
                                    var showDeleteConfirm by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = { showDeleteConfirm = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Task",
                                            tint = Slate400,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (showDeleteConfirm) {
                                        AlertDialog(
                                            onDismissRequest = { showDeleteConfirm = false },
                                            title = { Text("Delete Task?") },
                                            text = { Text("Permanently delete this task?") },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        viewModel.deleteTask(currentTask.id) {
                                                            onBack()
                                                        }
                                                        showDeleteConfirm = false
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                                ) { Text("Delete") }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

                    // ── Info Grid Block ────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Column 1: Assigned To
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (isDarkTheme) Slate800 else Slate100, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text("Assigned To", style = MaterialTheme.typography.labelSmall, color = Slate400)
                                    Text(
                                        text = currentTask.assignedTo ?: "Unassigned",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = if (isDarkTheme) White else Slate900
                                    )
                                }
                            }

                            // Column 2: Project
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (isDarkTheme) Slate800 else Slate100, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text("Project", style = MaterialTheme.typography.labelSmall, color = Slate400)
                                    Text(
                                        text = currentTask.projectName ?: "—",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Blue500
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Column 1: Deadline
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (isDarkTheme) Slate800 else Slate100, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text("Deadline", style = MaterialTheme.typography.labelSmall, color = Slate400)
                                    Text(
                                        text = currentTask.deadline?.take(10) ?: "—",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = if (isDarkTheme) White else Slate900
                                    )
                                }
                            }

                            // Column 2: Status Dropdown Selection
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (isDarkTheme) Slate800 else Slate100, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text("Status Update", style = MaterialTheme.typography.labelSmall, color = Slate400)
                                    
                                    var statusExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        Row(
                                            modifier = Modifier
                                                .clickable { statusExpanded = true }
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (selectedStatus == "InProgress") "In Progress" else selectedStatus,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = if (isDarkTheme) White else Slate900
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = Slate500,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = statusExpanded,
                                            onDismissRequest = { statusExpanded = false },
                                            modifier = Modifier.background(if (isDarkTheme) Slate900 else White)
                                        ) {
                                            listOf("Pending", "InProgress", "Completed").forEach { s ->
                                                DropdownMenuItem(
                                                    text = { Text(if (s == "InProgress") "In Progress" else s, color = if (isDarkTheme) White else Slate900) },
                                                    onClick = {
                                                        selectedStatus = s
                                                        statusExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

                    // ── Action Footer ──────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDarkTheme) Slate900.copy(alpha = 0.5f) else Slate50.copy(alpha = 0.5f))
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                saving = true
                                viewModel.updateTask(
                                    currentTask.id,
                                    UpdateTaskRequest(
                                        title = currentTask.title,
                                        description = currentTask.description,
                                        status = selectedStatus,
                                        priority = currentTask.priority,
                                        deadline = currentTask.deadline?.take(10),
                                        userId = currentTask.userId,
                                        projectId = currentTask.projectId
                                    )
                                ) {}
                            },
                            enabled = selectedStatus != currentTask.status && !saving,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue,
                                contentColor = White
                            )
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (saving) "Saving..." else "Update Status", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        if (saved) {
                            Text(
                                text = "Updated successfully!",
                                color = Emerald500,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }

                        if (error.isNotEmpty()) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        StatusBadge(status = currentTask.status)
                    }
                }
            }
        }
    }

    // ── Admin Edit Modal Dialog ──────────────────────────────────────────────
    if (isEditModalOpen && isAdmin) {
        var editPriorityExpanded by remember { mutableStateOf(false) }
        var modalError by remember { mutableStateOf("") }

        val modalDatePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                editDeadline = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        )

        Dialog(onDismissRequest = { isEditModalOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                            text = "Edit Task Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isDarkTheme) White else Slate900
                        )
                        IconButton(onClick = { isEditModalOpen = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                        }
                    }

                    HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

                    if (modalError.isNotEmpty()) {
                        Text(modalError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    // Inputs
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Title
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("TASK TITLE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
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

                        // Description
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("DESCRIPTION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                            OutlinedTextField(
                                value = editDescription,
                                onValueChange = { editDescription = it },
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

                        // Priority & Deadline Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Priority
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("PRIORITY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                                ExposedDropdownMenuBox(
                                    expanded = editPriorityExpanded,
                                    onExpandedChange = { editPriorityExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = editPriority,
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
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = editPriorityExpanded,
                                        onDismissRequest = { editPriorityExpanded = false },
                                        modifier = Modifier.background(if (isDarkTheme) Slate900 else White)
                                    ) {
                                        listOf("Low", "Medium", "High").forEach { p ->
                                            DropdownMenuItem(
                                                text = { Text(p, color = if (isDarkTheme) White else Slate900) },
                                                onClick = {
                                                    editPriority = p
                                                    editPriorityExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Deadline
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("DEADLINE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate400)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { modalDatePicker.show() }
                                ) {
                                    OutlinedTextField(
                                        value = editDeadline,
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                            disabledContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                            disabledTextColor = if (isDarkTheme) White else Slate900
                                        ),
                                        trailingIcon = { Icon(Icons.Default.DateRange, null, tint = Slate400) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
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
                        TextButton(onClick = { isEditModalOpen = false }) {
                            Text("Cancel", color = if (isDarkTheme) Slate300 else Slate700)
                        }

                        Button(
                            onClick = {
                                if (editTitle.isBlank() || editDescription.isBlank() || editDeadline.isBlank()) {
                                    modalError = "All fields are required."
                                    return@Button
                                }
                                modalError = ""
                                viewModel.updateTask(
                                    currentTask.id,
                                    UpdateTaskRequest(
                                        title = editTitle,
                                        description = editDescription,
                                        status = currentTask.status, // preserve status
                                        priority = editPriority,
                                        deadline = editDeadline,
                                        userId = currentTask.userId,
                                        projectId = currentTask.projectId
                                    )
                                ) {
                                    isEditModalOpen = false
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}
