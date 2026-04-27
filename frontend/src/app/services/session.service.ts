import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { FieldControllerService } from '../api/api/fieldController.service';
import { SiteControllerService } from '../api/api/siteController.service';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class SessionService {
  // Cache per-site: store original normalized sessions so we can hide/restore them when date is cleared/set
  private siteCache = new Map<number, { original: any[] | null }>();

  constructor(private fieldService: FieldControllerService, private siteService: SiteControllerService, private authService: AuthService) {}

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
  public fetchFieldsBySite(siteId: number): Observable<any[]> {
    try {
      this.setAuthHeader(this.fieldService);
    } catch (e) {
      // ignore
    }
    return this.fieldService.getFieldsBySite(siteId).pipe(
      catchError((err) => {
        console.error('SessionService.fetchFieldsBySite error', err);
        return of([]);
      })
    );
  }

  // Load sessions for a site, normalize them for UI and apply the "hide when no date" behavior.
  public loadSessionsForSite(siteId: number, matchDate?: string | null): Observable<any[]> {
    try { this.setAuthHeader(this.siteService); } catch (e) { }
    return this.siteService.getSiteById(siteId).pipe(
      map((site: any) => (site && site.sessions) ? (site.sessions as any[]) : []),
      map((rawSessions: any[]) => rawSessions.map(s => this.normalizeSessionForUi(s))),
      map((normalized: any[]) => {
        const cached = this.siteCache.get(siteId);
        if (!matchDate) {
          // hide sessions: cache original if not cached and return empty
          if (!cached) {
            this.siteCache.set(siteId, { original: normalized });
          }
          return [];
        }
        // matchDate provided: if we have cached original sessions, restore and clear cache (to mirror previous behavior)
        if (cached && cached.original && cached.original.length > 0) {
          const orig = cached.original;
          this.siteCache.delete(siteId);
          return [...orig];
        }
        // otherwise return freshly fetched normalized sessions
        return normalized;
      }),
      catchError((err) => {
        console.error('SessionService.loadSessionsForSite error', err);
        return of([]);
      })
    );
  }

  // Use a provided site object (for cases where the user profile included site.sessions)
  public loadSessionsFromSiteObject(site: any, matchDate?: string | null): Observable<any[]> {
    if (!site) return of([]);
    const siteId = Number(site.siteId);
    const raw = site.sessions || [];
    const normalized = (raw as any[]).map(s => this.normalizeSessionForUi(s));
    const cached = this.siteCache.get(siteId);
    if (!matchDate) {
      if (!cached) this.siteCache.set(siteId, { original: normalized });
      return of([]);
    }
    if (cached && cached.original && cached.original.length > 0) {
      const orig = cached.original;
      this.siteCache.delete(siteId);
      return of([...orig]);
    }
    return of(normalized);
  }

  // Convenience wrapper used when date input changes
  public onDateChange(siteId: number, matchDate?: string | null): Observable<any[]> {
    if (!siteId) return of([]);
    return this.loadSessionsForSite(siteId, matchDate);
  }

  // Find a session by its _start label in an array of normalized sessions
  public findSessionInArray(sessions: any[] | undefined | null, startHHMM: string): any | null {
    if (!sessions || !startHHMM) return null;
    return (sessions || []).find(s => s && s._start === startHHMM) || null;
  }

  // Convert raw session object into UI-friendly shape (_start/_end/label)
  private normalizeSessionForUi(s: any): any {
    const out: any = { ...s };
    let startTimeStr = s.start_time || null;
    let endTimeStr = s.end_time || null;
    if (s.startedAt && !startTimeStr) {
      const startDate = new Date(s.startedAt);
      if (!isNaN(startDate.getTime())) {
        startTimeStr = this.formatTimeHHMM(startDate);
      }
    }
    if (s.endedAt && !endTimeStr) {
      const endDate = new Date(s.endedAt);
      if (!isNaN(endDate.getTime())) {
        endTimeStr = this.formatTimeHHMM(endDate);
      }
    }
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
}


