---
name: react-core-skill
description: Enforce React 18+ component architecture, hooks patterns, state management, and TypeScript best practices for feature-based e-commerce frontend modules.
---

## Scope & Activation Rules

Activate when:
- Creating React functional components with hooks (useState, useEffect, useContext)
- Designing feature modules (Cart, Orders, Products) with composition
- Implementing custom hooks for reusable logic
- Managing component state, props, and side effects
- Using React patterns: compound components, render props, higher-order components
- Optimizing rendering with React.memo, useMemo, useCallback
- Writing TypeScript-first React code with strict null checks

## System Directives

### DO
- **Use Functional Components with Hooks**: Never use class components. Always leverage hooks for state and lifecycle.
- **Extract Custom Hooks**: Encapsulate reusable logic (data fetching, forms, persistence) in custom hooks (e.g., `useCart`, `useAsync`).
- **Type Everything with TypeScript**: Strict null checks enabled; interfaces for props, state, and API responses.
- **Separate Containers from Presentational Components**: Container components manage state/logic; Presentational components handle rendering.
- **Use React Context for App-Level State**: For authentication, theme, user preferences. Use Redux/Zustand only if state becomes complex.
- **Implement Compound Components**: Build composable component hierarchies (e.g., `<Form>`, `<Form.Input>`, `<Form.Submit>`).
- **Memoize Expensive Operations**: Use `useMemo` for computed values, `useCallback` for stable function references.
- **Use `key` Prop Correctly**: Always provide stable keys in lists; never use array index for keys.
- **Handle Loading/Error States Explicitly**: Every async operation should have loading, error, and success states.
- **Validate Props at Component Entry**: Use TypeScript types strictly; consider runtime validation with libraries like `zod` for API responses.
- **Cleanup Side Effects**: Always return cleanup functions from `useEffect` for subscriptions, timers, event listeners.
- **Structure Folders by Feature**: `/src/features/cart`, `/src/features/orders` not `/src/components`, `/src/containers`.
- **Implement Error Boundaries**: Wrap feature modules with error boundaries to catch rendering errors.

### DO NOT
- Use class components. Functional components + hooks are the React standard.
- Place business logic in component render functions. Extract to custom hooks.
- Mutate state directly. Always use setState or hook setters.
- Use array index as `key` in lists. This breaks component state when lists reorder.
- Omit cleanup functions in `useEffect`. This causes memory leaks and event listener buildup.
- Create new objects/functions in render. Memoize with `useMemo`/`useCallback` to prevent unnecessary re-renders.
- Mix TypeScript types and JavaScript. Always maintain strict type safety.
- Pass too many props deeply. Use Context or refactor component structure.
- Ignore TypeScript errors. Build should fail on any type error.
- Use `any` type. Always define explicit types or use `unknown` with type guards.
- Leave commented-out code in commits. Remove debug code before submitting.
- Render large lists without virtualization. Use `react-window` or `react-virtualized` for 1000+ items.

## Production Reference Implementation

### Custom Hook: useCart (Cart Management)

```typescript
import { useCallback, useContext, useState } from 'react';
import { CartContext } from '@context/CartContext';
import { useAsync } from './useAsync';
import { cartAPI } from '@services/api/cartAPI';
import type { Cart, CartItem, CreateOrderRequest } from '@types/index';

export const useCart = () => {
  const { cart, setCart } = useContext(CartContext);
  const [error, setError] = useState<string | null>(null);
  const { loading, execute } = useAsync();

  const addItem = useCallback(
    async (productId: string, quantity: number) => {
      if (quantity < 1) {
        setError('Quantity must be at least 1');
        return false;
      }

      try {
        setError(null);
        const updatedCart = await execute(() =>
          cartAPI.addItem(cart.id, { productId, quantity })
        );
        setCart(updatedCart);
        return true;
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Failed to add item';
        setError(message);
        return false;
      }
    },
    [cart.id, execute, setCart]
  );

  const removeItem = useCallback(
    async (itemId: string) => {
      try {
        setError(null);
        const updatedCart = await execute(() =>
          cartAPI.removeItem(cart.id, itemId)
        );
        setCart(updatedCart);
        return true;
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Failed to remove item';
        setError(message);
        return false;
      }
    },
    [cart.id, execute, setCart]
  );

  const updateItemQuantity = useCallback(
    async (itemId: string, quantity: number) => {
      if (quantity === 0) {
        return removeItem(itemId);
      }

      try {
        setError(null);
        const updatedCart = await execute(() =>
          cartAPI.updateItem(cart.id, itemId, { quantity })
        );
        setCart(updatedCart);
        return true;
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Failed to update quantity';
        setError(message);
        return false;
      }
    },
    [cart.id, execute, removeItem, setCart]
  );

  const checkout = useCallback(
    async (request: CreateOrderRequest) => {
      try {
        setError(null);
        const order = await execute(() => cartAPI.checkout(request));
        setCart({ ...cart, items: [], totalPrice: 0, itemCount: 0 });
        return order;
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Checkout failed';
        setError(message);
        throw new Error(message);
      }
    },
    [cart, execute, setCart]
  );

  return {
    cart,
    loading,
    error,
    addItem,
    removeItem,
    updateItemQuantity,
    checkout,
    clearError: () => setError(null),
  };
};
```

