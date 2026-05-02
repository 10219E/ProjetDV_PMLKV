import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';
import { MatchService } from '../../../../services/match.service';
import { MatchDto } from '../../../../api/model/matchDto';
import { InfoService } from '../../../../services/info.service';
import { SiteInfo } from '../../../../api/model/siteInfo';
import {FieldService} from '../../../../services/field.service';
import {FieldDto} from "../../../../api/model/fieldDto";
import { PayFormComponent } from '../../pay-form/pay-form';
import { PayService } from '../../../../services/pay.service';
import { UserService } from '../../../../services/user.service';
import { take } from 'rxjs/operators';
import { MatchPaymentDto } from '../../../../api/model/matchPaymentDto';

@Component({
  selector: 'app-join-public-match',
  standalone: true,
  imports: [CommonModule, NavMenu, HomeAccountHeader, PayFormComponent],
  templateUrl: './join-public-match.html'
})
export class JoinPublicMatch implements OnInit {
  matches: Array<MatchDto & { siteName?: string }> = [];
  loading = false;
  error: string | null = null;

  // Payment form state
  showPayForm = false;
  payAmount = 0;
  selectedMatch: MatchDto | null = null;

  // Confirmation popup state
  showSuccessDialog = false;
  popupMessage: string | null = null;
  confirmedMatch: (MatchDto & { siteName?: string }) | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private matchService: MatchService,
    private infoService: InfoService,
    private fieldService: FieldService,
    private cd: ChangeDetectorRef,
    private payService: PayService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    // load public matches with status open
    this.loadMatches();
  }

  private loadMatches(): void {
    this.loading = true;
    this.error = null;
    try {
        // Get the current user's matricule
        this.userService.getCurrentUser().pipe(take(1)).subscribe({
            next: (user: any) => {
                const userId = user?.matricule;
                if (!userId) {
                    this.error = 'Unable to determine current user.';
                    this.loading = false;
                    this.cd.detectChanges();
                    return;
                }

                // Use the new service method to get available public matches
                this.matchService.getAvailablePublicMatches(userId).subscribe({
                    next: (data: any) => {
                        const all = Array.isArray(data) ? data : [];
                        // keep only matches from tomorrow (exclude today and past matches)
                        const tomorrow = this.getTomorrowIsoDate();
                        const filtered: MatchDto[] = all.filter((m: MatchDto) => {
                            const dateOnly = (m?.matchDate ?? '').split('T')[0];
                            return dateOnly >= tomorrow;
                        });

                        // First, get all fields to build a fieldId to siteId map
                        this.fieldService.fetchAllFields().subscribe({
                            next: (fields: FieldDto[]) => {
                                const fieldToSiteMap: Record<number, number> = {};
                                (fields || []).forEach(f => {
                                    if (f?.fieldId != null && f?.siteId != null) {
                                        fieldToSiteMap[f.fieldId] = f.siteId;
                                    }
                                });

                                // Then get all sites to build a siteId to siteName map
                                this.infoService.getSites().subscribe({
                                    next: (sites: SiteInfo[]) => {
                                        const siteMap: Record<number, string> = {};
                                        (sites || []).forEach(s => {
                                            if (s?.siteId != null) {
                                                const name = (s.name ?? '').toString().trim();
                                                siteMap[s.siteId] = name.length > 0 ? name : '—';
                                            }
                                        });

                                        // Now attach siteName to each match using the fieldToSiteMap
                                        this.matches = filtered.map(m => {
                                            const siteId = m?.fieldId != null ? fieldToSiteMap[m.fieldId] : null;
                                            const siteName = siteId != null ? (siteMap[siteId] ?? '—') : '—';
                                            return { ...m, siteName } as MatchDto & { siteName?: string };
                                        });

                                        this.loading = false;
                                        this.cd.detectChanges();
                                    },
                                    error: (err) => {
                                        console.error('Error fetching sites:', err);
                                        // If site fetch fails, still show matches with fallback
                                        this.matches = filtered.map(m => ({ ...m, siteName: '—' } as MatchDto & { siteName?: string }));
                                        this.loading = false;
                                        this.cd.detectChanges();
                                    }
                                });
                            },
                            error: (err) => {
                                console.error('Error fetching fields:', err);
                                // If field fetch fails, still show matches with fallback
                                this.matches = filtered.map(m => ({ ...m, siteName: '—' } as MatchDto & { siteName?: string }));
                                this.loading = false;
                                this.cd.detectChanges();
                            }
                        });
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

  // Placeholder action when user clicks Join. Real implementation should call backend or navigate to match details.
  joinMatch(m: MatchDto) {
    console.log('Join match', m);
    this.selectedMatch = m;
    this.payAmount = m.pricing != null ? (m.pricing / 4) : 0;
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

    // Resolve current authenticated user's matricule and call join endpoint
    this.userService.getCurrentUser().pipe(take(1)).subscribe({
      next: (u: any) => {
        const currentMat = u?.matricule;
        if (!currentMat) {
          this.error = 'Unable to determine current user.';
          this.cd.detectChanges();
          return;
        }

        // Create payment DTO for the match join
        const dto: MatchPaymentDto = {
          matchId: this.selectedMatch!.matchId, // Add matchId to the payment DTO
          userMatricule: currentMat,
          amount: evt.amount,
          status: 'clear',
          paymentMethod: 'CARD'
        };

        // First, create the payment record
        this.payService.createPayment(dto).subscribe({
          next: (paymentResponse: any) => {
            // Extract the transaction reference (tr) from the payment response
            const paymentId = paymentResponse?.tr;

            if (!paymentId) {
              this.error = 'Payment was successful but no transaction reference was returned.';
              this.cd.detectChanges();
              return;
            }

            // Then join the match
            this.matchService.joinPublicMatch(this.selectedMatch!.matchId!, currentMat).subscribe({
              next: () => {
                // Set the confirmation message with match details
                const matchDate = this.selectedMatch!.matchDate ? new Date(this.selectedMatch!.matchDate) : null;
                const formattedDate = matchDate ? matchDate.toLocaleDateString('fr-FR') : '—';
                const startTime = this.formatTime(this.selectedMatch!.startTime);
                const endTime = this.formatTime(this.selectedMatch!.endTime);

                this.popupMessage = `Vous vous êtes inscrits pour ce match, le ${formattedDate} de ${startTime} à ${endTime}. Veuillez vous présenter 15 minutes avant le début du match.`;
                this.confirmedMatch = this.selectedMatch as MatchDto & { siteName?: string };
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

  closeConfirmation(): void {
    this.showSuccessDialog = false;
    const userId = this.route.snapshot.paramMap.get('userId');
    if (userId) {
      this.router.navigate(['/home', userId]).catch(() => {});
    }
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

  viewMatch(m: MatchDto) {
    console.log('View match', m);
    // implement view behavior: for now navigate to match detail route if available
    const userId = this.route.snapshot.paramMap.get('userId');
    if (userId && m && m.matchId != null) {
      this.router.navigate(['/home', userId, 'match', String(m.matchId)]).catch(() => {});
    }
  }

  formatTime(t?: any): string {
    if (!t) return '';
    // LocalTime from API typically has { hour?: number, minute?: number }
    const hour = (t && (t.hour ?? t.Hour)) ?? null;
    const minute = (t && (t.minute ?? t.Minute)) ?? 0;
    if (hour == null) {
      // fallback to string representation
      try { return String(t); } catch { return ''; }
    }
    return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
  }

  // Return tomorrow's date in local YYYY-MM-DD format (used to filter matches)
  private getTomorrowIsoDate(): string {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
}
