import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';

interface QuizTeamMember {
  name: string;
  leader?: boolean;
}

interface QuizTeam {
  name: string;
  color: string;
  points: number;
  eliminated: boolean;
  members: QuizTeamMember[];
}

interface QuizField {
  id: number;
  label: string;
  isTrue: boolean;
  points: number;
  state: 'open' | 'answered' | 'active';
  answeredCorrectly: boolean | null;
  column: number;
  row: number;
}

@Component({
  selector: 'app-quizbattle',
  imports: [],
  templateUrl: './quizbattle.html',
  styleUrl: './quizbattle.css',
})
export class Quizbattle implements OnDestroy, OnInit {
  readonly fields = signal<QuizField[]>([
    { id: 1, label: 'Merkur', isTrue: true, points: 1, state: 'open', answeredCorrectly: null, column: 1, row: 1 },
    { id: 2, label: 'Venus', isTrue: true, points: 1, state: 'open', answeredCorrectly: null, column: 2, row: 1 },
    { id: 3, label: 'Erde', isTrue: true, points: 1, state: 'open', answeredCorrectly: null, column: 3, row: 1 },
    { id: 4, label: 'Mars', isTrue: true, points: 1, state: 'open', answeredCorrectly: null, column: 4, row: 1 },
    { id: 5, label: 'Jupiter', isTrue: true, points: 1, state: 'open', answeredCorrectly: null, column: 1, row: 2 },
    { id: 6, label: 'Saturn', isTrue: true, points: 1, state: 'open', answeredCorrectly: null, column: 4, row: 2 },
    { id: 7, label: 'Uranus', isTrue: true, points: 1, state: 'open', answeredCorrectly: null, column: 1, row: 3 },
    { id: 8, label: 'Neptun', isTrue: true, points: 1, state: 'open', answeredCorrectly: null, column: 4, row: 3 },
    { id: 9, label: 'Pluto', isTrue: false, points: 1, state: 'open', answeredCorrectly: null, column: 1, row: 4 },
    { id: 10, label: 'Sonne', isTrue: false, points: 1, state: 'open', answeredCorrectly: null, column: 2, row: 4 },
    { id: 11, label: 'Mond', isTrue: false, points: 1, state: 'open', answeredCorrectly: null, column: 3, row: 4 },
    { id: 12, label: 'Europa', isTrue: false, points: 1, state: 'open', answeredCorrectly: null, column: 4, row: 4 },
  ]);

  readonly teams = signal<QuizTeam[]>([
    {
      name: 'Team Blau',
      color: '#09509d',
      points: 0,
      eliminated: false,
      members: [
        { name: 'Mia', leader: true },
        { name: 'Jonas' },
        { name: 'Lea' },
      ],
    },
    {
      name: 'Team Orange',
      color: '#f18806',
      points: 0,
      eliminated: false,
      members: [
        { name: 'Noah', leader: true },
        { name: 'Emma' },
      ],
    },
    {
      name: 'Team Grün',
      color: '#137a3f',
      points: 0,
      eliminated: false,
      members: [
        { name: 'Luca', leader: true },
        { name: 'Sofia' },
        { name: 'Ben' },
      ],
    },
  ]);

  readonly currentTeamIndex = signal(0);
  readonly selectedField = signal<QuizField | null>(null);
  readonly answerTimeLimitSeconds = 20;
  readonly answerSecondsRemaining = signal(this.answerTimeLimitSeconds);
  readonly currentRound = signal(1);
  readonly sampleQuestion = 'Dieser Himmelskörper gehört zu den acht Planeten unseres Sonnensystems.';
  readonly remainingQuestions = computed(() => this.fields().filter((field) => field.state !== 'answered').length);
  readonly currentTeam = computed(() => this.teams()[this.currentTeamIndex()]);
  readonly activeTeams = computed(() => this.teams().filter((team) => !team.eliminated).length);
  readonly gameOver = computed(() => this.activeTeams() === 0);
  readonly selectedPrompt = computed(() => {
    const field = this.selectedField();
    return field ? `${field.label}: wahr oder falsch?` : 'Wählt zuerst ein Antwortfeld.';
  });

  private answerTimer: ReturnType<typeof setInterval> | null = null;
  private answerTimerGeneration = 0;

