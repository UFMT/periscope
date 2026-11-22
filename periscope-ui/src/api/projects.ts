import { apiRequest } from './client';
import type { Project, ProjectRequest } from '../types';

export function listProjects(): Promise<Project[]> {
  return apiRequest<Project[]>('/projects');
}

export function getProject(id: string): Promise<Project> {
  return apiRequest<Project>(`/projects/${id}`);
}

export function createProject(data: ProjectRequest): Promise<Project> {
  return apiRequest<Project>('/projects', { method: 'POST', body: data });
}

export function updateProject(id: string, data: ProjectRequest): Promise<Project> {
  return apiRequest<Project>(`/projects/${id}`, { method: 'PUT', body: data });
}

export function deleteProject(id: string): Promise<void> {
  return apiRequest<void>(`/projects/${id}`, { method: 'DELETE' });
}

export function addObserver(projectId: string, userId: string): Promise<Project> {
  return apiRequest<Project>(`/projects/${projectId}/observers/${userId}`, { method: 'POST' });
}

export function removeObserver(projectId: string, userId: string): Promise<Project> {
  return apiRequest<Project>(`/projects/${projectId}/observers/${userId}`, { method: 'DELETE' });
}
