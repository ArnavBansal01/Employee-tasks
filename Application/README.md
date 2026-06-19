# Employee Task Tracker Android Client

A native Kotlin Android application built with **Jetpack Compose** and **Material 3**. It serves as the mobile app for the task tracker suite.

---

## 🛠️ Tech Stack

- **UI Framework:** Jetpack Compose (Material 3)
- **Language:** Kotlin
- **Asynchronous Flow:** Coroutines & Flow
- **Local Cache:** Jetpack DataStore Preferences
- **Network Client:** Retrofit 2 & OkHttp 3

---

## 🔒 Firebase Integration

To keep the application size minimal, this client communicates with Firebase via standard HTTP REST endpoints instead of importing the heavy Firebase Google SDKs:
- **Login**: Handled directly using Google's Identity Toolkit sign-in REST endpoint.
- **Token Refresh**: Handled directly using Google's secure token REST endpoint.

---

## 🔄 Network Client & Token Refresher

The network logic is managed in **[RetrofitClient.kt](./app/src/main/java/com/example/data/api/RetrofitClient.kt)**:

- **Headers**: Automatically appends the Firebase token as `Authorization: Bearer <token>` and the user's role as `X-User-Role`.
- **Auto Token Refresh**: If a backend request fails with a `401 Unauthorized` status (due to token expiration after 1 hour), the client automatically sends a refresh request to Google's token service, gets a new token, saves it, and retries the original request.
- **Safety Interception**: If the backend returns `"Role changed."` or `"Token invalidated."` inside the 401 error body (meaning an admin updated or deleted the user), the client skips token refresh and logs the user out immediately.

---

## 📂 Screens Overview

The application screens are located in `app/src/main/java/com/example/ui/screens/`:

- **[LoginScreen.kt](./app/src/main/java/com/example/ui/screens/LoginScreen.kt)**: Handles user credentials, logging in via Firebase, and linking the session with the backend database.
- **[DashboardScreen.kt](./app/src/main/java/com/example/ui/screens/DashboardScreen.kt)**: Shows progress metrics and task statistics.
- **[ProjectsScreen.kt](./app/src/main/java/com/example/ui/screens/ProjectsScreen.kt)** & **[ProjectDetailScreen.kt](./app/src/main/java/com/example/ui/screens/ProjectDetailScreen.kt)**: Lists visible projects. Users can view members and tasks assigned to each project.
- **[TasksScreen.kt](./app/src/main/java/com/example/ui/screens/TasksScreen.kt)** & **[TaskDetailScreen.kt](./app/src/main/java/com/example/ui/screens/TaskDetailScreen.kt)**: Displays assigned tasks. Employees can update a task's status here.
- **[TaskCreateScreen.kt](./app/src/main/java/com/example/ui/screens/TaskCreateScreen.kt)**: Administrative screen to create and assign tasks.
- **[UsersScreen.kt](./app/src/main/java/com/example/ui/screens/UsersScreen.kt)**: Administrative screen to manage user roles.
- **[EmptyStateScreen.kt](./app/src/main/java/com/example/ui/screens/EmptyStateScreen.kt)**: Reusable UI layout for displaying empty screens.

---

## ⚙️ Setup & Run Guide

### Prerequisites
- **Android Studio**: Ladybug or newer.
- **JDK**: Version 21.
- **Device/Emulator**: Android API Level 24 (Android 7.0) or higher.

### Setup Steps

1.  **Configure API Credentials:**
    Create a file named `local.properties` in this directory and define your Firebase API Key:
    ```properties
    firebase.apiKey="YOUR_FIREBASE_API_KEY"
    ```
    > [!IMPORTANT]
    > Do not commit `local.properties` to version control. The `.gitignore` file is pre-configured to ignore it. The build system reads this property and injects it into `BuildConfig.FIREBASE_API_KEY` dynamically during build.

2.  **API Connection Configuration:**
    The network base connection endpoint is defined in **[RetrofitClient.kt](./app/src/main/java/com/example/data/api/RetrofitClient.kt)**:
    ```kotlin
    private const val BASE_URL = "http://10.0.2.2:5221/api/"
    ```
    *(Note: `10.0.2.2` is a special loopback address that maps directly to the host machine's `localhost` from within the Android Emulator).*

3.  **Build & Run:**
    - Open this directory in Android Studio.
    - Sync Gradle and build the project.
    - Run the application on your emulator or test device.
    - Alternatively, compile a debug build from your command line:
      ```bash
      gradlew assembleDebug
      ```
      The output APK will be generated inside `app/build/outputs/apk/debug/`.
