export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

// Inventory Types
export interface InventoryResponse {
  id: number;
  productId: string;
  quantity: number;
  reservedQuantity: number;
  soldQuantity: number;
  availableQuantity: number;
}

export interface StockStatus {
  productId: string;
  redisStock: number;
  locked: boolean;
  lastSyncTimestamp: number;
}

// Payment & Checkout Types
export interface PaymentRequest {
  paymentMethod: string;
  successful: boolean;
  transactionId?: string;
  errorMessage?: string;
}

export interface PaymentResponse {
  orderId: string;
  status: string;
  message: string;
}

// Review Types
export interface ProductReviewRequest {
  variantSku: string;
  orderId?: string;
  rating: number;
  comment: string;
  imageUrls?: string[];
}

export interface ProductReviewResponse {
  id: number;
  userId: string;
  username: string;
  variantSku: string;
  orderId?: string;
  rating: number;
  comment: string;
  createdAt: string;
  imageUrls?: string[];
  replies?: Array<{
    id: number;
    reviewId: number;
    adminUsername: string;
    comment: string;
    createdAt: string;
  }>;
}

// Wishlist Types
export interface WishlistRequest {
  productId: string;
}

export interface WishlistResponse {
  id: number;
  userId: string;
  productId: string;
  productName: string;
  productPrice: number;
  createdAt: string;
}

// CSRF Types
export interface CsrfToken {
  headerName: string;
  token: string;
  parameterName: string;
}
