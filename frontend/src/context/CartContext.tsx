import React, { createContext, useState, useCallback, useEffect, useMemo } from 'react';
import { cartService } from '../services/api/cartService';
import { AxiosError } from 'axios';
import { CartResponse, CartItemRequest } from '../types/cart';
import { refreshSession } from '../services/api/apiClient';

export interface CartContextType {
  cart: CartResponse | null;
  isLoading: boolean;
  error: string | null;
  fetchCart: () => Promise<void>;
  addItem: (item: CartItemRequest) => Promise<void>;
  removeItem: (cartItemId: number) => Promise<void>;
  updateItem: (cartItemId: number, quantity: number) => Promise<void>;
  clear: () => Promise<void>;
}

export const CartContext = createContext<CartContextType | undefined>(undefined);

const getCurrentUserId = () => localStorage.getItem("userId");

const isTestEnv = typeof process !== 'undefined' && process.env.NODE_ENV === 'test';

export const CartProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isInitialized, setIsInitialized] = useState(isTestEnv);

  const fetchCart = useCallback(async () => {
    const userId = getCurrentUserId();
    if (!userId) {
      setCart(null);
      return;
    }

    setIsLoading(true);
    try {
      const response = await cartService.getCart(userId);
      setCart(response);
      setError(null);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const addItem = useCallback(async (item: CartItemRequest) => {
    const userId = getCurrentUserId();
    if (!userId) {
      throw new Error("Vui lòng dang nhap để thêm sản phẩm vào giỏ hàng.");
    }

    try {
      const response = await cartService.addToCart(userId, item);
      setCart(response);
      setError(null);
    } catch (err) {
      const errorMessage = (err as AxiosError<{message: string}>).response?.data?.message || (err as Error).message;
      setError(errorMessage);
      throw new Error(errorMessage);
    }
  }, []);

  const removeItem = useCallback(async (cartItemId: number) => {
    const userId = getCurrentUserId();
    if (!userId) {
      throw new Error("Vui lòng đăng nhập để thao tác giỏ hàng.");
    }

    try {
      const response = await cartService.removeFromCart(userId, cartItemId);
      setCart(response);
      setError(null);
    } catch (err) {
      setError((err as Error).message);
    }
  }, []);

  const updateItem = useCallback(async (cartItemId: number, quantity: number) => {
    const userId = getCurrentUserId();
    if (!userId) {
      throw new Error("Vui lòng đăng nhập để thao tác giỏ hàng.");
    }

    try {
      const response = await cartService.updateCartItem(userId, cartItemId, quantity);
      setCart(response);
      setError(null);
    } catch (err) {
      const errorMessage = (err as AxiosError<{message: string}>).response?.data?.message || (err as Error).message;
      setError(errorMessage);
      throw new Error(errorMessage);
    }
  }, []);

  const clear = useCallback(async () => {
    const userId = getCurrentUserId();
    if (!userId) {
      setCart(null);
      return;
    }

    await cartService.clearCart(userId);
    setCart({ userId, items: [], totalItems: 0, totalPrice: 0 });
    setError(null);
  }, []);

  useEffect(() => {
    const initSessionAndCart = async () => {
      const userId = getCurrentUserId();
      if (userId) {
        try {
          await refreshSession();
        } catch (e) {
          // ignore
        }
        await fetchCart();
      }
      setIsInitialized(true);
    };

    initSessionAndCart();
  }, [fetchCart]);

  const contextValue = useMemo(() => ({
    cart,
    isLoading,
    error,
    fetchCart,
    addItem,
    removeItem,
    updateItem,
    clear
  }), [cart, isLoading, error, fetchCart, addItem, removeItem, updateItem, clear]);

  if (!isInitialized && getCurrentUserId()) {
    return (
      <div className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-white/40 backdrop-blur-md">
        <div className="relative flex items-center justify-center">
          <div className="h-12 w-12 rounded-full border-2 border-zinc-950/10 border-t-zinc-950 animate-spin" />
        </div>
        <p className="mt-4 text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500 animate-pulse">
          Aetheris is Loading
        </p>
      </div>
    );
  }

  return (
    <CartContext.Provider value={contextValue}>
      {children}
    </CartContext.Provider>
  );
};
