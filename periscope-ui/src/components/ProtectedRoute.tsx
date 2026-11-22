import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useI18n } from '../i18n/useI18n';
import { Loading } from './Feedback';

export function ProtectedRoute() {
  const { isAuthenticated, loading } = useAuth();
  const { t } = useI18n();
  const location = useLocation();

  if (loading) {
    return <Loading label={t('common.checkingSession')} />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}

export function AdminRoute() {
  const { isAdmin, loading } = useAuth();
  if (loading) return <Loading />;
  if (!isAdmin) return <Navigate to="/projects" replace />;
  return <Outlet />;
}
