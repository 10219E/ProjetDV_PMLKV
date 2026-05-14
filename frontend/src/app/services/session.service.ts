import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { FieldControllerService } from '../api/api/fieldController.service';
import { SiteControllerService } from '../api/api/siteController.service';
import { AuthService } from './auth.service';
import { AvailabilityControllerService } from '../api/api/availabilityController.service';
import { AvailabilityDto, SessionDto } from '../api';

@Injectable({ providedIn: 'root' })
export class SessionService {
  // Cache per-site: store original normalized sessions so we can hide/restore them when date is cleared/set
  private siteCache = new Map<number, { original: any[] | null }>();

  // Cache for available dates to avoid repeated API calls
  private datesCache = new Map<string, { dates: string[] | null, timestamp: number }>();

  constructor(
    private fieldService: FieldControllerService,
    private siteService: SiteControllerService,
    private authService: AuthService,
    private availabilityService: AvailabilityControllerService
  ) {}

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

  // Load sessions for a field, normalize them for UI and apply the "hide when no date" behavior.
  public loadSessionsForField(siteId: number, fieldId: number, matchDate?: string | null): Observable<any[]> {
    try { this.setAuthHeader(this.availabilityService); } catch (e) { }

    if (!matchDate) {
      // If no date is provided, hide sessions by returning empty array
      const cached = this.siteCache.get(siteId);
      if (!cached) {
        // If not cached, we'll need to fetch sessions when date is set
        this.siteCache.set(siteId, { original: null });
      }
      return of([]);
    }

    // When date is provided, check if we have cached sessions
    const cached = this.siteCache.get(siteId);
    if (cached && cached.original) {
      // If we have cached sessions, return them and clear the cache
      const orig = cached.original;
      this.siteCache.delete(siteId);
      return of([...orig]);
    }

    // Otherwise, fetch fresh sessions from the availability service
    return this.availabilityService.getAvailableSessions(siteId, fieldId, matchDate).pipe(
      map((availability: AvailabilityDto) => availability.availableSessions || []),
      map((sessions: SessionDto[]) => sessions.map(s => this.normalizeSessionForUi(s))),
      map((normalized: any[]) => {
        // Cache the normalized sessions for potential future use
        this.siteCache.set(siteId, { original: normalized });
        return normalized;
      }),
      catchError((err) => {
        console.error('SessionService.loadSessionsForSite error', err);
        return of([]);
      })
    );
  }

  // Get available dates for a site and field based on user role
  public getAvailableDates(siteId: number, fieldId: number, startDate: string, roleId: number): Observable<string[]> {
    try { this.setAuthHeader(this.availabilityService); } catch (e) { }

    // Create a cache key based on the parameters
    const cacheKey = `${siteId}-${fieldId}-${startDate}-${roleId}`;

    // Check if we have a valid cached response (less than 5 minutes old)
    const cached = this.datesCache.get(cacheKey);
    if (cached && (Date.now() - cached.timestamp) < 300000) {
      return of(cached.dates || []);
    }

    return this.availabilityService.getAvailableDates(siteId, fieldId, startDate, roleId).pipe(
      map((dates: string[]) => {
        // Cache the response with a timestamp
        this.datesCache.set(cacheKey, { dates, timestamp: Date.now() });
        return dates;
      }),
      catchError((err) => {
        console.error('SessionService.getAvailableDates error', err);
        return of([]);
      })
    );
  }

  // Convenience wrapper used when date input changes
  public onDateChange(siteId: number, fieldId: number, matchDate?: string | null): Observable<any[]> {
    if (!siteId || !fieldId) return of([]);
    return this.loadSessionsForField(siteId, fieldId, matchDate);
  }

  // Convert raw session object into UI-friendly shape (_start/_end/label)
  private normalizeSessionForUi(s: SessionDto): any {
    const out: any = { ...s };
    let startTimeStr = s.startTime?.toString() || null;
    let endTimeStr = s.endTime?.toString() || null;

    if (startTimeStr && startTimeStr.includes(':')) {
      startTimeStr = startTimeStr.split(':').slice(0, 2).join(':');
    }
    if (endTimeStr && endTimeStr.includes(':')) {
      endTimeStr = endTimeStr.split(':').slice(0, 2).join(':');
    }

    out._start = startTimeStr;
    out._end = endTimeStr;
    out.label = out._start ? `${out._start}` : `Slot ${out.match_set_id ?? out.sessionId ?? ''}`;
    return out;
  }

  private formatTimeHHMM(d: Date): string {
    const hh = d.getHours().toString().padStart(2, '0');
    const mm = d.getMinutes().toString().padStart(2, '0');
    return `${hh}:${mm}`;
  }

  // Add a clear method to clear the caches so that form submission forces fetching fresh availabilities
  public clearCaches(): void {
    this.datesCache.clear();
    this.siteCache.clear();
  }
}
