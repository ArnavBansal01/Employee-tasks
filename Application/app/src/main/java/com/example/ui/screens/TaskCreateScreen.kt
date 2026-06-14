package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.CreateTaskRequest
import com.example.ui.viewmodel.TaskTrackerViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreateScreen(
    viewModel: TaskTrackerViewModel,
    onBack: () -> Unit
) {
    val projects       by viewModel.projects.collectAsState()
    val projectMembers by viewModel.projectMembers.collectAsState()
    val taskOpState    by viewModel.taskOpState.collectAsState()
    val isDarkTheme    by viewModel.themeIsDark.collectAsState()
    val context        = LocalContext.current

    // Form states
    var title             by remember { mutableStateOf("") }
    var description       by remember { mutableStateOf("") }
    var selectedStatus    by remember { mutableStateOf("Pending") }
    var selectedPriority  by remember { mutableStateOf("Medium") }
    var selectedProjectId by remember { mutableIntStateOf(0) }
    var selectedUserId    by remember { mutableIntStateOf(0) }
    var deadline          by remember { mutableStateOf("") }
    var submitError       by remember { mutableStateOf("") }

    // Dropdown expanded states
    var projectExpanded  by remember { mutableStateOf(false) }
    var userExpanded     by remember { mutableStateOf(false) }
    var statusExpanded   by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    // Date picker
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

    // Load initial data
    LaunchedEffect(Unit) {
        viewModel.fetchProjects()
        viewModel.resetTaskOpState()
    }

    // Load project members on selection and reset selected user
    LaunchedEffect(selectedProjectId) {
        if (selectedProjectId != 0) {
            viewModel.fetchProjectMembers(selectedProjectId)
            selectedUserId = 0
        }
    }

    // Handle success submission
    LaunchedEffect(taskOpState) {
        if (taskOpState is UiState.Success) {
            viewModel.resetTaskOpState()
            onBack()
        } else if (taskOpState is UiState.Error) {
            submitError = (taskOpState as UiState.Error).message
        }
    }

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

            // Title
            Text(
                text = "Create New Task",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDarkTheme) White else Slate900
            )

            // Form Content inside a Card
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    // Error Banner
                    if (submitError.isNotEmpty()) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = submitError,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }

                    // Task Title
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "TASK TITLE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = Slate400
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("e.g. Design Login UI Flow", color = Slate500) },
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
                        Text(
                            text = "DESCRIPTION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = Slate400
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Detail specific scope and requirements...", color = Slate500) },
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

                    // Project & Assignee row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Project
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PROJECT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = Slate400
                            )
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
                                        .fillMaxWidth(0.45f)
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

                        // Assignee
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isAssigneeEnabled = selectedProjectId != 0
                            val userName = if (!isAssigneeEnabled) {
                                "Select a project first"
                            } else {
                                projectMembers.find { it.idOrUserId == selectedUserId }?.name ?: "Select Assignee"
                            }
                            Text(
                                text = "ASSIGNEE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = Slate400
                            )
                            var assigneeSearch by remember { mutableStateOf("") }
                            var assigneeMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = userName,
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = if (isAssigneeEnabled) Slate500 else Slate500.copy(alpha = 0.5f)) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                        disabledContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                        disabledTextColor = if (isAssigneeEnabled) (if (isDarkTheme) White else Slate900) else Slate500
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(enabled = isAssigneeEnabled) { assigneeMenuExpanded = true }
                                )

                                DropdownMenu(
                                    expanded = assigneeMenuExpanded,
                                    onDismissRequest = { assigneeMenuExpanded = false; assigneeSearch = "" },
                                    modifier = Modifier
                                        .fillMaxWidth(0.45f)
                                        .background(if (isDarkTheme) Slate900 else White)
                                        .border(1.dp, if (isDarkTheme) Slate800 else Slate200, RoundedCornerShape(8.dp))
                                ) {
                                    OutlinedTextField(
                                        value = assigneeSearch,
                                        onValueChange = { assigneeSearch = it },
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

                                    val filteredUsers = projectMembers.filter {
                                        it.name.contains(assigneeSearch, ignoreCase = true) ||
                                        it.email.contains(assigneeSearch, ignoreCase = true)
                                    }
                                    filteredUsers.forEach { u ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(u.name, color = if (isDarkTheme) White else Slate900, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                                    Text(u.email, color = Slate400, style = MaterialTheme.typography.bodySmall)
                                                }
                                            },
                                            onClick = {
                                                selectedUserId = u.idOrUserId
                                                assigneeMenuExpanded = false
                                                assigneeSearch = ""
                                            }
                                        )
                                    }
                                    if (filteredUsers.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No users found", color = Slate500, style = MaterialTheme.typography.bodySmall) },
                                            onClick = {}
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Status & Priority row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Status
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "STATUS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = Slate400
                            )
                            ExposedDropdownMenuBox(
                                expanded = statusExpanded,
                                onExpandedChange = { statusExpanded = it }
                            ) {
                                val statusDisplay = if (selectedStatus == "InProgress") "In Progress" else selectedStatus
                                OutlinedTextField(
                                    value = statusDisplay,
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

                        // Priority
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PRIORITY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = Slate400
                            )
                            ExposedDropdownMenuBox(
                                expanded = priorityExpanded,
                                onExpandedChange = { priorityExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedPriority,
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
                                    expanded = priorityExpanded,
                                    onDismissRequest = { priorityExpanded = false },
                                    modifier = Modifier.background(if (isDarkTheme) Slate900 else White)
                                ) {
                                    listOf("Low", "Medium", "High").forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text(p, color = if (isDarkTheme) White else Slate900) },
                                            onClick = {
                                                selectedPriority = p
                                                priorityExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Deadline
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "DEADLINE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = Slate400
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() }
                        ) {
                            OutlinedTextField(
                                value = deadline,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                placeholder = { Text("Select task deadline date...", color = Slate500) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = if (isDarkTheme) Slate800 else Slate200,
                                    disabledContainerColor = if (isDarkTheme) Slate950 else Slate50,
                                    disabledTextColor = if (isDarkTheme) White else Slate900,
                                    disabledPlaceholderColor = Slate500
                                ),
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Select Date",
                                        tint = Slate400
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val submitLoading = taskOpState is UiState.Loading

                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.padding(end = 8.dp),
                            enabled = !submitLoading
                        ) {
                            Text(
                                text = "Cancel",
                                color = if (isDarkTheme) Slate300 else Slate700,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }

                        Button(
                            onClick = {
                                if (title.isBlank() || description.isBlank() || deadline.isBlank() || selectedProjectId == 0 || selectedUserId == 0) {
                                    submitError = "All fields are required."
                                    return@Button
                                }
                                submitError = ""
                                viewModel.createTask(
                                    CreateTaskRequest(
                                        title = title,
                                        description = description,
                                        status = selectedStatus,
                                        priority = selectedPriority,
                                        deadline = deadline,
                                        userId = selectedUserId,
                                        projectId = selectedProjectId
                                    )
                                ) {}
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue,
                                contentColor = White
                            ),
                            enabled = !submitLoading
                        ) {
                            if (submitLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Creating...", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            } else {
                                Text("Create Task", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            }
                        }
                    }
                }
            }
        }
    }
}
