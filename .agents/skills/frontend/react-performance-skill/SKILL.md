---
name: react-performance-skill
description: Enforce code-splitting, lazy loading, memoization, bundle optimization, and runtime performance monitoring for React e-commerce applications.
---

## Scope & Activation Rules

Activate when:
- Implementing route-based code splitting with `React.lazy` and `Suspense`
- Optimizing component rendering with `React.memo`, `useMemo`, `useCallback`
- Analyzing and reducing bundle size
- Implementing lazy-load images, virtualized lists (large datasets)
- Monitoring performance metrics (Core Web Vitals, Runtime Performance)
- Optimizing state updates and re-renders
- Configuring webpack/Vite for production optimization
- Using Lighthouse, DevTools Performance tab for profiling

## System Directives

### DO
- **Split Routes into Code Chunks**: Use `React.lazy()` and dynamic imports for route-based splitting; load feature modules on demand.
- **Wrap Lazy Components with Suspense**: Always provide a fallback while chunk is loading; never leave lazy components unguarded.
- **Memoize Expensive Computations**: Use `useMemo` for derived state, computations that depend on props/state changes.
- **Memoize Expensive Components**: Use `React.memo` for components with static props or when prop changes are infrequent.
- **Use useCallback for Event Handlers**: Prevent child re-renders by memoizing callback references, especially in lists.
- **Lazy-Load Images**: Use `<img loading="lazy">` or IntersectionObserver for below-fold images.
- **Virtualize Long Lists**: Use `react-window` or `react-virtualized` for lists >100 items; only render visible rows.
- **Defer Non-Critical Rendering**: Use `useTransition()` (React 18+) for non-blocking state updates (search, filtering).
- **Monitor Core Web Vitals**: Implement tracking for LCP, FID, CLS; use `web-vitals` library and send to monitoring service.
- **Analyze Bundle Size**: Use `webpack-bundle-analyzer` or Vite's built-in analyzer; identify and eliminate large dependencies.
- **Implement PWA Features**: Service workers, offline caching for faster repeat visits.
- **Compress Assets**: Use Gzip/Brotli compression; minify CSS, JS, HTML in production.
- **Tree-Shake Unused Code**: Ensure bundler removes unused exports; check import statements are not star imports.

### DO NOT
- Import entire libraries for single utilities. Use tree-shakeable imports: `import { debounce } from 'lodash-es'` not `import _ from 'lodash'`.
- Memoize everything indiscriminately. Memoization has overhead; use when proven necessary via profiling.
- Use `React.memo` on components that don't accept props or receive props every render anyway.
- Leave unoptimized images in production (large file sizes, no lazy loading, no responsive variants).
- Render very long lists without virtualization. This causes DOM bloat and memory leaks.
- Use heavy dependencies without considering lighter alternatives (moment → date-fns, lodash → lodash-es).
- Skip performance profiling. Always baseline performance before and after optimizations.
- Ignore bundle size growth. Monitor bundle size in CI/CD; fail builds if size increases >5%.
- Use inline styles or complex CSS-in-JS without optimization. These cause runtime overhead.
- Implement micro-optimizations that make code less readable. Optimize hot paths, not cold ones.
- Skip PWA optimizations in production. Even simple caching improves performance dramatically.
- Leave console.log statements in production code. These slow down execution.

## Production Reference Implementation

### Code Splitting with Route-Based Lazy Loading

```typescript
import React, { Suspense, ReactNode } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';

// Lazy-loaded feature components
const CartPage = React.lazy(() => import('@features/cart/CartPage'));
const OrdersPage = React.lazy(() => import('@features/orders/OrdersPage'));
const ProductListPage = React.lazy(() => import('@features/products/ProductListPage'));
const ProductDetailPage = React.lazy(() => import('@features/products/ProductDetailPage'));
const AdminPage = React.lazy(() => import('@pages/admin/AdminPage'));
const LoginPage = React.lazy(() => import('@pages/login/LoginPage'));

interface SuspenseWrapperProps {
  children: ReactNode;
}

const SuspenseWrapper: React.FC<SuspenseWrapperProps> = ({ children }) => (
  <Suspense
    fallback={
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin">
          <svg
            className="w-12 h-12 text-blue-600"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
        </div>
      </div>
    }
  >
    {children}
  </Suspense>
);

export const AppRouter: React.FC = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route
        path="/login"
        element={
          <SuspenseWrapper>
            <LoginPage />
          </SuspenseWrapper>
        }
      />

      {/* Protected Routes */}
      <Route
        path="/products"
        element={
          <SuspenseWrapper>
            <ProductListPage />
          </SuspenseWrapper>
        }
      />
      <Route
        path="/products/:id"
        element={
          <SuspenseWrapper>
            <ProductDetailPage />
          </SuspenseWrapper>
        }
      />
      <Route
        path="/cart"
        element={
          <SuspenseWrapper>
            <CartPage />
          </SuspenseWrapper>
        }
      />
      <Route
        path="/orders"
        element={
          <SuspenseWrapper>
            <OrdersPage />
          </SuspenseWrapper>
        }
      />

      {/* Admin Routes */}
      <Route
        path="/admin/*"
        element={
          <SuspenseWrapper>
            <AdminPage />
          </SuspenseWrapper>
        }
      />

      {/* Default Redirect */}
      <Route path="/" element={<Navigate to="/products" replace />} />
    </Routes>
  );
};
```

