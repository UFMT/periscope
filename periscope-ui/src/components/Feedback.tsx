import type { ReactNode } from 'react';
import { useI18n } from '../i18n/useI18n';

export function Loading({ label }: { label?: string }) {
  const { t } = useI18n();
  return (
    <div className="loading-block" role="status" aria-live="polite">
      {label ?? t('common.loading')}
    </div>
  );
}

export function EmptyState({ children }: { children: ReactNode }) {
  return <div className="empty-block">{children}</div>;
}

export function ErrorAlert({ message }: { message: string }) {
  return (
    <div className="alert alert-error" role="alert" data-testid="error-alert">
      {message}
    </div>
  );
}

export function SuccessAlert({ message }: { message: string }) {
  return (
    <div className="alert alert-success" role="status" data-testid="success-alert">
      {message}
    </div>
  );
}

export function InfoAlert({ message }: { message: string }) {
  return (
    <div className="alert alert-info" role="status">
      {message}
    </div>
  );
}
