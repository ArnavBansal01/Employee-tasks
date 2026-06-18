# TaskTrackerAPI Backend Analysis & Firebase Auth Migration Guide

This document provides a comprehensive overview of the `TaskTrackerAPI` backend codebase, its current architecture, its file structure, and a detailed guide on how to replace the local JWT authentication system with **Firebase Authentication**.

---

## 1. Directory File Tree

Here is the file structure of the `Task api` directory (the backend project):

```text
TaskTrackerAPI/Task api/
├── TaskTrackerAPI.csproj           # Project configuration & NuGet dependencies
├── Program.cs                      # Main entrypoint, services configuration, and middleware pipeline
├── appsettings.json                # Main configuration settings (connection string, local JWT settings)
├── appsettings.Development.json    # Development-specific configuration settings
├── Properties/
│   └── launchSettings.json         # Development server profiles (ports, environment variables)
├── Data/
│   └── AppDbContext.cs             # EF Core Database Context for SQL Server
├── Models/
│   ├── User.cs                     # User entity model (stores name, email, password hashes, and roles)
│   ├── Project.cs                  # Project entity model
│   ├── TaskItem.cs                 # TaskItem entity model
│   └── UserProject.cs              # Joint entity table for many-to-many relationship (User <-> Project)
├── DTOs/
│   ├── UserDto.cs                  # Data Transfer Objects for user actions (UserResponseDto, CreateUserDto, LoginDto)
│   ├── ProjectDto.cs               # DTOs for Projects
│   ├── TaskDto.cs                  # DTOs for Tasks
│   ├── userprojectdto.cs           # DTOs for Project assignments
│   └── PaginationHelpers.cs        # Common helpers for paginated queries
├── Controllers/
│   ├── AuthController.cs           # Handles authentication (login, local token generation)
│   ├── UsersController.cs          # Handles user CRUD, registration, promotion/demotion
│   ├── ProjectsController.cs       # Handles project CRUD and listings
│   ├── TasksController.cs          # Handles task CRUD, creation, assignments, and status updates
│   └── ProjectAssignmentsController.cs # Handles project membership assignments (assign, sync)
└── Migrations/                     # Entity Framework Core database migrations
```

---

## 2. Current Architecture & Technologies

The backend is built with modern, performant Microsoft technologies:

*   **Framework:** .NET 10.0 (using Minimal API style builders mapping controller routing).
*   **API Pattern:** Controller-based RESTful API (uses ASP.NET Core Controllers).
*   **Database Access:** Entity Framework Core (EF Core) 10.0, Code-First approach.
*   **Database Engine:** SQL Server (`Microsoft.EntityFrameworkCore.SqlServer`).
*   **Security & Hashing:** `BCrypt.Net-Next` for password hashing and verification.
*   **Authentication:** Local JWT Bearer Authentication (`Microsoft.AspNetCore.Authentication.JwtBearer`).
    *   Generates JWT tokens locally in `AuthController` using a configured `SecretKey`.
    *   Validates tokens on incoming requests.
    *   Includes validation checks inside middleware using a `SecurityStamp` database check to enable token invalidation (e.g. after demoting/promoting a user).
*   **Authorization:** Role-Based Access Control (RBAC) using roles `Admin` and `Employee` via `[Authorize(Roles = "...")]` attributes.
*   **Rate Limiting:** IP-based global rate limiter (100 requests/minute) and dedicated login endpoint rate limiter (5 requests/minute).

---

## 3. Shifting from Local JWT to Firebase Authentication

When moving from local JWT to Firebase Authentication, credentials management (saving email/password, verifying login credentials, token lifetime management) shifts entirely to Firebase. 

However, since database records (Tasks, Projects) are linked to a user using an integer foreign key (`UserId`), we need to bridge Firebase's alphanumeric UID (User ID) with our SQL database's integer ID.

Here is the recommended architecture and step-by-step migration blueprint.

### Recommended Architectural Design: Option Mapping

