package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
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
import com.example.data.api.ApiProject
import com.example.data.api.CreateProjectRequest
import com.example.ui.viewmodel.TaskTrackerViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.theme.*
import java.util.Calendar

@Composable
fun ProjectsScreen(
    viewModel: TaskTrackerViewModel,
    onProjectClick: (String) -> Unit
) {
    val projects    by viewModel.projects.collectAsState()
    val loading     by viewModel.projectsLoading.collectAsState()
    val authInfo    by viewModel.authInfo.collectAsState()
    val projectOp   by viewModel.projectOpState.collectAsState()
    val isDarkTheme by viewModel.themeIsDark.collectAsState()

    var showCreate  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.fetchProjects() }
    LaunchedEffect(projectOp) {
        if (projectOp is UiState.Success) {
            showCreate = false
            viewModel.resetProjectOpState()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Slate900 else Slate50)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Projects",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDarkTheme) White else Slate900
                )
                if (authInfo?.isAdmin == true) {
                    Button(
                        onClick = { showCreate = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "New Project",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            if (loading) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (projects.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No projects available.", color = Slate400)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize().weight(1f)
                ) {
                    items(projects) { project ->
                        ProjectCard(project = project, isDarkTheme = isDarkTheme, onClick = { onProjectClick(project.id.toString()) })
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateProjectDialog(viewModel = viewModel, onDismiss = { showCreate = false })
    }
}

@Composable
fun ProjectCard(project: ApiProject, isDarkTheme: Boolean, onClick: () -> Unit) {
    val progress = project.progressPercentage ?: 0
    val formattedDeadline = project.deadline?.take(10) ?: "No deadline"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Slate800 else White
        ),
        border = BorderStroke(
            1.dp,
            if (isDarkTheme) Slate800 else Slate200
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isDarkTheme) White else Slate900
            )

            Text(
                text = project.description,
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Deadline",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formattedDeadline,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }

                Surface(
                    color = if (isDarkTheme) Blue600.copy(alpha = 0.15f) else Blue500.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Tasks",
                            tint = if (isDarkTheme) Blue500 else Blue600,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${project.totalTasks} task${if (project.totalTasks != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDarkTheme) Blue500 else Blue600
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Slate400
                    )
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkTheme) White else Slate900
                    )
                }

                LinearProgressIndicator(
                    progress = { progress.toFloat() / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AccentBlue,
                    trackColor = if (isDarkTheme) Slate700 else Slate200
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateProjectDialog(
    viewModel: TaskTrackerViewModel,
    onDismiss: () -> Unit
) {
    val projectOp by viewModel.projectOpState.collectAsState()
    val isDarkTheme by viewModel.themeIsDark.collectAsState()
    val context = LocalContext.current

    var name        by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline    by remember { mutableStateOf("") }
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
                        text = "Create New Project",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkTheme) White else Slate900
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate400
                        )
                    }
                }

                HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

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

                // Inputs
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Name
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PROJECT NAME",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = Slate400
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("e.g., Global Relocation App", color = Slate500) },
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
                            placeholder = { Text("Detail scope requirements, targets, and goals...", color = Slate500) },
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

                    // Target Deadline
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "TARGET DEADLINE",
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
                                placeholder = { Text("Select date", color = Slate500) },
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
                }

                HorizontalDivider(color = if (isDarkTheme) Slate800 else Slate100)

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val submitLoading = projectOp is UiState.Loading
                    
                    TextButton(
                        onClick = onDismiss,
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
                            if (name.isBlank() || description.isBlank() || deadline.isBlank()) {
                                submitError = "All fields are required."
                                return@Button
                            }
                            submitError = ""
                            viewModel.createProject(
                                CreateProjectRequest(name, description, deadline)
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
                            Text("Create Project", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }
    }
}
