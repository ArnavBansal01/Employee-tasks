# Employee Task Tracker Suite

A complete task tracking and project management system. This project includes:
- **ASP.NET Core REST API** backend
- **React Web Dashboard** (built with Vite, TypeScript, and Tailwind CSS)
- **Kotlin Android App** (built with Jetpack Compose and Retrofit)

---

## 🏛️ System Architecture

Here is how the applications connect and work together:

```text
       +------------------------+
       |   Client Applications  |
       |  React Web & Android   |
       +-----+------------+-----+
             |            |
  (1) Login  |            | (2) Send Request
  & get Token|            | with Bearer Token
             v            v
       +----------+  +--------------+
       | Firebase |<-| ASP.NET Core |
       |   Auth   |  |   Web API    |
       +----------+  +------+-------+
                            | (3) Read/Write
                            | Data
                            v
                     +--------------+
                     |  SQL Server  |
                     |   Database   |
                     +--------------+
```

### Folder Structure

- **[Task api](./Task%20api)**: The backend API built with C# and Entity Framework Core. It handles databases, rate limits, user roles, projects, and task assignments.
- **[Website](./Website)**: The admin and employee web dashboard built with React. Admins can manage users, projects, and tasks here.
- **[Application](./Application)**: The native Android app built with Kotlin. It gives employees a lightweight interface to view and complete their assigned tasks.

---

## 🔑 Key Features

- **Firebase Authentication**: Secure client authentication powered by Firebase Auth, verified directly on the backend.
- **Auto Role Sync**: When a client logs in using Firebase, the backend automatically links their Firebase account to their local database profile.
- **Automatic Logout**: If an admin promotes, demotes, or deletes a user, the clients detect this instantly and log the user out.
- **Rate Limiting**: Protects login and main endpoints from excessive requests.

---

## 🛠️ Startup Guide

Follow these steps to run the services locally.

### 1. Backend API & SQL Database Setup

#### Prerequisites
- **.NET SDK**: 10.0 or higher.
- **SQL Server**: Express, Developer, or LocalDB.

#### Setup Steps
1. Navigate to the API folder:
   ```bash
   cd "Task api"
   ```
2. Place your service account JSON file named `firebase-adminsdk.json` in this folder.
3. Configure your SQL Server database connection and Firebase Project ID in `appsettings.json` or `appsettings.Development.json`:
   ```json
   {
     "ConnectionStrings": {
       "DefaultConnection": "Server=YOUR_SQL_SERVER;Database=TaskTrackerDB;Trusted_Connection=True;TrustServerCertificate=True;"
     },
     "Firebase": {
       "ProjectId": "YOUR_FIREBASE_PROJECT_ID"
     }
   }
   ```
4. Apply database migrations to create the tables:
   ```bash
   dotnet ef database update
   ```
5. Run the backend server:
   ```bash
   dotnet run --launch-profile http
   ```
   *The API will start at `http://localhost:5221`.*

---

### 2. React Web Frontend Setup

#### Prerequisites
- **Node.js**: v18.0.0 or higher.

#### Setup Steps
1. Navigate to the Website folder:
   ```bash
   cd ../Website
   ```
2. Install the packages:
   ```bash
   npm install
   ```
3. Create a `.env` file in the root of the `Website/` folder and add your Firebase credentials:
   ```env
   VITE_FIREBASE_API_KEY="YOUR_FIREBASE_API_KEY"
   VITE_FIREBASE_AUTH_DOMAIN="YOUR_PROJECT_ID.firebaseapp.com"
   VITE_FIREBASE_PROJECT_ID="YOUR_PROJECT_ID"
   VITE_FIREBASE_STORAGE_BUCKET="YOUR_PROJECT_ID.firebasestorage.app"
   VITE_FIREBASE_MESSAGING_SENDER_ID="YOUR_SENDER_ID"
   VITE_FIREBASE_APP_ID="YOUR_APP_ID"
   VITE_FIREBASE_MEASUREMENT_ID="YOUR_MEASUREMENT_ID"
   ```
   *(Note: Do not commit this `.env` file to Git. It is automatically ignored).*
4. Start the development server:
   ```bash
   npm run dev
   ```
   *Open `http://localhost:5173` to view the website.*

---

### 3. Android Mobile Application Setup

#### Prerequisites
- **Android Studio**: Ladybug or newer.
- **JDK**: Version 21.

#### Setup Steps
1. Navigate to the Application folder:
   ```bash
   cd ../Application
   ```
2. Create or open the `local.properties` file in the `Application/` folder and define your Firebase API Key:
   ```properties
   firebase.apiKey="YOUR_FIREBASE_API_KEY"
   ```
   *(Note: This file is ignored by Git. Gradle will compile it securely into the app).*
3. Open the `Application/` folder in Android Studio.
4. Let Gradle sync and build the project.
5. Run the app on an Android emulator or test device.
   *The app is pre-configured to talk to the backend via `http://10.0.2.2:5221/api/`.*
