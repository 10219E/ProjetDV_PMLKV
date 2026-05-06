import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormGroup, FormControl, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { UserService } from '../../../../services/user.service';
import { AuthService } from '../../../../services/auth.service';
import { UserProfileDto } from '../../../../api/model/userProfileDto';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';
import { take } from 'rxjs/operators';

export function passwordsMatchValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const password = control.get('newPassword');
    const confirmPassword = control.get('confirmPassword');
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      return { passwordsMismatch: true };
    }
    return null;
  };
}

export function passwordStrengthValidator(): ValidatorFn {
  const pattern = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@!\-\+&\$€])[A-Za-z0-9@!\-\+&\$€]{8,}$/;
  return (control: AbstractControl): ValidationErrors | null => {
    const v = control.value as string | null | undefined;
    if (!v) return { weakPassword: true };
    return pattern.test(v) ? null : { weakPassword: true };
  };
}

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, NavMenu, HomeAccountHeader, ReactiveFormsModule],
  templateUrl: './my-profile.html',
  styleUrl: './my-profile.css'
})
export class MyProfile implements OnInit {
  user: UserProfileDto | null = null;
  loading = true;
  error: string | null = null;

  // Toggle password visibility
  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;

  // Password change form
  showPasswordForm = false;
  passwordForm = new FormGroup({
    currentPassword: new FormControl('', [Validators.required]),
    newPassword: new FormControl('', [Validators.required, Validators.minLength(8), passwordStrengthValidator()]),
    confirmPassword: new FormControl('', [Validators.required])
  }, { validators: passwordsMatchValidator() });

  passwordChangeError: string | null = null;
  showPasswordSuccess = false;

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

  // Password management
  openPasswordForm(): void {
    this.showPasswordForm = true;
    this.passwordForm.reset();
    this.passwordChangeError = null;
    this.cdr.detectChanges();
  }

  closePasswordForm(): void {
    this.showPasswordForm = false;
    this.passwordChangeError = null;
    this.cdr.detectChanges();
  }

  submitPasswordChange(): void {
    if (this.passwordForm.invalid || !this.user?.matricule) return;

    const { currentPassword, newPassword } = this.passwordForm.value;
    this.passwordChangeError = null;

    // 1. Verify current password with auth service
    // Corrected: AuthLoginDto uses 'login' property for the identifier (email/matricule)
    this.authService.login({ login: this.user.email ?? '', password: currentPassword ?? '' }).subscribe({
      next: () => {
        // Current password is correct, now update it
        this.userService.updateUserInBackend(this.user!.matricule!, { password: newPassword }).subscribe({
          next: () => {
            this.showPasswordForm = false;
            this.showPasswordSuccess = true;
            this.cdr.detectChanges();
          },
          error: (err) => {
            this.passwordChangeError = "Erreur lors de la mise à jour du mot de passe.";
            this.cdr.detectChanges();
          }
        });
      },
      error: () => {
        this.passwordChangeError = "Mot de passe actuel incorrect.";
        this.cdr.detectChanges();
      }
    });
  }

  toggleCurrentPassword(): void {
    this.showCurrentPassword = !this.showCurrentPassword;
    this.cdr.detectChanges();
  }

  toggleNewPassword(): void {
    this.showNewPassword = !this.showNewPassword;
    this.cdr.detectChanges();
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
    this.cdr.detectChanges();
  }

  closeSuccessAndLogout(): void {
    this.showPasswordSuccess = false;
    this.authService.logout();
    this.router.navigate(['/']).then(() => window.location.reload());
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
