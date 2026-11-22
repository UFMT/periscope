import type { FormEvent } from 'react';
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as usersApi from '../api/users';
import { ErrorAlert, Loading, SuccessAlert } from '../components/Feedback';
import { Modal } from '../components/Modal';
import { useAuth } from '../hooks/useAuth';
import { useI18n } from '../i18n/useI18n';
import type { User, UserRequest } from '../types';
import { ApiError } from '../types';

const emptyForm: UserRequest = {
  username: '',
  password: '',
  firstname: '',
  lastname: '',
  email: '',
  userLevel: 'USER',
};

export function UsersPage() {
  const queryClient = useQueryClient();
  const { user: currentUser } = useAuth();
  const { t } = useI18n();
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<User | null>(null);
  const [form, setForm] = useState<UserRequest>(emptyForm);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const { data, isLoading, isError, error: queryError } = useQuery({
    queryKey: ['users'],
    queryFn: usersApi.listUsers,
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (editing) {
        const payload: UserRequest = {
          username: form.username?.trim(),
          firstname: form.firstname ?? '',
          lastname: form.lastname ?? '',
          email: form.email ?? '',
          userLevel: form.userLevel || 'USER',
        };
        if (form.password && form.password.trim()) {
          payload.password = form.password;
        }
        return usersApi.updateUser(editing.id, payload);
      }
      return usersApi.createUser({
        username: form.username?.trim(),
        password: form.password,
        firstname: form.firstname ?? '',
        lastname: form.lastname ?? '',
        email: form.email ?? '',
        userLevel: form.userLevel || 'USER',
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['users'] });
      void queryClient.invalidateQueries({ queryKey: ['reference-users'] });
      setModalOpen(false);
      setEditing(null);
      setForm(emptyForm);
      setFeedback(editing ? t('users.updated') : t('users.created'));
      setError(null);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : t('users.saveError'));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => usersApi.deleteUser(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['users'] });
      void queryClient.invalidateQueries({ queryKey: ['reference-users'] });
      setFeedback(t('users.deleted'));
      setError(null);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : t('users.deleteError'));
    },
  });

  function openCreate() {
    setEditing(null);
    setForm(emptyForm);
    setError(null);
    setModalOpen(true);
  }

  function openEdit(user: User) {
    setEditing(user);
    setForm({
      username: user.username,
      password: '',
      firstname: user.firstname || '',
      lastname: user.lastname || '',
      email: user.email || '',
      userLevel: user.userLevel || 'USER',
    });
    setError(null);
    setModalOpen(true);
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!form.username?.trim()) {
      setError(t('users.usernameRequired'));
      return;
    }
    if (!editing && !form.password?.trim()) {
      setError(t('users.passwordRequired'));
      return;
    }
    saveMutation.mutate();
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>{t('users.title')}</h1>
          <p>{t('users.subtitle')}</p>
        </div>
        <button type="button" className="btn" data-testid="user-create" onClick={openCreate}>
          {t('users.new')}
        </button>
      </div>

      {feedback && <SuccessAlert message={feedback} />}
      {error && <ErrorAlert message={error} />}
      {isError && (
        <ErrorAlert
          message={queryError instanceof Error ? queryError.message : t('users.loadError')}
        />
      )}

      <div className="card">
        {isLoading ? (
          <Loading />
        ) : !data?.length ? (
          <div className="empty-block">{t('users.empty')}</div>
        ) : (
          <div className="table-wrap">
            <table className="data" data-testid="users-table">
              <thead>
                <tr>
                  <th>{t('users.username')}</th>
                  <th>{t('users.fullName')}</th>
                  <th>{t('users.email')}</th>
                  <th>{t('users.level')}</th>
                  <th>{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {data.map((user) => {
                  const isSelf = currentUser?.id === user.id || currentUser?.username === user.username;
                  return (
                    <tr key={user.id} data-testid={`user-row-${user.username}`}>
                      <td>{user.username}</td>
                      <td>
                        {[user.firstname, user.lastname].filter(Boolean).join(' ') || '—'}
                      </td>
                      <td>{user.email || '—'}</td>
                      <td>
                        <span className="badge">{user.userLevel}</span>
                      </td>
                      <td>
                        <div className="stack-actions">
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => openEdit(user)}
                          >
                            {t('common.edit')}
                          </button>
                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            disabled={isSelf}
                            onClick={() => {
                              if (confirm(t('users.confirmDelete', { username: user.username }))) {
                                deleteMutation.mutate(user.id);
                              }
                            }}
                          >
                            {t('common.delete')}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal
        title={editing ? t('users.edit') : t('users.new')}
        open={modalOpen}
        onClose={() => setModalOpen(false)}
      >
        <form className="form-grid" onSubmit={onSubmit} data-testid="user-form">
          <div className="form-grid two">
            <div className="field">
              <label htmlFor="user-username">{t('users.username')}</label>
              <input
                id="user-username"
                data-testid="user-username"
                value={form.username || ''}
                onChange={(e) => setForm((f) => ({ ...f, username: e.target.value }))}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="user-level">{t('users.level')}</label>
              <select
                id="user-level"
                data-testid="user-level"
                value={form.userLevel || 'USER'}
                onChange={(e) => setForm((f) => ({ ...f, userLevel: e.target.value }))}
              >
                <option value="USER">USER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>
          </div>
          <div className="form-grid two">
            <div className="field">
              <label htmlFor="user-firstname">{t('users.firstname')}</label>
              <input
                id="user-firstname"
                data-testid="user-firstname"
                value={form.firstname || ''}
                onChange={(e) => setForm((f) => ({ ...f, firstname: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="user-lastname">{t('users.lastname')}</label>
              <input
                id="user-lastname"
                data-testid="user-lastname"
                value={form.lastname || ''}
                onChange={(e) => setForm((f) => ({ ...f, lastname: e.target.value }))}
              />
            </div>
          </div>
          <div className="field">
            <label htmlFor="user-email">{t('users.email')}</label>
            <input
              id="user-email"
              type="email"
              data-testid="user-email"
              value={form.email || ''}
              onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
            />
          </div>
          <div className="field">
            <label htmlFor="user-password">
              {editing ? t('users.passwordOptional') : t('users.password')}
            </label>
            <input
              id="user-password"
              type="password"
              data-testid="user-password"
              value={form.password || ''}
              onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
              required={!editing}
            />
          </div>
          <div className="stack-actions">
            <button type="submit" className="btn" data-testid="user-save" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? t('common.saving') : t('common.save')}
            </button>
            <button type="button" className="btn btn-secondary" onClick={() => setModalOpen(false)}>
              {t('common.cancel')}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
