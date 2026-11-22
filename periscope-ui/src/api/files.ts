import { apiRequest, getApiBase, getStoredToken } from './client';
import type { Patent } from '../types';

export function downloadFileUrl(fileId: string): string {
  return `${getApiBase()}/files/${fileId}`;
}

export async function downloadFile(fileId: string, filename?: string): Promise<void> {
  const token = getStoredToken();
  const response = await fetch(downloadFileUrl(fileId), {
    credentials: 'include',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!response.ok) {
    throw new Error(`Download failed (${response.status})`);
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename || fileId;
  a.click();
  URL.revokeObjectURL(url);
}

export function uploadPatentFile(
  patentId: string,
  file: File,
  kind: 'presentation' | 'patentInfo' = 'presentation',
): Promise<Patent> {
  const formData = new FormData();
  formData.append('file', file);
  return apiRequest<Patent>(`/files/patents/${patentId}?kind=${kind}`, {
    method: 'POST',
    formData,
  });
}
