import { Routes } from '@angular/router';
import { Start } from './start/start';

export const routes: Routes = [
  { path: '', component: Start },
  { path: '**', component: Start },
];
