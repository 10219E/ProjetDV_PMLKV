import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface AuthLoginResponse {
  tokenType: string;
  accessToken: string;
  expiresIn: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/auth'; // Adjusted to base auth URL

  constructor(private http: HttpClient) { }

  login(login: string, password: string): Observable<AuthLoginResponse> {
    return this.http.post<AuthLoginResponse>(`${this.apiUrl}/login`, { login, password }).pipe(
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
    // Basic stub, expects backend to have /auth/signup returning some confirmation or token
    return this.http.post<any>(`${this.apiUrl}/signup`, userData);
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
