using System.ComponentModel.DataAnnotations;
namespace TaskTrackerAPI.DTOs
{
    public class ProjectResponseDto
    {
        public int Id { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public DateTime? Deadline { get; set; }
        public DateTime CreatedAt { get; set; }
        public int TotalTasks { get; set; }
    }

   public class CreateProjectDto
    {
        [Required]
        [StringLength(150, MinimumLength = 3, ErrorMessage = "Project name must be between 3 and 150 characters.")]
        public string Name { get; set; } = string.Empty;

        [MaxLength(500, ErrorMessage = "Description cannot exceed 500 characters.")]
        public string Description { get; set; } = string.Empty;
        
        public DateTime? Deadline { get; set; }
    }
}