import type { FormEvent } from 'react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as projectsApi from '../api/projects';
import * as referenceApi from '../api/reference';
import { ErrorAlert, Loading, SuccessAlert } from '../components/Feedback';
import { Modal } from '../components/Modal';
import { useAuth } from '../hooks/useAuth';
import { useI18n } from '../i18n/useI18n';
import type { Project, ProjectRequest } from '../types';
import { ApiError } from '../types';

function formatDate(value: string | null, locale: string): string {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleString(locale);
  } catch {
    return value;
  }
}

const emptyForm: ProjectRequest = { title: '', description: '', isPublic: false, observerIds: [] };

export function ProjectsPage() {
  const queryClient = useQueryClient();
  const { user: currentUser } = useAuth();
  const { locale, t } = useI18n();
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Project | null>(null);
  const [form, setForm] = useState<ProjectRequest>(emptyForm);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const { data, isLoading, isError, error: queryError } = useQuery({
    queryKey: ['projects'],
    queryFn: projectsApi.listProjects,
  });

  const usersQuery = useQuery({
    queryKey: ['reference-users'],
    queryFn: referenceApi.listReferenceUsers,
    enabled: modalOpen,
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (editing) {
        return projectsApi.updateProject(editing.id, form);
      }
      return projectsApi.createProject(form);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
      setModalOpen(false);
      setEditing(null);
      setForm(emptyForm);
      setFeedback(editing ? t('projects.updated') : t('projects.created'));
      setError(null);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : t('projects.saveError'));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => projectsApi.deleteProject(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
      setFeedback(t('projects.deleted'));
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : t('projects.deleteError'));
    },
  });

  function openCreate() {
    setEditing(null);
    setForm(emptyForm);
    setModalOpen(true);
    setError(null);
  }

  function openEdit(project: Project) {
    setEditing(project);
    setForm({
      title: project.title,
      description: project.description || '',
      isPublic: project.isPublic ?? false,
      observerIds: project.observerIds || [],
    });
    setModalOpen(true);
    setError(null);
  }

  function toggleObserver(userId: string) {
    setForm((prev) => {
      const current = prev.observerIds || [];
      const exists = current.includes(userId);
      return {
        ...prev,
        observerIds: exists ? current.filter((id) => id !== userId) : [...current, userId],
      };
    });
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!form.title.trim()) {
      setError(t('projects.titleRequired'));
      return;
    }
    saveMutation.mutate();
  }

  const ownerId = editing?.ownerId ?? currentUser?.id;
  const availableObservers = (usersQuery.data || []).filter((u) => u.id !== ownerId);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>{t('projects.title')}</h1>
          <p>{t('projects.subtitle')}</p>
        </div>
        <button type="button" className="btn" data-testid="project-create" onClick={openCreate}>
          {t('projects.new')}
        </button>
      </div>

      {feedback && <SuccessAlert message={feedback} />}
      {error && <ErrorAlert message={error} />}
      {isError && (
        <ErrorAlert
          message={queryError instanceof Error ? queryError.message : t('projects.loadError')}
        />
      )}

      <div className="card">
        {isLoading ? (
          <Loading />
        ) : !data?.length ? (
          <div className="empty-block">{t('projects.empty')}</div>
        ) : (
          <div className="table-wrap">
            <table className="data" data-testid="projects-table">
              <thead>
                <tr>
                  <th>{t('projects.col.title')}</th>
                  <th>{t('projects.col.description')}</th>
                  <th>{t('projects.col.owner')}</th>
                  <th>{t('projects.col.patents')}</th>
                  <th>{t('projects.col.created')}</th>
                  <th>{t('projects.col.updated')}</th>
                  <th>{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {data.map((project) => (
                  <tr key={project.id} data-testid={`project-row-${project.id}`}>
                    <td>
                      <Link
                        to={`/projects/${project.id}/patents`}
                        data-testid={`project-link-${project.id}`}
                      >
                        {project.title}
                      </Link>
                      <div className="chips" style={{ marginTop: '0.25rem' }}>
                        {project.isPublic ? (
                          <span className="badge">{t('projects.publicBadge')}</span>
                        ) : null}
                        {project.observerIds?.length ? (
                          <span className="badge badge-muted">
                            +{project.observerIds.length}
                          </span>
                        ) : null}
                      </div>
                    </td>
                    <td className="muted">{project.description || '—'}</td>
                    <td>{project.ownerName || '—'}</td>
                    <td>{project.patentCount}</td>
                    <td>{formatDate(project.createdAt, locale)}</td>
                    <td>{formatDate(project.updateAt, locale)}</td>
                    <td>
                      <div className="stack-actions">
                        <Link
                          className="btn btn-secondary btn-sm"
                          to={`/projects/${project.id}/patents`}
                        >
                          {t('common.open')}
                        </Link>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => openEdit(project)}
                        >
                          {t('common.edit')}
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger btn-sm"
                          data-testid={`project-delete-${project.id}`}
                          onClick={() => {
                            if (confirm(t('projects.confirmDelete', { title: project.title }))) {
                              deleteMutation.mutate(project.id);
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
        )}
      </div>

      <Modal
        title={editing ? t('projects.edit') : t('projects.new')}
        open={modalOpen}
        onClose={() => setModalOpen(false)}
      >
        <form className="form-grid" onSubmit={onSubmit} data-testid="project-form">
          <div className="field">
            <label htmlFor="title">{t('projects.col.title')}</label>
            <input
              id="title"
              data-testid="project-title"
              value={form.title}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="description">{t('projects.col.description')}</label>
            <textarea
              id="description"
              data-testid="project-description"
              rows={3}
              value={form.description || ''}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            />
          </div>
          <label className="field-inline">
            <input
              type="checkbox"
              checked={!!form.isPublic}
              onChange={(e) => setForm((f) => ({ ...f, isPublic: e.target.checked }))}
            />
            {t('projects.isPublic')}
          </label>
          <div className="field">
            <label>{t('projects.observers')}</label>
            <span className="muted" style={{ fontSize: '0.82rem' }}>
              {t('projects.observersHelp')}
            </span>
            {availableObservers.length === 0 ? (
              <span className="muted" style={{ fontSize: '0.85rem' }}>
                {t('projects.noObserversAvailable')}
              </span>
            ) : (
              <div className="suggestions-box" style={{ maxHeight: 140 }} data-testid="project-observers">
                {availableObservers.map((u) => {
                  const selected = (form.observerIds || []).includes(u.id);
                  const label = [u.firstname, u.lastname].filter(Boolean).join(' ') || u.username;
                  return (
                    <button
                      key={u.id}
                      type="button"
                      onClick={() => toggleObserver(u.id)}
                      aria-pressed={selected}
                    >
                      {selected ? '✓ ' : ''}
                      {label} ({u.username})
                    </button>
                  );
                })}
              </div>
            )}
          </div>
          <div className="stack-actions">
            <button type="submit" className="btn" data-testid="project-save" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? t('common.saving') : t('common.save')}
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setModalOpen(false)}
            >
              {t('common.cancel')}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
