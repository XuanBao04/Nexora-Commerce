import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from "axios";
import { ApiResponseAuthResponse } from "@/features/auth/types";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

const apiClient: AxiosInstance = axios.create({
  baseURL: API_URL,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true, // Crucial for sending/receiving HttpOnly cookies (like refresh token)
});

// In-memory access token storage
let accessToken: string | null = null;

export const setAccessToken = (token: string | null) => {
  accessToken = token;
};

export const getAccessToken = () => accessToken;

// Variables to handle concurrent requests when refreshing token
let activeRefreshPromise: Promise<string | null> | null = null;
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

export const refreshSession = async (): Promise<string | null> => {
  if (activeRefreshPromise) {
    return activeRefreshPromise;
  }

  isRefreshing = true;
  activeRefreshPromise = (async () => {
    try {
      const { data } = await axios.post<ApiResponseAuthResponse>(
        `${API_URL}/v1/authentications/tokens`,
        {},
        { withCredentials: true }
      );
      const newAccessToken = data.data.token;
      setAccessToken(newAccessToken);
      processQueue(null, newAccessToken);
      return newAccessToken;
    } catch (err) {
      setAccessToken(null);
      processQueue(err, null);
      
      // Only wipe session and redirect to login if it is a genuine auth error (400 or 401)
      const isAuthError =
        axios.isAxiosError(err) &&
        err.response &&
        (err.response.status === 400 || err.response.status === 401);
      
      if (isAuthError && localStorage.getItem("userId")) {
        localStorage.removeItem("userId");
        localStorage.removeItem("role");
        localStorage.removeItem("username");
        localStorage.removeItem("email");
        localStorage.removeItem("fullName");
        window.location.href = "/login";
      }
      return null;
    } finally {
      activeRefreshPromise = null;
      isRefreshing = false;
    }
  })();

  return activeRefreshPromise;
};

// Interceptor to attach the access token to requests
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (config.url?.includes('/v1/authentications/sessions')) {
      return config;
    }
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// Interceptor to handle 401 Unauthorized and auto-refresh token
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Explicitly reject session endpoint failures immediately without side effects
    if (originalRequest?.url?.includes('/v1/authentications/sessions')) {
      return Promise.reject(error);
    }

    // Infinite HTTP 401 Loop Guard: If the refresh token request itself fails with 401
    if (error.response?.status === 401 && originalRequest?.url?.includes('/v1/authentications/tokens')) {
      processQueue(error, null);
      setAccessToken(null);
      localStorage.removeItem("userId");
      localStorage.removeItem("role");
      localStorage.removeItem("username");
      localStorage.removeItem("email");
      localStorage.removeItem("fullName");
      window.location.href = "/login";
      return Promise.reject(error);
    }

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/v1/authentications/sessions') &&
      !originalRequest.url?.includes('/v1/authentications/tokens')
    ) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return apiClient(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;

      try {
        const newAccessToken = await refreshSession();
        if (newAccessToken) {
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          return apiClient(originalRequest);
        } else {
          const err = new Error("Session refresh failed");
          return Promise.reject(err);
        }
      } catch (err) {
        return Promise.reject(err);
      }
    }

    return Promise.reject(error);
  }
);


export default apiClient;
