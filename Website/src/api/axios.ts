import axios from 'axios';
import { auth } from '../firebase'; // Import Firebase auth!

const api = axios.create({
  baseURL: 'http://localhost:5221/api',
});

// Attach the freshest Firebase token to every request automatically
api.interceptors.request.use(async (config) => {
  // Check if Firebase currently sees a logged-in user
  const user = auth.currentUser;
  
  if (user) {
    // This is the magic line!
    // getIdToken() automatically checks if the token is expired.
    // If it is, it silently refreshes it behind the scenes before returning it!
    const token = await user.getIdToken();
    config.headers.Authorization = `Bearer ${token}`;
  } else {
    // Fallback just in case Firebase hasn't finished loading yet
    const localToken = localStorage.getItem('token');
    if (localToken) {
      config.headers.Authorization = `Bearer ${localToken}`;
    }
  }

  // Attach client-side cached role so backend can detect immediate role updates
  const localUserStr = localStorage.getItem('user');
  if (localUserStr) {
    try {
      const localUser = JSON.parse(localUserStr);
      if (localUser && localUser.role) {
        config.headers['X-User-Role'] = localUser.role;
      }
    } catch {
      // Ignore
    }
  }

  return config;
});

// (Keep your response interceptor exactly the same as before!)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;