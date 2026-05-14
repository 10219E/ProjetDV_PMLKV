import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';
import { PayFormComponent } from '../../pay-form/pay-form';
import { PayService } from '../../../../services/pay.service';
import { InviteService } from '../../../../services/invite.service';
import { AuthService } from '../../../../services/auth.service';
import { UserService } from '../../../../services/user.service';
import { MatchService } from '../../../../services/match.service';
import { take } from 'rxjs/operators';
import { MatchPaymentDto } from '../../../../api/model/matchPaymentDto';
import { InvitesDto } from '../../../../api/model/invitesDto';

@Component({
  selector: 'app-invite-payments',
  standalone: true,
  imports: [CommonModule, MatIconModule, NavMenu, HomeAccountHeader, PayFormComponent],
  templateUrl: './invite-payments.html',
  styleUrls: ['./invite-payments.css']
})
export class InvitePaymentsPage implements OnInit {
  payments: InvitesDto[] = [];
  loading = false;
  error: string | null = null;

  // payment UI state
  showPayForm = false;
  payAmount = 0;
  selectedPayment: InvitesDto | null = null;

  // decline confirmation state
  showDeclineConfirm = false;
  paymentToDecline: InvitesDto | null = null;

  // success popup state
  showSuccessDialog = false;
  popupMessage: string | null = null;

  constructor(
	private route: ActivatedRoute,
	private router: Router,
	private payService: PayService,
	private inviteService: InviteService,
	private auth: AuthService,
	private userService: UserService,
	private matchService: MatchService,
	private cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
	const userId = this.route.snapshot.paramMap.get('userId');
	if (!userId) {
	  // fallback: try current user
	  this.payForCurrentUser();
	  return;
	}
	this.loadPending(userId);
  }

  private payForCurrentUser() {
	// try to resolve current matricule when route param is missing
	try {
	  this.loading = true;
	  this.auth.getToken();
	  // PayService expects a matricule; attempt to read from protected API via user service is out of scope here
	  // Redirect to /home so authGuard will resolve the proper user route
	  this.router.navigate(['/home']).then(() => {}).catch(() => {});
	} finally {
	  this.loading = false;
	}
  }

  private loadPending(matricule: string) {
	this.loading = true;
	this.error = null;
	this.inviteService.fetchPendingInvitesForUser(matricule).subscribe({
	  next: (data: any) => {
		// API returns an array of InvitesDto
		this.payments = Array.isArray(data) ? data : [];
		this.loading = false;
		this.cd.detectChanges();
	  },
	  error: (err: any) => {
		console.error('Failed to load pending invites', err);
		this.error = err?.message || 'Erreur lors du chargement des invitations.';
		this.payments = [];
		this.loading = false;
		this.cd.detectChanges();
	  }
	});
  }

  accept(payment: InvitesDto) {
	// Show the payment form with the amount expected for this invite
	this.selectedPayment = payment;
	this.payAmount = payment?.payment?.amount || 0;
	this.showPayForm = true;
	this.cd.detectChanges();
  }

  decline(payment: InvitesDto) {
	this.paymentToDecline = payment;
	this.showDeclineConfirm = true;
	this.cd.detectChanges();
  }

  confirmDecline() {
	if (!this.paymentToDecline) return;

	const payment = this.paymentToDecline;
	const matchId = payment.match?.matchId;
	const userId = this.route.snapshot.paramMap.get('userId');

	if (!matchId || !userId) {
	  this.error = 'Impossible de décliner cette invitation.';
	  this.showDeclineConfirm = false;
	  this.cd.detectChanges();
	  return;
	}

	// First, cancel the payment if it exists
	if (payment.payment?.tr) {
	  this.payService.cancelPayment(payment.payment.tr).subscribe({
		next: () => {
		  // Payment cancelled, now decline the match
		  this.declineMatchInvitation(matchId, userId, payment);
		},
		error: (err) => {
		  console.error('Failed to cancel payment', err);
		  this.error = 'Erreur lors de l\'annulation du paiement.';
		  this.showDeclineConfirm = false;
		  this.cd.detectChanges();
		}
	  });
	} else {
	  // No payment to cancel, just decline the match
	  this.declineMatchInvitation(matchId, userId, payment);
	}
  }