To minimize disruptions to the existing codebase, we should:
1.  **Keep the `User.Id` integer primary key** for database relationships (this avoids changing foreign keys in `Tasks` and `UserProjects` tables to strings, preventing a massive, risky database migration).
2.  **Add a `FirebaseUid` column (string)** to the `User` table to match the incoming Firebase Token's user ID.
3.  **Use custom Claims Transformation (`IClaimsTransformation`)** in `Program.cs`. When a request arrives with a valid Firebase JWT:
    *   We inspect the Firebase UID from the token.
    *   We lookup the matching user in the database.
    *   We inject claims for the local Integer `UserId` (`ClaimTypes.NameIdentifier`) and the user's role (`ClaimTypes.Role`) dynamically into the request context.
    *   **Result:** Almost no controller code needs to be modified! Existing controllers will continue to read `User.FindFirst(ClaimTypes.NameIdentifier)` and work exactly as before.

---

## 4. Step-by-Step Backend Migration Plan

Here are the precise modifications required in the backend project:

### Step 1: Install Firebase Admin SDK & Update Dependencies
First, remove local BCrypt and add Firebase capabilities:
*   Remove package: `BCrypt.Net-Next` (since Firebase will handle hashing).
*   Keep `Microsoft.AspNetCore.Authentication.JwtBearer` (used to validate Firebase JWTs).
*   (Optional but recommended) Install `FirebaseAdmin` NuGet package if you want to allow the backend to create/delete users directly.

