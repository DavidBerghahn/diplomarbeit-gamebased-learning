import { authenticatedGuard } from './auth.guard';
import { routes } from './app.routes';

describe('application routes', () => {
  it.each(['lobby', 'lobby/:id', 'statistics'])('protects %s as an authenticated route', (path) => {
    const route = routes.find((candidate) => candidate.path === path);

    expect(route?.canActivate).toEqual([authenticatedGuard]);
  });
});
