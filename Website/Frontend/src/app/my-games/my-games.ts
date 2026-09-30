import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { GameRestService } from '../game-rest.service';
import { Game, GameStatus } from '../model/game.model';

@Component({
  selector: 'app-my-games',
  imports: [DatePipe, RouterLink],
  templateUrl: './my-games.html',
  styleUrl: './my-games.css',
})
export class MyGames implements OnInit {
  private readonly gameRestService = inject(GameRestService);

  readonly games = signal<Game[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly drafts = computed(() => this.games().filter((game) => game.status === 'DRAFT'));
  readonly privateGames = computed(() => this.games().filter((game) => game.status === 'PRIVATE'));
  readonly publicGames = computed(() => this.games().filter((game) => game.status === 'PUBLIC' || !game.status));

  ngOnInit(): void { void this.loadGames(); }

  async loadGames(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      this.games.set(await this.gameRestService.getMyGames());
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Meine Spiele konnten nicht geladen werden.');
    } finally {
      this.loading.set(false);
    }
  }

  async changeStatus(game: Game, status: GameStatus): Promise<void> {
    try {
      const updated = await this.gameRestService.updateStatus(game.id, status);
      this.games.update((games) => games.map((entry) => entry.id === updated.id ? updated : entry));
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Der Spielstatus konnte nicht geändert werden.');
    }
  }
}
