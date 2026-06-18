using Microsoft.AspNetCore.Authentication;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;
using TaskTrackerAPI.Data;

namespace TaskTrackerAPI.Services
{
    public class FirebaseClaimsTransformation : IClaimsTransformation
    {
        private readonly IServiceProvider _serviceProvider;

        public FirebaseClaimsTransformation(IServiceProvider serviceProvider)
        {
            _serviceProvider = serviceProvider;
        }

        public async Task<ClaimsPrincipal> TransformAsync(ClaimsPrincipal principal)
        {
            if (principal.Identity == null || !principal.Identity.IsAuthenticated)
                return principal;

            var identity = (ClaimsIdentity)principal.Identity;

            // Already transformed — idempotency guard
            if (identity.HasClaim(c => c.Type == "TransformedByFirebaseClaims"))
                return principal;

            // LOCAL JWTs always carry SecurityStamp (added in GenerateToken).
            // Firebase tokens never have it. This is the most reliable way
            // to skip local tokens without checking AuthenticationType,
            // which is set by the JWT handler internally, not by scheme name.
            if (identity.HasClaim(c => c.Type == "SecurityStamp"))
                return principal;

            var firebaseUid = principal.FindFirst(ClaimTypes.NameIdentifier)?.Value;

            if (string.IsNullOrEmpty(firebaseUid))
                return principal;

            using var scope = _serviceProvider.CreateScope();
            var dbContext = scope.ServiceProvider.GetRequiredService<AppDbContext>();

            // Handles both numeric UIDs ("4") and real Firebase UIDs
            var dbUser = await dbContext.Users
                .FirstOrDefaultAsync(u => u.FirebaseUid == firebaseUid);

            if (dbUser == null)
                return principal;

            // Swap Firebase UID → local integer ID
            var existingIdClaim = identity.FindFirst(ClaimTypes.NameIdentifier);
            if (existingIdClaim != null) identity.RemoveClaim(existingIdClaim);
            identity.AddClaim(new Claim(ClaimTypes.NameIdentifier, dbUser.Id.ToString()));

            // Inject role so [Authorize(Roles = "Admin")] works
            var existingRoleClaim = identity.FindFirst(ClaimTypes.Role);
            if (existingRoleClaim != null) identity.RemoveClaim(existingRoleClaim);
            identity.AddClaim(new Claim(ClaimTypes.Role, dbUser.Role));

            // Ensure email is present
            if (!identity.HasClaim(c => c.Type == ClaimTypes.Email))
                identity.AddClaim(new Claim(ClaimTypes.Email, dbUser.Email));

            // Add SecurityStamp so the stamp-validation middleware in Program.cs
            // works correctly for Firebase-authenticated users too
            identity.AddClaim(new Claim("SecurityStamp", dbUser.SecurityStamp));

            identity.AddClaim(new Claim("TransformedByFirebaseClaims", "true"));

            return principal;
        }
    }
}