export interface AuthResponse {
  userId: string;
  username: string;
  fullName: string;
  email: string;
  role: string;
  message: string;
  token: string;
}

export interface PaginationInfo {
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiResponseAuthResponse {
  success: boolean;
  timestamp: string;
  status: number;
  message: string;
  data: AuthResponse;
  pagination?: PaginationInfo;
}

export interface ApiResponseVoid {
  success: boolean;
  timestamp: string;
  status: number;
  message: string;
  data: null;
}

export interface LoginRequest {
  username: string;
  password?: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password?: string;
  fullName?: string;
}

// User Profile Types
export interface UserProfileResponse {
  id: string;
  username: string;
  fullName: string;
  email: string;
  createdAt: string;
  roles?: string[];
}

export interface UpdateProfileRequest {
  fullName?: string;
  email?: string;
  password?: string;
}

export interface UserAddressResponse {
  id: number;
  receiverName: string;
  phoneNumber: string;
  addressLine: string;
  isDefault: boolean;
}

export interface UserAddressRequest {
  receiverName: string;
  phoneNumber: string;
  addressLine: string;
  isDefault?: boolean;
}

export interface ApiResponseUserProfileResponse {
  success: boolean;
  timestamp: string;
  status: number;
  message: string;
  data: UserProfileResponse;
  pagination?: PaginationInfo;
}

export interface ApiResponseUserAddressResponse {
  success: boolean;
  timestamp: string;
  status: number;
  message: string;
  data: UserAddressResponse;
  pagination?: PaginationInfo;
}

export interface ApiResponseListUserAddressResponse {
  success: boolean;
  timestamp: string;
  status: number;
  message: string;
  data: UserAddressResponse[];
  pagination?: PaginationInfo;
}
