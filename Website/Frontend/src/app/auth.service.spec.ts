import { AuthService, UserProfile } from './auth.service';

describe('AuthService login return URL', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, '', '/');
    sessionStorage.clear();
  });

  it('preserves an allowed internal teacher deep-link after the PKCE callback', async () => {
    const service = setupCallback('/createGame?draft=1');

    await service.finishLogin();

    expect(service.takePostLoginUrl()).toBe('/createGame?draft=1');
    expect(service.takePostLoginUrl()).toBe('/home');
  });

  it.each([
    'https://example.org/createGame',
    '//example.org/createGame',
    '/not-a-platform-route',
  ])('rejects unsafe return URL %s', async (returnUrl) => {
    const service = setupCallback(returnUrl);

    await service.finishLogin();

    expect(service.takePostLoginUrl()).toBe('/home');
  });

  function setupCallback(returnUrl: string): AuthService {
    window.history.replaceState({}, '', '/?code=code&state=state');
    sessionStorage.setItem('gamebased.keycloak.pkce', JSON.stringify({
      state: 'state',
      codeVerifier: 'verifier',
      returnUrl,
    }));

    const profile: UserProfile = {
      id: '1',
      externalSubject: 'subject',
      username: 'teacher',
      displayName: 'Teacher',
      role: 'TEACHER',
      active: true,
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(response({
        provider: 'keycloak',
        keycloakUrl: 'https://auth.example.org',
        keycloakRealm: 'school',
        keycloakClientId: 'frontend',
      }))
      .mockResolvedValueOnce(response({
        access_token: 'access-token',
        expires_in: 300,
      }))
      .mockResolvedValueOnce(response(profile));
    vi.stubGlobal('fetch', fetchMock);
    return new AuthService();
  }

  function response(body: unknown): Response {
    return new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    });
  }
});

