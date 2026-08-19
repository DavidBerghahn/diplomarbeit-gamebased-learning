import { Routes } from '@angular/router';
import { Start } from './start/start';
import {Home} from './home/home';
import {Games} from './games/games';
import {CreateGame} from './create-game/create-game';
import {MyGames} from './my-games/my-games';

export const routes: Routes = [
  { path: '', component: Start },
  { path: 'home', component: Home },
  { path: 'games', component: Games },
  { path: 'myGames', component: MyGames },
  { path: 'createGame', component: CreateGame },
  { path: '**', component: Start },
];
