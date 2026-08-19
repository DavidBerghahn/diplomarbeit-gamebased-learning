import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Quizbattle } from './quizbattle';

describe('Quizbattle', () => {
  let component: Quizbattle;
  let fixture: ComponentFixture<Quizbattle>;

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
});
