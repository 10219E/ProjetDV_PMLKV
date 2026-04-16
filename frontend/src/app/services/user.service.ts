import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UserControllerService } from '../api/api/userController.service';
import { UserProfileResponse } from '../api/model/userProfileResponse';
import { AuthService } from './auth.service';

/**
 * Simple wrapper service around the generated API client for users.
 * Provides methods consumed by UI components to fetch user information.
 */
@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private userControllerService: UserControllerService, private authService: AuthService) {}

  /**
   * Fetch user by id (matricule) from /api/users/{userid}
   * This uses the generated client method which maps to that endpoint.
   * @param userId the user's identifier (matricule)
   */
  getUserById(userId: string): Observable<UserProfileResponse> {
    if (!userId) throw new Error('userId is required');
    return this.userControllerService.getUserByMatricule(userId);
  }

  /**
   * Fetch the current authenticated user's information from /api/users/me
   */
  getCurrentUser(): Observable<UserProfileResponse> {
    return this.userControllerService.getCurrentUser();
  }

  /**
   * Fetch all users. Returns the generated controller model type so callers get a typed array.
   * Ensures an Authorization header is set from the AuthService token before calling the generated client.
   */
  getAllUsers(): Observable<Array<UserProfileResponse>> {
    const token = this.authService.getToken();
    if (token) {
      // The generated client uses `defaultHeaders` (from BaseService) for each request.
      // Set Authorization here so the underlying http call includes the bearer token.
      this.userControllerService.defaultHeaders = this.userControllerService.defaultHeaders.set('Authorization', `Bearer ${token}`);
    }
    return this.userControllerService.getAllUsers();
  }
}



