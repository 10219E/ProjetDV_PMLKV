import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MatchPaymentControllerService } from '../api/api/matchPaymentController.service';
import { InvitesDto } from '../api/model/invitesDto';
import { AuthService } from './auth.service';
import { SessionService } from './session.service';
import {SimpleInviteDto, UserControllerService} from '../api';

@Injectable({ providedIn: 'root' })
export class InviteService {
  constructor(private userControllerService : UserControllerService, private matchPaymentService: MatchPaymentControllerService, private auth: AuthService, private sessionService: SessionService) {}

  private _setAuthHeaderOnApiService(): void {
	try {
	  this.sessionService.setAuthHeader(this.matchPaymentService);
	} catch (e) {
	  const token = this.auth.getToken();
	  if (token && this.matchPaymentService && this.matchPaymentService.defaultHeaders && this.matchPaymentService.defaultHeaders.set) {
		this.matchPaymentService.defaultHeaders = this.matchPaymentService.defaultHeaders.set('Authorization', `Bearer ${token}`);
	  }
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
}


