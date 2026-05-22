import apiClient from "./apiClient";

export interface RegisterRequest {
  username: string;
  email: string;
  password?: string;
  fullName?: string;
}

export interface RegisterResponse {
  userId: number;
  username: string;
  email: string;
  role: string;
  message: string;
}

export async function registerService(
  request: RegisterRequest,
): Promise<RegisterResponse> {
  // Use username as fullName if fullName is not provided
  if (!request.fullName) {
    request.fullName = request.username;
  }

  const response = await apiClient.post<RegisterResponse>(
    "/auth/register",
    request
  );
  
  if (response.status === 201) {
    return response.data;
  }
  
  throw new Error("Register failed with status " + response.status);
}
