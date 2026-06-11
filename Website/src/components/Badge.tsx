import type { Status, Priority, Role } from '../types';

export function StatusBadge({ status }: { status: Status }) {
  const cls = {
    Pending: 'badge-pending',
    InProgress: 'badge-inprogress',
    Completed: 'badge-completed',
  }[status];
  return <span className={`badge ${cls}`}>{status === 'InProgress' ? 'In Progress' : status}</span>;
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  const cls = { Low: 'badge-low', Medium: 'badge-medium', High: 'badge-high' }[priority];
  return <span className={`badge ${cls}`}>{priority}</span>;
}

export function RoleBadge({ role }: { role: Role }) {
  const cls = { Admin: 'badge-admin', Employee: 'badge-employee' }[role];
  return <span className={`badge ${cls}`}>{role}</span>;
}