### Step 2: Update database model (`User.cs`)
Modify [User.cs](file:///e:/EmployeeTaskTracker/TaskTrackerAPI/Task%20api/Models/User.cs) to remove password hashes and add the Firebase UID linking column:

```csharp
namespace TaskTrackerAPI.Models
{
    public class User
    {
        public int Id { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        
        // Add this to link SQL user to Firebase Auth
        public string FirebaseUid { get; set; } = string.Empty; 
        
        public string Role { get; set; } = "Employee";
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        // Remove: PasswordHash (managed by Firebase)
        // Remove: SecurityStamp (managed by Firebase token revocation)

        // Navigation properties
        public ICollection<TaskItem> Tasks { get; set; } = new List<TaskItem>();
        public ICollection<UserProject> UserProjects { get; set; } = new List<UserProject>();
    }
}
```

After modifying the model, run EF Core tools to generate and apply a migration:
```bash
dotnet ef migrations add AddFirebaseUidAndRemovePasswords
dotnet ef database update
```

### Step 3: Implement Claims Transformation
Create a new class `Services/FirebaseClaimsTransformation.cs` to resolve the local user details dynamically when a Firebase JWT is authenticated:

```csharp
using Microsoft.AspNetCore.Authentication;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;
using TaskTrackerAPI.Data;

namespace TaskTrackerAPI.Services
{
    public class FirebaseClaimsTransformation : IClaimsTransformation
    {
        private readonly IServiceProvider _serviceProvider;

        public FirebaseClaimsTransformation(IServiceProvider serviceProvider)
        {
            _serviceProvider = serviceProvider;
        }

        public async Task<ClaimsPrincipal> TransformAsync(ClaimsPrincipal principal)
        {
            // If the user isn't authenticated, do nothing
            if (principal.Identity == null || !principal.Identity.IsAuthenticated)
                return principal;

            // Firebase User ID is in the 'sub' or NameIdentifier claim
            var firebaseUid = principal.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(firebaseUid))
                return principal;

            // Create a new scope to fetch DbContext
            using var scope = _serviceProvider.CreateScope();
            var dbContext = scope.ServiceProvider.GetRequiredService<AppDbContext>();

            // Find user in DB by Firebase UID
            var dbUser = await dbContext.Users.FirstOrDefaultAsync(u => u.FirebaseUid == firebaseUid);
            if (dbUser != null)
            {
                var identity = (ClaimsIdentity)principal.Identity;

                // Add local integer database ID as the NameIdentifier claim
                var idClaim = identity.FindFirst(ClaimTypes.NameIdentifier);
                if (idClaim != null) identity.RemoveClaim(idClaim);
                identity.AddClaim(new Claim(ClaimTypes.NameIdentifier, dbUser.Id.ToString()));

                // Add user role claim
                var roleClaim = identity.FindFirst(ClaimTypes.Role);
                if (roleClaim != null) identity.RemoveClaim(roleClaim);
                identity.AddClaim(new Claim(ClaimTypes.Role, dbUser.Role));
                
                // Add user email and name if needed
                if (!identity.HasClaim(c => c.Type == ClaimTypes.Email))
                    identity.AddClaim(new Claim(ClaimTypes.Email, dbUser.Email));
            }

            return principal;
        }
    }
}
```

### Step 4: Update `Program.cs` Configurations
Modify [Program.cs](file:///e:/EmployeeTaskTracker/TaskTrackerAPI/Task%20api/Program.cs):
1.  **Configure JwtBearer to target Firebase**:
    Replace the local `AddJwtBearer` block with Firebase configuration:
    ```csharp
    var firebaseProjectId = builder.Configuration["Firebase:ProjectId"] 
        ?? throw new InvalidOperationException("Firebase ProjectId is not configured.");

    builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
        .AddJwtBearer(options =>
        {
            options.Authority = $"https://securetoken.google.com/{firebaseProjectId}";
            options.TokenValidationParameters = new TokenValidationParameters
            {
                ValidateIssuer = true,
                ValidIssuer = $"https://securetoken.google.com/{firebaseProjectId}",
                ValidateAudience = true,
                ValidAudience = firebaseProjectId,
                ValidateLifetime = true
            };
        });
    ```
2.  **Register the Claims Transformer**:
    Add this service configuration:
    ```csharp
    builder.Services.AddTransient<IClaimsTransformation, FirebaseClaimsTransformation>();
    ```
3.  **Clean up the security stamp validation middleware**:
    In the middleware pipeline, remove the lines checking `dbUser.SecurityStamp != stampClaim` (lines 107-128 in your current `Program.cs`), as Firebase token verification and validation will now run on the middleware level.

### Step 5: Adjust `AuthController.cs`
Since Firebase verifies login credentials directly on the client (or via client SDK), the local `/api/auth/login` endpoint is no longer strictly needed. 
However, you can replace it with a token verification/sync endpoint. For instance:
*   The client logins with Firebase directly on the application/website.
*   The client gets a Firebase ID Token and passes it in the header.
*   You can create a `POST /api/auth/sync` or keep `POST /api/auth/login` accepting the Firebase ID Token in the body. If the user doesn't exist in the database, create them; otherwise, verify their presence and return their profile.

### Step 6: Adjust User Creation (`UsersController.cs`)
When registering a new user (`POST /api/users`), the system must write the Firebase UID:
*   Update `CreateUserDto` to accept a `FirebaseUid` instead of a `Password` (since the password will be configured in Firebase during frontend registration).
*   Alternatively, use the Firebase Admin SDK in the backend to create the user:
    ```csharp
    var userArgs = new UserRecordArgs
    {
        Email = dto.Email,
        Password = dto.Password,
        DisplayName = dto.Name,
    };
    var userRecord = await FirebaseAuth.DefaultInstance.CreateUserAsync(userArgs);
    var firebaseUid = userRecord.Uid;
    ```
    This allows the backend to orchestrate the registration.

---

## 5. Client Integration Notes

Whether configuring the Android App (`Application`) or the React Website (`Website`), the authentication flow should be changed to:

1.  Initialize Firebase SDK on the client side.
2.  Use Firebase auth APIs (e.g. `signInWithEmailAndPassword`) to authenticate.
3.  Get the ID token from Firebase:
    *   *React:* `const token = await firebase.auth().currentUser.getIdToken();`
    *   *Android:* `val token = Tasks.await(user.getIdToken(true)).token`
4.  Include this token in the header of all HTTP requests:
    ```http
    Authorization: Bearer <firebase-id-token>
    ```

This details everything required to carry out the transformation!
