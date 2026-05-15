import { Injectable } from '@angular/core';
import { Observable, forkJoin, of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { MatchPaymentControllerService, MatchPaymentDto } from '../api';

@Injectable({ providedIn: 'root' })
export class PayService {
  constructor(private matchPaymentService: MatchPaymentControllerService, private authService: AuthService) {}

  private setAuthHeader(): void {
    try {
      const token = this.authService.getToken();
      if (token && this.matchPaymentService && this.matchPaymentService.defaultHeaders && this.matchPaymentService.defaultHeaders.set) {
        this.matchPaymentService.defaultHeaders = this.matchPaymentService.defaultHeaders.set('Authorization', `Bearer ${token}`);
      }
    } catch (e) {
      // ignore
    }
  }


  createPayment(payment: MatchPaymentDto): Observable<any> {
	this.setAuthHeader();

	// client-side validation to avoid sending payloads that will fail DB constraints
	const validationError = this.validatePaymentDto(payment);
	if (validationError) {
	  // return an observable error so callers can handle it uniformly
	  return throwError(() => new Error(validationError));
	}

	// matchPaymentService.createPayment expects the generated DTO type
	return this.matchPaymentService.createPayment(payment);
  }

  // Update an existing payment (mark as cleared/refunded/etc.)
  updatePayment(paymentId: number, payment: MatchPaymentDto): Observable<any> {
	this.setAuthHeader();
	return this.matchPaymentService.updatePayment(paymentId, payment);
  }

  /**
   * Create organiser (cleared) payment and pending payments for invited users.
   * Returns an observable that completes when all requests finish.
   */
  createPaymentsForMatch(matchId: number, organiserMatricule: string, invites: string[], pricing: number): Observable<any[]> {
	const perPlayer = (pricing || 0) / 4.0;
	const calls: Observable<any>[] = [];

	const organiserPayment: MatchPaymentDto = {
	  matchId: matchId,
	  userMatricule: organiserMatricule,
	  amount: perPlayer,
	  // for 'clear' we can omit paymentDate and backend will set it; include if you prefer
	  status: 'clear',
	  paymentMethod: 'CARD'
	};

	// do NOT swallow errors here - let them propagate so caller can abort/rollback
	calls.push(this.createPayment(organiserPayment));

	(invites || []).forEach((u) => {
	  const p: MatchPaymentDto = {
		matchId: matchId,
		userMatricule: u,
		amount: perPlayer,
		status: 'pending'
	  };
	  calls.push(this.createPayment(p));
	});

	if (calls.length === 0) return of([]);
	return forkJoin(calls);
  }

  // Validate payment DTO according to backend rules to prevent avoidable server errors
  private validatePaymentDto(payment: MatchPaymentDto): string | null {
	if (!payment) return 'Payment is required';
	if (payment.amount == null) return 'Payment amount is required';
	if (!payment.status) return 'Payment status is required';

	// allowed statuses (same regex as backend)
	if (!/^(clear|pending|cancelled|failed|refunded)$/.test(payment.status)) {
	  return `Invalid payment status: ${payment.status}`;
	}

	// if method provided, validate values
	if (payment.paymentMethod && !/^(CARD|COUNTER)$/.test(payment.paymentMethod)) {
	  return `Invalid payment method: ${payment.paymentMethod}`;
	}

	// if status requires payment details
	if (payment.status === 'clear' || payment.status === 'refunded') {
	  if (!payment.paymentMethod) return 'Payment method is required for cleared or refunded payments';
	  // paymentDate may be omitted (backend will set it), so don't require here
	}

	// if paymentDate is present, paymentMethod must be present too
	if (payment.paymentDate && !payment.paymentMethod) {
	  return 'Payment method must be provided when payment date is present';
	}

	return null;
  }

  // Cancel a payment by setting its status to 'cancelled'
  cancelPayment(paymentId: number): Observable<any> {
	this.setAuthHeader();
	const paymentDto: MatchPaymentDto = {
	  status: 'cancelled'
	};
	return this.updatePayment(paymentId, paymentDto);
  }
}
