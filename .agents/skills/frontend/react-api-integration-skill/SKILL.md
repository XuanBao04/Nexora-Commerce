---
name: react-api-integration-skill
description: Enforce Axios HTTP client patterns, interceptors, error handling, request/response transformations, and API abstraction layers for e-commerce integrations.
---

## Scope & Activation Rules

Activate when:
- Creating Axios instances with interceptors for authentication and error handling
- Writing API service clients (cart, orders, products, payment)
- Implementing request/response transformations and data normalization
- Handling HTTP errors, retries, and timeout strategies
- Managing JWT tokens in request headers
- Implementing API response validation with TypeScript
- Configuring base URLs for multiple environments

## System Directives

### DO
- **Centralize Axios Configuration**: Create a single, configured Axios instance that all services use.
- **Implement Request Interceptor for JWT**: Automatically inject Bearer token from localStorage/sessionStorage.
- **Implement Response Interceptor for Global Error Handling**: Handle 401 (token expiry), 403 (forbidden), 500 (server errors) uniformly.
- **Validate API Responses**: Parse and validate response structure; throw early on schema violations.
- **Normalize API Responses to DTOs**: Transform backend response shape to frontend-friendly structure.
- **Create Service Classes for Each Domain**: `CartAPI`, `OrderAPI`, `ProductAPI` encapsulate endpoints; never make raw axios calls in components.
- **Implement Exponential Backoff Retry Logic**: For transient failures (network errors, 429 Too Many Requests).
- **Set Reasonable Timeouts**: 10-30 seconds depending on operation; prevent hanging requests.
- **Log API Calls in Development**: Track requests/responses for debugging; exclude sensitive data.
- **Type All API Responses**: Use TypeScript interfaces/types to ensure type safety; use runtime validators like `zod`.
- **Handle Network-Offline Scenarios**: Detect offline and provide user feedback.
- **Use HTTP Status Codes Semantically**: 2xx success, 4xx client errors, 5xx server errors; handle accordingly.

### DO NOT
- Make raw axios calls in components. Always use service layer abstraction.
- Hardcode API URLs in components. Use environment variables and centralized config.
- Inject tokens manually in every request. Use interceptors universally.
- Ignore HTTP error responses. Always check response status and throw on errors.
- Log sensitive data (tokens, passwords, PII). Sanitize logs before commit.
- Create separate Axios instances per request. Use single, shared instance.
- Catch errors and silently swallow them. Always propagate or handle explicitly.
- Skip timeout configuration. Always set reasonable timeouts.
- Use `any` type for API responses. Always define explicit interfaces.
- Implement retry logic at component level. Implement centrally in interceptors.
- Parse response without validation. Always validate structure before use.
- Leave API calls unaborted on component unmount. Always use AbortController.

## Production Reference Implementation

### Centralized Axios Configuration

