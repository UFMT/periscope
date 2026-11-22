import type { FormEvent } from 'react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import * as patentsApi from '../api/patents';
import { ErrorAlert, SuccessAlert } from '../components/Feedback';
import { useI18n } from '../i18n/useI18n';
import type { ImporterType, ImportResult } from '../types';
import { ApiError } from '../types';

const IMPORTERS: { value: ImporterType; label: string }[] = [
  { value: 'DPMA', label: 'DPMA' },
  { value: 'ESPACENET', label: 'Espacenet' },
  { value: 'PATENTSCOPE', label: 'Patentscope' },
];

export function ImportPage() {
  const { id: projectId = '' } = useParams();
  const { t } = useI18n();
  const [type, setType] = useState<ImporterType>('DPMA');
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const importMutation = useMutation({
    mutationFn: () => {
      if (!file) throw new Error(t('import.selectFile'));
      return patentsApi.importPatents(projectId, file, type);
    },
    onSuccess: (data) => {
      setResult(data);
      setError(null);
    },
    onError: (err: unknown) => {
      setResult(null);
      setError(err instanceof ApiError || err instanceof Error ? err.message : t('import.error'));
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!file) {
      setError(t('import.selectFile'));
      return;
    }
    importMutation.mutate();
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>{t('import.title')}</h1>
          <p>{t('import.subtitle')}</p>
        </div>
        <Link className="btn btn-secondary" to={`/projects/${projectId}/patents`}>
          {t('import.viewPatents')}
        </Link>
      </div>

      {error && <ErrorAlert message={error} />}
      {result && (
        <SuccessAlert
          message={t('import.successAlert', {
            count: result.imported,
            importer: result.importer,
            fileName: result.fileName,
          })}
        />
      )}

      <div className="card">
        <form className="form-grid" onSubmit={onSubmit} data-testid="import-form">
          <div className="field">
            <label htmlFor="type">{t('import.importerType')}</label>
            <select
              id="type"
              data-testid="import-type"
              value={type}
              onChange={(e) => setType(e.target.value as ImporterType)}
            >
              {IMPORTERS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="file">{t('import.file')}</label>
            <input
              id="file"
              data-testid="import-file"
              type="file"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              required
            />
          </div>
          <button
            type="submit"
            className="btn"
            data-testid="import-submit"
            disabled={importMutation.isPending}
          >
            {importMutation.isPending ? t('import.submitting') : t('import.submit')}
          </button>
        </form>
      </div>

      {result && (
        <div className="card">
          <h2>{t('import.resultTitle')}</h2>
          <p>
            {t('import.resultSummary', {
              count: result.imported,
              importer: result.importer,
            })}
          </p>
          {result.messages?.length > 0 && (
            <ul>
              {result.messages.map((msg) => (
                <li key={msg}>{msg}</li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
