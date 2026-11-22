import type { FormEvent } from 'react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as patentsApi from '../api/patents';
import * as projectsApi from '../api/projects';
import * as referenceApi from '../api/reference';
import { ErrorAlert, Loading, SuccessAlert } from '../components/Feedback';
import { Modal } from '../components/Modal';
import { Pagination } from '../components/Pagination';
import { useI18n } from '../i18n/useI18n';
import type { PatentCreateRequest } from '../types';
import { ApiError } from '../types';

function formatDate(value: string | null, locale: string): string {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleDateString(locale);
  } catch {
    return value;
  }
}

interface CreatePatentFormState {
  title: string;
  abstractText: string;
  publicationNumber: string;
  publicationDate: string;
  applicationNumber: string;
  applicationDate: string;
  applicationCountry: string;
  language: string;
  mainClassification: string;
  applicantsText: string;
  inventorsText: string;
}

const emptyCreateForm: CreatePatentFormState = {
  title: '',
  abstractText: '',
  publicationNumber: '',
  publicationDate: '',
  applicationNumber: '',
  applicationDate: '',
  applicationCountry: '',
  language: 'PT',
  mainClassification: '',
  applicantsText: '',
  inventorsText: '',
};

function parseListInput(raw: string): string[] {
  return raw
    .split(/[\n;]+/)
    .map((s) => s.trim())
    .filter(Boolean);
}

