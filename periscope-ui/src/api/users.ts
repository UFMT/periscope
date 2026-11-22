import { apiRequest } from './client';
import type { User, UserRequest } from '../types';

export function listUsers(): Promise<User[]> {
  return apiRequest<User[]>('/users');
}

export function getUser(id: string): Promise<User> {
  return apiRequest<User>(`/users/${id}`);
}

export function createUser(data: UserRequest): Promise<User> {
  return apiRequest<User>('/users', { method: 'POST', body: data });
}

export function updateUser(id: string, data: UserRequest): Promise<User> {
  return apiRequest<User>(`/users/${id}`, { method: 'PUT', body: data });
}

export function deleteUser(id: string): Promise<void> {
  return apiRequest<void>(`/users/${id}`, { method: 'DELETE' });
}
