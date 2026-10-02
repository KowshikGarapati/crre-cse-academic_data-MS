import type { ApiResponse, AuthResponse, LoginRequest, UserSummary } from '../types/auth.types';

export async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers || {});
  if (!headers.has('Content-Type') && options.body && typeof options.body === 'string') {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
    credentials: 'include', // Ensure JSESSIONID cookie is sent and received
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const errorMessage = data?.message || `Request failed with status ${response.status}`;
    throw new Error(errorMessage);
  }

  return data;
}

export const authApi = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const res = await request<ApiResponse<AuthResponse>>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    });
    return res.data;
  },

  async getCurrentUser(): Promise<UserSummary> {
    const res = await request<ApiResponse<UserSummary>>('/api/v1/auth/me', {
      method: 'GET',
    });
    return res.data;
  },

  async logout(): Promise<void> {
    await request<ApiResponse<void>>('/api/v1/auth/logout', {
      method: 'POST',
    });
  },
};
