import { Component } from '@angular/core';
import { Footer } from '../footer/footer';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-start',
  imports: [Footer, RouterLink],
  templateUrl: './start.html',
  styleUrl: './start.css',
})
export class Start {}