```typescript
import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { AuthService } from '@services/auth/AuthService';
import type { ApiResponse, ApiError } from '@types/api';

class ApiClient {
  private axiosInstance: AxiosInstance;
  private authService: AuthService;

  constructor(authService: AuthService) {
    this.authService = authService;

    this.axiosInstance = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
      timeout: 15000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request Interceptor: Add JWT Token
    this.axiosInstance.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const token = this.authService.getAccessToken();
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }

        // Add correlation ID for tracing
        config.headers['X-Request-ID'] = this.generateCorrelationId();

        if (import.meta.env.DEV) {
          console.debug(`[API] ${config.method?.toUpperCase()} ${config.url}`, {
            params: config.params,
            data: this.sanitizeData(config.data),
          });
        }

        return config;
      },
      (error: AxiosError) => {
        console.error('[API] Request interceptor error:', error);
        return Promise.reject(error);
      }
    );

    // Response Interceptor: Handle Errors & Token Refresh
    this.axiosInstance.interceptors.response.use(
      (response) => {
        if (import.meta.env.DEV) {
          console.debug(`[API] Response ${response.status}`, response.data);
        }
        return response;
      },
      async (error: AxiosError<ApiError>) => {
        const { response, config } = error;

        if (response?.status === 401) {
          console.warn('[API] Unauthorized - attempting token refresh');

          try {
            const newToken = await this.authService.refreshToken();
            if (config && newToken) {
              config.headers.Authorization = `Bearer ${newToken}`;
              return this.axiosInstance(config);
            }
          } catch (refreshError) {
            console.error('[API] Token refresh failed', refreshError);
            this.authService.logout();
            window.location.href = '/login';
          }
        }

        if (response?.status === 403) {
          console.warn('[API] Forbidden - insufficient permissions');
          throw new PermissionError(response.data?.message || 'Access denied');
        }

        if (response?.status === 404) {
          throw new NotFoundError(response.data?.message || 'Resource not found');
        }

        if (response?.status === 409) {
          throw new ConflictError(response.data?.message || 'Resource conflict');
        }

        if (response?.status === 422) {
          const validationErrors = this.parseValidationErrors(response.data);
          throw new ValidationError('Validation failed', validationErrors);
        }

        if (response?.status && response.status >= 500) {
          console.error('[API] Server error', response.data);
          throw new ServerError(response.data?.message || 'Server error');
        }

        if (!response && error.message === 'Network Error') {
          throw new NetworkError('Network connection failed');
        }

        throw new ApiError(
          response?.data?.message || error.message,
          response?.status || 0,
          response?.data
        );
      }
    );
  }

  private generateCorrelationId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  private sanitizeData(data: any): any {
    if (!data) return null;
    const sanitized = { ...data };
    const sensitiveFields = ['password', 'token', 'secret', 'apiKey', 'creditCard'];
    sensitiveFields.forEach((field) => {
      if (field in sanitized) {
        sanitized[field] = '***REDACTED***';
      }
    });
    return sanitized;
  }

  private parseValidationErrors(data: ApiError): Record<string, string> {
    const errors: Record<string, string> = {};
    if (Array.isArray(data.details)) {
      data.details.forEach((detail: any) => {
        errors[detail.field] = detail.message;
      });
    }
    return errors;
  }

  public getInstance(): AxiosInstance {
    return this.axiosInstance;
  }
}

export default ApiClient;
```

### API Error Classes

```typescript
export class ApiError extends Error {
  constructor(
    message: string,
    public statusCode: number,
    public data?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export class NetworkError extends ApiError {
  constructor(message: string = 'Network error') {
    super(message, 0);
    this.name = 'NetworkError';
  }
}

export class ValidationError extends ApiError {
  constructor(
    message: string,
    public fieldErrors: Record<string, string> = {}
  ) {
    super(message, 422);
    this.name = 'ValidationError';
  }
}

export class NotFoundError extends ApiError {
  constructor(message: string = 'Resource not found') {
    super(message, 404);
    this.name = 'NotFoundError';
  }
}

export class ConflictError extends ApiError {
  constructor(message: string = 'Resource conflict') {
    super(message, 409);
    this.name = 'ConflictError';
  }
}

export class PermissionError extends ApiError {
  constructor(message: string = 'Permission denied') {
    super(message, 403);
    this.name = 'PermissionError';
  }
}

export class ServerError extends ApiError {
  constructor(message: string = 'Server error') {
    super(message, 500);
    this.name = 'ServerError';
  }
}
```

### Cart API Service

