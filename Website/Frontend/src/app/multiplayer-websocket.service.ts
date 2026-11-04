import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { Game } from './model/game.model';

export interface MultiplayerCard {
  id: string;
  text: string;
  state: 'open' | 'resolved';
  solution?: string | boolean | number;
}
export interface MultiplayerQuestion {
  prompt: string;
  mode: 'TRUE_FALSE' | 'FREE_TEXT' | 'ORDERING';
  cards: MultiplayerCard[];
}
export interface MultiplayerPlayer {
  id: string;
  name: string;
  host: boolean;
  team: number;
}
export interface MultiplayerTeam {
  number: number;
  members: string[];
  leader: string | null;
  points: number;
  eliminated: boolean;
  passed: boolean;
}
export interface MultiplayerAnswerFeedback {
  correct: boolean;
  team: number;
  cardId: string;
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
  hostName: string;
  players: MultiplayerPlayer[];
  teams: MultiplayerTeam[];
  currentQuestion: MultiplayerQuestion;
  questionIndex: number;
  questionsTotal: number;
  currentTeam: number;
  selectedCard: string | null;
  turnDeadline: number | null;
  lastAnswer: MultiplayerAnswerFeedback | null;
  gameType: string;
  assignmentMode: 'self' | 'random';
  demo: boolean;
  expiresAt: number;
  turnSeconds: number;
  sessionId: string | null;
  saveError: string | null;
  justOne?: {
    phase: string;
    guesser: string;
    number: number | null;
    word?: string;
    guess: string | null;
    clues: { player: string; text: string; duplicate: boolean }[];
    submitted: string[];
    attempts: { id: string; team: number; awarded: boolean; invalidated: boolean }[];
  };
}

@Injectable({ providedIn: 'root' })
export class MultiplayerWebSocketService {
  private readonly api = inject(ApiService);
  readonly room = signal<MultiplayerRoomState | null>(null);
  readonly error = signal('');
  private socket?: WebSocket;
  private connectPromise?: Promise<WebSocket>;
  private readonly pending = new Map<
    string,
    { resolve: (value: unknown) => void; reject: (reason?: unknown) => void }
  >();

  get connectionId(): string | null {
    return this.room()?.selfId ?? null;
  }

  async createRoom(
    game: Game,
    displayName: string,
    teamCount: number,
    assignmentMode: 'self' | 'random',
    turnSeconds = 20,
    hostPlays = false,
  ): Promise<MultiplayerRoomState> {
    return this.send<MultiplayerRoomState>({
      type: 'mp_create',
      gameId: game.id,
      ...(game.id.startsWith('demo-') ? { game } : {}),
      displayName,
      teamCount,
      assignmentMode,
      turnSeconds,
      hostPlays,
    });
  }

  async joinRoom(code: string, displayName: string): Promise<MultiplayerRoomState> {
    return this.send<MultiplayerRoomState>({ type: 'mp_join', code, displayName });
  }

  startRoom(code: string, allowUneven = false): Promise<MultiplayerRoomState> {
    return this.send({ type: 'mp_start', code, allowUneven });
  }
  selectCard(code: string, cardId: string): Promise<MultiplayerRoomState> {
    return this.send({ type: 'mp_select_card', code, cardId });
  }
  answer(code: string, answer: string | boolean | number): Promise<MultiplayerRoomState> {
    return this.send({ type: 'mp_answer', code, answer });
  }
  pass(code: string): Promise<MultiplayerRoomState> {
    return this.send({ type: 'mp_pass', code });
  }
  chooseTeam(code: string, team: number, playerId?: string): Promise<MultiplayerRoomState> {
    return this.send({ type: 'mp_team', code, team, ...(playerId ? { playerId } : {}) });
  }
  retrySave(code: string): Promise<MultiplayerRoomState> {
    return this.send({ type: 'mp_retry_save', code });
  }
  justOne(
    code: string,
    action: string,
    data: Record<string, unknown> = {},
  ): Promise<MultiplayerRoomState> {
    return this.send({ type: 'mp_just_one', code, action, ...data });
  }
  async leave(): Promise<void> {
    const code = this.room()?.code;
    if (code) await this.send({ type: 'mp_leave', code });
    this.socket?.close();
    this.room.set(null);
  }

  private async send<T>(payload: Record<string, unknown>): Promise<T> {
    const socket = await this.connect();
    return this.sendOn<T>(socket, payload);
  }

  private sendOn<T>(socket: WebSocket, payload: Record<string, unknown>): Promise<T> {
    const requestId = crypto.randomUUID();
    return new Promise<T>((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.pending.delete(requestId);
        reject(new Error('Der Spielserver antwortet nicht. Bitte Verbindung prüfen.'));
      }, 15000);
      this.pending.set(requestId, {
        resolve: (value) => {
          clearTimeout(timeout);
          resolve(value as T);
        },
        reject: (reason) => {
          clearTimeout(timeout);
          reject(reason);
        },
      });
      socket.send(JSON.stringify({ requestId, ...payload }));
    });
  }

  private connect(): Promise<WebSocket> {
    if (this.socket && this.socket.readyState === WebSocket.OPEN)
      return Promise.resolve(this.socket);
    if (this.connectPromise) return this.connectPromise;

    this.connectPromise = this.api
      .request<{ ticket: string }>('/api/smart10/ws-ticket', {})
      .then(
        ({ ticket }) =>
          new Promise<WebSocket>((resolve, reject) => {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const backendHost =
              window.location.port === '4200'
                ? `${window.location.hostname}:8080`
                : window.location.host;
            const socket = new WebSocket(`${protocol}//${backendHost}/user-socket`);
            socket.onopen = () => {
              void this.sendOn(socket, { type: 'bind_ticket', ticket }).then(
                () => {
                  this.socket = socket;
                  this.connectPromise = undefined;
                  this.error.set('');
                  resolve(socket);
                },
                (error) => {
                  socket.close();
                  reject(error);
                },
              );
            };
            socket.onmessage = (event) => this.handleMessage(event);
            socket.onerror = () => {
              this.error.set('WebSocket-Verbindung zum Spielserver fehlgeschlagen.');
              reject(new Error(this.error()));
            };
            socket.onclose = () => {
              this.socket = undefined;
              this.connectPromise = undefined;
              this.room.set(null);
              this.pending.forEach((request) =>
                request.reject(new Error('Die Spielverbindung wurde geschlossen.')),
              );
              this.pending.clear();
              reject(new Error('Die Spielverbindung wurde geschlossen.'));
            };
          }),
      )
      .catch((error) => {
        this.connectPromise = undefined;
        throw error;
      });
    return this.connectPromise;
  }

  private handleMessage(event: MessageEvent<string>): void {
    const response = JSON.parse(event.data) as {
      requestId?: string;
      type: string;
      data?: MultiplayerRoomState;
      message?: string;
    };
    if (response.type === 'room_closed') {
      this.room.set(null);
      this.error.set(response.message ?? 'Raum geschlossen.');
    }
    if (response.type === 'room_state' && response.data) {
      const selfId = response.data.selfId ?? this.room()?.selfId;
      this.room.set(selfId ? { ...response.data, selfId } : response.data);
    }
    if (!response.requestId) return;
    const request = this.pending.get(response.requestId);
    if (!request) return;
    this.pending.delete(response.requestId);
    if (response.type === 'error') {
      this.error.set(response.message ?? 'Unbekannter Multiplayer-Fehler.');
      request.reject(new Error(this.error()));
    } else request.resolve(response.data);
  }
}
