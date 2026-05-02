import { Injectable } from '@angular/core';
import {Observable, of} from 'rxjs';
import { MatchControllerService } from '../api/api/matchController.service';
import {MatchPlayerControllerService, MatchPlayerDto} from '../api';
import { MatchDto } from '../api/model/matchDto';
import { AuthService } from './auth.service';
import {catchError, map} from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class MatchService {
  constructor(private matchControllerService: MatchControllerService, private authService: AuthService, private matchPlayerControllerService: MatchPlayerControllerService) {}

  // Fetch all matches for a given site using the generated API client.
  // This calls GET /api/matches/site/{siteId}
  getMatchesBySite(siteId: number): Observable<MatchDto[]> {
    this.setAuthHeader();
    return this.matchControllerService.getBySite(siteId).pipe(
      map((matches: MatchDto[]) => {
        // If no matches exist for the site, return an empty array
        if (!matches || matches.length === 0) {
          console.log(`No matches found for site ${siteId}`);
          return [];
        }
        return matches;
      }),
      catchError((error) => {
        // If we get a 404 error, assume no matches exist for this site
        if (error.status === 404) {
          console.log(`No matches found for site ${siteId} (404 error)`);
          return of([]);
        }
        // For other errors, rethrow the error
        console.error(`Error fetching matches for site ${siteId}:`, error);
        throw error;
      })
    );
  }

  getAvailablePublicMatches(userId: string): Observable<Array<MatchDto>> {
    if (!userId) {
      throw new Error('userId is required');
    }
    this.setAuthHeader();
    return this.matchControllerService.getAvailablePublicMatches(userId).pipe(
      map((data: any) => Array.isArray(data) ? data : []),
      catchError((error) => {
        if (error.status === 404) return of([]);
        throw error;
      })
    );
  }

  // Fetch matches by type: calls GET /api/matches/type/{type}
  getMatchesByType(type: string): Observable<MatchDto[]> {
    this.setAuthHeader();
    return this.matchControllerService.getByType(type).pipe(
      map((matches: MatchDto[]) => {
        // If no matches exist for the type, return an empty array
        if (!matches || matches.length === 0) {
          console.log(`No matches found for type ${type}`);
          return [];
        }
        return matches;
      }),
      catchError((error) => {
        // If we get a 404 error, assume no matches exist for this type
        if (error.status === 404) {
          console.log(`No matches found for type ${type} (404 error)`);
          return of([]);
        }
        // For other errors, rethrow the error
        console.error(`Error fetching matches for type ${type}:`, error);
        throw error;
      })
    );
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

  // Join Public match and update player status
  /**WATCHOUT HERE, PRIVATE MATCHES PLAYERS ARE INSERTED BY THE SERVICE, SO THIS FUNCTION IS ONLY FOR PUBLIC MATCHES (EXCEPT IF I HAVE TIME TO IMPLEMENT DECLINE ON PRIVATE INVITES)*/
  joinPublicMatch(matchId: number, userMatricule: string, playerRoleId?: string): Observable<MatchPlayerDto> {

    this.setAuthHeader();

    // Create a MatchPlayerDto to update the player status
    const matchPlayerDto: MatchPlayerDto = {
      match: { matchId: matchId },
      userMatricule: userMatricule,
      status: 'approved',
      playerRole: playerRoleId ? playerRoleId : undefined
    };

 // Update the match player status and return the Observable
    return this.matchPlayerControllerService.updateMatchPlayer(matchId, matchPlayerDto).pipe(
      catchError((error) => {
        console.error(`Error joining match ${matchId} for user ${userMatricule}:`, error);
        throw error;
      })
    );
  }


  getMyMatches(matricule: string): Observable<MatchPlayerDto[]> {
    this.setAuthHeader();
    return this.matchPlayerControllerService.getMyMatches(matricule).pipe(
      map((data: any) => Array.isArray(data) ? data : []),
      catchError((error) => {
        if (error.status === 404) return of([]);
        throw error;
      })
    );
  }

  // Ensure Authorization header is set on the generated client from AuthService token
  private setAuthHeader(): void {
	const token = this.authService.getToken();
	if (token) {
	  this.matchControllerService.defaultHeaders = this.matchControllerService.defaultHeaders.set('Authorization', `Bearer ${token}`);
	  this.matchPlayerControllerService.defaultHeaders = this.matchPlayerControllerService.defaultHeaders.set('Authorization', `Bearer ${token}`);
	}
  }
}


