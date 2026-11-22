import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { downloadFile, uploadPatentFile } from '../api/files';
import * as patentsApi from '../api/patents';
import * as referenceApi from '../api/reference';
import { ErrorAlert, Loading, SuccessAlert } from '../components/Feedback';
import { useI18n } from '../i18n/useI18n';
import type { Patent, PatentUpdateRequest } from '../types';
import { ApiError } from '../types';

function toDateInput(value: string | null | undefined): string {
  if (!value) return '';
  return value.slice(0, 10);
}

interface PriorityRow {
  value: string;
  countryAcronym: string;
  date: string;
}

interface PatentEditFormState {
  title: string;
  abstractText: string;
  publicationNumber: string;
  publicationDate: string;
  applicationNumber: string;
  applicationDate: string;
  applicationCountry: string;
  language: string;
  mainClassification: string;
  extraClassifications: string;
  applicantsText: string;
  inventorsText: string;
  priorities: PriorityRow[];
  blacklisted: boolean;
  completed: boolean;
}

function toForm(patent: Patent): PatentEditFormState {
  const extras = (patent.classifications || []).filter(
    (c) => c && c !== patent.mainClassification,
  );
  return {
    title: patent.title || '',
    abstractText: patent.abstractText || '',
    publicationNumber: patent.publicationNumber || '',
    publicationDate: toDateInput(patent.publicationDate),
    applicationNumber: patent.applicationNumber || '',
    applicationDate: toDateInput(patent.applicationDate),
    applicationCountry: patent.applicationCountry || '',
    language: patent.language || '',
    mainClassification: patent.mainClassification || '',
    extraClassifications: extras.join(', '),
    applicantsText: (patent.applicants || []).join('\n'),
    inventorsText: (patent.inventors || []).join('\n'),
    priorities: (patent.priorities || []).map((p) => ({
      value: p.value || '',
      countryAcronym: p.countryAcronym || '',
      date: toDateInput(p.date),
    })),
    blacklisted: Boolean(patent.blacklisted),
    completed: Boolean(patent.completed),
  };
}

function parseLines(raw: string): string[] {
  return raw
    .split(/[\n;]+/)
    .map((s) => s.trim())
    .filter(Boolean);
}

