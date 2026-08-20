import { Component, OnInit, inject } from '@angular/core';
import { Footer } from '../footer/footer';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-start',
  imports: [Footer],
  templateUrl: './start.html',
  styleUrl: './start.css',
})
export class Start implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  message = '';

  async ngOnInit(): Promise<void> {
    if (!new URLSearchParams(window.location.search).has('code')) {
      return;
    }

    try {
      await this.authService.finishLogin();
      await this.router.navigateByUrl('/home');
    } catch (error) {
      this.message = error instanceof Error ? error.message : String(error);
    }
  }

  async login(): Promise<void> {
    try {
      await this.authService.login();
    } catch (error) {
      this.message = error instanceof Error ? error.message : String(error);
    }
  }
}
