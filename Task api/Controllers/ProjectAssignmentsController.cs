using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using TaskTrackerAPI.Data;
using TaskTrackerAPI.DTOs;
using TaskTrackerAPI.Models;

namespace TaskTrackerAPI.Controllers
{
    [Authorize]
    [ApiController]
    [Route("api/[controller]")]
    public class ProjectAssignmentsController : ControllerBase
    {
        private readonly AppDbContext _context;

        public ProjectAssignmentsController(AppDbContext context)
        {
            _context = context;
        }

        // GET: api/ProjectAssignments/5
        // Purpose: See all users assigned to a specific project
        [HttpGet("{projectId}")]
        public async Task<IActionResult> GetAssignedUsers(int projectId)
        {
            // 1. Get the current user's identity
            var currentUserId = int.Parse(User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)!.Value);
            var currentUserRole = User.FindFirst(System.Security.Claims.ClaimTypes.Role)!.Value;

            // 2. SECURITY FIX (IDOR): If they aren't an admin, verify they belong to this project
            if (currentUserRole != "Admin")
            {
                var isMember = await _context.UserProjects
                    .AnyAsync(up => up.ProjectId == projectId && up.UserId == currentUserId);
                
                if (!isMember)
                {
                    return StatusCode(403, new { message = "Access Denied: You can only view members of projects you are assigned to." });
                }
            }

            var projectExists = await _context.Projects.AnyAsync(p => p.Id == projectId);
            if (!projectExists) return NotFound("Project not found.");

            var users = await _context.UserProjects
                .Where(up => up.ProjectId == projectId)
                .Include(up => up.User)
                .Select(up => new AssignedUserDto
                {
                    UserId = up.User.Id,
                    Name = up.User.Name,
                    Email = up.User.Email
                })
                .ToListAsync();

            return Ok(users);
        }

        // POST: api/ProjectAssignments/5
        // Purpose: Add a list of users to a project without removing existing ones
        [Authorize(Roles = "Admin")]
        [HttpPost("{projectId}")]
        public async Task<IActionResult> AssignUsers(int projectId, [FromBody] ProjectAssignmentDto dto)
        {

            // Verify all incoming UserIds actually exist in the database
            var existingUsersInDb = await _context.Users
                .Where(u => dto.UserIds.Contains(u.Id))
                .Select(u => u.Id)
                .ToListAsync();

            if (existingUsersInDb.Count != dto.UserIds.Distinct().Count())
            {
                return BadRequest("One or more User IDs provided do not exist.");
            }

            var projectExists = await _context.Projects.AnyAsync(p => p.Id == projectId);
            if (!projectExists) return NotFound("Project not found.");

            // Find users who are ALREADY assigned so we don't create duplicate database rows
            var existingUserIds = await _context.UserProjects
                .Where(up => up.ProjectId == projectId)
                .Select(up => up.UserId)
                .ToListAsync();

            // Filter out the duplicates and prepare the new assignments
            var newAssignments = dto.UserIds
                .Where(id => !existingUserIds.Contains(id))
                .Distinct() // Prevent duplicates in the incoming JSON
                .Select(userId => new UserProject { ProjectId = projectId, UserId = userId })
                .ToList();

            if (newAssignments.Any())
            {
                await _context.UserProjects.AddRangeAsync(newAssignments);
                await _context.SaveChangesAsync();
            }

            return Ok(new { message = $"Successfully added {newAssignments.Count} new users to the project." });
        }

        // PUT: api/ProjectAssignments/5
        // Purpose: EXACT sync. Whatever list of IDs you send is exactly who will be on the project.
        [Authorize(Roles = "Admin")]
        [HttpPut("{projectId}")]
        public async Task<IActionResult> SyncAssignments(int projectId, [FromBody] ProjectAssignmentDto dto)
        {

        // Verify all incoming UserIds actually exist in the database
        var existingUsersInDb = await _context.Users
            .Where(u => dto.UserIds.Contains(u.Id))
            .Select(u => u.Id)
            .ToListAsync();

        if (existingUsersInDb.Count != dto.UserIds.Distinct().Count())
        {
            return BadRequest("One or more User IDs provided do not exist.");
        }

            var projectExists = await _context.Projects.AnyAsync(p => p.Id == projectId);
            if (!projectExists) return NotFound("Project not found.");

            // 1. Get all CURRENT assignments for this project
            var currentAssignments = await _context.UserProjects
                .Where(up => up.ProjectId == projectId)
                .ToListAsync();

            // 2. Remove all of them (Wipe the slate clean)
            _context.UserProjects.RemoveRange(currentAssignments);

            // 3. Add the exact list of users passed in the JSON payload
            var newAssignments = dto.UserIds
                .Distinct()
                .Select(userId => new UserProject { ProjectId = projectId, UserId = userId })
                .ToList();

            await _context.UserProjects.AddRangeAsync(newAssignments);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Project assignments successfully synced." });
        }
    }
}