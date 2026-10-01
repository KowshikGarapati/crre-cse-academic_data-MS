import type { ApiResponse, AuthResponse, LoginRequest, UserSummary } from '../types/auth.types';

const TOKEN_KEY = 'crre_auth_token';

export const getStoredToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY);
};

export const setStoredToken = (token: string): void => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const removeStoredToken = (): void => {
  localStorage.removeItem(TOKEN_KEY);
};

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = getStoredToken();
  const headers = new Headers(options.headers || {});

  headers.set('Content-Type', 'application/json');
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    if (response.status === 401) {
      removeStoredToken();
    }
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
    if (res.data?.accessToken) {
      setStoredToken(res.data.accessToken);
    }
    return res.data;
  },

  async getCurrentUser(): Promise<UserSummary> {
    const res = await request<ApiResponse<UserSummary>>('/api/v1/auth/me', {
      method: 'GET',
    });
    return res.data;
  },

  async logout(): Promise<void> {
    try {
      await request<ApiResponse<void>>('/api/v1/auth/logout', {
        method: 'POST',
      });
    } finally {
      removeStoredToken();
    }
  },
};
