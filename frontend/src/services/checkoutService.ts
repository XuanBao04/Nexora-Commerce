import apiClient from "@/services/api/apiClient";
import { ApiResponse } from "@/types/apiResponse";

export interface CheckoutRequest {
  userId: string;
  items: Array<{
    productId: string;
    quantity: number;
  }>;
  shippingAddress: string;
  city: string;
  district: string;
  ward: string;
  postalCode?: string;
  phoneNumber: string;
  couponCode?: string;
}

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

const CHECKOUT_API = "/v1/checkouts";

export const checkoutService = {
  /**
   * Initiate checkout
   * POST /v1/checkouts
   */
  async checkout(checkoutData: CheckoutRequest): Promise<PaymentResponse> {
    const response = await apiClient.post<ApiResponse<PaymentResponse>>(
      CHECKOUT_API,
      checkoutData,
    );
    return response.data.data;
  },

  /**
   * Process payment for an order
   * POST /v1/checkouts/orders/{orderId}/payments
   */
  async processPayment(
    orderId: string,
    paymentData: PaymentRequest,
  ): Promise<PaymentResponse> {
    const response = await apiClient.post<ApiResponse<PaymentResponse>>(
      `${CHECKOUT_API}/orders/${orderId}/payments`,
      paymentData,
    );
    return response.data.data;
  },
};
