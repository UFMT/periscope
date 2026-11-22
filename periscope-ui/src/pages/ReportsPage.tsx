import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import * as referenceApi from '../api/reference';
import * as reportsApi from '../api/reports';
import { ErrorAlert, Loading } from '../components/Feedback';
import { Pagination } from '../components/Pagination';
import { useI18n } from '../i18n/useI18n';
import type { ReportName, ReportQueryParams } from '../types';

const REPORT_NAMES: ReportName[] = [
  'main-applicant',
  'main-inventor',
  'main-ipc',
  'application-date',
  'publication-date',
  'priority-country',
  'priority-date',
  'docs-national-priority',
  'technological-profile-applicant',
  'technological-profile-inventor',
  'nature-applicant',
  'state-distribution',
  'region-distribution',
];

const LINE_REPORTS = new Set<ReportName>([
  'application-date',
  'publication-date',
  'priority-date',
]);

const IPC_REPORTS = new Set<ReportName>([
  'main-ipc',
  'technological-profile-applicant',
  'technological-profile-inventor',
]);

const LIMIT_REPORTS = new Set<ReportName>([
  'main-applicant',
  'main-inventor',
  'main-ipc',
  'priority-country',
  'technological-profile-applicant',
  'technological-profile-inventor',
]);

function escapeCsvCell(value: string | number): string {
  const str = String(value);
  if (str.includes(',') || str.includes('"') || str.includes('\n')) {
    return `"${str.replaceAll('"', '""')}"`;
  }
  return str;
}

