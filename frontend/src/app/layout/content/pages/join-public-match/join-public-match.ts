import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';
import { MatchService } from '../../../../services/match.service';
import { MatchDto } from '../../../../api/model/matchDto';
import { PayFormComponent } from '../../pay-form/pay-form';
import { PayService } from '../../../../services/pay.service';
import { UserService } from '../../../../services/user.service';
import { take } from 'rxjs/operators';
import { MatchPaymentDto } from '../../../../api/model/matchPaymentDto';
import { MatchSiteFieldDto } from '../../../../api/model/matchSiteFieldDto';

@Component({
  selector: 'app-join-public-match',
  standalone: true,
  imports: [CommonModule, NavMenu, HomeAccountHeader, PayFormComponent],
  templateUrl: './join-public-match.html'
})
export class JoinPublicMatch implements OnInit {
  matches: Array<MatchSiteFieldDto> = [];
  loading = false;
  error: string | null = null;

  // Payment form state
  showPayForm = false;
  payAmount = 0;
  selectedMatch: MatchDto | null = null;

  // Confirmation popup state
  showSuccessDialog = false;
  popupMessage: string | null = null;
  confirmedMatch: MatchSiteFieldDto | null = null;

  // Status translations
  private statusTranslations: Record<string, string> = {
    open: 'Ouvert',
    closed: 'Fermé',
    cancelled: 'Annulé',
    completed: 'Terminé'
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private matchService: MatchService,
    private cd: ChangeDetectorRef,
    private payService: PayService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.loadMatches();
  }

  private loadMatches(): void {
    this.loading = true;
    this.error = null;
    try {
        this.userService.getCurrentUser().pipe(take(1)).subscribe({
            next: (user: any) => {
                const userId = user?.matricule;
                if (!userId) {
                    this.error = 'Unable to determine current user.';
                    this.loading = false;
                    this.cd.detectChanges();
                    return;
                }

                this.matchService.getAvailablePublicMatches(userId).subscribe({
                    next: (data: MatchSiteFieldDto[]) => {
                        const all = Array.isArray(data) ? data : [];
                        const tomorrow = this.getTomorrowIsoDate();

                        // Filter matches from tomorrow onwards
                        this.matches = all.filter(m => {
                            const dateOnly = (m.match?.matchDate ?? '').split('T')[0];
                            return dateOnly >= tomorrow;
                        });

                        this.loading = false;
                        this.cd.detectChanges();
                    },
                    error: (err: any) => {
                        console.error('Failed to load available public matches', err);
                        this.error = err?.message || 'Erreur lors du chargement des matchs publics disponibles.';
                        this.matches = [];
                        this.loading = false;
                        this.cd.detectChanges();
                    }
                });
            },
            error: (err: any) => {
                console.error('Failed to resolve current user', err);
                this.error = 'Impossible de récupérer l’utilisateur courant.';
                this.loading = false;
                this.cd.detectChanges();
            }
        });
    } catch (e) {
        this.loading = false;
        this.error = 'Erreur interne.';
        this.cd.detectChanges();
    }
  }

  joinMatch(m: MatchSiteFieldDto) {
    if (!m.match) {
      this.error = 'Invalid match data';
      this.cd.detectChanges();
      return;
    }

    console.log('Join match', m);
    this.selectedMatch = m.match;
    this.payAmount = m.match.pricing != null ? (m.match.pricing / 4) : 0;
    this.showPayForm = true;
    this.cd.detectChanges();
  }

