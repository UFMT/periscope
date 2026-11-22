export interface User {
  id: string;
  username: string;
  firstname: string | null;
  lastname: string | null;
  email: string | null;
  userLevel: 'ADMIN' | 'USER' | string;
  mustChangePassword?: boolean;
}

export interface UserRequest {
  username?: string;
  password?: string;
  firstname?: string | null;
  lastname?: string | null;
  email?: string | null;
  userLevel?: 'ADMIN' | 'USER' | string;
}

export interface UserSummary {
  id: string;
  username: string;
  firstname: string | null;
  lastname: string | null;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface StateItem {
  acronym: string;
  name: string;
  region: string | null;
}

export interface CountryItem {
  acronym: string;
  name: string;
  states: StateItem[];
}

export interface Project {
  id: string;
  title: string;
  description: string | null;
  isPublic: boolean | null;
  createdAt: string | null;
  updateAt: string | null;
  ownerId: string | null;
  ownerName: string | null;
  patentCount: number;
  observerIds: string[];
  observers?: UserSummary[];
}

export interface ProjectRequest {
  title: string;
  description?: string | null;
  isPublic?: boolean | null;
  observerIds?: string[];
}

export interface PriorityItem {
  value: string | null;
  countryAcronym: string | null;
  date: string | null;
}

export interface Patent {
  id: string;
  title: string | null;
  abstractText: string | null;
  publicationNumber: string | null;
  publicationDate: string | null;
  applicationNumber: string | null;
  applicationDate: string | null;
  applicationCountry: string | null;
  mainClassification: string | null;
  blacklisted: boolean | null;
  completed: boolean | null;
  projectId: string | null;
  applicants: string[];
  inventors: string[];
  presentationFileId: string | null;
  patentInfoFileId: string | null;
  language?: string | null;
  classifications?: string[];
  priorities?: PriorityItem[];
}

export interface PatentMutationRequest {
  title?: string | null;
  abstractText?: string | null;
  publicationNumber?: string | null;
  publicationDate?: string | null;
  applicationNumber?: string | null;
  applicationDate?: string | null;
  applicationCountry?: string | null;
  language?: string | null;
  mainClassification?: string | null;
  classifications?: string[];
  applicants?: string[];
  inventors?: string[];
  priorities?: { value: string; countryAcronym: string; date: string }[];
  blacklisted?: boolean | null;
  completed?: boolean | null;
}

export type PatentUpdateRequest = PatentMutationRequest;
export type PatentCreateRequest = PatentMutationRequest;

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ImportResult {
  imported: number;
  importer: string;
  fileName: string;
  messages: string[];
}

export type ImporterType = 'DPMA' | 'ESPACENET' | 'PATENTSCOPE';

export interface Rule {
  id: string;
  name: string;
  acronym: string | null;
  type: string;
  substitutions: string[] | Set<string> | null;
  countryAcronym: string | null;
  stateAcronym?: string | null;
  nature: string | null;
  projectId: string | null;
}

export interface RuleRequest {
  name: string;
  acronym?: string | null;
  type: 'APPLICANT' | 'INVENTOR' | string;
  substitutions?: string[];
  countryAcronym?: string | null;
  stateAcronym?: string | null;
  nature?: string | null;
}

export interface HarmonizationEntity {
  name: string;
  acronym: string | null;
  countryAcronym: string | null;
  stateAcronym: string | null;
  nature: string | null;
  harmonized: boolean | null;
  documentCount: number;
}

export interface SuggestionsResponse {
  type: string;
  query: string;
  suggestions: string[];
}

export interface ReportItem {
  key: string;
  value: number;
  description?: string | null;
}

export interface Report {
  name: string;
  label: string;
  items: ReportItem[];
}

export type ReportName =
  | 'main-applicant'
  | 'main-inventor'
  | 'main-ipc'
  | 'application-date'
  | 'publication-date'
  | 'priority-country'
  | 'priority-date'
  | 'docs-national-priority'
  | 'technological-profile-applicant'
  | 'technological-profile-inventor'
  | 'nature-applicant'
  | 'state-distribution'
  | 'region-distribution';

export interface ReportQueryParams {
  limit?: number;
  dateType?: 'publication' | 'application';
  startDate?: string;
  endDate?: string;
  complete?: boolean;
  applicantType?: string;
  applicantName?: string;
  inventorName?: string;
  ipcLevel?: 'section' | 'class' | 'subclass' | 'group' | 'subgroup';
  classificationType?: 'IPC' | 'CPC';
  page?: number;
  size?: number;
  q?: string;
}

export interface ApiErrorBody {
  error?: string;
  status?: number;
  message?: string;
}

export class ApiError extends Error {
  status: number;
  body: ApiErrorBody | null;

  constructor(status: number, message: string, body: ApiErrorBody | null = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}
