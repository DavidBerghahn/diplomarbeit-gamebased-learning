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
  private returnUrl: string | undefined;

  async ngOnInit(): Promise<void> {
    const params = new URLSearchParams(window.location.search);
    this.message = this.messageForReason(params.get('reason'));
    this.returnUrl = params.get('returnUrl') ?? undefined;

    if (!params.has('code')) {
      return;
    }

    try {
      await this.authService.finishLogin();
      await this.router.navigateByUrl(this.authService.takePostLoginUrl());
    } catch (error) {
      this.message = error instanceof Error ? error.message : String(error);
    }
  }

  async login(): Promise<void> {
    try {
      await this.authService.login(this.returnUrl);
    } catch (error) {
      this.message = error instanceof Error ? error.message : String(error);
    }
  }

  private messageForReason(reason: string | null): string {
    switch (reason) {
      case 'login-required':
        return 'Bitte melde dich zuerst mit deinem Schulaccount an.';
      case 'account-inactive':
        return 'Dein Konto ist derzeit nicht aktiv. Bitte wende dich an eine Lehrkraft.';
      case 'session-check-failed':
        return 'Deine Anmeldung konnte nicht überprüft werden. Bitte versuche es erneut.';
      default:
        return '';
    }
  }
}
