export type Status = 'Pending' | 'InProgress' | 'Completed';
export type Priority = 'Low' | 'Medium' | 'High';
export type Role = 'Admin' | 'Employee';

export interface User {
  id: string;
  name: string;
  email: string;
  role: Role;
  avatar: string;
  createdAt: string;
}

export interface Task {
  id: string;
  title: string;
  description: string;
  assignedTo: string;
  projectId: string;
  status: Status;
  priority: Priority;
  deadline: string;
  createdAt: string;
}

export interface Project {
  id: string;
  name: string;
  description: string;
  deadline: string;
  members: string[];
  createdAt: string;
}
