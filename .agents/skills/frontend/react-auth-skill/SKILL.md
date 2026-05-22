---
name: react-auth-skill
description: Enforce JWT-based authentication flow, protected routes, auto-logout on token expiry, secure token storage, and session management in React applications.
---

## Scope & Activation Rules

Activate when:
- Implementing authentication context and providers
- Creating protected route components
- Managing JWT token lifecycle (storage, refresh, expiry)
- Implementing auto-logout on 401 responses
- Handling login/logout/signup flows
- Persisting authentication state across browser refreshes
- Validating user permissions before rendering components

## System Directives

### DO
- **Use Context API for Auth State**: Centralize authentication state (user, token, loading, isAuthenticated) in a single provider.
- **Store JWT in Memory + Refresh Token in Secure Storage**: Keep access token in memory (survives page refresh if refresh token available); store refresh token in httpOnly cookie or sessionStorage.
- **Implement Protected Routes**: Check authentication status before rendering; redirect to login if unauthorized.
- **Auto-Refresh Tokens**: On app mount, check if refresh token is valid and use it to get new access token before user navigates.
- **Handle Token Expiry Gracefully**: On 401 response, attempt refresh; if refresh fails, clear auth state and redirect to login.
- **Validate JWT Structure**: Decode and validate JWT claims (expiry, roles); don't trust frontend validation alone.
- **Implement Logout on All Tabs**: Use `StorageEvent` or `BroadcastChannel` to sync logout across browser tabs.
- **Clear Sensitive Data on Logout**: Remove all tokens, user data, and auth state; don't leave traces in memory.
- **Validate Permissions Before Rendering**: Use role-based checks (`@RequireRole`, custom hooks) to conditionally render admin/user features.
- **Set Token Expiry Reminders**: Warn users before access token expires; offer to refresh.
- **Use Secure Storage**: Never store tokens in localStorage if you can use sessionStorage (cleared on tab close); consider httpOnly cookies for production.
- **Implement Silent Refresh**: Refresh tokens proactively before expiry, not reactively on 401.

### DO NOT
- Store access tokens in localStorage permanently. Use sessionStorage or memory with refresh token rotation.
- Store passwords, tokens, or sensitive data in plain text. Always hash passwords backend-side.
- Skip token validation. Always check expiry and signature when present.
- Implement role-based access entirely on frontend. Frontend UI hiding is for UX; backend enforces actual permissions.
- Leave authentication state undefined on app load. Initialize with refresh token check.
- Ignore 401 responses and let user navigate. Auto-logout and redirect to login.
- Use hardcoded token values in code or tests. Generate dynamically.
- Skip HTTPS in production. Always use HTTPS for authentication.
- Implement auth without error boundaries. Catch and handle auth errors gracefully.
- Use insecure storage methods (window.credentials, embedded in cookies without httpOnly).
- Forget to decode JWT. Always decode to check expiry and claims.
- Create infinite refresh loops. Implement max refresh attempts and final logout.

## Production Reference Implementation

### Auth Context & Provider