  onPaymentCompleted(evt: { amount: number; cardLast4?: string }) {
    console.log('Payment completed', evt, 'for', this.selectedMatch);
    this.showPayForm = false;

    if (!this.selectedMatch || !this.selectedMatch.matchId) {
      this.error = 'Internal error: no selected match to join.';
      this.cd.detectChanges();
      return;
    }

    this.userService.getCurrentUser().pipe(take(1)).subscribe({
      next: (u: any) => {
        const currentMat = u?.matricule;
        if (!currentMat) {
          this.error = 'Unable to determine current user.';
          this.cd.detectChanges();
          return;
        }

        const dto: MatchPaymentDto = {
          matchId: this.selectedMatch!.matchId,
          userMatricule: currentMat,
          amount: evt.amount,
          status: 'clear',
          paymentMethod: 'CARD'
        };

        this.payService.createPayment(dto).subscribe({
          next: (paymentResponse: any) => {
            const paymentId = paymentResponse?.tr;

            if (!paymentId) {
              this.error = 'Payment was successful but no transaction reference was returned.';
              this.cd.detectChanges();
              return;
            }

            this.matchService.joinPublicMatchOrUpdatePrivate(this.selectedMatch!.matchId!, currentMat).subscribe({
              next: () => {
                const matchDate = this.selectedMatch!.matchDate ? new Date(this.selectedMatch!.matchDate) : null;
                const formattedDate = matchDate ? matchDate.toLocaleDateString('fr-FR') : '—';
                const startTime = this.formatTime(this.selectedMatch!.startTime);
                const endTime = this.formatTime(this.selectedMatch!.endTime);

                this.popupMessage = `Vous vous êtes inscrits pour ce match, le ${formattedDate} de ${startTime} à ${endTime}. Veuillez vous présenter 15 minutes avant le début du match.`;
                this.confirmedMatch = this.matches.find(m => m.match?.matchId === this.selectedMatch?.matchId) || null;
                this.selectedMatch = null;
                this.showSuccessDialog = true;
                this.cd.detectChanges();
              },
              error: (err: any) => {
                console.error('Failed to join match', err);
                this.error = err?.message || 'Erreur lors de l\'inscription au match.';
                this.cd.detectChanges();
              }
            });
          },
          error: (err: any) => {
            console.error('Failed to create payment', err);
            this.error = err?.message || 'Erreur lors du traitement du paiement.';
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
    this.selectedMatch = null;
    this.cd.detectChanges();
  }

  acknowledgeSuccess(): void {
    this.showSuccessDialog = false;
    this.popupMessage = null;
    const userId = this.route.snapshot.paramMap.get('userId');
    if (userId) {
      this.router.navigate(['/home', userId]).catch(() => {});
    }
    this.cd.detectChanges();
  }

  // Helper methods for formatting and displaying data

  formatTime(t?: any): string {
    if (!t) return '';
    const hour = (t && (t.hour ?? t.Hour)) ?? null;
    const minute = (t && (t.minute ?? t.Minute)) ?? 0;
    if (hour == null) {
      try { return String(t); } catch { return ''; }
    }
    return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
  }

  getStatusTranslation(status?: string): string {
    if (!status) return '—';

    const lowerStatus = status.toLowerCase();
    return this.statusTranslations[lowerStatus] ||
           status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
  }

  formatMatchDate(date?: string): string {
    if (!date) return '—';
    const d = new Date(date);
    return d.toLocaleDateString('fr-FR', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  getMatchTimeRange(match: MatchSiteFieldDto['match']): string {
    const start = this.formatTime(match?.startTime);
    const end = this.formatTime(match?.endTime);
    return `${start} - ${end}`;
  }

  getFieldType(field: MatchSiteFieldDto['field']): string {
    return field?.isIndoor ? 'Intérieur' : 'Extérieur';
  }

  getSiteName(site: MatchSiteFieldDto['site']): string {
    return site?.name || '—';
  }

  getMatchType(match: MatchSiteFieldDto['match']): string {
    const type = match?.type ?? '';
    if (type === 'public') return 'Public';
    if (type === 'private') return 'Privé';
    return type || '—';
  }

  getMatchPricing(match: MatchSiteFieldDto['match']): string {
    if (match?.pricing == null) return 'Prix non disponible';
    return `${(match.pricing / 4).toFixed(2)} €`;
  }

  private getTomorrowIsoDate(): string {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
}

