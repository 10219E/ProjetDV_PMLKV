import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { UserControllerService } from '../api/api/userController.service';
import { UserProfileDto } from '../api/model/userProfileDto';
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
  getUserById(userId: string): Observable<UserProfileDto> {
    if (!userId) throw new Error('userId is required');
    this.setAuthHeader();
    return this.userControllerService.getUserByIdentifier(userId).pipe(
      // Map the raw JSON to a strongly typed UserProfileDto
      map((raw: any) => this.mapUserProfile(raw))
    );
  }

  // Fetch user by email from /api/users/email/{email}
  getUserByEmail(email: string): Observable<UserProfileDto> {
    if (!email) throw new Error('email is required');
    this.setAuthHeader();
    return this.userControllerService.getUserByIdentifier(email).pipe(
      map((raw: any) => this.mapUserProfile(raw))
    );
  }

  // Fetch the current authenticated user's information from /api/users/me
  getCurrentUser(): Observable<UserProfileDto> {
    this.setAuthHeader();
    return this.userControllerService.getCurrentUser().pipe(
      map((raw: any) => this.mapUserProfile(raw))
    );
  }

  // Update user details in the backend using the new PATCH endpoint
  updateUserInBackend(userId: string, updates: any): Observable<UserProfileDto> {
    if (!userId) throw new Error('userId is required');
    this.setAuthHeader();
    // Using the generated client method which should now support the PATCH endpoint
    return this.userControllerService.updateUser(userId, updates).pipe(
      map((updated: any) => this.mapUserProfile(updated))
    );
  }

  // Update user penalty and account information -- ONLY FOR USERS WITH DEBT
  updateUserPenaltyAndAccount(userId: string, penaltyId: number, amount: number): Observable<void> {
    const body = { penaltyId, amount };
    this.setAuthHeader();
    return this.userControllerService.updateUserPenaltyAndAccount(userId, body);
  }

  // Fetch all users. Returns the generated controller model type so callers get a typed array.
  // Ensures an Authorization header is set from the AuthService token before calling the generated client.
  getAllUsers(): Observable<Array<UserProfileDto>> {
    this.setAuthHeader();
    return this.userControllerService.getAllUsers();
  }

  getUsersForSite(siteId: number): Observable<Array<UserProfileDto>> {
    this.setAuthHeader();
    return this.userControllerService.getUsersForSite(siteId);
  }

  // Normalized setAuthHeader method
  private setAuthHeader(): void {
    try {
      const token = this.authService.getToken();
      if (token && this.userControllerService && this.userControllerService.defaultHeaders && this.userControllerService.defaultHeaders.set) {
        this.userControllerService.defaultHeaders = this.userControllerService.defaultHeaders.set('Authorization', `Bearer ${token}`);
      }
    } catch (e) {
      // ignore
    }
  }

  // Map the raw JSON to a UserProfileDto
  // We need to remap as we're combining multiple nested objects (account, penalties, sites) into a single response for easier use in the UI.
  private mapUserProfile(raw: any): UserProfileDto {
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
    } as UserProfileDto;
  }
}
