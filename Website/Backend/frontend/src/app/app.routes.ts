import { Routes } from '@angular/router';
import {Home} from './home/home';
import {Users} from './users/users';
import {AddUser} from './add-user/add-user';

export const routes: Routes = [
  {path: "home", component: Home},

  {path: "users", component: Users},
  {path: "add-user", component: AddUser},
  {path: "**", component: Home},
];

