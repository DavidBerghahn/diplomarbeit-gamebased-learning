import { TestBed } from '@angular/core/testing';
import { ApiService } from './api.service';
import { MultiplayerWebSocketService } from './multiplayer-websocket.service';
import { DEMO_SMART10_GAMES } from './model/demo-games';

class FakeSocket {
  static OPEN = 1;
  static last: FakeSocket;
  readyState = 1;
  messages: Record<string, unknown>[] = [];
  onopen?: () => void;
  onmessage?: (event: MessageEvent<string>) => void;
  onerror?: () => void;
  onclose?: () => void;
  constructor(readonly url: string) {
    FakeSocket.last = this;
    queueMicrotask(() => this.onopen?.());
  }
  send(text: string): void {
    const request = JSON.parse(text);
    this.messages.push(request);
    queueMicrotask(() =>
      this.onmessage?.({
        data: JSON.stringify(
          request.type === 'bind_ticket'
            ? { requestId: request.requestId, type: 'ticket_bound', data: { displayName: 'Test' } }
            : {
                requestId: request.requestId,
                type: 'room_state',
                data: { code: '1234', selfId: 'me', players: [], teams: [] },
              },
        ),
      } as MessageEvent<string>),
    );
  }
  close(): void {
    this.readyState = 3;
    this.onclose?.();
  }
}
describe('Authenticated multiplayer transport', () => {
  afterEach(() => vi.unstubAllGlobals());
  it('binds an HTTP ticket before creating a stored room and never sends client solutions', async () => {
    vi.stubGlobal('WebSocket', FakeSocket);
    const api = { request: vi.fn().mockResolvedValue({ ticket: 'opaque-test-ticket' }) };
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
    const service = TestBed.inject(MultiplayerWebSocketService);
    await service.createRoom(
      { ...DEMO_SMART10_GAMES[0], id: 'stored-id' },
      'Test',
      2,
      'random',
      30,
      true,
    );
    expect(api.request).toHaveBeenCalledWith('/api/smart10/ws-ticket', {});
    expect(FakeSocket.last.messages.map((m) => m['type'])).toEqual(['bind_ticket', 'mp_create']);
    expect(FakeSocket.last.messages[1]['game']).toBeUndefined();
    expect(FakeSocket.last.messages[1]['turnSeconds']).toBe(30);
    expect(FakeSocket.last.url).not.toContain('ticket');
    expect(FakeSocket.last.url).not.toContain('Bearer');
  });
  it('does not open a socket when the ticket request fails', async () => {
    const socket = vi.fn();
    vi.stubGlobal('WebSocket', socket);
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ApiService,
          useValue: { request: vi.fn().mockRejectedValue(new Error('Login erforderlich')) },
        },
      ],
    });
    await expect(
      TestBed.inject(MultiplayerWebSocketService).joinRoom('1234', 'Test'),
    ).rejects.toThrow('Login erforderlich');
    expect(socket).not.toHaveBeenCalled();
  });
});
