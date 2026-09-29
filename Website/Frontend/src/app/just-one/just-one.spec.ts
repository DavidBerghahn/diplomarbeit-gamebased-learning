import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JustOne } from './just-one';

describe('JustOne', () => {
  let component: JustOne;
  let fixture: ComponentFixture<JustOne>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JustOne],
    }).compileComponents();

    fixture = TestBed.createComponent(JustOne);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