export function ReportsPage() {
  const { id: projectId = '' } = useParams();
  const { locale, t, natureLabel } = useI18n();
  const [report, setReport] = useState<ReportName>('main-applicant');
  const [limit, setLimit] = useState(15);
  const [dateType, setDateType] = useState<'publication' | 'application'>('publication');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [complete, setComplete] = useState(false);
  const [ipcLevel, setIpcLevel] = useState<'section' | 'class' | 'subclass' | 'group' | 'subgroup'>(
    'class',
  );
  const [classificationType, setClassificationType] = useState<'IPC' | 'CPC'>('IPC');
  const [applicantType, setApplicantType] = useState('');
  const [applicantName, setApplicantName] = useState('');
  const [inventorName, setInventorName] = useState('');
  const [natPage, setNatPage] = useState(0);
  const [natQuery, setNatQuery] = useState('');

  const isNationalDocs = report === 'docs-national-priority';
  const showIpcControls = IPC_REPORTS.has(report);
  const showLimit = LIMIT_REPORTS.has(report);
  const showLine = LINE_REPORTS.has(report);

  const applicantTypesQuery = useQuery({
    queryKey: ['reference-applicant-types'],
    queryFn: referenceApi.listApplicantTypes,
    enabled: report === 'main-applicant',
  });

  const applicantsAcQuery = useQuery({
    queryKey: ['report-ac-applicants', projectId, applicantName],
    queryFn: () => reportsApi.autocompleteApplicants(projectId, applicantName),
    enabled: !!projectId && report === 'technological-profile-applicant',
  });

  const inventorsAcQuery = useQuery({
    queryKey: ['report-ac-inventors', projectId, inventorName],
    queryFn: () => reportsApi.autocompleteInventors(projectId, inventorName),
    enabled: !!projectId && report === 'technological-profile-inventor',
  });

  const queryParams: ReportQueryParams = useMemo(
    () => ({
      limit: showLimit ? limit : undefined,
      dateType: !showLine ? dateType : undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
      complete,
      ipcLevel: showIpcControls ? ipcLevel : undefined,
      classificationType: showIpcControls ? classificationType : undefined,
      applicantType: report === 'main-applicant' && applicantType ? applicantType : undefined,
      applicantName:
        report === 'technological-profile-applicant' && applicantName.trim()
          ? applicantName.trim()
          : undefined,
      inventorName:
        report === 'technological-profile-inventor' && inventorName.trim()
          ? inventorName.trim()
          : undefined,
    }),
    [
      showLimit,
      limit,
      showLine,
      dateType,
      startDate,
      endDate,
      complete,
      showIpcControls,
      ipcLevel,
      classificationType,
      report,
      applicantType,
      applicantName,
      inventorName,
    ],
  );

  const reportQuery = useQuery({
    queryKey: ['report', projectId, report, queryParams],
    queryFn: () => reportsApi.getReport(projectId, report, queryParams),
    enabled: !!projectId && !isNationalDocs,
  });

  const natDocsQuery = useQuery({
    queryKey: ['report-national-docs', projectId, natPage, natQuery],
    queryFn: () =>
      reportsApi.getNationalPriorityDocs(projectId, {
        page: natPage,
        size: 20,
        q: natQuery.trim() || undefined,
      }),
    enabled: !!projectId && isNationalDocs,
  });

  const chartData = useMemo(() => {
    const items = reportQuery.data?.items || [];
    return items.map((item) => {
      const rawKey = String(item.key);
      const displayName = report === 'nature-applicant' ? natureLabel(rawKey) : rawKey;
      return {
        name: displayName,
        value: Number(item.value),
        description: item.description || null,
      };
    });
  }, [reportQuery.data, report, natureLabel]);

  const hasDescriptions = chartData.some((row) => !!row.description);

  function clearFilters() {
    setLimit(15);
    setDateType('publication');
    setStartDate('');
    setEndDate('');
    setComplete(false);
    setIpcLevel('class');
    setClassificationType('IPC');
    setApplicantType('');
    setApplicantName('');
    setInventorName('');
    setNatQuery('');
    setNatPage(0);
  }

  function exportCsv() {
    if (!chartData.length) return;
    const headers = hasDescriptions
      ? [t('reports.col.key'), t('reports.col.description'), t('reports.col.value')]
      : [t('reports.col.key'), t('reports.col.value')];
    const rows = chartData.map((row) =>
      hasDescriptions
        ? [row.name, row.description || '', row.value].map(escapeCsvCell).join(',')
        : [row.name, row.value].map(escapeCsvCell).join(','),
    );
    const csv = [headers.map(escapeCsvCell).join(','), ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${report}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  const activeError = isNationalDocs ? natDocsQuery.error : reportQuery.error;
  const activeIsError = isNationalDocs ? natDocsQuery.isError : reportQuery.isError;
  const activeLoading = isNationalDocs ? natDocsQuery.isLoading : reportQuery.isLoading;

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>{t('reports.title')}</h1>
          <p>{t('reports.subtitle')}</p>
        </div>
        <div className="stack-actions">
          {!isNationalDocs && chartData.length > 0 && (
            <button
              type="button"
              className="btn btn-secondary"
              data-testid="export-csv"
              onClick={exportCsv}
            >
              {t('common.exportCsv')}
            </button>
          )}
          <Link className="btn btn-secondary" to={`/projects/${projectId}/patents`}>
            {t('reports.patents')}
          </Link>
        </div>
      </div>

      <div className="card" style={{ marginBottom: '1rem' }}>
        <div className="form-grid two">
          <div className="field">
            <label htmlFor="report">{t('reports.selectLabel')}</label>
            <select
              id="report"
              data-testid="report-select"
              value={report}
              onChange={(e) => {
                setReport(e.target.value as ReportName);
                setNatPage(0);
              }}
            >
              {REPORT_NAMES.map((name) => (
                <option key={name} value={name}>
                  {t(`reports.name.${name}`)}
                </option>
              ))}
            </select>
          </div>
          {isNationalDocs ? (
            <div className="field">
              <label htmlFor="nat-search">{t('reports.searchTitle')}</label>
              <input
                id="nat-search"
                value={natQuery}
                onChange={(e) => {
                  setNatPage(0);
                  setNatQuery(e.target.value);
                }}
              />
            </div>
          ) : (
            showLimit && (
              <div className="field">
                <label htmlFor="report-limit">{t('reports.limit')}</label>
                <input
                  id="report-limit"
                  type="number"
                  min={1}
                  max={100}
                  value={limit}
                  onChange={(e) => setLimit(Math.max(1, Math.min(100, Number(e.target.value) || 10)))}
                />
              </div>
            )
          )}
        </div>

        {!isNationalDocs && (
          <div className="filter-grid" style={{ marginTop: '1rem' }}>
            {!showLine && (
              <div className="field">
                <label htmlFor="filter-datetype">{t('reports.dateType')}</label>
                <select
                  id="filter-datetype"
                  value={dateType}
                  onChange={(e) => setDateType(e.target.value as 'publication' | 'application')}
                >
                  <option value="publication">{t('reports.dateType.publication')}</option>
                  <option value="application">{t('reports.dateType.application')}</option>
                </select>
              </div>
            )}
            <div className="field">
              <label htmlFor="filter-start">{t('reports.startDate')}</label>
              <input
                id="filter-start"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="filter-end">{t('reports.endDate')}</label>
              <input
                id="filter-end"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
            {showIpcControls && (
              <>
                <div className="field">
                  <label htmlFor="filter-ipc-level">{t('reports.ipcLevel')}</label>
                  <select
                    id="filter-ipc-level"
                    data-testid="filter-ipc-level"
                    value={ipcLevel}
                    onChange={(e) =>
                      setIpcLevel(
                        e.target.value as 'section' | 'class' | 'subclass' | 'group' | 'subgroup',
                      )
                    }
                  >
                    <option value="section">{t('reports.ipcLevel.section')}</option>
                    <option value="class">{t('reports.ipcLevel.class')}</option>
                    <option value="subclass">{t('reports.ipcLevel.subclass')}</option>
                    <option value="group">{t('reports.ipcLevel.group')}</option>
                    <option value="subgroup">{t('reports.ipcLevel.subgroup')}</option>
                  </select>
                </div>
                <div className="field">
                  <label htmlFor="filter-cls-type">{t('reports.classificationType')}</label>
                  <select
                    id="filter-cls-type"
                    value={classificationType}
                    onChange={(e) => setClassificationType(e.target.value as 'IPC' | 'CPC')}
                  >
                    <option value="IPC">IPC</option>
                    <option value="CPC">CPC</option>
                  </select>
                </div>
              </>
            )}
            {report === 'main-applicant' && (
              <div className="field">
                <label htmlFor="filter-applicant-type">{t('reports.applicantType')}</label>
                <select
                  id="filter-applicant-type"
                  value={applicantType}
                  onChange={(e) => setApplicantType(e.target.value)}
                >
                  <option value="">{t('reports.allNatures')}</option>
                  {(applicantTypesQuery.data || []).map((key) => (
                    <option key={key} value={key}>
                      {natureLabel(key)}
                    </option>
                  ))}
                </select>
              </div>
            )}
            {report === 'technological-profile-applicant' && (
              <div className="field">
                <label htmlFor="filter-applicant-name">{t('reports.applicantName')}</label>
                <input
                  id="filter-applicant-name"
                  list="applicants-datalist"
                  value={applicantName}
                  onChange={(e) => setApplicantName(e.target.value)}
                  placeholder={t('reports.applicantPlaceholder')}
                />
                <datalist id="applicants-datalist">
                  {(applicantsAcQuery.data || []).map((name) => (
                    <option key={name} value={name} />
                  ))}
                </datalist>
              </div>
            )}
            {report === 'technological-profile-inventor' && (
              <div className="field">
                <label htmlFor="filter-inventor-name">{t('reports.inventorName')}</label>
                <input
                  id="filter-inventor-name"
                  list="inventors-datalist"
                  value={inventorName}
                  onChange={(e) => setInventorName(e.target.value)}
                  placeholder={t('reports.inventorPlaceholder')}
                />
                <datalist id="inventors-datalist">
                  {(inventorsAcQuery.data || []).map((name) => (
                    <option key={name} value={name} />
                  ))}
                </datalist>
              </div>
            )}
            <label className="field-inline" style={{ alignSelf: 'end', paddingBottom: '0.5rem' }}>
              <input
                type="checkbox"
                checked={complete}
                onChange={(e) => setComplete(e.target.checked)}
              />
              {t('reports.completeOnly')}
            </label>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ alignSelf: 'end' }}
              onClick={clearFilters}
            >
              {t('reports.clearFilters')}
            </button>
          </div>
        )}
      </div>

      {activeLoading && <Loading />}
      {activeIsError && (
        <ErrorAlert
          message={activeError instanceof Error ? activeError.message : t('reports.loadError')}
        />
      )}

      {isNationalDocs && natDocsQuery.data && (
        <div className="card" data-testid="report-table">
          <h2>{t('reports.name.docs-national-priority')}</h2>
          {!natDocsQuery.data.content.length ? (
            <div className="empty-block">{t('reports.noData')}</div>
          ) : (
            <>
              <div className="table-wrap">
                <table className="data">
                  <thead>
                    <tr>
                      <th>{t('patents.col.title')}</th>
                      <th>{t('patents.col.publication')}</th>
                      <th>{t('patents.field.appNumber')}</th>
                      <th>{t('patents.col.classification')}</th>
                      <th>{t('patents.col.applicants')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {natDocsQuery.data.content.map((p) => (
                      <tr key={p.id}>
                        <td>
                          <Link to={`/projects/${projectId}/patents/${p.id}`}>
                            {p.title || t('patents.noTitle')}
                          </Link>
                        </td>
                        <td>
                          {p.publicationDate ? new Date(p.publicationDate).toLocaleDateString(locale) : '—'}
                        </td>
                        <td>{p.applicationNumber || '—'}</td>
                        <td>{p.mainClassification || '—'}</td>
                        <td className="muted">{(p.applicants || []).slice(0, 3).join(', ') || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Pagination
                page={natDocsQuery.data.page}
                totalPages={natDocsQuery.data.totalPages}
                totalElements={natDocsQuery.data.totalElements}
                onChange={setNatPage}
              />
            </>
          )}
        </div>
      )}

      {!isNationalDocs && reportQuery.data && (
        <>
          <div className="card" data-testid="report-chart">
            <h2>{reportQuery.data.label || t(`reports.name.${report}`)}</h2>
            {chartData.length === 0 ? (
              <div className="empty-block">{t('reports.noData')}</div>
            ) : (
              <div style={{ width: '100%', height: 360 }}>
                <ResponsiveContainer>
                  {showLine ? (
                    <LineChart
                      data={chartData}
                      margin={{ top: 8, right: 16, left: 8, bottom: 48 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" stroke="#d5dde6" />
                      <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                      <YAxis allowDecimals={false} />
                      <Tooltip />
                      <Line
                        type="monotone"
                        dataKey="value"
                        stroke="#1a6f9a"
                        strokeWidth={2}
                        name={t('reports.quantity')}
                      />
                    </LineChart>
                  ) : (
                    <BarChart
                      data={chartData}
                      margin={{ top: 8, right: 16, left: 8, bottom: 64 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" stroke="#d5dde6" />
                      <XAxis
                        dataKey="name"
                        interval={0}
                        angle={-35}
                        textAnchor="end"
                        height={80}
                        tick={{ fontSize: 11 }}
                      />
                      <YAxis allowDecimals={false} />
                      <Tooltip />
                      <Bar
                        dataKey="value"
                        fill="#1a6f9a"
                        name={t('reports.quantity')}
                        radius={[4, 4, 0, 0]}
                      />
                    </BarChart>
                  )}
                </ResponsiveContainer>
              </div>
            )}
          </div>

          <div className="card" data-testid="report-table">
            <h2>{t('reports.tableTitle')}</h2>
            {chartData.length === 0 ? (
              <div className="empty-block">{t('reports.emptyItems')}</div>
            ) : (
              <div className="table-wrap">
                <table className="data">
                  <thead>
                    <tr>
                      <th>{t('reports.col.key')}</th>
                      {hasDescriptions && <th>{t('reports.col.description')}</th>}
                      <th>{t('reports.col.value')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {chartData.map((row) => (
                      <tr key={row.name}>
                        <td>{row.name}</td>
                        {hasDescriptions && <td className="muted">{row.description || '—'}</td>}
                        <td>{row.value}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
