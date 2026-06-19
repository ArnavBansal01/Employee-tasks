using FirebaseAdmin.Auth; 
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
        // UPDATED: Now requires Admin role and uses Firebase Admin SDK
        [Authorize(Roles = "Admin")]
        [HttpPost]
        public async Task<IActionResult> Create(CreateUserDto dto)
        {
            // Check if email already exists locally
            var exists = await _context.Users
                .AnyAsync(u => u.Email == dto.Email);
            if (exists) return BadRequest("Email already registered in the database.");

            try
            {
                // 1. Forge the user inside Google's Firebase Vault
                var firebaseArgs = new UserRecordArgs
                {
                    Email = dto.Email,
                    Password = dto.Password, // Firebase handles secure hashing automatically!
                    DisplayName = dto.Name,
                    Disabled = false
                };

                UserRecord firebaseUser = await FirebaseAuth.DefaultInstance.CreateUserAsync(firebaseArgs);
                string generatedFirebaseUid = firebaseUser.Uid;

                // 2. Save the loyal subject to your SQL Database
                var user = new User
                {
                    Name = dto.Name,
                    Email = dto.Email,
                    Role = "Employee", // always Employee initially
                    FirebaseUid = generatedFirebaseUid, // Linked securely
                    CreatedAt = DateTime.UtcNow,
                    SecurityStamp = Guid.NewGuid().ToString() // Generate their first stamp
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
            catch (FirebaseAuthException ex)
            {
                return BadRequest($"Firebase Provisioning Failed: {ex.Message}");
            }
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
            user.SecurityStamp = Guid.NewGuid().ToString(); // Rotate security stamp to invalidate session
            await _context.SaveChangesAsync();

            // Commented out to avoid slow Firebase network checks on every request
            // if (!string.IsNullOrEmpty(user.FirebaseUid))
            //     await FirebaseAuth.DefaultInstance.RevokeRefreshTokensAsync(user.FirebaseUid);

            return Ok(new { message = $"{user.Name} has been promoted to Admin." });
        }

        // PUT: api/users/1/demote
        [Authorize(Roles = "Admin")]
        [HttpPut("{id}/demote")]
        public async Task<IActionResult> Demote(int id)
        {
            var user = await _context.Users.FindAsync(id);
            if (user == null) return NotFound();

            user.Role = "Employee";
            user.SecurityStamp = Guid.NewGuid().ToString(); // Rotate security stamp to invalidate session
            await _context.SaveChangesAsync();

            // Commented out to avoid slow Firebase network checks on every request
            // if (!string.IsNullOrEmpty(user.FirebaseUid))
            //     await FirebaseAuth.DefaultInstance.RevokeRefreshTokensAsync(user.FirebaseUid);

            return Ok(new { message = $"{user.Name} has been demoted to Employee." });
        }
    }
}