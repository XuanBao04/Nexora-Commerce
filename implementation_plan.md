# React Feature-Driven Architecture Refactoring Plan

This plan documents the transition of the `Nexora-commerce` React client codebase from a classic, tech-layered structure to a modern, modular, and scalable **Feature-Driven Architecture** as defined in `react-core-skill`.

## Inefficiencies in the Current Structure
1. **High Feature Dispersion:** A single functional domain like **Cart** or **Products** is scattered across 6-7 global directories (`components/Cart`, `context/`, `hooks/`, `services/api/`, `types/`, `utils/`). This creates heavy cognitive load and hampers velocity.
2. **Bloated Global Folders:** Folders like `src/components`, `src/hooks`, `src/services`, and `src/types` act as a "junk drawer" containing both generic utilities and highly specific domain code.
3. **Admin Subdomain Overlap:** Different administrative capabilities (Coupon, Inventory, Order, Product Management) are stored in a flat `src/components/admin` folder, obfuscating their relationship with client-side flows.
4. **Poor Encapsulation:** Components directly import deep nested dependencies, breaking the boundaries between modules.

---

## Proposed Feature-Driven Architecture

The refactored file layout will isolate all feature-specific files inside modular domain folders (`src/features/*`). Each feature folder acts as a self-contained unit and exposes its API through a clean public entry barrel (`index.ts`).

```
src/
├── components/          # Shared, pure presentation components (e.g. Buttons, Inputs, Spacers)
│   └── HeaderLayout.tsx # Refactored header layout wrapper
├── hooks/               # Shared global React hooks (useAsync, useLocalStorage, useDebounce)
├── services/            # Shared base API clients and interceptors
│   ├── api/
│   │   ├── apiClient.ts
│   │   └── responseInterceptor.ts
│   └── index.ts
├── types/               # Shared global TypeScript types
├── utils/               # Shared general utilities (cn, validation, constants)
├── features/            # Feature-driven domain packages
│   ├── auth/            # Auth feature modules
│   │   ├── components/  # LoginPage.tsx, RegisterPage.tsx
│   │   ├── services/    # loginService.ts, registerService.ts
│   │   └── index.ts     # Public export interface
│   ├── cart/            # Cart feature modules
│   │   ├── components/  # Cart.tsx, CartItem.tsx, AddressForm.tsx, etc.
│   │   ├── context/     # CartContext.tsx
│   │   ├── hooks/       # useCart.ts
│   │   ├── services/    # cartService.ts
│   │   ├── types/       # cart.ts
│   │   ├── utils/       # cartValidation.ts, priceCalculation.ts
│   │   └── index.ts     # Public export interface
│   ├── coupon/          # Coupon feature modules
│   │   ├── components/  # CouponManagement.tsx
│   │   ├── hooks/       # useCoupon.ts
│   │   ├── services/    # couponService.ts
│   │   ├── types/       # coupon.ts
│   │   └── index.ts     # Public export interface
│   ├── inventory/       # Inventory feature modules
│   │   ├── components/  # InventoryManagement.tsx
│   │   ├── services/    # inventoryService.ts
│   │   ├── types/       # inventory.ts
│   │   └── index.ts     # Public export interface
│   ├── orders/          # Orders feature modules
│   │   ├── components/  # Order.tsx, OrderManagement.tsx
│   │   ├── services/    # orderService.ts
│   │   ├── types/       # order.ts
│   │   └── index.ts     # Public export interface
│   └── products/        # Products feature modules
│       ├── components/  # ProductCard.tsx, ProductList.tsx, ProductManagement.tsx
│       ├── services/    # productService.ts
│       ├── types/       # product.ts
│       └── index.ts     # Public export interface
```

---

## File Migration Map

The table below outlines the precise moving list from the old structure to the new Feature-Driven architecture.

