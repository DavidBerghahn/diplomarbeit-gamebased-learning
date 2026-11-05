import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { apiErrorMessage } from './api-error';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly auth = inject(AuthService);
  async request<T>(
    path: string,
    body?: unknown,
    method = body === undefined ? 'GET' : 'POST',
  ): Promise<T> {
    const response = await fetch(path, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(await this.auth.authorizationHeaders()),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    if (response.status === 401) this.auth.invalidateSession();
    if (!response.ok)
      throw new Error(
        apiErrorMessage(await response.text(), `Anfrage fehlgeschlagen (HTTP ${response.status}).`),
      );
    return response.status === 204 ? (undefined as T) : response.json();
  }
}
