import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { Game, GameStatus } from './model/game.model';

export interface CreateGameRequest extends Partial<Game> {
  copyFromGameId?: string;
  sourceQuestionIds?: number[];
}

@Injectable({ providedIn: 'root' })
export class GameRestService {
  private readonly authService = inject(AuthService);

  getPublicGames(): Promise<Game[]> {
    return this.request<Game[]>('/api/games');
  }

  getMyGames(): Promise<Game[]> {
    return this.request<Game[]>('/api/games/mine');
  }

  getGame(id: string): Promise<Game> {
    return this.request<Game>(`/api/games/${encodeURIComponent(id)}`);
  }

  createGame(game: CreateGameRequest): Promise<Game> {
    return this.request<Game>('/api/games', { method: 'POST', body: JSON.stringify(game) });
  }

  updateGame(id: string, game: CreateGameRequest): Promise<Game> {
    return this.request<Game>(`/api/games/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(game),
    });
  }

  updateStatus(id: string, status: GameStatus): Promise<Game> {
    return this.request<Game>(`/api/games/${encodeURIComponent(id)}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    });
  }

  copyGame(id: string, changes: Partial<CreateGameRequest> = {}): Promise<Game> {
    return this.request<Game>(`/api/games/${encodeURIComponent(id)}/copy`, {
      method: 'POST',
      body: JSON.stringify(changes),
    });
  }

  private async request<T>(url: string, init: RequestInit = {}): Promise<T> {
    const authHeaders = await this.authService.authorizationHeaders();
    const response = await fetch(url, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...authHeaders,
        ...init.headers,
      },
    });

    if (response.status === 401) {
      this.authService.invalidateSession();
    }
    if (!response.ok) {
      const message = await response.text();
      throw new Error(message || `Spiel-API lieferte HTTP ${response.status}`);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return response.json() as Promise<T>;
  }
}
