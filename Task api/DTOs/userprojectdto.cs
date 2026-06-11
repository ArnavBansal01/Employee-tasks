using System.ComponentModel.DataAnnotations;

namespace TaskTrackerAPI.DTOs
{
    // Used for POST and PUT requests
    public class ProjectAssignmentDto
    {
        [Required]
        public List<int> UserIds { get; set; } = new List<int>();
    }

    // Used for the GET request to show who is on the project
    public class AssignedUserDto
    {
        public int UserId { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
    }
}