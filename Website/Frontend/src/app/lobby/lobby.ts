import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';
import { GameRestService } from '../game-rest.service';
import { DEMO_SMART10_GAMES } from '../model/demo-games';
import { Game } from '../model/game.model';
import {
  MultiplayerRoomState,
  MultiplayerTeam,
  MultiplayerWebSocketService,
} from '../multiplayer-websocket.service';

@Component({
  selector: 'app-lobby',
  imports: [FormsModule, RouterLink],
  templateUrl: './lobby.html',
  styleUrl: './lobby.css',
})
export class Lobby implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly gameRestService = inject(GameRestService);
  readonly multiplayer = inject(MultiplayerWebSocketService);

  readonly game = signal<Game | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly profileName = signal('Spieler');
  readonly teamCount = signal(4);
  readonly assignmentMode = signal<'self' | 'random'>('self');
  readonly answerDraft = signal('');
  readonly turnSeconds = signal(20);
  readonly configuredSeconds = signal(20);
  readonly hostPlays = signal(false);
  readonly allowUneven = signal(false);
  readonly busy = signal(false);
  readonly clueDraft = signal('');
  readonly guessDraft = signal('');
  readonly uneven = computed(() => {
    const sizes = this.room()?.teams.map((team) => team.members.length) ?? [0];
    return Math.max(...sizes) - Math.min(...sizes) > 1;
  });
  private timer?: ReturnType<typeof setInterval>;

  readonly room = this.multiplayer.room;
  readonly currentPlayer = computed(
    () =>
      this.room()?.players.find((player) => player.id === this.multiplayer.connectionId) ?? null,
  );
  readonly currentTeam = computed(() => {
    const room = this.room();
    return room?.teams.find((team) => team.number === room.currentTeam) ?? null;
  });
  readonly isHost = computed(() => this.room()?.hostId === this.multiplayer.connectionId);
  readonly isCurrentLeader = computed(
    () => this.currentTeam()?.leader === this.multiplayer.connectionId,
  );

  async ngOnInit(): Promise<void> {
    try {
      const profile = await this.authService.loadProfile();
      this.profileName.set(profile?.displayName || profile?.username || 'Spieler');
    } catch {
      /* The WebSocket still receives the login name when available. */
    }

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      const localGame = DEMO_SMART10_GAMES.find((game) => game.id === id);
      try {
        this.game.set(localGame ?? (await this.gameRestService.getGame(id)));
      } catch {
        this.game.set(localGame ?? null);
      }
      this.loading.set(false);
    } else if (this.room()) {
      this.loading.set(false);
    } else {
      this.loading.set(false);
      this.error.set('Kein Spielraum ausgewählt.');
    }

    this.timer = setInterval(() => this.updateTurnTimer(), 250);
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
    if (this.room()) void this.multiplayer.leave().catch(() => {});
  }

  get answerCount(): number {
    return (
      this.game()?.fragen.reduce(
        (sum, question) => sum + question.antwortmoeglichkeiten.length,
        0,
      ) ?? 0
    );
  }

  async createLobby(): Promise<void> {
    const game = this.game();
    if (!game) return;
    try {
      await this.multiplayer.createRoom(
        game,
        this.profileName(),
        this.teamCount(),
        this.assignmentMode(),
        this.configuredSeconds(),
        this.hostPlays(),
      );
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : 'Der Spielraum konnte nicht geöffnet werden.',
      );
    }
  }

  async startGame(): Promise<void> {
    const room = this.room();
    if (!room || !this.isHost()) return;
    await this.run(() => this.multiplayer.startRoom(room.code, this.allowUneven()));
  }

  async selectCard(cardId: string): Promise<void> {
    const room = this.room();
    if (!room || !this.isCurrentLeader() || room.phase !== 'PLAYING') return;
    await this.run(() => this.multiplayer.selectCard(room.code, cardId));
  }

  async answerTrueFalse(answer: boolean): Promise<void> {
    await this.answer(answer);
  }
  async answerText(): Promise<void> {
    await this.answer(this.answerDraft());
  }
  async answerOrdering(): Promise<void> {
    const value = Number(this.answerDraft());
    if (Number.isInteger(value)) await this.answer(value);
  }

  async passTurn(): Promise<void> {
    const room = this.room();
    if (room) await this.run(() => this.multiplayer.pass(room.code));
  }

  playersForTeam(team: MultiplayerTeam): string[] {
    const room = this.room();
    return team.members.map(
      (id) => room?.players.find((player) => player.id === id)?.name ?? 'Spieler',
    );
  }

  isOpen(card: { state: string }): boolean {
    return card.state === 'open';
  }

  playerName(id: string | null | undefined): string {
    return (
      this.room()?.players.find((player) => player.id === id)?.name ?? 'wird zum Start ausgelost'
    );
  }
  async chooseTeam(team: number, playerId?: string): Promise<void> {
    const room = this.room();
    if (room) await this.run(() => this.multiplayer.chooseTeam(room.code, team, playerId));
  }
  async retrySave(): Promise<void> {
    const room = this.room();
    if (room) await this.run(() => this.multiplayer.retrySave(room.code));
  }
  async justOne(action: string, data: Record<string, unknown> = {}): Promise<void> {
    const room = this.room();
    if (!room) return;
    await this.run(() => this.multiplayer.justOne(room.code, action, data));
    if (!this.error()) {
      this.clueDraft.set('');
      this.guessDraft.set('');
    }
  }
  async leave(): Promise<void> {
    await this.multiplayer.leave().catch(() => {});
    await this.router.navigate(['/games']);
  }

  private async answer(answer: string | boolean | number): Promise<void> {
    const room = this.room();
    if (!room || !this.isCurrentLeader() || room.selectedCard === null) return;
    await this.run(() => this.multiplayer.answer(room.code, answer));
    this.answerDraft.set('');
  }

  private async run(action: () => Promise<MultiplayerRoomState>): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    try {
      await action();
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : 'Der Multiplayer-Befehl ist fehlgeschlagen.',
      );
    } finally {
      this.busy.set(false);
    }
  }

  private updateTurnTimer(): void {
    const deadline = this.room()?.turnDeadline;
    const seconds = deadline ? Math.max(0, Math.ceil((deadline - Date.now()) / 1000)) : 0;
    this.turnSeconds.set(seconds);
  }
}
