import { Component, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { HomeAccountHeader } from '../../header/header';
import { NavMenu } from '../../nav-menu/nav-menu';
import { AuthService } from '../../../../services/auth.service';
import { UserService } from '../../../../services/user.service';
import { MatchService } from '../../../../services/match.service';
import { InviteService } from '../../../../services/invite.service';

import { take } from 'rxjs/operators';

@Component({
  selector: 'app-home-account',
  standalone: true,
  imports: [CommonModule, MatIconModule, NavMenu, HomeAccountHeader],
  templateUrl: './home-account.html',
  styleUrls: ['./home-account.css']
})
export class HomeAccount implements OnInit {
  userName = 'Utilisateur';
  isVip = true;
  isAdmin = false;
  todayDate = new Date();

  // Dummy data for visual
  upcomingMatches: any[] = [];
  invitations: any[] = [];

  stats = {
    played: 0,
    penalties: 0,
    actives: 0,
    privateOrganized: 0,
    publicParticipation: 0
  };

  userAccount: any = null;

  // roleClass controls the color of the user full name
  roleClass = 'text-white';
  // full user display name (fname + lname)
  userFullName = '';
  userId = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private userService: UserService,
    private matchService: MatchService,
    private inviteService: InviteService,
    private cd: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    const userId = this.route.snapshot.paramMap.get('userId');
    if (!userId) {
      throw new Error('userId route parameter is required');
    }

    // compute the role flags and the role-based color class
    const role = this.authService.getUserRole() || '';
    this.isAdmin = !!(role && role.toUpperCase().includes('ADMIN'));
    this.isVip = !!(role && role.toUpperCase().includes('ALL_SITE_ACCESS'));
    if (this.isAdmin) {
      this.roleClass = 'text-red-500';
    } else if (this.isVip) {
      this.roleClass = 'text-orange-500';
    } else {
      this.roleClass = 'text-white';
    }

    this.userId = userId;

    // If a route param userId exists, use it immediately as fallback display (Joueur {id})
    this.userFullName = 'Joueur ' + userId;
    this.userName = this.userFullName;

    // Wait for token then try getCurrentUser first, fallback to getUserById
    this.waitForTokenAndFetchUser(userId);
    this.loadUserData(userId);
  }

  private loadUserData(userId: string) {
    console.log('loadUserData called with userId:', userId); //check if user data is loading
    this.loadingData = true;

    // Load upcoming matches
    this.matchService.getMyMatches(userId).subscribe({
      next: (matchesData: any[]) => {
        if (Array.isArray(matchesData)) {
          // Sort matches by date
          const upcoming = matchesData
            .filter((m: any) => m.match && m.match.matchDate)
            .map((m: any) => {
              const d = new Date(m.match.matchDate);
              return {
                id: m.match.matchId,
                type: this.translateMatchType(m.match.type),
                site: m.site?.name || "Inconnu",
                field: m.field.fieldId || "N/A",
                date: d.toLocaleDateString('fr-FR'),
                matchstart: this.convertToFourDigitsTime(m.match.startTime|| '00:00'),
                matchend: this.convertToFourDigitsTime(m.match.endTime|| '00:00'),
                timestamp: d.getTime()
              };
            })
            .sort((a, b) => a.timestamp - b.timestamp);

          this.upcomingMatches = upcoming.slice(0, 5);
          this.cd.detectChanges();
        }
      }
    });

    // Load pending invitations
    this.inviteService.fetchPendingInvitesForUser(userId).subscribe({
      next: (invitesData: any[]) => {
        if (Array.isArray(invitesData)) {
          this.invitations = invitesData.map(inv => {
            const d = new Date(inv.match.matchDate || new Date());
            return {
              id: inv.match?.matchId,
              original: inv,
              type: "Privée",
              site: inv.site?.name || 'Inconnu',
              field: inv.field.fieldId || "N/A",
              date: d.toLocaleDateString('fr-FR'),
              matchstart: this.convertToFourDigitsTime(inv.match?.startTime || '00:00'),
              matchend: this.convertToFourDigitsTime(inv.match?.endTime || '00:00'),
              timestamp: d.getTime()
            };
          }).slice(0, 5);
          this.cd.detectChanges();
        }
      }
    });

    // We get some stats using user details
    this.userService.getUserById(userId).pipe(take(1)).subscribe({
      next: (u: any) => {
        if (u) {
          this.userAccount = u.account || null;
          this.stats.actives = (u.penalties || []).filter((p: any) => p.isActive).length;
          this.stats.penalties = (u.penalties || []).length;

          this.cd.detectChanges();
        }
      }
    });
  }

  loadingData = false;

  private waitForTokenAndFetchUser(userId: string, retries = 15) {
    const token = this.authService.getToken();
    if (token) {
      // Try current user first
      this.userService.getCurrentUser().subscribe({
        next: (u: any) => {
          console.debug('getCurrentUser response', u);
          const fname = (u && u.firstName) || '';
          const lname = (u && u.lastName) || '';
          const full = (fname + ' ' + lname).trim();
          if (full) {
            // ensure change detection runs if this callback executes outside Angular zone
            this.ngZone.run(() => {
              this.userFullName = full;
              this.userName = this.userFullName;
              this.cd.detectChanges();
            });
            return;
          }
          // If current user doesn't provide a name, fallback to getUserById
          this.fetchByIdFallback(userId);
        },
        error: (err) => {
          console.warn('getCurrentUser failed, falling back to getUserById', err);
          this.fetchByIdFallback(userId);
        }
      });
    } else if (retries > 0) {
      setTimeout(() => this.waitForTokenAndFetchUser(userId, retries - 1), 100);
    } else {
      console.warn('Token not available after waiting, skipping user fetch');
    }
  }

  private fetchByIdFallback(userId: string) {
    this.userService.getUserById(userId).subscribe({
      next: (u: any) => {
        console.debug('getUserById response', u);
        const fname = (u && u.firstName) || '';
        const lname = (u && u.lastName) || '';
        const full = (fname + ' ' + lname).trim();
        if (full) {
          this.ngZone.run(() => {
            this.userFullName = full;
            this.userName = this.userFullName;
            this.cd.detectChanges();
          });
        }
      },
      error: (err) => {
        console.warn('getUserById failed; keeping fallback display value', err);
      }
    });
  }

  goToMyMatches() {
    if (this.userId) {
      this.router.navigate(['/home', this.userId, 'my_matches']);
    }
  }

  goToMyInvites() {
    if (this.userId) {
      this.router.navigate(['/home', this.userId, 'invites']);
    }
  }

  goToMyProfile() {
    if (this.userId) {
      this.router.navigate(['/home', this.userId, 'me']);
    }
  }

  convertToFourDigitsTime(time: string): string {
    // Split by ':' and take only the first two parts (hours and minutes)
    return time.split(':').slice(0, 2).join(':');
  }

  translateMatchType(type?: string): string {
    if (!type) return 'Inconnu';
    switch (type.toLowerCase()) {
      case 'private': return 'Privé';
      case 'public': return 'Public';
      default: return type;
    }
  }

  translateAccountStatus(status?: string): string {
    if (!status) return 'Inconnu';
    switch (status.toLowerCase()) {
      case 'active': return 'Actif';
      case 'clear': return 'Apuré';
      case 'suspended': return 'Suspendu';
      case 'debt': return 'En dette';
      default: return status;
    }
  }
}
