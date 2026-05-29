import apiClient, { setAccessToken } from "@/services/api/apiClient";
import {
  ApiResponseAuthResponse,
  AuthResponse,
  ApiResponseVoid,
  UserProfileResponse,
  ApiResponseUserProfileResponse,
} from "@/features/auth/types";

export async function loginService(
  username: string,
  password: string,
): Promise<AuthResponse> {
  const response = await apiClient.post<ApiResponseAuthResponse>(
    "/v1/authentications/sessions",
    {
      username,
      password,
    },
  );
  if (response.status === 200 && response.data.success) {
    setAccessToken(response.data.data.token);
    return response.data.data;
  }
  throw new Error(response.data.message || "Login failed");
}

export async function refreshTokenService(): Promise<string> {
  const response = await apiClient.post<ApiResponseAuthResponse>(
    "/v1/authentications/tokens",
  );
  if (response.status === 200 && response.data.success) {
    setAccessToken(response.data.data.token);
    return response.data.data.token;
  }
  throw new Error(response.data.message || "Token refresh failed");
}

export async function logoutService(): Promise<void> {
  const response = await apiClient.delete<ApiResponseVoid>(
    "/v1/authentications/sessions",
  );
  if (response.status === 200 && response.data.success) {
    setAccessToken(null);
    return;
  }
  throw new Error(response.data.message || "Logout failed");
}

export async function getCurrentUserService(): Promise<UserProfileResponse> {
  const response = await apiClient.get<ApiResponseUserProfileResponse>(
    "/v1/authentications/profiles/current",
  );
  if (response.status === 200 && response.data.success) {
    return response.data.data;
  }
  throw new Error(response.data.message || "Failed to get current user");
}
