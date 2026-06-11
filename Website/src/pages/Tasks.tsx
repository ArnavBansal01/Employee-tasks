import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Eye, Plus, Trash2, ChevronDown, Search } from 'lucide-react';
import { StatusBadge, PriorityBadge } from '../components/Badge';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';

interface TaskItem {
  id: number;
  title: string;
  assignedTo: string;
  projectName: string;
  projectId: number;
  status: string;
  priority: string;
  deadline: string;
}
const SearchableProjectSelect = ({ options, value, onChange, placeholder }: any) => {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  
  // Find label, fallback to placeholder if no selection
  const selectedLabel = options.find((o: any) => o.name === value)?.name || placeholder;
  const filtered = options.filter((o: any) => o.name.toLowerCase().includes(search.toLowerCase()));

  return (
    <div className="relative w-48">
      {/* Main trigger button */}
      <div 
        onClick={() => setIsOpen(!isOpen)}
        className="w-full pl-3 pr-8 py-2 text-sm bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white cursor-pointer flex justify-between items-center truncate"
      >
        <span className="truncate">{selectedLabel}</span>
        {/* The Arrow Icon */}
        <ChevronDown 
          size={16} 
          className={`absolute right-2 text-slate-500 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} 
        />
      </div>

      {isOpen && (
        <div className="absolute z-50 w-full mt-1 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-lg shadow-xl max-h-60 overflow-hidden flex flex-col">
          <div className="relative p-2 border-b border-slate-200 dark:border-slate-800">
            <Search size={14} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
            <input 
              autoFocus 
              className="w-full pl-6 pr-2 py-1.5 text-sm bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-md outline-none text-slate-900 dark:text-white" 
              placeholder="Search..." 
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="overflow-y-auto">
            <div 
              onClick={() => { onChange(''); setIsOpen(false); }} 
              className="px-3 py-2 text-sm hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer text-slate-700 dark:text-slate-300"
            >
              All Projects
            </div>
            {filtered.map((p: any) => (
              <div 
                key={p.id} 
                onClick={() => { onChange(p.name); setIsOpen(false); }} 
                className="px-3 py-2 text-sm hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer text-slate-700 dark:text-slate-300"
              >
                {p.name}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
export function TasksPage() {
  const { isAdmin } = useAuth();
  
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [projects, setProjects] = useState<any[]>([]); // For the filter dropdown
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Filters
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [projectFilter, setProjectFilter] = useState('');
  
  // Pagination
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const PAGE_SIZE = 8;

  // Initial Data Load
  useEffect(() => {
    const fetchDropdownData = async () => {
      try {
        const res = await api.get('/projects');
        setProjects(res.data);
      } catch (err) {}
    };
    fetchDropdownData();
  }, []);

  useEffect(() => {
    fetchTasks();
  }, [page, statusFilter, priorityFilter, projectFilter]);

  const fetchTasks = async () => {
    setLoading(true);
    try {
      // If your backend doesn't support query filters natively yet, we filter on the frontend.
      // (Fetching a large page size locally to ensure filters apply across the dataset)
      const res = await api.get(`/tasks?PageNumber=${page}&PageSize=${PAGE_SIZE}`);
      let items = res.data.items;

      // Apply Frontend filtering
      if (statusFilter) items = items.filter((t: TaskItem) => t.status === statusFilter);
      if (priorityFilter) items = items.filter((t: TaskItem) => t.priority === priorityFilter);
      if (projectFilter) items = items.filter((t: TaskItem) => t.projectName === projectFilter);

      setTasks(items);
      setTotalPages(res.data.totalPages);
      setTotalCount(res.data.totalCount);
    } catch (err) {
      setError('Failed to load tasks.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Are you sure you want to delete this task?")) return;
    try {
      await api.delete(`/tasks/${id}`);
      fetchTasks(); // Refresh the list
    } catch (err) {
      alert("Failed to delete task.");
    }
  };

  if (error) return (
    <div className="flex items-center justify-center h-64">
      <p className="text-red-500">{error}</p>
    </div>
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Tasks</h1>
        <div className="flex items-center gap-3">
          <span className="text-sm text-slate-500 dark:text-slate-400">
            {totalCount} task{totalCount !== 1 ? 's' : ''}
          </span>
          {isAdmin && (
            <Link
              to="/tasks/create"
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg inline-flex items-center gap-2 transition-colors"
            >
              <Plus size={16} /> New Task
            </Link>
          )}
        </div>
      </div>

      <div className="card bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm">
        {/* Filters */}
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 px-6 py-4 dark:border-slate-700/50">
          <SearchableProjectSelect 
  options={projects} 
  value={projectFilter} 
  onChange={(val: string) => { setProjectFilter(val); setPage(1); }} 
  placeholder="Filter by Project"
/>
          <select
            value={statusFilter}
            onChange={e => { setStatusFilter(e.target.value); setPage(1); }}
            className="px-3 py-2 text-sm bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
          >
            <option value="">All Statuses</option>
            <option value="Pending">Pending</option>
            <option value="InProgress">In Progress</option>
            <option value="Completed">Completed</option>
          </select>
          <select
            value={priorityFilter}
            onChange={e => { setPriorityFilter(e.target.value); setPage(1); }}
            className="px-3 py-2 text-sm bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
          >
            <option value="">All Priorities</option>
            <option value="Low">Low</option>
            <option value="Medium">Medium</option>
            <option value="High">High</option>
          </select>
          {(statusFilter || priorityFilter || projectFilter) && (
            <button
              onClick={() => { setStatusFilter(''); setPriorityFilter(''); setProjectFilter(''); setPage(1); }}
              className="text-sm font-medium text-blue-500 hover:text-blue-600"
            >
              Clear filters
            </button>
          )}
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
                <tr className="border-b border-slate-100 dark:border-slate-700/50 bg-slate-50/50 dark:bg-slate-800/20">
                  <th className="px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Title</th>
                  <th className="hidden sm:table-cell px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Assigned To</th>
                  <th className="hidden md:table-cell px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Project</th>
                  <th className="px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Status</th>
                  <th className="hidden sm:table-cell px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Priority</th>
                  <th className="hidden lg:table-cell px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Deadline</th>
                  <th className="px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Actions</th>
                </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-700/50">
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-sm text-slate-500">Loading...</td>
                </tr>
              ) : tasks.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-sm text-slate-500 dark:text-slate-400">
                    No tasks found.
                  </td>
                </tr>
              ) : (
                tasks.map(t => (
                  <tr key={t.id} className="hover:bg-slate-50 dark:hover:bg-slate-700/30 transition-colors">
                    <td className="px-4 sm:px-6 py-3 sm:py-3.5 text-sm font-medium text-slate-900 dark:text-slate-100 max-w-[140px] sm:max-w-none truncate">
                      {t.title}
                    </td>
                    <td className="hidden sm:table-cell px-4 sm:px-6 py-3 sm:py-3.5">
                      <div className="flex items-center gap-2">
                        <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-blue-100 text-xs font-semibold text-blue-700 dark:bg-blue-900/30 dark:text-blue-400">
                          {t.assignedTo?.split(' ').map((n: string) => n[0]).join('').toUpperCase().slice(0, 2)}
                        </div>
                        <span className="text-sm text-slate-600 dark:text-slate-300 truncate">{t.assignedTo}</span>
                      </div>
                    </td>
                    <td className="hidden md:table-cell px-4 sm:px-6 py-3 sm:py-3.5 text-sm text-slate-600 dark:text-slate-300">
                      {t.projectName}
                    </td>
                    <td className="px-4 sm:px-6 py-3 sm:py-3.5">
                      <StatusBadge status={t.status as any} />
                    </td>
                    <td className="hidden sm:table-cell px-4 sm:px-6 py-3 sm:py-3.5">
                      <PriorityBadge priority={t.priority as any} />
                    </td>
                    <td className="hidden lg:table-cell px-4 sm:px-6 py-3 sm:py-3.5 text-sm text-slate-600 dark:text-slate-300">
                      {t.deadline ? new Date(t.deadline).toLocaleDateString() : '—'}
                    </td>
                    <td className="px-4 sm:px-6 py-3 sm:py-3.5">
                      <div className="flex items-center gap-1">
                        <Link
                          to={`/tasks/${t.id}`}
                          className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-blue-600 dark:hover:bg-slate-800 dark:hover:text-blue-400 transition-colors"
                        >
                          <Eye size={16} />
                        </Link>
                        {isAdmin && (
                          <button
                            onClick={() => handleDelete(t.id)}
                            className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/20 dark:hover:text-red-400 transition-colors"
                          >
                            <Trash2 size={16} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-200 px-6 py-3 dark:border-slate-700/50">
            <span className="text-sm text-slate-500 dark:text-slate-400">
              Page {page} of {totalPages}
            </span>
            <div className="flex gap-1">
              <button
                onClick={() => setPage(p => Math.max(1, p - 1))}
                disabled={page === 1}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 disabled:opacity-40 dark:hover:bg-slate-700 transition-colors"
              >
                <ChevronLeft size={16} />
              </button>
              <button
                onClick={() => setPage(p => Math.min(totalPages, p + 1))}
                disabled={page === totalPages}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 disabled:opacity-40 dark:hover:bg-slate-700 transition-colors"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>  
  );
}