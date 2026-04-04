import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, Observable, of, switchMap, tap } from 'rxjs';

import { AuthResponse, CurrentUser, LoginRequest, RegisterRequest } from './auth.models';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;
  private readonly tokenStorageKey = 'pulse_auth_token';

  readonly currentUser = signal<CurrentUser | null>(null);

  constructor(private readonly http: HttpClient) {}

  login(payload: LoginRequest): Observable<CurrentUser> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, payload).pipe(
      tap((response) => this.storeJwt(response.token)),
      switchMap(() => this.getCurrentUser())
    );
  }

  register(payload: RegisterRequest): Observable<CurrentUser> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, payload).pipe(
      tap((response) => this.storeJwt(response.token)),
      switchMap(() => this.getCurrentUser())
    );
  }

  getCurrentUser(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(`${this.baseUrl}/me`).pipe(
      tap((user) => this.currentUser.set(user))
    );
  }

  hydrateCurrentUser(): Observable<CurrentUser | null> {
    if (!this.getJwtToken()) {
      this.currentUser.set(null);
      return of(null);
    }

    return this.getCurrentUser().pipe(
      catchError(() => {
        this.clearSession();
        return of(null);
      })
    );
  }

  isAuthenticated(): boolean {
    return !!this.getJwtToken();
  }

  getJwtToken(): string | null {
    return localStorage.getItem(this.tokenStorageKey);
  }

  clearSession(): void {
    localStorage.removeItem(this.tokenStorageKey);
    this.currentUser.set(null);
  }

  private storeJwt(token: string): void {
    localStorage.setItem(this.tokenStorageKey, token);
  }
}