  selectField(fieldId: number): void {
    if (this.gameOver()) {
      return;
    }

    const selectedField = this.fields().find((field) => field.id === fieldId);
    if (!selectedField || selectedField.state === 'answered') {
      return;
    }

    this.fields.update((fields) =>
      fields.map((field) => ({
        ...field,
        state: field.id === fieldId ? 'active' : field.state === 'active' ? 'open' : field.state,
      })),
    );
    this.selectedField.set({ ...selectedField, state: 'active' });
  }

  answerSelectedField(answer: boolean): void {
    if (this.gameOver()) {
      return;
    }

    const selectedField = this.selectedField();
    if (!selectedField) {
      return;
    }

    if (selectedField.isTrue === answer) {
      this.teams.update((teams) =>
        teams.map((team, index) => ({
          ...team,
          points: index === this.currentTeamIndex() ? team.points + selectedField.points : team.points,
        })),
      );
      this.fields.update((fields) =>
        fields.map((field) => ({
          ...field,
          state: field.id === selectedField.id ? 'answered' : field.state === 'active' ? 'open' : field.state,
          answeredCorrectly: field.id === selectedField.id ? selectedField.isTrue === answer : field.answeredCorrectly,
        })),
      );
      this.selectedField.set(null);
      this.moveToNextTeam();
      return;
    }

    const currentIndex = this.currentTeamIndex();
    this.teams.update((teams) =>
      teams.map((team, index) => ({
        ...team,
        points: index === currentIndex ? 0 : team.points,
        eliminated: index === currentIndex ? true : team.eliminated,
      })),
    );
    this.fields.update((fields) =>
      fields.map((field) => ({
        ...field,
        state: field.id === selectedField.id ? 'answered' : field.state === 'active' ? 'open' : field.state,
        answeredCorrectly: field.id === selectedField.id ? selectedField.isTrue === answer : field.answeredCorrectly,
      })),
    );
    this.selectedField.set(null);
    this.moveToNextTeam(currentIndex);
  }

  cancelAnswer(): void {
    if (this.gameOver()) {
      return;
    }

    this.fields.update((fields) =>
      fields.map((field) => ({
        ...field,
        state: field.state === 'active' ? 'open' : field.state,
      })),
    );
    this.selectedField.set(null);
  }

  passTurn(): void {
    if (this.gameOver()) {
      return;
    }

    this.cancelAnswer();
    const currentIndex = this.currentTeamIndex();
    this.teams.update((teams) =>
      teams.map((team, index) => ({
        ...team,
        eliminated: index === currentIndex ? true : team.eliminated,
      })),
    );

    this.moveToNextTeam(currentIndex);
  }

  private moveToNextTeam(previousIndex = this.currentTeamIndex()): void {
    const teams = this.teams();
    const nextTeamIndex = teams.findIndex((team, index) => index > previousIndex && !team.eliminated);
    if (nextTeamIndex !== -1) {
      this.currentTeamIndex.set(nextTeamIndex);
      this.startAnswerTimer();
      return;
    }

    const wrappedTeamIndex = teams.findIndex((team) => !team.eliminated);
    if (wrappedTeamIndex !== -1) {
      this.currentTeamIndex.set(wrappedTeamIndex);
      this.startAnswerTimer();
      return;
    }

    this.stopAnswerTimer(0);
  }

  ngOnInit(): void {
    this.startAnswerTimer();
  }

  ngOnDestroy(): void {
    this.stopAnswerTimer(0);
  }

  private startAnswerTimer(): void {
    this.stopAnswerTimer(this.answerTimeLimitSeconds);
    if (this.activeTeams() === 0) {
      this.answerSecondsRemaining.set(0);
      return;
    }

    const generation = this.answerTimerGeneration;
    this.answerTimer = setInterval(() => {
      if (generation !== this.answerTimerGeneration) {
        return;
      }
      if (this.activeTeams() === 0) {
        this.stopAnswerTimer(0);
        return;
      }

      const secondsRemaining = this.answerSecondsRemaining() - 1;
      this.answerSecondsRemaining.set(Math.max(secondsRemaining, 0));
      if (secondsRemaining <= 0) {
        this.clearAnswerTimerInterval();
        this.passTurn();
      }
    }, 1000);
  }

  private stopAnswerTimer(secondsRemaining = this.answerTimeLimitSeconds): void {
    this.answerTimerGeneration++;
    this.clearAnswerTimerInterval();
    this.answerSecondsRemaining.set(secondsRemaining);
  }

  private clearAnswerTimerInterval(): void {
    if (this.answerTimer !== null) {
      clearInterval(this.answerTimer);
      this.answerTimer = null;
    }
  }
}
