import React, { useState, useEffect } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  Calendar,
  Users,
  ListTodo,
  Plus,
  X,
  Edit,
  Trash2,
  UserMinus,
  Search,
} from "lucide-react";
import api from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { StatusBadge, PriorityBadge } from "../components/Badge";

// --- Interfaces matching your backend DTOs ---
interface Project {
  id: number;
  name: string;
  description: string;
  deadline: string;
}

interface Member {
  userId: number;
  name: string;
  email: string;
}

interface Task {
  id: number;
  title: string;
  status: string;
  priority: string;
  deadline: string;
  assignedTo: string;
  projectId: number;
}

interface User {
  id: number;
  name: string;
  email: string;
}

export function ProjectDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAdmin } = useAuth();

  // --- Core State ---
  const [project, setProject] = useState<Project | null>(null);
  const [members, setMembers] = useState<Member[]>([]);
  const [projectTasks, setProjectTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // --- Modal States ---
  // 1. Add Member
  const [isAddMemberOpen, setIsAddMemberOpen] = useState(false);
  const [availableUsers, setAvailableUsers] = useState<User[]>([]);
  const [selectedUserIds, setSelectedUserIds] = useState<number[]>([]);
  const [searchQuery, setSearchQuery] = useState(""); // New search state

  // 2. Edit Project
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editFormData, setEditFormData] = useState({
    name: "",
    description: "",
    deadline: "",
  });

  // 3. Delete Project
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

  // --- Form Status States ---
  const [submitLoading, setSubmitLoading] = useState(false);
  const [submitError, setSubmitError] = useState("");

  // --- Fetch Data ---
  useEffect(() => {
    const fetchProjectData = async () => {
      try {
        setLoading(true);
        setError("");

        const [projRes, memRes, tasksRes] = await Promise.all([
          api.get(`/projects/${id}`),
          api.get(`/projectassignments/${id}`),
          api.get(`/tasks?PageSize=100`),
        ]);

        setProject(projRes.data);
        setMembers(memRes.data);

        const filteredTasks = tasksRes.data.items.filter(
          (t: Task) => t.projectId === Number(id),
        );
        setProjectTasks(filteredTasks);

        if (isAdmin) {
          const usersRes = await api.get("/users?PageSize=50");
          setAvailableUsers(usersRes.data.items);
        }
      } catch (err) {
        setError(
          "Failed to load project details. It may not exist or you lack access.",
        );
      } finally {
        setLoading(false);
      }
    };

    if (id) fetchProjectData();
  }, [id, isAdmin]);

  // --- Handlers: Project ---
  const openEditModal = () => {
    if (project) {
      setEditFormData({
        name: project.name,
        description: project.description,
        deadline: new Date(project.deadline).toISOString().split("T")[0],
      });
      setIsEditModalOpen(true);
      setSubmitError("");
    }
  };

  const handleEditProject = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSubmitLoading(true);
      setSubmitError("");
      const res = await api.put(`/projects/${id}`, editFormData);
      setProject(res.data);
      setIsEditModalOpen(false);
    } catch (err) {
      setSubmitError("Failed to update project.");
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDeleteProject = async () => {
    try {
      setSubmitLoading(true);
      setSubmitError("");
      await api.delete(`/projects/${id}`);
      navigate("/projects");
    } catch (err) {
      setSubmitError("Failed to delete project.");
      setSubmitLoading(false);
    }
  };

  // --- Handlers: Members ---
  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedUserIds.length === 0) {
      setSubmitError("Please select at least one user.");
      return;
    }
    try {
      setSubmitLoading(true);
      setSubmitError("");
      // Pass the array directly to the backend
      await api.post(`/projectassignments/${id}`, { userIds: selectedUserIds });

      const memRes = await api.get(`/projectassignments/${id}`);
      setMembers(memRes.data);

      setIsAddMemberOpen(false);
      setSelectedUserIds([]); // Reset the array
      setSearchQuery("");
    } catch (err) {
      setSubmitError("Failed to add members to the project.");
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleRemoveMember = async (userIdToRemove: number) => {
    try {
      const remainingUserIds = members
        .filter((m) => m.userId !== userIdToRemove)
        .map((m) => m.userId);

      await api.put(`/projectassignments/${id}`, { userIds: remainingUserIds });
      setMembers(members.filter((m) => m.userId !== userIdToRemove));
    } catch (err) {
      alert("Failed to remove member.");
    }
  };

  const closeAddMemberModal = () => {
    setIsAddMemberOpen(false);
    setSearchQuery("");
    setSelectedUserIds([]);
    setSubmitError("");
  };

  // --- Render logic ---
  if (loading)
    return (
      <div className="flex justify-center py-20 text-slate-500">
        Loading project...
      </div>
    );
  if (error || !project)
    return (
      <div className="text-red-500 p-6">{error || "Project not found"}</div>
    );

  // Filter available users for the Add Member modal
  const nonMembers = availableUsers.filter(
    (u) => !members.some((m) => m.userId === u.id),
  );
  const filteredNonMembers = nonMembers.filter(
    (u) =>
      u.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      u.email.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  const formattedDeadline = new Date(project.deadline).toLocaleDateString();

  return (
    <div className="space-y-6">
      <Link
        to="/projects"
        className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
      >
        <ArrowLeft size={16} /> Back to Projects
      </Link>

      {/* Project Header */}
      <div className="card p-6 border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 rounded-xl shadow-sm">
        <div className="flex justify-between items-start">
          <div>
            <h1 className="text-xl font-bold text-slate-900 dark:text-white">
              {project.name}
            </h1>
            <p className="mt-2 text-sm leading-relaxed text-slate-500 dark:text-slate-400">
              {project.description}
            </p>
          </div>

          {isAdmin && (
            <div className="flex gap-2">
              <button
                onClick={openEditModal}
                className="p-2 text-slate-400 hover:text-blue-600 dark:hover:text-blue-400 transition-colors rounded-lg hover:bg-slate-50 dark:hover:bg-slate-800"
                title="Edit Project"
              >
                <Edit size={18} />
              </button>
              <button
                onClick={() => setIsDeleteModalOpen(true)}
                className="p-2 text-slate-400 hover:text-red-600 dark:hover:text-red-400 transition-colors rounded-lg hover:bg-slate-50 dark:hover:bg-slate-800"
                title="Delete Project"
              >
                <Trash2 size={18} />
              </button>
            </div>
          )}
        </div>

        <div className="mt-4 flex flex-wrap items-center gap-4 text-sm text-slate-500 dark:text-slate-400">
          <span className="inline-flex items-center gap-1.5">
            <Calendar size={14} /> {formattedDeadline}
          </span>
          <span className="inline-flex items-center gap-1.5">
            <ListTodo size={14} /> {projectTasks.length} tasks
          </span>
          <span className="inline-flex items-center gap-1.5">
            <Users size={14} /> {members.length} members
          </span>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Members Column */}
        <div className="card lg:col-span-1 border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 rounded-xl shadow-sm h-fit">
          <div className="border-b border-slate-200 px-5 py-4 dark:border-slate-700/50 flex justify-between items-center">
            <h2 className="text-sm font-semibold text-slate-900 dark:text-white">
              Members
            </h2>
            {isAdmin && (
              <button
                onClick={() => setIsAddMemberOpen(true)}
                className="inline-flex items-center gap-1 text-xs font-medium text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300"
              >
                <Plus size={14} /> Add
              </button>
            )}
          </div>
          <div className="divide-y divide-slate-100 dark:divide-slate-700/50">
            {members.length === 0 ? (
              <div className="px-5 py-6 text-center text-sm text-slate-500">
                No members assigned.
              </div>
            ) : (
              members.map((m) => (
                <div
                  key={m.userId}
                  className="flex items-center justify-between px-5 py-3 group"
                >
                  <div className="flex items-center gap-3">
                    <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-500 text-xs font-semibold text-white uppercase">
                      {m.name.charAt(0)}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-slate-900 dark:text-slate-100">
                        {m.name}
                      </p>
                      <p className="truncate text-xs text-slate-500 dark:text-slate-400">
                        {m.email}
                      </p>
                    </div>
                  </div>
                  {isAdmin && (
                    <button
                      onClick={() => handleRemoveMember(m.userId)}
                      className="text-slate-300 hover:text-red-500 dark:text-slate-600 dark:hover:text-red-400 opacity-0 group-hover:opacity-100 transition-all"
                      title="Remove Member"
                    >
                      <UserMinus size={16} />
                    </button>
                  )}
                </div>
              ))
            )}
          </div>
        </div>

        {/* Tasks Column */}
        <div className="card lg:col-span-2 border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 rounded-xl shadow-sm">
          <div className="border-b border-slate-200 px-5 py-4 dark:border-slate-700/50 flex justify-between items-center">
            <h2 className="text-sm font-semibold text-slate-900 dark:text-white">
              Tasks
            </h2>
            {isAdmin && (
              <button 
                onClick={() => navigate('/tasks/create', { state: { preselectedProjectId: Number(id) } })}
                className="inline-flex items-center gap-1 text-xs font-medium text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300"
              >
                <Plus size={14} /> Add Task
              </button>
            )}
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-slate-100 dark:border-slate-700/50">
                  <th className="px-4 sm:px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Title</th>
                  <th className="hidden sm:table-cell px-4 sm:px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Assigned To</th>
                  <th className="px-4 sm:px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Status</th>
                  <th className="hidden sm:table-cell px-4 sm:px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Priority</th>
                  <th className="hidden lg:table-cell px-4 sm:px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Deadline</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700/50">
                {projectTasks.length === 0 ? (
                  <tr>
                    <td
                      colSpan={5}
                      className="px-5 py-6 text-center text-sm text-slate-500"
                    >
                      No tasks created for this project yet.
                    </td>
                  </tr>
                ) : (
                  projectTasks.map((t) => (
                    <tr
                      key={t.id}
                      className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors"
                    >
                      <td className="px-4 sm:px-5 py-3 max-w-[140px] sm:max-w-none truncate">
                        <Link
                          to={`/tasks/${t.id}`}
                          className="text-sm font-medium text-slate-900 hover:text-blue-500 dark:text-slate-100 dark:hover:text-blue-400"
                        >
                          {t.title}
                        </Link>
                      </td>
                      <td className="hidden sm:table-cell px-4 sm:px-5 py-3 text-sm text-slate-600 dark:text-slate-300 truncate max-w-[120px] sm:max-w-none">
                        {t.assignedTo || "Unassigned"}
                      </td>
                      <td className="px-4 sm:px-5 py-3">
                        <StatusBadge status={t.status as any} />
                      </td>
                      <td className="hidden sm:table-cell px-4 sm:px-5 py-3">
                        <PriorityBadge priority={t.priority as any} />
                      </td>
                      <td className="hidden lg:table-cell px-4 sm:px-5 py-3 text-sm text-slate-600 dark:text-slate-300">
                        {new Date(t.deadline).toLocaleDateString()}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* --- MODALS --- */}

      {/* 1. Add Member Modal */}
      {isAddMemberOpen && isAdmin && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 w-full max-w-md rounded-xl shadow-xl overflow-hidden flex flex-col max-h-[90vh]">
            <div className="p-5 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center shrink-0">
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                Add Project Member
              </h2>
              <button
                onClick={closeAddMemberModal}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              >
                <X size={20} />
              </button>
            </div>

            <form
              onSubmit={handleAddMember}
              className="flex flex-col overflow-hidden"
            >
              <div className="p-5 overflow-y-auto">
                {submitError && (
                  <div className="mb-4 p-3 text-sm text-red-600 bg-red-50 dark:bg-red-900/20 rounded-lg">
                    {submitError}
                  </div>
                )}

                {/* Search Bar */}
                <div className="relative mb-4">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Search className="h-4 w-4 text-slate-400" />
                  </div>
                  <input
                    type="text"
                    placeholder="Search by name or email..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full pl-9 pr-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-sm text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                {/* Custom Scrollable List */}
                <div className="border border-slate-200 dark:border-slate-800 rounded-lg overflow-hidden max-h-60 overflow-y-auto">
                  {filteredNonMembers.length === 0 ? (
                    <div className="p-4 text-center text-sm text-slate-500">
                      {nonMembers.length === 0
                        ? "All users are already in this project."
                        : "No users match your search."}
                    </div>
                  ) : (
                    <div className="divide-y divide-slate-100 dark:divide-slate-800">
                      {filteredNonMembers.map((u) => (
                        <label
                          key={u.id}
                          className={`flex items-center px-4 py-3 cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors ${selectedUserIds.includes(u.id) ? "bg-blue-50 dark:bg-blue-900/10" : ""}`}
                        >
                          <input
                            type="checkbox"
                            checked={selectedUserIds.includes(u.id)}
                            onChange={(e) => {
                              if (e.target.checked) {
                                setSelectedUserIds([...selectedUserIds, u.id]);
                              } else {
                                setSelectedUserIds(
                                  selectedUserIds.filter((id) => id !== u.id),
                                );
                              }
                            }}
                            className="w-4 h-4 rounded text-blue-600 border-slate-300 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-700"
                          />
                          <div className="ml-3 flex-1 min-w-0">
                            <p className="text-sm font-medium text-slate-900 dark:text-white truncate">
                              {u.name}
                            </p>
                            <p className="text-xs text-slate-500 dark:text-slate-400 truncate">
                              {u.email}
                            </p>
                          </div>
                        </label>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              <div className="p-5 border-t border-slate-100 dark:border-slate-800 flex justify-end gap-3 shrink-0 bg-slate-50/50 dark:bg-slate-900/50">
                <button
                  type="button"
                  onClick={closeAddMemberModal}
                  className="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitLoading || selectedUserIds.length === 0}
                  className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg disabled:opacity-50 transition-colors"
                >
                  {submitLoading
                    ? "Adding..."
                    : `Add Selected (${selectedUserIds.length})`}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 2. Edit Project Modal */}
      {isEditModalOpen && isAdmin && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 w-full max-w-md rounded-xl shadow-xl overflow-hidden">
            <div className="p-5 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center">
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                Edit Project
              </h2>
              <button
                onClick={() => setIsEditModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              >
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleEditProject} className="p-5 space-y-4">
              {submitError && (
                <div className="p-3 text-sm text-red-600 bg-red-50 dark:bg-red-900/20 rounded-lg">
                  {submitError}
                </div>
              )}
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Name
                </label>
                <input
                  type="text"
                  value={editFormData.name}
                  onChange={(e) =>
                    setEditFormData({ ...editFormData, name: e.target.value })
                  }
                  required
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Description
                </label>
                <textarea
                  rows={3}
                  value={editFormData.description}
                  onChange={(e) =>
                    setEditFormData({
                      ...editFormData,
                      description: e.target.value,
                    })
                  }
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Deadline
                </label>
                <input
                  type="date"
                  value={editFormData.deadline}
                  onChange={(e) =>
                    setEditFormData({
                      ...editFormData,
                      deadline: e.target.value,
                    })
                  }
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div className="pt-2 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  className="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitLoading}
                  className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg disabled:opacity-50"
                >
                  {submitLoading ? "Saving..." : "Save Changes"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 3. Delete Project Confirmation Modal */}
      {isDeleteModalOpen && isAdmin && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 w-full max-w-sm rounded-xl shadow-xl overflow-hidden p-6 text-center">
            <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-red-100 dark:bg-red-900/30 mb-4">
              <Trash2 className="h-6 w-6 text-red-600 dark:text-red-400" />
            </div>
            <h3 className="text-lg font-medium text-slate-900 dark:text-white">
              Delete Project?
            </h3>
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
              Are you sure you want to delete this project? All associated tasks
              and assignments will be permanently removed.
            </p>
            {submitError && (
              <p className="mt-2 text-sm text-red-600">{submitError}</p>
            )}
            <div className="mt-6 flex justify-center gap-3">
              <button
                onClick={() => setIsDeleteModalOpen(false)}
                className="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700"
              >
                Cancel
              </button>
              <button
                onClick={handleDeleteProject}
                disabled={submitLoading}
                className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg disabled:opacity-50"
              >
                {submitLoading ? "Deleting..." : "Delete Permanently"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
