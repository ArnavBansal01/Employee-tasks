package com.example.data.api

import com.google.gson.annotations.SerializedName

// ── Auth ─────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: ApiUser
)

// ── User ─────────────────────────────────────────────────────────────────────

data class ApiUser(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val createdAt: String,
    @com.google.gson.annotations.SerializedName("userId") val userId: Int? = null
) {
    val idOrUserId: Int get() = userId ?: id
}
data class PagedUserResponse(
    val items: List<ApiUser>,
    val totalCount: Int,
    val pageNumber: Int,
    val pageSize: Int,
    val totalPages: Int
)
data class CreateUserRequest(
    val name: String,
    val email: String,
    val password: String
)

// ── Project ───────────────────────────────────────────────────────────────────

data class ApiProject(
    val id: Int,
    val name: String,
    val description: String,
    val deadline: String?,
    val totalTasks: Int = 0,
    val progressPercentage: Int? = null
)

data class CreateProjectRequest(
    val name: String,
    val description: String,
    val deadline: String?
)

data class UpdateProjectRequest(
    val name: String,
    val description: String,
    val deadline: String?
)

// ── Project Assignments ───────────────────────────────────────────────────────

data class ProjectAssignmentRequest(
    val userIds: List<Int>
)

// ── Task ─────────────────────────────────────────────────────────────────────

data class ApiTask(
    val id: Int,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val deadline: String?,
    val assignedTo: String?,
    val projectName: String?,
    val userId: Int,
    val projectId: Int
)

data class PagedTaskResponse(
    val items: List<ApiTask>,
    val totalCount: Int,
    val pageNumber: Int,
    val pageSize: Int,
    val totalPages: Int
)

data class CreateTaskRequest(
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val deadline: String?,
    val userId: Int,
    val projectId: Int
)

data class UpdateTaskRequest(
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val deadline: String?,
    val userId: Int,
    val projectId: Int
)
