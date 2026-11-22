import { apiRequest } from './client';
import type {
  ImportResult,
  ImporterType,
  Page,
  Patent,
  PatentCreateRequest,
  PatentUpdateRequest,
} from '../types';

export interface PatentListParams {
  page?: number;
  size?: number;
  q?: string;
  country?: string;
  blacklisted?: boolean;
}

export function listPatents(
  projectId: string,
  params: PatentListParams = {},
): Promise<Page<Patent>> {
  const search = new URLSearchParams();
  if (params.page !== undefined) search.set('page', String(params.page));
  if (params.size !== undefined) search.set('size', String(params.size));
  if (params.q) search.set('q', params.q);
  if (params.country) search.set('country', params.country);
  if (params.blacklisted !== undefined) search.set('blacklisted', String(params.blacklisted));
  const qs = search.toString();
  return apiRequest<Page<Patent>>(
    `/projects/${projectId}/patents${qs ? `?${qs}` : ''}`,
  );
}

export function createPatent(projectId: string, data: PatentCreateRequest): Promise<Patent> {
  return apiRequest<Patent>(`/projects/${projectId}/patents`, {
    method: 'POST',
    body: data,
  });
}

export function getPatent(id: string): Promise<Patent> {
  return apiRequest<Patent>(`/patents/${id}`);
}

export function updatePatent(id: string, data: PatentUpdateRequest): Promise<Patent> {
  return apiRequest<Patent>(`/patents/${id}`, { method: 'PUT', body: data });
}

export function deletePatent(id: string): Promise<void> {
  return apiRequest<void>(`/patents/${id}`, { method: 'DELETE' });
}

export function importPatents(
  projectId: string,
  file: File,
  type: ImporterType,
): Promise<ImportResult> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('type', type);
  return apiRequest<ImportResult>(`/projects/${projectId}/patents/import`, {
    method: 'POST',
    formData,
  });
}
