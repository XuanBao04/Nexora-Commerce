import { create } from 'zustand';
import apiClient, { refreshSession, setAccessToken } from "@/services/api/apiClient";
import { loginService } from "@features/auth/services/loginService";
import { registerService } from "@features/auth/services/registerService";
import { AuthResponse, RegisterRequest } from "@features/auth/types";

interface AuthState {
  user: AuthResponse | null;
  role: string | null;
  username: string | null;
  userId: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  
  // Actions
  initializeAuth: () => Promise<void>;
  login: (username: string, password: string) => Promise<AuthResponse>;
  register: (request: RegisterRequest) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  role: localStorage.getItem('role'),
  username: localStorage.getItem('username'),
  userId: localStorage.getItem('userId'),
  isAuthenticated: !!localStorage.getItem('userId'),
  isLoading: true, // Initially true for session restoration

  initializeAuth: async () => {
    try {
      const hasPersistedSession = !!localStorage.getItem("userId");
      if (!hasPersistedSession) {
        set({ user: null, isAuthenticated: false, role: null, username: null, userId: null });
        return;
      }

      const token = await refreshSession();
      if (token) {
        const userId = localStorage.getItem("userId") || "";
        const username = localStorage.getItem("username") || "";
        const role = localStorage.getItem("role") || "";
        const email = localStorage.getItem("email") || "";
        const fullName = localStorage.getItem("fullName") || "";
        
        if (userId && username) {
          const authData: AuthResponse = {
            userId,
            username,
            role,
            email,
            fullName,
            token,
            message: "Session restored"
          };
          set({
            user: authData,
            isAuthenticated: true,
            role,
            username,
            userId,
          });
        }
      } else {
        // Clear stale local state if refresh fails
        localStorage.removeItem("userId");
        localStorage.removeItem("username");
        localStorage.removeItem("role");
        localStorage.removeItem("email");
        localStorage.removeItem("fullName");
        set({ user: null, isAuthenticated: false, role: null, username: null, userId: null });
      }
    } catch (err) {
      console.error("Failed to restore session:", err);
      localStorage.removeItem("userId");
      localStorage.removeItem("username");
      localStorage.removeItem("role");
      localStorage.removeItem("email");
      localStorage.removeItem("fullName");
      set({ user: null, isAuthenticated: false, role: null, username: null, userId: null });
    } finally {
      set({ isLoading: false });
    }
  },

  login: async (username: string, password: string): Promise<AuthResponse> => {
    try {
      const authData = await loginService(username, password);
      
      localStorage.setItem("userId", authData.userId);
      localStorage.setItem("username", authData.username);
      localStorage.setItem("role", authData.role);
      localStorage.setItem("email", authData.email);
      localStorage.setItem("fullName", authData.fullName);
      
      set({
        user: authData,
        isAuthenticated: true,
        userId: authData.userId,
        username: authData.username,
        role: authData.role
      });
      
      return authData;
    } catch (error) {
      set({ user: null, isAuthenticated: false });
      throw error;
    }
  },

  register: async (request: RegisterRequest): Promise<AuthResponse> => {
    try {
      const authData = await registerService(request);
      
      localStorage.setItem("userId", authData.userId);
      localStorage.setItem("username", authData.username);
      localStorage.setItem("role", authData.role);
      localStorage.setItem("email", authData.email);
      localStorage.setItem("fullName", authData.fullName);
      
      set({
        user: authData,
        isAuthenticated: true,
        userId: authData.userId,
        username: authData.username,
        role: authData.role
      });
      
      return authData;
    } catch (error) {
      set({ user: null, isAuthenticated: false });
      throw error;
    }
  },

  logout: async (): Promise<void> => {
    try {
      await apiClient.delete("/v1/authentications/sessions");
    } catch (err) {
      console.warn("Server logout request failed:", err);
    } finally {
      setAccessToken(null);
      
      localStorage.removeItem("userId");
      localStorage.removeItem("username");
      localStorage.removeItem("role");
      localStorage.removeItem("email");
      localStorage.removeItem("fullName");
      
      set({
        user: null,
        isAuthenticated: false,
        userId: null,
        username: null,
        role: null,
      });
      
      window.location.href = "/login";
    }
  },

  clearAuth: () => {
    localStorage.removeItem("userId");
    localStorage.removeItem("username");
    localStorage.removeItem("role");
    localStorage.removeItem("email");
    localStorage.removeItem("fullName");
    set({
      user: null,
      isAuthenticated: false,
      userId: null,
      username: null,
      role: null,
    });
  }
}));
