import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateGame } from './create-game';

describe('CreateGame', () => {
  let component: CreateGame;
  let fixture: ComponentFixture<CreateGame>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateGame],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateGame);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
