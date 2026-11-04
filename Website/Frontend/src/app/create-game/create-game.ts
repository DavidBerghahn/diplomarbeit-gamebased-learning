import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { GameRestService } from '../game-rest.service';
import { ApiService } from '../api.service';
import { Game, Question, Smart10Mode } from '../model/game.model';
import { parseGameCsv, csvTemplate } from '../game-csv';

@Component({
  selector: 'app-create-game',
  imports: [FormsModule, RouterLink],
  templateUrl: './create-game.html',
  styleUrl: './create-game.css',
})
export class CreateGame implements OnInit {
  private readonly games = inject(GameRestService);
  private readonly api = inject(ApiService);
  selectedGameType: 'smart10' | 'just-one' = 'smart10';
  smart10Mode: Smart10Mode = 'TRUE_FALSE';
  creationMode: 'new-questions' | 'reuse-game' | 'reuse-questions' = 'new-questions';
  isPublic = true;
  gameName = '';
  description = '';
  subject = '';
  tags = '';
  questions: Question[] = [];
  catalog: Game[] = [];
  selectedGameId = '';
  submitted = false;
  saveError = '';
  busy = false;
  saved: Game | null = null;
  csvMessage = '';
  library: Question[] = [];
  selectedQuestions = new Set<number>();
  async ngOnInit(): Promise<void> {
    this.newQuestion();
    try {
      this.catalog = await this.games.getPublicGames();
    } catch {
      this.saveError =
        'Spielekatalog konnte nicht geladen werden. Neue Spiele können weiterhin erstellt werden.';
    }
  }
  get justOne(): boolean {
    return this.selectedGameType === 'just-one';
  }
  selectGameType(type: 'smart10' | 'just-one'): void {
    this.selectedGameType = type;
    this.questions = [];
    this.newQuestion();
    this.library = [];
    this.selectedQuestions.clear();
  }
  newQuestion(): void {
    this.questions.push({
      typ: this.justOne ? 'JUST_ONE' : this.smart10Mode,
      modus: this.justOne ? undefined : this.smart10Mode,
      frage: '',
      antwortmoeglichkeiten: this.justOne
        ? Array.from({ length: 5 }, () => ({ text: '', ist_richtig: false }))
        : [],
      karten: this.justOne
        ? undefined
        : Array.from({ length: 12 }, (_, i) => ({
            position: i + 1,
            text: '',
            loesung:
              this.smart10Mode === 'TRUE_FALSE'
                ? true
                : this.smart10Mode === 'ORDERING'
                  ? i + 1
                  : '',
          })),
    });
  }
  changeMode(): void {
    this.questions = [];
    this.newQuestion();
  }
  async loadLibrary(): Promise<void> {
    try {
      const game = await this.api.request<Game>(
        '/api/games/' + encodeURIComponent(this.selectedGameId) + '/editor',
      );
      if (game.spiel_typ !== (this.justOne ? 'JustOne' : 'Smart10'))
        throw new Error('Bitte passenden Spieltyp auswählen.');
      this.library = game.fragen;
      this.selectedQuestions.clear();
      this.saveError = '';
    } catch (error) {
      this.saveError = error instanceof Error ? error.message : 'Laden fehlgeschlagen';
    }
  }
  toggleQuestion(index: number): void {
    this.selectedQuestions.has(index)
      ? this.selectedQuestions.delete(index)
      : this.selectedQuestions.add(index);
  }
  async readCsv(event: Event): Promise<void> {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    try {
      if (file.size > 2_000_000) throw new Error('CSV-Datei ist größer als 2 MB.');
      const parsed = parseGameCsv(await file.text(), this.justOne);
      this.questions = parsed;
      this.creationMode = 'new-questions';
      this.csvMessage =
        parsed.length +
        ' Fragen/Karten importiert. Bitte Vorschau prüfen und anschließend speichern.';
      this.saveError = '';
    } catch (error) {
      this.csvMessage = '';
      this.saveError = error instanceof Error ? error.message : 'CSV ungültig';
    }
  }
  downloadTemplate(): void {
    const url = URL.createObjectURL(
      new Blob([csvTemplate(this.justOne ? 'JUST_ONE' : this.smart10Mode)], {
        type: 'text/csv;charset=utf-8',
      }),
    );
    const link = document.createElement('a');
    link.href = url;
    link.download = 'spiel-vorlage.csv';
    link.click();
    URL.revokeObjectURL(url);
  }
  async submit(): Promise<void> {
    if (this.busy || this.submitted) return;
    this.busy = true;
    this.saveError = '';
    this.submitted = false;
    try {
      if (!this.gameName.trim()) throw new Error('Bitte einen Spieltitel eingeben.');
      if (this.creationMode === 'reuse-game') {
        if (!this.selectedGameId) throw new Error('Bitte ein Spiel auswählen.');
        const copied = await this.games.copyGame(this.selectedGameId);
        this.saved = await this.games.updateGame(copied.id, {
          ...(await this.api.request<Game>('/api/games/' + copied.id + '/editor')),
          name: this.gameName,
          beschreibung: this.description,
          status: this.isPublic ? 'PUBLIC' : 'PRIVATE',
        });
      } else {
        const questions =
          this.creationMode === 'reuse-questions'
            ? [...this.selectedQuestions].map((i) => this.library[i])
            : this.questions;
        if (!questions.length) throw new Error('Bitte mindestens eine Frage/Karte auswählen.');
        this.saved = await this.games.createGame({
          id: crypto.randomUUID(),
          spiel_typ: this.justOne ? 'JustOne' : 'Smart10',
          name: this.gameName,
          beschreibung: this.description,
          fach: this.subject,
          zweige: [],
          status: this.isPublic ? 'PUBLIC' : 'PRIVATE',
          tags: this.tags
            .split(',')
            .map((tag) => tag.trim())
            .filter(Boolean),
          fragen: questions.map((q) => ({
            ...q,
            id: undefined,
            karten: q.karten?.map((c) => ({ ...c, id: undefined })),
          })),
        });
      }
      this.submitted = true;
    } catch (error) {
      this.saveError = error instanceof Error ? error.message : 'Speichern fehlgeschlagen';
    } finally {
      this.busy = false;
    }
  }
}
