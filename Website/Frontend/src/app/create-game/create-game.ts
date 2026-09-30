import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

type CreationMode = 'reuse-game' | 'reuse-questions' | 'new-questions';
type GameType = 'smart10' | 'just-one';
type Smart10Mode = 'true-false' | 'free-text' | 'ordering';

interface AnswerCardDraft {
  id: number;
  text: string;
  answer: string;
}

interface QuestionDraft {
  id: number;
  prompt: string;
  tags: string;
  answerCards: AnswerCardDraft[];
}

@Component({
  selector: 'app-create-game',
  imports: [FormsModule, RouterLink],
  templateUrl: './create-game.html',
  styleUrl: './create-game.css',
})
export class CreateGame {
  readonly creationModes = [
    { id: 'reuse-game' as const, title: 'Spiel wiederverwenden', description: 'Wähle ein bestehendes Spiel und sein Fragenset als Ausgangspunkt.', icon: '↻' },
    { id: 'reuse-questions' as const, title: 'Spiel mit Fragen erstellen', description: 'Kombiniere eigene oder freigegebene Fragen zu einem neuen Spiel.', icon: '＋' },
    { id: 'new-questions' as const, title: 'Neue Fragen erstellen', description: 'Baue dein Spiel direkt mit neuen Fragen und Antworten auf.', icon: '✦' },
  ];

  readonly gameTypes = [
    { id: 'smart10' as const, title: 'Smart10 / Quizbattle', description: 'Eine zentrale Frage mit 12 Antwortkarten.', icon: '▦', available: true },
    { id: 'just-one' as const, title: 'Just One', description: 'Gemeinsam Hinweise sammeln und erraten.', icon: '✎', available: false },
  ];

  readonly smart10Modes = [
    { id: 'true-false' as const, title: 'Wahr oder falsch', description: 'Jede Karte enthält eine Aussage und eine Wahr/Falsch-Lösung.', icon: '✓' },
    { id: 'free-text' as const, title: 'Konkrete Antwort', description: 'Für jede Karte wird eine konkrete Antwort eingegeben.', icon: 'Aa' },
    { id: 'ordering' as const, title: 'Richtige Reihenfolge', description: 'Jede Karte erhält eine Position von 1 bis 12.', icon: '↕' },
  ];

  readonly existingGames = [
    { id: 'smart10-space', name: 'Weltraum-Wissen', type: 'Smart10 / Quizbattle', questions: 10, author: 'M. Huber' },
    { id: 'smart10-sew', name: 'SEW Grundlagen', type: 'Smart10 / Quizbattle', questions: 10, author: 'A. Leitner' },
    { id: 'smart10-history', name: 'Europa im Wandel', type: 'Smart10 / Quizbattle', questions: 12, author: 'Dein Team' },
  ];

  readonly questionLibrary = [
    { id: 1, prompt: 'Welche Sprache wird im Browser ausgeführt?', author: 'M. Huber', tags: ['SEW', 'Web'] },
    { id: 2, prompt: 'Ordne die Schritte eines Git-Workflows.', author: 'A. Leitner', tags: ['SEW'] },
    { id: 3, prompt: 'HTML beschreibt die Struktur einer Webseite.', author: 'Dein Team', tags: ['SEW', 'Web'] },
    { id: 4, prompt: 'Was bedeutet die Abkürzung API?', author: 'L. Berger', tags: ['Informatik'] },
  ];

  creationMode: CreationMode = 'new-questions';
  selectedGameType: GameType = 'smart10';
  smart10Mode: Smart10Mode = 'true-false';
  isPublic = true;
  gameName = '';
  description = '';
  subject = '';
  tags = '';
  selectedGameId = this.existingGames[0].id;
  selectedQuestionIds = new Set<number>();
  questions: QuestionDraft[] = [this.createQuestion(1)];
  submitted = false;
  saveError = '';
  showTagHelp = false;

  selectMode(mode: CreationMode): void {
    this.creationMode = mode;
    if (mode === 'reuse-game') this.questions = [];
    else if (this.questions.length === 0) this.questions = [this.createQuestion(1)];
  }

  selectGameType(gameType: GameType): void {
    if (gameType === 'just-one') return;
    this.selectedGameType = gameType;
  }

  toggleLibraryQuestion(id: number): void {
    this.selectedQuestionIds.has(id) ? this.selectedQuestionIds.delete(id) : this.selectedQuestionIds.add(id);
  }

  isQuestionSelected(id: number): boolean { return this.selectedQuestionIds.has(id); }

  addQuestion(): void {
    const nextId = this.questions.length ? Math.max(...this.questions.map((question) => question.id)) + 1 : 1;
    this.questions = [...this.questions, this.createQuestion(nextId)];
  }

  removeQuestion(id: number): void {
    if (this.questions.length > 1) this.questions = this.questions.filter((question) => question.id !== id);
  }

  addAnswerCard(question: QuestionDraft): void {
    if (question.answerCards.length >= 12) return;
    const nextId = Math.max(...question.answerCards.map((card) => card.id)) + 1;
    question.answerCards.push({ id: nextId, text: '', answer: '' });
  }

  removeAnswerCard(question: QuestionDraft, cardId: number): void {
    if (question.answerCards.length <= 12) return;
    question.answerCards = question.answerCards.filter((card) => card.id !== cardId);
  }

  submit(): void {
    if (this.selectedGameType === 'just-one') return;

    this.submitted = false;
    this.saveError = '';
    this.submitted = true;
  }

  get selectedGame() {
    return this.existingGames.find((game) => game.id === this.selectedGameId) ?? this.existingGames[0];
  }

  get selectedQuestionCount(): number { return this.selectedQuestionIds.size; }

  get smart10ModeDetails() {
    return this.smart10Modes.find((mode) => mode.id === this.smart10Mode) ?? this.smart10Modes[0];
  }

  private createQuestion(id: number): QuestionDraft {
    return {
      id,
      prompt: '',
      tags: '',
      answerCards: Array.from({ length: 12 }, (_, index) => ({ id: index + 1, text: '', answer: '' })),
    };
  }
}
