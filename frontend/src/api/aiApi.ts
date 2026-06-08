import apiClient from "@/services/api/apiClient";
import { ApiResponse } from "@/types/apiResponse";
import { Product } from "@/features/products/types/product";

export const searchAiProducts = async (query: string, limit = 5): Promise<Product[]> => {
    const response = await apiClient.get<ApiResponse<Product[]>>(`/v1/search`, {
        params: { q: query, limit }
    });
    return response.data.data || [];
};
