import type { User, Task, Project } from '../types';

export const users: User[] = [
  { id: 'u1', name: 'Alex Morgan', email: 'alex@company.com', role: 'Admin', avatar: 'AM', createdAt: '2025-01-15' },
  { id: 'u2', name: 'Jordan Lee', email: 'jordan@company.com', role: 'Manager', avatar: 'JL', createdAt: '2025-02-20' },
  { id: 'u3', name: 'Sam Rivera', email: 'sam@company.com', role: 'Member', avatar: 'SR', createdAt: '2025-03-10' },
  { id: 'u4', name: 'Casey Chen', email: 'casey@company.com', role: 'Member', avatar: 'CC', createdAt: '2025-04-05' },
  { id: 'u5', name: 'Riley Kim', email: 'riley@company.com', role: 'Manager', avatar: 'RK', createdAt: '2025-05-12' },
  { id: 'u6', name: 'Taylor Brooks', email: 'taylor@company.com', role: 'Member', avatar: 'TB', createdAt: '2025-06-01' },
];

export const projects: Project[] = [
  { id: 'p1', name: 'Website Redesign', description: 'Complete overhaul of the company website with modern UI/UX patterns and improved performance.', deadline: '2026-08-30', members: ['u1', 'u2', 'u3'], createdAt: '2025-06-01' },
  { id: 'p2', name: 'Mobile App v2', description: 'Second version of the mobile application with new features and enhanced security.', deadline: '2026-10-15', members: ['u2', 'u4', 'u5'], createdAt: '2025-07-15' },
  { id: 'p3', name: 'API Migration', description: 'Migrate legacy REST APIs to GraphQL with improved documentation and testing.', deadline: '2026-07-20', members: ['u1', 'u3', 'u6'], createdAt: '2025-08-01' },
  { id: 'p4', name: 'Analytics Dashboard', description: 'Build real-time analytics dashboard for business intelligence reporting.', deadline: '2026-09-30', members: ['u4', 'u5', 'u6'], createdAt: '2025-09-01' },
  { id: 'p5', name: 'Security Audit', description: 'Comprehensive security review and penetration testing across all services.', deadline: '2026-06-30', members: ['u1', 'u2'], createdAt: '2025-10-01' },
  { id: 'p6', name: 'Onboarding Flow', description: 'Redesign the user onboarding experience to improve activation rates.', deadline: '2026-11-15', members: ['u3', 'u5', 'u6'], createdAt: '2025-11-01' },
];

export const tasks: Task[] = [
  { id: 't1', title: 'Design homepage mockups', description: 'Create high-fidelity mockups for the new homepage layout including hero section, features grid, and footer redesign.', assignedTo: 'u3', projectId: 'p1', status: 'Completed', priority: 'High', deadline: '2026-02-15', createdAt: '2025-12-01' },
  { id: 't2', title: 'Implement authentication flow', description: 'Build login, registration, and password reset flows with JWT token management and refresh logic.', assignedTo: 'u1', projectId: 'p1', status: 'InProgress', priority: 'High', deadline: '2026-03-01', createdAt: '2025-12-05' },
  { id: 't3', title: 'Set up CI/CD pipeline', description: 'Configure GitHub Actions for automated testing, building, and deployment to staging and production environments.', assignedTo: 'u4', projectId: 'p2', status: 'Pending', priority: 'Medium', deadline: '2026-04-10', createdAt: '2025-12-10' },
  { id: 't4', title: 'Write API documentation', description: 'Document all REST endpoints with request/response examples, error codes, and authentication requirements.', assignedTo: 'u6', projectId: 'p3', status: 'InProgress', priority: 'Medium', deadline: '2026-05-20', createdAt: '2025-12-15' },
  { id: 't5', title: 'Database schema design', description: 'Design normalized database schema for the analytics data warehouse with proper indexing strategy.', assignedTo: 'u2', projectId: 'p4', status: 'Completed', priority: 'High', deadline: '2026-01-30', createdAt: '2025-11-20' },
  { id: 't6', title: 'Security vulnerability scan', description: 'Run automated security scanning tools and compile findings report with severity ratings and remediation steps.', assignedTo: 'u1', projectId: 'p5', status: 'Pending', priority: 'High', deadline: '2026-06-15', createdAt: '2026-01-05' },
  { id: 't7', title: 'Build onboarding wizard', description: 'Create multi-step onboarding wizard with progress indicator, skip options, and personalization questions.', assignedTo: 'u3', projectId: 'p6', status: 'Pending', priority: 'Medium', deadline: '2026-07-01', createdAt: '2026-01-10' },
  { id: 't8', title: 'Performance optimization', description: 'Profile and optimize page load times, reduce bundle size, implement code splitting and lazy loading.', assignedTo: 'u4', projectId: 'p1', status: 'InProgress', priority: 'Low', deadline: '2026-04-20', createdAt: '2025-12-20' },
  { id: 't9', title: 'Unit test coverage', description: 'Increase unit test coverage to 80% for core business logic modules with meaningful assertions.', assignedTo: 'u6', projectId: 'p2', status: 'Pending', priority: 'Low', deadline: '2026-05-30', createdAt: '2026-01-15' },
  { id: 't10', title: 'GraphQL schema migration', description: 'Convert REST endpoints to GraphQL resolvers with proper type definitions and DataLoader patterns.', assignedTo: 'u3', projectId: 'p3', status: 'InProgress', priority: 'High', deadline: '2026-04-15', createdAt: '2025-12-25' },
  { id: 't11', title: 'Real-time chart components', description: 'Build interactive chart components with live data updates using WebSocket connections.', assignedTo: 'u5', projectId: 'p4', status: 'Pending', priority: 'Medium', deadline: '2026-06-01', createdAt: '2026-01-20' },
  { id: 't12', title: 'Penetration testing report', description: 'Conduct manual penetration testing and document all discovered vulnerabilities with exploitation steps.', assignedTo: 'u2', projectId: 'p5', status: 'Completed', priority: 'High', deadline: '2026-02-28', createdAt: '2025-11-25' },
  { id: 't13', title: 'User feedback survey', description: 'Design and deploy post-onboarding survey to collect user feedback on the new experience.', assignedTo: 'u5', projectId: 'p6', status: 'Pending', priority: 'Low', deadline: '2026-08-01', createdAt: '2026-02-01' },
  { id: 't14', title: 'Responsive layout fixes', description: 'Fix layout issues on tablet and mobile breakpoints across all major pages and components.', assignedTo: 'u3', projectId: 'p1', status: 'Pending', priority: 'Medium', deadline: '2026-03-15', createdAt: '2026-01-25' },
  { id: 't15', title: 'Push notification service', description: 'Implement push notification service with topic subscriptions and delivery tracking.', assignedTo: 'u4', projectId: 'p2', status: 'InProgress', priority: 'High', deadline: '2026-05-01', createdAt: '2025-12-28' },
  { id: 't16', title: 'Rate limiting middleware', description: 'Add rate limiting middleware to all API endpoints with configurable thresholds per route.', assignedTo: 'u1', projectId: 'p3', status: 'Completed', priority: 'Medium', deadline: '2026-02-10', createdAt: '2025-11-15' },
];

export const currentUser = users[0];