function PatentEditForm({
  patent,
  projectId,
}: {
  patent: Patent;
  projectId: string;
}) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { t } = useI18n();
  const [form, setForm] = useState<PatentEditFormState>(() => toForm(patent));
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const countriesQuery = useQuery({
    queryKey: ['reference-countries'],
    queryFn: referenceApi.listCountries,
  });

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload: PatentUpdateRequest = {
        title: form.title,
        abstractText: form.abstractText,
        publicationNumber: form.publicationNumber,
        publicationDate: form.publicationDate,
        applicationNumber: form.applicationNumber,
        applicationDate: form.applicationDate,
        applicationCountry: form.applicationCountry,
        language: form.language,
        mainClassification: form.mainClassification,
        classifications: form.extraClassifications
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean),
        applicants: parseLines(form.applicantsText),
        inventors: parseLines(form.inventorsText),
        priorities: form.priorities.filter(
          (p) => p.value.trim() || p.countryAcronym.trim() || p.date.trim(),
        ),
        blacklisted: form.blacklisted,
        completed: form.completed,
      };
      return patentsApi.updatePatent(patent.id, payload);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['patent', patent.id] });
      void queryClient.invalidateQueries({ queryKey: ['patents', projectId] });
      setFeedback(t('patents.updated'));
      setError(null);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : t('patents.saveError'));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => patentsApi.deletePatent(patent.id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['patents', projectId] });
      navigate(`/projects/${projectId}/patents`);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : t('patents.deleteError'));
    },
  });

  const uploadMutation = useMutation({
    mutationFn: ({ file, kind }: { file: File; kind: 'presentation' | 'patentInfo' }) =>
      uploadPatentFile(patent.id, file, kind),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['patent', patent.id] });
      setFeedback(t('patents.uploadSuccess'));
      setError(null);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : t('patents.uploadError'));
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    saveMutation.mutate();
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 data-testid="patent-detail-title">{t('patents.detailTitle')}</h1>
          <p className="muted">{patent.publicationNumber || patent.applicationNumber || patent.id}</p>
        </div>
        <div className="stack-actions">
          <Link className="btn btn-secondary" to={`/projects/${projectId}/patents`}>
            {t('patents.backToList')}
          </Link>
          <button
            type="button"
            className="btn btn-danger"
            onClick={() => {
              if (confirm(t('patents.confirmDelete'))) deleteMutation.mutate();
            }}
          >
            {t('common.delete')}
          </button>
        </div>
      </div>

      {feedback && <SuccessAlert message={feedback} />}
      {error && <ErrorAlert message={error} />}

      <div className="card">
        <form className="form-grid" onSubmit={onSubmit}>
          <div className="field">
            <label htmlFor="title">{t('patents.field.title')}</label>
            <input
              id="title"
              value={form.title}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
            />
          </div>
          <div className="field">
            <label htmlFor="abstractText">{t('patents.field.abstract')}</label>
            <textarea
              id="abstractText"
              rows={4}
              value={form.abstractText}
              onChange={(e) => setForm((f) => ({ ...f, abstractText: e.target.value }))}
            />
          </div>

          <div className="form-grid two">
            <div className="field">
              <label htmlFor="publicationNumber">{t('patents.field.pubNumber')}</label>
              <input
                id="publicationNumber"
                value={form.publicationNumber}
                onChange={(e) => setForm((f) => ({ ...f, publicationNumber: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="publicationDate">{t('patents.field.pubDate')}</label>
              <input
                id="publicationDate"
                type="date"
                value={form.publicationDate}
                onChange={(e) => setForm((f) => ({ ...f, publicationDate: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="applicationNumber">{t('patents.field.appNumber')}</label>
              <input
                id="applicationNumber"
                value={form.applicationNumber}
                onChange={(e) => setForm((f) => ({ ...f, applicationNumber: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="applicationDate">{t('patents.field.appDate')}</label>
              <input
                id="applicationDate"
                type="date"
                value={form.applicationDate}
                onChange={(e) => setForm((f) => ({ ...f, applicationDate: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="applicationCountry">{t('patents.field.appCountry')}</label>
              <select
                id="applicationCountry"
                value={form.applicationCountry}
                onChange={(e) => setForm((f) => ({ ...f, applicationCountry: e.target.value }))}
              >
                <option value="">—</option>
                {form.applicationCountry &&
                  !(countriesQuery.data || []).some((c) => c.acronym === form.applicationCountry) && (
                    <option value={form.applicationCountry}>{form.applicationCountry}</option>
                  )}
                {(countriesQuery.data || []).map((c) => (
                  <option key={c.acronym} value={c.acronym}>
                    {c.acronym} — {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="language">{t('patents.field.language')}</label>
              <input
                id="language"
                value={form.language}
                onChange={(e) => setForm((f) => ({ ...f, language: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="mainClassification">{t('patents.field.mainClass')}</label>
              <input
                id="mainClassification"
                value={form.mainClassification}
                onChange={(e) => setForm((f) => ({ ...f, mainClassification: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="extraClassifications">{t('patents.field.extraClass')}</label>
              <input
                id="extraClassifications"
                value={form.extraClassifications}
                onChange={(e) => setForm((f) => ({ ...f, extraClassifications: e.target.value }))}
              />
            </div>
          </div>

          <div className="form-grid two">
            <div className="field">
              <label htmlFor="applicantsText">{t('patents.field.applicants')}</label>
              <textarea
                id="applicantsText"
                rows={4}
                value={form.applicantsText}
                onChange={(e) => setForm((f) => ({ ...f, applicantsText: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="inventorsText">{t('patents.field.inventors')}</label>
              <textarea
                id="inventorsText"
                rows={4}
                value={form.inventorsText}
                onChange={(e) => setForm((f) => ({ ...f, inventorsText: e.target.value }))}
              />
            </div>
          </div>

          <div className="field">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <label>{t('patents.priorities')}</label>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() =>
                  setForm((f) => ({
                    ...f,
                    priorities: [...f.priorities, { value: '', countryAcronym: '', date: '' }],
                  }))
                }
              >
                + {t('patents.addPriority')}
              </button>
            </div>
            {form.priorities.map((pr, idx) => (
              <div key={idx} className="stack-actions" style={{ alignItems: 'end' }}>
                <div className="field" style={{ flex: 2 }}>
                  <label>{t('patents.priorityValue')}</label>
                  <input
                    value={pr.value}
                    onChange={(e) =>
                      setForm((f) => ({
                        ...f,
                        priorities: f.priorities.map((row, i) =>
                          i === idx ? { ...row, value: e.target.value } : row,
                        ),
                      }))
                    }
                  />
                </div>
                <div className="field" style={{ flex: 1 }}>
                  <label>{t('patents.priorityCountry')}</label>
                  <input
                    value={pr.countryAcronym}
                    placeholder="BR"
                    onChange={(e) =>
                      setForm((f) => ({
                        ...f,
                        priorities: f.priorities.map((row, i) =>
                          i === idx ? { ...row, countryAcronym: e.target.value } : row,
                        ),
                      }))
                    }
                  />
                </div>
                <div className="field" style={{ flex: 1 }}>
                  <label>{t('patents.priorityDate')}</label>
                  <input
                    type="date"
                    value={pr.date}
                    onChange={(e) =>
                      setForm((f) => ({
                        ...f,
                        priorities: f.priorities.map((row, i) =>
                          i === idx ? { ...row, date: e.target.value } : row,
                        ),
                      }))
                    }
                  />
                </div>
                <button
                  type="button"
                  className="btn btn-danger btn-sm"
                  onClick={() =>
                    setForm((f) => ({
                      ...f,
                      priorities: f.priorities.filter((_, i) => i !== idx),
                    }))
                  }
                >
                  ✕
                </button>
              </div>
            ))}
          </div>

          <div className="form-grid two">
            <label className="field-inline">
              <input
                type="checkbox"
                checked={form.blacklisted}
                onChange={(e) => setForm((f) => ({ ...f, blacklisted: e.target.checked }))}
              />
              {t('patents.field.blacklisted')}
            </label>
            <label className="field-inline">
              <input
                type="checkbox"
                checked={form.completed}
                onChange={(e) => setForm((f) => ({ ...f, completed: e.target.checked }))}
              />
              {t('patents.field.completed')}
            </label>
          </div>
          <button type="submit" className="btn" disabled={saveMutation.isPending}>
            {saveMutation.isPending ? t('common.saving') : t('patents.saveChanges')}
          </button>
        </form>
      </div>

      <div className="card">
        <h2>{t('patents.files')}</h2>
        <div className="form-grid two">
          <div className="field">
            <label htmlFor="upload-presentation">{t('patents.filePresentation')}</label>
            <div className="stack-actions" style={{ marginBottom: '0.5rem' }}>
              {patent.presentationFileId ? (
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => void downloadFile(patent.presentationFileId!, 'presentation')}
                >
                  {t('patents.downloadPresentation')}
                </button>
              ) : (
                <span className="muted">{t('patents.noPresentation')}</span>
              )}
            </div>
            <input
              id="upload-presentation"
              data-testid="upload-presentation"
              type="file"
              disabled={uploadMutation.isPending}
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) uploadMutation.mutate({ file: f, kind: 'presentation' });
              }}
            />
          </div>
          <div className="field">
            <label htmlFor="upload-patent-info">{t('patents.fileInfo')}</label>
            <div className="stack-actions" style={{ marginBottom: '0.5rem' }}>
              {patent.patentInfoFileId ? (
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => void downloadFile(patent.patentInfoFileId!, 'patent-info')}
                >
                  {t('patents.downloadInfo')}
                </button>
              ) : (
                <span className="muted">{t('patents.noInfo')}</span>
              )}
            </div>
            <input
              id="upload-patent-info"
              data-testid="upload-patent-info"
              type="file"
              disabled={uploadMutation.isPending}
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) uploadMutation.mutate({ file: f, kind: 'patentInfo' });
              }}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

export function PatentDetailPage() {
  const { id: projectId = '', patentId = '' } = useParams();
  const { t } = useI18n();

  const patentQuery = useQuery({
    queryKey: ['patent', patentId],
    queryFn: () => patentsApi.getPatent(patentId),
    enabled: !!patentId,
  });

  if (patentQuery.isLoading) return <Loading />;
  if (patentQuery.isError || !patentQuery.data) {
    return (
      <ErrorAlert
        message={
          patentQuery.error instanceof Error
            ? patentQuery.error.message
            : t('patents.notFound')
        }
      />
    );
  }

  return <PatentEditForm key={patentQuery.data.id} patent={patentQuery.data} projectId={projectId} />;
}
