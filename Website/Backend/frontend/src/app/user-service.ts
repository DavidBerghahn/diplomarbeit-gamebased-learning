import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Console, Game, UserModel} from './model/UserModel';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  http = inject(HttpClient)

  getAllUsers(){
    return this.http.get<UserModel[]>("http://localhost:8080/api/user")
  }

  getAllConsoles(){
    return this.http.get<Console[]>("http://localhost:8080/api/console")
  }

  getAllGames(){
    return this.http.get<Game[]>("http://localhost:8080/api/game")
  }

  createUser(username: string, consoleId: number, gameId: number){
    return this.http.post(
      `http://localhost:8080/api/user/createUser/${username}/${consoleId}/${gameId}`,
      null
    )
  }
}
