import { Routes } from '@angular/router';
import { Start } from './start/start';
import {Home} from './home/home';
import {Games} from './games/games';
import {CreateGame} from './create-game/create-game';
import {MyGames} from './my-games/my-games';
import { Login } from './login/login';
import {Quizbattle} from './quizbattle/quizbattle';
import {Lobby} from './lobby/lobby';
import { authenticatedGuard, teacherGuard } from './auth.guard';

export const routes: Routes = [
  { path: '', component: Start },
  { path: 'login', component: Login},
  { path: 'lobby/:id', component: Lobby, canActivate: [teacherGuard]},
  { path: 'lobby', component: Lobby, canActivate: [teacherGuard]},
  { path: 'kwizbattle', component: Quizbattle, canActivate: [authenticatedGuard]},
  { path: 'home', component: Home, canActivate: [authenticatedGuard] },
  { path: 'games', component: Games, canActivate: [authenticatedGuard] },
  { path: 'myGames', component: MyGames, canActivate: [teacherGuard] },
  { path: 'createGame', component: CreateGame, canActivate: [teacherGuard] },
  { path: '**', component: Start },
];
