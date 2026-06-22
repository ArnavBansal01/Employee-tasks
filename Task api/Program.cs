using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Swashbuckle.AspNetCore.SwaggerGen;
using System.Text;
using TaskTrackerAPI.Data;
using Microsoft.AspNetCore.RateLimiting;
using System.Threading.RateLimiting;
using System.Security.Claims;
using Microsoft.AspNetCore.Authentication;
using TaskTrackerAPI.Services;
using FirebaseAdmin;


using Google.Apis.Auth.OAuth2;


var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers()

    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.ReferenceHandler = System.Text.Json.Serialization.ReferenceHandler.IgnoreCycles;
    });

builder.Services.AddProblemDetails(); 

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new() { Title = "TaskTracker API", Version = "v1" });
});

builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("DefaultConnection")));

var jwtSecret = builder.Configuration["JwtSettings:SecretKey"] 
    ?? throw new InvalidOperationException("JWT SecretKey is not configured.");

var firebaseProjectId = builder.Configuration["Firebase:ProjectId"]
    ?? throw new InvalidOperationException("Firebase ProjectId is not configured.");

// NOTE: "Local" is the existing scheme (your own signed tokens).
// "Firebase" is the new scheme (tokens signed by Firebase/Google).
// Default stays "Local" so anything that doesn't specify a scheme
// behaves exactly as it did before this change.
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(JwtBearerDefaults.AuthenticationScheme, options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer = builder.Configuration["JwtSettings:Issuer"],
            ValidAudience = builder.Configuration["JwtSettings:Audience"],
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtSecret))
        };
    })
    .AddJwtBearer("Firebase", options =>
    {
        options.Authority = $"https://securetoken.google.com/{firebaseProjectId}";
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = $"https://securetoken.google.com/{firebaseProjectId}",
            ValidateAudience = true,
            ValidAudience = firebaseProjectId,
            ValidateLifetime = true
        };
        /*
        // NEW: check Firebase's revocation list on every request.
        // Without this, RevokeRefreshTokensAsync() only blocks the user
        // once their 1-hour ID token naturally expires — not immediately.
        options.Events = new JwtBearerEvents
        {
            OnTokenValidated = async context =>
            {
                var rawToken = context.HttpContext.Request.Headers["Authorization"]
                    .FirstOrDefault()?.Split(" ").Last();

                if (string.IsNullOrEmpty(rawToken))
                {
                    context.Fail("Missing token.");
                    return;
                }

                try
                {
                    // checkRevoked: true forces a check against Firebase's
                    // revocation list, not just signature/expiry validation
                    await FirebaseAdmin.Auth.FirebaseAuth.DefaultInstance
                        .VerifyIdTokenAsync(rawToken, checkRevoked: true);
                }
                catch (FirebaseAdmin.Auth.FirebaseAuthException)
                {
                    context.Fail("Token has been revoked.");
                }
            }
        };
        */
});
   

// Lets a single [Authorize] attribute accept EITHER scheme.
// Existing local JWTs keep working; new Firebase ID tokens also work.
builder.Services.AddAuthorization(options =>
{
    var multiSchemePolicy = new Microsoft.AspNetCore.Authorization.AuthorizationPolicyBuilder(
            JwtBearerDefaults.AuthenticationScheme, "Firebase")
        .RequireAuthenticatedUser()
        .Build();

    options.DefaultPolicy = multiSchemePolicy;
});

// Resolves Firebase-authenticated requests to your local integer UserId + Role,
// so existing controllers reading ClaimTypes.NameIdentifier / ClaimTypes.Role
// don't need to change at all. Has no effect on local-JWT-authenticated requests.
builder.Services.AddTransient<IClaimsTransformation, FirebaseClaimsTransformation>();

