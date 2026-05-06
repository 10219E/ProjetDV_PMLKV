import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { UserService } from '../../../../services/user.service';
import { AuthService } from '../../../../services/auth.service';
import { UserProfileDto } from '../../../../api/model/userProfileDto';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';
import { take } from 'rxjs/operators';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, NavMenu, HomeAccountHeader],
  templateUrl: './my-profile.html',
  styleUrl: './my-profile.css'
})
export class MyProfile implements OnInit {
  user: UserProfileDto | null = null;
  loading = true;
  error: string | null = null;

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const userId = params.get('userId');
      if (userId) {
        this.fetchUser(userId);
      } else {
        this.error = "Utilisateur non spécifié.";
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  fetchUser(userId: string): void {
    this.loading = true;
    this.cdr.detectChanges();

    this.userService.getUserById(userId).pipe(take(1)).subscribe({
      next: (u) => {
        this.user = u;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = "Erreur lors de la récupération du profil.";
        this.loading = false;
        this.cdr.detectChanges();
        console.error(err);
      }
    });
  }

  canUpgrade(): boolean {
    if (!this.user) return false;
    const rid = Number(this.user.roleId ?? -1);
    // Role 2: Member? Role 7/9: Admins?
    // User requested: != 7 or != 9 or != 2
    return ![2, 7, 9].includes(rid);
  }

  onUpgrade(): void {
    // Further implementations later (the actual backend logic)
    // For now, redirect to settings or show a message, or simply log.
    // Triggering "pay form" as requested.
    console.log('Upgrade membership triggered');
    // Navigation to settings where upgrade might be handled or specifically to a pay route if exists.
    if (this.user?.matricule) {
       this.router.navigate(['/home', this.user.matricule, 'settings']);
    }
  }

  gotoSettings(): void {
    if (this.user?.matricule) {
      this.router.navigate(['/home', this.user.matricule, 'settings']);
    }
  }

  translatePenaltyReason(code: string | undefined | null): string {
    if (!code) return 'Pénalité';
    const map: { [k: string]: string } = {
      'unpaid_balance': 'Dette impayée',
      'no_show': "Ne s'est pas présenté",
      'insufficient_players': "Réservation d'un match incomplet"
    };
    return map[code] ?? code;
  }

  translateAccountStatus(status: string | undefined | null): string {
    if (!status) return 'N/A';
    const s = status.toLowerCase();
    if (s === 'clear') return 'Apuré';
    if (s === 'debt') return 'Dette';
    return status;
  }
}
