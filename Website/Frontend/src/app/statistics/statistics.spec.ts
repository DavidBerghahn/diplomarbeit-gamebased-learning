import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ApiService } from '../api.service';
import { AuthService, UserProfile } from '../auth.service';
import { Statistics } from './statistics';

describe('Klassenstatistik', () => {
  let fixture: ComponentFixture<Statistics>;
  let component: Statistics;
  const api = { request: vi.fn() };
  const auth = { loadProfile: vi.fn<() => Promise<UserProfile>>() };
  const profile = (role: UserProfile['role']): UserProfile => ({
    id: 'viewer', externalSubject: 'subject', username: 'viewer', displayName: 'Viewer', role, active: true,
  });
  const play = (sessionId: string, score: number, finishedAt = '2026-11-05T12:00:00Z') => ({
    sessionId,
    score,
    finishedAt,
    gameId: 'smart10',
    game: 'Programmieren',
    type: 'Smart10',
    mode: 'MULTIPLAYER',
    players: ['Anna', 'Ben'],
  });
  const students = () => [
    {
      id: 'anna',
      name: 'Anna',
      schoolClass: '5BHITM',
      plays: [play('shared', 10), play('older', 5, '2026-10-01T12:00:00Z')],
    },
    { id: 'ben', name: 'Ben', schoolClass: '5BHITM', plays: [play('shared', 10)] },
    { id: 'clara', name: 'Clara', schoolClass: '5BHITM', plays: [] },
    { id: 'david', name: 'David', schoolClass: '4AHITM', plays: [play('other', 3)] },
    { id: 'emil', name: 'Emil', schoolClass: 'Ohne Klasse', plays: [play('unassigned', 2)] },
  ];

  beforeEach(async () => {
    api.request.mockReset().mockResolvedValue(students());
    auth.loadProfile.mockReset().mockResolvedValue(profile('TEACHER'));
    await TestBed.configureTestingModule({
      imports: [Statistics],
      providers: [provideRouter([]), { provide: ApiService, useValue: api }, { provide: AuthService, useValue: auth }],
    }).compileComponents();
    fixture = TestBed.createComponent(Statistics);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('groups every student, sums individual scores and counts a shared session only once', () => {
    expect(api.request).toHaveBeenCalledWith('/api/statistics');
    const group = component.classSummaries().find((c) => c.name === '5BHITM')!;
    expect(group.points).toBe(25);
    expect(group.games).toBe(2);
    expect(group.students.map((s) => s.name)).toEqual(['Anna', 'Ben', 'Clara']);
    expect(component.totalPoints()).toBe(30);
    const section = fixture.nativeElement.querySelector(
      '[aria-label="Klasse 5BHITM"]',
    ) as HTMLElement;
    expect(section.querySelector('.class-total strong')?.textContent).toBe('25');
    expect([...section.querySelectorAll('.student-points')].map((el) => el.textContent)).toEqual([
      '15',
      '10',
      '0',
    ]);
  });

  it('filters both the class total and student list through the class dropdown', async () => {
    const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    select.value = '5BHITM';
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    expect(component.filtered()).toHaveLength(3);
    expect(component.totalPoints()).toBe(25);
    expect(fixture.nativeElement.querySelectorAll('.class-section')).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('.overview').textContent).toContain('25');
    component.studentId.set('anna');
    component.selectClass('4AHITM');
    expect(component.selected()).toBeUndefined();
  });

  it('applies the same date filter to all totals, game counts and student histories', async () => {
    component.selectClass('5BHITM');
    component.since.set('2026-11-01');
    component.studentId.set('anna');
    await fixture.whenStable();
    expect(component.totalPoints()).toBe(20);
    expect(component.classSummaries()[0].games).toBe(1);
    expect(component.plays(component.selected()!)).toHaveLength(1);
    expect(component.points(component.selected()!)).toBe(10);
    expect(fixture.nativeElement.querySelector('.details')?.textContent).toContain('10 Punkte');
    component.since.set('2027-01-01');
    expect(component.totalPoints()).toBe(0);
    expect(component.classSummaries()[0].students).toHaveLength(3);
  });

  it('persists a missing class and moves all existing points into the new class', async () => {
    component.selectClass(component.unassignedClass);
    const student = component.filtered()[0];
    component.classInputs[student.id] = ' 5bhitm ';
    api.request.mockResolvedValueOnce({ schoolClass: '5BHITM' });
    await component.assignClass(student);
    await fixture.whenStable();
    expect(api.request).toHaveBeenLastCalledWith(
      '/api/users/emil/school-class',
      { schoolClass: '5bhitm' },
      'PUT',
    );
    expect(component.unassignedCount()).toBe(0);
    expect(component.schoolClass()).toBe('5BHITM');
    expect(component.totalPoints()).toBe(27);
    expect(component.assignmentMessage()).toContain('Emil');
    expect(fixture.nativeElement.querySelector('.notice')).toBeNull();
  });

  it('keeps points and assignment unchanged when saving fails and rejects an empty class', async () => {
    const student = component.students().find((s) => s.id === 'emil')!;
    await component.assignClass(student);
    expect(api.request).toHaveBeenCalledTimes(1);
    component.classInputs[student.id] = '5BHITM';
    api.request.mockRejectedValueOnce(new Error('Speichern fehlgeschlagen'));
    await component.assignClass(student);
    expect(component.assignmentError()).toBe('Speichern fehlgeschlagen');
    expect(component.unassignedCount()).toBe(1);
    expect(component.totalPoints()).toBe(30);
  });

  it('shows authorization failures without displaying stale class totals', async () => {
    api.request.mockRejectedValueOnce(new Error('Kein Zugriff'));
    await component.load();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'Kein Zugriff',
    );
    expect(fixture.nativeElement.querySelector('.class-section')).toBeNull();
  });

  it('lets students read and filter every class without offering class assignment', async () => {
    auth.loadProfile.mockResolvedValue(profile('STUDENT'));
    const studentFixture = TestBed.createComponent(Statistics);
    studentFixture.detectChanges();
    await studentFixture.whenStable();
    studentFixture.detectChanges();
    const studentView = studentFixture.nativeElement as HTMLElement;
    const studentComponent = studentFixture.componentInstance;
    expect(studentView.querySelectorAll('.class-section')).toHaveLength(3);
    expect(studentComponent.totalPoints()).toBe(30);
    expect(studentView.textContent).not.toContain('Klassen zuordnen');
    expect(studentView.textContent).not.toContain('Zuordnen');
    expect(studentView.querySelector('.assignment')).toBeNull();
    studentComponent.selectClass('4AHITM');
    studentFixture.detectChanges();
    expect(studentComponent.totalPoints()).toBe(3);
    expect(studentView.querySelectorAll('.class-section')).toHaveLength(1);
    const emil = studentComponent.students().find((s) => s.id === 'emil')!;
    studentComponent.classInputs[emil.id] = '5BHITM';
    const requestCount = api.request.mock.calls.length;
    await studentComponent.assignClass(emil);
    expect(api.request).toHaveBeenCalledTimes(requestCount);
  });

  it.each(['TEACHER', 'ADMIN'] as const)('keeps class assignment available to %s', async (role) => {
    auth.loadProfile.mockResolvedValue(profile(role));
    const managementFixture = TestBed.createComponent(Statistics);
    managementFixture.detectChanges();
    await managementFixture.whenStable();
    managementFixture.detectChanges();
    const view = managementFixture.nativeElement as HTMLElement;
    expect(view.textContent).toContain('Klassen zuordnen');
    expect(view.querySelector('.assignment')).not.toBeNull();
  });
});