  private declineMatchInvitation(matchId: number, userId: string, payment: InvitesDto) {
	this.inviteService.declineMatch(matchId, userId).subscribe({
	  next: () => {
		// Successfully declined, remove from list
		this.payments = this.payments.filter(p => p !== payment);
		this.showDeclineConfirm = false;
		this.paymentToDecline = null;
		this.cd.detectChanges();
	  },
	  error: (err) => {
		console.error('Failed to decline match', err);
		this.error = err?.message || 'Erreur lors du refus de l\'invitation.';
		this.showDeclineConfirm = false;
		this.cd.detectChanges();
	  }
	});
  }

  cancelDecline() {
	this.showDeclineConfirm = false;
	this.paymentToDecline = null;
	this.cd.detectChanges();
  }

  acknowledgeSuccess(): void {
    this.showSuccessDialog = false;
    this.popupMessage = null;
    const userId = this.route.snapshot.paramMap.get('userId');
    if (userId) {
      this.router.navigate(['/home', userId]).catch(() => {});
    } else {
      this.router.navigate(['/home']).catch(() => {});
    }
    this.cd.detectChanges();
  }

  formatTime(t?: any): string {
    if (!t) return '';
    const hour = (t && (t.hour ?? t.Hour)) ?? null;
    const minute = (t && (t.minute ?? t.Minute)) ?? 0;
    if (hour == null) {
      try { return String(t); } catch { return ''; }
    }
    return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
  }

  onPaymentCompleted(evt: { amount: number; cardLast4?: string }) {
    // Close the form and attempt to update the payment on the backend using the current user as payer
    console.log('Payment completed', evt, 'for', this.selectedPayment);
    this.showPayForm = false;

    if (!this.selectedPayment || !this.selectedPayment.payment || !this.selectedPayment.payment.tr) {
      this.error = 'Internal error: no selected payment to complete.';
      this.cd.detectChanges();
      return;
    }

    // Resolve current authenticated user's matricule and call update endpoint
    this.userService.getCurrentUser().pipe(take(1)).subscribe({
      next: (u: any) => {
        const currentMat = u?.matricule;
        if (!currentMat) {
          this.error = 'Unable to determine current user.';
          this.cd.detectChanges();
          return;
        }

        const dto: MatchPaymentDto = {
          tr: this.selectedPayment!.payment!.tr,
          userMatricule: currentMat,
          status: 'clear',
          paymentMethod: 'CARD'
        };

        this.payService.updatePayment(this.selectedPayment!.payment!.tr!, dto).subscribe({
          next: (_res) => {
            if (this.selectedPayment?.match?.matchId) {
              this.matchService.joinPublicMatchOrUpdatePrivate(this.selectedPayment.match.matchId, currentMat).subscribe({
                next: () => {
                  this.completePaymentSuccess();
                },
                error: (err: any) => {
                  console.error('Failed to update match player', err);
                  this.error = err?.message || 'Erreur lors de la mise à jour de l\'inscription.';
                  this.selectedPayment = null;
                  this.cd.detectChanges();
                }
              });
            } else {
              this.completePaymentSuccess();
            }
          },
          error: (err: any) => {
            console.error('Failed to update payment', err);
            this.error = err?.message || 'Erreur lors du traitement du paiement.';
            // keep the payment in the list so user can retry
            this.selectedPayment = null;
            this.cd.detectChanges();
          }
        });
      },
      error: (err: any) => {
        console.error('Failed to resolve current user', err);
        this.error = 'Impossible de récupérer l’utilisateur courant.';
        this.cd.detectChanges();
      }
    });
  }

  onPaymentCancelled() {
	this.showPayForm = false;
	this.selectedPayment = null;
	this.cd.detectChanges();
  }

  private completePaymentSuccess() {
    const m = this.selectedPayment?.match;
    const matchDate = m?.matchDate ? new Date(m.matchDate) : null;
    const formattedDate = matchDate ? matchDate.toLocaleDateString('fr-FR') : '—';
    const startTime = this.formatTime(m?.startTime);
    const endTime = this.formatTime(m?.endTime);

    this.popupMessage = `Votre paiement a été accepté. Vous êtes inscrit pour ce match le ${formattedDate} de ${startTime} à ${endTime}.`;

    // remove locally and clear selection
    this.payments = this.payments.filter(p => p.payment?.tr !== this.selectedPayment?.payment?.tr);
    this.selectedPayment = null;
    this.showSuccessDialog = true;
    this.cd.detectChanges();
  }
}