builder.Services.AddRateLimiter(options =>
{
    options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;

    // 1. Global Limiter (100 per minute)
    options.GlobalLimiter = PartitionedRateLimiter.Create<HttpContext, string>(httpContext =>
        RateLimitPartition.GetFixedWindowLimiter(
            partitionKey: httpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown",
            factory: partition => new FixedWindowRateLimiterOptions
            {
                AutoReplenishment = true,
                PermitLimit = 100, 
                QueueLimit = 0,    
                Window = TimeSpan.FromMinutes(1) 
            }));

    // 2. Login specific limiter (5 per minute)
    options.AddFixedWindowLimiter("login", limiterOptions =>
    {
        limiterOptions.PermitLimit = 5; 
        limiterOptions.Window = TimeSpan.FromMinutes(1); 
        limiterOptions.QueueLimit = 0; 
    });
});
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowReact", policy =>
    {
        policy.WithOrigins("http://localhost:5173")
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});
// Initialize Firebase Admin SDK to allow the Admin to forge accounts
// 1. Try to get the JSON string from the server's Environment Variables
var firebaseJsonConfig = Environment.GetEnvironmentVariable("FIREBASE_CONFIG");

if (string.IsNullOrEmpty(firebaseJsonConfig))
{
    // 2. If it's not on the server, fallback to the local file for your testing on localhost
    FirebaseApp.Create(new AppOptions()
    {
        Credential = GoogleCredential.FromFile("firebase-adminsdk.json")
    });
}
else
{
    // 3. If the server has the variable, use it directly!
    FirebaseApp.Create(new AppOptions()
    {
        Credential = GoogleCredential.FromJson(firebaseJsonConfig)
    });
}
var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler(); 
}
else 
{
    app.UseSwagger();   
    app.UseSwaggerUI();
}

app.UseCors("AllowReact");


app.UseStatusCodePages(); // Turns 401s and 404s into clean JSON

app.UseHttpsRedirection();

app.UseRateLimiter(); 
app.UseAuthentication();
app.Use(async (context, next) =>
{
    // Authenticate the Firebase scheme if the default (Local Bearer) scheme wasn't successful
    if (context.User.Identity?.IsAuthenticated != true)
    {
        var result = await context.AuthenticateAsync("Firebase");
        if (result.Succeeded && result.Principal != null)
        {
            context.User = result.Principal;
        }
    }

    Console.WriteLine($"[AuthMiddleware] Request Path: {context.Request.Path}, IsAuthenticated: {context.User.Identity?.IsAuthenticated}");
    if (context.User.Identity?.IsAuthenticated == true)
    {
        foreach (var claim in context.User.Claims)
        {
            Console.WriteLine($"[AuthMiddleware]   Claim: {claim.Type} = {claim.Value}");
        }
        var userIdClaim = context.User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        var stampClaim = context.User.FindFirst("SecurityStamp")?.Value;

        if (userIdClaim != null)
        {
            var dbContext = context.RequestServices.GetRequiredService<AppDbContext>();
            var dbUser = await dbContext.Users.FindAsync(int.Parse(userIdClaim));

            if (dbUser == null)
            {
                context.Response.StatusCode = 401;
                await context.Response.WriteAsync("Token invalidated.");
                return;
            }

            // 1. Check local security stamp (only if it was present in the token)
            if (stampClaim != null && dbUser.SecurityStamp != stampClaim)
            {
                context.Response.StatusCode = 401;
                await context.Response.WriteAsync("Token invalidated.");
                return;
            }

            // 2. Check if client-cached role matches database role
            if (context.Request.Headers.TryGetValue("X-User-Role", out var clientRole))
            {
                Console.WriteLine($"[AuthMiddleware] DB Role: '{dbUser.Role}', Client Role Header: '{clientRole}'");
                if (dbUser.Role != clientRole.ToString())
                {
                    Console.WriteLine("[AuthMiddleware] Role mismatch! Returning 401 Unauthorized.");
                    context.Response.StatusCode = 401;
                    await context.Response.WriteAsync("Role changed.");
                    return;
                }
            }
            else
            {
                Console.WriteLine($"[AuthMiddleware] User {dbUser.Email} (ID {dbUser.Id}) sent no X-User-Role header.");
            }
        }
    }
    await next();
});
app.UseAuthorization();




app.MapControllers();
app.Run();

