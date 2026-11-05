import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { AuthService } from '../auth.service';

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
interface ClassSummary {
  name: string;
  students: Student[];
  points: number;
  games: number;
}
@Component({
  selector: 'app-statistics',
  imports: [RouterLink, FormsModule, DatePipe, DecimalPipe],
  styleUrl: './statistics.css',
  templateUrl: './statistics.html',
})
export class Statistics implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  readonly canAssignClasses = signal(false);
  readonly unassignedClass = 'Ohne Klasse';
  readonly students = signal<Student[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly schoolClass = signal('');
  readonly studentId = signal('');
  readonly since = signal('');
  readonly assigning = signal('');
  readonly assignmentMessage = signal('');
  readonly assignmentError = signal('');
  classInputs: Record<string, string> = {};
  readonly classes = computed(() =>
    [...new Set(this.students().map((s) => s.schoolClass))].sort((a, b) =>
      a.localeCompare(b, 'de', { numeric: true }),
    ),
  );
  readonly assignableClasses = computed(() =>
    this.classes().filter((c) => c !== this.unassignedClass),
  );
  readonly filtered = computed(() =>
    this.students().filter((s) => !this.schoolClass() || s.schoolClass === this.schoolClass()),
  );
  readonly selected = computed(() => this.filtered().find((s) => s.id === this.studentId()));
  readonly classSummaries = computed<ClassSummary[]>(() => {
    const groups = new Map<string, Student[]>();
    for (const student of this.filtered()) {
      const members = groups.get(student.schoolClass) ?? [];
      members.push(student);
      groups.set(student.schoolClass, members);
    }
    return [...groups.entries()]
      .sort(([a], [b]) => a.localeCompare(b, 'de', { numeric: true }))
      .map(([name, students]) => ({
        name,
        students: students.sort(
          (a, b) => this.points(b) - this.points(a) || a.name.localeCompare(b.name, 'de'),
        ),
        points: students.reduce((sum, s) => sum + this.points(s), 0),
        games: new Set(students.flatMap((s) => this.plays(s).map((p) => p.sessionId))).size,
      }));
  });
  readonly totalPoints = computed(() =>
    this.classSummaries().reduce((sum, c) => sum + c.points, 0),
  );
  readonly unassignedCount = computed(
    () => this.students().filter((s) => s.schoolClass === this.unassignedClass).length,
  );
  ngOnInit(): void {
    void this.load();
    void this.auth.loadProfile().then((profile) => {
      this.canAssignClasses.set(!!profile?.active && (profile.role === 'TEACHER' || profile.role === 'ADMIN'));
    }).catch(() => this.canAssignClasses.set(false));
  }
  selectClass(name: string): void {
    this.schoolClass.set(name);
    this.studentId.set('');
  }
  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      this.students.set(await this.api.request<Student[]>('/api/statistics'));
      if (this.schoolClass() && !this.classes().includes(this.schoolClass())) this.selectClass('');
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : 'Statistiken konnten nicht geladen werden.',
      );
    } finally {
      this.loading.set(false);
    }
  }
  async assignClass(student: Student): Promise<void> {
    if (!this.canAssignClasses()) return;
    if (this.assigning()) return;
    this.assignmentError.set('');
    this.assignmentMessage.set('');
    const schoolClass = (this.classInputs[student.id] ?? '').trim();
    if (!schoolClass || schoolClass.length > 40 || schoolClass.toUpperCase() === 'OHNE KLASSE') {
      this.assignmentError.set('Bitte eine Klasse mit höchstens 40 Zeichen angeben.');
      return;
    }
    this.assigning.set(student.id);
    try {
      const updated = await this.api.request<{ schoolClass: string }>(
        `/api/users/${encodeURIComponent(student.id)}/school-class`,
        { schoolClass },
        'PUT',
      );
      this.students.update((students) =>
        students.map((s) => (s.id === student.id ? { ...s, schoolClass: updated.schoolClass } : s)),
      );
      this.classInputs[student.id] = '';
      if (this.schoolClass() === this.unassignedClass && !this.unassignedCount())
        this.selectClass(updated.schoolClass);
      this.assignmentMessage.set(
        `${student.name} wurde der Klasse ${updated.schoolClass} zugeordnet.`,
      );
    } catch (error) {
      this.assignmentError.set(
        error instanceof Error ? error.message : 'Zuordnung fehlgeschlagen.',
      );
    } finally {
      this.assigning.set('');
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