```typescript
import { AxiosInstance } from 'axios';
import type {
  Cart,
  CartItem,
  CreateOrderRequest,
  CreateOrderResponse,
} from '@types/index';
import { ApiError } from './ApiErrors';

export class CartAPI {
  constructor(private axiosInstance: AxiosInstance) {}

  async getCart(): Promise<Cart> {
    try {
      const response = await this.axiosInstance.get<Cart>('/cart');
      return this.validateCartResponse(response.data);
    } catch (error) {
      throw this.handleError(error, 'Failed to fetch cart');
    }
  }

  async addItem(cartId: string, request: { productId: string; quantity: number }): Promise<Cart> {
    try {
      const response = await this.axiosInstance.post<Cart>(`/cart/${cartId}/items`, request);
      return this.validateCartResponse(response.data);
    } catch (error) {
      throw this.handleError(error, 'Failed to add item to cart');
    }
  }

  async removeItem(cartId: string, itemId: string): Promise<Cart> {
    try {
      const response = await this.axiosInstance.delete<Cart>(`/cart/${cartId}/items/${itemId}`);
      return this.validateCartResponse(response.data);
    } catch (error) {
      throw this.handleError(error, 'Failed to remove item from cart');
    }
  }

  async updateItem(
    cartId: string,
    itemId: string,
    request: { quantity: number }
  ): Promise<Cart> {
    try {
      const response = await this.axiosInstance.patch<Cart>(
        `/cart/${cartId}/items/${itemId}`,
        request
      );
      return this.validateCartResponse(response.data);
    } catch (error) {
      throw this.handleError(error, 'Failed to update cart item');
    }
  }

  async checkout(request: CreateOrderRequest): Promise<CreateOrderResponse> {
    try {
      const response = await this.axiosInstance.post<CreateOrderResponse>('/orders', request);
      return this.validateOrderResponse(response.data);
    } catch (error) {
      throw this.handleError(error, 'Checkout failed');
    }
  }

  async clearCart(cartId: string): Promise<void> {
    try {
      await this.axiosInstance.delete(`/cart/${cartId}`);
    } catch (error) {
      throw this.handleError(error, 'Failed to clear cart');
    }
  }

  private validateCartResponse(data: any): Cart {
    if (!data || typeof data !== 'object') {
      throw new Error('Invalid cart response structure');
    }

    const { id, userId, items, totalPrice, itemCount } = data;

    if (!id || !userId || !Array.isArray(items)) {
      throw new Error('Missing required cart fields');
    }

    return {
      id,
      userId,
      items: items.map(this.validateCartItem),
      totalPrice: Number(totalPrice),
      itemCount: Number(itemCount),
    };
  }

  private validateCartItem(item: any): CartItem {
    const { id, productId, quantity, price, product } = item;

    if (!id || !productId || !quantity || !price) {
      throw new Error('Invalid cart item structure');
    }

    return {
      id,
      productId,
      quantity: Number(quantity),
      price: Number(price),
      product: product || { id: productId, name: '', price: Number(price) },
    };
  }

  private validateOrderResponse(data: any): CreateOrderResponse {
    const { id, status } = data;

    if (!id || !status) {
      throw new Error('Invalid order response structure');
    }

    return { id, status };
  }

  private handleError(error: any, defaultMessage: string): Error {
    if (error instanceof ApiError) {
      return error;
    }

    if (error?.response?.status === 400) {
      return new Error(`Validation error: ${error.response.data?.message || defaultMessage}`);
    }

    return new Error(error?.message || defaultMessage);
  }
}
```

### Product API Service with Retry Logic

```typescript
import { AxiosInstance } from 'axios';
import type { Product, ProductsPage } from '@types/product';

export class ProductAPI {
  private maxRetries = 3;
  private retryDelay = 1000;

  constructor(private axiosInstance: AxiosInstance) {}

  async getProducts(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'createdAt'
  ): Promise<ProductsPage> {
    return this.retryRequest(() =>
      this.axiosInstance.get<ProductsPage>('/products', {
        params: { page, size, sortBy },
      })
    );
  }

  async getProductById(id: string): Promise<Product> {
    return this.retryRequest(() =>
      this.axiosInstance.get<Product>(`/products/${id}`)
    );
  }

  async searchProducts(keyword: string, page: number = 0, size: number = 20): Promise<ProductsPage> {
    return this.retryRequest(() =>
      this.axiosInstance.get<ProductsPage>('/products/search', {
        params: { keyword, page, size },
      })
    );
  }

  private async retryRequest<T>(
    request: () => Promise<{ data: T }>,
    attempt: number = 0
  ): Promise<T> {
    try {
      const response = await request();
      return response.data;
    } catch (error: any) {
      const isNetworkError =
        !error.response || error.code === 'ECONNABORTED';
      const isServerError = error.response?.status >= 500;
      const shouldRetry = (isNetworkError || isServerError) && attempt < this.maxRetries;

      if (shouldRetry) {
        const delayMs = this.retryDelay * Math.pow(2, attempt);
        console.warn(
          `[API] Retry attempt ${attempt + 1}/${this.maxRetries} after ${delayMs}ms`,
          error.message
        );

        await new Promise((resolve) => setTimeout(resolve, delayMs));
        return this.retryRequest(request, attempt + 1);
      }

      throw error;
    }
  }
}
```

### API Service Factory

