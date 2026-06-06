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
            var projects = await _context.Projects
                .Include(p => p.Tasks)
                .ToListAsync();

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
            var p = await _context.Projects
                .Include(p => p.Tasks)
                .FirstOrDefaultAsync(p => p.Id == id);

            if (p == null) return NotFound();

            var result = new ProjectResponseDto
            {
                Id = p.Id,
                Name = p.Name,
                Description = p.Description,
                Deadline = p.Deadline,
                CreatedAt = p.CreatedAt,
                TotalTasks = p.Tasks.Count
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

            _context.Projects.Remove(project);
            await _context.SaveChangesAsync();
            return NoContent();
        }
        // POST: api/projects/1/members/2  (Admin adds user 2 to project 1)
        [Authorize(Roles = "Admin")]
        [HttpPost("{projectId}/members/{userId}")]
        public async Task<IActionResult> AddMember(int projectId, int userId)
{
    var project = await _context.Projects.FindAsync(projectId);
    if (project == null) return NotFound("Project not found.");

    var user = await _context.Users.FindAsync(userId);
    if (user == null) return NotFound("User not found.");

    var exists = await _context.UserProjects
        .AnyAsync(up => up.ProjectId == projectId && up.UserId == userId);
    if (exists) return BadRequest("User already in project.");

    _context.UserProjects.Add(new UserProject
    {
        ProjectId = projectId,
        UserId = userId
    });

    await _context.SaveChangesAsync();
    return Ok(new { message = $"{user.Name} added to {project.Name}." });
}

    // GET: api/projects/1/members
    [HttpGet("{projectId}/members")]
    public async Task<IActionResult> GetMembers(int projectId)
    {
        var project = await _context.Projects.FindAsync(projectId);
        if (project == null) return NotFound("Project not found.");

        var members = await _context.UserProjects
            .Where(up => up.ProjectId == projectId)
            .Include(up => up.User)
            .Select(up => new UserResponseDto
            {
                Id = up.User.Id,
                Name = up.User.Name,
                Email = up.User.Email,
                Role = up.User.Role,
                CreatedAt = up.User.CreatedAt
            })
            .ToListAsync();

        return Ok(members);
}
    }
}