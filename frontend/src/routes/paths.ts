export const PATHS = {
  HOME: "/",
  LOGIN: "/login",
  REGISTER: "/register",
  ADMIN_DASHBOARD: "/admin/dashboard",
  
  // Parent layout path
  AUTHENTICATED: "/authenticated",
  
  // Full navigation paths
  PRODUCTS: "/authenticated/products",
  CART: "/authenticated/cart",
  CHECKOUT: "/authenticated/checkout",
  ORDERS: "/authenticated/orders",

  // Relative segments for nested routing configuration
  SEGMENTS: {
    PRODUCTS: "products",
    CART: "cart",
    CHECKOUT: "checkout",
    ORDERS: "orders",
  }
} as const;
