import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Moon, Sun, Search, ListTodo, FolderKanban, Menu } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';

interface TopbarProps {
  onMenuClick?: () => void;
}

export function Topbar({ onMenuClick }: TopbarProps) {
  const { dark, toggle } = useTheme();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [query, setQuery] = useState('');
  const [results, setResults] = useState<any[]>([]);
  const [isOpen, setIsOpen] = useState(false);

  const performSearch = async (searchTerm: string) => {
    if (searchTerm.length < 2) {
      setResults([]);
      setIsOpen(false);
      return;
    }
    try {
      const [tasksRes, projRes] = await Promise.all([
        api.get('/tasks?PageSize=100'),
        api.get('/projects')
      ]);

      const tResults = (tasksRes.data.items || [])
        .filter((t: any) => t.title.toLowerCase().includes(searchTerm.toLowerCase()))
        .map((t: any) => ({ ...t, type: 'task', label: t.title, link: `/tasks/${t.id}` }));

      const pResults = (projRes.data || [])
        .filter((p: any) => p.name.toLowerCase().includes(searchTerm.toLowerCase()))
        .map((p: any) => ({ ...p, type: 'project', label: p.name, link: `/projects/${p.id}` }));

      setResults([...tResults, ...pResults]);
      setIsOpen(true);
    } catch (err) {
      console.error('Search failed', err);
    }
  };

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 md:px-6 dark:border-slate-700/50 dark:bg-slate-900">
      <div className="flex items-center gap-3 flex-1 min-w-0">
        <button
          onClick={onMenuClick}
          className="flex h-9 w-9 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 md:hidden dark:hover:bg-slate-800"
        >
          <Menu size={20} />
        </button>

        {/* Search */}
        <div className="relative flex-1 max-w-md min-w-0">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search tasks, projects..."
            value={query}
            onChange={e => {
              setQuery(e.target.value);
              performSearch(e.target.value);
            }}
            onBlur={() => setTimeout(() => setIsOpen(false), 150)}
            className="h-9 w-full rounded-lg border border-slate-200 bg-slate-50 pl-9 pr-3 text-sm text-slate-900 placeholder-slate-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
          />

          {/* Search Results Dropdown */}
          {isOpen && results.length > 0 && (
            <div className="absolute top-11 left-0 w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl shadow-2xl z-50 overflow-hidden">
              {results.map((res, i) => (
                <button
                  key={i}
                  onMouseDown={e => {
                    e.preventDefault();
                    navigate(res.link);
                    setIsOpen(false);
                    setQuery('');
                  }}
                  className="w-full flex items-center gap-3 px-4 py-3 hover:bg-slate-50 dark:hover:bg-slate-800 text-left transition-colors"
                >
                  {res.type === 'task' ? (
                    <ListTodo size={16} className="text-blue-500 shrink-0" />
                  ) : (
                    <FolderKanban size={16} className="text-purple-500 shrink-0" />
                  )}
                  <span className="text-sm text-slate-700 dark:text-slate-200 truncate">{res.label}</span>
                </button>
              ))}
            </div>
          )}

          {/* No results */}
          {isOpen && query.length >= 2 && results.length === 0 && (
            <div className="absolute top-11 left-0 w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl shadow-2xl z-50 px-4 py-3">
              <p className="text-sm text-slate-500 dark:text-slate-400">No results found.</p>
            </div>
          )}
        </div>
      </div>

      {/* Right side — user name + theme toggle */}
      <div className="flex items-center gap-3 ml-4">
        {user && (
          <div className="hidden md:flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-500 text-xs font-semibold text-white">
              {user.name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)}
            </div>
            <span className="text-sm font-medium text-slate-700 dark:text-slate-300">
              {user.name}
            </span>
          </div>
        )}
        <button
          onClick={toggle}
          className="flex h-9 w-9 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800"
        >
          {dark ? <Sun size={18} /> : <Moon size={18} />}
        </button>
      </div>
    </header>
  );
}