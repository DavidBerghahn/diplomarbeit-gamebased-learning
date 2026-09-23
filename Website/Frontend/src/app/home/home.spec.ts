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
  });

  it.each(['TEACHER', 'ADMIN'] as const)('shows game management actions to %s users', async (role) => {
    await createHome(role);

    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('Meine Spiele');
    expect(text).toContain('Spiel erstellen');
  });

  async function createHome(role: UserProfile['role']): Promise<void> {
    authService.loadProfile.mockResolvedValue(profile(role));
    fixture = TestBed.createComponent(Home);
    component = fixture.componentInstance;
    component.profile = profile(role);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
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
