import { Injector, runInInjectionContext } from '@angular/core';
import { AuthService } from './auth.service';
import { GameRestService } from './game-rest.service';

describe('GameRestService authentication', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it('uses the renewed access token for game requests', async () => {
    sessionStorage.setItem('gamebased.keycloak.tokens', JSON.stringify({
      access_token: 'old-access',
      refresh_token: 'old-refresh',
      expires_in: 300,
      expires_at: Date.now() - 1_000,
      refresh_expires_at: Date.now() + 1_800_000,
    }));
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(response({
        provider: 'keycloak',
        keycloakUrl: 'https://auth.example.org',
        keycloakRealm: 'school',
        keycloakClientId: 'frontend',
      }))
      .mockResolvedValueOnce(response({
        access_token: 'new-access',
        refresh_token: 'new-refresh',
        expires_in: 300,
        refresh_expires_in: 1_800,
      }))
      .mockResolvedValueOnce(response([]));
    vi.stubGlobal('fetch', fetchMock);

    const authService = new AuthService();
    const injector = Injector.create({ providers: [{ provide: AuthService, useValue: authService }] });
    const gameRestService = runInInjectionContext(injector, () => new GameRestService());

    expect(await gameRestService.getPublicGames()).toEqual([]);
    expect(fetchMock.mock.calls[2][0]).toBe('/api/games');
    expect(fetchMock.mock.calls[2][1].headers.Authorization).toBe('Bearer new-access');
  });

  function response(body: unknown): Response {
    return new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    });
  }
});
