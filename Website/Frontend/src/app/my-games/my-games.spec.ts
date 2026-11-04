import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MyGames } from './my-games';
import { provideRouter } from '@angular/router';
import { GameRestService } from '../game-rest.service';

describe('MyGames', () => {
  let component: MyGames;
  let fixture: ComponentFixture<MyGames>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyGames],
      providers: [
        provideRouter([]),
        { provide: GameRestService, useValue: { getMyGames: vi.fn().mockResolvedValue([]) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MyGames);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