| Old Location | New Location | Description |
| :--- | :--- | :--- |
| **Auth** | | |
| `src/pages/login/LoginPage.tsx` | `src/features/auth/components/LoginPage.tsx` | Main Login layout and form wrapper |
| `src/pages/register/RegisterPage.tsx` | `src/features/auth/components/RegisterPage.tsx` | Main Register layout and form wrapper |
| `src/services/api/loginService.ts` | `src/features/auth/services/loginService.ts` | Login endpoint calling |
| `src/services/api/registerService.ts` | `src/features/auth/services/registerService.ts` | Registration endpoint calling |
| **Cart** | | |
| `src/components/Cart/Cart.tsx` | `src/features/cart/components/Cart.tsx` | Main Cart container page view |
| `src/components/Cart/CartItem.tsx` | `src/features/cart/components/CartItem.tsx` | Single cart item item card row |
| `src/components/Cart/AddressForm.tsx` | `src/features/cart/components/AddressForm.tsx` | Billing address input validator |
| `src/components/Cart/CouponInput.tsx` | `src/features/cart/components/CouponInput.tsx` | Active coupon code check box form |
| `src/components/Cart/PriceBreakdown.tsx` | `src/features/cart/components/PriceBreakdown.tsx` | Subtotal, discounts and tax breakdown |
| `src/context/CartContext.tsx` | `src/features/cart/context/CartContext.tsx` | Global Cart state React context |
| `src/hooks/useCart.ts` | `src/features/cart/hooks/useCart.ts` | Custom cart hook orchestrator |
| `src/services/api/cartService.ts` | `src/features/cart/services/cartService.ts` | Backend integration APIs for Cart |
| `src/types/cart.ts` | `src/features/cart/types/cart.ts` | TypeScript schemas and typings |
| `src/utils/cartValidation.ts` | `src/features/cart/utils/cartValidation.ts` | Rules enforcing checkout constraints |
| `src/utils/priceCalculation.ts` | `src/features/cart/utils/priceCalculation.ts` | Computations for pricing calculations |
| **Coupon** | | |
| `src/components/admin/CouponManagement.tsx` | `src/features/coupon/components/CouponManagement.tsx` | Admin panel for coupon configuration |
| `src/hooks/useCoupon.ts` | `src/features/coupon/hooks/useCoupon.ts` | Coupon state hooks |
| `src/services/api/couponService.ts` | `src/features/coupon/services/couponService.ts` | Backend integration APIs for coupons |
| `src/types/coupon.ts` | `src/features/coupon/types/coupon.ts` | Coupon model mappings |
| **Inventory** | | |
| `src/components/admin/InventoryManagement.tsx` | `src/features/inventory/components/InventoryManagement.tsx` | Product levels audit control panel |
| `src/services/api/inventoryService.ts` | `src/features/inventory/services/inventoryService.ts` | Stock API service layer |
| `src/types/inventory.ts` | `src/features/inventory/types/inventory.ts` | Stock status entity types |
| **Orders** | | |
| `src/components/Order/Order.tsx` | `src/features/orders/components/Order.tsx` | Client orders dashboard listing |
| `src/components/admin/OrderManagement.tsx` | `src/features/orders/components/OrderManagement.tsx` | Operational admin order fulfillments panel |
| `src/services/api/orderService.ts` | `src/features/orders/services/orderService.ts` | Backend integration APIs for orders |
| `src/types/order.ts` | `src/features/orders/types/order.ts` | Order entity definition mappings |
| **Products** | | |
| `src/components/ProductList/ProductCard.tsx` | `src/features/products/components/ProductCard.tsx` | Individual product display block card |
| `src/components/ProductList/ProductList.tsx` | `src/features/products/components/ProductList.tsx` | Marketplace grid listing shell |
| `src/components/admin/ProductManagement.tsx` | `src/features/products/components/ProductManagement.tsx` | SKU creation and details panel |
| `src/services/api/productService.ts` | `src/features/products/services/productService.ts` | Marketplace catalogue APIs |
| `src/types/product.ts` | `src/features/products/types/product.ts` | SKU model interfaces |
| **Shared Layout** | | |
| `src/components/HeaderLayout/HeaderLayout.tsx` | `src/components/HeaderLayout.tsx` | Standard shell layout |
| `src/pages/admin/AdminDashboard.tsx` | `src/features/admin/components/AdminDashboard.tsx` | Operations panel orchestrator |

---

## Import Alignment and Path Mapping

To preserve execution and compile status cleanly, we will leverage TypeScript path mappings.

### 1. Register `@features` Alias in `tsconfig.json`
```json
"paths": {
  "@/*": ["src/*"],
  "@components/*": ["src/components/*"],
  "@services/*": ["src/services/*"],
  "@utils/*": ["src/utils/*"],
  "@hooks/*": ["src/hooks/*"],
  "@features/*": ["src/features/*"]
}
```

### 2. Configure Aliasing in `vite.config.ts`
```typescript
alias: {
  "@": path.resolve(__dirname, "./src"),
  "@components": path.resolve(__dirname, "./src/components"),
  "@services": path.resolve(__dirname, "./src/services"),
  "@utils": path.resolve(__dirname, "./src/utils"),
  "@hooks": path.resolve(__dirname, "./src/hooks"),
  "@features": path.resolve(__dirname, "./src/features"),
}
```

### 3. Update File Imports
All moved components will have their relative and alias paths corrected. For example, in `App.tsx`:
```typescript
// Old Deep Imports
const LoginPage = lazy(() => import("./pages/login/LoginPage"));
const AdminDashboard = lazy(() => import("./pages/admin/AdminDashboard"));

// New Clean Modular Feature Barrels
const LoginPage = lazy(() => import("@features/auth").then(module => ({ default: module.LoginPage })));
const AdminDashboard = lazy(() => import("@features/admin").then(module => ({ default: module.AdminDashboard })));
```

---

## Verification Plan

### Automated Checks
- Compiling code base with typechecking `npm run build` or `npx tsc --noEmit`.
- Clean linting review on import maps.

---

## Implementation Status

- [x] Migrated Auth, Cart, Coupon, Inventory, Orders, Products, Admin, and HeaderLayout files into the feature-driven layout.
- [x] Added `@features` path aliases in `tsconfig.json` and `vite.config.ts`.
- [x] Rebuilt feature public barrels with named exports for lazy route imports.
- [x] Scoped shared root barrels (`hooks`, `services`, `types`, `utils`) to shared-only modules.
- [x] Removed stale global `orderService.updated.ts` and `productService.updated.ts` files after their logic was represented in feature services.
- [x] Fixed moved-file import paths for cart pricing, address, inventory, and constants dependencies.
- [x] Verified with `npx tsc --noEmit`.
- [x] Verified with `npm run build`.