describe('AuthService token renewal', () => {
  const storageKey = 'gamebased.keycloak.tokens';
  const profile: UserProfile = {
    id: '1',
    externalSubject: 'subject',
    username: 'student',
    displayName: 'Student',
    role: 'STUDENT',
    active: true,
  };

  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it('renews an expired access token and stores the rotated refresh token', async () => {
    setTokens({ expires_at: Date.now() - 1_000 });
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(response(config()))
      .mockResolvedValueOnce(response({
        access_token: 'new-access',
        refresh_token: 'new-refresh',
        expires_in: 300,
        refresh_expires_in: 1_800,
      }))
      .mockResolvedValueOnce(response(profile));
    vi.stubGlobal('fetch', fetchMock);

    const service = new AuthService();
    const result = await service.loadProfile();

    expect(result).toEqual(profile);
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[1][0]).toBe('https://auth.example.org/realms/school/protocol/openid-connect/token');
    const tokenRequest = fetchMock.mock.calls[1][1] as RequestInit;
    expect(tokenRequest.body).toBeInstanceOf(URLSearchParams);
    expect((tokenRequest.body as URLSearchParams).toString()).toBe(
      'grant_type=refresh_token&client_id=frontend&refresh_token=old-refresh',
    );
    expect(fetchMock.mock.calls[2][1].headers.Authorization).toBe('Bearer new-access');
    const stored = JSON.parse(sessionStorage.getItem(storageKey)!);
    expect(stored.refresh_token).toBe('new-refresh');
    expect(stored.expires_at).toBeGreaterThan(Date.now());
    expect(stored.refresh_expires_at).toBeGreaterThan(stored.expires_at);

    sessionStorage.setItem(storageKey, JSON.stringify({ ...stored, expires_at: Date.now() - 1_000 }));
    fetchMock.mockResolvedValueOnce(response({
      access_token: 'newer-access',
      refresh_token: 'newer-refresh',
      expires_in: 300,
      refresh_expires_in: 1_800,
    }));

    expect(await service.authorizationHeaders()).toEqual({ Authorization: 'Bearer newer-access' });
    expect((fetchMock.mock.calls[3][1].body as URLSearchParams).get('refresh_token')).toBe('new-refresh');
  });

  it('coalesces parallel requests into one refresh', async () => {
    setTokens({ expires_at: Date.now() - 1_000 });
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(response(config()))
      .mockResolvedValueOnce(response({ access_token: 'new-access', expires_in: 300 }))
      .mockResolvedValueOnce(response(profile));
    vi.stubGlobal('fetch', fetchMock);
    const service = new AuthService();

    const [loadedProfile, headers] = await Promise.all([
      service.loadProfile(),
      service.authorizationHeaders(),
    ]);

    expect(loadedProfile).toEqual(profile);
    expect(headers).toEqual({ Authorization: 'Bearer new-access' });
    expect(fetchMock.mock.calls.filter(([url]) => String(url).endsWith('/token'))).toHaveLength(1);
    expect(JSON.parse(sessionStorage.getItem(storageKey)!).refresh_token).toBe('old-refresh');
  });

  it('clears the session when the refresh token is missing or expired', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    setTokens({ expires_at: Date.now() - 1_000, refresh_token: undefined });
    expect(await new AuthService().loadProfile()).toBeNull();
    expect(sessionStorage.getItem(storageKey)).toBeNull();

    setTokens({ expires_at: Date.now() - 1_000, refresh_expires_at: Date.now() - 1_000 });
    expect(await new AuthService().authorizationHeaders()).toEqual({});
    expect(sessionStorage.getItem(storageKey)).toBeNull();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('does not claim a session when no tokens are stored', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const service = new AuthService();

    expect(await service.loadProfile()).toBeNull();
    expect(await service.authorizationHeaders()).toEqual({});
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('clears the session after Keycloak rejects refresh', async () => {
    setTokens({ expires_at: Date.now() - 1_000 });
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(response(config()))
      .mockResolvedValueOnce(response({ error: 'invalid_grant' }, 400));
    vi.stubGlobal('fetch', fetchMock);

    expect(await new AuthService().loadProfile()).toBeNull();
    expect(sessionStorage.getItem(storageKey)).toBeNull();
  });

  it('does not use an expired access token when refresh is temporarily unavailable', async () => {
    setTokens({ expires_at: Date.now() - 1_000 });
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(response(config()))
      .mockRejectedValueOnce(new Error('network unavailable'));
    vi.stubGlobal('fetch', fetchMock);

    await expect(new AuthService().authorizationHeaders()).rejects.toThrow('Keycloak-Token konnte nicht erneuert werden.');
    expect(JSON.parse(sessionStorage.getItem(storageKey)!).refresh_token).toBe('old-refresh');
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('clears a token rejected by the backend', async () => {
    setTokens({ expires_at: Date.now() + 300_000 });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response({}, 401)));

    expect(await new AuthService().loadProfile()).toBeNull();
    expect(sessionStorage.getItem(storageKey)).toBeNull();
  });

  it('refreshes proactively while the page stays open', async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-09-30T12:00:00Z'));
    setTokens({ expires_at: Date.now() + 300_000 });
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(response(config()))
      .mockResolvedValueOnce(response({ access_token: 'new-access', expires_in: 300 }));
    vi.stubGlobal('fetch', fetchMock);
    new AuthService();

    await vi.advanceTimersByTimeAsync(240_000);

    expect(fetchMock.mock.calls.filter(([url]) => String(url).endsWith('/token'))).toHaveLength(1);
    expect(JSON.parse(sessionStorage.getItem(storageKey)!).access_token).toBe('new-access');
  });

  function setTokens(changes: Record<string, unknown>): void {
    sessionStorage.setItem(storageKey, JSON.stringify({
      access_token: 'old-access',
      refresh_token: 'old-refresh',
      expires_in: 300,
      expires_at: Date.now() + 300_000,
      refresh_expires_at: Date.now() + 1_800_000,
      ...changes,
    }));
  }

  function config(): unknown {
    return {
      provider: 'keycloak',
      keycloakUrl: 'https://auth.example.org',
      keycloakRealm: 'school',
      keycloakClientId: 'frontend',
    };
  }

  function response(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    });
  }
});
