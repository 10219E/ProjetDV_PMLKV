import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MigrateUserControllerService } from '../api/api/migrateUserController.service';
import { User } from '../api/model/user';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class MigrationService {
  constructor(private migrateController: MigrateUserControllerService, private authService: AuthService) {}

  migrateToVip(userId: string): Observable<User> {
    this.setAuthHeader();
    return this.migrateController.migrateUser(userId);
  }

  // Normalized setAuthHeader method
  private setAuthHeader(): void {
    try {
      const token = this.authService.getToken();
      if (token && this.migrateController && this.migrateController.defaultHeaders && this.migrateController.defaultHeaders.set) {
        this.migrateController.defaultHeaders = this.migrateController.defaultHeaders.set('Authorization', `Bearer ${token}`);
      }
    } catch (e) {
      // ignore
    }
  }
}
