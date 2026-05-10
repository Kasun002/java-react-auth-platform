export interface UserDto {
  id: number;
  name: string;
  email: string;
  phone: string | null;
  role: string;
  status: string;
  authProvider: "LOCAL" | "AZURE_AD";
  groups: string[];
  roles: string[];
  effectivePermissions: string[];
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: UserDto;
}

export interface ApiResponse<T> {
  status: "SUCCESS" | "FAIL";
  message: string;
  data: T | null;
  errors: string[] | null;
}

export interface AddressRequest {
  addressLine1: string;
  addressLine2?: string;
  street?: string;
  postalCode?: string;
  state?: string;
  country: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  phone?: string;
  password: string;
  addresses: AddressRequest[];
}
