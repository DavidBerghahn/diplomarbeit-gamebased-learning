import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService, UserProfile } from '../auth.service';
import { Home } from './home';

describe('Home', () => {
  let component: Home;
  let fixture: ComponentFixture<Home>;
  const authService = {
    loadProfile: vi.fn<() => Promise<UserProfile | null>>(),
    debugInfo: vi.fn(() => ({
      backendUser: null,
      token: {
        hasAccessToken: true,
        hasIdToken: true,
        expiresAt: null,
        expiresInSeconds: null,
      },
      accessTokenClaims: null,
      idTokenClaims: null,
    })),
  };

  beforeEach(async () => {
    authService.loadProfile.mockReset();
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();
  });

  it('should create', async () => {
    await createHome('STUDENT');

    expect(component).toBeTruthy();
  });

  it('hides game management actions from students', async () => {
    await createHome('STUDENT');
    const text = (fixture.nativeElement as HTMLElement).textContent;

    expect(text).not.toContain('Meine Spiele');
    expect(text).not.toContain('Spiel erstellen');
    expect(text).not.toContain('Schüleransicht');
  });

  it.each(['TEACHER', 'ADMIN'] as const)('shows game management actions to %s users', async (role) => {
    await createHome(role);

    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('Meine Spiele');
    expect(text).toContain('Spiel erstellen');
  });

  it('lets admins switch between student and teacher views without changing their role', async () => {
    await createHome('ADMIN');
    const home = fixture.nativeElement as HTMLElement;
    const buttons = [...home.querySelectorAll<HTMLButtonElement>('.view-switch-button')];

    expect(buttons.map((button) => button.textContent?.trim())).toEqual(['Schüleransicht', 'Lehreransicht']);
    expect(home.textContent).toContain('Spiel erstellen');

    buttons[0].click();
    fixture.detectChanges();
    expect(component.profile()?.role).toBe('ADMIN');
    expect(component.canManageGames).toBe(false);
    expect(home.textContent).not.toContain('Meine Spiele');
    expect(home.textContent).not.toContain('Spiel erstellen');
    expect(buttons[0].getAttribute('aria-pressed')).toBe('true');

    buttons[1].click();
    fixture.detectChanges();
    expect(component.canManageGames).toBe(true);
    expect(home.textContent).toContain('Meine Spiele');
    expect(home.textContent).toContain('Spiel erstellen');
    expect(buttons[1].getAttribute('aria-pressed')).toBe('true');
  });

  it('shows the admin view switch as soon as the profile finishes loading', async () => {
    let resolveProfile!: (value: UserProfile) => void;
    authService.loadProfile.mockReturnValue(new Promise((resolve) => { resolveProfile = resolve; }));
    fixture = TestBed.createComponent(Home);
    fixture.detectChanges();

    const home = fixture.nativeElement as HTMLElement;
    expect(home.textContent).not.toContain('Schüleransicht');

    resolveProfile(profile('ADMIN'));
    await Promise.resolve();
    await fixture.whenStable();

    expect(home.textContent).toContain('Schüleransicht');
    expect(home.textContent).toContain('Lehreransicht');
    expect(home.textContent).toContain('Spiel erstellen');
  });

  async function createHome(role: UserProfile['role']): Promise<void> {
    authService.loadProfile.mockResolvedValue(profile(role));
    fixture = TestBed.createComponent(Home);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await Promise.resolve();
    await fixture.whenStable();
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