### Custom Hook: useAsync (Data Fetching)

```typescript
import { useCallback, useState } from 'react';

interface UseAsyncState<T> {
  data: T | null;
  loading: boolean;
  error: Error | null;
}

export const useAsync = <T,>(): UseAsyncState<T> & { execute: (fn: () => Promise<T>) => Promise<T> } => {
  const [state, setState] = useState<UseAsyncState<T>>({
    data: null,
    loading: false,
    error: null,
  });

  const execute = useCallback(
    async (fn: () => Promise<T>): Promise<T> => {
      setState({ data: null, loading: true, error: null });
      try {
        const result = await fn();
        setState({ data: result, loading: false, error: null });
        return result;
      } catch (err) {
        const error = err instanceof Error ? err : new Error(String(err));
        setState({ data: null, loading: false, error });
        throw error;
      }
    },
    []
  );

  return { ...state, execute };
};
```

### Compound Component Pattern: Form

```typescript
import React, { ReactNode, createContext, useContext, useState } from 'react';

interface FormContextValue {
  values: Record<string, string>;
  errors: Record<string, string>;
  touched: Record<string, boolean>;
  handleChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  handleBlur: (e: React.FocusEvent<HTMLInputElement>) => void;
  setFieldValue: (name: string, value: string) => void;
  setFieldError: (name: string, error: string) => void;
}

const FormContext = createContext<FormContextValue | null>(null);

interface FormProps {
  onSubmit: (values: Record<string, string>) => Promise<void>;
  initialValues: Record<string, string>;
  children: ReactNode;
}

const Form: React.FC<FormProps> & {
  Input: typeof FormInput;
  Submit: typeof FormSubmit;
  ErrorMessage: typeof FormErrorMessage;
} = ({ onSubmit, initialValues, children }) => {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setValues((prev) => ({ ...prev, [name]: value }));
  };

  const handleBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    setTouched((prev) => ({ ...prev, [e.target.name]: true }));
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await onSubmit(values);
    } catch (err) {
      console.error('Form submission error:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <FormContext.Provider
      value={{
        values,
        errors,
        touched,
        handleChange,
        handleBlur,
        setFieldValue: (name, value) =>
          setValues((prev) => ({ ...prev, [name]: value })),
        setFieldError: (name, error) =>
          setErrors((prev) => ({ ...prev, [name]: error })),
      }}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {children}
      </form>
    </FormContext.Provider>
  );
};

const FormInput: React.FC<{
  name: string;
  label: string;
  type?: string;
  placeholder?: string;
  required?: boolean;
}> = ({ name, label, type = 'text', placeholder, required = false }) => {
  const context = useContext(FormContext);
  if (!context) throw new Error('FormInput must be used within Form');

  const { values, errors, touched, handleChange, handleBlur } = context;
  const hasError = touched[name] && errors[name];

  return (
    <div>
      <label htmlFor={name} className="block text-sm font-medium">
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      <input
        id={name}
        name={name}
        type={type}
        placeholder={placeholder}
        value={values[name] || ''}
        onChange={handleChange}
        onBlur={handleBlur}
        className={`w-full px-3 py-2 border rounded ${
          hasError ? 'border-red-500 bg-red-50' : 'border-gray-300'
        }`}
      />
      {hasError && <p className="mt-1 text-sm text-red-500">{errors[name]}</p>}
    </div>
  );
};

const FormSubmit: React.FC<{ label: string }> = ({ label }) => {
  const context = useContext(FormContext);
  if (!context) throw new Error('FormSubmit must be used within Form');

  // Simulating isSubmitting from parent component (would need to be added to context)
  return (
    <button
      type="submit"
      className="w-full px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition"
    >
      {label}
    </button>
  );
};

const FormErrorMessage: React.FC<{ name: string }> = ({ name }) => {
  const context = useContext(FormContext);
  if (!context) throw new Error('FormErrorMessage must be used within Form');

  const { errors, touched } = context;
  if (!touched[name] || !errors[name]) return null;

  return <p className="text-sm text-red-500 mt-1">{errors[name]}</p>;
};

Form.Input = FormInput;
Form.Submit = FormSubmit;
Form.ErrorMessage = FormErrorMessage;

export { Form };
```

