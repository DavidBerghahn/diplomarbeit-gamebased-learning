import {Component, inject, OnInit, signal} from '@angular/core';
import {UserService} from '../user-service';
import {UserModel} from '../model/UserModel';
import {AddUser} from '../add-user/add-user';

@Component({
  selector: 'app-users',
  imports: [
    AddUser
  ],
  templateUrl: './users.html',
  styleUrl: './users.css',
})
export class Users implements OnInit {
  userService = inject(UserService)
  users = signal<UserModel[]>([])
  myWebSocket?: WebSocket

  ngOnInit(){
    this.getData()
    this.connectWebSocket()
  }

  getData(){
    this.userService.getAllUsers().subscribe(
      (data) => {
        this.users.set(data);
        console.log(data)
      }
    )
  }

  connectWebSocket(){
    this.myWebSocket = new WebSocket("ws://localhost:8080/user-socket")

    this.myWebSocket.onopen = () => {
      console.log('WebSocket verbunden');
    };

    this.myWebSocket.onmessage = (event) => {
      if (event.data === "Neue Daten vorhanden!") {
        this.getData();
      }
    };
  }
}
