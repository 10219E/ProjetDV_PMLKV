import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AuthApi } from '../api/auth.api';
import { AuthLoginResponse } from '../models/auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(private authApi: AuthApi) { }

  login(login: string, password: string): Observable<AuthLoginResponse> {
    return this.authApi.login({ login, password }).pipe(
      tap(response => {
        if (response.accessToken) {
          localStorage.setItem('auth_token', response.accessToken);
        }
      })
    );
  }

  logout(): void {
    localStorage.removeItem('auth_token');
  }

  getToken(): string | null {
    return localStorage.getItem('auth_token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  signup(userData: any): Observable<any> {
    return this.authApi.register(userData);
  }

  getUserRole(): string {
    const token = this.getToken();
    if (!token) return 'Unknown';
    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload));

      // Spring populates a 'roles' array/string
      let roles = decoded.roles || decoded.role || [];
      if (Array.isArray(roles)) {
        return roles.join(', ');
      }
      return roles;
    } catch (e) {
      return 'role not found';
    }
  }
}
