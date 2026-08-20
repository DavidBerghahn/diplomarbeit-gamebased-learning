import { Injectable } from '@angular/core';
import { Game } from './model/game.model';

interface WebSocketResponse<T> {
  requestId?: string;
  type: string;
  data?: T;
  message?: string;
}

@Injectable({
  providedIn: 'root',
})
export class GameWebSocketService {
  private socket?: WebSocket;
  private connectPromise?: Promise<WebSocket>;
  private readonly pendingRequests = new Map<string, {
    resolve: (value: unknown) => void;
    reject: (reason?: unknown) => void;
  }>();

  getGames(): Promise<Game[]> {
    return this.send<Game[]>({ type: 'get_games' });
  }

  getGamesByType(gameType: string): Promise<Game[]> {
    return this.send<Game[]>({ type: 'get_games_by_type', gameType });
  }

  getGame(id: string): Promise<Game> {
    return this.send<Game>({ type: 'get_game', id });
  }

  private async send<T>(payload: Record<string, string>): Promise<T> {
    const socket = await this.connect();
    const requestId = crypto.randomUUID();

    return new Promise<T>((resolve, reject) => {
      this.pendingRequests.set(requestId, {
        resolve: (value) => resolve(value as T),
        reject,
      });

      socket.send(JSON.stringify({ requestId, ...payload }));
    });
  }

  private connect(): Promise<WebSocket> {
    if (this.socket?.readyState === WebSocket.OPEN) {
      return Promise.resolve(this.socket);
    }

    if (this.connectPromise) {
      return this.connectPromise;
    }

    this.connectPromise = new Promise<WebSocket>((resolve, reject) => {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const backendHost = window.location.port === '4200'
        ? `${window.location.hostname}:8080`
        : window.location.host;
      const socket = new WebSocket(`${protocol}//${backendHost}/user-socket`);

      socket.onopen = () => {
        this.socket = socket;
        this.connectPromise = undefined;
        resolve(socket);
      };

      socket.onmessage = (event) => this.handleMessage(event);

      socket.onerror = () => {
        reject(new Error('WebSocket-Verbindung zum Backend fehlgeschlagen.'));
      };

      socket.onclose = () => {
        this.socket = undefined;
        this.connectPromise = undefined;
        this.rejectOpenRequests('WebSocket-Verbindung wurde geschlossen.');
      };
    });

    return this.connectPromise;
  }

  private handleMessage(event: MessageEvent<string>): void {
    const response = JSON.parse(event.data) as WebSocketResponse<unknown>;

    if (!response.requestId) {
      return;
    }

    const pendingRequest = this.pendingRequests.get(response.requestId);
    if (!pendingRequest) {
      return;
    }

    this.pendingRequests.delete(response.requestId);

    if (response.type === 'error') {
      pendingRequest.reject(new Error(response.message ?? 'Unbekannter WebSocket-Fehler.'));
      return;
    }

    pendingRequest.resolve(response.data);
  }

  private rejectOpenRequests(message: string): void {
    this.pendingRequests.forEach((request) => request.reject(new Error(message)));
    this.pendingRequests.clear();
  }
}
