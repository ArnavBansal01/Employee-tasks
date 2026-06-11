using System.ComponentModel.DataAnnotations;
namespace TaskTrackerAPI.DTOs
{
    public class TaskResponseDto
    {
        public int Id { get; set; }
        public string Title { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public string Status { get; set; } = string.Empty;
        public string Priority { get; set; } = string.Empty;
        public DateTime? Deadline { get; set; }
        public DateTime CreatedAt { get; set; }
        public DateTime UpdatedAt { get; set; }
        public int UserId { get; set; }
        public string AssignedTo { get; set; } = string.Empty;
        public int ProjectId { get; set; }
        public string ProjectName { get; set; } = string.Empty;
    }

    public class CreateTaskDto
    {
        [Required]
        [MaxLength(200)]
        public string Title { get; set; } = string.Empty;

        [MaxLength(1000)]
        public string Description { get; set; } = string.Empty;

        [RegularExpression("^(Pending|InProgress|Completed)$", ErrorMessage = "Status must be Pending, InProgress, or Completed")]
        public string Status { get; set; } = "Pending";

        public string Priority { get; set; } = "Medium";
        public DateTime? Deadline { get; set; }
        
        [Required]
        public int UserId { get; set; }
        
        [Required]
        public int ProjectId { get; set; }
    }


    public class UpdateTaskDto
{
    [Required]
    [MaxLength(200)]
    public string Title { get; set; } = string.Empty;

    [MaxLength(1000)]
    public string Description { get; set; } = string.Empty;

    [RegularExpression("^(Pending|InProgress|Completed)$")]
    public string Status { get; set; } = "Pending";

    public string Priority { get; set; } = "Medium";
    public DateTime? Deadline { get; set; }
}
}