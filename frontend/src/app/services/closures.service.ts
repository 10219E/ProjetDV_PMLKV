import { Injectable } from '@angular/core';
import { Observable, of, combineLatest } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { SiteControllerService } from '../api/api/siteController.service';
import { FieldControllerService } from '../api/api/fieldController.service';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class ClosuresService {
  constructor(
    private siteController: SiteControllerService,
    private fieldController: FieldControllerService,
    private authService: AuthService
  ) {}

  /** Return a Set of YYYY-MM-DD strings for site closures */
  getClosuresForSite(siteId: number): Observable<Set<string>> {
    if (siteId === null || siteId === undefined) return of(new Set<string>());
    this.setAuthHeader();

    // Combine both API calls into a single observable
    return combineLatest([
      this.siteController.getClosuresForSite(siteId),
      this.siteController.getClosuresForSite(0)
    ]).pipe(
      map(([siteRows, allSitesRows]) => {
        const set = new Set<string>();

        // Process site-specific closures
        (siteRows || []).forEach(r => {
          try {
            // More robust type assertion
            const closure = r as { closureDate?: string };
            const iso = closure.closureDate;
            if (iso) set.add(this.normalizeIso(iso));
          } catch (e) {
            // ignore malformed rows
          }
        });

        // Process all-sites closures
        (allSitesRows || []).forEach(r => {
          try {
            const closureWithFlag = { ...r, isForAllSites: true };
            // More robust type assertion
            const closure = closureWithFlag as { closureDate?: string };
            const iso = closure.closureDate;
            if (iso && closureWithFlag) set.add(this.normalizeIso(iso));
          } catch (e) {
            // ignore malformed rows
          }
        });

        return set;
      }),
      catchError(err => {
        console.error('ClosuresService.getClosuresForSite error', err);
        return of(new Set<string>());
      })
    );
  }

  /** Return a Set of YYYY-MM-DD strings for a field maintenance range (inclusive) */
  getMaintenanceForField(fieldId: number): Observable<Set<string>> {
    if (fieldId === null || fieldId === undefined) return of(new Set<string>());
    this.setAuthHeader();
    return this.fieldController.getFieldMaintenanceById(fieldId).pipe(
      map((f: any) => {
        const set = new Set<string>();
        if (!f) return set;
        const from = f?.maintenanceFromDate;
        const to = f?.maintenanceToDate;
        if (!from || !to) return set;
        this.expandRangeIntoSet(from, to, set);
        return set;
      }),
      catchError(err => {
        console.error('ClosuresService.getMaintenanceForField error', err);
        return of(new Set<string>());
      })
    );
  }

  /** Combined helper: closures for site + maintenance for field (if provided) */
  getBlockedDates(siteId?: number | null, fieldId?: number | null): Observable<Set<string>> {
    if (!siteId && !fieldId) return of(new Set<string>());
    const closures$ = siteId ? this.getClosuresForSite(siteId) : of(new Set<string>());
    const maint$ = fieldId ? this.getMaintenanceForField(fieldId) : of(new Set<string>());
    return combineLatest([closures$, maint$]).pipe(
      map(([c, m]) => {
        const union = new Set<string>(c);
        for (const d of m) union.add(d);
        return union;
      }),
      catchError(err => {
        console.error('ClosuresService.getBlockedDates error', err);
        return of(new Set<string>());
      })
    );
  }

  // Helper to normalize date-like strings into YYYY-MM-DD (assumes ISO-like input)
  private normalizeIso(d: string): string {
    if (/^\d{4}-\d{2}-\d{2}$/.test(d)) return d;
    try {
      // Parse leniently but format as local YYYY-MM-DD to avoid UTC shifts
      const p = new Date(d);
      return this.toLocalYMD(p);
    } catch (e) {
      return String(d);
    }
  }

  private expandRangeIntoSet(from: string, to: string, set: Set<string>) {
    try {
      // parse YYYY-MM-DD into local Date to avoid timezone shifts
      const parseLocal = (s: string) => {
        if (!s) return null;
        const m = s.match(/^(\d{4})-(\d{2})-(\d{2})$/);
        if (m) return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
        const p = new Date(s);
        if (!isNaN(p.getTime())) return new Date(p.getFullYear(), p.getMonth(), p.getDate());
        return null;
      };
      const start = parseLocal(from);
      const end = parseLocal(to);
      if (!start || !end) return;
      let cur = new Date(start.getFullYear(), start.getMonth(), start.getDate());
      const last = new Date(end.getFullYear(), end.getMonth(), end.getDate());
      while (cur <= last) {
        set.add(this.toLocalYMD(cur));
        cur = new Date(cur.getFullYear(), cur.getMonth(), cur.getDate() + 1);
      }
    } catch (e) {
      // ignore
    }
  }

  private toLocalYMD(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${dd}`;
  }

  // Normalized setAuthHeader method
  private setAuthHeader(): void {
    try {
      const token = this.authService.getToken();
      if (token && this.siteController && this.siteController.defaultHeaders && this.siteController.defaultHeaders.set) {
        this.siteController.defaultHeaders = this.siteController.defaultHeaders.set('Authorization', `Bearer ${token}`);
      }
      if (token && this.fieldController && this.fieldController.defaultHeaders && this.fieldController.defaultHeaders.set) {
        this.fieldController.defaultHeaders = this.fieldController.defaultHeaders.set('Authorization', `Bearer ${token}`);
      }
    } catch (e) {
      // ignore
    }
  }
}



