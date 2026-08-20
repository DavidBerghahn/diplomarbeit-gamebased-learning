import { Injectable } from '@angular/core';

interface AuthConfig {
  provider: string;
  keycloakUrl: string;
  keycloakRealm: string;
  keycloakClientId: string;
}

interface TokenResponse {
  access_token: string;
  id_token?: string;
  expires_in: number;
  expires_at?: number;
}

export interface UserProfile {
  id: string;
  externalSubject: string;
  username: string;
  displayName: string;
  schoolClass?: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
  active: boolean;
}

export interface AuthDebugInfo {
  backendUser: UserProfile | null;
  token: {
    hasAccessToken: boolean;
    hasIdToken: boolean;
    expiresAt: string | null;
    expiresInSeconds: number | null;
  };
  accessTokenClaims: Record<string, unknown> | null;
  idTokenClaims: Record<string, unknown> | null;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly tokenStorageKey = 'gamebased.keycloak.tokens';
  private readonly pkceStorageKey = 'gamebased.keycloak.pkce';
  private authConfig?: AuthConfig;

  async login(): Promise<void> {
    const config = await this.config();
    const state = this.randomBase64Url(32);
    const codeVerifier = this.randomBase64Url(64);
    const codeChallenge = await this.sha256Base64Url(codeVerifier);

    sessionStorage.setItem(this.pkceStorageKey, JSON.stringify({ state, codeVerifier }));

    const params = new URLSearchParams({
      client_id: config.keycloakClientId,
      redirect_uri: this.redirectUri(),
      response_type: 'code',
      scope: 'openid profile email',
      state,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
    });

    window.location.href = `${this.realmUrl(config)}/protocol/openid-connect/auth?${params}`;
  }

  async finishLogin(): Promise<UserProfile> {
    const config = await this.config();
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');
    const state = params.get('state');
    const pkce = JSON.parse(sessionStorage.getItem(this.pkceStorageKey) || '{}');

    if (!code || !state || state !== pkce.state || !pkce.codeVerifier) {
      throw new Error('Keycloak-Rückleitung konnte nicht verifiziert werden.');
    }

    const tokenResponse = await fetch(`${this.realmUrl(config)}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        grant_type: 'authorization_code',
        client_id: config.keycloakClientId,
        redirect_uri: this.redirectUri(),
        code,
        code_verifier: pkce.codeVerifier,
      }),
    });

    if (!tokenResponse.ok) {
      throw new Error(`Token-Austausch fehlgeschlagen: HTTP ${tokenResponse.status}`);
    }

    const tokens = await tokenResponse.json() as TokenResponse;
    tokens.expires_at = Date.now() + tokens.expires_in * 1000;
    sessionStorage.setItem(this.tokenStorageKey, JSON.stringify(tokens));
    sessionStorage.removeItem(this.pkceStorageKey);
    window.history.replaceState({}, document.title, window.location.pathname);

    const profile = await this.loadProfile();
    if (!profile) {
      throw new Error('Backend-Profil konnte nach dem Login nicht geladen werden.');
    }
    return profile;
  }

  async loadProfile(): Promise<UserProfile | null> {
    const tokens = this.readTokens();
    if (!tokens?.access_token) {
      return null;
    }

    if (tokens.expires_at && tokens.expires_at < Date.now()) {
      sessionStorage.removeItem(this.tokenStorageKey);
      return null;
    }

    const response = await fetch('/api/auth/me', {
      headers: {
        Authorization: `Bearer ${tokens.access_token}`,
      },
    });

    if (!response.ok) {
      throw new Error(`Backend-Profil konnte nicht geladen werden: HTTP ${response.status}`);
    }

    return response.json() as Promise<UserProfile>;
  }

  debugInfo(profile: UserProfile | null): AuthDebugInfo {
    const tokens = this.readTokens();

    return {
      backendUser: profile,
      token: {
        hasAccessToken: Boolean(tokens?.access_token),
        hasIdToken: Boolean(tokens?.id_token),
        expiresAt: tokens?.expires_at ? new Date(tokens.expires_at).toISOString() : null,
        expiresInSeconds: tokens?.expires_at ? Math.max(0, Math.floor((tokens.expires_at - Date.now()) / 1000)) : null,
      },
      accessTokenClaims: this.tokenClaims(tokens?.access_token),
      idTokenClaims: this.tokenClaims(tokens?.id_token),
    };
  }

  private async config(): Promise<AuthConfig> {
    if (this.authConfig) {
      return this.authConfig;
    }

    const response = await fetch('/api/auth/config');
    if (!response.ok) {
      throw new Error(`/api/auth/config lieferte HTTP ${response.status}`);
    }
    this.authConfig = await response.json() as AuthConfig;
    return this.authConfig;
  }

  private realmUrl(config: AuthConfig): string {
    return `${config.keycloakUrl}/realms/${config.keycloakRealm}`;
  }

  private redirectUri(): string {
    return `${window.location.origin}/`;
  }

  private readTokens(): TokenResponse | null {
    return JSON.parse(sessionStorage.getItem(this.tokenStorageKey) || 'null') as TokenResponse | null;
  }

  private async sha256Base64Url(value: string): Promise<string> {
    const data = new TextEncoder().encode(value);
    const digest = await crypto.subtle.digest('SHA-256', data);
    return this.bytesToBase64Url(new Uint8Array(digest));
  }

  private randomBase64Url(length: number): string {
    const bytes = new Uint8Array(length);
    crypto.getRandomValues(bytes);
    return this.bytesToBase64Url(bytes);
  }

  private bytesToBase64Url(bytes: Uint8Array): string {
    let binary = '';
    bytes.forEach((byte) => {
      binary += String.fromCharCode(byte);
    });
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }

  private tokenClaims(token?: string): Record<string, unknown> | null {
    const payload = token?.split('.')[1];
    if (!payload) {
      return null;
    }

    return JSON.parse(new TextDecoder().decode(this.base64UrlToBytes(payload))) as Record<string, unknown>;
  }

  private base64UrlToBytes(value: string): Uint8Array {
    const base64 = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=');
    const binary = atob(base64);
    return Uint8Array.from(binary, (character) => character.charCodeAt(0));
  }
}
