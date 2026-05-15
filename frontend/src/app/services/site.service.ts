import { Injectable } from '@angular/core';
import { SiteControllerService } from '../api';
import { Observable } from 'rxjs';
import { Site } from '../api';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class SiteService {

  constructor(
    private siteController: SiteControllerService,
    private authService: AuthService
  ) { }

  private setAuthHeader(): void {
    try {
      const token = this.authService.getToken();
      if (token && this.siteController && this.siteController.defaultHeaders && this.siteController.defaultHeaders.set) {
        this.siteController.defaultHeaders = this.siteController.defaultHeaders.set('Authorization', `Bearer ${token}`);
      }
    } catch (e) {
      // ignore
    }
  }

  getAllSites(active?: boolean): Observable<Site[]> {
    this.setAuthHeader();
    return this.siteController.getAllSites(active);
  }
}
