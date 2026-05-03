import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';
import { MatchService } from '../../../../services/match.service';
import { MatchPlayerSiteFieldDto } from '../../../../api/model/matchPlayerSiteFieldDto';
import { DeclinedPlayersDto } from '../../../../api/model/declinedPlayersDto';

@Component({
  selector: 'app-my-matches',
  standalone: true,
  imports: [CommonModule, NavMenu, HomeAccountHeader],
  templateUrl: './my-matches.html'
})
export class MyMatches implements OnInit {
  matchPlayers: MatchPlayerSiteFieldDto[] = [];
  declinedPlayers: DeclinedPlayersDto[] = [];
  loading = false;
  error: string | null = null;
  userId: string | null = null;
  showInviteForm = false;
  selectedMatchId: number | null = null;

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

    // Use forkJoin to wait for both API calls to complete
    forkJoin([
      this.matchService.getMyMatches(matricule),
      this.matchService.getOrganiserMatchesWithDeclinedPlayers(matricule)
    ]).subscribe({
      next: ([matchesData, declinedData]: [MatchPlayerSiteFieldDto[], DeclinedPlayersDto[]]) => {
        // Process matches data
        this.matchPlayers = Array.isArray(matchesData) ? matchesData : [];

        // Sort matches by date (closest first)
        this.matchPlayers.sort((a, b) => {
          const dateA = a.match?.matchDate ? new Date(a.match.matchDate).getTime() : Infinity;
          const dateB = b.match?.matchDate ? new Date(b.match.matchDate).getTime() : Infinity;
          return dateA - dateB;
        });

        // Process declined players data
        this.declinedPlayers = Array.isArray(declinedData) ? declinedData : [];

        this.loading = false;
        this.cd.detectChanges();
      },
      error: (err: any) => {
        console.error('Error loading matches or declined players:', err);
        this.error = err?.message || 'Erreur lors du chargement de vos matchs.';
        this.matchPlayers = [];
        this.declinedPlayers = [];
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

  // Check if current user is the organiser of this match
  isOrganiser(match: any): boolean {
    if (!match || !this.userId) return false;
    return match.match?.organiserId === this.userId;
  }

  // Check if current user is a player in this match
  isCurrentUserPlayer(match: any): boolean {
    if (!match || !this.userId) return false;
    return match.player?.userMatricule === this.userId;
  }

  // Check if current match has declined players AND current user is the organiser
  hasDeclinedPlayers(match: any): boolean {
    if (!match || !this.userId) return false;

    // Get the actual match ID - it might be in match.match?.matchId, not match.matchId
    const actualMatchId = match.match?.matchId || match.matchId;
    if (!actualMatchId) return false;

    // Check if current user is the organiser of this match
    const isOrganiser = this.isOrganiser(match);

    // Check if this match has declined players
    const hasDeclined = this.declinedPlayers.some(dp => dp.matchId === actualMatchId);

    return isOrganiser && hasDeclined;
  }

  // Show invite form for replacing declined players
  showReplaceInviteForm(matchId: number): void {
    this.selectedMatchId = matchId;
    this.showInviteForm = true;
  }

  // Hide invite form
  hideInviteForm(): void {
    this.showInviteForm = false;
    this.selectedMatchId = null;
  }

  // Handle inviting replacement players
  inviteReplacementPlayers(newPlayerIds: string[]): void {
    if (!this.selectedMatchId || !this.userId) return;

    // For each new player, update the match player status
    newPlayerIds.forEach((newPlayerId, index) => {
      const playerRole = index === 0 ? 'p2' : index === 1 ? 'p3' : 'p4';

      this.matchService.joinPublicMatch(this.selectedMatchId!, newPlayerId, playerRole).subscribe({
        next: () => {
          console.log(`Successfully invited replacement player ${newPlayerId} for role ${playerRole}`);
        },
        error: (err) => {
          console.error(`Error inviting replacement player ${newPlayerId}:`, err);
        }
      });
    });

    // Show success message
    this.showSuccessPopup();
    this.hideInviteForm();
  }

  // Show success popup
  showSuccessPopup(): void {
    // Implement popup logic here
    alert('Nouveaux joueurs invités avec succès!');
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

  // Method to get declined players count for a specific match
  getDeclinedCount(matchId: number | undefined): number {
    if (!matchId) return 0;
    return this.declinedPlayers.filter(dp => dp.matchId === matchId).length;
  }
}
