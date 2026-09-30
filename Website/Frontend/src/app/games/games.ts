import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { GameRestService } from '../game-rest.service';
import { Game } from '../model/game.model';
import {FormsModule} from '@angular/forms';
import { AuthService, UserProfile } from '../auth.service';
import { AdminViewService } from '../admin-view.service';
import { DEMO_SMART10_GAMES } from '../model/demo-games';

@Component({
  selector: 'app-games',
  imports: [DatePipe, FormsModule],
  templateUrl: './games.html',
  styleUrl: './games.css',
})
export class Games implements OnInit {
  private readonly gameRestService = inject(GameRestService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly adminViewService = inject(AdminViewService);

  games = signal<Game[]>([]);
  profile = signal<UserProfile | null>(null);
  canHost = computed(() => this.profile()?.role === 'TEACHER' ||
    (this.profile()?.role === 'ADMIN' && this.adminViewService.view() === 'TEACHER'));
  quizbattleGames = computed(() =>
    this.games().filter((game) => ['Quizbattle', 'Smart10', 'Smart10 / Quizbattle'].includes(game.spiel_typ)),
  );
  duellUmDieWeltGames = computed(() =>
    this.games().filter((game) => game.spiel_typ === 'DuellUmDieWelt'),
  );

  selectedGameType: 'Quizbattle' | 'DuellUmDieWelt' | '' = '';
  selectedGameForHosting: Game | null = null;
  copyMessage = '';

  selectedGames = computed(() => {
    if (this.selectedGameType === 'Quizbattle') {
      return this.quizbattleGames();
    }

    if (this.selectedGameType === 'DuellUmDieWelt') {
      return this.duellUmDieWeltGames();
    }

    return [];
  });

  selectedGameTypeTitle = computed(() => {
    if (this.selectedGameType === 'Quizbattle') {
      return 'Smart10 / Quizbattle';
    }

    if (this.selectedGameType === 'DuellUmDieWelt') {
      return 'Duell um die Welt';
    }

    return '';
  });

  setSelectedGameType(gameType: 'Quizbattle' | 'DuellUmDieWelt'){
    this.selectedGameType = gameType;
  }

  clearSelectedGameType(): void {
    this.selectedGameType = '';
  }

  openHostDialog(event: Event, game: Game): void {
    event.stopPropagation();
    this.selectedGameForHosting = game;
  }

  async playSolo(): Promise<void> {
    const game = this.selectedGameForHosting;
    if (!game) return;
    await this.router.navigate(['/solo', game.id]);
  }

  closeHostDialog(): void {
    this.selectedGameForHosting = null;
  }

  async hostSelectedGame(): Promise<void> {
    const game = this.selectedGameForHosting;
    if (!game) {
      return;
    }

    await this.router.navigate(['/lobby', game.id]);
  }

  async copySelectedGame(): Promise<void> {
    const game = this.selectedGameForHosting;
    if (!game || !this.canHost()) return;

    try {
      await this.gameRestService.copyGame(game.id);
      this.copyMessage = 'Eine neue Kopie wurde in „Meine Spiele“ angelegt.';
    } catch (error) {
      this.copyMessage = error instanceof Error ? error.message : 'Das Spiel konnte nicht kopiert werden.';
    }
  }

  ngOnInit(): void {
    void this.loadGames();
    void this.loadProfile();
  }

  async loadGames(): Promise<void> {
    try {
      const remoteGames = await this.gameRestService.getPublicGames();
      this.games.set(this.withDemoGames(remoteGames));
    } catch {
      this.games.set(DEMO_SMART10_GAMES);
    }
  }

  private withDemoGames(games: Game[]): Game[] {
    const existingIds = new Set(games.map((game) => game.id));
    return [...games, ...DEMO_SMART10_GAMES.filter((game) => !existingIds.has(game.id))];
  }

  async loadProfile(): Promise<void> {
    this.profile.set(await this.authService.loadProfile());
  }
}
