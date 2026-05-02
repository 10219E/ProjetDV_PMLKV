import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';
import { MatchService } from '../../../../services/match.service';
import { MatchPlayerSiteFieldDto } from '../../../../api/model/matchPlayerSiteFieldDto';

@Component({
  selector: 'app-my-matches',
  standalone: true,
  imports: [CommonModule, NavMenu, HomeAccountHeader],
  templateUrl: './my-matches.html'
})
export class MyMatches implements OnInit {
  matchPlayers: MatchPlayerSiteFieldDto[] = [];
  loading = false;
  error: string | null = null;
  userId: string | null = null;

  // Status translations
  private statusTranslations: Record<string, string> = {
    approved: 'Confirmé',
    pending: 'En attente',
    declined: 'Rejeté',
    invited: 'Invité'
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private matchService: MatchService,
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
      next: (data: MatchPlayerSiteFieldDto[]) => {
        // Directly use the data from the API response
        this.matchPlayers = Array.isArray(data) ? data : [];

        // Sort matches by date (closest first)
        this.matchPlayers.sort((a, b) => {
          const dateA = a.match?.matchDate ? new Date(a.match.matchDate).getTime() : Infinity;
          const dateB = b.match?.matchDate ? new Date(b.match.matchDate).getTime() : Infinity;
          return dateA - dateB;
        });

        this.loading = false;
        this.cd.detectChanges();
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

  matchType(mp: MatchPlayerSiteFieldDto): string {
    const type = mp.match?.type ?? '';
    if (type === 'public') return 'Public';
    if (type === 'private') return 'Privé';
    return type || '—';
  }

  isPending(mp: MatchPlayerSiteFieldDto): boolean {
    return (mp.player?.status ?? '').toLowerCase() === 'pending';
  }

  // Method to get status translation from the constructor
  getStatusTranslation(status?: string): string {
    if (!status) return '—';

    const lowerStatus = status.toLowerCase();
    return this.statusTranslations[lowerStatus] ||
           status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
  }

  // Method to format match date
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

  // Method to get match time range
  getMatchTimeRange(match: MatchPlayerSiteFieldDto['match']): string {
    const start = this.formatTime(match?.startTime);
    const end = this.formatTime(match?.endTime);
    return `${start} - ${end}`;
  }

  // Method to get field type
  getFieldType(field: MatchPlayerSiteFieldDto['field']): string {
    return field?.isIndoor ? 'Intérieur' : 'Extérieur';
  }

  // Method to get site name
  getSiteName(site: MatchPlayerSiteFieldDto['site']): string {
    return site?.name || '—';
  }
}
