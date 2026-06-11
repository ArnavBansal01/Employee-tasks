using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using TaskTrackerAPI.Data;
using TaskTrackerAPI.DTOs;
using TaskTrackerAPI.Models;
using Microsoft.AspNetCore.Authorization;

namespace TaskTrackerAPI.Controllers
{
    [Authorize]
    [ApiController]
    [Route("api/[controller]")]
    public class TasksController : ControllerBase
    {
        private readonly AppDbContext _context;

        public TasksController(AppDbContext context)
        {
            _context = context;
        }

        // GET: api/tasks
    [HttpGet]
    public async Task<IActionResult> GetAll([FromQuery] PaginationParams @params)
    {
    var currentUserId = int.Parse(User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)!.Value);
    var currentUserRole = User.FindFirst(System.Security.Claims.ClaimTypes.Role)!.Value;

    IQueryable<TaskItem> query = _context.Tasks
        .Include(t => t.User)
        .Include(t => t.Project);

    // Employees only see their own tasks
    if (currentUserRole != "Admin")
        query = query.Where(t => t.UserId == currentUserId);

    // 1. Get the total count BEFORE we slice the data (needed for the frontend)
    var totalCount = await query.CountAsync();

    // 2. Apply Pagination: Skip the previous pages, Take the current page amount
    var tasks = await query
        .Skip((@params.PageNumber - 1) * @params.PageSize)
        .Take(@params.PageSize)
        .ToListAsync();

    // 3. Map to DTO
    var resultDtos = tasks.Select(t => new TaskResponseDto
    {
        Id = t.Id,
        Title = t.Title,
        Description = t.Description,
        Status = t.Status,
        Priority = t.Priority,
        Deadline = t.Deadline,
        CreatedAt = t.CreatedAt,
        UpdatedAt = t.UpdatedAt,
        UserId = t.UserId,
        AssignedTo = t.User != null ? t.User.Name : "Unassigned",
        ProjectId = t.ProjectId,
        ProjectName = t.Project != null ? t.Project.Name : "No Project"
    }).ToList();

    // 4. Wrap the results in our new PagedResult class
    var pagedResult = new PagedResult<TaskResponseDto>
    {
        Items = resultDtos,
        TotalCount = totalCount,
        PageNumber = @params.PageNumber,
        PageSize = @params.PageSize
    };

    return Ok(pagedResult);
}

        // GET: api/tasks/1
       [HttpGet("{id}")]
public async Task<IActionResult> GetById(int id)
{
    var currentUserId = int.Parse(User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)!.Value);
    var currentUserRole = User.FindFirst(System.Security.Claims.ClaimTypes.Role)!.Value;

    var t = await _context.Tasks
        .Include(t => t.User)
        .Include(t => t.Project)
        .FirstOrDefaultAsync(t => t.Id == id);

    if (t == null) return NotFound();

    // Employee can only see their own tasks
    if (currentUserRole != "Admin" && t.UserId != currentUserId)
        return Forbid();

    var result = new TaskResponseDto
    {
        Id = t.Id,
        Title = t.Title,
        Description = t.Description,
        Status = t.Status,
        Priority = t.Priority,
        Deadline = t.Deadline,
        CreatedAt = t.CreatedAt,
        UpdatedAt = t.UpdatedAt,
        UserId = t.UserId,
        AssignedTo = t.User != null ? t.User.Name : "Unassigned",
        ProjectId = t.ProjectId,
        ProjectName = t.Project != null ? t.Project.Name : "No Project"
    };

