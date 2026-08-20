import { Component, OnInit, inject } from '@angular/core';
import { Footer } from '../footer/footer';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-start',
  imports: [Footer, RouterLink],
  templateUrl: './start.html',
  styleUrl: './start.css',
})
export class Start implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  async ngOnInit(): Promise<void> {
    if (!new URLSearchParams(window.location.search).has('code')) {
      return;
    }

    await this.authService.finishLogin();
    await this.router.navigateByUrl('/home');
  }
}
