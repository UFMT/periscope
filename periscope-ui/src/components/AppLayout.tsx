import { useState, type FormEvent } from 'react';
import { NavLink, Outlet, useParams } from 'react-router-dom';
import * as authApi from '../api/auth';
import { ErrorAlert, InfoAlert, SuccessAlert } from './Feedback';
import { Modal } from './Modal';
import { useAuth } from '../hooks/useAuth';
import { useI18n } from '../i18n/useI18n';
import { ApiError } from '../types';

export function AppLayout() {
  const { user, isAdmin, logout, updateCurrentUser } = useAuth();
  const { locale, setLocale, t } = useI18n();
  const { id } = useParams();
  const [menuOpen, setMenuOpen] = useState(false);
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [pwError, setPwError] = useState<string | null>(null);
  const [pwSuccess, setPwSuccess] = useState<string | null>(null);
  const [pwSubmitting, setPwSubmitting] = useState(false);

  const mustChangePassword = Boolean(user?.mustChangePassword);
  const effectivePasswordModalOpen = passwordModalOpen || mustChangePassword;

  const displayName =
    [user?.firstname, user?.lastname].filter(Boolean).join(' ') ||
    user?.username ||
    t('common.user');

  const closeMenu = () => setMenuOpen(false);

  function openPasswordModal() {
    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
    setPwError(null);
    setPwSuccess(null);
    setPasswordModalOpen(true);
  }

  function closePasswordModal() {
    if (mustChangePassword) return;
    setPasswordModalOpen(false);
  }

  async function onSubmitPassword(e: FormEvent) {
    e.preventDefault();
    setPwError(null);
    setPwSuccess(null);
    if (newPassword.length < 8) {
      setPwError(t('password.tooShort'));
      return;
    }
    if (newPassword !== confirmPassword) {
      setPwError(t('password.mismatch'));
      return;
    }
    setPwSubmitting(true);
    try {
      const updated = await authApi.changePassword({ currentPassword, newPassword });
      updateCurrentUser(updated);
      setPwSuccess(t('password.success'));
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setPasswordModalOpen(false);
    } catch (err) {
      setPwError(err instanceof ApiError ? err.message : t('password.error'));
    } finally {
      setPwSubmitting(false);
    }
  }

  return (
    <div className="app-shell">
      <aside className={`sidebar ${menuOpen ? 'open' : ''}`} aria-label={t('nav.main')}>
        <div className="sidebar-brand">
          <strong>Periscope</strong>
          <span>{t('app.subtitle')}</span>
        </div>
        <nav className="sidebar-nav">
          <NavLink to="/projects" className="nav-link" onClick={closeMenu} end>
            {t('nav.projects')}
          </NavLink>
          {isAdmin && (
            <NavLink to="/users" className="nav-link" onClick={closeMenu}>
              {t('nav.users')}
            </NavLink>
          )}
          {id && (
            <>
              <div className="nav-section">{t('nav.currentProject')}</div>
              <NavLink to={`/projects/${id}/patents`} className="nav-link" onClick={closeMenu}>
                {t('nav.patents')}
              </NavLink>
              <NavLink to={`/projects/${id}/import`} className="nav-link" onClick={closeMenu}>
                {t('nav.import')}
              </NavLink>
              <NavLink
                to={`/projects/${id}/harmonization`}
                className="nav-link"
                onClick={closeMenu}
              >
                {t('nav.harmonization')}
              </NavLink>
              <NavLink to={`/projects/${id}/reports`} className="nav-link" onClick={closeMenu}>
                {t('nav.reports')}
              </NavLink>
            </>
          )}
        </nav>
      </aside>

      <div className="main-area">
        <header className="topbar">
          <button
            type="button"
            className="menu-toggle"
            aria-label={t('common.openMenu')}
            onClick={() => setMenuOpen((v) => !v)}
          >
            {t('common.menu')}
          </button>
          <div className="lang-switch" role="group" aria-label="Idioma / Language">
            <button
              type="button"
              data-testid="lang-pt"
              className={`btn btn-sm ${locale === 'pt-BR' ? '' : 'btn-secondary'}`}
              onClick={() => setLocale('pt-BR')}
            >
              PT
            </button>
            <button
              type="button"
              data-testid="lang-en"
              className={`btn btn-sm ${locale === 'en-US' ? '' : 'btn-secondary'}`}
              onClick={() => setLocale('en-US')}
            >
              EN
            </button>
          </div>
          <div className="topbar-user">
            <span>
              {displayName}
              {user?.userLevel ? ` · ${user.userLevel}` : ''}
            </span>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              data-testid="change-password-btn"
              onClick={openPasswordModal}
            >
              {t('common.changePassword')}
            </button>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              data-testid="logout-btn"
              onClick={() => void logout()}
            >
              {t('common.logout')}
            </button>
          </div>
        </header>
        <main className="content">
          {pwSuccess && <SuccessAlert message={pwSuccess} />}
          <Outlet />
        </main>
      </div>
      {menuOpen && (
        <div
          className="modal-backdrop"
          style={{ zIndex: 30 }}
          onClick={closeMenu}
          aria-hidden="true"
        />
      )}

      <Modal
        title={t('password.title')}
        open={effectivePasswordModalOpen}
        onClose={closePasswordModal}
      >
        {mustChangePassword && <InfoAlert message={t('password.requiredNotice')} />}
        {pwError && <ErrorAlert message={pwError} />}
        <form className="form-grid" onSubmit={(e) => void onSubmitPassword(e)}>
          <div className="field">
            <label htmlFor="pw-current">{t('password.current')}</label>
            <input
              id="pw-current"
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="pw-new">{t('password.new')}</label>
            <input
              id="pw-new"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              minLength={8}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="pw-confirm">{t('password.confirm')}</label>
            <input
              id="pw-confirm"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              minLength={8}
              required
            />
          </div>
          <div className="stack-actions">
            <button type="submit" className="btn" disabled={pwSubmitting}>
              {pwSubmitting ? t('common.saving') : t('common.save')}
            </button>
            {!mustChangePassword && (
              <button type="button" className="btn btn-secondary" onClick={closePasswordModal}>
                {t('common.cancel')}
              </button>
            )}
          </div>
        </form>
      </Modal>
    </div>
  );
}
