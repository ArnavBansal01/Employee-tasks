import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from './context/ThemeContext';
import { AuthProvider } from './context/AuthContext';
import { Layout } from './components/Layout';
import { LoginPage } from './pages/Login';
import { DashboardPage } from './pages/Dashboard';
import { TasksPage } from './pages/Tasks';
import { TaskDetailPage } from './pages/TaskDetail';
import { ProjectsPage } from './pages/Projects';
import { ProjectDetailPage } from './pages/ProjectDetail';
import { UsersPage } from './pages/Users';
import { NotFoundPage } from './pages/NotFound';

import { ProtectedRoute } from './components/ProtectedRoute';
import { ProfilePage } from './pages/Profile';
import { CreateTaskPage } from './pages/CreateTask';

export default function App() {
  return (
    <AuthProvider>
    <ThemeProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<Layout />}>
            <Route path="/dashboard" element={    <ProtectedRoute><DashboardPage /></ProtectedRoute>
} />
            <Route path="/tasks" element={    <ProtectedRoute><TasksPage /></ProtectedRoute>
} />
            <Route path="/tasks/:id" element={<ProtectedRoute><TaskDetailPage /></ProtectedRoute>} />
            <Route path="/tasks/create" 
                  element={<ProtectedRoute adminOnly><CreateTaskPage /></ProtectedRoute>} 
                />
            <Route path="/projects" element={    <ProtectedRoute><ProjectsPage /></ProtectedRoute>
} />
            <Route path="/projects/:id" element={<ProtectedRoute><ProjectDetailPage /></ProtectedRoute>} />
            
            <Route path="/users" element={<ProtectedRoute adminOnly><UsersPage /></ProtectedRoute>} />          </Route>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<NotFoundPage />} />
          <Route path="/profile" element={
  <ProtectedRoute><ProfilePage /></ProtectedRoute>
} />
        </Routes>
      </BrowserRouter>
    
    </ThemeProvider>
    </AuthProvider>
  );
}
