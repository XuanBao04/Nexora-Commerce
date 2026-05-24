import apiClient, { setAccessToken } from "@/services/api/apiClient";
import { ApiResponseAuthResponse, AuthResponse, RegisterRequest } from "@/features/auth/types";

export async function registerService(
  request: RegisterRequest,
): Promise<AuthResponse> {
  // Use username as fullName if fullName is not provided
  if (!request.fullName) {
    request.fullName = request.username;
  }

  const response = await apiClient.post<ApiResponseAuthResponse>(
    "/v1/authentications/registrations",
    request
  );
  
  if ((response.status === 201 || response.status === 200) && response.data.success) {
    setAccessToken(response.data.data.token);
    return response.data.data;
  }
  
  throw new Error(response.data.message || "Registration failed");
}
