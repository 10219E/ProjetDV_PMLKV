import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';
import { UserService } from '../../../../services/user.service';
import { SiteService } from '../../../../services/site.service';
import { FieldService } from '../../../../services/field.service';
import { StatisticsService } from '../../../../services/statistics.service';
import { catchError, of, Observable } from 'rxjs';
import { FormsModule, ReactiveFormsModule, FormGroup, FormControl, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { UserFormComponent } from '../../user-form/user-form';
import { SiteDto, FieldDto, FinancialRecordDto } from '../../../../api';

export function passwordStrengthValidator(): ValidatorFn {
  const pattern = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@!\-+&$€])[A-Za-z0-9@!\-+&$€]{8,}$/;
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
  activeTab: 'users' | 'sites' | 'stats' = 'users';
  loading = false;

  users: any[] = [];
  sites: any[] = [];
  fieldsBySiteId: { [key: number]: any[] } = {};
  expandedSiteId: number | null = null;

  // Statistics
  financialRecords: FinancialRecordDto[] = [];
  loadingStats = false;
  userRole: number | null = null;
  userSites: any[] = [];

  // Site Toggle
  isTogglingSite = false;

  // Field Maintenance Modal
  showMaintenanceModal = false;
  selectedFieldForMaintenance: any = null;
  maintenanceForm = new FormGroup({
    fromDate: new FormControl('', [Validators.required]),
    toDate: new FormControl('', [Validators.required])
  });
  isSubmittingMaintenance = false;

  // Create Site Modal
  showSiteForm = false;
  siteForm = new FormGroup({
    name: new FormControl('', [Validators.required]),
    address: new FormControl('', [Validators.required]),
    openingTime: new FormControl('08:00:00', [Validators.required]),
    closingTime: new FormControl('22:00:00', [Validators.required])
  });
  isSubmittingSite = false;
  siteFormError: string | null = null;

  // Create Field Modal
  showFieldForm = false;
  selectedSiteIdForNewField: number | null = null;
  fieldForm = new FormGroup({
    isIndoor: new FormControl(true, [Validators.required])
  });
  isSubmittingField = false;

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

  statsFilters = {
    clientName: '',
    siteId: '',
    dateFrom: '',
    dateTo: '',
    status: 'clear'
  };

  constructor(
    private userService: UserService,
    private siteService: SiteService,
    private fieldService: FieldService,
    private statsService: StatisticsService,
    private cd: ChangeDetectorRef
  ) {
    this.setDefaultStatsDates();
  }

  ngOnInit() {
    this.loadData();
  }

  private setDefaultStatsDates() {
    const now = new Date();
    const lastMonth = new Date();
    lastMonth.setMonth(now.getMonth() - 1);

    this.statsFilters.dateFrom = lastMonth.toISOString().split('T')[0];
    this.statsFilters.dateTo = now.toISOString().split('T')[0];
  }

  loadData() {
    this.loading = true;

    // Load current user first to determine permissions
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

      this.userRole = Number(profile?.roleId ?? -1);
      this.userSites = profile?.sites || [];
      const isAllSites = [2, 9].includes(this.userRole);

      if (isAllSites) {
        // Load all sites for super admins
        this.siteService.getAllSites().pipe(
          catchError(() => of([]))
        ).subscribe((sites) => {
          this.sites = sites || [];
          this.loadAllFields();
        });

        // Load all users
        this.fetchUsers(this.userService.getAllUsers());
      } else {
        // Use assigned sites
        this.sites = profile?.sites || [];
        this.loadAllFields();

        // Load specific users if Site Admin
        if (this.userRole === 7 && this.sites.length > 0) {
          const primarySite = this.sites.find((s: any) => s.isPrimary) || this.sites[0];
          const siteId = primarySite.siteId || primarySite.id;
          this.fetchUsers(this.userService.getUsersForSite(siteId));
        } else {
          this.users = [];
          this.cd.detectChanges();
        }
      }
    });
  }

  loadFinancialStats() {
    this.loadingStats = true;
    this.financialRecords = []; // Reset current records to show loading state clearly
    let obs: Observable<FinancialRecordDto[]>;

    // If role is still null (async race condition), wait a bit and retry once
    if (this.userRole === null) {
      setTimeout(() => {
        if (this.userRole !== null) this.loadFinancialStats();
        else {
          this.loadingStats = false;
          this.cd.detectChanges();
        }
      }, 500);
      return;
    }

    if (this.userRole === 9) {
      obs = this.statsService.getFinancialReport();
    } else if (this.userRole === 7 && this.userSites.length > 0) {
      // For Site Admin, we use their primary site or first site
      const primarySite = this.userSites.find((s: any) => s.isPrimary) || this.userSites[0];
      obs = this.statsService.getFinancialReportBySite(primarySite.siteId || primarySite.id);
    } else {
      obs = of([]);
    }

    obs.pipe(
      catchError(err => {
        console.error('Failed to load stats', err);
        return of([]);
      })
    ).subscribe(data => {
      console.log('Statistics Data received:', data);

      // Handle both raw arrays and wrapped objects (OpenAPI standard)
      if (Array.isArray(data)) {
          this.financialRecords = [...data];
      } else if (data && typeof data === 'object') {
          // Check for common wrappers like object.body or records
          if (Array.isArray((data as any).body)) {
              this.financialRecords = [...(data as any).body];
          } else {
              // Try to iterate keys if it's an object acting like a map, but we expect an array
              this.financialRecords = [];
          }
      } else {
          this.financialRecords = [];
      }

      this.loadingStats = false;
      this.cd.detectChanges();
    });
  }

  get filteredFinancialRecords() {
    return this.financialRecords.filter(r => {
      if (this.statsFilters.status && r.status !== this.statsFilters.status) return false;
      if (this.statsFilters.clientName && !r.userFullName?.toLowerCase().includes(this.statsFilters.clientName.toLowerCase())) return false;
      if (this.statsFilters.siteId && r.siteName !== this.sites.find(s => String(s.siteId || s.id) === String(this.statsFilters.siteId))?.name) {
         // Fallback check if siteId doesn't match name directly
         const selectedSite = this.sites.find(s => String(s.siteId || s.id) === String(this.statsFilters.siteId));
         if (r.siteName !== (selectedSite?.name || selectedSite?.siteName)) return false;
      }
      if (this.statsFilters.dateFrom && r.paymentDate && new Date(r.paymentDate) < new Date(this.statsFilters.dateFrom)) return false;
      if (this.statsFilters.dateTo) {
        const toDate = new Date(this.statsFilters.dateTo);
        toDate.setHours(23, 59, 59);
        if (r.paymentDate && new Date(r.paymentDate) > toDate) return false;
      }
      return true;
    });
  }

  get totalFinancialRevenue(): number {
    return this.filteredFinancialRecords.reduce((acc, curr) => acc + (curr.amount || 0), 0);
  }

  switchTab(tab: 'users' | 'sites' | 'stats') {
    this.activeTab = tab;
    if (tab === 'stats') {
      // Ensure we have user role before loading stats, or reload everything
      if (this.userRole === null) {
        this.loadData();
      } else {
        this.loadFinancialStats();
      }
    }
    this.cd.detectChanges();
  }

  private fetchUsers(source: Observable<any[]>) {
    source.pipe(
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

  toggleSiteActiveStatus(site: any) {
    const sid = site.siteId || site.id;
    if (!sid) return;

    this.isTogglingSite = true;
    const previousStatus = site.isActive;
    site.isActive = !site.isActive;

    const siteUpdate: SiteDto = {
      isActive: site.isActive
    };

    this.siteService.updateSite(sid, siteUpdate).subscribe({
      next: () => {
        this.isTogglingSite = false;
        this.cd.detectChanges();
      },
      error: () => {
        site.isActive = previousStatus;
        this.isTogglingSite = false;
        alert("Erreur lors de la mise à jour du statut du site.");
        this.cd.detectChanges();
      }
    });
  }

  openMaintenanceModal(field: any) {
    this.selectedFieldForMaintenance = field;
    this.showMaintenanceModal = true;
    this.maintenanceForm.reset({
      fromDate: field.maintenanceFromDate || '',
      toDate: field.maintenanceToDate || ''
    });
    this.cd.detectChanges();
  }

  closeMaintenanceModal() {
    this.showMaintenanceModal = false;
    this.selectedFieldForMaintenance = null;
    this.cd.detectChanges();
  }

  submitMaintenance() {
    if (this.maintenanceForm.invalid || !this.selectedFieldForMaintenance) return;

    this.isSubmittingMaintenance = true;
    const fid = this.selectedFieldForMaintenance.fieldId || this.selectedFieldForMaintenance.id;
    const fieldUpdate: any = {
      maintenanceFromDate: this.maintenanceForm.value.fromDate || null,
      maintenanceToDate: this.maintenanceForm.value.toDate || null
    };

    this.fieldService.updateField(fid, fieldUpdate).subscribe({
      next: () => {
        this.selectedFieldForMaintenance.maintenanceFromDate = fieldUpdate.maintenanceFromDate;
        this.selectedFieldForMaintenance.maintenanceToDate = fieldUpdate.maintenanceToDate;
        this.isSubmittingMaintenance = false;
        this.closeMaintenanceModal();
        this.cd.detectChanges();
      },
      error: () => {
        this.isSubmittingMaintenance = false;
        alert("Erreur lors de la mise à jour des dates de maintenance.");
        this.cd.detectChanges();
      }
    });
  }

  cancelMaintenance(field: any) {
    const fid = field.fieldId || field.id;

    // Use 1970-01-01 as a magic date to signal the backend to set maintenance dates to null
    const resetDate = "1970-01-01";

    const fieldUpdate: any = {
      maintenanceFromDate: resetDate,
      maintenanceToDate: resetDate
    };

    this.fieldService.updateField(fid, fieldUpdate).subscribe({
      next: () => {
        field.maintenanceFromDate = null;
        field.maintenanceToDate = null;
        this.cd.detectChanges();
      },
      error: () => {
        alert("Erreur lors de l'annulation de la maintenance.");
        this.cd.detectChanges();
      }
    });
  }

  openSiteForm() {
    this.showSiteForm = true;
    this.siteFormError = null;
    this.siteForm.reset({
      openingTime: '08:00:00',
      closingTime: '22:00:00'
    });
    this.cd.detectChanges();
  }

  closeSiteForm() {
    this.showSiteForm = false;
    this.cd.detectChanges();
  }

  submitSite() {
    if (this.siteForm.invalid) return;

    this.isSubmittingSite = true;
    this.siteFormError = null;

    // We send time strings "HH:mm:ss" as the backend Jackson expects strings or arrays for LocalTime
    const opening = this.siteForm.value.openingTime || '08:00:00';
    const closing = this.siteForm.value.closingTime || '22:00:00';

    // Ensure they have the seconds part if only HH:mm is provided
    const formattedOpening = opening.split(':').length === 2 ? `${opening}:00` : opening;
    const formattedClosing = closing.split(':').length === 2 ? `${closing}:00` : closing;

    const siteDto: SiteDto = {
      name: this.siteForm.value.name!,
      address: this.siteForm.value.address!,
      openingTime: formattedOpening as any, // Cast to any because SiteDto interface expects LocalTime object
      closingTime: formattedClosing as any,
      isActive: true
    };

    this.siteService.createSite(siteDto).subscribe({
      next: () => {
        this.isSubmittingSite = false;
        this.closeSiteForm();
        this.loadData(); // Reload sites
      },
      error: (err) => {
        this.isSubmittingSite = false;
        this.siteFormError = err.error?.message || "Erreur lors de la création du site.";
        this.cd.detectChanges();
      }
    });
  }

  openFieldForm(siteId: number) {
    this.selectedSiteIdForNewField = siteId;
    this.showFieldForm = true;
    this.fieldForm.reset({
      isIndoor: true
    });
    this.cd.detectChanges();
  }

  closeFieldForm() {
    this.showFieldForm = false;
    this.selectedSiteIdForNewField = null;
    this.cd.detectChanges();
  }

  submitField() {
    if (this.fieldForm.invalid || !this.selectedSiteIdForNewField) return;

    this.isSubmittingField = true;
    const fieldDto: FieldDto = {
      siteId: this.selectedSiteIdForNewField,
      isIndoor: this.fieldForm.value.isIndoor!,
      isActive: true
    };

    this.fieldService.createField(fieldDto).subscribe({
      next: () => {
        this.isSubmittingField = false;
        this.closeFieldForm();
        this.loadAllFields(); // Reload fields
      },
      error: () => {
        this.isSubmittingField = false;
        alert("Erreur lors de la création du terrain.");
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
