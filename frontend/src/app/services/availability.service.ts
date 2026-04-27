import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { MatchService } from './match.service';

@Injectable({ providedIn: 'root' })
export class AvailabilityService {
  constructor(private matchService: MatchService) {}

  /**
   * Filter normalized sessions (with _start) by removing those that conflict with existing matches
   * on the same site/date and (optionally) for the same field.
   * @param siteId site id
   * @param sessions normalized sessions array (each item expected to have _start)
   * @param matchDate yyyy-mm-dd string (if falsy, sessions are returned unchanged)
   * @param fieldId optional selected field id to only consider matches for that field or matches without a field
   */
  filterSessionsByAvailability(siteId: number, sessions: any[], matchDate?: string | null, fieldId?: number | null): Observable<any[]> {
	if (!siteId) return of(sessions || []);
	if (!matchDate) return of(sessions || []);

	return this.matchService.getMatchesBySite(siteId).pipe(
	  map((matches: any[]) => {
		const taken = new Set<string>();
		(matches || []).forEach(m => {
		  if (!m) return;
		  if (m.matchDate !== matchDate) return;
		  const hhmm = this.matchStartToHHMM(m.startTime);
		  if (!hhmm) return;
		  // Only count as taken if match is not field-specific OR matches the selected field
		  if (!m.fieldId || (fieldId !== null && fieldId !== undefined && Number(m.fieldId) === Number(fieldId))) {
			taken.add(hhmm);
		  }
		});
		return (sessions || []).filter(s => !taken.has(s._start));
	  }),
	  catchError((err) => {
		console.error('AvailabilityService.filterSessionsByAvailability error', err);
		return of(sessions || []);
	  })
	);
  }

  // Convert MatchDto.startTime (LocalTime object or string like '15:00:00') into 'HH:MM'
  private matchStartToHHMM(start: any): string | null {
	if (!start) return null;
	try {
	  if (typeof start === 'string') {
		if (!start.includes(':')) return null;
		return start.split(':').slice(0, 2).join(':');
	  }
	  // assume LocalTime object {hour, minute, second}
	  const h = start.hour ?? start['Hour'] ?? null;
	  const m = start.minute ?? start['Minute'] ?? 0;
	  if (h === null || h === undefined) return null;
	  return `${this.padNumber(h)}:${this.padNumber(m)}`;
	} catch (e) {
	  return null;
	}
  }

  private padNumber(n: number): string {
	return (n ?? 0).toString().padStart(2, '0');
  }
}

