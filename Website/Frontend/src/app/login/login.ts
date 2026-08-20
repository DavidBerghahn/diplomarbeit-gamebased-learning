import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService, UserProfile } from '../auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  profile: UserProfile | null = null;
  message = '';

  async ngOnInit(): Promise<void> {
    try {
      if (new URLSearchParams(window.location.search).has('code')) {
        this.profile = await this.authService.finishLogin();
        this.message = 'Login erfolgreich.';
        return;
      }

      this.profile = await this.authService.loadProfile();
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

  async continueToPlatform(): Promise<void> {
    await this.router.navigateByUrl('/home');
  }
}
