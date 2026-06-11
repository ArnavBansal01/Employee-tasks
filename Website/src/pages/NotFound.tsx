import { Link } from 'react-router-dom';
import { AlertTriangle } from 'lucide-react';

export function NotFoundPage() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-100 dark:bg-slate-700/50">
        <AlertTriangle size={32} className="text-slate-400 dark:text-slate-500" />
      </div>
      <h1 className="text-4xl font-bold text-slate-900 dark:text-white">404</h1>
      <p className="text-sm text-slate-500 dark:text-slate-400">
        The page you're looking for doesn't exist.
      </p>
      <Link to="/dashboard" className="btn-primary mt-2">
        Go to Dashboard
      </Link>
    </div>
  );
}
