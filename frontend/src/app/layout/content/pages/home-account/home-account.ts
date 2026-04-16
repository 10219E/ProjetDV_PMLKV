import { Component, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../../services/auth.service';
import { UserService } from '../../../../services/user.service';

@Component({
  selector: 'app-home-account',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home-account.html',
  styleUrls: ['./home-account.css']
})
export class HomeAccount implements OnInit {
  userName = 'Utilisateur';
  isVip = true;
  todayDate = new Date();

  // Dummy data for visual
  upcomingMatches = [
    { site: 'Centre Sportif', date: '12/05/2026', heure: '14:00', status: 'Confirmé' },
    { site: 'Padel Club XY', date: '14/05/2026', heure: '18:30', status: 'En attente' }
  ];

  invitations = [
    { site: 'Padel Arena', date: '20/05/2026', heure: '10:00' }
  ];

  stats = {
    played: 12,
    penalties: 0,
    actives: 1,
    privateOrganized: 5,
    publicParticipation: 7
  };

  // roleClass controls the color of the user full name
  roleClass = 'text-white';
  // full user display name (fname + lname)
  userFullName = '';

  constructor(private route: ActivatedRoute, private authService: AuthService, private userService: UserService, private cd: ChangeDetectorRef, private ngZone: NgZone) {}

  ngOnInit(): void {
    const userId = this.route.snapshot.paramMap.get('userId');
    if (!userId) {
      throw new Error('userId route parameter is required');
    }

    // compute the role-based color class
    const role = this.authService.getUserRole() || '';
    if (role && role.toUpperCase().includes('ADMIN')) {
      this.roleClass = 'text-red-500';
    } else if (role && role.toUpperCase().includes('ALL_SITE_ACCESS')) {
      this.roleClass = 'text-orange-500';
    } else {
      this.roleClass = 'text-white';
    }

    // If a route param userId exists, use it immediately as fallback display (Joueur {id})
    this.userFullName = 'Joueur ' + userId;
    this.userName = this.userFullName;

    // Wait for token then try getCurrentUser first, fallback to getUserById
    this.waitForTokenAndFetchUser(userId);
  }

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

  acceptInvite(invite: any) {
    console.log('Accept invite', invite);
  }

  declineInvite(invite: any) {
    console.log('Decline invite', invite);
  }
}
