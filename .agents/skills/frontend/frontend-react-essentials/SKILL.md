---
name: frontend-react-essentials
description: React 18+ core patterns for Phase 5 - hooks, API integration, component composition, and state management for Nexora Commerce.
---

## Scope & Activation Rules

Activate when:
- Creating React components with hooks (useState, useEffect, useContext)
- Integrating with REST APIs (cart, products, orders)
- Managing global state (authentication, cart, user preferences)
- Implementing forms, loading states, and error handling
- Building feature modules (products, cart, checkout, orders)

## System Directives

### React Patterns
- **Functional Components Only**: No class components; always use hooks
- **Custom Hooks for Logic**: Extract reusable logic into `useCart()`, `useProducts()`, `useAsync()` hooks
- **Props Typing**: Always use TypeScript interfaces for props; no `any` types
- **Effect Cleanup**: Always return cleanup functions from `useEffect` for subscriptions
- **Memoization When Needed**: Selective Memoization: Use React.memo() only for heavy rendering tree nodes or components with frequent reference prop mutations. Never use it globally.
- **Context for Global State**: Auth state + Cart in Context; use for cross-component data only
- **Controlled Components**: All form inputs must be controlled via state
- **Error Boundaries**: Wrap feature modules to catch rendering errors
- **Loading States**: Every async operation needs loading + error + success states

### Anti-Patterns to Avoid
- No class components
- No `any` types in TypeScript
- No fetching data in render (always in `useEffect`)
- No skipping cleanup functions in `useEffect`
- No prop drilling more than 2 levels (use Context instead)
- No creating new objects/functions in render without memoization
- No missing `key` props in lists
- No hardcoded API URLs (use environment variables)

## Configuration

### Environment
```bash
# .env.local
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=Nexora Commerce
```

### TypeScript Setup
```typescript
// types/api.ts
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  pagination?: {
    currentPage: number;
    totalPages: number;
    totalElements: number;
  };
}

export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  images: string[];
}

export interface ProductVariant {
  id: string;
  sku: string;
  productId: string;
  attributes: Record<string, string>;
  price: number;
  stock: number;
}

export interface CartItem {
  variantId: string;
  quantity: number;
  product: Product;
  variant: ProductVariant;
}

export interface Order {
  id: string;
  status: 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  totalAmount: number;
  items: CartItem[];
  createdAt: string;
}

export interface User {
  id: string;
  email: string;
  name: string;
  role: 'USER' | 'ADMIN';
}
```

## API Service Layer

```typescript
// services/api/baseClient.ts
import axios, { AxiosInstance, AxiosError } from 'axios';

const createApiClient = (): AxiosInstance => {
  const client = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  // Request interceptor: add JWT token
  client.interceptors.request.use((config) => {
    const token = localStorage.getItem('access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  // Response interceptor: handle 401 (token expiry) and clean response data unwrap
  client.interceptors.response.use(
    (response) => response.data as any, // Returns direct ApiResponse instead of full AxiosResponse
    (error: AxiosError) => {
      if (error.response?.status === 401) {
        localStorage.removeItem('access_token');
        window.location.href = '/login';
      }
      
      // Extract detailed error information from backend payload
      const errorData = error.response?.data as any;
      const errorMessage = errorData?.message || error.message || 'An unknown error occurred';
      return Promise.reject(new Error(errorMessage));
    }
  );

  return client;
};

export const apiClient = createApiClient();

// services/api/productApi.ts
import { ApiResponse, Product } from '../../types/api';

export const productApi = {
  getProducts: async (page = 0, limit = 20): Promise<ApiResponse<Product[]>> => {
    return apiClient.get('/products', {
      params: { page, limit },
    }) as unknown as Promise<ApiResponse<Product[]>>;
  },

  getProductById: async (id: string): Promise<ApiResponse<Product>> => {
    return apiClient.get(`/products/${id}`) as unknown as Promise<ApiResponse<Product>>;
  },

  search: async (query: string): Promise<ApiResponse<Product[]>> => {
    return apiClient.get('/ai/search', {
      params: { q: query },
    }) as unknown as Promise<ApiResponse<Product[]>>;
  },
};

// services/api/cartApi.ts
import { ApiResponse, CartItem } from '../../types/api';

export const cartApi = {
  getCart: async (): Promise<ApiResponse<CartItem[]>> => {
    return apiClient.get('/cart') as unknown as Promise<ApiResponse<CartItem[]>>;
  },

  addItem: async (variantId: string, quantity: number): Promise<ApiResponse<CartItem[]>> => {
    return apiClient.post('/cart/items', {
      variantId,
      quantity,
    }) as unknown as Promise<ApiResponse<CartItem[]>>;
  },

  removeItem: async (itemId: string): Promise<ApiResponse<CartItem[]>> => {
    return apiClient.delete(`/cart/items/${itemId}`) as unknown as Promise<ApiResponse<CartItem[]>>;
  },

  checkout: async (paymentMethod: string): Promise<ApiResponse<{ orderId: string; paymentUrl?: string }>> => {
    return apiClient.post('/checkout', { paymentMethod }) as unknown as Promise<ApiResponse<{ orderId: string; paymentUrl?: string }>>;
  },
};

// services/api/orderApi.ts
import { ApiResponse, Order } from '../../types/api';

export const orderApi = {
  getOrders: async (page = 0): Promise<ApiResponse<Order[]>> => {
    return apiClient.get('/orders', { params: { page } }) as unknown as Promise<ApiResponse<Order[]>>;
  },

  getOrderById: async (id: string): Promise<ApiResponse<Order>> => {
    return apiClient.get(`/orders/${id}`) as unknown as Promise<ApiResponse<Order>>;
  },

  cancelOrder: async (id: string): Promise<ApiResponse<Order>> => {
    return apiClient.put(`/orders/${id}/cancel`) as unknown as Promise<ApiResponse<Order>>;
  },
};

// services/api/aiApi.ts
import { ApiResponse, Product } from '../../types/api';

export const aiApi = {
  chat: async (message: string, sessionId: string): Promise<ApiResponse<{ response: string }>> => {
    return apiClient.post('/ai/chat', {
      message,
      sessionId,
    }) as unknown as Promise<ApiResponse<{ response: string }>>;
  },
};
```

