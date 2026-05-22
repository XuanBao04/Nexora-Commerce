import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from "axios";

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

export const refreshSession = async (): Promise<string | null> => {
  try {
    const { data } = await axios.post(`${API_URL}/auth/refresh`, {}, { withCredentials: true });
    const newAccessToken = data.token;
    setAccessToken(newAccessToken);
    return newAccessToken;
  } catch (err) {
    setAccessToken(null);
    if (localStorage.getItem("userId")) {
      localStorage.removeItem("userId");
      localStorage.removeItem("role");
      localStorage.removeItem("username");
      window.location.href = "/login";
    }
    return null;
  }
};

// Interceptor to attach the access token to requests
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// Variables to handle concurrent requests when refreshing token
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Interceptor to handle 401 Unauthorized and auto-refresh token
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/login') &&
      !originalRequest.url?.includes('/auth/refresh')
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
      isRefreshing = true;

      try {
        const { data } = await axios.post(`${API_URL}/auth/refresh`, {}, { withCredentials: true });
        
        const newAccessToken = data.token;
        setAccessToken(newAccessToken);
        
        processQueue(null, newAccessToken);
        
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);
      } catch (err) {
        processQueue(err, null);
        setAccessToken(null);
        
        // Clear local storage and redirect to login
        localStorage.removeItem("userId");
        localStorage.removeItem("role");
        localStorage.removeItem("username");
        window.location.href = "/login";
        
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
