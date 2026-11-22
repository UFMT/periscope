import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import * as projectsApi from '../api/projects';
import * as referenceApi from '../api/reference';
import { I18nProvider } from '../i18n/I18nProvider';
import { ProjectsPage } from '../pages/ProjectsPage';

vi.mock('../api/projects');
vi.mock('../api/reference');
vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      id: 'u-owner',
      username: 'owner',
      firstname: 'Owner',
      lastname: 'User',
      email: 'owner@ufmt.br',
      userLevel: 'USER',
    },
    isAdmin: false,
  }),
}));

describe('ProjectsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(projectsApi.listProjects).mockResolvedValue([]);
    vi.mocked(referenceApi.listReferenceUsers).mockResolvedValue([
      { id: 'u-owner', username: 'owner', firstname: 'Owner', lastname: 'User' },
      { id: 'u-collab', username: 'collab', firstname: 'Maria', lastname: 'Silva' },
    ]);
    vi.mocked(projectsApi.createProject).mockResolvedValue({
      id: 'p-1',
      title: 'Solar',
      description: 'Desc',
      isPublic: false,
      createdAt: null,
      updateAt: null,
      ownerId: 'u-owner',
      ownerName: 'Owner User',
      patentCount: 0,
      observerIds: ['u-collab'],
    });
  });

  it('allows selecting observers when creating a project', async () => {
    const user = userEvent.setup();
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <I18nProvider initialLocale="pt-BR">
        <QueryClientProvider client={qc}>
          <MemoryRouter initialEntries={['/projects']}>
            <Routes>
              <Route path="/projects" element={<ProjectsPage />} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      </I18nProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('project-create')).toBeInTheDocument();
    });

    await user.click(screen.getByTestId('project-create'));
    await user.type(screen.getByTestId('project-title'), 'Solar');

    await waitFor(() => {
      expect(screen.getByTestId('project-observers')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /Maria Silva/i }));
    await user.click(screen.getByTestId('project-save'));

    await waitFor(() => {
      expect(projectsApi.createProject).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'Solar',
          observerIds: ['u-collab'],
        }),
      );
    });
  });
});
