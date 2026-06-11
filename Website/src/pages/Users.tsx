import { useState, useEffect } from 'react';
import { Trash2, ArrowUpCircle, ArrowDownCircle, Plus, X, AlertTriangle } from 'lucide-react';
import api from '../api/axios';
import { RoleBadge } from '../components/Badge';
import type { Role } from '../types';

interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  avatar: string;
  createdAt: string;
}

interface ConfirmDialog {
  open: boolean;
  userId: number | null;
  userName: string;
}

export function UsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalCount, setTotalCount] = useState(0);

  const [confirmDialog, setConfirmDialog] = useState<ConfirmDialog>({
    open: false, userId: null, userName: '',
  });
  const [deleting, setDeleting] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newUser, setNewUser] = useState({ name: '', email: '', password: '' });
  const [creating, setCreating] = useState(false);

  useEffect(() => { fetchUsers(); }, [page]);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const res = await api.get(`/users?PageNumber=${page}&PageSize=10`);
      const items: User[] = (res.data.items ?? res.data).map((u: any) => ({
        ...u,
        avatar: u.name?.split(' ').map((n: string) => n[0]).join('').slice(0, 2).toUpperCase(),
        createdAt: u.createdAt
          ? new Date(u.createdAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
          : '—',
      }));
      setUsers(items);
      setTotalPages(res.data.totalPages ?? 1);
      setTotalCount(res.data.totalCount ?? items.length);
    } catch {
      alert('Failed to load users.');
    } finally {
      setLoading(false);
    }
  };

  const promote = async (id: number) => {
    try {
      await api.put(`/users/${id}/promote`);
      fetchUsers();
    } catch { alert('Failed to promote user.'); }
  };

  const demote = async (id: number) => {
    try {
      await api.put(`/users/${id}/demote`);
      fetchUsers();
    } catch { alert('Failed to demote user.'); }
  };

  const askDelete = (user: User) => {
    setConfirmDialog({ open: true, userId: user.id, userName: user.name });
  };

  const confirmDelete = async () => {
    if (!confirmDialog.userId) return;
    try {
      setDeleting(true);
      await api.delete(`/users/${confirmDialog.userId}`);
      setConfirmDialog({ open: false, userId: null, userName: '' });
      fetchUsers();
    } catch {
      alert('Failed to delete user. They may have tasks assigned.');
    } finally {
      setDeleting(false);
    }
  };

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setCreating(true);
      await api.post('/users', newUser);
      setIsModalOpen(false);
      setNewUser({ name: '', email: '', password: '' });
      fetchUsers();
    } catch {
      alert('Failed to create user.');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Users</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {totalCount} user{totalCount !== 1 ? 's' : ''} total
          </p>
        </div>
        <button
          onClick={() => { setNewUser({ name: '', email: '', password: '' }); setIsModalOpen(true); }}
          className="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700"
        >
          <Plus size={15} /> Create User
        </button>
      </div>

      {/* Table Card */}
      <div className="card">
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 dark:border-slate-700/50">
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Admin only — manage team members and their roles.
          </p>
          <span className="text-sm font-medium text-slate-700 dark:text-slate-300">
            {totalCount} user{totalCount !== 1 ? 's' : ''}
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-slate-100 dark:border-slate-700/50">
                <th className="px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Name</th>
                <th className="hidden sm:table-cell px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Email</th>
                <th className="px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Role</th>
                <th className="hidden md:table-cell px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Created At</th>
                <th className="px-4 sm:px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-700/50">
              {loading ? (
                <tr><td colSpan={5} className="px-6 py-8 text-center text-sm text-slate-500">Loading…</td></tr>
              ) : users.length === 0 ? (
                <tr><td colSpan={5} className="px-6 py-8 text-center text-sm text-slate-500">No users found.</td></tr>
              ) : (
                users.map(u => (
                  <tr key={u.id} className="hover:bg-slate-50 dark:hover:bg-slate-700/30">
                    <td className="px-4 sm:px-6 py-3.5">
                      <div className="flex items-center gap-3">
                        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-blue-500 text-xs font-semibold text-white">
                          {u.avatar}
                        </div>
                        <span className="text-sm font-medium text-slate-900 dark:text-slate-100">{u.name}</span>
                      </div>
                    </td>
                    <td className="hidden sm:table-cell px-4 sm:px-6 py-3.5 text-sm text-slate-600 dark:text-slate-300">{u.email}</td>
                    <td className="px-4 sm:px-6 py-3.5"><RoleBadge role={u.role} /></td>
                    <td className="hidden md:table-cell px-4 sm:px-6 py-3.5 text-sm text-slate-600 dark:text-slate-300">{u.createdAt}</td>
                    <td className="px-4 sm:px-6 py-3.5">
                      <div className="flex flex-wrap items-center gap-2">
                        <button onClick={() => promote(u.id)} disabled={u.role === 'Admin'}
                          className="inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium text-sky-600 hover:bg-sky-50 disabled:opacity-40 dark:text-sky-400 dark:hover:bg-sky-900/20">
                          <ArrowUpCircle size={14} /> <span className="hidden sm:inline">Promote</span>
                        </button>
                        <button onClick={() => demote(u.id)} disabled={u.role === 'Employee'}
                          className="inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium text-amber-600 hover:bg-amber-50 disabled:opacity-40 dark:text-amber-400 dark:hover:bg-amber-900/20">
                          <ArrowDownCircle size={14} /> <span className="hidden sm:inline">Demote</span>
                        </button>
                        <button onClick={() => askDelete(u)}
                          className="inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-900/20">
                          <Trash2 size={14} /> <span className="hidden sm:inline">Delete</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-100 px-6 py-3 dark:border-slate-700/50">
            <p className="text-xs text-slate-500 dark:text-slate-400">Page {page} of {totalPages}</p>
            <div className="flex gap-2">
              <button onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1}
                className="rounded-lg px-3 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-100 disabled:opacity-40 dark:text-slate-300 dark:hover:bg-slate-700">
                Previous
              </button>
              <button onClick={() => setPage(p => Math.min(totalPages, p + 1))} disabled={page === totalPages}
                className="rounded-lg px-3 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-100 disabled:opacity-40 dark:text-slate-300 dark:hover:bg-slate-700">
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {confirmDialog.open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-sm rounded-xl bg-white p-6 shadow-xl dark:bg-slate-900">
            <div className="mb-4 flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-red-100 dark:bg-red-900/30">
                <AlertTriangle size={20} className="text-red-600 dark:text-red-400" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-slate-900 dark:text-white">Delete user?</h2>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                  <span className="font-medium text-slate-700 dark:text-slate-200">{confirmDialog.userName}</span> will be permanently removed.
                </p>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <button onClick={() => setConfirmDialog({ open: false, userId: null, userName: '' })} disabled={deleting}
                className="rounded-lg px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 disabled:opacity-50 dark:text-slate-300 dark:hover:bg-slate-800">
                Cancel
              </button>
              <button onClick={confirmDelete} disabled={deleting}
                className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-60">
                {deleting ? 'Deleting…' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create User Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-sm rounded-xl bg-white p-6 shadow-xl dark:bg-slate-900">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">New User</h2>
              <button onClick={() => setIsModalOpen(false)}
                className="rounded-lg p-1 text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800">
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleCreateUser} className="space-y-3" autoComplete="off">
              <input placeholder="Full name" value={newUser.name} required
                onChange={e => setNewUser({ ...newUser, name: e.target.value })}
                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100" />
              <input type="email" placeholder="Email address" value={newUser.email} required
                onChange={e => setNewUser({ ...newUser, email: e.target.value })}
                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100" />
              <input type="password" placeholder="Password" value={newUser.password} required
                autoComplete="new-password"
                onChange={e => setNewUser({ ...newUser, password: e.target.value })}
                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100" />
              <button type="submit" disabled={creating}
                className="w-full rounded-lg bg-blue-600 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-60">
                {creating ? 'Creating…' : 'Create User'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}