## Core Hooks

```typescript
// hooks/useAsync.ts
import { useState, useCallback } from 'react';

export const useAsync = <T,>() => {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const execute = useCallback(async (fn: () => Promise<T>) => {
    setLoading(true);
    setError(null);
    try {
      const result = await fn();
      setData(result);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unknown error';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return { data, loading, error, execute };
};

// hooks/useCart.ts
import { useState, useEffect, useCallback } from 'react';
import { cartApi } from '../services/api/cartApi';
import { CartItem } from '../types/api';
import { useAsync } from './useAsync';

export const useCart = () => {
  const [cart, setCart] = useState<CartItem[]>([]);
  const { loading, execute } = useAsync<CartItem[]>();

  useEffect(() => {
    execute(async () => {
      const res = await cartApi.getCart();
      return res.data; // Unwrap the data field from ApiResponse
    })
      .then(setCart)
      .catch(() => setCart([]));
  }, [execute]);

  const addItem = useCallback(
    async (variantId: string, quantity: number) => {
      const updatedList = await execute(async () => {
        const res = await cartApi.addItem(variantId, quantity);
        return res.data; // Unwrap the data field from ApiResponse
      });
      setCart(updatedList || []);
    },
    [execute]
  );

  const removeItem = useCallback(
    async (itemId: string) => {
      const updatedList = await execute(async () => {
        const res = await cartApi.removeItem(itemId);
        return res.data; // Unwrap the data field from ApiResponse
      });
      setCart(updatedList || []);
    },
    [execute]
  );

  const checkout = useCallback(async (paymentMethod: string) => {
    const result = await execute(async () => {
      const res = await cartApi.checkout(paymentMethod);
      return res.data as any; // Unwrap the data field from ApiResponse
    });
    setCart([]);
    return result;
  }, [execute]);

  return { cart, loading, addItem, removeItem, checkout };
};

// hooks/useProducts.ts
import { useState, useCallback } from 'react';
import { productApi } from '../services/api/productApi';
import { Product } from '../types/api';
import { useAsync } from './useAsync';

export const useProducts = () => {
  const { data: products, loading, execute } = useAsync<Product[]>();
  const [page, setPage] = useState(0);

  const fetchProducts = useCallback(async (pageNum = 0) => {
    const dataList = await execute(async () => {
      const res = await productApi.getProducts(pageNum, 20);
      return res.data; // Unwrap the data field from ApiResponse
    });
    setPage(pageNum);
    return dataList;
  }, [execute]);

  return { products, loading, page, fetchProducts };
};

// hooks/useDebounce.ts
import { useState, useEffect } from 'react';

export const useDebounce = <T,>(value: T, delay = 500): T => {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
};
```

## Component Examples