### Memoized Components with Smart Props Comparison

```typescript
import React, { useMemo, useCallback, useState } from 'react';

interface ProductCardProps {
  product: {
    id: string;
    name: string;
    price: number;
    image: string;
    rating: number;
  };
  onAddToCart: (productId: string, quantity: number) => void;
  inCart: boolean;
}

const ProductCard = React.memo<ProductCardProps>(
  ({ product, onAddToCart, inCart }) => {
    const [quantity, setQuantity] = useState(1);

    return (
      <div className="bg-white rounded-lg shadow-md p-4 hover:shadow-lg transition">
        <img
          src={product.image}
          alt={product.name}
          loading="lazy"
          className="w-full h-48 object-cover rounded mb-4"
        />

        <h3 className="text-lg font-semibold text-gray-800">{product.name}</h3>

        <div className="flex items-center justify-between mt-2 mb-4">
          <span className="text-2xl font-bold text-blue-600">${product.price.toFixed(2)}</span>
          <span className="flex items-center text-yellow-500">
            {'⭐'.repeat(product.rating)}
          </span>
        </div>

        <div className="flex items-center justify-between">
          <input
            type="number"
            min="1"
            max="10"
            value={quantity}
            onChange={(e) => setQuantity(parseInt(e.target.value))}
            className="w-16 px-2 py-1 border border-gray-300 rounded"
          />

          <button
            onClick={() => onAddToCart(product.id, quantity)}
            disabled={inCart}
            className={`px-4 py-2 rounded font-semibold transition ${
              inCart
                ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                : 'bg-blue-600 text-white hover:bg-blue-700'
            }`}
          >
            {inCart ? 'In Cart' : 'Add to Cart'}
          </button>
        </div>
      </div>
    );
  },
  (prevProps, nextProps) => {
    // Custom comparison: only re-render if product ID or inCart status changes
    return (
      prevProps.product.id === nextProps.product.id &&
      prevProps.inCart === nextProps.inCart
    );
  }
);

ProductCard.displayName = 'ProductCard';

export { ProductCard };
```

### Virtualized Product List with react-window

```typescript
import React, { useMemo, useCallback, useState } from 'react';
import { FixedSizeList as List } from 'react-window';
import AutoSizer from 'react-virtualized-auto-sizer';
import { ProductCard } from './ProductCard';
import type { Product } from '@types/product';

interface ProductListProps {
  products: Product[];
  cartItems: string[];
  onAddToCart: (productId: string, quantity: number) => void;
  loading: boolean;
}

export const ProductList: React.FC<ProductListProps> = React.memo(
  ({ products, cartItems, onAddToCart, loading }) => {
    if (loading) {
      return <div className="text-center py-8">Loading products...</div>;
    }

    if (products.length === 0) {
      return <div className="text-center py-8">No products found</div>;
    }

    const ITEM_SIZE = 300; // Height of product card
    const COLUMNS = 4; // Products per row

    const Row: React.FC<{ index: number; style: React.CSSProperties }> = ({
      index,
      style,
    }) => {
      const startIdx = index * COLUMNS;
      const endIdx = Math.min(startIdx + COLUMNS, products.length);
      const rowProducts = products.slice(startIdx, endIdx);

      return (
        <div style={style} className="flex gap-4 px-4">
          {rowProducts.map((product) => (
            <ProductCard
              key={product.id}
              product={product}
              inCart={cartItems.includes(product.id)}
              onAddToCart={onAddToCart}
            />
          ))}
        </div>
      );
    };

    const rowCount = Math.ceil(products.length / COLUMNS);

    return (
      <AutoSizer>
        {({ height, width }) => (
          <List
            height={height}
            itemCount={rowCount}
            itemSize={ITEM_SIZE}
            width={width}
          >
            {Row}
          </List>
        )}
      </AutoSizer>
    );
  }
);

ProductList.displayName = 'ProductList';
```

