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
  refresh_token?: string;
  refresh_expires_in?: number;
  refresh_expires_at?: number;
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
  private postLoginUrl = '/home';
  private refreshPromise?: Promise<string | null>;
  private refreshTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.scheduleRefresh(this.readTokens());
  }

  async login(returnUrl?: string): Promise<void> {
    const config = await this.config();
    const state = this.randomBase64Url(32);
    const codeVerifier = this.randomBase64Url(64);
    const codeChallenge = await this.sha256Base64Url(codeVerifier);

    sessionStorage.setItem(this.pkceStorageKey, JSON.stringify({
      state,
      codeVerifier,
      returnUrl: this.safeInternalReturnUrl(returnUrl),
    }));

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
    this.storeTokens(tokens);
    this.postLoginUrl = this.safeInternalReturnUrl(pkce.returnUrl);
    sessionStorage.removeItem(this.pkceStorageKey);
    window.history.replaceState({}, document.title, window.location.pathname);

    const profile = await this.loadProfile();
    if (!profile) {
      throw new Error('Backend-Profil konnte nach dem Login nicht geladen werden.');
    }
    return profile;
  }

  takePostLoginUrl(): string {
    const returnUrl = this.postLoginUrl;
    this.postLoginUrl = '/home';
    return returnUrl;
  }

  async loadProfile(): Promise<UserProfile | null> {
    const accessToken = await this.validAccessToken();
    if (!accessToken) {
      return null;
    }

    const response = await fetch('/api/auth/me', {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (response.status === 401) {
      this.clearTokens();
      return null;
    }
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

  async authorizationHeaders(): Promise<HeadersInit> {
    const accessToken = await this.validAccessToken();
    return accessToken
      ? { Authorization: `Bearer ${accessToken}` }
      : {};
  }

  invalidateSession(): void {
    this.clearTokens();
  }

  private async validAccessToken(forceRefresh = false): Promise<string | null> {
    const tokens = this.readTokens();
    if (!tokens) {
      return null;
    }

    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    const refreshWindow = Math.min(30_000, tokens.expires_in * 250);
    if (!forceRefresh && tokens.access_token && tokens.expires_at
      && tokens.expires_at > Date.now() + refreshWindow) {
      return tokens.access_token;
    }

    if (!tokens.refresh_token || (tokens.refresh_expires_at !== undefined && tokens.refresh_expires_at <= Date.now())) {
      this.clearTokens();
      return null;
    }

    if (!this.refreshPromise) {
      this.refreshPromise = this.refreshAccessToken(tokens).finally(() => {
        this.refreshPromise = undefined;
      });
    }
    return this.refreshPromise;
  }

  private async refreshAccessToken(tokens: TokenResponse): Promise<string | null> {
    let response: Response;
    try {
      const config = await this.config();
      response = await fetch(`${this.realmUrl(config)}/protocol/openid-connect/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          grant_type: 'refresh_token',
          client_id: config.keycloakClientId,
          refresh_token: tokens.refresh_token!,
        }),
      });
    } catch {
      this.scheduleRetry();
      throw new Error('Keycloak-Token konnte nicht erneuert werden.');
    }

    if (!response.ok) {
      this.clearTokens();
      return null;
    }

    try {
      const refreshed = await response.json() as TokenResponse;
      return this.storeTokens(refreshed, tokens);
    } catch {
      this.clearTokens();
      return null;
    }
  }

  private storeTokens(response: TokenResponse, previous?: TokenResponse): string {
    if (typeof response.access_token !== 'string' || !response.access_token
      || !Number.isFinite(response.expires_in) || response.expires_in <= 0) {
      throw new Error('Keycloak lieferte keinen gültigen Access-Token.');
    }

    const now = Date.now();
    const refreshToken = typeof response.refresh_token === 'string' && response.refresh_token
      ? response.refresh_token : previous?.refresh_token;
    const refreshExpiresIn = Number.isFinite(response.refresh_expires_in)
      ? response.refresh_expires_in : undefined;
    const refreshExpiresAt = refreshExpiresIn !== undefined
      ? now + refreshExpiresIn * 1000
      : refreshToken === previous?.refresh_token ? previous?.refresh_expires_at : undefined;
    const tokens: TokenResponse = {
      access_token: response.access_token,
      id_token: response.id_token ?? previous?.id_token,
      expires_in: response.expires_in,
      expires_at: now + response.expires_in * 1000,
      refresh_token: refreshToken,
      refresh_expires_in: refreshExpiresIn,
      refresh_expires_at: refreshExpiresAt,
    };
    sessionStorage.setItem(this.tokenStorageKey, JSON.stringify(tokens));
    this.scheduleRefresh(tokens);
    return tokens.access_token;
  }

  private scheduleRefresh(tokens: TokenResponse | null): void {
    clearTimeout(this.refreshTimer);
    if (!tokens?.refresh_token || !tokens.expires_at
      || (tokens.refresh_expires_at !== undefined && tokens.refresh_expires_at <= Date.now())) {
      return;
    }

    const leadTime = Math.min(60_000, tokens.expires_in * 500);
    const delay = Math.max(1_000, tokens.expires_at - Date.now() - leadTime);
    this.refreshTimer = setTimeout(() => {
      if (document.visibilityState === 'visible') {
        void this.validAccessToken(true).catch(() => undefined);
      }
    }, delay);
  }

  private scheduleRetry(): void {
    clearTimeout(this.refreshTimer);
    this.refreshTimer = setTimeout(() => {
      void this.validAccessToken(true).catch(() => undefined);
    }, 30_000);
  }

  private clearTokens(): void {
    clearTimeout(this.refreshTimer);
    sessionStorage.removeItem(this.tokenStorageKey);
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

  private safeInternalReturnUrl(value?: unknown): string {
    if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) {
      return '/home';
    }

    const url = new URL(value, window.location.origin);
    const allowedPath = /^\/(?:home|games|myGames|createGame|kwizbattle|lobby(?:\/[^/]+)?)$/;
    if (url.origin !== window.location.origin || !allowedPath.test(url.pathname)) {
      return '/home';
    }

    return `${url.pathname}${url.search}${url.hash}`;
  }

  private readTokens(): TokenResponse | null {
    try {
      return JSON.parse(sessionStorage.getItem(this.tokenStorageKey) || 'null') as TokenResponse | null;
    } catch {
      this.clearTokens();
      return null;
    }
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
