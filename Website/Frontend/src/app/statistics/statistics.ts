import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';

interface Play {
  sessionId: string;
  gameId: string;
  game: string;
  type: string;
  mode: string;
  finishedAt: string;
  score: number;
  players: string[];
}
interface Student {
  id: string;
  name: string;
  schoolClass: string;
  plays: Play[];
}
@Component({
  selector: 'app-statistics',
  imports: [RouterLink, FormsModule],
  styleUrl: './statistics.css',
  template: ` <main class="statistics-page">
    <nav><a routerLink="/home">← Startseite</a><a routerLink="/games">Alle Spiele</a></nav>
    <span class="eyebrow">Lehrerbereich</span>
    <h1>Klassenstatistik</h1>
    <p>Abgeschlossene Solo- und Multiplayer-Spiele aller Klassen. Demos werden nicht gezählt.</p>
    @if (error()) {
      <p role="alert">{{ error() }}</p>
    }
    <div class="filters">
      <label
        >Klasse<select
          [ngModel]="schoolClass()"
          (ngModelChange)="schoolClass.set($event); studentId.set('')"
        >
          <option value="">Alle Klassen</option>
          @for (name of classes(); track name) {
            <option [value]="name">{{ name }}</option>
          }
        </select></label
      >
      <label
        >Ab Datum<input type="date" [ngModel]="since()" (ngModelChange)="since.set($event)"
      /></label>
      <button type="button" (click)="load()">Aktualisieren</button>
    </div>
    @if (loading()) {
      <p>Lädt …</p>
    }
    <section class="students">
      @for (student of filtered(); track student.id) {
        <button
          type="button"
          [class.selected]="studentId() === student.id"
          (click)="studentId.set(student.id)"
        >
          <strong>{{ student.name }}</strong
          ><span
            >{{ student.schoolClass }} · {{ plays(student).length }} Spiele ·
            {{ points(student) }} Punkte</span
          >
        </button>
      } @empty {
        @if (!loading()) {
          <p>Keine Schüler in dieser Auswahl.</p>
        }
      }
    </section>
    @if (selected(); as student) {
      <section class="details">
        <h2>{{ student.name }} · {{ student.schoolClass }}</h2>
        <h3>Welche Spiele wurden wie oft gespielt?</h3>
        @for (entry of frequencies(student); track entry.id) {
          <p>{{ entry.name }} · {{ entry.count }} Mal</p>
        }
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Spiel</th>
                <th>Modus</th>
                <th>Abgeschlossen</th>
                <th>Punkte</th>
                <th>Mitspieler</th>
              </tr>
            </thead>
            <tbody>
              @for (play of plays(student); track play.sessionId) {
                <tr>
                  <td>{{ play.game }}</td>
                  <td>{{ play.mode === 'SINGLEPLAYER' ? 'Solo' : 'Multiplayer' }}</td>
                  <td>{{ play.finishedAt }}</td>
                  <td>{{ play.score }}</td>
                  <td>{{ play.players.join(', ') }}</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="5">Noch keine abgeschlossenen Spiele im Zeitraum.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    }
  </main>`,
})
export class Statistics implements OnInit {
  private readonly api = inject(ApiService);
  readonly students = signal<Student[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly schoolClass = signal('');
  readonly studentId = signal('');
  readonly since = signal('');
  readonly classes = computed(() => [...new Set(this.students().map((s) => s.schoolClass))].sort());
  readonly filtered = computed(() =>
    this.students().filter((s) => !this.schoolClass() || s.schoolClass === this.schoolClass()),
  );
  readonly selected = computed(() => this.filtered().find((s) => s.id === this.studentId()));
  ngOnInit(): void {
    void this.load();
  }
  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      this.students.set(await this.api.request<Student[]>('/api/statistics'));
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : 'Statistiken konnten nicht geladen werden.',
      );
    } finally {
      this.loading.set(false);
    }
  }
  plays(student: Student): Play[] {
    return student.plays.filter((p) => !this.since() || p.finishedAt.slice(0, 10) >= this.since());
  }
  points(student: Student): number {
    return this.plays(student).reduce((sum, p) => sum + p.score, 0);
  }
  frequencies(student: Student): { id: string; name: string; count: number }[] {
    const games = new Map<string, { id: string; name: string; count: number }>();
    for (const play of this.plays(student)) {
      const entry = games.get(play.gameId) ?? { id: play.gameId, name: play.game, count: 0 };
      entry.count++;
      games.set(play.gameId, entry);
    }
    return [...games.values()];
  }
}
