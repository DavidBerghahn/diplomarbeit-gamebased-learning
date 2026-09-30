import { Injectable, signal } from '@angular/core';
import { Game } from './model/game.model';

export interface MultiplayerCard { id: number; text: string; state: 'open' | 'resolved'; solution?: string | boolean | number; }
export interface MultiplayerQuestion { prompt: string; mode: 'TRUE_FALSE' | 'FREE_TEXT' | 'ORDERING'; cards: MultiplayerCard[]; }
export interface MultiplayerPlayer { id: string; name: string; host: boolean; team: number; }
export interface MultiplayerTeam { number: number; members: string[]; leader: string | null; points: number; eliminated: boolean; passed: boolean; }
export interface MultiplayerAnswerFeedback {
  correct: boolean;
  team: number;
  cardId: number;
  solution: string | boolean | number | null;
  answeredBy: string;
}
export interface MultiplayerRoomState {
  selfId?: string;
  code: string;
  gameId: string;
  gameName: string;
  phase: 'LOBBY' | 'PLAYING' | 'FINISHED';
  hostId: string;
  players: MultiplayerPlayer[];
  teams: MultiplayerTeam[];
  currentQuestion: MultiplayerQuestion;
  questionIndex: number;
  questionsTotal: number;
  currentTeam: number;
  selectedCard: number | null;
  turnDeadline: number | null;
  lastAnswer: MultiplayerAnswerFeedback | null;
}

@Injectable({ providedIn: 'root' })
export class MultiplayerWebSocketService {
  readonly room = signal<MultiplayerRoomState | null>(null);
  readonly error = signal('');
  private socket?: WebSocket;
  private connectPromise?: Promise<WebSocket>;
  private readonly pending = new Map<string, { resolve: (value: unknown) => void; reject: (reason?: unknown) => void }>();

  get connectionId(): string | null { return this.room()?.selfId ?? null; }

  async createRoom(game: Game, displayName: string, teamCount: number, assignmentMode: 'self' | 'random'): Promise<MultiplayerRoomState> {
    return this.send<MultiplayerRoomState>({ type: 'mp_create', gameId: game.id, game, displayName, teamCount, assignmentMode });
  }

  async joinRoom(code: string, displayName: string): Promise<MultiplayerRoomState> {
    return this.send<MultiplayerRoomState>({ type: 'mp_join', code, displayName });
  }

  startRoom(code: string): Promise<MultiplayerRoomState> { return this.send({ type: 'mp_start', code }); }
  selectCard(code: string, cardId: number): Promise<MultiplayerRoomState> { return this.send({ type: 'mp_select_card', code, cardId }); }
  answer(code: string, answer: string | boolean | number): Promise<MultiplayerRoomState> { return this.send({ type: 'mp_answer', code, answer }); }
  pass(code: string): Promise<MultiplayerRoomState> { return this.send({ type: 'mp_pass', code }); }

  private async send<T>(payload: Record<string, unknown>): Promise<T> {
    const socket = await this.connect();
    const requestId = crypto.randomUUID();
    return new Promise<T>((resolve, reject) => {
      this.pending.set(requestId, { resolve: (value) => resolve(value as T), reject });
      socket.send(JSON.stringify({ requestId, ...payload }));
    });
  }

  private connect(): Promise<WebSocket> {
    if (this.socket?.readyState === WebSocket.OPEN) return Promise.resolve(this.socket);
    if (this.connectPromise) return this.connectPromise;

    this.connectPromise = new Promise<WebSocket>((resolve, reject) => {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const backendHost = window.location.port === '4200' ? `${window.location.hostname}:8080` : window.location.host;
      const socket = new WebSocket(`${protocol}//${backendHost}/user-socket`);
      socket.onopen = () => { this.socket = socket; this.connectPromise = undefined; resolve(socket); };
      socket.onmessage = (event) => this.handleMessage(event);
      socket.onerror = () => { this.error.set('WebSocket-Verbindung zum Spielserver fehlgeschlagen.'); reject(new Error(this.error())); };
      socket.onclose = () => { this.socket = undefined; this.connectPromise = undefined; this.pending.forEach((request) => request.reject(new Error('Die Spielverbindung wurde geschlossen.'))); this.pending.clear(); };
    });
    return this.connectPromise;
  }

  private handleMessage(event: MessageEvent<string>): void {
    const response = JSON.parse(event.data) as { requestId?: string; type: string; data?: MultiplayerRoomState; message?: string };
    if (response.type === 'room_state' && response.data) {
      const selfId = response.data.selfId ?? this.room()?.selfId;
      this.room.set(selfId ? { ...response.data, selfId } : response.data);
    }
    if (!response.requestId) return;
    const request = this.pending.get(response.requestId);
    if (!request) return;
    this.pending.delete(response.requestId);
    if (response.type === 'error') { this.error.set(response.message ?? 'Unbekannter Multiplayer-Fehler.'); request.reject(new Error(this.error())); }
    else request.resolve(response.data);
  }
}
