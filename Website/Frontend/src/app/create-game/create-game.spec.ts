import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateGame } from './create-game';
import { provideRouter } from '@angular/router';
import { GameRestService } from '../game-rest.service';
import { ApiService } from '../api.service';

describe('CreateGame', () => {
  let component: CreateGame;
  let fixture: ComponentFixture<CreateGame>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateGame],
      providers: [
        provideRouter([]),
        {
          provide: GameRestService,
          useValue: {
            getPublicGames: vi.fn().mockResolvedValue([]),
            createGame: vi.fn().mockImplementation(async (game) => game),
          },
        },
        {
          provide: ApiService,
          useValue: { request: vi.fn() },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateGame);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
  it('saves typed Smart10 cards as a private game and avoids duplicate submission', async () => {
    component.gameName = 'Privates Spiel';
    component.isPublic = false;
    component.questions[0].frage = 'Programmieren?';
    component.questions[0].karten?.forEach((card, index) => (card.text = 'Karte ' + index));
    await component.submit();
    await component.submit();
    const save = vi.mocked(TestBed.inject(GameRestService).createGame);
    expect(save).toHaveBeenCalledTimes(1);
    expect(save.mock.calls[0][0]).toMatchObject({ spiel_typ: 'Smart10', status: 'PRIVATE' });
    expect(save.mock.calls[0][0].fragen?.[0].karten).toHaveLength(12);
    expect(component.submitted).toBe(true);
  });

  it('loads public author library content without creating a copy or opening the editor', async () => {
    component.selectGameType('just-one');
    component.selectedGameId = 'public-game';
    const request = vi.mocked(TestBed.inject(ApiService).request);
    request.mockResolvedValue({
      spiel_typ: 'JustOne',
      fragen: [{ typ: 'JUST_ONE', frage: 'Kategorie', antwortmoeglichkeiten: [
        { text: 'VerstecktesZielwort' }, { text: 'Zwei' }, { text: 'Drei' },
        { text: 'Vier' }, { text: 'Fünf' },
      ] }],
    });

    await component.loadLibrary();

    expect(request).toHaveBeenCalledWith('/api/games/public-game/library');
    expect(component.library[0].antwortmoeglichkeiten[0].text).toBe('VerstecktesZielwort');
    expect(component.saveError).toBe('');
    expect(TestBed.inject(GameRestService).createGame).not.toHaveBeenCalled();
  });
});
