import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import * as referenceApi from '../api/reference';
import * as reportsApi from '../api/reports';
import { I18nProvider } from '../i18n/I18nProvider';
import { ReportsPage } from '../pages/ReportsPage';

vi.mock('../api/reports');
vi.mock('../api/reference');

describe('ReportsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(referenceApi.listApplicantTypes).mockResolvedValue(['private.company']);
    vi.mocked(reportsApi.autocompleteApplicants).mockResolvedValue(['ACME']);
    vi.mocked(reportsApi.autocompleteInventors).mockResolvedValue(['ADA']);
    vi.mocked(reportsApi.getReport).mockImplementation(async (_pid, name) => {
      if (name === 'nature-applicant') {
        return {
          name,
          label: 'Natureza',
          items: [{ key: 'private.company', value: 4 }],
        };
      }
      if (name === 'main-ipc') {
        return {
          name,
          label: 'IPC',
          items: [{ key: 'H', value: 3, description: 'Eletricidade' }],
        };
      }
      return {
        name,
        label: name,
        items: [{ key: 'ACME', value: 5 }],
      };
    });
    vi.mocked(reportsApi.getNationalPriorityDocs).mockResolvedValue({
      content: [
        {
          id: 'p-br',
          title: 'PATENTE BR',
          abstractText: null,
          publicationNumber: 'BR102022',
          publicationDate: '2023-05-01T00:00:00Z',
          applicationNumber: 'BRAPP1',
          applicationDate: null,
          applicationCountry: 'BR',
          mainClassification: 'H01L',
          blacklisted: false,
          completed: true,
          projectId: 'proj-1',
          applicants: ['UFMT'],
          inventors: ['ADA'],
          presentationFileId: null,
          patentInfoFileId: null,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
  });

  it('offers all 13 reports, translates nature keys, shows IPC descriptions, and renders national docs table', async () => {
    const user = userEvent.setup();
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <I18nProvider initialLocale="pt-BR">
        <QueryClientProvider client={qc}>
          <MemoryRouter initialEntries={['/projects/proj-1/reports']}>
            <Routes>
              <Route path="/projects/:id/reports" element={<ReportsPage />} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      </I18nProvider>,
    );

    const select = screen.getByTestId('report-select') as HTMLSelectElement;
    expect(select.options).toHaveLength(13);

    await user.selectOptions(select, 'nature-applicant');
    await waitFor(() => {
      expect(screen.getByTestId('report-table')).toHaveTextContent('Empresa Privada');
    });

    await user.selectOptions(select, 'main-ipc');
    await user.selectOptions(screen.getByTestId('filter-ipc-level'), 'section');
    await waitFor(() => {
      expect(screen.getByTestId('report-table')).toHaveTextContent('Eletricidade');
    });

    await user.selectOptions(select, 'docs-national-priority');
    await waitFor(() => {
      expect(screen.getByTestId('report-table')).toHaveTextContent('PATENTE BR');
    });
  });
});
