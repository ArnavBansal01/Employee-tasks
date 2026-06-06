using System.ComponentModel.DataAnnotations;
namespace TaskTrackerAPI.DTOs
{
    // What the user sends to the API
    public class PaginationParams
    {
        private const int MaxPageSize = 50; // Prevent users from requesting 1,000,000 rows
        [Range(1, int.MaxValue, ErrorMessage = "PageNumber must be 1 or greater.")]
        public int PageNumber { get; set; } = 1;
        private int _pageSize = 10; // Default to 10 items per page
        public int PageSize
        {
            get => _pageSize;
            set => _pageSize = (value > MaxPageSize) ? MaxPageSize : value;
        }
    }

    // What the API sends back to the user
    public class PagedResult<T>
    {
        public List<T> Items { get; set; } = new List<T>();
        public int TotalCount { get; set; }
        public int PageNumber { get; set; }
        public int PageSize { get; set; }
        public int TotalPages => (int)Math.Ceiling(TotalCount / (double)PageSize);
    }
}