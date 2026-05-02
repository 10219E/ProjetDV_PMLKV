import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MatchPaymentControllerService } from '../api/api/matchPaymentController.service';
import { InvitesDto } from '../api/model/invitesDto';
import { AuthService } from './auth.service';
import { SessionService } from './session.service';

@Injectable({ providedIn: 'root' })
export class InviteService {
  constructor(private matchPaymentService: MatchPaymentControllerService, private auth: AuthService, private sessionService: SessionService) {}

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


