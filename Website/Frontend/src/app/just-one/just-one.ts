import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface JustOneMember {
  name: string;
  leader: boolean;
  isGuesser: boolean;
}

interface JustOneTeam {
  name: string;
  color: string;
  points: number;
  members: JustOneMember[];
  guesserIndex: number;
}

interface JustOneCard {
  category: string;
  words: string[];
}

interface JustOneClue {
  memberName: string;
  word: string;
  duplicate: boolean;
}

interface JustOneRoundResult {
  teamName: string;
  category: string;
  selectedWord: string;
  clues: JustOneClue[];
  guess: string;
  correct: boolean;
}

type GamePhase =
  | 'word-selection'
  | 'clue-input'
  | 'reveal-clues'
  | 'guess'
  | 'decision'
  | 'round-over';

@Component({
  selector: 'app-just-one',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './just-one.html',
  styleUrl: './just-one.css'
})
export class JustOneComponent {
  gameTitle = 'Just One';

  cards: JustOneCard[] = [
    {
      category: 'Tiere',
      words: ['Elefant', 'Pinguin', 'Giraffe', 'Krokodil', 'Delfin']
    },
    {
      category: 'Technik',
      words: ['Roboter', 'Computer', 'Smartphone', 'Drucker', 'Kamera']
    },
    {
      category: 'Essen',
      words: ['Pizza', 'Apfel', 'Schokolade', 'Hamburger', 'Nudeln']
    }
  ];

  currentRound = signal(1);
  currentCardIndex = signal(0);
  currentTeamIndex = signal(0);

  phase = signal<GamePhase>('word-selection');

  currentCard = computed(() => this.cards[this.currentCardIndex()]);
  currentCategory = computed(() => this.currentCard().category);

  selectedWordIndex = signal<number | null>(null);

  selectedWord = computed(() => {
    const index = this.selectedWordIndex();

    if (index === null) {
      return null;
    }

    return this.currentCard().words[index];
  });

  usedWordIndices = signal<number[]>([]);

  teams = signal<JustOneTeam[]>([
    {
      name: 'Team Blau',
      color: '#004b8a',
      points: 0,
      guesserIndex: 0,
      members: [
        { name: 'Anna', leader: true, isGuesser: false },
        { name: 'Max', leader: false, isGuesser: false },
        { name: 'Lisa', leader: false, isGuesser: false },
        { name: 'Tom', leader: false, isGuesser: false }
      ]
    },
    {
      name: 'Team Grün',
      color: '#137a3f',
      points: 0,
      guesserIndex: 0,
      members: [
        { name: 'Jonas', leader: true, isGuesser: false },
        { name: 'Sophie', leader: false, isGuesser: false },
        { name: 'Paul', leader: false, isGuesser: false },
        { name: 'Emma', leader: false, isGuesser: false }
      ]
    },
    {
      name: 'Team Rot',
      color: '#b42318',
      points: 0,
      guesserIndex: 0,
      members: [
        { name: 'David', leader: true, isGuesser: false },
        { name: 'Lena', leader: false, isGuesser: false },
        { name: 'Felix', leader: false, isGuesser: false },
        { name: 'Mia', leader: false, isGuesser: false }
      ]
    }
  ]);

  currentTeam = computed(() => this.teams()[this.currentTeamIndex()]);

  currentGuesser = computed(() => {
    const team = this.currentTeam();
    return team.members[team.guesserIndex];
  });

  clues = signal<JustOneClue[]>([]);

  currentClue = '';
  guess = '';

  answerCorrect = signal<boolean | null>(null);

  completedTeams = signal<string[]>([]);

  roundResults = signal<JustOneRoundResult[]>([]);

  constructor() {
    this.initializeGuessers();
  }

  private initializeGuessers(): void {
    this.teams.update(teams =>
      teams.map(team => {
        const randomIndex = Math.floor(
          Math.random() * team.members.length
        );

        return {
          ...team,
          guesserIndex: randomIndex,
          members: team.members.map((member, memberIndex) => ({
            ...member,
            isGuesser: memberIndex === randomIndex
          }))
        };
      })
    );
  }

  isWordUsed(index: number): boolean {
    return this.usedWordIndices().includes(index);
  }

  selectWord(index: number): void {
    if (this.phase() !== 'word-selection') {
      return;
    }

    if (this.isWordUsed(index)) {
      return;
    }

    this.selectedWordIndex.set(index);

    this.usedWordIndices.update(indices => [
      ...indices,
      index
    ]);

    this.clues.set([]);
    this.currentClue = '';
    this.guess = '';
    this.answerCorrect.set(null);

    this.phase.set('clue-input');
  }

