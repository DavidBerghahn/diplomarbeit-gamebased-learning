import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { GameWebSocketService } from '../game-websocket.service';
import { Game } from '../model/game.model';

@Component({
  selector: 'app-games',
  imports: [DatePipe],
  templateUrl: './games.html',
  styleUrl: './games.css',
})
export class Games implements OnInit {
  private readonly gameWebSocketService = inject(GameWebSocketService);

  games = signal<Game[]>([]);

  ngOnInit(): void {
    void this.loadGames();
  }

  async loadGames(): Promise<void> {
    this.games.set(await this.gameWebSocketService.getGames());
  }
}
