import { apiRequest } from './client';
import type { Page, Patent, Report, ReportName, ReportQueryParams } from '../types';

export function getReport(
  projectId: string,
  name: ReportName,
  paramsOrLimit: ReportQueryParams | number = {},
): Promise<Report> {
  const params: ReportQueryParams =
    typeof paramsOrLimit === 'number' ? { limit: paramsOrLimit } : paramsOrLimit;
  const search = new URLSearchParams();

  if (params.limit !== undefined) search.set('limit', String(params.limit));
  if (params.dateType) search.set('dateType', params.dateType);
  if (params.startDate) search.set('startDate', params.startDate);
  if (params.endDate) search.set('endDate', params.endDate);
  if (params.complete !== undefined) search.set('complete', String(params.complete));
  if (params.applicantType) search.set('applicantType', params.applicantType);
  if (params.applicantName) search.set('applicantName', params.applicantName);
  if (params.inventorName) search.set('inventorName', params.inventorName);
  if (params.ipcLevel) search.set('ipcLevel', params.ipcLevel);
  if (params.classificationType) search.set('classificationType', params.classificationType);

  const qs = search.toString();
  return apiRequest<Report>(`/projects/${projectId}/reports/${name}${qs ? `?${qs}` : ''}`);
}

export function getNationalPriorityDocs(
  projectId: string,
  params: { page?: number; size?: number; q?: string } = {},
): Promise<Page<Patent>> {
  const search = new URLSearchParams();
  if (params.page !== undefined) search.set('page', String(params.page));
  if (params.size !== undefined) search.set('size', String(params.size));
  if (params.q) search.set('q', params.q);
  const qs = search.toString();
  return apiRequest<Page<Patent>>(
    `/projects/${projectId}/reports/docs-national-priority${qs ? `?${qs}` : ''}`,
  );
}

export function autocompleteApplicants(projectId: string, q: string): Promise<string[]> {
  const qs = new URLSearchParams({ q }).toString();
  return apiRequest<string[]>(`/projects/${projectId}/reports/applicants?${qs}`);
}

export function autocompleteInventors(projectId: string, q: string): Promise<string[]> {
  const qs = new URLSearchParams({ q }).toString();
  return apiRequest<string[]>(`/projects/${projectId}/reports/inventors?${qs}`);
}