### Cart Feature Component

```typescript
import React, { useEffect } from 'react';
import { useCart } from '@hooks/useCart';
import { useAsync } from '@hooks/useAsync';
import { cartAPI } from '@services/api/cartAPI';
import type { Cart } from '@types/cart';

interface CartPageProps {
  onCheckout?: (orderId: string) => void;
}

export const CartPage: React.FC<CartPageProps> = ({ onCheckout }) => {
  const { cart, loading, error, removeItem, updateItemQuantity, checkout } = useCart();
  const { data: initialCart, loading: initialLoading } = useAsync<Cart>();

  useEffect(() => {
    // Load cart on mount
    const loadCart = async () => {
      try {
        const cartData = await cartAPI.getCart();
        // Update cart context
      } catch (err) {
        console.error('Failed to load cart:', err);
      }
    };

    loadCart();
  }, []);

  if (initialLoading || loading) {
    return <div className="text-center py-8">Loading cart...</div>;
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        <p>Error: {error}</p>
      </div>
    );
  }

  if (cart.items.length === 0) {
    return (
      <div className="text-center py-12">
        <h2 className="text-2xl font-semibold mb-2">Your cart is empty</h2>
        <p className="text-gray-600">Start shopping to add items to your cart</p>
      </div>
    );
  }

  const handleQuantityChange = async (itemId: string, newQuantity: number) => {
    await updateItemQuantity(itemId, newQuantity);
  };

  const handleRemoveItem = async (itemId: string) => {
    await removeItem(itemId);
  };

  const handleCheckout = async () => {
    try {
      const order = await checkout({
        cartId: cart.id,
        address: {
          street: '123 Main St',
          city: 'New York',
          postalCode: '10001',
          country: 'US',
        },
        paymentMethod: 'CREDIT_CARD',
      });
      onCheckout?.(order.id);
    } catch (err) {
      console.error('Checkout failed:', err);
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6">Shopping Cart</h1>

      <div className="space-y-4 mb-8">
        {cart.items.map((item) => (
          <CartItemRow
            key={item.id}
            item={item}
            onQuantityChange={(qty) => handleQuantityChange(item.id, qty)}
            onRemove={() => handleRemoveItem(item.id)}
          />
        ))}
      </div>

      <div className="border-t pt-4">
        <div className="flex justify-between text-xl font-semibold mb-6">
          <span>Total:</span>
          <span>${cart.totalPrice.toFixed(2)}</span>
        </div>
        <button
          onClick={handleCheckout}
          disabled={loading}
          className="w-full px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
        >
          {loading ? 'Processing...' : 'Proceed to Checkout'}
        </button>
      </div>
    </div>
  );
};

interface CartItemRowProps {
  item: CartItem;
  onQuantityChange: (quantity: number) => void;
  onRemove: () => void;
}

const CartItemRow: React.FC<CartItemRowProps> = React.memo(
  ({ item, onQuantityChange, onRemove }) => (
    <div className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50">
      <div className="flex-1">
        <h3 className="font-semibold">{item.product.name}</h3>
        <p className="text-gray-600">${item.price.toFixed(2)} each</p>
      </div>

      <div className="flex items-center gap-4">
        <div className="flex items-center border rounded">
          <button
            onClick={() => onQuantityChange(item.quantity - 1)}
            className="px-3 py-1 hover:bg-gray-100"
          >
            −
          </button>
          <span className="px-4 py-1">{item.quantity}</span>
          <button
            onClick={() => onQuantityChange(item.quantity + 1)}
            className="px-3 py-1 hover:bg-gray-100"
          >
            +
          </button>
        </div>

        <p className="font-semibold w-24 text-right">
          ${(item.price * item.quantity).toFixed(2)}
        </p>

        <button
          onClick={onRemove}
          className="px-4 py-2 bg-red-100 text-red-600 rounded hover:bg-red-200"
        >
          Remove
        </button>
      </div>
    </div>
  )
);

CartItemRow.displayName = 'CartItemRow';
```

