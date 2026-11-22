import { apiRequest } from './client';
import type { CountryItem, UserSummary } from '../types';

export function listCountries(): Promise<CountryItem[]> {
  return apiRequest<CountryItem[]>('/reference/countries');
}

export function listApplicantTypes(): Promise<string[]> {
  return apiRequest<string[]>('/reference/applicant-types');
}

export function listReferenceUsers(): Promise<UserSummary[]> {
  return apiRequest<UserSummary[]>('/reference/users');
}
