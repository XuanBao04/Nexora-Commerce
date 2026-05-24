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
