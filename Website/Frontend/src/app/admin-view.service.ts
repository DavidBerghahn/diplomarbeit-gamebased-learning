import { Injectable, signal } from '@angular/core';

export type AdminView = 'STUDENT' | 'TEACHER';

@Injectable({ providedIn: 'root' })
export class AdminViewService {
  readonly view = signal<AdminView>('TEACHER');

  select(view: AdminView): void {
    this.view.set(view);
  }
}
