import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { FieldControllerService } from '../api/api/fieldController.service';
import { AuthService } from './auth.service';
import {FieldDto} from '../api';

@Injectable({ providedIn: 'root' })
export class FieldService {

  constructor(private fieldService: FieldControllerService, private authService: AuthService) {
  }

  // Public helper to set Authorization header on generated API services when needed
  public setAuthHeader(service: any): void {
    try {
      const token = this.authService.getToken();
      if (token && service && service.defaultHeaders && service.defaultHeaders.set) {
        service.defaultHeaders = service.defaultHeaders.set('Authorization', `Bearer ${token}`);
      }
    } catch (e) {
      // ignore
    }
  }

  // Fetch fields for a site (centralized header setting + error handling)
  // NOTE: use the "active" endpoint so callers get only active fields by default
  public fetchFieldsBySite(siteId: number): Observable<any[]> {
    try {
      this.setAuthHeader(this.fieldService);
    } catch (e) {
      // ignore
    }
    // Use getActiveFieldsBySite to avoid returning inactive fields to UI flows
    return this.fieldService.getActiveFieldsBySite(siteId).pipe(
      catchError((err) => {
        console.error('SessionService.fetchFieldsBySite (active) error', err);
        return of([]);
      })
    );
  }

  // Get ALL fields
  public fetchAllFields(): Observable<FieldDto[]> {
    try {
      this.setAuthHeader(this.fieldService);
    } catch (e) {
      // ignore
    }

    return this.fieldService.getAllFields().pipe(
      catchError((err) => {
        console.error('SessionService.getAllFields error', err);
        return of([]);
      })
    );
  }
}
