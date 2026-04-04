export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  displayName?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  userId: number;
  username: string;
  email: string;
  displayName: string;
}

export interface CurrentUser {
  id: number;
  username: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  status: string;
  lastSeenAt: string | null;
  createdAt: string;
}
