# Employee Task Tracker Web Client

A web dashboard for managing the task tracker system, built with **React 18**, **TypeScript**, **Tailwind CSS**, and **Vite**.

---

## 🛠️ Tech Stack

- **Framework:** React 18
- **Language:** TypeScript
- **Styling:** Tailwind CSS & Lucide Icons
- **Routing:** React Router DOM
- **HTTP Client:** Axios with custom interceptors
- **Auth Service:** Firebase Web SDK

---

## 🔒 Authentication Flow

The app connects Firebase Authentication to the C# backend API using an Axios client:

### Axios Interceptor (`./src/api/axios.ts`)
For every network request:
1.  **Firebase ID Token**: The interceptor grabs the latest token from Firebase (`getIdToken()`) and attaches it in the `Authorization: Bearer <token>` header.
2.  **User Role**: It attaches the user's role in the `X-User-Role` header.

### Automated Logout
If the backend detects a role change or an invalid session, it returns a `401 Unauthorized` status. The Axios interceptor captures this status, clears local storage, and redirects the user to `/login`.

---

## 📂 Pages Directory

The pages are located inside the `src/pages` folder:

- **[Login.tsx](./src/pages/Login.tsx)**: Authenticates the user with Firebase and synchronizes their profile with the backend API.
- **[Dashboard.tsx](./src/pages/Dashboard.tsx)**: Displays metrics and status charts. Employees see their own tasks, while Admins see system-wide task statuses.
- **[Projects.tsx](./src/pages/Projects.tsx)** & **[ProjectDetail.tsx](./src/pages/ProjectDetail.tsx)**: View project memberships and details. Admins can create projects and manage which employees are assigned to them.
- **[Tasks.tsx](./src/pages/Tasks.tsx)** & **[TaskDetail.tsx](./src/pages/TaskDetail.tsx)**: Filter and view tasks. Employees can change their task status here.
- **[CreateTask.tsx](./src/pages/CreateTask.tsx)**: Admin-only page to create tasks and assign them to project members.
- **[Users.tsx](./src/pages/Users.tsx)**: Admin-only page to promote, demote, or delete users.
- **[Profile.tsx](./src/pages/Profile.tsx)**: Shows user credentials, profile creation date, and current role.

---

## ⚙️ Setup & Run Guide

### Prerequisites
- **Node.js**: v18.0.0 or higher.
- **npm** or **yarn**.

### Setup Steps

1.  **Install Dependencies:**
    Navigate to this folder and run:
    ```bash
    npm install
    ```

2.  **Configure Environment Variables:**
    Create a `.env` file in the root of this folder and add your Firebase credentials:
    ```env
    VITE_FIREBASE_API_KEY="YOUR_FIREBASE_API_KEY"
    VITE_FIREBASE_AUTH_DOMAIN="YOUR_PROJECT_ID.firebaseapp.com"
    VITE_FIREBASE_PROJECT_ID="YOUR_PROJECT_ID"
    VITE_FIREBASE_STORAGE_BUCKET="YOUR_PROJECT_ID.firebasestorage.app"
    VITE_FIREBASE_MESSAGING_SENDER_ID="YOUR_SENDER_ID"
    VITE_FIREBASE_APP_ID="YOUR_APP_ID"
    VITE_FIREBASE_MEASUREMENT_ID="YOUR_MEASUREMENT_ID"
    ```
    > [!WARNING]
    > Never commit your `.env` file to Git. The `.gitignore` file is configured to ignore it.

3.  **Run Development Server:**
    ```bash
    npm run dev
    ```
    *Open `http://localhost:5173` to access the application.*

4.  **Production Build:**
    ```bash
    npm run build
    ```
    *Generates optimized production assets in the `dist/` directory.*
