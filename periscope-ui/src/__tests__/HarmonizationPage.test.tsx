import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import * as harmApi from '../api/harmonization';
import * as referenceApi from '../api/reference';
import { I18nProvider } from '../i18n/I18nProvider';
import { HarmonizationPage } from '../pages/HarmonizationPage';

vi.mock('../api/harmonization');
vi.mock('../api/reference');

describe('HarmonizationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(referenceApi.listCountries).mockResolvedValue([
      {
        acronym: 'BR',
        name: 'BRAZIL',
        states: [{ acronym: 'MT', name: 'Mato Grosso', region: 'Centro-Oeste' }],
      },
    ]);
    vi.mocked(referenceApi.listApplicantTypes).mockResolvedValue(['public.university']);
    vi.mocked(harmApi.listEntities).mockResolvedValue({
      content: [
        {
          name: 'UNIV FED MATO GROSSO',
          acronym: null,
          countryAcronym: 'BR',
          stateAcronym: 'MT',
          nature: null,
          harmonized: false,
          documentCount: 4,
        },
      ],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    });
    vi.mocked(harmApi.listRules).mockResolvedValue([
      {
        id: 'r-1',
        name: 'UFMT',
        acronym: 'UFMT',
        type: 'APPLICANT',
        substitutions: ['UNIV FED MATO GROSSO'],
        countryAcronym: 'BR',
        stateAcronym: 'MT',
        nature: 'public.university',
        projectId: 'proj-1',
      },
    ]);
    vi.mocked(harmApi.getSuggestions).mockResolvedValue({
      type: 'applicant',
      query: 'UFMT',
      suggestions: ['UNIV FED MATO GROSSO'],
    });
    vi.mocked(harmApi.createRule).mockResolvedValue({
      id: 'r-2',
      name: 'UFMT NEW',
      acronym: 'UFMT',
      type: 'APPLICANT',
      substitutions: [],
      countryAcronym: 'BR',
      stateAcronym: 'MT',
      nature: 'public.university',
      projectId: 'proj-1',
    });
    vi.mocked(harmApi.undoRule).mockResolvedValue({
      undone: 1,
      ruleId: 'r-1',
      message: 'Rule undone',
    });
  });

  it('lists entities with nature translations and creates/undoes rules with country and state', async () => {
    const user = userEvent.setup();
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <I18nProvider initialLocale="pt-BR">
        <QueryClientProvider client={qc}>
          <MemoryRouter initialEntries={['/projects/proj-1/harmonization']}>
            <Routes>
              <Route path="/projects/:id/harmonization" element={<HarmonizationPage />} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      </I18nProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('entities-table')).toBeInTheDocument();
      expect(screen.getByTestId('rules-table')).toBeInTheDocument();
    });

    expect(screen.getByTestId('rules-table')).toHaveTextContent('Universidade Pública');

    await user.type(screen.getByTestId('rule-name'), 'UFMT NEW');
    await user.selectOptions(screen.getByTestId('rule-country'), 'BR');
    await waitFor(() => {
      expect(screen.getByTestId('rule-state').tagName).toBe('SELECT');
    });
    await user.selectOptions(screen.getByTestId('rule-state'), 'MT');
    await user.selectOptions(screen.getByTestId('rule-nature'), 'public.university');
    await user.click(screen.getByTestId('rule-submit'));

    await waitFor(() => {
      expect(harmApi.createRule).toHaveBeenCalledWith(
        'proj-1',
        expect.objectContaining({
          name: 'UFMT NEW',
          countryAcronym: 'BR',
          stateAcronym: 'MT',
          nature: 'public.university',
        }),
      );
    });

    await user.click(screen.getByRole('button', { name: 'Desfazer' }));
    await waitFor(() => {
      expect(harmApi.undoRule).toHaveBeenCalledWith('proj-1', 'r-1');
    });
  });
});
