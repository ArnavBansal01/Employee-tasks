import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, ListTodo } from 'lucide-react';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

interface Project {
  id: number;
  name: string;
  description: string;
  deadline: string;
  createdAt: string;
  totalTasks: number;
  // We will append this locally after fetching tasks
  progressPercentage?: number; 
}

export function ProjectsPage() {
  const { isAdmin } = useAuth();

  // Core Data Fetching State
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Admin Create Project Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    deadline: '',
  });
  const [submitLoading, setSubmitLoading] = useState(false);
  const [submitError, setSubmitError] = useState('');

  const fetchProjectsAndProgress = async () => {
    try {
      setLoading(true);
      setError('');
      
      // 1. Fetch all projects
      const res = await api.get('/projects');
      const baseProjects: Project[] = res.data;

      // 2. Fetch tasks to calculate progress (Note: fetching all to calculate)
      // If you have hundreds of tasks, you should ideally add "CompletedTasks" to the backend ProjectDto
      const tasksRes = await api.get('/tasks?PageSize=1000'); 
      const allTasks = tasksRes.data.items || [];

      // 3. Map over projects and calculate completion
      const projectsWithProgress = baseProjects.map(p => {
        const projectTasks = allTasks.filter((t: any) => t.projectId === p.id);
        const completedTasks = projectTasks.filter((t: any) => t.status === 'Completed').length;
        
        let progress = 0;
        if (projectTasks.length > 0) {
          progress = Math.round((completedTasks / projectTasks.length) * 100);
        }

        return {
          ...p,
          progressPercentage: progress
        };
      });

      setProjects(projectsWithProgress);
    } catch (err) {
      setError('Failed to load projects.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjectsAndProgress();
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleCreateProject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name || !formData.description || !formData.deadline) {
      setSubmitError('All fields are required.');
      return;
    }

    try {
      setSubmitLoading(true);
      setSubmitError('');
      await api.post('/projects', formData);
      setIsModalOpen(false);
      setFormData({ name: '', description: '', deadline: '' });
      fetchProjectsAndProgress();
    } catch (err) {
      setSubmitError('Failed to create project. Please verify inputs.');
    } finally {
      setSubmitLoading(false);
    }
  };

  if (loading) {
    return <div className="flex justify-center py-20 text-slate-500 font-medium">Loading projects...</div>;
  }

  if (error) {
    return <div className="text-red-500 p-6 font-medium">{error}</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Projects</h1>
        {isAdmin && (
          <button
            onClick={() => setIsModalOpen(true)}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
          >
            New Project
          </button>
        )}
      </div>

      {projects.length === 0 ? (
        <div className="flex justify-center py-12 text-slate-500 dark:text-slate-400">
          No projects available.
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((p) => {
            const progress = p.progressPercentage || 0; 
            const formattedDeadline = new Date(p.deadline).toLocaleDateString();

            return (
              <Link key={p.id} to={`/projects/${p.id}`} className="card group p-5 transition-shadow hover:shadow-md">
                <h3 className="text-base font-semibold text-slate-900 group-hover:text-blue-500 dark:text-white dark:group-hover:text-blue-400">
                  {p.name}
                </h3>
                <p className="mt-2 line-clamp-2 text-sm leading-relaxed text-slate-500 dark:text-slate-400">
                  {p.description}
                </p>
                <div className="mt-4 flex items-center justify-between">
                  <div className="flex items-center gap-1.5 text-sm text-slate-500 dark:text-slate-400">
                    <Calendar size={14} />
                    {formattedDeadline}
                  </div>
                  <span className="badge bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400">
                    <ListTodo size={12} className="mr-1" />
                    {p.totalTasks} task{p.totalTasks !== 1 ? 's' : ''}
                  </span>
                </div>
                <div className="mt-3">
                  <div className="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
                    <span>Progress</span>
                    <span>{progress}%</span>
                  </div>
                  <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-700">
                    <div
                      className="h-full rounded-full bg-blue-500 transition-all duration-300"
                      style={{ width: `${progress}%` }}
                    />
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}

      {/* Admin Action Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md transform overflow-hidden rounded-xl bg-white shadow-xl transition-all dark:bg-slate-900 dark:border dark:border-slate-800">
            <div className="flex items-center justify-between border-b border-slate-100 p-6 dark:border-slate-800">
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">Create New Project</h2>
              <button
                onClick={() => {
                  setIsModalOpen(false);
                  setSubmitError('');
                }}
                className="cursor-pointer text-xl font-medium text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleCreateProject} className="space-y-4 p-6">
              {submitError && (
                <div className="rounded-lg bg-red-50 p-3 text-xs font-medium text-red-600 dark:bg-red-950/30 dark:text-red-400">
                  {submitError}
                </div>
              )}

              <div>
                <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Project Name
                </label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder="e.g., Global Relocation App"
                  className="w-full rounded-lg border border-slate-200 bg-slate-50 px-3.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-slate-800 dark:bg-slate-950 dark:text-white"
                  required
                />
              </div>

              <div>
                <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Description
                </label>
                <textarea
                  name="description"
                  value={formData.description}
                  onChange={handleInputChange}
                  placeholder="Detail scope requirements, targets, and goals..."
                  rows={4}
                  className="w-full resize-none rounded-lg border border-slate-200 bg-slate-50 px-3.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-slate-800 dark:bg-slate-950 dark:text-white"
                  required
                />
              </div>

              <div>
                <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Target Deadline
                </label>
                <input
                  type="date"
                  name="deadline"
                  value={formData.deadline}
                  onChange={handleInputChange}
                  className="w-full rounded-lg border border-slate-200 bg-slate-50 px-3.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-slate-800 dark:bg-slate-950 dark:text-white"
                  required
                />
              </div>

              <div className="mt-6 flex justify-end gap-3 border-t border-slate-100 pt-4 dark:border-slate-800">
                <button
                  type="button"
                  onClick={() => {
                    setIsModalOpen(false);
                    setSubmitError('');
                  }}
                  className="cursor-pointer rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 dark:border-slate-800 dark:text-slate-300 dark:hover:bg-slate-950"
                  disabled={submitLoading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="cursor-pointer rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition-colors hover:bg-blue-700 active:bg-blue-800 disabled:opacity-50"
                  disabled={submitLoading}
                >
                  {submitLoading ? 'Creating...' : 'Create Project'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
} 