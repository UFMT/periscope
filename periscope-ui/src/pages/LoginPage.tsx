import type { FormEvent } from 'react';
import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ErrorAlert, Loading } from '../components/Feedback';
import { useAuth } from '../hooks/useAuth';
import { useI18n } from '../i18n/useI18n';
import { ApiError } from '../types';

export function LoginPage() {
  const { login, isAuthenticated, loading } = useAuth();
  const { locale, setLocale, t } = useI18n();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (loading) return <Loading />;
  if (isAuthenticated) {
    const from = (location.state as { from?: string } | null)?.from || '/projects';
    return <Navigate to={from} replace />;
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username.trim(), password);
      const from = (location.state as { from?: string } | null)?.from || '/projects';
      navigate(from, { replace: true });
    } catch (err) {
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        setError(t('login.invalid'));
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError(t('login.error'));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={(e) => void onSubmit(e)}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div className="brand">PERISCOPE</div>
          <div className="lang-switch">
            <button
              type="button"
              data-testid="login-lang-pt"
              className={`btn btn-sm ${locale === 'pt-BR' ? '' : 'btn-secondary'}`}
              onClick={() => setLocale('pt-BR')}
            >
              PT
            </button>
            <button
              type="button"
              data-testid="login-lang-en"
              className={`btn btn-sm ${locale === 'en-US' ? '' : 'btn-secondary'}`}
              onClick={() => setLocale('en-US')}
            >
              EN
            </button>
          </div>
        </div>
        <h1>{t('login.title')}</h1>
        <p className="muted">{t('login.subtitle')}</p>
        {error && <ErrorAlert message={error} />}
        <div className="form-grid">
          <div className="field">
            <label htmlFor="username">{t('login.username')}</label>
            <input
              id="username"
              name="username"
              data-testid="login-username"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="password">{t('login.password')}</label>
            <input
              id="password"
              name="password"
              type="password"
              data-testid="login-password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="btn" data-testid="login-submit" disabled={submitting}>
            {submitting ? t('login.submitting') : t('login.submit')}
          </button>
        </div>
      </form>
    </div>
  );
}
