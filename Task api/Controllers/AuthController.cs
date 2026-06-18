using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.AspNetCore.Authorization; // Added for the [Authorize] attribute
using TaskTrackerAPI.Data;
using TaskTrackerAPI.DTOs;
using TaskTrackerAPI.Models; // Added to create a new User
using Microsoft.AspNetCore.RateLimiting;

namespace TaskTrackerAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly IConfiguration _config;

        public AuthController(AppDbContext context, IConfiguration config)
        {
            _context = context;
            _config = config;
        }

        [HttpPost("login")]
        [EnableRateLimiting("login")]
        public async Task<IActionResult> Login(LoginDto dto)
        {
            var user = await _context.Users
                .FirstOrDefaultAsync(u => u.Email == dto.Email);

            if (user == null)
                return Unauthorized("Invalid email or password.");

            bool validPassword = BCrypt.Net.BCrypt.Verify(dto.Password, user.PasswordHash);
            if (!validPassword)
                return Unauthorized("Invalid email or password.");

            var token = GenerateToken(user.Id, user.Email, user.Role, user.SecurityStamp);

            return Ok(new
            {
                token,
                user = new UserResponseDto
                {
                    Id = user.Id,
                    Name = user.Name,
                    Email = user.Email,
                    Role = user.Role,
                    CreatedAt = user.CreatedAt
                }
            });
        }

        // NEW: Firebase Sync Endpoint
        [HttpPost("firebase-sync")]
        [Authorize(AuthenticationSchemes = "Firebase")] // Explicitly requires a Firebase token
        public async Task<IActionResult> SyncFirebaseUser()
        {
            // Firebase guarantees these claims exist in their tokens
            var email = User.FindFirst(ClaimTypes.Email)?.Value;
            var nameIdentifier = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;

            if (string.IsNullOrEmpty(email) || string.IsNullOrEmpty(nameIdentifier))
                return BadRequest("Invalid Firebase token structure.");

            // If the nameIdentifier is already an integer, our FirebaseClaimsTransformation 
            // already successfully found this user in the DB. We just return that integer!
            if (int.TryParse(nameIdentifier, out int parsedId))
                return Ok(new { message = "User already synced and active.", userId = parsedId });
            var firebaseUid = nameIdentifier;

            // Search by email to link existing users to their new Firebase identity
            var user = await _context.Users.FirstOrDefaultAsync(u => u.Email == email);

            if (user != null)
            {
                // Existing user, first time logging in with Firebase. Update the row!
                user.FirebaseUid = firebaseUid;
                await _context.SaveChangesAsync();
                return Ok(new { message = "Existing user successfully linked to Firebase.", userId = user.Id });
            }
            else
            {
                // Brand new user signing up via Firebase
                var newUser = new User
                {
                    Email = email,
                    Name = email.Split('@')[0], // Default name from email prefix
                    FirebaseUid = firebaseUid,
                    Role = "Employee" // Standard default role
                };
                
                _context.Users.Add(newUser);
                await _context.SaveChangesAsync();
                return Ok(new { message = "New user created via Firebase.", userId = newUser.Id });
            }
        }

        private string GenerateToken(int userId, string email, string role, string securityStamp)
        {
            var secret = _config["JwtSettings:SecretKey"]!;
            var issuer = _config["JwtSettings:Issuer"]!;
            var audience = _config["JwtSettings:Audience"]!;
            var expiryDays = int.Parse(_config["JwtSettings:ExpiryDays"]!);

            var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secret));
            var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

            var claims = new[]
            {
                new Claim(ClaimTypes.NameIdentifier, userId.ToString()),
                new Claim(ClaimTypes.Email, email),
                new Claim(ClaimTypes.Role, role),
                new Claim("SecurityStamp", securityStamp)
            };

            var token = new JwtSecurityToken(
                issuer: issuer,
                audience: audience,
                claims: claims,
                expires: DateTime.UtcNow.AddDays(expiryDays),
                signingCredentials: creds
            );

            return new JwtSecurityTokenHandler().WriteToken(token);
        }
    }}