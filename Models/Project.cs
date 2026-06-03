namespace TaskTrackerAPI.Models
{
    public class Project
    {
        public int Id { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public DateTime? Deadline { get; set; }
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        // Navigation properties
        public ICollection<TaskItem> Tasks { get; set; } = new List<TaskItem>();
        public ICollection<UserProject> UserProjects { get; set; } = new List<UserProject>();
    }
}