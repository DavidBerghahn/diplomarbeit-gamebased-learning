import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { GameRestService } from '../game-rest.service';
import { AnswerOption, Game, Question, Smart10Mode } from '../model/game.model';
import { DEMO_SMART10_GAMES } from '../model/demo-games';

type CardState = 'open' | 'correct' | 'incorrect';

interface SoloCard {
  id: number;
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
export class Solo implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly gameRestService = inject(GameRestService);

  readonly game = signal<Game>(DEMO_SMART10_GAMES[0]);
  readonly questions = signal<SoloQuestion[]>([]);
  readonly loading = signal(true);
  readonly loadMessage = signal('');
  readonly currentQuestionIndex = signal(0);
  readonly selectedCardId = signal<number | null>(null);
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
  readonly selectedCard = computed(() => this.currentQuestion()?.cards.find((card) => card.id === this.selectedCardId()) ?? null);
  readonly questionComplete = computed(() => {
    const question = this.currentQuestion();
    return !!question && question.cards.length > 0 && question.cards.every((card) => card.state !== 'open');
  });
  readonly score = computed(() => this.correctCards() + this.completedQuestions() * 10 + this.bestStreak() * 2);
  readonly progress = computed(() => {
    const questions = this.questions();
    const total = questions.reduce((sum, question) => sum + question.cards.length, 0);
    return total === 0 ? 0 : Math.round((this.answeredCards() / total) * 100);
  });

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    const localGame = DEMO_SMART10_GAMES.find((game) => game.id === id);
    if (localGame) this.game.set(localGame);
    if (id) {
      try {
        this.game.set(await this.gameRestService.getGame(id));
      } catch {
        this.loadMessage.set('Das Demo-Spiel wird lokal verwendet. Es werden keine Spieldaten gespeichert.');
      }
    }
    this.questions.set(this.toSoloQuestions(this.game().fragen));
    this.loading.set(false);
  }

  selectCard(card: SoloCard): void {
    if (this.finished() || card.state !== 'open') return;
    this.selectedCardId.set(card.id);
    this.answerDraft.set('');
    this.feedback.set('');
    this.feedbackCorrect.set(null);
  }

  answerTrueFalse(answer: boolean): void {
    this.submitAnswer(answer);
  }

  submitTextAnswer(): void {
    this.submitAnswer(this.answerDraft());
  }

  submitOrderingAnswer(): void {
    const value = Number(this.answerDraft());
    if (Number.isInteger(value)) this.submitAnswer(value);
  }

  nextQuestion(): void {
    if (!this.questionComplete()) return;
    if (this.currentQuestionIndex() >= this.questions().length - 1) {
      this.finished.set(true);
      return;
    }
    this.currentQuestionIndex.update((index) => index + 1);
    this.selectedCardId.set(null);
    this.answerDraft.set('');
    this.feedback.set('');
    this.feedbackCorrect.set(null);
  }

  restart(): void {
    this.questions.update((questions) => questions.map((question) => ({
      ...question,
      cards: question.cards.map((card) => ({ ...card, state: 'open' as CardState })),
    })));
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
  }

  isTrueFalseMode(): boolean { return this.currentQuestion()?.mode === 'TRUE_FALSE'; }
  isFreeTextMode(): boolean { return this.currentQuestion()?.mode === 'FREE_TEXT'; }
  isOrderingMode(): boolean { return this.currentQuestion()?.mode === 'ORDERING'; }

  revealedSolution(card: SoloCard, mode: Smart10Mode): string {
    return this.formatSolution(card.solution, mode);
  }

  private submitAnswer(answer: string | boolean | number): void {
    const question = this.currentQuestion();
    const cardId = this.selectedCardId();
    const card = question?.cards.find((entry) => entry.id === cardId);
    if (!question || !card || card.state !== 'open') return;

    const correct = this.isCorrect(answer, card.solution, question.mode);
    const nextCards = question.cards.map((entry) => entry.id === card.id
      ? { ...entry, state: correct ? 'correct' as CardState : 'incorrect' as CardState }
      : entry);
    this.questions.update((questions) => questions.map((entry, index) => index === this.currentQuestionIndex()
      ? { ...entry, cards: nextCards }
      : entry));
    this.answeredCards.update((value) => value + 1);
    this.feedbackCorrect.set(correct);
    this.feedback.set(correct
      ? 'Richtig! Die Karte zählt für deinen Score.'
      : `Nicht ganz. Die richtige Lösung ist: ${this.formatSolution(card.solution, question.mode)}.`);
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
  }

  private isCorrect(answer: string | boolean | number, solution: string | boolean | number, mode: Smart10Mode): boolean {
    if (mode === 'TRUE_FALSE') return answer === (solution === true || String(solution).toLowerCase() === 'true');
    if (mode === 'ORDERING') return Number(answer) === Number(solution);
    return String(answer).trim().toLocaleLowerCase() === String(solution).trim().toLocaleLowerCase();
  }

  private formatSolution(solution: string | boolean | number, mode: Smart10Mode): string {
    if (mode === 'TRUE_FALSE') return solution === true || String(solution).toLowerCase() === 'true' ? 'Wahr' : 'Falsch';
    return String(solution);
  }

  private toSoloQuestions(questions: Question[]): SoloQuestion[] {
    const mapped = questions.map((question) => {
      const mode = question.modus ?? 'TRUE_FALSE';
      const cards = question.antwortmoeglichkeiten.map((option: AnswerOption, index) => ({
        id: option.id ?? index + 1,
        text: option.text,
        solution: option.loesung ?? (mode === 'TRUE_FALSE' ? Boolean(option.ist_richtig) : option.text),
        state: 'open' as CardState,
      }));
      return { prompt: question.frage, mode, cards };
    }).filter((question) => question.cards.length === 12);
    return mapped.length > 0 ? mapped : this.toSoloQuestions(DEMO_SMART10_GAMES[0].fragen);
  }
}
