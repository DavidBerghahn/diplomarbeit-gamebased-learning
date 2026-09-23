import { inject } from '@angular/core';
import { CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthService, UserProfile } from './auth.service';

const managementRoles: ReadonlySet<UserProfile['role']> = new Set(['TEACHER', 'ADMIN']);

async function loadActiveProfile(
  authService: AuthService,
  router: Router,
  state: RouterStateSnapshot,
): Promise<UserProfile | UrlTree> {
  try {
    const profile = await authService.loadProfile();

    if (!profile) {
      return router.createUrlTree(['/'], {
        queryParams: { reason: 'login-required', returnUrl: state.url },
      });
    }

    if (!profile.active) {
      return router.createUrlTree(['/'], {
        queryParams: { reason: 'account-inactive' },
      });
    }

    return profile;
  } catch {
    return router.createUrlTree(['/'], {
      queryParams: { reason: 'session-check-failed' },
    });
  }
}

export const authenticatedGuard: CanActivateFn = async (_route, state) => {
  const profile = await loadActiveProfile(inject(AuthService), inject(Router), state);
  return profile instanceof UrlTree ? profile : true;
};

export const teacherGuard: CanActivateFn = async (_route, state) => {
  const router = inject(Router);
  const profile = await loadActiveProfile(inject(AuthService), router, state);

  if (profile instanceof UrlTree) {
    return profile;
  }

  if (!managementRoles.has(profile.role)) {
    return router.createUrlTree(['/home'], {
      queryParams: { reason: 'teacher-required' },
    });
  }

  return true;
};
