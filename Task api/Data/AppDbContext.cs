using Microsoft.EntityFrameworkCore;
using TaskTrackerAPI.Models;

namespace TaskTrackerAPI.Data
{
    public class AppDbContext : DbContext
    {
        public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

        public DbSet<User> Users { get; set; }
        public DbSet<Project> Projects { get; set; }
        public DbSet<TaskItem> Tasks { get; set; }
        public DbSet<UserProject> UserProjects { get; set; }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            // Composite primary key for UserProjects
            modelBuilder.Entity<UserProject>()
                .HasKey(up => new { up.UserId, up.ProjectId });
        }
    }
}