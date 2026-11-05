import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
interface Hosted {
  id: string;
  title: string;
  finishedAt: string;
  attempts: { id: string; team: string; awarded: boolean; invalidated: boolean }[];
}
@Component({
  selector: 'app-hosted',
  imports: [RouterLink],
  styleUrl: './statistics.css',
  template: ` <main class="statistics-page">
    <nav><a routerLink="/home">← Startseite</a><a routerLink="/games">Alle Spiele</a></nav>
    <h1>Just-One-Spielverlauf</h1>
    <p>
      Ungültige Versuche können nachträglich gelöscht werden. Der dafür erhaltene Punkt wird dem
      Team und seinen Mitgliedern abgezogen.
    </p>
    @if (error()) {
      <p role="alert">{{ error() }}</p>
    }
    @for (session of sessions(); track session.id) {
      <section class="details">
        <h2>{{ session.title }}</h2>
        <p>{{ session.finishedAt }}</p>
        @for (attempt of session.attempts; track attempt.id; let index = $index) {
          <p>
            Versuch {{ index + 1 }} · {{ attempt.team }} ·
            {{ attempt.invalidated ? 'gelöscht' : attempt.awarded ? '+1 Punkt' : 'kein Punkt' }}
            @if (!attempt.invalidated) {
              <button
                type="button"
                [disabled]="busy()"
                (click)="invalidate(session.id, attempt.id)"
              >
                Versuch löschen
              </button>
            }
          </p>
        }
      </section>
    } @empty {
      <p>Noch keine gespeicherten Just-One-Spiele gehostet.</p>
    }
  </main>`,
})
export class HostedSessions implements OnInit {
  private readonly api = inject(ApiService);
  readonly sessions = signal<Hosted[]>([]);
  readonly error = signal('');
  readonly busy = signal(false);
  ngOnInit(): void {
    void this.load();
  }
  async load(): Promise<void> {
    try {
      this.sessions.set(await this.api.request<Hosted[]>('/api/statistics/hosted'));
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Laden fehlgeschlagen');
    }
  }
  async invalidate(sessionId: string, attemptId: string): Promise<void> {
    this.busy.set(true);
    this.error.set('');
    try {
      await this.api.request(
        '/api/statistics/sessions/' + sessionId + '/attempts/' + attemptId + '/invalidate',
        {},
      );
      await this.load();
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Korrektur fehlgeschlagen');
    } finally {
      this.busy.set(false);
    }
  }
}
