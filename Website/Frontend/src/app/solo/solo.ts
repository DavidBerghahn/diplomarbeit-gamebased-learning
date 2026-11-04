import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { ApiService } from '../api.service';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { GameRestService } from '../game-rest.service';
import { AnswerOption, Game, Question, Smart10Mode } from '../model/game.model';
import { DEMO_SMART10_GAMES } from '../model/demo-games';

type CardState = 'open' | 'correct' | 'incorrect';

interface SoloCard {
  id: number | string;
  position?: number;
  text: string;
  solution: string | boolean | number;
  state: CardState;
}

interface SoloQuestion {
  prompt: string;
  mode: Smart10Mode;
  cards: SoloCard[];
}

@Component({
  selector: 'app-solo',
  imports: [FormsModule, RouterLink],
  templateUrl: './solo.html',
  styleUrl: './solo.css',
})
export class Solo implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  readonly roundId = signal<string | null>(null);
  readonly sessionId = signal<string | null>(null);
  readonly error = signal('');
  readonly busy = signal(false);
  private readonly route = inject(ActivatedRoute);
  private readonly gameRestService = inject(GameRestService);

  readonly game = signal<Game>(DEMO_SMART10_GAMES[0]);
  readonly questions = signal<SoloQuestion[]>([]);
  readonly loading = signal(true);
  readonly loadMessage = signal('');
  readonly currentQuestionIndex = signal(0);
  readonly selectedCardId = signal<number | string | null>(null);
  readonly answerDraft = signal('');
  readonly feedback = signal('');
  readonly feedbackCorrect = signal<boolean | null>(null);
  readonly correctCards = signal(0);
  readonly answeredCards = signal(0);
  readonly completedQuestions = signal(0);
  readonly currentStreak = signal(0);
  readonly bestStreak = signal(0);
  readonly finished = signal(false);

  readonly currentQuestion = computed(() => this.questions()[this.currentQuestionIndex()] ?? null);
  readonly selectedCard = computed(
    () => this.currentQuestion()?.cards.find((card) => card.id === this.selectedCardId()) ?? null,
  );
  readonly questionComplete = computed(() => {
    const question = this.currentQuestion();
    return (
      !!question &&
      question.cards.length > 0 &&
      question.cards.every((card) => card.state !== 'open')
    );
  });
  readonly score = computed(
    () => this.correctCards() + this.completedQuestions() * 10 + this.bestStreak() * 2,
  );
  readonly progress = computed(() => {
    const questions = this.questions();
    const total = questions.reduce((sum, question) => sum + question.cards.length, 0);
    return total === 0 ? 0 : Math.round((this.answeredCards() / total) * 100);
  });

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    const localGame = DEMO_SMART10_GAMES.find((game) => game.id === id);
    this.loading.set(true);
    this.error.set('');
    try {
      if (localGame) {
        this.game.set(localGame);
        this.loadMessage.set('Demo: Dieses Spiel wird nicht gespeichert.');
        this.questions.set(this.toSoloQuestions(localGame.fragen));
      } else {
        if (!id) throw new Error('Kein Spiel ausgewählt.');
        const start = await this.api.request<{
          roundId: string;
          title: string;
          questions: {
            prompt: string;
            mode: Smart10Mode;
            cards: { id: string; position: number; text: string }[];
          }[];
        }>('/api/smart10/solo/start', { gameId: id });
        this.roundId.set(start.roundId);
        this.game.set({ ...this.game(), id, name: start.title });
        this.questions.set(
          start.questions.map((q) => ({
            ...q,
            cards: q.cards.map((c) => ({ ...c, solution: '', state: 'open' as CardState })),
          })),
        );
      }
    } catch (error) {
      this.questions.set([]);
      this.error.set(
        error instanceof Error ? error.message : 'Spiel konnte nicht gestartet werden.',
      );
    } finally {
      this.loading.set(false);
    }
  }

  ngOnDestroy(): void {
    if (this.roundId() && !this.finished())
      void this.api
        .request('/api/smart10/solo/' + this.roundId(), undefined, 'DELETE')
        .catch(() => {});
  }

  selectCard(card: SoloCard): void {
    if (this.finished() || this.busy() || card.state !== 'open') return;
    this.selectedCardId.set(card.id);
    this.answerDraft.set('');
    this.feedback.set('');
    this.feedbackCorrect.set(null);
  }

  async answerTrueFalse(answer: boolean): Promise<void> {
    await this.submitAnswer(answer);
  }

  async submitTextAnswer(): Promise<void> {
    await this.submitAnswer(this.answerDraft());
  }

  async submitOrderingAnswer(): Promise<void> {
    const value = Number(this.answerDraft());
    if (Number.isInteger(value)) await this.submitAnswer(value);
  }

  async nextQuestion(): Promise<void> {
    if (!this.questionComplete()) return;
    if (this.currentQuestionIndex() >= this.questions().length - 1) {
      this.finished.set(true);
      if (this.roundId()) await this.finishSaved();
      return;
    }
    this.currentQuestionIndex.update((index) => index + 1);
    this.selectedCardId.set(null);
    this.answerDraft.set('');
    this.feedback.set('');
    this.feedbackCorrect.set(null);
  }

  restart(): void {
    this.roundId.set(null);
    this.sessionId.set(null);
    this.questions.update((questions) =>
      questions.map((question) => ({
        ...question,
        cards: question.cards.map((card) => ({ ...card, state: 'open' as CardState })),
      })),
    );
    this.currentQuestionIndex.set(0);
    this.selectedCardId.set(null);
    this.answerDraft.set('');
    this.feedback.set('');
    this.feedbackCorrect.set(null);
    this.correctCards.set(0);
    this.answeredCards.set(0);
    this.completedQuestions.set(0);
    this.currentStreak.set(0);
    this.bestStreak.set(0);
    this.finished.set(false);
    void this.ngOnInit();
  }

  isTrueFalseMode(): boolean {
    return this.currentQuestion()?.mode === 'TRUE_FALSE';
  }
  isFreeTextMode(): boolean {
    return this.currentQuestion()?.mode === 'FREE_TEXT';
  }
  isOrderingMode(): boolean {
    return this.currentQuestion()?.mode === 'ORDERING';
  }

  revealedSolution(card: SoloCard, mode: Smart10Mode): string {
    return this.formatSolution(card.solution, mode);
  }

  async finishSaved(): Promise<void> {
    this.error.set('');
    this.busy.set(true);
    try {
      const result = await this.api.request<{ sessionId: string }>(
        '/api/smart10/solo/' + this.roundId() + '/finish',
        {},
      );
      this.sessionId.set(result.sessionId);
    } catch (error) {
      this.error.set(
        error instanceof Error
          ? error.message
          : 'Speicherung fehlgeschlagen. Bitte erneut versuchen.',
      );
    } finally {
      this.busy.set(false);
    }
  }

  private async submitAnswer(answer: string | boolean | number): Promise<void> {
    const question = this.currentQuestion();
    const cardId = this.selectedCardId();
    const card = question?.cards.find((entry) => entry.id === cardId);
    if (!question || !card || card.state !== 'open' || this.busy()) return;

    this.busy.set(true);
    let server: {
      correct: boolean;
      solution: string | number | boolean;
      correctCards: number;
      completedQuestions: number;
      bestStreak: number;
    } | null = null;
    try {
      if (this.roundId())
        server = await this.api.request('/api/smart10/solo/' + this.roundId() + '/answers', {
          cardId: card.id,
          answer,
        });
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : 'Antwort konnte nicht gesendet werden.',
      );
      this.busy.set(false);
      return;
    }
    this.error.set('');
    const correct = server ? server.correct : this.isCorrect(answer, card.solution, question.mode);
    if (server) card.solution = server.solution;
    const nextCards = question.cards.map((entry) =>
      entry.id === card.id
        ? { ...entry, state: correct ? ('correct' as CardState) : ('incorrect' as CardState) }
        : entry,
    );
    this.questions.update((questions) =>
      questions.map((entry, index) =>
        index === this.currentQuestionIndex() ? { ...entry, cards: nextCards } : entry,
      ),
    );
    this.answeredCards.update((value) => value + 1);
    this.feedbackCorrect.set(correct);
    this.feedback.set(
      correct
        ? 'Richtig! Die Karte zählt für deinen Score.'
        : `Nicht ganz. Die richtige Lösung ist: ${this.formatSolution(card.solution, question.mode)}.`,
    );
    if (correct) {
      this.correctCards.update((value) => value + 1);
      this.currentStreak.update((value) => value + 1);
      this.bestStreak.update((best) => Math.max(best, this.currentStreak()));
    } else {
      this.currentStreak.set(0);
    }
    if (nextCards.every((entry) => entry.state !== 'open')) {
      this.completedQuestions.update((value) => value + 1);
    }
    if (server) {
      this.correctCards.set(server.correctCards);
      this.completedQuestions.set(server.completedQuestions);
      this.bestStreak.set(server.bestStreak);
    }
    this.busy.set(false);
  }

  private isCorrect(
    answer: string | boolean | number,
    solution: string | boolean | number,
    mode: Smart10Mode,
  ): boolean {
    if (mode === 'TRUE_FALSE')
      return answer === (solution === true || String(solution).toLowerCase() === 'true');
    if (mode === 'ORDERING') return Number(answer) === Number(solution);
    return (
      String(answer).trim().toLocaleLowerCase() === String(solution).trim().toLocaleLowerCase()
    );
  }

  private formatSolution(solution: string | boolean | number, mode: Smart10Mode): string {
    if (mode === 'TRUE_FALSE')
      return solution === true || String(solution).toLowerCase() === 'true' ? 'Wahr' : 'Falsch';
    return String(solution);
  }

  private toSoloQuestions(questions: Question[]): SoloQuestion[] {
    const mapped = questions
      .map((question) => {
        const mode = question.modus ?? 'TRUE_FALSE';
        const cards = question.antwortmoeglichkeiten.map((option: AnswerOption, index) => ({
          id: option.id ?? index + 1,
          text: option.text,
          solution:
            option.loesung ?? (mode === 'TRUE_FALSE' ? Boolean(option.ist_richtig) : option.text),
          state: 'open' as CardState,
        }));
        return { prompt: question.frage, mode, cards };
      })
      .filter((question) => question.cards.length === 12);
    return mapped;
  }
}
