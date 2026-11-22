import { apiRequest } from './client';
import type {
  HarmonizationEntity,
  Page,
  Rule,
  RuleRequest,
  SuggestionsResponse,
} from '../types';

export function getSuggestions(
  projectId: string,
  type: 'applicant' | 'inventor',
  query: string,
): Promise<SuggestionsResponse> {
  const params = new URLSearchParams({ type, query });
  return apiRequest<SuggestionsResponse>(
    `/projects/${projectId}/harmonization/suggestions?${params}`,
  );
}

export function listEntities(
  projectId: string,
  params: { type: 'applicant' | 'inventor'; page?: number; size?: number; q?: string },
): Promise<Page<HarmonizationEntity>> {
  const search = new URLSearchParams({ type: params.type });
  if (params.page !== undefined) search.set('page', String(params.page));
  if (params.size !== undefined) search.set('size', String(params.size));
  if (params.q) search.set('q', params.q);
  return apiRequest<Page<HarmonizationEntity>>(
    `/projects/${projectId}/harmonization/entities?${search}`,
  );
}

export function listRules(
  projectId: string,
  type?: 'applicant' | 'inventor',
): Promise<Rule[]> {
  const qs = type ? `?type=${type}` : '';
  return apiRequest<Rule[]>(`/projects/${projectId}/harmonization/rules${qs}`);
}

export function createRule(projectId: string, data: RuleRequest): Promise<Rule> {
  return apiRequest<Rule>(`/projects/${projectId}/harmonization/rules`, {
    method: 'POST',
    body: data,
  });
}

export function updateRule(projectId: string, ruleId: string, data: RuleRequest): Promise<Rule> {
  return apiRequest<Rule>(`/projects/${projectId}/harmonization/rules/${ruleId}`, {
    method: 'PUT',
    body: data,
  });
}

export function undoRule(
  projectId: string,
  ruleId: string,
): Promise<{ undone: number; ruleId: string; message: string }> {
  return apiRequest(`/projects/${projectId}/harmonization/rules/${ruleId}/undo`, {
    method: 'POST',
  });
}

export function deleteRule(projectId: string, ruleId: string): Promise<void> {
  return apiRequest<void>(`/projects/${projectId}/harmonization/rules/${ruleId}`, {
    method: 'DELETE',
  });
}

export function applyAllRules(projectId: string): Promise<{ applied: number; message: string }> {
  return apiRequest(`/projects/${projectId}/harmonization/apply`, { method: 'POST' });
}

export function applyRule(
  projectId: string,
  ruleId: string,
): Promise<{ applied: number; ruleId: string; message: string }> {
  return apiRequest(`/projects/${projectId}/harmonization/rules/${ruleId}/apply`, {
    method: 'POST',
  });
}
