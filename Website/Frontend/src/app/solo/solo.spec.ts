import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { ApiService } from '../api.service';
import { GameRestService } from '../game-rest.service';
import { Solo } from './solo';

describe('Server-backed solo sessions', () => {
  async function setup(id: string, request: ReturnType<typeof vi.fn>) {
    TestBed.configureTestingModule({
      imports: [Solo],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id }) } },
        },
        { provide: ApiService, useValue: { request } },
        { provide: GameRestService, useValue: {} },
      ],
    });
    const fixture = TestBed.createComponent(Solo);
    await fixture.whenStable();
    return fixture;
  }
  it('does not silently replace a failed stored game with a demo', async () => {
    const fixture = await setup(
      'stored-game',
      vi.fn().mockRejectedValue(new Error('Spiel nicht verfügbar')),
    );
    expect(fixture.componentInstance.questions()).toHaveLength(0);
    expect(fixture.componentInstance.error()).toBe('Spiel nicht verfügbar');
  });
  it('keeps explicit demos completely local', async () => {
    const request = vi.fn();
    const fixture = await setup('demo-programming-true-false', request);
    const solo = fixture.componentInstance;
    solo.selectCard(solo.questions()[0].cards[0]);
    await solo.answerTrueFalse(true);
    expect(solo.correctCards()).toBe(1);
    expect(request).not.toHaveBeenCalled();
  });
  it('only confirms saving after a successful server commit and supports retry', async () => {
    let answers = 0,
      finishes = 0;
    const request = vi.fn(async (path: string) => {
      if (path.endsWith('/start'))
        return {
          roundId: 'round-1',
          title: 'Test',
          questions: [
            {
              prompt: 'Test?',
              mode: 'TRUE_FALSE',
              cards: Array.from({ length: 12 }, (_, i) => ({
                id: String(i),
                position: i + 1,
                text: 'Karte',
              })),
            },
          ],
        };
      if (path.endsWith('/answers')) {
        answers++;
        return {
          correct: true,
          solution: true,
          correctCards: answers,
          completedQuestions: answers === 12 ? 1 : 0,
          bestStreak: answers,
        };
      }
      if (path.endsWith('/finish')) {
        if (++finishes === 1) throw new Error('DB nicht erreichbar');
        return { sessionId: 'session-1' };
      }
      return undefined;
    });
    const fixture = await setup('stored-game', request);
    const solo = fixture.componentInstance;
    for (let i = 0; i < 12; i++) {
      solo.selectCard(solo.questions()[0].cards[i]);
      await solo.answerTrueFalse(true);
    }
    await solo.nextQuestion();
    expect(solo.finished()).toBe(true);
    expect(solo.sessionId()).toBeNull();
    expect(solo.error()).toContain('DB');
    await solo.finishSaved();
    expect(solo.sessionId()).toBe('session-1');
    expect(solo.score()).toBe(46);
  });
});