export function PatentsPage() {
  const { id: projectId = '' } = useParams();
  const queryClient = useQueryClient();
  const { locale, t } = useI18n();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [countryInput, setCountryInput] = useState('');
  const [search, setSearch] = useState('');
  const [countryFilter, setCountryFilter] = useState('');
  const [blacklisted, setBlacklisted] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState<CreatePatentFormState>(emptyCreateForm);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const size = 20;

  const projectQuery = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectsApi.getProject(projectId),
    enabled: !!projectId,
  });

  const countriesQuery = useQuery({
    queryKey: ['reference-countries'],
    queryFn: referenceApi.listCountries,
    enabled: createOpen,
  });

  const patentsQuery = useQuery({
    queryKey: ['patents', projectId, page, size, search, countryFilter, blacklisted],
    queryFn: () =>
      patentsApi.listPatents(projectId, {
        page,
        size,
        q: search || undefined,
        country: countryFilter || undefined,
        blacklisted,
      }),
    enabled: !!projectId,
  });

  const createMutation = useMutation({
    mutationFn: (payload: PatentCreateRequest) => patentsApi.createPatent(projectId, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['patents', projectId] });
      void queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      setCreateOpen(false);
      setForm(emptyCreateForm);
      setFeedback(t('patents.created'));
      setError(null);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : t('patents.createError'));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (patentId: string) => patentsApi.deletePatent(patentId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['patents', projectId] });
      void queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      setFeedback(t('patents.deleted'));
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : t('patents.deleteError'));
    },
  });

  function onCreateSubmit(e: FormEvent) {
    e.preventDefault();
    if (!form.title.trim() || (!form.publicationNumber.trim() && !form.applicationNumber.trim())) {
      setError(t('patents.validationRequired'));
      return;
    }
    createMutation.mutate({
      title: form.title.trim(),
      abstractText: form.abstractText.trim() || undefined,
      publicationNumber: form.publicationNumber.trim() || undefined,
      publicationDate: form.publicationDate || undefined,
      applicationNumber: form.applicationNumber.trim() || undefined,
      applicationDate: form.applicationDate || undefined,
      applicationCountry: form.applicationCountry.trim() || undefined,
      language: form.language.trim() || undefined,
      mainClassification: form.mainClassification.trim() || undefined,
      applicants: parseListInput(form.applicantsText),
      inventors: parseListInput(form.inventorsText),
    });
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>{t('patents.title')}</h1>
          <p>
            {t('patents.projectLabel')}{' '}
            <strong>{projectQuery.data?.title || projectId}</strong>
            {projectQuery.data
              ? ` · ${t('patents.countSuffix', { count: projectQuery.data.patentCount })}`
              : ''}
          </p>
        </div>
        <div className="stack-actions">
          <button
            type="button"
            className="btn"
            data-testid="patent-create"
            onClick={() => {
              setForm(emptyCreateForm);
              setError(null);
              setCreateOpen(true);
            }}
          >
            {t('patents.new')}
          </button>
          <Link className="btn btn-secondary" to={`/projects/${projectId}/import`}>
            {t('patents.import')}
          </Link>
          <Link className="btn btn-secondary" to={`/projects/${projectId}/reports`}>
            {t('patents.reports')}
          </Link>
          <Link className="btn btn-secondary" to="/projects">
            {t('common.back')}
          </Link>
        </div>
      </div>

      {feedback && <SuccessAlert message={feedback} />}
      {error && <ErrorAlert message={error} />}
      {patentsQuery.isError && (
        <ErrorAlert
          message={
            patentsQuery.error instanceof Error
              ? patentsQuery.error.message
              : t('patents.loadError')
          }
        />
      )}

      <div className="card" style={{ marginBottom: '1rem' }}>
        <form
          className="stack-actions"
          onSubmit={(e) => {
            e.preventDefault();
            setPage(0);
            setSearch(q.trim());
            setCountryFilter(countryInput.trim().toUpperCase());
          }}
        >
          <div className="field" style={{ flex: 2, minWidth: 200 }}>
            <label htmlFor="q">{t('patents.searchTitle')}</label>
            <input id="q" value={q} onChange={(e) => setQ(e.target.value)} />
          </div>
          <div className="field" style={{ flex: 1, minWidth: 120 }}>
            <label htmlFor="country-filter">{t('patents.countryFilter')}</label>
            <input
              id="country-filter"
              value={countryInput}
              onChange={(e) => setCountryInput(e.target.value)}
              placeholder="BR, DE, US…"
            />
          </div>
          <label className="field-inline" style={{ alignSelf: 'end', paddingBottom: '0.5rem' }}>
            <input
              type="checkbox"
              checked={blacklisted}
              onChange={(e) => {
                setPage(0);
                setBlacklisted(e.target.checked);
              }}
            />
            {t('patents.showBlacklisted')}
          </label>
          <button type="submit" className="btn" style={{ alignSelf: 'end' }}>
            {t('common.search')}
          </button>
        </form>
      </div>

      <div className="card">
        {patentsQuery.isLoading ? (
          <Loading />
        ) : !patentsQuery.data?.content.length ? (
          <div className="empty-block">{t('patents.empty')}</div>
        ) : (
          <>
            <div className="table-wrap">
              <table className="data" data-testid="patents-table">
                <thead>
                  <tr>
                    <th>{t('patents.col.title')}</th>
                    <th>{t('patents.col.publication')}</th>
                    <th>{t('patents.col.country')}</th>
                    <th>{t('patents.col.classification')}</th>
                    <th>{t('patents.col.applicants')}</th>
                    <th>{t('patents.col.inventors')}</th>
                    <th>{t('common.actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {patentsQuery.data.content.map((patent) => (
                    <tr key={patent.id} data-testid={`patent-row-${patent.id}`}>
                      <td>
                        <Link
                          to={`/projects/${projectId}/patents/${patent.id}`}
                          data-testid={`patent-link-${patent.id}`}
                        >
                          {patent.title || t('patents.noTitle')}
                        </Link>
                      </td>
                      <td>
                        <div>{patent.publicationNumber || '—'}</div>
                        <div className="muted">{formatDate(patent.publicationDate, locale)}</div>
                      </td>
                      <td>{patent.applicationCountry || '—'}</td>
                      <td>{patent.mainClassification || '—'}</td>
                      <td className="muted">
                        {(patent.applicants || []).slice(0, 3).join(', ') || '—'}
                        {(patent.applicants || []).length > 3 ? '…' : ''}
                      </td>
                      <td className="muted">
                        {(patent.inventors || []).slice(0, 3).join(', ') || '—'}
                        {(patent.inventors || []).length > 3 ? '…' : ''}
                      </td>
                      <td>
                        <div className="stack-actions">
                          <Link
                            className="btn btn-secondary btn-sm"
                            to={`/projects/${projectId}/patents/${patent.id}`}
                          >
                            {t('patents.detail')}
                          </Link>
                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            onClick={() => {
                              if (confirm(t('patents.confirmDelete'))) {
                                deleteMutation.mutate(patent.id);
                              }
                            }}
                          >
                            {t('common.delete')}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={patentsQuery.data.page}
              totalPages={patentsQuery.data.totalPages}
              totalElements={patentsQuery.data.totalElements}
              onChange={setPage}
            />
          </>
        )}
      </div>

      <Modal title={t('patents.new')} open={createOpen} onClose={() => setCreateOpen(false)} wide>
        <form className="form-grid" onSubmit={onCreateSubmit} data-testid="patent-create-form">
          <div className="field">
            <label htmlFor="new-patent-title">{t('patents.field.title')}</label>
            <input
              id="new-patent-title"
              data-testid="new-patent-title"
              value={form.title}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              required
            />
          </div>
          <div className="form-grid two">
            <div className="field">
              <label htmlFor="new-patent-pubnum">{t('patents.field.pubNumber')}</label>
              <input
                id="new-patent-pubnum"
                data-testid="new-patent-pubnum"
                value={form.publicationNumber}
                onChange={(e) => setForm((f) => ({ ...f, publicationNumber: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="new-patent-pubdate">{t('patents.field.pubDate')}</label>
              <input
                id="new-patent-pubdate"
                type="date"
                value={form.publicationDate}
                onChange={(e) => setForm((f) => ({ ...f, publicationDate: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="new-patent-appnum">{t('patents.field.appNumber')}</label>
              <input
                id="new-patent-appnum"
                data-testid="new-patent-appnum"
                value={form.applicationNumber}
                onChange={(e) => setForm((f) => ({ ...f, applicationNumber: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="new-patent-appdate">{t('patents.field.appDate')}</label>
              <input
                id="new-patent-appdate"
                type="date"
                value={form.applicationDate}
                onChange={(e) => setForm((f) => ({ ...f, applicationDate: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="new-patent-country">{t('patents.field.appCountry')}</label>
              <select
                id="new-patent-country"
                value={form.applicationCountry}
                onChange={(e) => setForm((f) => ({ ...f, applicationCountry: e.target.value }))}
              >
                <option value="">—</option>
                {(countriesQuery.data || []).map((c) => (
                  <option key={c.acronym} value={c.acronym}>
                    {c.acronym} — {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="new-patent-maincls">{t('patents.field.mainClass')}</label>
              <input
                id="new-patent-maincls"
                value={form.mainClassification}
                onChange={(e) => setForm((f) => ({ ...f, mainClassification: e.target.value }))}
                placeholder="H01L 31/04"
              />
            </div>
          </div>
          <div className="form-grid two">
            <div className="field">
              <label htmlFor="new-patent-applicants">{t('patents.field.applicants')}</label>
              <textarea
                id="new-patent-applicants"
                rows={2}
                value={form.applicantsText}
                onChange={(e) => setForm((f) => ({ ...f, applicantsText: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="new-patent-inventors">{t('patents.field.inventors')}</label>
              <textarea
                id="new-patent-inventors"
                rows={2}
                value={form.inventorsText}
                onChange={(e) => setForm((f) => ({ ...f, inventorsText: e.target.value }))}
              />
            </div>
          </div>
          <div className="field">
            <label htmlFor="new-patent-abstract">{t('patents.field.abstract')}</label>
            <textarea
              id="new-patent-abstract"
              rows={3}
              value={form.abstractText}
              onChange={(e) => setForm((f) => ({ ...f, abstractText: e.target.value }))}
            />
          </div>
          <div className="stack-actions">
            <button
              type="submit"
              className="btn"
              data-testid="patent-create-save"
              disabled={createMutation.isPending}
            >
              {createMutation.isPending ? t('common.saving') : t('common.save')}
            </button>
            <button type="button" className="btn btn-secondary" onClick={() => setCreateOpen(false)}>
              {t('common.cancel')}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
