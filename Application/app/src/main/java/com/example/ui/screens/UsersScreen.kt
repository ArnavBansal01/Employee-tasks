package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.api.ApiUser
import com.example.ui.viewmodel.TaskTrackerViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(viewModel: TaskTrackerViewModel) {
    val users       by viewModel.users.collectAsState()
    val loading     by viewModel.usersLoading.collectAsState()
    val authInfo    by viewModel.authInfo.collectAsState()
    val userOpState by viewModel.userOpState.collectAsState()
    val isDarkTheme by viewModel.themeIsDark.collectAsState()

    var searchQuery       by remember { mutableStateOf("") }
    var showCreateDialog  by remember { mutableStateOf(false) }
    var deleteTargetId    by remember { mutableStateOf<Int?>(null) }

    val filteredUsers = remember(users, searchQuery) {
        users.filter { user ->
            user.name.contains(searchQuery, ignoreCase = true) ||
            user.email.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) { 
        viewModel.fetchUsers() 
        viewModel.resetUserOpState()
    }
    
    LaunchedEffect(userOpState) {
        if (userOpState is UiState.Success) {
            showCreateDialog = false
            viewModel.resetUserOpState()
        }
    }

    val isAdmin = authInfo?.isAdmin == true

    Scaffold(
        containerColor = if (isDarkTheme) Slate950 else Slate50,
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true }, 
                    containerColor = AccentBlue,
                    contentColor = White
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Create User")
                }
            }
        }
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
                Column {
                    Text(
                        text = "Users",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkTheme) White else Slate900
                    )

                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search users by name or email...", color = Slate400) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Slate400) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (isDarkTheme) White else Slate900,
                    unfocusedTextColor = if (isDarkTheme) White else Slate900,
                    focusedBorderColor = Blue500,
                    unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                    focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                    unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredUsers) { user ->
                        UserCard(
                            user = user,
                            isAdmin = isAdmin,
                            currentUserId = authInfo?.userId,
                            isDarkTheme = isDarkTheme,
                            onPromote   = { viewModel.promoteUser(user.id) },
                            onDemote    = { viewModel.demoteUser(user.id) },
                            onDelete    = { deleteTargetId = user.id }
                        )
                    }
                    if (filteredUsers.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No users found", color = Slate400)
                            }
                        }
                    }
                }
            }
        }
    }

    // Create user dialog
    if (showCreateDialog) {
        CreateUserDialog(viewModel = viewModel, onDismiss = { showCreateDialog = false })
    }

    // Delete confirmation
    deleteTargetId?.let { targetId ->
        val targetUser = users.find { it.id == targetId }
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("Delete User?") },
            text = { Text("Delete ${targetUser?.name}? This will fail if they have assigned tasks.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(targetId) {}
                        deleteTargetId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTargetId = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun UserCard(
    user: ApiUser,
    isAdmin: Boolean,
    currentUserId: Int?,
    isDarkTheme: Boolean,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onDelete: () -> Unit
) {
    val roleIsAdmin = user.role.equals("Admin", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Slate900 else White
        ),
        border = BorderStroke(1.dp, if (isDarkTheme) Slate800 else Slate200)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: Avatar + Info details
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar circle with initials
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(AccentBlue.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = user.name.split(" ")
                        .filter { it.isNotEmpty() }
                        .take(2)
                        .joinToString("") { it.take(1).uppercase() }
                    Text(
                        text = if (initials.isNotEmpty()) initials else "U",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentBlue
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.name, 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkTheme) White else Slate900
                    )
                    Text(
                        text = user.email, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Slate400
                    )
                    
                    // Role Badge (distinct colors for admin and employee)
                    Surface(
                        color = if (roleIsAdmin) {
                            if (isDarkTheme) Blue600.copy(alpha = 0.15f) else Blue100
                        } else {
                            if (isDarkTheme) Slate800 else Slate200
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = user.role.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (roleIsAdmin) {
                                if (isDarkTheme) Blue400 else Blue700
                            } else {
                                if (isDarkTheme) Slate300 else Slate700
                            }
                        )
                    }
                }
            }

            // Right side: Action buttons
            if (isAdmin && user.id != currentUserId) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    if (!roleIsAdmin) {
                        TextButton(
                            onClick = onPromote,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = Blue500)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Promote", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    } else {
                        TextButton(
                            onClick = onDemote,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = StatusPending)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Demote", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog(viewModel: TaskTrackerViewModel, onDismiss: () -> Unit) {
    val userOpState by viewModel.userOpState.collectAsState()
    val isDarkTheme by viewModel.themeIsDark.collectAsState()
    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create User", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it },
                    label = { Text("Name") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDarkTheme) White else Slate900,
                        unfocusedTextColor = if (isDarkTheme) White else Slate900,
                        focusedBorderColor = Blue500,
                        unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                        focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                        unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                    )
                )
                OutlinedTextField(
                    value = email, 
                    onValueChange = { email = it },
                    label = { Text("Email") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDarkTheme) White else Slate900,
                        unfocusedTextColor = if (isDarkTheme) White else Slate900,
                        focusedBorderColor = Blue500,
                        unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                        focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                        unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                    )
                )
                OutlinedTextField(
                    value = password, 
                    onValueChange = { password = it },
                    label = { Text("Password") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDarkTheme) White else Slate900,
                        unfocusedTextColor = if (isDarkTheme) White else Slate900,
                        focusedBorderColor = Blue500,
                        unfocusedBorderColor = if (isDarkTheme) Slate800 else Slate200,
                        focusedContainerColor = if (isDarkTheme) Slate950 else Slate50,
                        unfocusedContainerColor = if (isDarkTheme) Slate950 else Slate50
                    )
                )
                if (userOpState is UiState.Error)
                    Text((userOpState as UiState.Error).message, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.createUser(name, email, password) {} },
                enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && userOpState !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                if (userOpState is UiState.Loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
