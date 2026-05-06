import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MatchPaymentControllerService } from '../api/api/matchPaymentController.service';
import { MatchPlayerControllerService } from '../api/api/matchPlayerController.service';
import { InvitesDto } from '../api/model/invitesDto';
import { AuthService } from './auth.service';
import { SessionService } from './session.service';
import {SimpleInviteDto, UserControllerService, MatchPlayerDto} from '../api';

@Injectable({ providedIn: 'root' })
export class InviteService {
  constructor(private userControllerService : UserControllerService, private matchPaymentService: MatchPaymentControllerService, private matchPlayerService: MatchPlayerControllerService, private auth: AuthService, private sessionService: SessionService) {}

  private _setAuthHeaderOnApiService(): void {
	try {
	  this.sessionService.setAuthHeader(this.matchPaymentService);
	} catch (e) {
	  // ignore
	}
  }

  private _setAuthHeaderOnMatchPlayerService(): void {
	try {
	  this.sessionService.setAuthHeader(this.matchPlayerService);
	} catch (e) {
	  // ignore
	}
  }

  // Fetch user by email from /api/users/invite/{email} using the new SimpleInviteDto
  getInviteByEmail(email: string): Observable<SimpleInviteDto> {
    if (!email) throw new Error('email is required');
    this._setAuthHeaderOnApiService();
    return this.userControllerService.getUserByEmail(email);
  }

  fetchPendingInvitesForUser(matricule: string): Observable<InvitesDto[]> {
	this._setAuthHeaderOnApiService();
	return this.matchPaymentService.getPendingWithDetailsPaymentsByUser(matricule).pipe(
	  catchError((err: any) => {
		if (err && (err.status === 404 || err.status === '404')) {
		  return of([] as InvitesDto[]);
		}
		return throwError(() => err);
	  })
	);
  }

  declineMatch(matchId: number, userId: string): Observable<{ [p: string]: string }> {
	this._setAuthHeaderOnMatchPlayerService();
	return this.matchPlayerService.declineMatch(matchId, userId);
  }

  getPlayersForMatch(matchId: number): Observable<string[]> {
    this._setAuthHeaderOnMatchPlayerService();
    return this.matchPlayerService.getPlayersForMatch(matchId) as unknown as Observable<string[]>;
  }
}
