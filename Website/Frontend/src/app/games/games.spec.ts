import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AuthService, UserProfile } from '../auth.service';
import { AdminViewService } from '../admin-view.service';
import { GameRestService } from '../game-rest.service';
import { Game } from '../model/game.model';
import { Games } from './games';

describe('Games', () => {
  let component: Games;
  let fixture: ComponentFixture<Games>;
  const gameRestService = {
    getPublicGames: vi.fn().mockResolvedValue([]),
  };
  const authService = {
    loadProfile: vi.fn<() => Promise<UserProfile | null>>(),
  };
  const router = {
    navigate: vi.fn().mockResolvedValue(true),
  };

  beforeEach(async () => {
    gameRestService.getPublicGames.mockClear();
    authService.loadProfile.mockReset();
    router.navigate.mockClear();
    await TestBed.configureTestingModule({
      imports: [Games],
      providers: [
        { provide: GameRestService, useValue: gameRestService },
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();
  });

  it('allows students to open the solo/multiplayer game dialog', async () => {
    await createGames('STUDENT');

    const card = fixture.nativeElement.querySelector('.game-card') as HTMLElement;
    card.click();
    fixture.detectChanges();

    expect(card.getAttribute('role')).toBe('button');
    expect(card.getAttribute('tabindex')).toBe('0');
    expect(component.selectedGameForHosting?.id).toBe('game-1');
    expect(fixture.nativeElement.querySelector('.host-dialog')?.textContent).toContain('Solo spielen');
  });

  it.each(['TEACHER', 'ADMIN'] as const)('allows %s users to open the hosting dialog', async (role) => {
    await createGames(role);

    const card = fixture.nativeElement.querySelector('.game-card') as HTMLElement;
    card.click();
    fixture.detectChanges();

    expect(card.getAttribute('role')).toBe('button');
    expect(component.selectedGameForHosting?.id).toBe('game-1');
    expect(fixture.nativeElement.querySelector('.host-dialog')?.textContent).toContain('Lobby erstellen');
  });

  it('shows the student game view to admins after they switch views', async () => {
    await createGames('ADMIN');
    TestBed.inject(AdminViewService).select('STUDENT');
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('.game-card') as HTMLElement;
    card.click();
    fixture.detectChanges();

    expect(component.profile()?.role).toBe('ADMIN');
    expect(component.canHost()).toBe(false);
    expect(card.getAttribute('role')).toBe('button');
    expect(component.selectedGameForHosting?.id).toBe('game-1');
  });

  async function createGames(role: UserProfile['role']): Promise<void> {
    authService.loadProfile.mockResolvedValue(profile(role));
    fixture = TestBed.createComponent(Games);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    component.profile.set(profile(role));
    component.games.set([game]);
    component.setSelectedGameType('Quizbattle');
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

  const game: Game = {
    id: 'game-1',
    spiel_typ: 'Quizbattle',
    lehrer: 'Teacher',
    name: 'Test game',
    zweige: [],
    gespielte_runden: 1,
    fragen: [],
  };
});
