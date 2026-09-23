import { teacherGuard } from './auth.guard';
import { routes } from './app.routes';

describe('application routes', () => {
  it.each(['lobby', 'lobby/:id'])('protects %s as a teacher route', (path) => {
    const route = routes.find((candidate) => candidate.path === path);

    expect(route?.canActivate).toEqual([teacherGuard]);
  });
});
