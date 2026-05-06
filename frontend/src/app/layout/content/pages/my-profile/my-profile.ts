import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormGroup, FormControl, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { UserService } from '../../../../services/user.service';
import { AuthService } from '../../../../services/auth.service';
import { UserProfileDto } from '../../../../api/model/userProfileDto';
import { UserPenaltyDto } from '../../../../api/model/userPenaltyDto';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';
import { take } from 'rxjs/operators';
import { MigrationService} from '../../../../services/migration.service';
import { PayFormComponent } from '../../pay-form/pay-form';

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
  imports: [CommonModule, RouterModule, NavMenu, HomeAccountHeader, ReactiveFormsModule, PayFormComponent],
  templateUrl: './my-profile.html',
  styleUrl: './my-profile.css'
})
export class MyProfile implements OnInit {
  user: UserProfileDto | null = null;
  loading = true;
  error: string | null = null;

  showDebtPayment = false;
  debtAmount = 0;
  penaltyToClear: number | null = null;
  showDebtPaymentSuccess = false;

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

  // VIP Migration
  showVipTerms = false;
  showPayForm = false;
  showVipSuccess = false;
  vipExpirationDate: Date | null = null;
  paymentDetails: any = null;

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private migrationService: MigrationService
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
    this.showVipTerms = true;
    this.cdr.detectChanges();
  }

  cancelVipUpgrade(): void {
    this.showVipTerms = false;
    this.cdr.detectChanges();
  }

  acceptVipTerms(): void {
    this.showVipTerms = false;
    this.showPayForm = true;
    this.cdr.detectChanges();
  }

  onPaymentPaid(event: any): void {
    if (!this.user?.matricule) return;

    this.showPayForm = false; // Close the form immediately

    if (this.showDebtPayment) {
      // It's a debt payment
      this.userService.updateUserPenaltyAndAccount(this.user.matricule, this.penaltyToClear!, event.amount).subscribe({
        next: () => {
          this.showDebtPaymentSuccess = true;
          this.showDebtPayment = false; // Reset flag
          this.cdr.detectChanges();
          this.fetchUser(this.user!.matricule!); // Refresh user data
        },
        error: (err) => {
          this.error = "Le paiement de la dette a échoué.";
          console.error(err);
          this.showDebtPayment = false; // Reset flag on error
          this.cdr.detectChanges();
        }
      });
    } else {
      // It's a VIP payment
      const now = new Date();
      this.vipExpirationDate = new Date(now.setFullYear(now.getFullYear() + 1));

      this.paymentDetails = {
        amount: 99.00,
        description: `abonnement annuel ${this.user.matricule}`,
        expiration: this.vipExpirationDate,
        cardLast4: event.cardLast4
      };

      // Trigger backend migration
      this.migrationService.migrateToVip(this.user.matricule).subscribe({
        next: () => {
          this.showVipSuccess = true;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Migration failed', err);
          this.error = "La migration vers le statut VIP a échoué. Veuillez contacter le support.";
          this.cdr.detectChanges();
        }
      });
    }
  }

  onPaymentCancelled(): void {
    this.showPayForm = false;
    this.cdr.detectChanges();
  }

  payDebt(penalty: any): void {
    this.debtAmount = Math.abs(this.user?.account?.balance || 0);
    this.penaltyToClear = penalty.tr;
    this.showDebtPayment = true;
    this.showPayForm = true;
    this.cdr.detectChanges();
  }

  closeDebtSuccess(): void {
    this.showDebtPaymentSuccess = false;
  }

  closeVipSuccessAndLogout(): void {
    this.showVipSuccess = false;
    this.authService.logout();
    this.router.navigate(['/']).then(() => window.location.reload());
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

  getActivePenalties(): UserPenaltyDto[] {
    return (this.user?.penalties ?? []).filter(p => !!p.isActive);
  }

  hasActivePenalties(): boolean {
    return this.getActivePenalties().length > 0;
  }
}
