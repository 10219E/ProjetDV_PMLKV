import { Component, OnInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HomeAccountHeader } from '../../header/header';
import { MatchForm } from '../../match-form/match-form';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../../services/auth.service';
import { UserService } from '../../../../services/user.service';
import { NavMenu } from '../../nav-menu/nav-menu';

@Component({
  selector: 'app-new-pub-match',
  standalone: true,
  imports: [CommonModule, HomeAccountHeader, MatchForm, NavMenu],
  templateUrl: './new-pub-match.html',
})
export class NewPubMatch implements OnInit {
  today = new Date();
  userId: string | null = null;
  userName: string = 'Utilisateur';

  constructor(private route: ActivatedRoute, private authService: AuthService, private userService: UserService, private cd: ChangeDetectorRef, private ngZone: NgZone) {}

  ngOnInit(): void {
    const userId = this.route.snapshot.paramMap.get('userId');
    this.userId = userId;

    // Try to get current user name; fallback to getUserById
    this.waitForTokenAndFetchUser(userId);
  }

  private waitForTokenAndFetchUser(userId: string | null, retries = 15) {
    const token = this.authService.getToken();
    if (token) {
      this.userService.getCurrentUser().subscribe({
        next: (u: any) => {
          const fname = (u && u.firstName) || '';
          const lname = (u && u.lastName) || '';
          const full = (fname + ' ' + lname).trim();
          if (full) {
            this.ngZone.run(() => {
              this.userName = full;
              this.cd.detectChanges();
            });
            return;
          }
          if (userId) this.fetchByIdFallback(userId);
        },
        error: () => { if (userId) this.fetchByIdFallback(userId); }
      });
    } else if (retries > 0) {
      setTimeout(() => this.waitForTokenAndFetchUser(userId, retries - 1), 100);
    }
  }

  private fetchByIdFallback(userId: string) {
    this.userService.getUserById(userId).subscribe({
      next: (u: any) => {
        const fname = (u && u.firstName) || '';
        const lname = (u && u.lastName) || '';
        const full = (fname + ' ' + lname).trim();
        if (full) {
          this.ngZone.run(() => {
            this.userName = full;
            this.cd.detectChanges();
          });
        }
      },
      error: () => {}
    });
  }
}

