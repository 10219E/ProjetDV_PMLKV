import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MatchControllerService } from '../api/api/matchController.service';
import { MatchDto } from '../api/model/matchDto';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class MatchService {
  constructor(private matchControllerService: MatchControllerService, private authService: AuthService) {}

  // Fetch all matches for a given site using the generated API client.
  // This calls GET /api/matches/site/{siteId}
  getMatchesBySite(siteId: number): Observable<Array<MatchDto>> {
	if (siteId === null || siteId === undefined) {
	  throw new Error('siteId is required');
	}
	this.setAuthHeader();
	return this.matchControllerService.getBySite(siteId);
  }

  // Ensure Authorization header is set on the generated client from AuthService token
  private setAuthHeader(): void {
	const token = this.authService.getToken();
	if (token) {
	  this.matchControllerService.defaultHeaders = this.matchControllerService.defaultHeaders.set('Authorization', `Bearer ${token}`);
	}
  }
}


