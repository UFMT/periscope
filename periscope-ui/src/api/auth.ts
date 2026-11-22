import { apiRequest } from './client';
import type { ChangePasswordRequest, LoginRequest, LoginResponse, User } from '../types';

export function login(credentials: LoginRequest): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/auth/login', {
    method: 'POST',
    body: credentials,
    auth: false,
  });
}

export function me(skipUnauthorizedHandler = false): Promise<User> {
  return apiRequest<User>('/auth/me', { skipUnauthorizedHandler });
}

export function refresh(): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/auth/refresh', { method: 'POST' });
}

export function changePassword(data: ChangePasswordRequest): Promise<User> {
  return apiRequest<User>('/auth/change-password', { method: 'POST', body: data });
}

export function logout(): Promise<void> {
  return apiRequest<void>('/auth/logout', { method: 'POST', skipUnauthorizedHandler: true });
}
