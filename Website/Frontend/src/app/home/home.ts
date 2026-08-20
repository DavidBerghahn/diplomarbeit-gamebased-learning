import { Component } from '@angular/core';
import {RouterLink} from '@angular/router';
import { Footer } from '../footer/footer';

@Component({
  selector: 'app-home',
  imports: [RouterLink, Footer],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {}
