import { Injectable } from '@angular/core';
import {Observable, of} from 'rxjs';
import { MatchControllerService } from '../api/api/matchController.service';
import {MatchCreationControllerService} from '../api';
import {MatchPlayerControllerService, MatchPlayerDto, MatchPlayerSiteFieldDto} from '../api';
import { MatchDto } from '../api/model/matchDto';
import { MatchSiteFieldDto} from '../api/model/matchSiteFieldDto';
import { MatchCreationDto } from '../api/model/matchCreationDto';
import { DeclinedPlayersDto } from '../api/model/declinedPlayersDto';
import { AuthService } from './auth.service';
import {catchError, map} from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class MatchService {
  constructor(
    private matchControllerService: MatchControllerService,
    private matchCreationControllerService: MatchCreationControllerService,
    private authService: AuthService,
    private matchPlayerControllerService: MatchPlayerControllerService
  ) {}

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

  getAvailablePublicMatches(userId: string): Observable<MatchSiteFieldDto[]> {
    if (!userId) {
      throw new Error('userId is required');
    }
    this.setAuthHeader();
    return this.matchControllerService.getAvailablePublicMatches(userId).pipe(
      map((data: MatchSiteFieldDto[]) => Array.isArray(data) ? data : []),
      catchError((error) => {
        if (error.status === 404) return of([]);
        throw error;
      })
    );
  }

  // Join Public or Update private match : update player status
  joinPublicMatchOrUpdatePrivate(matchId: number, userMatricule: string, status: string = 'approved'): Observable<MatchPlayerDto> {
    this.setAuthHeader();

    // Create a MatchPlayerDto to update the player status
    const matchPlayerDto: MatchPlayerDto = {
      match: { matchId: matchId },
      userMatricule: userMatricule,
      status: status
    };

    // Update the match player status and return the Observable
    return this.matchPlayerControllerService.updateMatchPlayer(matchId, matchPlayerDto).pipe(
      catchError((error) => {
        console.error(`Error joining match ${matchId} for user ${userMatricule}:`, error);
        throw error;
      })
    );
  }

  getMyMatches(matricule: string): Observable<MatchPlayerSiteFieldDto[]> {
    this.setAuthHeader();
    return this.matchPlayerControllerService.getMyMatches(matricule).pipe(
      map((data: MatchPlayerSiteFieldDto[]) => Array.isArray(data) ? data : []),
      catchError((error) => {
        if (error.status === 404) return of([]);
        throw error;
      })
    );
  }

  // Add this method to the MatchService class
  createMatch(matchCreationDto: MatchCreationDto): Observable<{ [key: string]: object; }> {
    this.setAuthHeader();
    return this.matchCreationControllerService.create(matchCreationDto).pipe(
      catchError((error) => {
        console.error('Error creating match:', error);
        throw error;
      })
    );
  }

  // Check if user is organiser of any matches with declined players
  getOrganiserMatchesWithDeclinedPlayers(organiserId: string): Observable<DeclinedPlayersDto[]> {
    this.setAuthHeader();
    return this.matchPlayerControllerService.hasDeclinedMatches(organiserId).pipe(
      map((data: DeclinedPlayersDto[]) => Array.isArray(data) ? data : []),
      catchError((error) => {
        if (error.status === 404) return of([]);
        throw error;
      })
    );
  }

  getCollidingMatches(userId: string, matchDate: string, startTime: string): Observable<boolean> {
    this.setAuthHeader();
    return this.matchControllerService.getCollidingMatches(userId, matchDate, startTime).pipe(
      catchError((error) => {
        console.error('Error fetching colliding matches:', error);
        return of(false);
      })
    );
  }

  // Normalized setAuthHeader method
  private setAuthHeader(): void {
    try {
      const token = this.authService.getToken();
      if (token && this.matchControllerService && this.matchControllerService.defaultHeaders && this.matchControllerService.defaultHeaders.set) {
        this.matchControllerService.defaultHeaders = this.matchControllerService.defaultHeaders.set('Authorization', `Bearer ${token}`);
      }
      if (token && this.matchPlayerControllerService && this.matchPlayerControllerService.defaultHeaders && this.matchPlayerControllerService.defaultHeaders.set) {
        this.matchPlayerControllerService.defaultHeaders = this.matchPlayerControllerService.defaultHeaders.set('Authorization', `Bearer ${token}`);
      }
    } catch (e) {
      // ignore
    }
  }
}
