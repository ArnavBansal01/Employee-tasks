import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Eye, EyeOff, Moon, Sun } from "lucide-react";
import { useTheme } from "../context/ThemeContext";
import { useAuth } from "../context/AuthContext";
import api from "../api/axios";

import { signInWithEmailAndPassword } from "firebase/auth";
import { auth } from "../firebase";

export function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const { dark, toggle } = useTheme();
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

   try {
      // STEP 1: Let Firebase verify the password and give us the token
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      const firebaseToken = await userCredential.user.getIdToken();

      // STEP 2: Send the token to .NET to sync the database
      const syncResponse = await api.post("/auth/firebase-sync", {}, {
        headers: {
          Authorization: `Bearer ${firebaseToken}` // Pass it manually this first time
        }
      });

      // FIX: Ensure we are accessing the data correctly. 
      // If your backend returns { message: "...", userId: 1 }, access it via .data
      const backendData = syncResponse.data;
      // We will default to 'undefined' to trigger a controlled error if it's missing, rather than a crash.
      const userId = backendData?.userId || backendData?.user?.id;

      if (!userId) {
          throw new Error("Could not retrieve user ID from backend sync.");
      }

      // STEP 3: Fetch the full user profile
      const userResponse = await api.get(`/users/${userId}`, {
        headers: {
          Authorization: `Bearer ${firebaseToken}`
        }
      });

      // STEP 4: Store the token and full user profile in your context, then redirect
      login(firebaseToken, userResponse.data);
      navigate("/dashboard");
      
    } catch (err: any) {
      console.error("Login pipeline failed:", err);
      
      // Catch specific Firebase errors (wrong password, user not found, etc.)
      if (err.code === 'auth/invalid-credential' || err.code === 'auth/user-not-found' || err.code === 'auth/wrong-password') {
        setError("Invalid email or password.");
      } 
      else {
        // FIX: Safely extract the error message from the complex .NET error object
        const errorMessage = 
            err.response?.data?.message || 
            err.response?.data?.title || // Catches .NET ValidationProblemDetails
            (typeof err.response?.data === 'string' ? err.response.data : null) || 
            err.message || 
            "An error occurred during login.";
            
        setError(errorMessage);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4 dark:bg-navy-900">
      <button
        onClick={toggle}
        className="fixed right-6 top-6 flex h-10 w-10 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-200 hover:text-slate-600 dark:hover:bg-slate-700 dark:hover:text-slate-300"
      >
        {dark ? <Sun size={20} /> : <Moon size={20} />}
      </button>

      <div className="card w-full max-w-md p-8">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-blue-500">
            <span className="text-xl font-bold text-white">TT</span>
          </div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
            Welcome back
          </h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Sign in to your Task Tracker account
          </p>
        </div>

        <form onSubmit={handleLogin} className="space-y-5">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@company.com"
              className="input"
              required
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
              Password
            </label>
            <div className="relative">
              <input
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
                className="input pr-12"
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                className="absolute inset-y-0 right-0 flex items-center px-3 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          {error && <p className="text-sm text-red-500">{error}</p>}

          <button
            type="submit"
            className="btn-primary w-full"
            disabled={loading}
          >
            {loading ? "Signing in..." : "Sign In"}
          </button>
        </form>
      </div>
    </div>
  );
}