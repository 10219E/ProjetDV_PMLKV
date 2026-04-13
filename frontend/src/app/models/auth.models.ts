export interface AuthLoginRequest {
  login?: string;
  password?: string;
}

export interface AuthLoginResponse {
  tokenType: string;
  accessToken: string;
  expiresIn: number;
}

export interface UserRegistrationRequest {
  login?: string;
  password?: string;
  email?: string;
  [key: string]: any;
}