### Error Boundary

```typescript
import React, { ReactNode } from 'react';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: (error: Error, retry: () => void) => ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  retry = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      return (
        this.props.fallback?.(this.state.error!, this.retry) ?? (
          <div className="p-4 bg-red-50 border border-red-200 rounded">
            <h2 className="font-bold text-red-800 mb-2">Something went wrong</h2>
            <p className="text-red-700 mb-4">{this.state.error?.message}</p>
            <button
              onClick={this.retry}
              className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
            >
              Try Again
            </button>
          </div>
        )
      );
    }

    return this.props.children;
  }
}
```

## Anti-Patterns & Automated Fixes

### Anti-Pattern 1: Creating Functions in Render
**Problem**: New function instances on every render cause child components to re-render.
```typescript
// ❌ WRONG
const CartList = ({ items }) => {
  return items.map(item => (
    <CartItem
      key={item.id}
      onRemove={() => removeItem(item.id)} // New function every render
    />
  ));
};
```
**Fix**: Use `useCallback` to memoize function references.
```typescript
// ✅ CORRECT
const CartList = ({ items }) => {
  const handleRemove = useCallback(
    (itemId: string) => removeItem(itemId),
    []
  );

  return items.map(item => (
    <CartItem key={item.id} onRemove={() => handleRemove(item.id)} />
  ));
};
```

### Anti-Pattern 2: Array Index as Key
**Problem**: If list reorders, components lose state.
```typescript
// ❌ WRONG
{items.map((item, index) => (
  <CartItem key={index} item={item} />
))}
```
**Fix**: Use unique, stable identifier.
```typescript
// ✅ CORRECT
{items.map(item => (
  <CartItem key={item.id} item={item} />
))}
```

### Anti-Pattern 3: Missing Cleanup in useEffect
**Problem**: Event listeners accumulate; memory leaks occur.
```typescript
// ❌ WRONG
useEffect(() => {
  window.addEventListener('resize', handleResize);
}, []);
```
**Fix**: Return cleanup function.
```typescript
// ✅ CORRECT
useEffect(() => {
  window.addEventListener('resize', handleResize);
  return () => window.removeEventListener('resize', handleResize);
}, []);
```

### Anti-Pattern 4: Mutating State
**Problem**: React may not detect state changes; renders don't trigger.
```typescript
// ❌ WRONG
const addItem = (item: CartItem) => {
  cart.items.push(item); // Direct mutation
  setCart(cart);
};
```
**Fix**: Create new object/array.
```typescript
// ✅ CORRECT
const addItem = (item: CartItem) => {
  setCart(prev => ({
    ...prev,
    items: [...prev.items, item]
  }));
};
```

## Verification Commands

### TypeScript Strict Checks
```bash
# Build with strict TypeScript
cd frontend && npx tsc --noEmit --strict

# Run type checking on specific files
npx tsc --noEmit src/features/cart/CartPage.tsx
```

### Lint React Hooks
```bash
# Check for missing dependencies in useEffect
npx eslint src --plugin react-hooks --rule react-hooks/exhaustive-deps:warn

# Check for stale closures
npx eslint src --rule react/jsx-no-useless-fragment:warn
```

### Component Testing
```bash
# Run component tests
npm test -- --coverage

# Test specific component
npm test CartPage.test.tsx

# Watch mode for TDD
npm test -- --watch
```

### Performance Profiling
```bash
# Build optimized bundle
npm run build

# Analyze bundle size
npm run build -- --analyze

# Check for unnecessary re-renders
npm run dev -- --profile
```
