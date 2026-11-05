import { Routes } from '@angular/router';
import { Start } from './start/start';
import { Home } from './home/home';
import { Games } from './games/games';
import { CreateGame } from './create-game/create-game';
import { MyGames } from './my-games/my-games';
import { Login } from './login/login';
import { Quizbattle } from './quizbattle/quizbattle';
import { Lobby } from './lobby/lobby';
import { Solo } from './solo/solo';
import { authenticatedGuard, teacherGuard } from './auth.guard';
import { Statistics } from './statistics/statistics';
import { HostedSessions } from './statistics/hosted';

export const routes: Routes = [
  { path: '', component: Start },
  { path: 'login', component: Login },
  { path: 'lobby/:id', component: Lobby, canActivate: [authenticatedGuard] },
  { path: 'lobby', component: Lobby, canActivate: [authenticatedGuard] },
  { path: 'justOne', redirectTo: 'games', pathMatch: 'full' },
  { path: 'kwizbattle', component: Quizbattle, canActivate: [authenticatedGuard] },
  { path: 'solo/:id', component: Solo, canActivate: [authenticatedGuard] },
  { path: 'home', component: Home, canActivate: [authenticatedGuard] },
  { path: 'statistics', component: Statistics, canActivate: [authenticatedGuard] },
  { path: 'hosted', component: HostedSessions, canActivate: [authenticatedGuard] },
  { path: 'games', component: Games, canActivate: [authenticatedGuard] },
  { path: 'myGames', component: MyGames, canActivate: [teacherGuard] },
  { path: 'createGame', component: CreateGame, canActivate: [teacherGuard] },
  { path: '**', component: Start },
];