```typescript
import React, { createContext, useCallback, useEffect, useState, ReactNode } from 'react';
import { jwtDecode } from 'jwt-decode';
import { authAPI } from '@services/api/authAPI';
import type { User, AuthTokenResponse } from '@types/index';

export interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  register: (email: string, password: string, firstName: string, lastName: string) => Promise<void>;
  refreshToken: () => Promise<boolean>;
  clearError: () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);

interface JWTPayload {
  userId: string;
  email: string;
  roles: string[];
  exp: number;
}

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [tokenExpiry, setTokenExpiry] = useState<number | null>(null);
  const refreshTokenRef = React.useRef<string | null>(null);

  // Initialize auth on app mount
  useEffect(() => {
    initializeAuth();
    
    // Sync logout across tabs
    const handleStorageChange = (event: StorageEvent) => {
      if (event.key === 'auth:logout' && event.newValue === 'true') {
        logout();
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  // Auto-refresh token before expiry
  useEffect(() => {
    if (!tokenExpiry || !accessToken) return;

    const now = Date.now() / 1000;
    const timeUntilExpiry = tokenExpiry - now;
    const refreshAt = timeUntilExpiry - 60; // Refresh 1 minute before expiry

    if (refreshAt <= 0) {
      refreshToken();
      return;
    }

    const timer = setTimeout(() => {
      refreshToken();
    }, refreshAt * 1000);

    return () => clearTimeout(timer);
  }, [tokenExpiry, accessToken]);

  const initializeAuth = async () => {
    try {
      setIsLoading(true);

      // Check for refresh token in sessionStorage
      const storedRefreshToken = sessionStorage.getItem('auth:refreshToken');
      refreshTokenRef.current = storedRefreshToken;

      if (storedRefreshToken) {
        const success = await refreshToken();
        if (!success) {
          // Refresh token expired; require re-login
          clearAuth();
        }
      }
    } catch (err) {
      console.error('Auth initialization failed:', err);
      clearAuth();
    } finally {
      setIsLoading(false);
    }
  };

  const setTokens = (response: AuthTokenResponse) => {
    setAccessToken(response.accessToken);
    refreshTokenRef.current = response.refreshToken;

    // Store refresh token securely
    sessionStorage.setItem('auth:refreshToken', response.refreshToken);

    // Decode and set expiry
    try {
      const decoded = jwtDecode<JWTPayload>(response.accessToken);
      setTokenExpiry(decoded.exp);

      // Extract user info from token
      setUser({
        id: decoded.userId,
        email: decoded.email,
        firstName: response.email.split('@')[0], // Parse from email or response
        lastName: '',
        roles: decoded.roles,
        createdAt: new Date(),
      });
    } catch (err) {
      console.error('Failed to decode token:', err);
      throw new Error('Invalid token format');
    }
  };

  const clearAuth = () => {
    setAccessToken(null);
    setUser(null);
    setTokenExpiry(null);
    refreshTokenRef.current = null;
    sessionStorage.removeItem('auth:refreshToken');
  };

  const login = async (email: string, password: string) => {
    try {
      setIsLoading(true);
      setError(null);

      const response = await authAPI.login({ email, password });
      setTokens(response);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Login failed';
      setError(message);
      clearAuth();
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    try {
      if (accessToken) {
        await authAPI.logout();
      }
    } catch (err) {
      console.warn('Logout API call failed (proceeding with local cleanup):', err);
    } finally {
      clearAuth();
      localStorage.setItem('auth:logout', 'true');
      setTimeout(() => localStorage.removeItem('auth:logout'), 100);
    }
  };

  const register = async (
    email: string,
    password: string,
    firstName: string,
    lastName: string
  ) => {
    try {
      setIsLoading(true);
      setError(null);

      const response = await authAPI.register({ email, password, firstName, lastName });
      // After registration, auto-login
      await login(email, password);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Registration failed';
      setError(message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const refreshToken = async (): Promise<boolean> => {
    try {
      if (!refreshTokenRef.current) {
        return false;
      }

      const response = await authAPI.refreshToken(refreshTokenRef.current);
      setTokens(response);
      return true;
    } catch (err) {
      console.warn('Token refresh failed:', err);
      clearAuth();
      return false;
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        error,
        login,
        logout,
        register,
        refreshToken,
        clearError: () => setError(null),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = React.useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
```

### Protected Route Component

```typescript
import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '@context/AuthContext';

interface ProtectedRouteProps {
  element: React.ReactElement;
  requiredRoles?: string[];
  fallback?: React.ReactElement;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  element,
  requiredRoles,
  fallback,
}) => {
  const { isAuthenticated, isLoading, user } = useAuth();

  if (isLoading) {
    return <div className="flex items-center justify-center min-h-screen">Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRoles && requiredRoles.length > 0) {
    const hasRequiredRole = user?.roles.some((role) =>
      requiredRoles.includes(role.toUpperCase())
    );

    if (!hasRequiredRole) {
      return fallback || <Navigate to="/unauthorized" replace />;
    }
  }

  return element;
};
```

