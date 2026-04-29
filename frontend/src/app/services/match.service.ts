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

  // Fetch matches by type: calls GET /api/matches/type/{type}
  getMatchesByType(type: string): Observable<Array<MatchDto>> {
	if (type === null || type === undefined) {
	  throw new Error('type is required');
	}
	this.setAuthHeader();
	return this.matchControllerService.getByType(type);
  }

  // Fetch matches by type and status: calls GET /api/matches/type/{type}/status/{status}
  getMatchesByTypeAndStatus(type: string, status: string): Observable<Array<MatchDto>> {
	if (type === null || type === undefined) {
	  throw new Error('type is required');
	}
	if (status === null || status === undefined) {
	  throw new Error('status is required');
	}
	this.setAuthHeader();
	return this.matchControllerService.getByTypeAndStatus(type, status);
  }

  // Ensure Authorization header is set on the generated client from AuthService token
  private setAuthHeader(): void {
	const token = this.authService.getToken();
	if (token) {
	  this.matchControllerService.defaultHeaders = this.matchControllerService.defaultHeaders.set('Authorization', `Bearer ${token}`);
	}
  }
}


