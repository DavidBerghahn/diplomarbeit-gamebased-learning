import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { GameWebSocketService } from '../game-websocket.service';
import { Game } from '../model/game.model';

@Component({
  selector: 'app-lobby',
  imports: [FormsModule],
  templateUrl: './lobby.html',
  styleUrl: './lobby.css',
})
export class Lobby implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly gameWebSocketService = inject(GameWebSocketService);

  readonly game = signal<Game | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly lobbyCode = '4827';
  readonly teamColors = ['#09509d', '#6bb6dd', '#f18806', '#be1723', '#137a3f', '#6ec17d'];
  readonly participants = signal([
    'Teilnehmer 1',
    'Teilnehmer 2',
    'Teilnehmer 3',
    'Teilnehmer 4',
    'Teilnehmer 5',
    'Teilnehmer 6',
    'Teilnehmer 7',
  ]);

  readonly teamCount = signal(4);
  readonly teamAssignments = signal<Record<string, number>>({});
  readonly setupComplete = signal(false);
  assignmentMode: 'self' | 'random' = 'self';
  showStartDialog = false;

  readonly visibleTeams = computed(() =>
    Array.from({ length: this.teamCount() }, (_, index) => ({
      name: `Team ${index + 1}`,
      color: this.teamColors[index],
      members: this.participants().filter((participant) => this.teamAssignments()[participant] === index),
    })),
  );

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.loading.set(false);
      this.error.set('Kein Spiel ausgewaehlt.');
      return;
    }

    try {
      this.game.set(await this.gameWebSocketService.getGame(id));
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : String(error));
    } finally {
      this.loading.set(false);
    }
  }

  get answerCount(): number {
    return this.game()?.fragen.reduce((sum, question) => sum + question.antwortmoeglichkeiten.length, 0) ?? 0;
  }

  updateTeamCount(value: string): void {
    const nextTeamCount = Number(value);
    const clampedTeamCount = Math.min(6, Math.max(1, nextTeamCount));
    this.teamCount.set(clampedTeamCount);
  }

  setAssignmentMode(mode: 'self' | 'random'): void {
    this.assignmentMode = mode;
  }

  createLobby(): void {
    this.assignPlaceholderPlayers();
    this.setupComplete.set(true);
  }

  assignPlaceholderPlayers(): void {
    const shuffledParticipants = [...this.participants()].sort(() => Math.random() - 0.5);
    const nextAssignments: Record<string, number> = {};
    shuffledParticipants.forEach((participant, index) => {
      nextAssignments[participant] = index % this.teamCount();
    });
    this.teamAssignments.set(nextAssignments);
  }

  openStartDialog(): void {
    this.showStartDialog = true;
  }

  closeStartDialog(): void {
    this.showStartDialog = false;
  }
}
