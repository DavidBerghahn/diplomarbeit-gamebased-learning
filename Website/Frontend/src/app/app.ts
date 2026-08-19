import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Start } from './start/start';
import { Footer } from './footer/footer';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Start, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('Frontend');
}
