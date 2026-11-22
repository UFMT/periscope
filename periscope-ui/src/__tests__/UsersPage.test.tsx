import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import * as usersApi from '../api/users';
import { I18nProvider } from '../i18n/I18nProvider';
import { UsersPage } from '../pages/UsersPage';
import type { User } from '../types';

vi.mock('../api/users');
vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      id: 'u-admin',
      username: 'admin',
      firstname: 'Admin',
      lastname: 'User',
      email: 'admin@ufmt.br',
      userLevel: 'ADMIN',
    },
    isAdmin: true,
  }),
}));

function renderUsersPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <I18nProvider initialLocale="pt-BR">
      <QueryClientProvider client={qc}>
        <MemoryRouter>
          <UsersPage />
        </MemoryRouter>
      </QueryClientProvider>
    </I18nProvider>,
  );
}

describe('UsersPage', () => {
  const baseUsers: User[] = [
    {
      id: 'u-admin',
      username: 'admin',
      firstname: 'Admin',
      lastname: 'User',
      email: 'admin@ufmt.br',
      userLevel: 'ADMIN',
    },
    {
      id: 'u-2',
      username: 'alice',
      firstname: 'Alice',
      lastname: 'Silva',
      email: 'alice@ufmt.br',
      userLevel: 'USER',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(usersApi.listUsers).mockResolvedValue(baseUsers);
    vi.mocked(usersApi.createUser).mockResolvedValue({
      id: 'u-3',
      username: 'bob',
      firstname: 'Bob',
      lastname: 'Costa',
      email: 'bob@ufmt.br',
      userLevel: 'USER',
    });
    vi.mocked(usersApi.updateUser).mockResolvedValue({
      ...baseUsers[1],
      firstname: 'Alice Updated',
    });
    vi.mocked(usersApi.deleteUser).mockResolvedValue(undefined);
  });

  it('lists users, disables self-deletion, creates and edits a user', async () => {
    const user = userEvent.setup();
    renderUsersPage();

    await waitFor(() => {
      expect(screen.getByTestId('users-table')).toBeInTheDocument();
    });

    const adminRow = screen.getByTestId('user-row-admin');
    expect(adminRow.querySelector('button.btn-danger')).toBeDisabled();

    await user.click(screen.getByTestId('user-create'));
    await user.type(screen.getByTestId('user-username'), 'bob');
    await user.type(screen.getByTestId('user-firstname'), 'Bob');
    await user.type(screen.getByTestId('user-password'), 'Secret123');
    await user.click(screen.getByTestId('user-save'));

    await waitFor(() => {
      expect(usersApi.createUser).toHaveBeenCalledWith(
        expect.objectContaining({
          username: 'bob',
          firstname: 'Bob',
          password: 'Secret123',
        }),
      );
    });

    const aliceRow = screen.getByTestId('user-row-alice');
    await user.click(aliceRow.querySelector('button.btn-secondary')!);
    const firstInput = screen.getByTestId('user-firstname');
    await user.clear(firstInput);
    await user.type(firstInput, 'Alice Updated');
    await user.click(screen.getByTestId('user-save'));

    await waitFor(() => {
      expect(usersApi.updateUser).toHaveBeenCalledWith(
        'u-2',
        expect.objectContaining({ firstname: 'Alice Updated' }),
      );
    });
  });
});