  hasSubmittedClue(memberName: string): boolean {
    return this.clues().some(
      clue => clue.memberName === memberName
    );
  }

  submitClue(memberName: string): void {
    if (this.phase() !== 'clue-input') {
      return;
    }

    const team = this.currentTeam();

    const member = team.members.find(
      member => member.name === memberName
    );

    if (!member || member.isGuesser) {
      return;
    }

    const clue = this.currentClue.trim();

    if (!clue) {
      return;
    }

    if (this.hasSubmittedClue(memberName)) {
      return;
    }

    this.clues.update(clues => [
      ...clues,
      {
        memberName,
        word: clue,
        duplicate: false
      }
    ]);

    this.currentClue = '';

    this.checkAllCluesSubmitted();
  }

  private checkAllCluesSubmitted(): void {
    const team = this.currentTeam();

    const clueGivers = team.members.filter(
      member => !member.isGuesser
    );

    if (this.clues().length >= clueGivers.length) {
      this.removeDuplicateClues();
      this.phase.set('reveal-clues');
    }
  }

  private removeDuplicateClues(): void {
    const currentClues = this.clues();

    const counts = new Map<string, number>();

    for (const clue of currentClues) {
      const normalized = clue.word.trim().toLowerCase();

      counts.set(
        normalized,
        (counts.get(normalized) ?? 0) + 1
      );
    }

    this.clues.set(
      currentClues.map(clue => {
        const normalized = clue.word.trim().toLowerCase();

        return {
          ...clue,
          duplicate:
            (counts.get(normalized) ?? 0) > 1
        };
      })
    );
  }

  visibleCluesForGuesser(): JustOneClue[] {
    return this.clues().filter(
      clue => !clue.duplicate
    );
  }

  startGuess(): void {
    if (this.phase() !== 'reveal-clues') {
      return;
    }

    this.guess = '';
    this.phase.set('guess');
  }

  submitGuess(): void {
    if (this.phase() !== 'guess') {
      return;
    }

    if (!this.guess.trim()) {
      return;
    }

    this.answerCorrect.set(null);
    this.phase.set('decision');
  }

  markCorrect(): void {
    if (this.phase() !== 'decision') {
      return;
    }

    this.answerCorrect.set(true);

    const teamIndex = this.currentTeamIndex();

    this.teams.update(teams =>
      teams.map((team, index) =>
        index !== teamIndex
          ? team
          : {
              ...team,
              points: team.points + 1
            }
      )
    );

    this.saveRoundResult(true);
    this.finishCurrentTeam();
  }

  markIncorrect(): void {
    if (this.phase() !== 'decision') {
      return;
    }

    this.answerCorrect.set(false);

    this.saveRoundResult(false);
    this.finishCurrentTeam();
  }

  private saveRoundResult(correct: boolean): void {
    const word = this.selectedWord();

    if (!word) {
      return;
    }

    const result: JustOneRoundResult = {
      teamName: this.currentTeam().name,
      category: this.currentCategory(),
      selectedWord: word,
      clues: [...this.clues()],
      guess: this.guess.trim(),
      correct
    };

    this.roundResults.update(results => [
      ...results,
      result
    ]);
  }

  private finishCurrentTeam(): void {
    const teamName = this.currentTeam().name;

    this.completedTeams.update(teams => [
      ...teams,
      teamName
    ]);

    const allTeamsCompleted =
      this.completedTeams().length >= this.teams().length;

    if (allTeamsCompleted) {
      this.phase.set('round-over');
      return;
    }

    this.moveToNextTeam();
  }

  private moveToNextTeam(): void {
    const nextIndex =
      (this.currentTeamIndex() + 1) %
      this.teams().length;

    this.currentTeamIndex.set(nextIndex);

    this.selectedWordIndex.set(null);
    this.clues.set([]);
    this.currentClue = '';
    this.guess = '';
    this.answerCorrect.set(null);

    this.phase.set('word-selection');
  }

  nextRound(): void {
    this.currentRound.update(round => round + 1);

    this.currentCardIndex.update(
      index => (index + 1) % this.cards.length
    );

    this.currentTeamIndex.set(0);
    this.selectedWordIndex.set(null);
    this.usedWordIndices.set([]);
    this.clues.set([]);
    this.currentClue = '';
    this.guess = '';
    this.answerCorrect.set(null);
    this.completedTeams.set([]);
    this.roundResults.set([]);

    this.initializeGuessers();

    this.phase.set('word-selection');
  }
}