### Deferred State Updates with useTransition

```typescript
import React, { useTransition, useState, useMemo } from 'react';
import type { Product } from '@types/product';

interface ProductSearchProps {
  products: Product[];
  onAddToCart: (productId: string) => void;
}

export const ProductSearch: React.FC<ProductSearchProps> = ({ products, onAddToCart }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isPending, startTransition] = useTransition();

  const filteredProducts = useMemo(() => {
    if (!searchTerm.trim()) return products;

    const term = searchTerm.toLowerCase();
    return products.filter(
      (p) =>
        p.name.toLowerCase().includes(term) ||
        p.description?.toLowerCase().includes(term)
    );
  }, [products, searchTerm]);

  const handleSearch = (value: string) => {
    // Update input immediately for responsive UI
    setSearchTerm(value);

    // Defer filtering and re-rendering to lower priority
    startTransition(() => {
      // Filtering happens here, non-blocking
    });
  };

  return (
    <div className="space-y-4">
      <input
        type="text"
        placeholder="Search products..."
        value={searchTerm}
        onChange={(e) => handleSearch(e.target.value)}
        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-600"
      />

      {isPending && (
        <div className="text-sm text-gray-500">Searching...</div>
      )}

      <div className="grid grid-cols-4 gap-4">
        {filteredProducts.map((product) => (
          <ProductCard
            key={product.id}
            product={product}
            onAddToCart={() => onAddToCart(product.id)}
            inCart={false}
          />
        ))}
      </div>
    </div>
  );
};
```

### Performance Monitoring with Core Web Vitals

```typescript
import { getCLS, getFID, getFCP, getLCP, getTTFB } from 'web-vitals';

interface PerformanceMetrics {
  name: string;
  value: number;
  rating: 'good' | 'needs-improvement' | 'poor';
}

const sendMetrics = (metrics: PerformanceMetrics[]) => {
  // Send to analytics service
  if (import.meta.env.VITE_ANALYTICS_ENDPOINT) {
    navigator.sendBeacon(
      import.meta.env.VITE_ANALYTICS_ENDPOINT,
      JSON.stringify(metrics)
    );
  }

  // Log in development
  if (import.meta.env.DEV) {
    console.table(metrics);
  }
};

export const initializePerformanceMonitoring = () => {
  const metrics: PerformanceMetrics[] = [];

  getCLS((metric) => {
    metrics.push({
      name: 'CLS (Cumulative Layout Shift)',
      value: metric.value,
      rating: metric.rating,
    });
  });

  getFID((metric) => {
    metrics.push({
      name: 'FID (First Input Delay)',
      value: metric.value,
      rating: metric.rating,
    });
  });

  getFCP((metric) => {
    metrics.push({
      name: 'FCP (First Contentful Paint)',
      value: metric.value,
      rating: metric.rating,
    });
  });

  getLCP((metric) => {
    metrics.push({
      name: 'LCP (Largest Contentful Paint)',
      value: metric.value,
      rating: metric.rating,
    });
  });

  getTTFB((metric) => {
    metrics.push({
      name: 'TTFB (Time to First Byte)',
      value: metric.value,
      rating: metric.rating,
    });

    if (metrics.length === 5) {
      sendMetrics(metrics);
    }
  });
};
```

### Vite Configuration for Production Optimization

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { visualizer } from 'rollup-plugin-visualizer';
import compression from 'vite-plugin-compression';

export default defineConfig({
  plugins: [
    react(),
    compression({ algorithm: 'brotli', ext: '.br' }),
    visualizer({ open: false, filename: 'dist/bundle-report.html' }),
  ],
  build: {
    target: 'ES2020',
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
      },
    },
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-react': ['react', 'react-dom', 'react-router-dom'],
          'vendor-axios': ['axios'],
          'vendor-ui': ['@headlessui/react', '@heroicons/react'],
        },
        entryFileNames: 'js/[name].[hash].js',
        chunkFileNames: 'js/[name].[hash].js',
        assetFileNames: (assetInfo) => {
          const info = assetInfo.name.split('.');
          const ext = info[info.length - 1];
          if (/png|jpe?g|gif|svg/.test(ext)) {
            return `images/[name].[hash][extname]`;
          }
          return `css/[name].[hash][extname]`;
        },
      },
    },
  },
  resolve: {
    alias: {
      '@': '/src',
      '@components': '/src/components',
      '@features': '/src/features',
      '@services': '/src/services',
      '@types': '/src/types',
      '@hooks': '/src/hooks',
      '@context': '/src/context',
    },
  },
});
```

### Service Worker for Offline Caching

```typescript
// src/service-worker.ts
declare const self: ServiceWorkerGlobalScope;

