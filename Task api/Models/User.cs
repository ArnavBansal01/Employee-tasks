namespace TaskTrackerAPI.Models
{
    public class User
    {
        public int Id { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string PasswordHash { get; set; } = string.Empty;
        public string Role { get; set; } = "Employee";
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
        public string SecurityStamp { get; set; } = Guid.NewGuid().ToString();

        // NEW: links this row to the Firebase user. Nullable/empty for users
        // created before the migration, until they log in via Firebase once.
        public string? FirebaseUid { get; set; }

        // Navigation properties
        public ICollection<TaskItem> Tasks { get; set; } = new List<TaskItem>();
        public ICollection<UserProject> UserProjects { get; set; } = new List<UserProject>();
    }}