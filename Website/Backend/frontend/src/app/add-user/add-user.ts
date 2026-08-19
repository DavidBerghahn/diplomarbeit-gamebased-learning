import {Component, inject, OnInit, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Console, Game} from '../model/UserModel';
import {UserService} from '../user-service';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-add-user',
  imports: [
    ReactiveFormsModule,
    RouterLink
  ],
  templateUrl: './add-user.html',
  styleUrl: './add-user.css',
})
export class AddUser implements OnInit {
  formbuilder = inject(FormBuilder)
  userService = inject(UserService)

  consoles = signal<Console[]>([])
  games = signal<Game[]>([])

  form = this.formbuilder.group({
    username: ["", Validators.required],
    favGame: ["", Validators.required],
    favConsole: ["", Validators.required],
  })

  ngOnInit() {
    this.userService.getAllConsoles().subscribe((data) => this.consoles.set(data))
    this.userService.getAllGames().subscribe((data) => this.games.set(data))
  }

  saveUser() {
    if (this.form.invalid){
      return;
    }

    const username = this.form.value.username!;
    const consoleId = Number(this.form.value.favConsole);
    const gameId = Number(this.form.value.favGame);

    this.userService.createUser(username, consoleId, gameId).subscribe(() => {
      this.form.reset();
    })
  }
}
