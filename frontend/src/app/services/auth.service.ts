import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { map } from 'rxjs/operators';
import { AuthControllerService } from '../api/api/authController.service';
import { AuthLoginResponse } from '../api/model/authLoginResponse';
import { AuthLoginDto } from '../api/model/authLoginDto';
import { UserRegistrationDto } from '../api/model/userRegistrationDto';
import { UserRegistrationControllerService } from '../api/api/userRegistrationController.service';
import { UserControllerService } from '../api/api/userController.service';
import { UserProfileResponse } from '../api/model/userProfileResponse';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(private userControllerService: UserControllerService, private authControllerService: AuthControllerService, private userRegistrationControllerService: UserRegistrationControllerService) { }

  login(loginDto: AuthLoginDto): Observable<AuthLoginResponse> {
    return this.authControllerService.login(loginDto).pipe(
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


  getMatriculeByEmail(email: string): Observable<string | undefined> {
    return this.userControllerService.getUserByEmail(email).pipe(
      map(profile => profile?.matricule)
    );
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  signup(userData: UserRegistrationDto): Observable<{ [key: string]: string; }> {
    return this.userRegistrationControllerService.register(userData);
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
