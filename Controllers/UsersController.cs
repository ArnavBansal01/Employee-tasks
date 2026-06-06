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
    public class UsersController : ControllerBase
    {
        private readonly AppDbContext _context;

        public UsersController(AppDbContext context)
        {
            _context = context;
        }

        // GET: api/users
        [Authorize(Roles = "Admin")]
        [HttpGet]
        public async Task<IActionResult> GetAll([FromQuery] PaginationParams @params)
        {
            var query = _context.Users.AsQueryable();

            var totalCount = await query.CountAsync();

            var users = await query
                .Skip((@params.PageNumber - 1) * @params.PageSize)
                .Take(@params.PageSize)
                .ToListAsync();

            var resultDtos = users.Select(u => new UserResponseDto
            {
                Id = u.Id,
                Name = u.Name,
                Email = u.Email,
                Role = u.Role,
                CreatedAt = u.CreatedAt
            }).ToList();

            var pagedResult = new PagedResult<UserResponseDto>
            {
                Items = resultDtos,
                TotalCount = totalCount,
                PageNumber = @params.PageNumber,
                PageSize = @params.PageSize
            };

            return Ok(pagedResult);
        }
        

        // GET: api/users/1
        [HttpGet("{id}")]
        public async Task<IActionResult> GetById(int id)
        {
            // 1. Get the identity of the person making the request
            var currentUserId = int.Parse(User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)!.Value);
            var currentUserRole = User.FindFirst(System.Security.Claims.ClaimTypes.Role)!.Value;

            // 2. SECURITY FIX: If they aren't an Admin, they can only view their own ID
            if (currentUserRole != "Admin" && currentUserId != id)
            {
               return StatusCode(403, new { message = "Access Denied: You do not have permission to view other users' profiles." });
            }

            var u = await _context.Users.FindAsync(id);
            if (u == null) return NotFound();

            var result = new UserResponseDto
            {
                Id = u.Id,
                Name = u.Name,
                Email = u.Email,
                Role = u.Role,
                CreatedAt = u.CreatedAt
            };

            return Ok(result);
        }

        // POST: api/users
        [AllowAnonymous]
        [HttpPost]
        public async Task<IActionResult> Create(CreateUserDto dto)
        {
            // Check if email already exists
            var exists = await _context.Users
                .AnyAsync(u => u.Email == dto.Email);
            if (exists) return BadRequest("Email already registered.");

           var user = new User
            {
                Name = dto.Name,
                Email = dto.Email,
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(dto.Password),
                Role = "Employee"  // always Employee, never trust client input
            };

            _context.Users.Add(user);
            await _context.SaveChangesAsync();

            return CreatedAtAction(nameof(GetById), new { id = user.Id }, new UserResponseDto
            {
                Id = user.Id,
                Name = user.Name,
                Email = user.Email,
                Role = user.Role,
                CreatedAt = user.CreatedAt
            });
        }

        // DELETE: api/users/1
        [Authorize(Roles = "Admin")]
        [HttpDelete("{id}")]
        public async Task<IActionResult> Delete(int id)
        {
            var user = await _context.Users.FindAsync(id);
            if (user == null) return NotFound();
            // Check if user has active tasks
            var hasTasks = await _context.Tasks.AnyAsync(t => t.UserId == id);
            if (hasTasks) 
                return BadRequest("Cannot delete user because they have assigned tasks. Reassign tasks first.");
             _context.Users.Remove(user);
            await _context.SaveChangesAsync();
            return NoContent();
        }
        // PUT: api/users/1/promote
        [Authorize(Roles = "Admin")]
        [HttpPut("{id}/promote")]
        public async Task<IActionResult> Promote(int id)
        {
            var user = await _context.Users.FindAsync(id);
            if (user == null) return NotFound();

            user.Role = "Admin";
            await _context.SaveChangesAsync();

            return Ok(new { message = $"{user.Name} has been promoted to Admin." });
        }
    }
}