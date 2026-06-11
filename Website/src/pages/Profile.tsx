import { useAuth } from '../context/AuthContext';

export function ProfilePage() {
  const { user } = useAuth();

  return (
    <div className="max-w-md mx-auto mt-10">
      <div className="card p-8">
        <div className="flex items-center gap-4 mb-6">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-blue-500 text-xl font-bold text-white">
            {user?.name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)}
          </div>
          <div>
            <h1 className="text-xl font-bold text-slate-900 dark:text-white">{user?.name}</h1>
            <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
              user?.role === 'Admin'
                ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'
                : 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
            }`}>
              {user?.role}
            </span>
          </div>
        </div>

        <div className="space-y-4">
          <div>
            <p className="text-xs text-slate-500 dark:text-slate-400 uppercase tracking-wide">Name</p>
            <p className="text-sm font-medium text-slate-900 dark:text-white mt-1">{user?.name}</p>
          </div>
          <div>
            <p className="text-xs text-slate-500 dark:text-slate-400 uppercase tracking-wide">Email</p>
            <p className="text-sm font-medium text-slate-900 dark:text-white mt-1">{user?.email}</p>
          </div>
          <div>
            <p className="text-xs text-slate-500 dark:text-slate-400 uppercase tracking-wide">Role</p>
            <p className="text-sm font-medium text-slate-900 dark:text-white mt-1">{user?.role}</p>
          </div>
        </div>
      </div>
    </div>
  );
}