import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';
import { UserService } from '../../../../services/user.service';
import { SiteService } from '../../../../services/site.service';
import { FieldService } from '../../../../services/field.service';
import { catchError, of } from 'rxjs';
import { FormsModule, ReactiveFormsModule, FormGroup, FormControl, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { UserFormComponent } from '../../user-form/user-form';

export function passwordStrengthValidator(): ValidatorFn {
  const pattern = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@!\-\+&\$€])[A-Za-z0-9@!\-\+&\$€]{8,}$/;
  return (control: AbstractControl): ValidationErrors | null => {
    const v = control.value as string | null | undefined;
    if (!v) return { weakPassword: true };
    return pattern.test(v) ? null : { weakPassword: true };
  };
}

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, RouterModule, NavMenu, HomeAccountHeader, FormsModule, ReactiveFormsModule, UserFormComponent],
  templateUrl: './admin.html'
})
export class AdminComponent implements OnInit {
  activeTab: 'users' | 'sites' = 'users';
  loading = false;

  users: any[] = [];
  sites: any[] = [];
  fieldsBySiteId: { [key: number]: any[] } = {};
  expandedSiteId: number | null = null;

  // Password reset Modal state
  showPasswordModal = false;
  selectedUserIdForPassword: string | null = null;
  selectedUserEmailForPassword: string | null = null;
  passwordForm = new FormGroup({
    newPassword: new FormControl('', [Validators.required, Validators.minLength(8), passwordStrengthValidator()]),
  });
  passwordUpdateMessage: string | null = null;
  passwordUpdateError = false;
  isSubmittingPassword = false;

  showUserForm = false;

  userFilters: {
    name: string;
    email: string;
    siteId: string;
    isActive: string;
    hasDebt: boolean;
    hasPenalty: boolean;
  } = {
    name: '',
    email: '',
    siteId: '',
    isActive: '',
    hasDebt: false,
    hasPenalty: false
  };

  constructor(
    private userService: UserService,
    private siteService: SiteService,
    private fieldService: FieldService,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.loading = true;

    // Load Users
    this.userService.getAllUsers().pipe(
      catchError((err) => {
        console.error('Failed to load users', err);
        return of([]);
      })
    ).subscribe((users) => {
      this.users = users || [];

      // Pin Admins to top (SA = 9, SITE = 7)
      this.users.sort((a, b) => {
        const roleA = Number(a.roleId);
        const roleB = Number(b.roleId);
        const aIsAdmin = roleA === 9 || roleA === 7;
        const bIsAdmin = roleB === 9 || roleB === 7;

        if (aIsAdmin && !bIsAdmin) return -1;
        if (!aIsAdmin && bIsAdmin) return 1;
        if (aIsAdmin && bIsAdmin && roleA !== roleB) {
          return roleA === 9 ? -1 : 1; // SA before SITE
        }
        return 0;
      });

      this.cd.detectChanges();
    });

    // Load Sites & Fields based on user role
    this.userService.getCurrentUser().pipe(
      catchError((err) => {
        console.error('Failed to load current user', err);
        return of(null);
      })
    ).subscribe((profile) => {
      if (!profile) {
        this.loading = false;
        this.cd.detectChanges();
        return;
      }

      const roleId = profile?.roleId ?? -1;
      const isAllSites = [2, 9].includes(Number(roleId)) || (profile?.sites && profile.sites.some((s: any) => s.isVip));

      if (isAllSites) {
        this.siteService.getAllSites().pipe(
          catchError(() => of([]))
        ).subscribe((sites) => {
          this.sites = sites || [];
          this.loadAllFields();
        });
      } else {
        this.sites = profile?.sites || [];
        this.loadAllFields(); // Load fields for their assigned sites
      }
    });
  }

  loadAllFields() {
    this.fieldService.fetchAllFields().pipe(
      catchError(() => of([]))
    ).subscribe((fields) => {
      this.fieldsBySiteId = {};
      (fields || []).forEach((f: any) => {
        const sid = f.siteId ?? f.site?.siteId;
        if (sid) {
          if (!this.fieldsBySiteId[sid]) this.fieldsBySiteId[sid] = [];
          this.fieldsBySiteId[sid].push(f);
        }
      });
      this.loading = false;
      this.cd.detectChanges();
    });
  }

