import { Component, OnInit, inject, signal } from '@angular/core';
import {Router, RouterLink} from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Footer } from '../footer/footer';
import { AuthService, UserProfile } from '../auth.service';
import { AdminView, AdminViewService } from '../admin-view.service';
import { MultiplayerWebSocketService } from '../multiplayer-websocket.service';

@Component({
  selector: 'app-home',
  imports: [RouterLink, FormsModule, Footer],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly adminViewService = inject(AdminViewService);

  profile = signal<UserProfile | null>(null);
  debugVisible = false;
  debugText = '';
  authError = '';
  accessNotice = '';
  joinCode = '';
  joinError = '';
  private readonly router = inject(Router);
  private readonly multiplayer = inject(MultiplayerWebSocketService);

  get adminView(): AdminView {
    return this.adminViewService.view();
  }

  get isAdmin(): boolean {
    return this.profile()?.role === 'ADMIN';
  }

  get canManageGames(): boolean {
    return this.profile()?.role === 'TEACHER' || (this.isAdmin && this.adminView === 'TEACHER');
  }

  selectAdminView(view: AdminView): void {
    if (this.isAdmin) {
      this.adminViewService.select(view);
    }
  }

  async ngOnInit(): Promise<void> {
    if (new URLSearchParams(window.location.search).get('reason') === 'teacher-required') {
      this.accessNotice = 'Diese Funktion ist nur für Lehrkräfte und Administratoren verfügbar.';
    }

    try {
      this.profile.set(await this.authService.loadProfile());
      this.debugText = JSON.stringify(this.authService.debugInfo(this.profile()), null, 2);
    } catch (error) {
      this.authError = error instanceof Error ? error.message : String(error);
    }
  }

  toggleAuthDetails(): void {
    this.debugVisible = !this.debugVisible;
  }

  async joinGame(): Promise<void> {
    this.joinError = '';
    const code = this.joinCode.trim();
    const name = this.profile()?.displayName || this.profile()?.username;
    if (!/^\d{4}$/.test(code)) {
      this.joinError = 'Bitte gib einen vierstelligen Spielcode ein.';
      return;
    }
    try {
      await this.multiplayer.joinRoom(code, name || 'Spieler');
      await this.router.navigate(['/lobby']);
    } catch (error) {
      this.joinError = error instanceof Error ? error.message : 'Der Beitritt zum Spiel ist fehlgeschlagen.';
    }
  }
}