    return Ok(result);


}

        // POST: api/tasks
        [Authorize(Roles = "Admin")]
        [HttpPost]
        public async Task<IActionResult> Create(CreateTaskDto dto)
        {
            var projectExists = await _context.Projects.AnyAsync(p => p.Id == dto.ProjectId);
    if (!projectExists) 
    {
        return BadRequest(new { message = $"Project with ID {dto.ProjectId} does not exist." });
    }

    // 2. Check if the User actually exists
    var userExists = await _context.Users.AnyAsync(u => u.Id == dto.UserId);
    if (!userExists)
    {
        return BadRequest(new { message = $"User with ID {dto.UserId} does not exist." });
    }
        var isMember = await _context.UserProjects
        .AnyAsync(up => up.ProjectId == dto.ProjectId && up.UserId == dto.UserId);

        if (!isMember)
        {
            return BadRequest(new { message = "Cannot assign task: The user is not a member of this project." });
        }
            var task = new TaskItem
            {
                Title = dto.Title,
                Description = dto.Description,
                Status = dto.Status,
                Priority = dto.Priority,
                Deadline = dto.Deadline,
                UserId = dto.UserId,
                ProjectId = dto.ProjectId
            };

            _context.Tasks.Add(task);
            await _context.SaveChangesAsync();

            // FETCH THE NAMES FOR THE RESPONSE
            var projectName = await _context.Projects
                .Where(p => p.Id == task.ProjectId)
                .Select(p => p.Name)
                .FirstOrDefaultAsync();

            var userName = await _context.Users
                .Where(u => u.Id == task.UserId)
                .Select(u => u.Name)
                .FirstOrDefaultAsync();

            // Map to Response DTO so we don't get null objects
            var responseDto = new TaskResponseDto
            {
                Id = task.Id,
                Title = task.Title,
                Description = task.Description,
                Status = task.Status,
                Priority = task.Priority,
                Deadline = task.Deadline,
                CreatedAt = task.CreatedAt,
                UpdatedAt = task.UpdatedAt,
                UserId = task.UserId,
                ProjectId = task.ProjectId,
                AssignedTo = userName ?? "Unknown User",
                ProjectName = projectName ?? "Unknown Project"
            };

            return CreatedAtAction(nameof(GetById), new { id = task.Id }, responseDto);
        }

        // PUT: api/tasks/1
        [HttpPut("{id}")]
        public async Task<IActionResult> Update(int id, UpdateTaskDto dto)
        {
            var task = await _context.Tasks.FindAsync(id);
            if (task == null) return NotFound();

            var currentUserId = int.Parse(User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)!.Value);
            var currentUserRole = User.FindFirst(System.Security.Claims.ClaimTypes.Role)!.Value;
            
            // Only Admin or the user assigned to the task can update it
            if (currentUserRole != "Admin" && task.UserId != currentUserId)
                return Forbid();

            task.Title = dto.Title;
            task.Description = dto.Description;
            task.Status = dto.Status;
            task.Priority = dto.Priority;
            task.Deadline = dto.Deadline;
            task.UpdatedAt = DateTime.UtcNow;

            await _context.SaveChangesAsync();
           // FETCH THE NAMES FOR THE RESPONSE
            var projectName = await _context.Projects
                .Where(p => p.Id == task.ProjectId)
                .Select(p => p.Name)
                .FirstOrDefaultAsync();

            var userName = await _context.Users
                .Where(u => u.Id == task.UserId)
                .Select(u => u.Name)
                .FirstOrDefaultAsync();

            // Map to Response DTO so we don't get null objects
            var responseDto = new TaskResponseDto
            {
                Id = task.Id,
                Title = task.Title,
                Description = task.Description,
                Status = task.Status,
                Priority = task.Priority,
                Deadline = task.Deadline,
                CreatedAt = task.CreatedAt,
                UpdatedAt = task.UpdatedAt,
                UserId = task.UserId,
                ProjectId = task.ProjectId,
                AssignedTo = userName ?? "Unknown User",
                ProjectName = projectName ?? "Unknown Project"
            };

            return Ok(responseDto);
        }

        // DELETE: api/tasks/1
        [Authorize(Roles = "Admin")]
        [HttpDelete("{id}")]
        public async Task<IActionResult> Delete(int id)
        {
            var task = await _context.Tasks.FindAsync(id);
            if (task == null) return NotFound();

            _context.Tasks.Remove(task);
            await _context.SaveChangesAsync();
            return NoContent();
        }
    }
}