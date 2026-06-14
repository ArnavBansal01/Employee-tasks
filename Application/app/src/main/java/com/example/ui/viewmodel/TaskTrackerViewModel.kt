package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.*
import com.example.data.preferences.TokenDataStore
import com.example.ui.navigation.Routes
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── UI state wrappers ─────────────────────────────────────────────────────────

sealed class UiState<out T> {
    object Idle    : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// ── Auth state ────────────────────────────────────────────────────────────────

data class AuthInfo(
    val userId: Int,
    val name: String,
    val email: String,
    val role: String,
    val token: String
) {
    val isAdmin: Boolean get() = role.equals("Admin", ignoreCase = true)
}

// ─────────────────────────────────────────────────────────────────────────────

data class SearchResult(
    val id: String,
    val type: String, // "task" or "project"
    val label: String,
    val link: String
)

class TaskTrackerViewModel(
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val localTaskUpdateTimes = mutableMapOf<Int, Long>()

    fun getTaskUpdateTime(taskId: Int): Long {
        return localTaskUpdateTimes[taskId] ?: 0L
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    private val _themeIsDark = MutableStateFlow(true)
    val themeIsDark: StateFlow<Boolean> = _themeIsDark.asStateFlow()

    // ── Search State ──────────────────────────────────────────────────────────

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchLoading.value = true
            try {
                // Fetch tasks and projects in parallel
                val tasksDeferred = runCatching { RetrofitClient.api.getTasks(pageNumber = 1, pageSize = 100) }
                val projectsDeferred = runCatching { RetrofitClient.api.getProjects() }

                val tList = tasksDeferred.getOrNull()?.body()?.items ?: emptyList()
                val pList = projectsDeferred.getOrNull()?.body() ?: emptyList()

                val filteredTasks = tList.filter { it.title.contains(trimmed, ignoreCase = true) }
                    .map { SearchResult(it.id.toString(), "task", it.title, Routes.taskDetail(it.id.toString())) }

                val filteredProjects = pList.filter { it.name.contains(trimmed, ignoreCase = true) }
                    .map { SearchResult(it.id.toString(), "project", it.name, Routes.projectDetail(it.id.toString())) }

                _searchResults.value = filteredTasks + filteredProjects
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _searchLoading.value = false
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    private val _authInfo = MutableStateFlow<AuthInfo?>(null)
    val authInfo: StateFlow<AuthInfo?> = _authInfo.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = _authInfo
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val loginState: StateFlow<UiState<Unit>> = _loginState.asStateFlow()

    // ── Tasks ─────────────────────────────────────────────────────────────────

    private val _tasks = MutableStateFlow<List<ApiTask>>(emptyList())
    val tasks: StateFlow<List<ApiTask>> = _tasks.asStateFlow()

    private val _selectedTask = MutableStateFlow<ApiTask?>(null)
    val selectedTask: StateFlow<ApiTask?> = _selectedTask.asStateFlow()

    private val _tasksTotalPages = MutableStateFlow(1)
    val tasksTotalPages: StateFlow<Int> = _tasksTotalPages.asStateFlow()

    private val _tasksTotalCount = MutableStateFlow(0)
    val tasksTotalCount: StateFlow<Int> = _tasksTotalCount.asStateFlow()

    private val _tasksLoading = MutableStateFlow(false)
    val tasksLoading: StateFlow<Boolean> = _tasksLoading.asStateFlow()

    private val _taskOpState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val taskOpState: StateFlow<UiState<Unit>> = _taskOpState.asStateFlow()

    // ── Projects ──────────────────────────────────────────────────────────────

    private val _projects = MutableStateFlow<List<ApiProject>>(emptyList())
    val projects: StateFlow<List<ApiProject>> = _projects.asStateFlow()

    private val _selectedProject = MutableStateFlow<ApiProject?>(null)
    val selectedProject: StateFlow<ApiProject?> = _selectedProject.asStateFlow()

    private val _projectMembers = MutableStateFlow<List<ApiUser>>(emptyList())
    val projectMembers: StateFlow<List<ApiUser>> = _projectMembers.asStateFlow()

    private val _projectTasks = MutableStateFlow<List<ApiTask>>(emptyList())
    val projectTasks: StateFlow<List<ApiTask>> = _projectTasks.asStateFlow()

    private val _projectsLoading = MutableStateFlow(false)
    val projectsLoading: StateFlow<Boolean> = _projectsLoading.asStateFlow()

    private val _projectOpState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val projectOpState: StateFlow<UiState<Unit>> = _projectOpState.asStateFlow()

    // ── Users ─────────────────────────────────────────────────────────────────

    private val _users = MutableStateFlow<List<ApiUser>>(emptyList())
    val users: StateFlow<List<ApiUser>> = _users.asStateFlow()
    private val _usersTotalPages = MutableStateFlow(1)
    val usersTotalPages: StateFlow<Int> = _usersTotalPages.asStateFlow()
    private val _usersLoading = MutableStateFlow(false)
    val usersLoading: StateFlow<Boolean> = _usersLoading.asStateFlow()

    private val _userOpState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val userOpState: StateFlow<UiState<Unit>> = _userOpState.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Init: restore persisted session
    // ─────────────────────────────────────────────────────────────────────────

    init {
        RetrofitClient.setOnUnauthorizedListener {
            logout()
        }
        viewModelScope.launch {
            // Restore theme
            tokenDataStore.isDarkTheme.collect { dark -> _themeIsDark.value = dark }
        }
        viewModelScope.launch {
            // Restore auth session
            combine(
                tokenDataStore.token,
                tokenDataStore.userId,
                tokenDataStore.userName,
                tokenDataStore.userEmail,
                tokenDataStore.userRole
            ) { token, id, name, email, role ->
                if (token != null && id != null && name != null && email != null && role != null) {
                    AuthInfo(id, name, email, role, token)
                } else null
            }.collect { auth ->
                _authInfo.value = auth
                if (auth != null) RetrofitClient.setToken(auth.token)
                else RetrofitClient.setToken(null)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Theme
    // ─────────────────────────────────────────────────────────────────────────

    fun toggleTheme() {
        val newVal = !_themeIsDark.value
        _themeIsDark.value = newVal
        viewModelScope.launch { tokenDataStore.saveTheme(newVal) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Auth
    // ─────────────────────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            runCatching {
                RetrofitClient.api.login(LoginRequest(email, password))
            }.onSuccess { resp ->
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    RetrofitClient.setToken(body.token)
                    tokenDataStore.saveAuth(body.token, body.user)
                    _authInfo.value = AuthInfo(
                        userId = body.user.id,
                        name   = body.user.name,
                        email  = body.user.email,
                        role   = body.user.role,
                        token  = body.token
                    )
                    _loginState.value = UiState.Success(Unit)
                } else {
                    _loginState.value = UiState.Error("Invalid email or password")
                }
            }.onFailure {
                _loginState.value = UiState.Error("Network error: ${it.message}")
            }
        }
    }

    fun resetLoginState() { _loginState.value = UiState.Idle }

    fun logout() {
        viewModelScope.launch {
            tokenDataStore.clearAuth()
            RetrofitClient.setToken(null)
            _authInfo.value = null
            _tasks.value = emptyList()
            _projects.value = emptyList()
            _users.value = emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tasks
    // ─────────────────────────────────────────────────────────────────────────

    fun fetchTasks(page: Int = 1, status: String? = null, priority: String? = null, projectId: Int? = null, pageSize: Int = 8) {
        viewModelScope.launch {
            if (_tasks.value.isEmpty()) {
                _tasksLoading.value = true
            }
            runCatching {
                RetrofitClient.api.getTasks(page, pageSize, status, priority, projectId)
            }.onSuccess { resp ->
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    _tasks.value = body.items
                    _tasksTotalPages.value = body.totalPages
                    _tasksTotalCount.value = body.totalCount
                }
            }
            _tasksLoading.value = false
        }
    }

    fun fetchTaskById(id: Int) {
        viewModelScope.launch {
            runCatching { RetrofitClient.api.getTaskById(id) }
                .onSuccess { if (it.isSuccessful) _selectedTask.value = it.body() }
        }
    }

    fun createTask(req: CreateTaskRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _taskOpState.value = UiState.Loading
            runCatching { RetrofitClient.api.createTask(req) }
                .onSuccess { resp ->
                    if (resp.isSuccessful && resp.body() != null) {
                        val newTask = resp.body()!!
                        _tasks.value = listOf(newTask) + _tasks.value
                        localTaskUpdateTimes[newTask.id] = System.currentTimeMillis()
                        _taskOpState.value = UiState.Success(Unit)
                        onSuccess()
                    } else _taskOpState.value = UiState.Error("Failed to create task")
                }.onFailure { _taskOpState.value = UiState.Error(it.message ?: "Error") }
        }
    }

    fun updateTask(id: Int, req: UpdateTaskRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _taskOpState.value = UiState.Loading
            runCatching { RetrofitClient.api.updateTask(id, req) }
                .onSuccess { resp ->
                    if (resp.isSuccessful && resp.body() != null) {
                        val updatedTask = resp.body()!!
                        _selectedTask.value = updatedTask
                        _tasks.value = _tasks.value.map {
                            if (it.id == id) updatedTask else it
                        }
                        localTaskUpdateTimes[id] = System.currentTimeMillis()
                        _taskOpState.value = UiState.Success(Unit)
                        onSuccess()
                    } else _taskOpState.value = UiState.Error("Failed to update task")
                }.onFailure { _taskOpState.value = UiState.Error(it.message ?: "Error") }
        }
    }

    fun deleteTask(id: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            runCatching { RetrofitClient.api.deleteTask(id) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _tasks.value = _tasks.value.filter { it.id != id }
                        onSuccess()
                    }
                }
        }
    }

    fun resetTaskOpState() { _taskOpState.value = UiState.Idle }

    // ─────────────────────────────────────────────────────────────────────────
    // Projects
    // ─────────────────────────────────────────────────────────────────────────
    fun fetchProjects() {
        viewModelScope.launch {
            _projectsLoading.value = true
            try {
                val projectsResp = RetrofitClient.api.getProjects()
                val tasksResp = RetrofitClient.api.getTasks(pageSize = 1000)
                if (projectsResp.isSuccessful) {
                    val baseProjects = projectsResp.body() ?: emptyList()
                    val allTasks = if (tasksResp.isSuccessful) tasksResp.body()?.items ?: emptyList() else emptyList()
                    
                    val projectsWithProgress = baseProjects.map { p ->
                        val projectTasks = allTasks.filter { it.projectId == p.id }
                        val completedTasks = projectTasks.count { it.status.equals("Completed", ignoreCase = true) }
                        val progress = if (projectTasks.isNotEmpty()) {
                            Math.round((completedTasks.toDouble() / projectTasks.size) * 100).toInt()
                        } else 0
                        p.copy(progressPercentage = progress)
                    }
                    _projects.value = projectsWithProgress
                }
            } catch (e: Exception) {
                // Keep existing list on error
            }
            _projectsLoading.value = false
        }
    }

    fun fetchProjectById(id: Int) {
        viewModelScope.launch {
            runCatching { RetrofitClient.api.getProjectById(id) }
                .onSuccess { if (it.isSuccessful) _selectedProject.value = it.body() }
        }
    }

    fun fetchProjectMembers(projectId: Int) {
        viewModelScope.launch {
            runCatching { RetrofitClient.api.getProjectAssignments(projectId) }
                .onSuccess { if (it.isSuccessful) _projectMembers.value = it.body() ?: emptyList() }
        }
    }

    fun fetchProjectTasks(projectId: Int) {
        viewModelScope.launch {
            runCatching { RetrofitClient.api.getTasks(pageSize = 100, projectId = projectId) }
                .onSuccess { if (it.isSuccessful) _projectTasks.value = it.body()?.items ?: emptyList() }
        }
    }

    fun createProject(req: CreateProjectRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _projectOpState.value = UiState.Loading
            runCatching { RetrofitClient.api.createProject(req) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _projectOpState.value = UiState.Success(Unit)
                        fetchProjects()
                        onSuccess()
                    } else _projectOpState.value = UiState.Error("Failed to create project")
                }.onFailure { _projectOpState.value = UiState.Error(it.message ?: "Error") }
        }
    }

    fun updateProject(id: Int, req: UpdateProjectRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _projectOpState.value = UiState.Loading
            runCatching { RetrofitClient.api.updateProject(id, req) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _selectedProject.value = resp.body()
                        _projectOpState.value = UiState.Success(Unit)
                        onSuccess()
                    } else _projectOpState.value = UiState.Error("Failed to update project")
                }.onFailure { _projectOpState.value = UiState.Error(it.message ?: "Error") }
        }
    }

    fun deleteProject(id: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            runCatching { RetrofitClient.api.deleteProject(id) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _projects.value = _projects.value.filter { it.id != id }
                        onSuccess()
                    }
                }
        }
    }

    fun addProjectMembers(projectId: Int, userIds: List<Int>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            runCatching { RetrofitClient.api.addProjectMembers(projectId, ProjectAssignmentRequest(userIds)) }
                .onSuccess { if (it.isSuccessful) { fetchProjectMembers(projectId); onSuccess() } }
        }
    }

    fun replaceProjectMembers(projectId: Int, userIds: List<Int>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            runCatching { RetrofitClient.api.replaceProjectMembers(projectId, ProjectAssignmentRequest(userIds)) }
                .onSuccess { if (it.isSuccessful) { fetchProjectMembers(projectId); onSuccess() } }
        }
    }

    fun resetProjectOpState() { _projectOpState.value = UiState.Idle }

    // ─────────────────────────────────────────────────────────────────────────
    // Users
    // ─────────────────────────────────────────────────────────────────────────

    fun fetchUsers(page: Int = 1) {
        viewModelScope.launch {
            _usersLoading.value = true
            runCatching { RetrofitClient.api.getUsers(page, 20) }
                .onSuccess { resp ->
                    if (resp.isSuccessful && resp.body() != null) {
                        val body = resp.body()!!
                        _users.value = body.items        // Extract the list
                        _usersTotalPages.value = body.totalPages // Extract pagination
                    } else {
                        // Handle error or empty state
                    }
                }
            _usersLoading.value = false
        }
    }

    fun createUser(name: String, email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _userOpState.value = UiState.Loading
            runCatching { RetrofitClient.api.createUser(CreateUserRequest(name, email, password)) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _userOpState.value = UiState.Success(Unit)
                        fetchUsers()
                        onSuccess()
                    } else _userOpState.value = UiState.Error("Failed to create user")
                }.onFailure { _userOpState.value = UiState.Error(it.message ?: "Error") }
        }
    }

    fun promoteUser(id: Int) {
        viewModelScope.launch {
            runCatching { RetrofitClient.api.promoteUser(id) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _users.value = _users.value.map {
                            if (it.id == id) it.copy(role = "Admin") else it
                        }
                    }
                }
        }
    }

    fun demoteUser(id: Int) {
        viewModelScope.launch {
            runCatching { RetrofitClient.api.demoteUser(id) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _users.value = _users.value.map {
                            if (it.id == id) it.copy(role = "Employee") else it
                        }
                    }
                }
        }
    }

    fun deleteUser(id: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            runCatching { RetrofitClient.api.deleteUser(id) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _users.value = _users.value.filter { it.id != id }
                        onSuccess()
                    }
                }
        }
    }

    fun resetUserOpState() { _userOpState.value = UiState.Idle }

    // ─────────────────────────────────────────────────────────────────────────
    // Factory
    // ─────────────────────────────────────────────────────────────────────────

    class Factory(private val dataStore: TokenDataStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TaskTrackerViewModel(dataStore) as T
    }
}
