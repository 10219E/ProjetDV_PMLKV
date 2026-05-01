import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';
import { MatchService } from '../../../../services/match.service';
import { MatchPlayerDto } from '../../../../api/model/matchPlayerDto';
import { MatchDto } from '../../../../api/model/matchDto';
import { InfoService } from '../../../../services/info.service';
import { SiteInfo } from '../../../../api/model/siteInfo';
import { FieldService } from '../../../../services/field.service';
import { FieldDto } from '../../../../api/model/fieldDto';

@Component({
  selector: 'app-my-matches',
  standalone: true,
  imports: [CommonModule, NavMenu, HomeAccountHeader],
  templateUrl: './my-matches.html'
})
export class MyMatches implements OnInit {
  matchPlayers: Array<{ match: MatchDto & { siteName?: string }, player: MatchPlayerDto }> = [];
  loading = false;
  error: string | null = null;
  userId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private matchService: MatchService,
    private infoService: InfoService,
    private fieldService: FieldService,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.userId = this.route.snapshot.paramMap.get('userId');
    if (this.userId) {
      this.loadMatches(this.userId);
    }
  }

  private loadMatches(matricule: string): void {
    this.loading = true;
    this.error = null;
    this.matchService.getMyMatches(matricule).subscribe({
      next: (data: any) => {
        const all: Array<{ match: MatchDto, player: MatchPlayerDto }> = Array.isArray(data) ? data : [];

        this.fieldService.fetchAllFields().subscribe({
          next: (fields: FieldDto[]) => {
            const fieldToSiteMap: Record<number, number> = {};
            (fields || []).forEach(f => {
              if (f?.fieldId != null && f?.siteId != null) fieldToSiteMap[f.fieldId] = f.siteId;
            });

            this.infoService.getSites().subscribe({
              next: (sites: SiteInfo[]) => {
                const siteMap: Record<number, string> = {};
                (sites || []).forEach(s => {
                  if (s?.siteId != null) siteMap[s.siteId] = (s.name ?? '').toString().trim() || '—';
                });

                this.matchPlayers = all.map(item => {
                  const siteId = item.match?.fieldId != null ? fieldToSiteMap[item.match.fieldId] : undefined;
                  const siteName = siteId != null ? (siteMap[siteId] ?? '—') : '—';
                  return { match: { ...item.match, siteName }, player: item.player };
                });
                this.loading = false;
                this.cd.detectChanges();
              },
              error: () => {
                this.matchPlayers = all.map(item => ({ match: { ...item.match, siteName: '—' }, player: item.player }));
                this.loading = false;
                this.cd.detectChanges();
              }
            });
          },
          error: () => {
            this.matchPlayers = all.map(item => ({ match: { ...item.match, siteName: '—' }, player: item.player }));
            this.loading = false;
            this.cd.detectChanges();
          }
        });
      },
      error: (err: any) => {
        this.error = err?.message || 'Erreur lors du chargement de vos matchs.';
        this.matchPlayers = [];
        this.loading = false;
        this.cd.detectChanges();
      }
    });
  }

  goToInvitation(): void {
    if (this.userId) {
      this.router.navigate(['/home', this.userId, 'invites']);
    }
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

  matchType(mp: { match: MatchDto, player: MatchPlayerDto }): string {
    const type = mp.match?.type ?? '';
    if (type === 'public') return 'Public';
    if (type === 'private') return 'Privé';
    return type || '—';
  }

  isPending(mp: { match: MatchDto, player: MatchPlayerDto }): boolean {
    return (mp.player?.status ?? '').toLowerCase() === 'pending';
  }

  // Add this method to your MyMatches class
  getStatusTranslation(status?: string): string {
    if (!status) return '—';

    const lowerStatus = status.toLowerCase();

    switch (lowerStatus) {
      case 'approved':
        return 'Confirmé';
      case 'pending':
        return 'En attente';
      case 'declined':
        return 'Rejeté';
      case 'invited':
        return 'Invité';
      default:
        return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
    }
  }
}
