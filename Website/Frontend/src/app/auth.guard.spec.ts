import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService, UserProfile } from './auth.service';
import { authenticatedGuard, teacherGuard } from './auth.guard';

describe('route guards', () => {
  const authService = {
    loadProfile: vi.fn<() => Promise<UserProfile | null>>(),
  };

  beforeEach(() => {
    authService.loadProfile.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    });
  });

  it('allows an authenticated student to use general platform routes', async () => {
    authService.loadProfile.mockResolvedValue(profile('STUDENT'));

    const result = await runGuard(authenticatedGuard, '/games');

    expect(result).toBe(true);
  });

  it('redirects unauthenticated users to the start page', async () => {
    authService.loadProfile.mockResolvedValue(null);

    const result = await runGuard(authenticatedGuard, '/games');

    expect(url(result)).toBe('/?reason=login-required&returnUrl=%2Fgames');
  });

  it('redirects inactive users to the start page', async () => {
    authService.loadProfile.mockResolvedValue({ ...profile('TEACHER'), active: false });

    const result = await runGuard(authenticatedGuard, '/games');

    expect(url(result)).toBe('/?reason=account-inactive');
  });

  it('redirects to a retry message when loading the profile fails', async () => {
    authService.loadProfile.mockRejectedValue(new Error('backend unavailable'));

    const result = await runGuard(authenticatedGuard, '/games');

    expect(url(result)).toBe('/?reason=session-check-failed');
  });

  it('redirects students away from teacher routes', async () => {
    authService.loadProfile.mockResolvedValue(profile('STUDENT'));

    const result = await runGuard(teacherGuard, '/createGame');

    expect(url(result)).toBe('/home?reason=teacher-required');
  });

  it.each(['TEACHER', 'ADMIN'] as const)('allows %s users to use teacher routes', async (role) => {
    authService.loadProfile.mockResolvedValue(profile(role));

    const result = await runGuard(teacherGuard, '/createGame');

    expect(result).toBe(true);
  });

  function runGuard(
    guard: typeof authenticatedGuard,
    targetUrl: string,
  ): Promise<boolean | UrlTree> {
    return TestBed.runInInjectionContext(() =>
      guard(
        {} as ActivatedRouteSnapshot,
        { url: targetUrl } as RouterStateSnapshot,
      ) as Promise<boolean | UrlTree>,
    );
  }

  function url(result: boolean | UrlTree): string {
    expect(result).toBeInstanceOf(UrlTree);
    return TestBed.inject(Router).serializeUrl(result as UrlTree);
  }

  function profile(role: UserProfile['role']): UserProfile {
    return {
      id: '1',
      externalSubject: 'subject',
      username: 'user',
      displayName: 'Test User',
      role,
      active: true,
    };
  }
});
