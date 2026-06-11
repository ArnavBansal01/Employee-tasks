import React, { useState, useEffect } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { ArrowLeft, Save, Search, ChevronDown } from 'lucide-react';
import api from '../api/axios';

// --- Custom Searchable Dropdown Component ---
interface SearchableSelectProps {
  options: { id: string | number; label: string }[];
  value: string | number;
  onChange: (value: string) => void;
  placeholder: string;
  disabled?: boolean;
}

const SearchableSelect: React.FC<SearchableSelectProps> = ({ options, value, onChange, placeholder, disabled }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');

  const selectedOption = options.find((o) => String(o.id) === String(value));
  const filteredOptions = options.filter((o) =>
    o.label.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="relative">
      {/* Main trigger button */}
      <div
        onClick={() => !disabled && setIsOpen(!isOpen)}
        className={`w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-sm text-slate-900 dark:text-white flex justify-between items-center transition-colors ${
          disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer hover:border-blue-400 focus:ring-2 focus:ring-blue-500'
        }`}
      >
        <span className="truncate">{selectedOption ? selectedOption.label : placeholder}</span>
        <ChevronDown size={16} className={`text-slate-500 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
      </div>

      {/* Dropdown Menu */}
      {isOpen && (
        <>
          {/* Invisible overlay to close dropdown when clicking outside */}
          <div className="fixed inset-0 z-10" onClick={() => setIsOpen(false)}></div>
          
          <div className="absolute z-20 w-full mt-1 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-lg shadow-xl max-h-60 flex flex-col overflow-hidden">
            <div className="p-2 border-b border-slate-100 dark:border-slate-800 shrink-0 relative">
              <Search size={14} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                autoFocus
                className="w-full pl-8 pr-2 py-1.5 text-sm bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-md focus:outline-none focus:border-blue-500 text-slate-900 dark:text-white"
                placeholder="Search..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <div className="overflow-y-auto p-1">
              {filteredOptions.length === 0 ? (
                <div className="px-3 py-4 text-sm text-slate-500 text-center">No matches found</div>
              ) : (
                filteredOptions.map((opt) => (
                  <div
                    key={opt.id}
                    onClick={() => {
                      onChange(String(opt.id));
                      setIsOpen(false);
                      setSearch('');
                    }}
                    className={`px-3 py-2 text-sm rounded-md cursor-pointer transition-colors ${
                      String(value) === String(opt.id)
                        ? 'bg-blue-50 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400 font-medium'
                        : 'text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                    }`}
                  >
                    {opt.label}
                  </div>
                ))
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
};
// ---------------------------------------------

export function CreateTaskPage() {
  const navigate = useNavigate();
  const location = useLocation();
  
  // Grab the preselected project ID if we came from the ProjectDetail page
  const preselectedProjectId = location.state?.preselectedProjectId || '';

  const [projects, setProjects] = useState<any[]>([]);
  const [projectMembers, setProjectMembers] = useState<any[]>([]);
  
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    status: 'Pending',
    priority: 'Medium',
    deadline: '',
    projectId: String(preselectedProjectId), // Ensure string for consistency
    userId: ''
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // 1. Fetch all projects on load
  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const res = await api.get('/projects');
        setProjects(res.data);
      } catch (err) {
        setError('Failed to load projects.');
      }
    };
    fetchProjects();
  }, []);

  // 2. Fetch project members whenever the selected project changes
  useEffect(() => {
    const fetchMembers = async () => {
      if (!formData.projectId) {
        setProjectMembers([]);
        return;
      }
      try {
        const res = await api.get(`/projectassignments/${formData.projectId}`);
        setProjectMembers(res.data);
        // Reset selected user if they change the project
        setFormData(prev => ({ ...prev, userId: '' }));
      } catch (err) {
        console.error("Failed to load project members");
      }
    };
    fetchMembers();
  }, [formData.projectId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Custom validation because our custom dropdowns bypass HTML5 'required' tag
    if (!formData.projectId || !formData.userId) {
      setError('Please select both a Project and an Assignee.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await api.post('/tasks', {
        ...formData,
        projectId: Number(formData.projectId),
        userId: Number(formData.userId)
      });
      navigate('/tasks');
    } catch (err) {
      setError('Failed to create task. Make sure all fields are valid.');
    } finally {
      setLoading(false);
    }
  };

  // Prepare options for the custom dropdowns
  const projectOptions = projects.map(p => ({ id: p.id, label: p.name }));
  const memberOptions = projectMembers.map(m => ({ id: m.userId, label: `${m.name} (${m.email})` }));

  return (
    <div className="max-w-2xl mx-auto space-y-6 p-6">
      <Link to="/tasks" className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200">
        <ArrowLeft size={16} /> Back to Tasks
      </Link>

      <div className="card p-6 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm">
        <h1 className="text-xl font-bold text-slate-900 dark:text-white mb-6">Create New Task</h1>
        
        {error && <div className="mb-6 p-3 text-sm font-medium text-red-600 bg-red-50 dark:bg-red-900/20 rounded-lg">{error}</div>}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Task Title</label>
            <input type="text" required value={formData.title} onChange={e => setFormData({...formData, title: e.target.value})} className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Description</label>
            <textarea required rows={3} value={formData.description} onChange={e => setFormData({...formData, description: e.target.value})} className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none" />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Project</label>
              <SearchableSelect 
                options={projectOptions}
                value={formData.projectId}
                onChange={(val) => setFormData({...formData, projectId: val})}
                placeholder="-- Search Project --"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Assign To</label>
              <SearchableSelect 
                options={memberOptions}
                value={formData.userId}
                onChange={(val) => setFormData({...formData, userId: val})}
                placeholder="-- Search Member --"
                disabled={!formData.projectId}
              />
              {!formData.projectId && <p className="text-xs text-slate-500 mt-1">Select a project first</p>}
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Status</label>
              <select value={formData.status} onChange={e => setFormData({...formData, status: e.target.value})} className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500">
                <option value="Pending">Pending</option>
                <option value="InProgress">In Progress</option>
                <option value="Completed">Completed</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Priority</label>
              <select value={formData.priority} onChange={e => setFormData({...formData, priority: e.target.value})} className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500">
                <option value="Low">Low</option>
                <option value="Medium">Medium</option>
                <option value="High">High</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Deadline</label>
              <input type="date" required value={formData.deadline} onChange={e => setFormData({...formData, deadline: e.target.value})} className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          </div>

          <div className="pt-4 border-t border-slate-200 dark:border-slate-800 flex justify-end">
            <button type="submit" disabled={loading} className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg disabled:opacity-50 inline-flex items-center gap-2 transition-colors">
              <Save size={16} /> {loading ? 'Creating...' : 'Create Task'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );

}