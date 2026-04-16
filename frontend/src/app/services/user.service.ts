import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { UserControllerService } from '../api/api/userController.service';
import { UserProfileResponse } from '../api/model/userProfileResponse';
import { UserAccountDto } from '../api/model/userAccountDto';
import { UserPenaltyDto } from '../api/model/userPenaltyDto';
import { UserSiteDto } from '../api/model/userSiteDto';
import { AuthService } from './auth.service';

// ALL PROTECTED ENDPOINTS IN THIS SERVICE MUST CALL setAuthHeader() TO ENSURE THE TOKEN IS INCLUDED IN THE REQUEST
@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private userControllerService: UserControllerService, private authService: AuthService) {}

  // Fetch user by id (matricule) from /api/users/{userid}
  // Uses the generated client method which maps to that endpoint.
  getUserById(userId: string): Observable<UserProfileResponse> {
    if (!userId) throw new Error('userId is required');
    this.setAuthHeader();
    return this.userControllerService.getUserByMatricule(userId).pipe(
      // Map the raw JSON to a strongly typed UserProfileResponse
      map((raw: any) => this.mapUserProfile(raw))
    );
  }

  // Fetch the current authenticated user's information from /api/users/me
  getCurrentUser(): Observable<UserProfileResponse> {
    this.setAuthHeader();
    return this.userControllerService.getCurrentUser().pipe(
      map((raw: any) => this.mapUserProfile(raw))
    );
  }

  // Fetch all users. Returns the generated controller model type so callers get a typed array.
  // Ensures an Authorization header is set from the AuthService token before calling the generated client.
  getAllUsers(): Observable<Array<UserProfileResponse>> {
    this.setAuthHeader();
    return this.userControllerService.getAllUsers();
  }

  // Ensures Authorization header is set from AuthService token before calling the generated client.
  private setAuthHeader(): void {
    const token = this.authService.getToken();
    if (token) {
      this.userControllerService.defaultHeaders = this.userControllerService.defaultHeaders.set('Authorization', `Bearer ${token}`);
    }
  }

  // Map the raw JSON to a UserProfileResponse
  // We need to remap as we're combining multiple nested objects (account, penalties, sites) into a single response for easier use in the UI.
  private mapUserProfile(raw: any): UserProfileResponse {
    return {
      matricule: raw.matricule,
      firstName: raw.firstName,
      lastName: raw.lastName,
      email: raw.email,
      birthDate: raw.birthDate,
      level: raw.level,
      isActive: raw.isActive,
      roleId: raw.roleId,
      account: raw.account as UserAccountDto,
      penalties: raw.penalties as UserPenaltyDto[],
      sites: raw.sites as UserSiteDto[]
    } as UserProfileResponse;
  }
}
