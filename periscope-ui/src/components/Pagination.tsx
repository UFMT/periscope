import { useI18n } from '../i18n/useI18n';

interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, totalElements, onChange }: PaginationProps) {
  const { t } = useI18n();
  return (
    <div className="pagination">
      <span className="muted">
        {t('common.pageInfo', {
          page: totalPages === 0 ? 0 : page + 1,
          totalPages,
          total: totalElements,
        })}
      </span>
      <div className="stack-actions">
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={page <= 0}
          onClick={() => onChange(page - 1)}
        >
          {t('common.previous')}
        </button>
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={page + 1 >= totalPages}
          onClick={() => onChange(page + 1)}
        >
          {t('common.next')}
        </button>
      </div>
    </div>
  );
}
