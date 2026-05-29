import apiClient from "@/services/api/apiClient";
import {
  UserProfileResponse,
  UpdateProfileRequest,
  UserAddressResponse,
  UserAddressRequest,
  ApiResponseUserProfileResponse,
  ApiResponseUserAddressResponse,
  ApiResponseListUserAddressResponse,
  ApiResponseVoid,
} from "@/features/auth/types";

export async function getUserProfileService(
  userId: string,
): Promise<UserProfileResponse> {
  const response = await apiClient.get<ApiResponseUserProfileResponse>(
    `/v1/users/${userId}/profile`,
  );
  if (response.status === 200 && response.data.success) {
    return response.data.data;
  }
  throw new Error(response.data.message || "Failed to get user profile");
}

export async function updateUserProfileService(
  userId: string,
  updateData: UpdateProfileRequest,
): Promise<UserProfileResponse> {
  const response = await apiClient.put<ApiResponseUserProfileResponse>(
    `/v1/users/${userId}/profile`,
    updateData,
  );
  if (response.status === 200 && response.data.success) {
    return response.data.data;
  }
  throw new Error(response.data.message || "Failed to update user profile");
}

export async function getUserAddressesService(
  userId: string,
): Promise<UserAddressResponse[]> {
  const response = await apiClient.get<ApiResponseListUserAddressResponse>(
    `/v1/users/${userId}/addresses`,
  );
  if (response.status === 200 && response.data.success) {
    return response.data.data;
  }
  throw new Error(response.data.message || "Failed to get user addresses");
}

export async function addUserAddressService(
  userId: string,
  addressData: UserAddressRequest,
): Promise<UserAddressResponse> {
  const response = await apiClient.post<ApiResponseUserAddressResponse>(
    `/v1/users/${userId}/addresses`,
    addressData,
  );
  if (response.status === 200 && response.data.success) {
    return response.data.data;
  }
  throw new Error(response.data.message || "Failed to add user address");
}

export async function updateUserAddressService(
  userId: string,
  addressId: number,
  addressData: UserAddressRequest,
): Promise<UserAddressResponse> {
  const response = await apiClient.put<ApiResponseUserAddressResponse>(
    `/v1/users/${userId}/addresses/${addressId}`,
    addressData,
  );
  if (response.status === 200 && response.data.success) {
    return response.data.data;
  }
  throw new Error(response.data.message || "Failed to update user address");
}

export async function deleteUserAddressService(
  userId: string,
  addressId: number,
): Promise<void> {
  const response = await apiClient.delete<ApiResponseVoid>(
    `/v1/users/${userId}/addresses/${addressId}`,
  );
  if (response.status === 200 && response.data.success) {
    return;
  }
  throw new Error(response.data.message || "Failed to delete user address");
}
