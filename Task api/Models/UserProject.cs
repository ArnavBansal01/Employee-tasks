namespace TaskTrackerAPI.Models
{
    public class UserProject
    {
        public int UserId { get; set; }
        public int ProjectId { get; set; }

        // Navigation properties
        public User User { get; set; } = null!;
        public Project Project { get; set; } = null!;
    }
}