  toggleSite(siteId: number) {
    if (this.expandedSiteId === siteId) {
      this.expandedSiteId = null;
    } else {
      this.expandedSiteId = siteId;
    }
  }

  toggleUserActiveStatus(user: any) {
    const matricule = user.matricule || user.id;
    if (!matricule) return;

    // Optimistic UI update
    const previousStatus = user.isActive;
    user.isActive = !user.isActive;

    this.userService.updateUserInBackend(matricule, { isActive: user.isActive }).subscribe({
      next: () => {
        // Success
        this.cd.detectChanges();
      },
      error: () => {
        // Revert on error
        user.isActive = previousStatus;
        alert("Erreur lors de la mise à jour du statut de l'utilisateur.");
        this.cd.detectChanges();
      }
    });
  }

  openPasswordModal(user: any) {
    this.selectedUserIdForPassword = user.matricule || user.id;
    this.selectedUserEmailForPassword = user.email;
    this.showPasswordModal = true;
    this.passwordForm.reset();
    this.passwordUpdateMessage = null;
    this.passwordUpdateError = false;
    this.isSubmittingPassword = false;
    this.cd.detectChanges();
  }

  closePasswordModal() {
    this.showPasswordModal = false;
    this.selectedUserIdForPassword = null;
    this.selectedUserEmailForPassword = null;
    this.isSubmittingPassword = false;
    this.cd.detectChanges();
  }

  openUserForm() {
    this.showUserForm = true;
    this.cd.detectChanges();
  }

  closeUserForm() {
    this.showUserForm = false;
    this.cd.detectChanges();
  }

  onUserAdded(_newUser?: any) {
    this.closeUserForm();
    window.location.reload();
  }

  get filteredUsers() {
    return this.users.filter(u => {
      if (this.userFilters.name) {
        const fullName = `${u.firstName || ''} ${u.lastName || ''}`.toLowerCase();
        if (!fullName.includes(this.userFilters.name.toLowerCase())) return false;
      }
      if (this.userFilters.email) {
        const mail = (u.email || '').toLowerCase();
        if (!mail.includes(this.userFilters.email.toLowerCase())) return false;
      }
      if (this.userFilters.siteId) {
        if (!u.sites || !u.sites.some((s: any) => String(s.siteId || s.id) === String(this.userFilters.siteId))) return false;
      }
      if (this.userFilters.isActive !== '') {
        const isAct = this.userFilters.isActive === 'true';
        if (u.isActive !== isAct) return false;
      }
      if (this.userFilters.hasDebt) {
        if (!u.account || typeof u.account.balance !== 'number' || u.account.balance >= 0) return false;
      }
      if (this.userFilters.hasPenalty) {
        if (!u.penalties || u.penalties.length === 0) return false;
      }
      return true;
    });
  }

  submitPasswordChange() {
    if (this.passwordForm.invalid || !this.selectedUserIdForPassword) return;

    this.isSubmittingPassword = true;
    this.passwordUpdateMessage = null;
    this.passwordUpdateError = false;

    const newPassword = this.passwordForm.value.newPassword;
    this.userService.updateUserInBackend(this.selectedUserIdForPassword, { password: newPassword }).subscribe({
      next: () => {
        this.passwordUpdateMessage = "Le mot de passe a été mis à jour avec succès.";
        this.passwordUpdateError = false;
        // Keep isSubmittingPassword true to keep the button disabled during the timeout
        this.cd.detectChanges();
        setTimeout(() => this.closePasswordModal(), 1500);
      },
      error: () => {
        this.passwordUpdateMessage = "Erreur lors de la mise à jour du mot de passe.";
        this.passwordUpdateError = true;
        this.isSubmittingPassword = false;
        this.cd.detectChanges();
      }
    });
  }
}
