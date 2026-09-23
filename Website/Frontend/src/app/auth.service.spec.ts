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
