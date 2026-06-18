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
FirebaseApp.Create(new AppOptions()
{
    // Pointing to the JSON file we placed in the folder earlier!
    Credential = GoogleCredential.FromFile("firebase-adminsdk.json")
});
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
    if (context.User.Identity?.IsAuthenticated == true)
    {
        var userIdClaim = context.User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        var stampClaim = context.User.FindFirst("SecurityStamp")?.Value;

        // IMPORTANT: this check now only applies to LOCAL-JWT requests.
        // Firebase-authenticated requests won't carry a "SecurityStamp" claim
        // at all (Firebase tokens don't know about it), so stampClaim will be
        // null for them and this block is skipped entirely - it does not
        // block Firebase logins, and local JWT behavior is unchanged.
        if (userIdClaim != null && stampClaim != null)
        {
            var dbContext = context.RequestServices.GetRequiredService<AppDbContext>();
            var dbUser = await dbContext.Users.FindAsync(int.Parse(userIdClaim));

            if (dbUser == null || dbUser.SecurityStamp != stampClaim)
            {
                context.Response.StatusCode = 401;
                await context.Response.WriteAsync("Token invalidated.");
                return;
            }
        }
    }
    await next();
});
app.UseAuthorization();




app.MapControllers();
app.Run();