### Auth API Service

```typescript
import { AxiosInstance } from 'axios';
import type { AuthTokenResponse } from '@types/index';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export class AuthAPI {
  constructor(private axiosInstance: AxiosInstance) {}

  async login(request: LoginRequest): Promise<AuthTokenResponse> {
    try {
      const response = await this.axiosInstance.post<AuthTokenResponse>('/auth/login', request);
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 401) {
        throw new Error('Invalid email or password');
      }
      throw error;
    }
  }

  async register(request: RegisterRequest): Promise<AuthTokenResponse> {
    try {
      const response = await this.axiosInstance.post<AuthTokenResponse>('/auth/register', request);
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 409) {
        throw new Error('Email already registered');
      }
      throw error;
    }
  }

  async logout(): Promise<void> {
    try {
      await this.axiosInstance.post('/auth/logout');
    } catch (error) {
      console.warn('Logout request failed:', error);
      // Don't throw; local logout should proceed regardless
    }
  }

  async refreshToken(refreshToken: string): Promise<AuthTokenResponse> {
    try {
      const response = await this.axiosInstance.post<AuthTokenResponse>(
        '/auth/refresh',
        {},
        {
          headers: {
            Authorization: `Bearer ${refreshToken}`,
          },
        }
      );
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 401) {
        throw new Error('Refresh token expired');
      }
      throw error;
    }
  }
}

export const createAuthAPI = (axiosInstance: AxiosInstance) => {
  return new AuthAPI(axiosInstance);
};
```

### Login Page Component

```typescript
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@context/AuthContext';
import { Form } from '@components/Form';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const { login, isLoading, error } = useAuth();
  const [validationError, setValidationError] = useState<string | null>(null);

  const handleSubmit = async (values: Record<string, string>) => {
    try {
      setValidationError(null);

      if (!values.email || !values.password) {
        setValidationError('Email and password are required');
        return;
      }

      await login(values.email, values.password);
      navigate('/dashboard', { replace: true });
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Login failed';
      setValidationError(message);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="max-w-md w-full bg-white rounded-lg shadow-md p-8">
        <h2 className="text-3xl font-bold text-center mb-6">Sign In</h2>

        {validationError && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded">
            {validationError}
          </div>
        )}

        <Form
          onSubmit={handleSubmit}
          initialValues={{ email: '', password: '' }}
        >
          <Form.Input
            name="email"
            label="Email"
            type="email"
            placeholder="your@email.com"
            required
          />

          <Form.Input
            name="password"
            label="Password"
            type="password"
            placeholder="Enter your password"
            required
          />

          <Form.Submit label={isLoading ? 'Signing in...' : 'Sign In'} />
        </Form>

        <p className="mt-4 text-center text-gray-600">
          Don't have an account?{' '}
          <a href="/register" className="text-blue-600 hover:underline">
            Register here
          </a>
        </p>
      </div>
    </div>
  );
};
```

### App Router Setup

```typescript
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from '@context/AuthContext';
import { ProtectedRoute } from '@components/ProtectedRoute';
import { ErrorBoundary } from '@components/ErrorBoundary';

import { LoginPage } from '@pages/login/LoginPage';
import { RegisterPage } from '@pages/register/RegisterPage';
import { CartPage } from '@features/cart/CartPage';
import { OrdersPage } from '@features/orders/OrdersPage';
import { AdminPage } from '@pages/admin/AdminPage';
import { UnauthorizedPage } from '@pages/errors/UnauthorizedPage';
import { NotFoundPage } from '@pages/errors/NotFoundPage';

export const AppRouter: React.FC = () => {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ErrorBoundary>
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            {/* Protected Routes - USER role */}
            <Route
              path="/cart"
              element={<ProtectedRoute element={<CartPage />} requiredRoles={['USER']} />}
            />
            <Route
              path="/orders"
              element={<ProtectedRoute element={<OrdersPage />} requiredRoles={['USER']} />}
            />

            {/* Protected Routes - ADMIN role */}
            <Route
              path="/admin"
              element={<ProtectedRoute element={<AdminPage />} requiredRoles={['ADMIN']} />}
            />

            {/* Error Routes */}
            <Route path="/unauthorized" element={<UnauthorizedPage />} />
            <Route path="*" element={<NotFoundPage />} />

            {/* Redirect root to cart or login */}
            <Route path="/" element={<Navigate to="/cart" replace />} />
          </Routes>
        </ErrorBoundary>
      </AuthProvider>
    </BrowserRouter>
  );
};
```

