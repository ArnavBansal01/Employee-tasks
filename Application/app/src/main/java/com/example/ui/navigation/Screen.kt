package com.example.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val TASKS = "tasks"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val TASK_CREATE = "tasks/create"
    const val PROJECTS = "projects"
    const val PROJECT_DETAIL = "project_detail/{projectId}"
    const val USERS = "users"
    const val ERROR = "error"

    fun taskDetail(taskId: String) = "task_detail/$taskId"
    fun projectDetail(projectId: String) = "project_detail/$projectId"
}