```typescript
import { AxiosInstance } from 'axios';
import { CartAPI } from './CartAPI';
import { ProductAPI } from './ProductAPI';
import { OrderAPI } from './OrderAPI';

export class ApiServiceFactory {
  constructor(private axiosInstance: AxiosInstance) {}

  createCartAPI(): CartAPI {
    return new CartAPI(this.axiosInstance);
  }

  createProductAPI(): ProductAPI {
    return new ProductAPI(this.axiosInstance);
  }

  createOrderAPI(): OrderAPI {
    return new OrderAPI(this.axiosInstance);
  }
}

// Export singleton instances
export let apiServiceFactory: ApiServiceFactory;

export const initializeApiServices = (axiosInstance: AxiosInstance) => {
  apiServiceFactory = new ApiServiceFactory(axiosInstance);
};
```

### Usage in Custom Hooks

```typescript
import { useCallback } from 'react';
import { apiServiceFactory } from '@services/api/ApiServiceFactory';
import { useAsync } from './useAsync';

export const useProducts = () => {
  const { data: products, loading, error, execute } = useAsync<Product[]>();
  const productAPI = apiServiceFactory.createProductAPI();

  const fetchProducts = useCallback(
    async (page: number = 0, size: number = 20) => {
      return execute(() => productAPI.getProducts(page, size));
    },
    [execute, productAPI]
  );

  const searchProducts = useCallback(
    async (keyword: string) => {
      return execute(() => productAPI.searchProducts(keyword));
    },
    [execute, productAPI]
  );

  return { products, loading, error, fetchProducts, searchProducts };
};
```

## Anti-Patterns & Automated Fixes

### Anti-Pattern 1: Raw Axios Calls in Components
**Problem**: No centralized error handling; duplicated interceptor logic.
```typescript
// ❌ WRONG
const CartPage = () => {
  const [items, setItems] = useState([]);

  useEffect(() => {
    axios.get('http://localhost:8080/api/cart')
      .then(res => setItems(res.data.items))
      .catch(err => console.log(err)); // Silent failure
  }, []);
};
```
**Fix**: Use service layer with centralized API client.
```typescript
// ✅ CORRECT
const CartPage = () => {
  const { items, loading, error, fetchCart } = useCart();

  useEffect(() => {
    fetchCart();
  }, []);

  if (error) return <ErrorAlert message={error} />;
  return <CartList items={items} />;
};
```

### Anti-Pattern 2: No Token Refresh on 401
**Problem**: Users get logged out instead of silently refreshing tokens.
```typescript
// ❌ WRONG
if (response.status === 401) {
  localStorage.clear();
  window.location.href = '/login';
}
```
**Fix**: Attempt token refresh before logout.
```typescript
// ✅ CORRECT
if (response.status === 401) {
  const newToken = await authService.refreshToken();
  if (newToken) {
    return axiosInstance(config); // Retry original request
  }
  window.location.href = '/login';
}
```

### Anti-Pattern 3: No Response Validation
**Problem**: Invalid or unexpected API response structure breaks component.
```typescript
// ❌ WRONG
const cart = await cartAPI.getCart();
console.log(cart.items[0].price); // May be undefined
```
**Fix**: Validate response structure before use.
```typescript
// ✅ CORRECT
const cart = await cartAPI.getCart();
if (!Array.isArray(cart.items)) throw new Error('Invalid cart structure');
const price = cart.items[0]?.price ?? 0;
```

### Anti-Pattern 4: Hardcoded URLs
**Problem**: Cannot easily switch environments; URLs scattered throughout code.
```typescript
// ❌ WRONG
const url = 'https://api.nexora.com/products'; // Hardcoded
```
**Fix**: Use environment variables and centralized config.
```typescript
// ✅ CORRECT
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const url = `${baseURL}/products`;
```

## Verification Commands

### Test API Client Configuration
```bash
# Build and start frontend dev server
cd frontend && npm run dev

# Test API client with sample request
curl -X GET http://localhost:5173/api/products \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### Verify Interceptor Logic
```bash
# Check network tab in DevTools for:
# - JWT token in Authorization header
# - X-Request-ID correlation ID header
# - Proper error response handling
# - Token refresh on 401
```

### Type Checking
```bash
# Verify all API responses are typed
npx tsc --noEmit

# Check for 'any' types in API layer
grep -r ": any" src/services/api --include="*.ts"
```

### Test Error Handling
```bash
# Test 401 response (simulate token expiry)
npm test -- CartAPI.test.ts

# Test network error retry logic
npm test -- ProductAPI.test.ts
```
