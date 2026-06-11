import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, Calendar, User, FolderKanban, Flag, Edit, Trash2, X } from 'lucide-react';
import { StatusBadge, PriorityBadge } from '../components/Badge';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';

interface TaskDetail {
  id: number;
  title: string;
  description: string;
  status: string;
  priority: string;
  deadline: string;
  assignedTo: string;
  userId: number;
  projectId: number;
  projectName: string;
}

export function TaskDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAdmin } = useAuth();
  
  const [task, setTask] = useState<TaskDetail | null>(null);
  
  // Quick-edit status (for assigned employees)
  const [status, setStatus] = useState('');
  
  // Status states
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState('');

  // Admin Edit Modal States
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editFormData, setEditFormData] = useState({
    title: '',
    description: '',
    priority: '',
    deadline: '',
  });

  const fetchTask = async () => {
    try {
      setLoading(true);
      const res = await api.get(`/tasks/${id}`);
      setTask(res.data);
      setStatus(res.data.status);
    } catch (err) {
      setError('Task not found or access denied.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTask();
  }, [id]);

  // Employee/Quick save for Status
  const handleSaveStatus = async () => {
    if (!task) return;
    setSaving(true);
    try {
      await api.put(`/tasks/${id}`, {
        ...task,
        status: status, 
      });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
      setTask({ ...task, status: status });
    } catch (err) {
      setError('Failed to update status.');
    } finally {
      setSaving(false);
    }
  };

  // Admin Full Edit
  const openEditModal = () => {
    if (task) {
      setEditFormData({
        title: task.title,
        description: task.description,
        priority: task.priority,
        deadline: new Date(task.deadline).toISOString().split('T')[0]
      });
      setIsEditModalOpen(true);
    }
  };

  const handleAdminEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!task) return;
    setSaving(true);
    try {
      await api.put(`/tasks/${id}`, {
        ...task,
        title: editFormData.title,
        description: editFormData.description,
        priority: editFormData.priority,
        deadline: editFormData.deadline,
        status: status // Keep the current status
      });
      setIsEditModalOpen(false);
      fetchTask(); // Refresh to get accurate data
    } catch (err) {
      alert('Failed to update task details.');
    } finally {
      setSaving(false);
    }
  };

  // Admin Delete
  const handleDeleteTask = async () => {
    if (!window.confirm("Permanently delete this task?")) return;
    try {
      await api.delete(`/tasks/${id}`);
      navigate('/tasks');
    } catch (err) {
      alert('Failed to delete task.');
    }
  };

  if (loading) return <div className="flex justify-center py-20 text-slate-500">Loading task...</div>;
  if (error || !task) return (
    <div className="flex flex-col items-center justify-center py-20">
      <p className="text-lg text-slate-500 dark:text-slate-400">{error}</p>
      <Link to="/tasks" className="mt-4 text-sm font-medium text-blue-500 hover:text-blue-600">
        Back to Tasks
      </Link>
    </div>
  );

  return (
    <div className="space-y-6">
      <Link to="/tasks" className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200">
        <ArrowLeft size={16} /> Back to Tasks
      </Link>

      <div className="card bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm">
        {/* Header */}
        <div className="border-b border-slate-200 px-6 py-5 dark:border-slate-800">
          <div className="flex items-start justify-between">
            <div>
              <h1 className="text-xl font-bold text-slate-900 dark:text-white">{task.title}</h1>
              <p className="mt-2 text-sm leading-relaxed text-slate-500 dark:text-slate-400">
                {task.description}
              </p>
            </div>
            <div className="flex flex-col items-end gap-3">
              <PriorityBadge priority={task.priority as any} />
              
              {/* Admin Actions */}
              {isAdmin && (
                <div className="flex gap-2">
                  <button onClick={openEditModal} className="p-1.5 text-slate-400 hover:bg-slate-100 hover:text-blue-600 dark:hover:bg-slate-800 dark:hover:text-blue-400 rounded-md transition-colors" title="Edit Task details">
                    <Edit size={16} />
                  </button>
                  <button onClick={handleDeleteTask} className="p-1.5 text-slate-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/20 dark:hover:text-red-400 rounded-md transition-colors" title="Delete Task">
                    <Trash2 size={16} />
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Task Info Grid */}
        <div className="grid gap-6 p-6 sm:grid-cols-2">
          <div className="space-y-5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 dark:bg-slate-800">
                <User size={16} className="text-slate-500 dark:text-slate-400" />
              </div>
              <div>
                <p className="text-xs text-slate-500 dark:text-slate-400">Assigned To</p>
                <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{task.assignedTo}</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 dark:bg-slate-800">
                <FolderKanban size={16} className="text-slate-500 dark:text-slate-400" />
              </div>
              <div>
                <p className="text-xs text-slate-500 dark:text-slate-400">Project</p>
                <Link to={`/projects/${task.projectId}`} className="text-sm font-medium text-blue-500 hover:text-blue-600">
                  {task.projectName}
                </Link>
              </div>
            </div>
          </div>
          
          <div className="space-y-5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 dark:bg-slate-800">
                <Calendar size={16} className="text-slate-500 dark:text-slate-400" />
              </div>
              <div>
                <p className="text-xs text-slate-500 dark:text-slate-400">Deadline</p>
                <p className="text-sm font-medium text-slate-900 dark:text-slate-100">
                  {task.deadline ? new Date(task.deadline).toLocaleDateString() : '—'}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 dark:bg-slate-800">
                <Flag size={16} className="text-slate-500 dark:text-slate-400" />
              </div>
              <div>
                <p className="mb-1 text-xs text-slate-500 dark:text-slate-400">Status Update</p>
                <select
                  value={status}
                  onChange={e => setStatus(e.target.value)}
                  className="px-3 py-1.5 text-sm bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="Pending">Pending</option>
                  <option value="InProgress">In Progress</option>
                  <option value="Completed">Completed</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        {/* Action Footer */}
        <div className="flex items-center gap-3 border-t border-slate-200 dark:border-slate-800 px-6 py-4 bg-slate-50/50 dark:bg-slate-900/50">
          <button
            onClick={handleSaveStatus}
            disabled={saving || status === task.status}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg disabled:opacity-50 inline-flex items-center gap-2 transition-colors"
          >
            <Save size={16} /> {saving ? 'Saving...' : 'Update Status'}
          </button>
          {saved && (
            <span className="text-sm font-medium text-emerald-500">Updated successfully!</span>
          )}
          <div className="ml-auto">
            <StatusBadge status={task.status as any} />
          </div>
        </div>
      </div>

      {/* --- Admin Edit Task Details Modal --- */}
      {isEditModalOpen && isAdmin && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 w-full max-w-md rounded-xl shadow-xl overflow-hidden">
            <div className="p-5 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center">
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">Edit Task Details</h2>
              <button onClick={() => setIsEditModalOpen(false)} className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200">
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleAdminEdit} className="p-5 space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Task Title</label>
                <input type="text" value={editFormData.title} onChange={e => setEditFormData({...editFormData, title: e.target.value})} required className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Description</label>
                <textarea rows={3} value={editFormData.description} onChange={e => setEditFormData({...editFormData, description: e.target.value})} required className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none" />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Priority</label>
                  <select value={editFormData.priority} onChange={e => setEditFormData({...editFormData, priority: e.target.value})} className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="Low">Low</option>
                    <option value="Medium">Medium</option>
                    <option value="High">High</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Deadline</label>
                  <input type="date" value={editFormData.deadline} onChange={e => setEditFormData({...editFormData, deadline: e.target.value})} required className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500" />
                </div>
              </div>
              <div className="pt-2 flex justify-end gap-3">
                <button type="button" onClick={() => setIsEditModalOpen(false)} className="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg">Cancel</button>
                <button type="submit" disabled={saving} className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg disabled:opacity-50">
                  {saving ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}