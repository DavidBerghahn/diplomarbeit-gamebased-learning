import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { GameWebSocketService } from '../game-websocket.service';
import { Game } from '../model/game.model';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-games',
  imports: [DatePipe, FormsModule],
  templateUrl: './games.html',
  styleUrl: './games.css',
})
export class Games implements OnInit {
  private readonly gameWebSocketService = inject(GameWebSocketService);
  private readonly router = inject(Router);

  games = signal<Game[]>([]);
  quizbattleGames = computed(() =>
    this.games().filter((game) => game.spiel_typ === 'Quizbattle'),
  );
  duellUmDieWeltGames = computed(() =>
    this.games().filter((game) => game.spiel_typ === 'DuellUmDieWelt'),
  );

  selectedGameType:String = ""
  selectedGameForHosting: Game | null = null;

  setSelectedGameType(gameType:String){
    this.selectedGameType = gameType;
  }

  openHostDialog(event: Event, game: Game): void {
    event.stopPropagation();
    this.selectedGameForHosting = game;
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

  ngOnInit(): void {
    void this.loadGames();
  }

  async loadGames(): Promise<void> {
    this.games.set(await this.gameWebSocketService.getGames());
  }
}