const CACHE_NAME = 'nexora-v1';
const ASSETS_TO_CACHE = [
  '/',
  '/index.html',
  '/manifest.json',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(ASSETS_TO_CACHE);
    })
  );
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') {
    return;
  }

  event.respondWith(
    caches.match(event.request).then((response) => {
      if (response) {
        return response;
      }

      return fetch(event.request).then((response) => {
        // Only cache successful responses
        if (!response || response.status !== 200) {
          return response;
        }

        const responseToCache = response.clone();
        caches.open(CACHE_NAME).then((cache) => {
          cache.put(event.request, responseToCache);
        });

        return response;
      });
    })
  );
});
```

## Anti-Patterns & Automated Fixes

### Anti-Pattern 1: No Code Splitting
**Problem**: Single large bundle delays initial load; all code loads even for unused routes.
```typescript
// ❌ WRONG
import CartPage from '@features/cart/CartPage';
import OrdersPage from '@features/orders/OrdersPage';
// All loaded upfront
```
**Fix**: Use React.lazy() for route-based code splitting.
```typescript
// ✅ CORRECT
const CartPage = React.lazy(() => import('@features/cart/CartPage'));
const OrdersPage = React.lazy(() => import('@features/orders/OrdersPage'));
```

### Anti-Pattern 2: Missing React.memo on Lists
**Problem**: Entire list re-renders when parent state changes.
```typescript
// ❌ WRONG
const ProductList = ({ items, onSelect }) => (
  <div>
    {items.map(item => (
      <ProductCard key={item.id} item={item} onSelect={onSelect} />
    ))}
  </div>
);
```
**Fix**: Memoize list item components.
```typescript
// ✅ CORRECT
const ProductCard = React.memo(({ item, onSelect }) => (
  <div onClick={() => onSelect(item.id)}>{item.name}</div>
));
```

### Anti-Pattern 3: Creating Functions in Render Without useCallback
**Problem**: New function reference every render causes memoized children to re-render.
```typescript
// ❌ WRONG
<ProductList items={items} onSelect={(id) => handleSelect(id)} />
```
**Fix**: Memoize callback with useCallback.
```typescript
// ✅ CORRECT
const handleSelectMemo = useCallback((id) => handleSelect(id), []);
<ProductList items={items} onSelect={handleSelectMemo} />
```

### Anti-Pattern 4: Rendering Large Lists Without Virtualization
**Problem**: 10,000 items = 10,000 DOM nodes, causing jank and memory leaks.
```typescript
// ❌ WRONG
<div>{largeList.map(item => <Item key={item.id} {...item} />)}</div>
```
**Fix**: Use react-window for virtualization.
```typescript
// ✅ CORRECT
<FixedSizeList height={600} itemCount={largeList.length} itemSize={50}>
  {Row}
</FixedSizeList>
```

## Verification Commands

### Analyze Bundle Size

```bash
# Build and generate bundle report
cd frontend && npm run build

# View bundle report
open dist/bundle-report.html

# Check gzip size
npx bundlesize --config bundlesize.config.json
```

### Profile Performance with Lighthouse
```bash
# Run Lighthouse CLI
npx lighthouse http://localhost:5173 --view

# Check Core Web Vitals
npx web-vitals --output json > vitals.json && cat vitals.json | jq
```

### Monitor Runtime Performance
```bash
# Start dev server with profiler
npm run dev -- --profile

# Check for unnecessary re-renders in React DevTools Profiler
# Record interaction → Look for redundant renders on list items
```

### Check Code Splitting
```bash
# Verify route chunks are separate files
npm run build && ls -lh dist/js/

# Should see: js/vendor-react.hash.js, js/index.hash.js, etc.
```

### Test Virtualization on Long List
```bash
# Open DevTools → Performance tab
# Navigate to products page with 10,000 items
# Record: Should only see 20-50 DOM nodes (visible items), not 10,000

# Check memory usage before/after virtualization
# Should see dramatic improvement
```
