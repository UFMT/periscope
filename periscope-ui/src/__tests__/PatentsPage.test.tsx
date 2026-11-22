import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import * as filesApi from '../api/files';
import * as patentsApi from '../api/patents';
import * as projectsApi from '../api/projects';
import * as referenceApi from '../api/reference';
import { I18nProvider } from '../i18n/I18nProvider';
import { PatentDetailPage } from '../pages/PatentDetailPage';
import { PatentsPage } from '../pages/PatentsPage';
import type { Patent } from '../types';

vi.mock('../api/patents');
vi.mock('../api/projects');
vi.mock('../api/reference');
vi.mock('../api/files');

const samplePatent: Patent = {
  id: 'pat-1',
  title: 'SOLAR CELL',
  abstractText: 'Abstract',
  publicationNumber: 'BR100',
  publicationDate: '2024-01-10T00:00:00Z',
  applicationNumber: 'APP100',
  applicationDate: '2023-01-10T00:00:00Z',
  applicationCountry: 'BR',
  mainClassification: 'H01L 31/04',
  blacklisted: false,
  completed: true,
  projectId: 'proj-1',
  applicants: ['UFMT'],
  inventors: ['ADA'],
  presentationFileId: null,
  patentInfoFileId: null,
  language: 'PT',
  classifications: ['H01L 31/04'],
  priorities: [{ value: 'BR1', countryAcronym: 'BR', date: '2022-01-01T00:00:00Z' }],
};

describe('PatentsPage & PatentDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(projectsApi.getProject).mockResolvedValue({
      id: 'proj-1',
      title: 'Solar',
      description: null,
      isPublic: false,
      createdAt: null,
      updateAt: null,
      ownerId: 'u-1',
      ownerName: 'Admin',
      patentCount: 1,
      observerIds: [],
    });
    vi.mocked(referenceApi.listCountries).mockResolvedValue([
      { acronym: 'BR', name: 'BRAZIL', states: [] },
    ]);
    vi.mocked(patentsApi.listPatents).mockResolvedValue({
      content: [samplePatent],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    vi.mocked(patentsApi.createPatent).mockResolvedValue(samplePatent);
    vi.mocked(patentsApi.getPatent).mockResolvedValue(samplePatent);
    vi.mocked(filesApi.uploadPatentFile).mockResolvedValue({
      ...samplePatent,
      presentationFileId: 'file-99',
    });
  });

  it('creates a manual patent from PatentsPage modal', async () => {
    const user = userEvent.setup();
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <I18nProvider initialLocale="pt-BR">
        <QueryClientProvider client={qc}>
          <MemoryRouter initialEntries={['/projects/proj-1/patents']}>
            <Routes>
              <Route path="/projects/:id/patents" element={<PatentsPage />} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      </I18nProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('patent-create')).toBeInTheDocument();
    });

    await user.click(screen.getByTestId('patent-create'));
    await user.type(screen.getByTestId('new-patent-title'), 'Manual Solar');
    await user.type(screen.getByTestId('new-patent-pubnum'), 'BR2025001');
    await user.click(screen.getByTestId('patent-create-save'));

    await waitFor(() => {
      expect(patentsApi.createPatent).toHaveBeenCalledWith(
        'proj-1',
        expect.objectContaining({
          title: 'Manual Solar',
          publicationNumber: 'BR2025001',
        }),
      );
    });
  });

  it('uploads patent presentation attachment on PatentDetailPage', async () => {
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <I18nProvider initialLocale="pt-BR">
        <QueryClientProvider client={qc}>
          <MemoryRouter initialEntries={['/projects/proj-1/patents/pat-1']}>
            <Routes>
              <Route path="/projects/:id/patents/:patentId" element={<PatentDetailPage />} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      </I18nProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('upload-presentation')).toBeInTheDocument();
    });

    const file = new File(['hello'], 'deck.pdf', { type: 'application/pdf' });
    fireEvent.change(screen.getByTestId('upload-presentation'), { target: { files: [file] } });

    await waitFor(() => {
      expect(filesApi.uploadPatentFile).toHaveBeenCalledWith('pat-1', file, 'presentation');
    });
  });
});
