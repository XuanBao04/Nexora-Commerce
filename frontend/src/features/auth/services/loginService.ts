import apiClient, { setAccessToken } from "@/services/api/apiClient";
import { ApiResponseAuthResponse, AuthResponse } from "@/features/auth/types";

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
