import { create } from 'zustand';
import { cartService } from '@features/cart/services/cartService';
import { AxiosError } from 'axios';
import { CartResponse, CartItemRequest } from '@features/cart/types/cart';

interface CartState {
  cart: CartResponse | null;
  isLoading: boolean;
  error: string | null;
  isInitialized: boolean;
  
  // Actions
  fetchCart: () => Promise<void>;
  addItem: (item: CartItemRequest) => Promise<void>;
  removeItem: (productId: string) => Promise<void>;
  updateItem: (productId: string, quantity: number) => Promise<void>;
  clear: () => Promise<void>;
  initSessionAndCart: () => Promise<void>;
}

const getCurrentUserId = () => localStorage.getItem("userId");

const isTestEnv = typeof process !== 'undefined' && process.env.NODE_ENV === 'test';

export const useCartStore = create<CartState>((set, get) => ({
  cart: null,
  isLoading: false,
  error: null,
  isInitialized: isTestEnv,

  fetchCart: async () => {
    const userId = getCurrentUserId();
    if (!userId) {
      set({ cart: null });
      return;
    }

    set({ isLoading: true });
    try {
      const response = await cartService.getCart(userId);
      set({ cart: response, error: null });
    } catch (err) {
      set({ error: (err as Error).message });
    } finally {
      set({ isLoading: false });
    }
  },

  addItem: async (item: CartItemRequest) => {
    const userId = getCurrentUserId();
    if (!userId) {
      throw new Error("Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.");
    }

    try {
      const response = await cartService.addToCart(userId, item);
      set({ cart: response, error: null });
    } catch (err) {
      const errorMessage = (err as AxiosError<{message: string}>).response?.data?.message || (err as Error).message;
      set({ error: errorMessage });
      throw new Error(errorMessage);
    }
  },

  removeItem: async (productId: string) => {
    const userId = getCurrentUserId();
    if (!userId) {
      throw new Error("Vui lòng đăng nhập để thao tác giỏ hàng.");
    }

    try {
      const response = await cartService.removeFromCart(userId, productId);
      set({ cart: response, error: null });
    } catch (err) {
      set({ error: (err as Error).message });
    }
  },

  updateItem: async (productId: string, quantity: number) => {
    const userId = getCurrentUserId();
    if (!userId) {
      throw new Error("Vui lòng đăng nhập để thao tác giỏ hàng.");
    }

    try {
      const response = await cartService.updateCartItem(userId, productId, quantity);
      set({ cart: response, error: null });
    } catch (err) {
      const errorMessage = (err as AxiosError<{message: string}>).response?.data?.message || (err as Error).message;
      set({ error: errorMessage });
      throw new Error(errorMessage);
    }
  },

  clear: async () => {
    const userId = getCurrentUserId();
    if (!userId) {
      set({ cart: null });
      return;
    }

    await cartService.clearCart(userId);
    set({ cart: { userId, items: [], totalItems: 0, totalPrice: 0 }, error: null });
  },

  initSessionAndCart: async () => {
    const userId = getCurrentUserId();
    if (userId) {
      await get().fetchCart();
    }
    set({ isInitialized: true });
  }
}));
