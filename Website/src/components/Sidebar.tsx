import { NavLink, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  ListTodo,
  FolderKanban,
  Users,
  ChevronLeft,
  ChevronRight,
  LogOut,
  User,
} from "lucide-react";
import { useState, useRef, useEffect } from "react";
import { useAuth } from "../context/AuthContext";

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
  mobileMenuOpen?: boolean;
  setMobileMenuOpen?: (open: boolean) => void;
}

export function Sidebar({ collapsed, onToggle, mobileMenuOpen, setMobileMenuOpen }: SidebarProps) {
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const navigate = useNavigate();
  const { user, logout, isAdmin } = useAuth();
  const profileRef = useRef<HTMLDivElement>(null);

  const links = [
    { to: "/dashboard", icon: LayoutDashboard, label: "Dashboard" },
    { to: "/tasks", icon: ListTodo, label: "Tasks" },
    { to: "/projects", icon: FolderKanban, label: "Projects" },
    ...(isAdmin ? [{ to: "/users", icon: Users, label: "Users" }] : []),
  ];

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const initials = user?.name
    ? user.name
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)
    : "??";

  // Close popup when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        profileRef.current &&
        !profileRef.current.contains(event.target as Node)
      ) {
        setShowProfileMenu(false);
      }
    }
    if (showProfileMenu) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [showProfileMenu]);

  return (
    <aside
      className={`fixed left-0 top-0 z-40 h-screen flex flex-col border-r border-slate-200 bg-white transition-all duration-300 dark:border-slate-700/50 dark:bg-navy-800 ${
        mobileMenuOpen ? "translate-x-0" : "-translate-x-full"
      } md:translate-x-0 ${collapsed ? "w-64 md:w-[68px]" : "w-64 md:w-60"}`}
    >
      {/* Header */}
      <div className="flex h-16 items-center justify-between border-b border-slate-200 px-4 dark:border-slate-700/50">
        <span className={`text-lg font-bold text-slate-900 dark:text-white whitespace-nowrap ${collapsed ? "block md:hidden" : "block"}`}>
          Task Tracker
        </span>
        <button
          onClick={onToggle}
          className="hidden md:flex ml-auto h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-700 dark:hover:text-slate-300"
        >
          {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
        </button>
        <button
          onClick={() => setMobileMenuOpen?.(false)}
          className="md:hidden ml-auto flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-700 dark:hover:text-slate-300"
        >
          <ChevronLeft size={18} />
        </button>
      </div>

      {/* Nav Links */}
      <nav className="flex-1 space-y-1 px-3 py-4 overflow-y-auto">
        {links.map((l) => (
          <NavLink
            key={l.to}
            to={l.to}
            onClick={() => setMobileMenuOpen?.(false)}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors duration-150 ${
                isActive
                  ? "bg-blue-50 text-blue-600 dark:bg-blue-900/20 dark:text-blue-400"
                  : "text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-700/50 dark:hover:text-slate-200"
              }`
            }
          >
            <l.icon size={20} className="shrink-0" />
            <span className={`whitespace-nowrap ${collapsed ? "block md:hidden" : "block"}`}>{l.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* User Section */}
      <div
        ref={profileRef}
        className="border-t border-slate-200 p-3 dark:border-slate-700/50 relative"
      >
        <button
          onClick={() => setShowProfileMenu((m) => !m)}
          className={`flex w-full items-center gap-3 rounded-lg p-2 hover:bg-slate-100 dark:hover:bg-slate-700/50 transition-colors ${
            collapsed ? "md:justify-center" : ""
          }`}
        >
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-500 text-sm font-semibold text-white">
            {initials}
          </div>
          <div className={`min-w-0 flex-1 text-left ${collapsed ? "block md:hidden" : "block"}`}>
            <p className="truncate text-sm font-medium text-slate-900 dark:text-slate-100">
              {user?.name}
            </p>
            <p className="truncate text-xs text-slate-500 dark:text-slate-400">
              {user?.role}
            </p>
          </div>
        </button>

        {/* Profile Popup: outside when collapsed, inside when expanded */}
        {showProfileMenu && (
          <div
            className={`absolute z-50 rounded-xl border border-slate-200 bg-white shadow-xl dark:border-slate-700 dark:bg-navy-800 overflow-hidden ${
              collapsed
                ? "bottom-0 left-full ml-2 w-56"
                : "bottom-full left-0 right-0 mb-2"
            }`}
          >
            {/* Profile Info */}
            <div className="px-4 py-3 border-b border-slate-200 dark:border-slate-700">
              <p className="text-sm font-semibold text-slate-900 dark:text-white">
                {user?.name}
              </p>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {user?.email}
              </p>
              <span
                className={`mt-1 inline-block text-xs px-2 py-0.5 rounded-full font-medium ${
                  isAdmin
                    ? "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400"
                    : "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                }`}
              >
                {user?.role}
              </span>
            </div>

            {/* View Profile */}
            <button
              onClick={() => {
                navigate("/profile");
                setShowProfileMenu(false);
              }}
              className="flex w-full items-center gap-2 px-4 py-2.5 text-sm text-slate-600 hover:bg-slate-50 dark:text-slate-300 dark:hover:bg-slate-700/50"
            >
              <User size={16} />
              View Profile
            </button>

            {/* Logout */}
            <button
              onClick={handleLogout}
              className="flex w-full items-center gap-2 px-4 py-2.5 text-sm text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20"
            >
              <LogOut size={16} />
              Log Out
            </button>
          </div>
        )}
      </div>
    </aside>
  );
}
