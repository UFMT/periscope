import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  apiRequest,
  clearSession,
  getStoredToken,
  setStoredToken,
  setUnauthorizedHandler,
} from '../api/client';

describe('apiRequest client', () => {
  afterEach(() => {
    clearSession();
    setUnauthorizedHandler(null);
    vi.restoreAllMocks();
  });

  it('sends credentials include, never writes JWT to localStorage, and sets Authorization header', async () => {
    setStoredToken('mem-token-123');
    expect(localStorage.getItem('periscope.token')).toBeNull();
    expect(getStoredToken()).toBe('mem-token-123');

    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    const res = await apiRequest<{ ok: boolean }>('/projects');
    expect(res.ok).toBe(true);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [, init] = fetchMock.mock.calls[0];
    expect(init?.credentials).toBe('include');
    expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer mem-token-123');
  });

  it('clears session and invokes unauthorized handler on 401', async () => {
    setStoredToken('stale-token');
    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ error: 'Token revoked', status: 401 }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    await expect(apiRequest('/projects')).rejects.toThrow('Token revoked');
    expect(getStoredToken()).toBeNull();
    expect(handler).toHaveBeenCalledTimes(1);
  });
});
