import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, vi } from 'vitest';

import { Quizbattle } from './quizbattle';

describe('Quizbattle', () => {
  let component: Quizbattle;
  let fixture: ComponentFixture<Quizbattle>;

  afterEach(() => {
    fixture.destroy();
    vi.useRealTimers();
  });

  function recreateWithFakeTimers(): void {
    fixture.destroy();
    vi.useFakeTimers();
    fixture = TestBed.createComponent(Quizbattle);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Quizbattle],
    }).compileComponents();

    fixture = TestBed.createComponent(Quizbattle);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('reveals the correct answer in green after a correct answer and awards points', () => {
    component.selectField(1);
    component.answerSelectedField(true);
    fixture.detectChanges();

    const answeredField: HTMLButtonElement = fixture.nativeElement.querySelector('.quizbattle-field.answered');
    expect(answeredField.textContent).toContain('Wahr');
    expect(answeredField.classList).toContain('correct');
    expect(component.teams()[0].points).toBe(1);
    expect(component.teams()[0].eliminated).toBe(false);
  });

  it('reveals false in green after correctly answering a false field', () => {
    component.selectField(9);
    component.answerSelectedField(false);
    fixture.detectChanges();

    const answeredField: HTMLButtonElement = fixture.nativeElement.querySelector('.quizbattle-field.answered');
    expect(answeredField.textContent).toContain('Falsch');
    expect(answeredField.classList).toContain('correct');
    expect(component.teams()[0].points).toBe(1);
    expect(component.teams()[0].eliminated).toBe(false);
  });

  it('reveals the correct answer in red after an incorrect answer and eliminates the team', () => {
    component.selectField(1);
    component.answerSelectedField(false);
    fixture.detectChanges();

    const answeredField: HTMLButtonElement = fixture.nativeElement.querySelector('.quizbattle-field.answered');
    expect(answeredField.textContent).toContain('Wahr');
    expect(answeredField.classList).toContain('incorrect');
    expect(component.teams()[0].points).toBe(0);
    expect(component.teams()[0].eliminated).toBe(true);
  });

  it('does not reveal the correct answer before an answer is submitted', () => {
    component.selectField(1);
    fixture.detectChanges();

    const activeField: HTMLButtonElement = fixture.nativeElement.querySelector('.quizbattle-field.active');
    expect(activeField.textContent).not.toContain('Wahr');
    expect(activeField.textContent).not.toContain('Falsch');
  });

  it('starts the first team timer before any field is selected and shows it in the turn banner', () => {
    recreateWithFakeTimers();

    expect(component.currentTeamIndex()).toBe(0);
    expect(component.selectedField()).toBeNull();
    expect(fixture.nativeElement.querySelector('.turn-banner .answer-timer').textContent).toContain('20 s');
    expect(fixture.nativeElement.querySelector('.answer-panel .answer-timer')).toBeNull();

    vi.advanceTimersByTime(5000);
    fixture.detectChanges();
    expect(component.answerSecondsRemaining()).toBe(15);
    expect(fixture.nativeElement.querySelector('.turn-banner .answer-timer').textContent).toContain('15 s');
  });

  it('keeps counting through field selection, field changes, and cancellation', () => {
    recreateWithFakeTimers();
    vi.advanceTimersByTime(5000);
    component.selectField(2);
    expect(component.answerSecondsRemaining()).toBe(15);

    vi.advanceTimersByTime(4000);
    component.selectField(1);
    expect(component.answerSecondsRemaining()).toBe(11);
    vi.advanceTimersByTime(4000);
    component.cancelAnswer();
    expect(component.answerSecondsRemaining()).toBe(7);
    expect(component.selectedField()).toBeNull();

    vi.advanceTimersByTime(7000);
    expect(component.teams()[0].eliminated).toBe(true);
    expect(component.currentTeamIndex()).toBe(1);
  });

  it('times out without a field selection and skips the first team once', () => {
    recreateWithFakeTimers();

    vi.advanceTimersByTime(20_000);

    expect(component.teams()[0].eliminated).toBe(true);
    expect(component.activeTeams()).toBe(2);
    expect(component.currentTeamIndex()).toBe(1);
    expect(component.selectedField()).toBeNull();
    expect(component.answerSecondsRemaining()).toBe(20);

    vi.advanceTimersByTime(1000);
    expect(component.activeTeams()).toBe(2);
    expect(component.currentTeamIndex()).toBe(1);
    expect(component.answerSecondsRemaining()).toBe(19);
  });

  it('starts the next team with a fresh 20 second timer after an answer', () => {
    recreateWithFakeTimers();
    vi.advanceTimersByTime(7000);
    component.selectField(1);
    component.answerSelectedField(true);
    expect(component.currentTeamIndex()).toBe(1);
    expect(component.selectedField()).toBeNull();
    expect(component.answerSecondsRemaining()).toBe(20);

    vi.advanceTimersByTime(1000);
    expect(component.answerSecondsRemaining()).toBe(19);
  });

  it('starts the next team with a fresh 20 second timer after manual pass', () => {
    recreateWithFakeTimers();
    vi.advanceTimersByTime(7000);
    component.passTurn();

    expect(component.currentTeamIndex()).toBe(1);
    expect(component.answerSecondsRemaining()).toBe(20);

    vi.advanceTimersByTime(1000);
    expect(component.answerSecondsRemaining()).toBe(19);
  });

  it('renews the timer for the same last team after each correct answer', () => {
    recreateWithFakeTimers();
    component.passTurn();
    component.passTurn();
    expect(component.currentTeamIndex()).toBe(2);
    expect(component.activeTeams()).toBe(1);

    vi.advanceTimersByTime(6000);
    component.selectField(1);
    component.answerSelectedField(true);
    expect(component.currentTeamIndex()).toBe(2);
    expect(component.answerSecondsRemaining()).toBe(20);
    vi.advanceTimersByTime(1000);
    expect(component.answerSecondsRemaining()).toBe(19);

    vi.advanceTimersByTime(5000);
    component.selectField(2);
    component.answerSelectedField(true);
    expect(component.currentTeamIndex()).toBe(2);
    expect(component.answerSecondsRemaining()).toBe(20);
    vi.advanceTimersByTime(1000);
    expect(component.answerSecondsRemaining()).toBe(19);

    vi.advanceTimersByTime(19_000);
    expect(component.teams()[2].eliminated).toBe(true);
    expect(component.activeTeams()).toBe(0);
    expect(component.answerSecondsRemaining()).toBe(0);
    vi.advanceTimersByTime(10_000);
    expect(component.answerSecondsRemaining()).toBe(0);
    expect(component.activeTeams()).toBe(0);
  });

  it('ends the game after the last timeout, reveals unanswered fields, and preserves answered feedback', () => {
    recreateWithFakeTimers();
    component.selectField(1);
    component.answerSelectedField(false);
    component.passTurn();
    expect(component.activeTeams()).toBe(1);

    component.selectField(9);
    component.answerSelectedField(false);
    expect(component.currentTeamIndex()).toBe(2);
    expect(component.answerSecondsRemaining()).toBe(20);
    vi.advanceTimersByTime(20_000);

    expect(component.activeTeams()).toBe(0);
    expect(component.gameOver()).toBe(true);
    expect(component.answerSecondsRemaining()).toBe(0);
    fixture.detectChanges();

    const banner: HTMLElement = fixture.nativeElement.querySelector('.turn-banner');
    expect(banner.textContent).toContain('Spiel beendet');
    const fields: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.quizbattle-field'));
    const wrongAnsweredField = fields.find((field) => field.textContent?.includes('Merkur'));
    const correctAnsweredField = fields.find((field) => field.textContent?.includes('Pluto'));
    const unansweredTrueField = fields.find((field) => field.textContent?.includes('Venus'));
    const unansweredFalseField = fields.find((field) => field.textContent?.includes('Sonne'));

    expect(wrongAnsweredField?.textContent).toContain('Wahr');
    expect(wrongAnsweredField?.classList).toContain('incorrect');
    expect(correctAnsweredField?.textContent).toContain('Falsch');
    expect(correctAnsweredField?.classList).toContain('correct');
    expect(unansweredTrueField?.textContent).toContain('Wahr');
    expect(unansweredTrueField?.classList).toContain('game-over-unanswered');
    expect(unansweredTrueField?.disabled).toBe(true);
    expect(unansweredFalseField?.textContent).toContain('Falsch');
    expect(unansweredFalseField?.classList).toContain('game-over-unanswered');
    expect(unansweredFalseField?.disabled).toBe(true);

    const answerButtons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.answer-panel .answer-button'));
    expect(answerButtons.every((button) => button.disabled)).toBe(true);
    expect((fixture.nativeElement.querySelector('.pass-button') as HTMLButtonElement).disabled).toBe(true);

    const fieldsAtGameOver = component.fields();
    const teamsAtGameOver = component.teams();
    component.selectField(2);
    component.answerSelectedField(true);
    component.cancelAnswer();
    component.passTurn();
    expect(component.fields()).toEqual(fieldsAtGameOver);
    expect(component.teams()).toEqual(teamsAtGameOver);

    vi.advanceTimersByTime(10_000);
    expect(component.activeTeams()).toBe(0);
    expect(component.answerSecondsRemaining()).toBe(0);
  });
});
