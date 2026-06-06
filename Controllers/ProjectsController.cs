using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using TaskTrackerAPI.Data;
using TaskTrackerAPI.DTOs;
using TaskTrackerAPI.Models;
using Microsoft.AspNetCore.Authorization;

namespace TaskTrackerAPI.Controllers
{   [Authorize]
    [ApiController]
    [Route("api/[controller]")]
    public class ProjectsController : ControllerBase
    {
        private readonly AppDbContext _context;

        public ProjectsController(AppDbContext context)
        {
            _context = context;
        }

        // GET: api/projects
        [HttpGet]
public async Task<IActionResult> GetAll()
{
    var currentUserId = int.Parse(User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)!.Value);
    var currentUserRole = User.FindFirst(System.Security.Claims.ClaimTypes.Role)!.Value;

    IQueryable<Project> query = _context.Projects.Include(p => p.Tasks);

    if (currentUserRole != "Admin")
        query = query.Where(p => p.UserProjects.Any(up => up.UserId == currentUserId));

    var projects = await query.ToListAsync();

    var result = projects.Select(p => new ProjectResponseDto
    {
        Id = p.Id,
        Name = p.Name,
        Description = p.Description,
        Deadline = p.Deadline,
        CreatedAt = p.CreatedAt,
        TotalTasks = p.Tasks.Count
    });

    return Ok(result);
}

    // GET: api/projects/1
        [HttpGet("{id}")]
        public async Task<IActionResult> GetById(int id)
        {
            var currentUserId = int.Parse(User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)!.Value);
            var currentUserRole = User.FindFirst(System.Security.Claims.ClaimTypes.Role)!.Value;

            // 1. OPTIMIZATION: Do the counting and security checks inside the SQL query
            var projectData = await _context.Projects
                .Where(p => p.Id == id)
                .Select(p => new 
                {
                    Project = p,
                    TaskCount = p.Tasks.Count, // SQL does the counting, saving massive RAM
                    IsMember = p.UserProjects.Any(up => up.UserId == currentUserId)
                })
                .FirstOrDefaultAsync();

            // 2. Clean Error Message for Not Found
            if (projectData == null) 
            {
                return NotFound(new { message = $"Project with ID {id} not found." });
            }

            // 3. Clean Error Message for Security Block
            if (currentUserRole != "Admin" && !projectData.IsMember)
            {
                return StatusCode(403, new { message = "Access Denied: You can only view details of projects you are assigned to." });
            }

            // 4. Map to DTO
            var result = new ProjectResponseDto
            {
                Id = projectData.Project.Id,
                Name = projectData.Project.Name,
                Description = projectData.Project.Description,
                Deadline = projectData.Project.Deadline,
                CreatedAt = projectData.Project.CreatedAt,
                TotalTasks = projectData.TaskCount
            };

            return Ok(result);
        }

        // POST: api/projects
        [Authorize(Roles = "Admin")]
        [HttpPost]
        public async Task<IActionResult> Create(CreateProjectDto dto)
        {
            var project = new Project
            {
                Name = dto.Name,
                Description = dto.Description,
                Deadline = dto.Deadline
            };

            _context.Projects.Add(project);
            await _context.SaveChangesAsync();

            return CreatedAtAction(nameof(GetById), new { id = project.Id }, new ProjectResponseDto
            {
                Id = project.Id,
                Name = project.Name,
                Description = project.Description,
                Deadline = project.Deadline,
                CreatedAt = project.CreatedAt,
                TotalTasks = 0
            });
        }

        // PUT: api/projects/1
        [Authorize(Roles = "Admin")]
        [HttpPut("{id}")]
        public async Task<IActionResult> Update(int id, CreateProjectDto dto)
        {
            var project = await _context.Projects.FindAsync(id);
            if (project == null) return NotFound();

            project.Name = dto.Name;
            project.Description = dto.Description;
            project.Deadline = dto.Deadline;

            await _context.SaveChangesAsync();
            return Ok(project);
        }

        // DELETE: api/projects/1
        [Authorize(Roles = "Admin")]
        [HttpDelete("{id}")]
        public async Task<IActionResult> Delete(int id)
        {
            var project = await _context.Projects.FindAsync(id);
            if (project == null) return NotFound();

            // 1. Find and delete all tasks associated with this project
            var projectTasks = await _context.Tasks
                .Where(t => t.ProjectId == id)
                .ToListAsync();
            _context.Tasks.RemoveRange(projectTasks);

            // 2. Find and delete all user assignments for this project
            var projectAssignments = await _context.UserProjects
                .Where(up => up.ProjectId == id)
                .ToListAsync();
            _context.UserProjects.RemoveRange(projectAssignments);

            // 3. Finally, delete the project itself
            _context.Projects.Remove(project);
            
            // Save all the deletions at once
            await _context.SaveChangesAsync();
            
            return NoContent();
        }
       
    }
}