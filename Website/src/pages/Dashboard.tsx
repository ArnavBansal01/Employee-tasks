import { Link } from "react-router-dom";
import {
  ListTodo,
  Clock,
  Loader2,
  CheckCircle2,
  ArrowRight,
  FolderKanban,
  Target,
  Trophy
} from "lucide-react";
import { useState, useEffect } from "react";
import { StatusBadge, PriorityBadge } from "../components/Badge";
import { useAuth } from "../context/AuthContext";
import api from "../api/axios";

interface TaskItem {
  id: number;
  title: string;
  assignedTo: string;
  status: string;
  priority: string;
  deadline: string;
  projectName: string;
  projectId: number; // Added so we can link tasks to projects
  updatedAt?: string;
  createdAt?: string;
}

interface ProjectItem {
  id: number;
  name: string;
  totalTasks: number;
}

export function DashboardPage() {
  const { user } = useAuth();
  
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [projects, setProjects] = useState<ProjectItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        const [tasksRes, projectsRes] = await Promise.all([
          api.get("/tasks?PageNumber=1&PageSize=100"),
          api.get("/projects")
        ]);
        
        setTasks(tasksRes.data.items || []);
        setProjects(projectsRes.data || []);
      } catch (err) {
        setError("Failed to load dashboard data.");
      } finally {
        setLoading(false);
      }
    };
    
    fetchDashboardData();
  }, []);

  // --- Dynamic Calculations ---
  
  // Calculate Completed Projects (Projects where ALL tasks are 'Completed')
  const completedProjectsCount = projects.filter((p) => {
    const projectTasks = tasks.filter((t) => t.projectId === p.id);
    return projectTasks.length > 0 && projectTasks.every((t) => t.status === "Completed");
  }).length;

  const completedTasks = tasks.filter((t) => t.status === "Completed").length;
  const completionRate = tasks.length > 0 ? Math.round((completedTasks / tasks.length) * 100) : 0;

  // --- Separated Stat Arrays ---

  const projectStats = [
    {
      label: "Total Projects",
      value: projects.length,
      icon: FolderKanban,
      bg: "bg-purple-100 dark:bg-purple-900/30",
      iconColor: "text-purple-500",
    },
    {
      label: "Completed Projects",
      value: completedProjectsCount,
      icon: Trophy,
      bg: "bg-indigo-100 dark:bg-indigo-900/30",
      iconColor: "text-indigo-500",
    }
  ];

  const taskStats = [
    {
      label: "Total Tasks",
      value: tasks.length,
      icon: ListTodo,
      bg: "bg-blue-100 dark:bg-blue-900/30",
      iconColor: "text-blue-500",
    },
    {
      label: "Pending",
      value: tasks.filter((t) => t.status === "Pending").length,
      icon: Clock,
      bg: "bg-amber-100 dark:bg-amber-900/30",
      iconColor: "text-amber-500",
    },
    {
      label: "In Progress",
      value: tasks.filter((t) => t.status === "InProgress").length,
      icon: Loader2,
      bg: "bg-sky-100 dark:bg-sky-900/30",
      iconColor: "text-sky-500",
    },
    {
      label: "Completed",
      value: completedTasks,
      icon: CheckCircle2,
      bg: "bg-emerald-100 dark:bg-emerald-900/30",
      iconColor: "text-emerald-500",
    },
    {
      label: "Completion Rate",
      value: `${completionRate}%`,
      icon: Target,
      bg: "bg-teal-100 dark:bg-teal-900/30",
      iconColor: "text-teal-500",
    },
  ];

  // Sort tasks for the Recent list
  const recentTasks = [...tasks]
    .sort((a, b) => {
      const dateA = new Date(a.updatedAt || a.createdAt || 0).getTime();
      const dateB = new Date(b.updatedAt || b.createdAt || 0).getTime();
      if (dateA === dateB) return b.id - a.id;
      return dateB - dateA;
    })
    .slice(0, 6);

  if (loading)
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-slate-500 dark:text-slate-400 font-medium">Loading workspace data...</p>
      </div>
    );

  if (error)
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-red-500 font-medium">{error}</p>
      </div>
    );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white tracking-tight">
          Welcome back, {user?.name.split(" ")[0]}
        </h1>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
          Here is the latest snapshot of your projects and task activity.
        </p>
      </div>

      {/* --- ROW 1: Project Overview --- */}
      <div>
        <h2 className="text-sm font-bold text-slate-900 dark:text-white uppercase tracking-wider mb-3 ml-1">
          Project Overview
        </h2>
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          {projectStats.map((s) => (
            <div key={s.label} className="card p-5 border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 rounded-xl shadow-sm hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between gap-2">
                <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 break-words flex-1 min-w-0">
                  {s.label}
                </p>
                <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${s.bg}`}>
                  <s.icon size={18} className={s.iconColor} />
                </div>
              </div>
              <p className="mt-2 text-2xl font-bold text-slate-900 dark:text-white">
                {s.value}
              </p>
            </div>
          ))}
        </div>
      </div>

      {/* --- ROW 2: Task Pipeline --- */}
      <div>
        <h2 className="text-sm font-bold text-slate-900 dark:text-white uppercase tracking-wider mb-3 ml-1">
          Task Pipeline
        </h2>
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
          {taskStats.map((s) => (
            <div key={s.label} className="card p-5 border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 rounded-xl shadow-sm hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between gap-2">
                <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 break-words flex-1 min-w-0">
                  {s.label}
                </p>
                <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${s.bg}`}>
                  <s.icon size={18} className={s.iconColor} />
                </div>
              </div>
              <p className="mt-2 text-2xl font-bold text-slate-900 dark:text-white">
                {s.value}
              </p>
            </div>
          ))}
        </div>
      </div>

      {/* --- ROW 3: Recent Tasks Table --- */}
      <div className="card border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 rounded-xl shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 dark:border-slate-800">
          <h2 className="text-base font-semibold text-slate-900 dark:text-white">
            Recently Updated Tasks
          </h2>
          <Link
            to="/tasks"
            className="flex items-center gap-1 text-sm font-medium text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 transition-colors"
          >
            View all <ArrowRight size={14} />
          </Link>
        </div>

        {tasks.length === 0 ? (
          <div className="px-6 py-10 text-center text-sm text-slate-500 dark:text-slate-400">
            No active tasks found in the workspace.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-slate-100 dark:border-slate-800/50 bg-slate-50/50 dark:bg-slate-800/20">
                  <th className="px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    Title
                  </th>
                  <th className="hidden sm:table-cell px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    Assigned To
                  </th>
                  <th className="hidden md:table-cell px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    Project
                  </th>
                  <th className="px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    Status
                  </th>
                  <th className="hidden sm:table-cell px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    Priority
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800/50">
                {recentTasks.map((t) => (
                  <tr
                    key={t.id}
                    className="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors"
                  >
                    <td className="px-4 sm:px-6 py-3 sm:py-3.5 max-w-[150px] sm:max-w-none truncate">
                      <Link
                        to={`/tasks/${t.id}`}
                        className="text-sm font-medium text-slate-900 hover:text-blue-600 dark:text-slate-100 dark:hover:text-blue-400 transition-colors"
                      >
                        {t.title}
                      </Link>
                    </td>
                    <td className="hidden sm:table-cell px-4 sm:px-6 py-3 sm:py-3.5">
                      <div className="flex items-center gap-2">
                        <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-slate-200 text-xs font-semibold text-slate-600 dark:bg-slate-700 dark:text-slate-300">
                          {t.assignedTo
                            ?.split(" ")
                            .map((n: string) => n[0])
                            .join("")
                            .toUpperCase()
                            .slice(0, 2)}
                        </div>
                        <span className="text-sm text-slate-600 dark:text-slate-300 truncate">
                          {t.assignedTo}
                        </span>
                      </div>
                    </td>
                    <td className="hidden md:table-cell px-4 sm:px-6 py-3 sm:py-3.5">
                      <span className="text-sm text-slate-600 dark:text-slate-300">
                        {t.projectName}
                      </span>
                    </td>
                    <td className="px-4 sm:px-6 py-3 sm:py-3.5">
                      <StatusBadge status={t.status as any} />
                    </td>
                    <td className="hidden sm:table-cell px-4 sm:px-6 py-3 sm:py-3.5">
                      <PriorityBadge priority={t.priority as any} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}