```typescript
// components/ProductGrid.tsx
interface ProductGridProps {
  products: Product[];
  loading: boolean;
  onAddToCart: (productId: string) => void;
}

export const ProductGrid: React.FC<ProductGridProps> = React.memo(
  ({ products, loading, onAddToCart }) => {
    if (loading) return <div>Loading...</div>;
    if (!products?.length) return <div>No products found</div>;

    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {products.map((product) => (
          <ProductCard
            key={product.id}
            product={product}
            onAddToCart={onAddToCart}
          />
        ))}
      </div>
    );
  }
);

// components/ProductCard.tsx
interface ProductCardProps {
  product: Product;
  onAddToCart: (productId: string) => void;
}

export const ProductCard: React.FC<ProductCardProps> = React.memo(
  ({ product, onAddToCart }) => (
    <div className="border rounded-lg overflow-hidden hover:shadow-lg transition">
      <img
        src={product.images[0]}
        alt={product.name}
        className="w-full h-48 object-cover"
        loading="lazy"
      />
      <div className="p-4">
        <h3 className="font-semibold line-clamp-2">{product.name}</h3>
        <p className="text-lg font-bold text-blue-600 mt-2">
          ${product.price.toFixed(2)}
        </p>
        <button
          onClick={() => onAddToCart(product.id)}
          className="w-full mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition"
        >
          Add to Cart
        </button>
      </div>
    </div>
  )
);

// components/SearchBar.tsx
export const SearchBar: React.FC = () => {
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebounce(query, 300);
  const [results, setResults] = useState<Product[]>([]);

  useEffect(() => {
    if (!debouncedQuery) {
      setResults([]);
      return;
    }

    productApi.search(debouncedQuery)
      .then((res) => setResults(res.data))
      .catch(() => setResults([]));
  }, [debouncedQuery]);

  return (
    <div className="relative">
      <input
        type="text"
        placeholder="Tìm kiếm sản phẩm..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        className="w-full px-4 py-2 border rounded-lg"
      />
      {results.length > 0 && (
        <div className="absolute top-full left-0 right-0 bg-white border rounded-lg mt-1 max-h-64 overflow-y-auto">
          {results.map((product) => (
            <div
              key={product.id}
              className="px-4 py-2 hover:bg-gray-100 cursor-pointer"
            >
              {product.name}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// pages/CheckoutPage.tsx
export const CheckoutPage: React.FC = () => {
  const { cart, checkout, loading } = useCart();
  const [paymentMethod, setPaymentMethod] = useState('COD');
  const navigate = useNavigate();

  const handleCheckout = async () => {
    try {
      const result = await checkout(paymentMethod);

      if (paymentMethod === 'VNPAY' && result.paymentUrl) {
        window.location.href = result.paymentUrl;
      } else {
        navigate(`/orders/${result.orderId}`);
      }
    } catch (error) {
      console.error('Checkout failed:', error);
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6">Checkout</h1>

      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">Order Summary</h2>
        <div className="space-y-2">
          {cart.map((item) => (
            <div key={item.variantId} className="flex justify-between">
              <span>{item.product.name} x {item.quantity}</span>
              <span>${(item.product.price * item.quantity).toFixed(2)}</span>
            </div>
          ))}
        </div>
        <div className="border-t mt-4 pt-4 font-bold">
          Total: ${cart.reduce((sum, item) => sum + item.product.price * item.quantity, 0).toFixed(2)}
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">Payment Method</h2>
        <div className="space-y-2">
          <label>
            <input
              type="radio"
              value="COD"
              checked={paymentMethod === 'COD'}
              onChange={(e) => setPaymentMethod(e.target.value)}
            />
            Cash on Delivery
          </label>
          <label>
            <input
              type="radio"
              value="VNPAY"
              checked={paymentMethod === 'VNPAY'}
              onChange={(e) => setPaymentMethod(e.target.value)}
            />
            VNPAY Payment
          </label>
        </div>
      </div>

      <button
        onClick={handleCheckout}
        disabled={loading || cart.length === 0}
        className="w-full px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
      >
        {loading ? 'Processing...' : 'Complete Order'}
      </button>
    </div>
  );
};
```

## Error Boundary

```typescript
// components/ErrorBoundary.tsx
export class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { hasError: boolean; error: Error | null }
> {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error) {
    console.error('Error caught:', error);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
          <h2 className="font-bold text-red-800">Something went wrong</h2>
          <p className="text-red-700">{this.state.error?.message}</p>
        </div>
      );
    }

    return this.props.children;
  }
}
```

## Verification Commands

```bash
# Type check
cd frontend && npx tsc --noEmit

# Build
npm run build

# Test API integration
npm test -- services/api

# Check for console errors
npm run dev -- --profile
```