## Anti-Patterns & Automated Fixes

### Anti-Pattern 1: Storing Access Token in localStorage
**Problem**: Token persists after browser close; exposed to XSS attacks.
```typescript
// ❌ WRONG
localStorage.setItem('accessToken', token);
const token = localStorage.getItem('accessToken');
```
**Fix**: Keep access token in memory; store refresh token in sessionStorage.
```typescript
// ✅ CORRECT
sessionStorage.setItem('refreshToken', response.refreshToken);
const token = accessTokenRef.current; // In-memory reference
```

### Anti-Pattern 2: No Token Expiry Check
**Problem**: User navigates with expired token; gets 401 mid-action.
```typescript
// ❌ WRONG
const makeRequest = () => {
  const token = localStorage.getItem('token');
  return axiosInstance.get('/cart', {
    headers: { Authorization: `Bearer ${token}` }
  });
};
```
**Fix**: Check expiry before use and refresh if needed.
```typescript
// ✅ CORRECT
const getValidToken = async () => {
  const decoded = jwtDecode(accessToken);
  if (decoded.exp < Date.now() / 1000) {
    await refreshToken();
  }
  return accessToken;
};
```

### Anti-Pattern 3: Unhandled 401 Responses
**Problem**: User gets stuck at 401 without redirect to login.
```typescript
// ❌ WRONG
if (error.response?.status === 401) {
  console.error('Unauthorized');
  // No action taken
}
```
**Fix**: Auto-attempt refresh, then logout and redirect.
```typescript
// ✅ CORRECT
if (error.response?.status === 401) {
  const refreshed = await authService.refreshToken();
  if (!refreshed) {
    authService.logout();
    window.location.href = '/login';
  }
}
```

### Anti-Pattern 4: No Auth State Initialization on Page Refresh
**Problem**: User refreshes page; auth state is lost.
```typescript
// ❌ WRONG
const [user, setUser] = useState(null); // Loses state on refresh
```
**Fix**: Initialize from refresh token on mount.
```typescript
// ✅ CORRECT
useEffect(() => {
  const refreshToken = sessionStorage.getItem('refreshToken');
  if (refreshToken) {
    authService.refreshToken(refreshToken).then(setUser);
  }
}, []);
```

## Verification Commands

### Test Auth Flow
```bash
# Start frontend dev server
cd frontend && npm run dev

# Test login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'

# Check JWT token structure
JWT_TOKEN="<token_from_login_response>"
echo $JWT_TOKEN | jq -R 'split(".") | .[1] | @base64d | fromjson'

# Test protected route (should include Authorization header)
curl -X GET http://localhost:8080/api/cart \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### Verify Token Refresh
```bash
# Manually expire access token in DevTools Console
sessionStorage.removeItem('auth:refreshToken');

# Try to access protected route
# Should be redirected to /login
```

### Check Auth Context State
```bash
# In browser DevTools Console
localStorage.setItem('debugAuth', 'true');

# Check AuthContext values
console.log(window.__AUTH_STATE__);
```

### Test Logout Sync Across Tabs
```bash
# Open two browser tabs with the app
# In tab 1, click logout
# Tab 2 should also logout automatically (verify via StorageEvent listener)
```
