import { Component, OnInit, inject } from '@angular/core';
import {RouterLink} from '@angular/router';
import { Footer } from '../footer/footer';
import { AuthService, UserProfile } from '../auth.service';

@Component({
  selector: 'app-home',
  imports: [RouterLink, Footer],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  private readonly authService = inject(AuthService);

  profile: UserProfile | null = null;
  debugVisible = false;
  debugText = '';
  authError = '';
  accessNotice = '';

  get canManageGames(): boolean {
    return this.profile?.role === 'TEACHER' || this.profile?.role === 'ADMIN';
  }

  async ngOnInit(): Promise<void> {
    if (new URLSearchParams(window.location.search).get('reason') === 'teacher-required') {
      this.accessNotice = 'Diese Funktion ist nur für Lehrkräfte und Administratoren verfügbar.';
    }

    try {
      this.profile = await this.authService.loadProfile();
      this.debugText = JSON.stringify(this.authService.debugInfo(this.profile), null, 2);
    } catch (error) {
      this.authError = error instanceof Error ? error.message : String(error);
    }
  }

  toggleAuthDetails(): void {
    this.debugVisible = !this.debugVisible;
  }
}
