import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MyGames } from './my-games';

describe('MyGames', () => {
  let component: MyGames;
  let fixture: ComponentFixture<MyGames>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyGames],
    }).compileComponents();

    fixture = TestBed.createComponent(MyGames);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
