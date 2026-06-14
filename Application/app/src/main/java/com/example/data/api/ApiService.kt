package com.example.data.api

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ─────────────────────────────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ── Users ─────────────────────────────────────────────────────────────────

    /** Admin: paginated list of all users */
    @GET("users")
    suspend fun getUsers(
        @Query("PageNumber") pageNumber: Int = 1,
        @Query("PageSize") pageSize: Int = 20
    ): Response<PagedUserResponse>

    /** Get user by ID (employees can only fetch themselves) */
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Int): Response<ApiUser>

    /** Public: register a new employee account */
    @POST("users")
    suspend fun createUser(@Body request: CreateUserRequest): Response<ApiUser>

    /** Admin: delete a user */
    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): Response<Unit>

    /** Admin: promote user to Admin */
    @PUT("users/{id}/promote")
    suspend fun promoteUser(@Path("id") id: Int): Response<Unit>

    /** Admin: demote user to Employee */
    @PUT("users/{id}/demote")
    suspend fun demoteUser(@Path("id") id: Int): Response<Unit>

    // ── Projects ───────────────────────────────────────────────────────────────

    /** Employee/Admin: get visible projects */
    @GET("projects")
    suspend fun getProjects(): Response<List<ApiProject>>

    /** Employee/Admin: get project by ID */
    @GET("projects/{id}")
    suspend fun getProjectById(@Path("id") id: Int): Response<ApiProject>

    /** Admin: create a project */
    @POST("projects")
    suspend fun createProject(@Body request: CreateProjectRequest): Response<ApiProject>

    /** Admin: update a project */
    @PUT("projects/{id}")
    suspend fun updateProject(
        @Path("id") id: Int,
        @Body request: UpdateProjectRequest
    ): Response<ApiProject>

    /** Admin: delete a project */
    @DELETE("projects/{id}")
    suspend fun deleteProject(@Path("id") id: Int): Response<Unit>

    // ── Project Assignments ───────────────────────────────────────────────────

    /** Get users assigned to a project */
    @GET("projectassignments/{projectId}")
    suspend fun getProjectAssignments(@Path("projectId") projectId: Int): Response<List<ApiUser>>

    /** Admin: add users to a project (non-destructive) */
    @POST("projectassignments/{projectId}")
    suspend fun addProjectMembers(
        @Path("projectId") projectId: Int,
        @Body request: ProjectAssignmentRequest
    ): Response<Unit>

    /** Admin: replace all project assignments */
    @PUT("projectassignments/{projectId}")
    suspend fun replaceProjectMembers(
        @Path("projectId") projectId: Int,
        @Body request: ProjectAssignmentRequest
    ): Response<Unit>

    // ── Tasks ─────────────────────────────────────────────────────────────────

    /** Employee/Admin: get tasks with pagination */
    @GET("tasks")
    suspend fun getTasks(
        @Query("PageNumber") pageNumber: Int = 1,
        @Query("PageSize") pageSize: Int = 20,
        @Query("Status") status: String? = null,
        @Query("Priority") priority: String? = null,
        @Query("ProjectId") projectId: Int? = null
    ): Response<PagedTaskResponse>

    /** Employee/Admin: get task by ID */
    @GET("tasks/{id}")
    suspend fun getTaskById(@Path("id") id: Int): Response<ApiTask>

    /** Admin: create and assign a task */
    @POST("tasks")
    suspend fun createTask(@Body request: CreateTaskRequest): Response<ApiTask>

    /** Assigned Employee/Admin: update a task */
    @PUT("tasks/{id}")
    suspend fun updateTask(
        @Path("id") id: Int,
        @Body request: UpdateTaskRequest
    ): Response<ApiTask>

    /** Admin: delete a task */
    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<Unit>
}
