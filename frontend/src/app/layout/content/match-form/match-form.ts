import { Component, Input, OnInit, ChangeDetectorRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators, FormArray } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { MatchCreationControllerService } from '../../../api/api/matchCreationController.service';
import { SiteControllerService } from '../../../api/api/siteController.service';
import { AuthService } from '../../../services/auth.service';
import { UserService } from '../../../services/user.service';
import { SessionService } from '../../../services/session.service';
import { MatchService } from '../../../services/match.service';
import { AvailabilityControllerService } from '../../../api/api/availabilityController.service';
import { Router } from '@angular/router';
import { MatchCal } from '../match-cal/match-cal';
import { UserFormComponent } from '../user-form/user-form';
import { PayFormComponent } from '../pay-form/pay-form';
import { PayService } from '../../../services/pay.service';
import {FieldService} from '../../../services/field.service';
import {SimpleInviteDto} from '../../../api';
import {InviteService} from '../../../services/invite.service';


@Component({
  selector: 'app-match-form',
  standalone: true,
    imports: [CommonModule, ReactiveFormsModule, MatchCal, UserFormComponent, PayFormComponent],
  templateUrl: './match-form.html',
  styleUrls: ['./match-form.css']
})
export class MatchForm implements OnInit {
  @ViewChild(MatchCal) matchCalComponent!: MatchCal;

  @Input() organiserId?: string | null;
  @Input() organiserName?: string | null;
  @Input() defaultType?: string | null; // e.g. 'private' or 'public'
  @Input() hideOrganiser?: boolean | null;
  @Input() hideInvites?: boolean | null;
  @Input() stayOnPageAfterSuccess?: boolean | null;
  @Input() editMode = false;

  fields: any[] = [];
  // all fields loaded from server (unfiltered). `fields` is the currently displayed list after site filtering.
  fieldsAll: any[] = [];
  // list of sites the user can choose from
  sites: any[] = [];
  // allowed site ids derived from profile or /api/sites
  allowedSiteIds?: number[] | undefined;
  loading = false;
  error: string | null = null;
  // legacy inline success message (kept for compatibility) - prefer popupMessage for modal
  successMessage: string | null = null;
  // message shown inside the confirmation popup only
  popupMessage: string | null = null;
  // show a simple confirmation popup after successful creation
  showSuccessDialog = false;

  form = new FormGroup({
   siteId: new FormControl<number | null>({value: null, disabled: false}, [Validators.required]),
   fieldId: new FormControl<number | null>(null, [Validators.required]),
   type: new FormControl<string | null>(null),
    matchDate: new FormControl<string | null>(null, [Validators.required]),
    // matchDate must be chosen after a field is selected; startTime is disabled until a date is set
    startTime: new FormControl<string | null>({value: null, disabled: true}, [Validators.required]),
   endTime: new FormControl<string | null>({value: null, disabled: true}, [Validators.required]),
   organiserId: new FormControl<string | null>(null, [Validators.required]),
   invites: new FormArray([
     // use the same strong email validation as `user-form` (email + pattern for domain suffix)
     new FormControl<string | null>(null, [Validators.email, Validators.pattern(/^[^\s@]+@[^\s@]+\.[a-zA-Z]{2,6}$/)]),
     new FormControl<string | null>(null, [Validators.email, Validators.pattern(/^[^\s@]+@[^\s@]+\.[a-zA-Z]{2,6}$/)]),
     new FormControl<string | null>(null, [Validators.email, Validators.pattern(/^[^\s@]+@[^\s@]+\.[a-zA-Z]{2,6}$/)])
   ])
  });

  // calendar overlay state (do not prompt overlay on form load)
  showCalendarOverlay = false;
  tempSelectedDate: Date | null = null;
  dateReadOnly = false;

  // sessions for the currently selected Field (normalized for UI: _start/_end labels)
  sessionsForField: any[] = [];
  private updatingFromSession = false;

  // per-invite validation state (idle, checking, found, not_found, error)
  inviteStates: Array<{ status: 'idle' | 'checking' | 'found' | 'not_found' | 'error', user?: any }> = [
    { status: 'idle' },
    { status: 'idle' },
    { status: 'idle' }
  ];

  // current user info (used to prevent inviting self or admins)
  currentUserEmail?: string | null;
  currentUserMatricule?: string | null;
  currentUserRoleId?: number | null;
  // timers for invite lookups to avoid indefinite spinner
  private inviteTimeouts: Array<any> = [null, null, null];

  // user-form overlay state
  showUserForm = false;
  userFormPrefillEmail?: string | null = null;
  userFormInviteIndex: number | null = null;
  // pay form overlay state
  showPayForm = false;
  payAmount = 0;
  // DTO stored while waiting for payment
  private pendingDto: any | null = null;

  constructor(private fieldService: FieldService, private matchCreationService: MatchCreationControllerService, private siteController: SiteControllerService, private authService: AuthService, private userService: UserService, private sessionService: SessionService, private availabilityService: AvailabilityControllerService, private matchService: MatchService, private router: Router, private cd: ChangeDetectorRef, private payService: PayService, private inviteService: InviteService) {}
  // keep a direct reference to PayFormComponent to satisfy analyzers that the imported component is used
  // (template uses <app-pay-form> conditionally with @if which some static analyzers may not detect)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  private _payFormRef = PayFormComponent;

  ngOnInit(): void {
    // ensure calendar overlay is not prompted on initial form load
    const existingDate = this.form.get('matchDate')?.value;
    this.showCalendarOverlay = false;
    this.dateReadOnly = !!existingDate;

    // Disable date selection until a field is selected
    if (!this.form.get('fieldId')?.value) {
      this.form.get('matchDate')?.disable();
      // startTime already initialized as disabled; ensure endTime is disabled as well
      this.form.get('endTime')?.disable();
    }

    // prefill organiser if provided and fetch organiser name (first + last only)
    if (this.organiserId) {
      this.form.get('organiserId')?.setValue(this.organiserId);
      // try to fetch user profile (will set organiserName to "First Last" only)
      try {
        this.userService.getUserById(this.organiserId).subscribe({
          next: (u: any) => {
            const fname = (u && u.firstName) || '';
            const lname = (u && u.lastName) || '';
            const full = (fname + ' ' + lname).trim();
            if (full) {
              this.organiserName = full;
              this.cd.detectChanges();
            }
          },
          error: () => {
            // ignore; organiserName may be provided by parent
          }
        });
      } catch (e) {
        // ignore
      }
    }

    // If parent requests organiser to be hidden, remove requirement and disable control
    if (this.hideOrganiser) {
      try {
        const org = this.form.get('organiserId');
        if (org) {
          org.clearValidators();
          org.setValue(null);
          org.disable({ emitEvent: false });
          org.updateValueAndValidity({ emitEvent: false });
        }
      } catch (e) {
        // ignore
      }
    }

    // prefill type if provided and disable changing it
    if (this.defaultType) {
      this.form.get('type')?.setValue(this.defaultType);
      this.form.get('type')?.disable();
    }

    // Add validators to invite fields for private matches
    // always ensure email syntax validator is present; for private matches also require the field
    const invitesArray = this.form.get('invites') as FormArray;
    const emailPattern = Validators.pattern(/^[^\s@]+@[^\s@]+\.[a-zA-Z]{2,6}$/);
    // if hideInvites flag is provided, disable invite controls entirely
    if (this.hideInvites) {
      invitesArray.controls.forEach(control => { control.clearValidators(); control.setValue(null); control.disable({ emitEvent: false }); control.updateValueAndValidity({ emitEvent: false }); });
    } else if (this.isPrivate()) {
      invitesArray.controls.forEach(control => {
        control.setValidators([Validators.required, Validators.email, emailPattern]);
        control.updateValueAndValidity();
      });
    } else {
      // ensure email + pattern validator remains applied (non-required)
      invitesArray.controls.forEach(control => {
        control.setValidators([Validators.email, emailPattern]);
        control.updateValueAndValidity();
      });
    }

    // Manage invite controls' enabled state from the component (avoid template [disabled] binding).
    // If no site is selected initially, keep invite inputs disabled; enable them when a site is chosen.
    const siteSelected = !!this.form.get('siteId')?.value;
    invitesArray.controls.forEach(control => {
      if (siteSelected) {
        control.enable({ emitEvent: false });
      } else {
        control.disable({ emitEvent: false });
      }
    });

    // Reset invite validation UI/state when the user edits the invite input.
    // If the user changes the email text, clear any previous 'found'/'not_found' message
    // and reset the button/status to idle so they can re-validate the new value.
    invitesArray.controls.forEach((control, idx) => {
      control.valueChanges.subscribe(() => {
        // only change state if it's not already 'checking' to avoid interrupting an in-flight check
        if (this.inviteStates[idx]?.status !== 'checking') {
          this.inviteStates[idx] = { status: 'idle' };
        }
        // if user cleared the input, ensure the control is untouched so UI validation messages hide
        if (!control.value) {
          control.markAsUntouched();
        }
        // re-run cross-field validation: uniqueness and self-invite checks
        this.runInviteCrossValidation();
        this.cd.detectChanges();
      });
    });

    // keep endTime disabled (greyed) and set placeholder via template; we'll still set its value programmatically
    this.form.get('endTime')?.disable();

    // react to site selection changes -> fetch fields for the selected site
    this.form.get('siteId')?.valueChanges.subscribe((siteId) => {
      const id = siteId ? Number(siteId) : null;
      if (!id) {
        // disable invite inputs when no site selected
        invitesArray.controls.forEach(control => { control.disable(); control.updateValueAndValidity(); });
        this.fields = [];
        this.sessionsForField = [];
        // clear date and times when site is deselected
        this.form.get('matchDate')?.setValue(null);
        this.form.get('startTime')?.setValue(null);
        this.form.get('endTime')?.setValue(null);
        this.form.get('fieldId')?.setValue(null);
        this.tempSelectedDate = null;
        this.dateReadOnly = false;
        return;
      }
      // When changing the selected site (complexe sportif), clear date and start time
      this.form.get('matchDate')?.setValue(null);
      this.form.get('startTime')?.setValue(null);
      this.form.get('endTime')?.setValue(null);
      // clear selected field and sessions so user picks a field for the new site
      this.form.get('fieldId')?.setValue(null);
      this.sessionsForField = [];
      this.tempSelectedDate = null;
      this.dateReadOnly = false;
      // fetch fields only; sessions will be loaded when a field is selected
      this.fieldService.fetchFieldsBySite(id).subscribe({
        next: (data: any[]) => {
          this.fields = data || [];
          this.cd.detectChanges();
          // enable invite inputs now that a site is selected
          invitesArray.controls.forEach(control => { control.enable(); control.updateValueAndValidity(); });
        },
        error: (err) => {
          console.error('Failed to load fields for site', id, err);
          this.fields = [];
        }
      });
    });

    // when user selects a start time (from dropdown), populate endTime based on the session with matching _start
    this.form.get('startTime')?.valueChanges.subscribe((val) => {
      if (this.updatingFromSession) return;
      if (!val) {
        this.form.get('endTime')?.setValue(null);
        // keep endTime disabled until a valid startTime is chosen
        this.form.get('endTime')?.disable();
        return;
      }
      const session = (this.sessionsForField || []).find(s => s._start === val);
      if (!session) {
        this.form.get('endTime')?.setValue(null);
        this.form.get('endTime')?.disable();
        return;
      }
      this.updatingFromSession = true;
      if (session.fieldId) this.form.get('fieldId')?.setValue(session.fieldId);
      if (session.siteId) this.form.get('siteId')?.setValue(session.siteId);
      if (session._end) this.form.get('endTime')?.setValue(session._end);
      // enable endTime now that a startTime (and matching session) is selected
      this.form.get('endTime')?.enable();
      if (session.startedAt) {
        const sd = this.parseDateTime(session.startedAt);
        if (sd) this.form.get('matchDate')?.setValue(this.formatDateForInput(sd));
      }
      this.updatingFromSession = false;

      // Check collision for organiser
      const organiserId = this.form.get('organiserId')?.value;
      const matchDate = this.form.get('matchDate')?.value;
      if (organiserId && matchDate && val) {
        this.matchService.getCollidingMatches(String(organiserId), matchDate, val).subscribe({
          next: (isColliding) => {
            if (isColliding) {
              this.form.get('startTime')?.setValue(null, { emitEvent: false });
              this.form.get('endTime')?.setValue(null, { emitEvent: false });
              this.form.get('startTime')?.setErrors({ required: true, collidingOrganiser: true });
              this.form.get('startTime')?.markAsTouched();
              this.cd.detectChanges();
            }
          }
        });
      }
    });

    // Load sessions when a field is selected (instead of when a site is selected)
    this.form.get('fieldId')?.valueChanges.subscribe((fieldId) => {
      // If we're updating the form from a session selection, avoid clearing state
      if (this.updatingFromSession) return;
      const fid = fieldId ? Number(fieldId) : null;
      // When the user explicitly changes the field, clear only start/end times
      // (preserve the selected date per requested behaviour)
      this.form.get('startTime')?.setValue(null);
      this.form.get('endTime')?.setValue(null);
      // When a field is selected, enable the date picker. When no field is selected, disable date and times.
      if (!fid) {
        // disable date selection until a field is chosen
        this.form.get('matchDate')?.setValue(null);
        this.form.get('matchDate')?.disable();
        // disable time selection until a date is chosen
        this.form.get('startTime')?.disable();
        this.form.get('endTime')?.disable();
      } else {
        this.form.get('matchDate')?.enable();
        this.error = null;
        // startTime remains disabled until a date is chosen
        this.form.get('startTime')?.disable();
        this.form.get('endTime')?.disable();
      }

      if (!fid) {
        this.sessionsForField = [];
        return;
      }

      // When a field is selected, we don't immediately load sessions.
      // Sessions will be loaded when a date is selected via updateSessionsBasedOnDate()
      this.sessionsForField = [];

      // try to find the field in the currently loaded fields to get its siteId
      const fld = (this.fields || []).find((f: any) => Number(f.fieldId) === Number(fid));
      const siteId = fld?.siteId ? Number(fld.siteId) : Number(this.form.get('siteId')?.value) || null;

    });

    // React to date changes - do NOT clear the selected field when the user changes the date.
    // Instead enable startTime only after a date is set. If the change originates from a session
    // selection, we still update sessions based on the date.
    this.form.get('matchDate')?.valueChanges.subscribe((date) => {
      // If the date change originates from a session selection, do not clear the field
      if (this.updatingFromSession) {
        // ensure startTime is enabled when session set a date
        if (date) this.form.get('startTime')?.enable();
        this.updateSessionsBasedOnDate();
        return;
      }
      // Do not clear the fieldId when the user changes the date (requested behaviour)
      // Clear only start/end times and enable startTime when a valid date is present
      this.form.get('startTime')?.setValue(null);
      this.form.get('endTime')?.setValue(null);
      if (date) {
        this.form.get('startTime')?.enable();
      } else {
        this.form.get('startTime')?.disable();
        this.form.get('endTime')?.disable();
      }
      this.updateSessionsBasedOnDate();
    });

    // Load user's accessible sites and then load fields filtered by those sites.
        this.userService.getCurrentUser().subscribe({
          next: (profile: any) => {

            const roleId = profile?.roleId ?? -1;
            // role ids that grant access to all sites: ALL_SITE_ACCESS(2), ADMIN(9)
            // NOTE: SITE_ADMIN (7) should NOT be treated as 'all-sites' here — site_admins must be bound to their site(s)
            const isAllSites = [2, 9].includes(Number(roleId)) || (profile?.sites && profile.sites.some((s: any) => s.isVip));
            if (isAllSites) {
              // fetch all sites
              // ensure Authorization header is set on the site controller
              this.sessionService.setAuthHeader(this.siteController);
              this.siteController.getAllSites(true).subscribe({
                next: (sites: any[]) => {
                  this.sites = sites || [];
                        // store current user info if present on profile
                        if (profile) {
                          this.currentUserEmail = profile.email ?? null;
                          this.currentUserMatricule = profile.matricule ?? null;
                          this.currentUserRoleId = profile.roleId ?? null;
                        }
                  const allowed = (this.sites || []).map(s => s.siteId).filter((id: any) => id !== undefined && id !== null);
                  this.loadFieldsForAllowedSites(allowed);
                },
                error: (err) => {
                  console.error('Failed to load sites for all-site user', err);
                  // fallback: load all fields (unfiltered) so user can still use the form
                  this.sites = [];
                  this.loadFieldsForAllowedSites(undefined);
                }
              });
            } else {
              // use sites from user profile
                  this.sites = profile?.sites || [];
                  // store current user info from profile
                  this.currentUserEmail = profile?.email ?? null;
                  this.currentUserMatricule = profile?.matricule ?? null;
                  this.currentUserRoleId = profile?.roleId ?? null;
              const userSites = (this.sites || []).map((s: any) => s.siteId).filter((id: any) => id !== undefined && id !== null);
              this.loadFieldsForAllowedSites(userSites);
              // Site selection and disabling is now handled within loadFieldsForAllowedSites
            }
          },
          error: (err) => {
            console.error('Failed to load current user profile', err);
            // fallback: load all fields unfiltered
            this.loadFieldsForAllowedSites(undefined);
          }
        });


  }

  // Load fields and filter by allowed site ids. If allowedSiteIds is undefined, do not filter (load all fields)
  private loadFieldsForAllowedSites(allowedSiteIds?: number[] | undefined): void {
    // store allowed site ids and if there is exactly one allowed site, preselect it and load fields for it
    this.allowedSiteIds = allowedSiteIds;
    if (!allowedSiteIds || allowedSiteIds.length === 0) {
      // no allowed sites known -> clear sites/fields
      this.fields = [];
      return;
    }

    if (allowedSiteIds.length === 1) {
      const only = Number(allowedSiteIds[0]);
      this.form.get('siteId')?.setValue(only);
      this.form.get('siteId')?.disable();
      // this will trigger the valueChanges handler and load fields via getFieldsBySite
    } else {
      // For multiple sites, enable the site selection
      this.form.get('siteId')?.enable();
      // do not load fields until the user selects a site (valueChanges will handle it)
    }
  }



  // Update sessionsForField based on whether a date is selected
  private updateSessionsBasedOnDate(): void {
    const matchDate = this.form.get('matchDate')?.value;
    const fid = Number(this.form.get('fieldId')?.value) || null;

    // If no field selected, clear sessions (we load sessions when a field is selected)
    if (!fid) {
      this.sessionsForField = [];
      if (!matchDate) {
        this.form.get('startTime')?.setValue(null);
        this.form.get('endTime')?.setValue(null);
      }
      this.cd.detectChanges();
      return;
    }

    // If no date selected, clear sessions but don't fetch
    if (!matchDate) {
      this.sessionsForField = [];
      this.form.get('startTime')?.setValue(null);
      this.form.get('endTime')?.setValue(null);
      this.cd.detectChanges();
      return;
    }

    // find site's id from loaded fields if possible
    const fld = (this.fields || []).find((f: any) => Number(f.fieldId) === Number(fid));
    const siteId = fld?.siteId ? Number(fld.siteId) : Number(this.form.get('siteId')?.value) || null;
    if (!siteId) {
      this.sessionsForField = [];
      this.cd.detectChanges();
      return;
    }

    // Use the new API endpoint to get available sessions for the specific field and date
    this.sessionService.loadSessionsForField(siteId, fid, matchDate).subscribe({
      next: (sessions) => {
        this.sessionsForField = sessions || [];
        this.cd.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load sessions for field', fid, 'on date', matchDate, err);
        this.sessionsForField = [];
        this.cd.detectChanges();
      }
    });
  }

  onDateSelected(date: Date | null): void {
    this.tempSelectedDate = date;
  }

  onCalendarConfirm(date: Date): void {
    if (!date) return;
    this.tempSelectedDate = date;
    this.confirmDate();
  }

  onCalendarCancel(): void {
    // simply hide overlay without setting a date
    this.showCalendarOverlay = false;
  }

  private formatDateForInput(d: Date): string {
    const y = d.getFullYear();
    const m = (d.getMonth() + 1).toString().padStart(2, '0');
    const day = d.getDate().toString().padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  // Format an ISO date (yyyy-MM-dd or full ISO datetime) into French display dd/MM/yyyy
  formatDateForDisplay(dateStr: string | null | undefined): string {
    if (!dateStr) return '';
    try {
      // if full datetime provided, extract date part
      const dpart = String(dateStr).split('T')[0];
      const parts = dpart.split('-');
      if (parts.length === 3) {
        return `${parts[2].padStart(2, '0')}/${parts[1].padStart(2, '0')}/${parts[0]}`;
      }
      return dateStr;
    } catch {
      return dateStr || '';
    }
  }

  // parse ISO datetime or timestamp-ish strings into Date or return null
  private parseDateTime(v: any): Date | null {
    if (!v) return null;
    const d = new Date(v);
    if (!isNaN(d.getTime())) return d;
    return null;
  }

  confirmDate(): void {
    if (!this.tempSelectedDate) return;
    const formatted = this.formatDateForInput(this.tempSelectedDate);
    this.form.get('matchDate')?.setValue(formatted);
    // keep the control enabled but mark readonly so user cannot type directly
    this.dateReadOnly = true;
    this.showCalendarOverlay = false;
    this.cd.detectChanges();
  }

  async onDateInputClick(): Promise<void> {
    // open calendar overlay for selecting a new date
    // do not allow opening the calendar until a field is selected
    const fid = this.form.get('fieldId')?.value ? Number(this.form.get('fieldId')?.value) : null;
    if (!fid) {
      this.error = 'Merci de sélectionner le terrain avant de choisir une date.';
      return;
    }
    // Ensure we have the current user's role before opening the calendar to avoid defaulting
    // to 'subscribed' (14 days) while the profile is still loading.
    if (this.currentUserRoleId === undefined || this.currentUserRoleId === null) {
      try {
        const profile = await firstValueFrom(this.userService.getCurrentUser());
        this.currentUserRoleId = profile?.roleId ?? null;
      } catch (e) {
        // If fetching fails, we still allow opening the calendar but prefer explicit reservationWindowDays
        console.warn('Failed to fetch user profile before opening calendar', e);
      }
    }

    // set tempSelectedDate from current form value if present
    const v = this.form.get('matchDate')?.value;
    if (v) {
      // expect yyyy-mm-dd
      const d = new Date(v);
      if (!isNaN(d.getTime())) {
        this.tempSelectedDate = d;
      } else {
        this.tempSelectedDate = null;
      }
    } else {
      this.tempSelectedDate = null;
    }
    // clear start and end times when re-selecting date
    this.form.get('startTime')?.setValue(null);
    this.form.get('endTime')?.setValue(null);
    this.showCalendarOverlay = true;
    this.dateReadOnly = false;
  }

  isPrivate(): boolean {
    const t = this.form.get('type')?.value || this.defaultType;
    return t === 'private';
  }

  get invitesControls(): any[] {
    return (this.form.get('invites') as FormArray).controls as any[];
  }

  // Return the display name of the currently selected site (used to prefill child forms)
  getSelectedSiteName(): string | null {
    const siteId = this.form.get('siteId')?.value;
    // handle number | null | string cases safely to satisfy strict type checks
    if (siteId === null || siteId === undefined) return null;
    // treat an empty-string siteId as unset; use String(...) to satisfy strict typing
    if (String(siteId).trim() === '') return null;
    const idNum = Number(siteId);
    const s = (this.sites || []).find((x: any) => Number(x.siteId) === idNum);
    if (!s) return null;
    return s.name || s.siteName || null;
  }

  // Helper method to get individual invite control for validation
  getInviteControl(index: number): any {
    return (this.form.get('invites') as FormArray).at(index);
  }

  // Cross-field validation for invites: ensure all entered invites are unique and
  // none equals the current user's email (self-invite). This sets specific
  // errors on individual controls so the template can display targeted messages.
  private runInviteCrossValidation(): void {
    const arr = this.form.get('invites') as FormArray;
    if (!arr || !arr.controls) return;
    const seen = new Map<string, number[]>();
    // collect normalized emails
    arr.controls.forEach((c: any, idx: number) => {
      const v = (c.value || '').toString().trim().toLowerCase();
      if (!v) return;
      if (!seen.has(v)) seen.set(v, []);
      seen.get(v)!.push(idx);
    });
    // clear previous duplicate/self errors, but preserve other errors (like pattern)
    arr.controls.forEach((c: any) => {
      if (!c) return;
      const errors = c.errors || {};
      // remove our custom keys
      delete errors['duplicate'];
      delete errors['selfInvite'];
      delete errors['adminNotAllowed'];
      // inviteNotAllowed used when a user has penalties or outstanding debt
      delete errors['inviteNotAllowed'];
      delete errors['timeout'];
      // if no other errors remain, clear; otherwise set back
      if (Object.keys(errors).length === 0) {
        c.setErrors(null);
      } else {
        c.setErrors(errors);
      }
    });
    // mark duplicates
    seen.forEach((indices, email) => {
      if (indices.length > 1) {
        indices.forEach(i => {
          const c = arr.at(i);
          const err = c.errors || {};
          err['duplicate'] = true;
          c.setErrors(err);
        });
      }
    });
    // mark self-invite if matches current user's email
    if (this.currentUserEmail) {
      const norm = this.currentUserEmail.toString().trim().toLowerCase();
      const matches = seen.get(norm);
      if (matches && matches.length > 0) {
        matches.forEach(i => {
          const c = arr.at(i);
          const err = c.errors || {};
          err['selfInvite'] = true;
          c.setErrors(err);
        });
      }
    }
  }

  // Validate invite email at given index: call backend to see if user exists
  validateInvite(index: number): void {
    const control = this.getInviteControl(index);
    if (!control) return;
    const email = control.value ? String(control.value).trim() : '';
    // mark touched so validation messages show
    control.markAsTouched();
    // do not proceed if the control is invalid (either empty when required or bad email syntax)
    // protect against disabled controls (we manage enable/disable from component)
    if (control.disabled) {
      return;
    }
    if (!email) {
      return;
    }
    if (control.invalid) {
      // ensure the template disables the button, but protect here as well
      return;
    }

    // set checking state
    this.inviteStates[index] = { status: 'checking' };
    this.cd.detectChanges();

    // call InviteService to lookup by email
    try {
      // start watchdog timer BEFORE subscribing to avoid races with synchronous observables
      if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
      let sub: any = null;
      const watchdog = () => setTimeout(() => {
        if (this.inviteStates[index]?.status === 'checking') {
          // mark as not_found so UI stops showing spinner
          this.inviteStates[index] = { status: 'not_found' };
          const c = this.getInviteControl(index);
          if (c) {
            const errs = c.errors || {};
            errs['timeout'] = true;
            c.setErrors(errs);
          }
          // unsubscribe if still subscribed
          try { sub?.unsubscribe?.(); } catch {}
          this.cd.detectChanges();
        }
      }, 3000);
      this.inviteTimeouts[index] = watchdog();

      sub = this.inviteService.getInviteByEmail(email).pipe(finalize(() => {
        // finalize: ensure timeout is cleared and spinner is not left running
        if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
        // if still checking (no next/error received), mark as not_found to stop spinner
        if (this.inviteStates[index]?.status === 'checking') {
          this.inviteStates[index] = { status: 'not_found' };
          const c = this.getInviteControl(index);
          if (c) {
            const errs = c.errors || {};
            errs['timeout'] = true;
            c.setErrors(errs);
          }
          this.cd.detectChanges();
        }
      })).subscribe({
        next: (user: SimpleInviteDto) => {
          // user found -> if admin, disallow invite; otherwise accept
          const roleId = user?.roleId ?? null;
          if (roleId === 7 || roleId === 9) {
            // cannot invite admins
            const control = this.getInviteControl(index);
            if (control) {
              const err = control.errors || {};
              err['adminNotAllowed'] = true;
              control.setErrors(err);
            }
            this.inviteStates[index] = { status: 'error', user: { matricule: user.matricule, email: user.email } };
            // clear any pending timeout
            if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
            this.cd.detectChanges();
            return;
          }

          // Block invites when the found user has an active penalty
          if (user.hasActivePenalties) {
            const control = this.getInviteControl(index);
            if (control) {
              const err = control.errors || {};
              err['inviteNotAllowed'] = true;
              control.setErrors(err);
            }
            this.inviteStates[index] = { status: 'error', user: { matricule: user.matricule, email: user.email } };
            if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
            this.cd.detectChanges();
            return;
          }

          const matchDate = this.form.get('matchDate')?.value;
          const startTime = this.form.get('startTime')?.value;

          if (user.matricule && matchDate && startTime) {
            this.matchService.getCollidingMatches(user.matricule, matchDate, startTime).subscribe({
              next: (isColliding: boolean) => {
                if (isColliding) {
                  const control = this.getInviteControl(index);
                  if (control) {
                    const err = control.errors || {};
                    err['collidingMatch'] = true;
                    control.setErrors(err);
                  }
                  this.inviteStates[index] = { status: 'error', user: { matricule: user.matricule, email: user.email } };
                  if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
                  this.cd.detectChanges();
                  return;
                }
                this.finalizeInviteSuccess(index, user);
              },
              error: () => {
                this.finalizeInviteSuccess(index, user);
              }
            });
            return;
          }

          this.finalizeInviteSuccess(index, user);
        },
        error: (err) => {
          // if backend returns 404 or similar, mark as not_found
          console.warn('Invite validation error for', email, err);
          this.inviteStates[index] = { status: 'not_found' };
          // clear any pending timeout
          if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
          this.cd.detectChanges();
        }
      });
      // watchdog already scheduled before subscribe; nothing more to do here
    } catch (e) {
      console.error('validateInvite caught', e);
      this.inviteStates[index] = { status: 'error' };
      this.cd.detectChanges();
    }
  }

  private finalizeInviteSuccess(index: number, user: SimpleInviteDto): void {
    const control = this.getInviteControl(index);
    if (control) {
      const errs = control.errors || {};
      delete errs['adminNotAllowed'];
      delete errs['inviteNotAllowed'];
      delete errs['collidingMatch'];
      if (Object.keys(errs).length === 0) control.setErrors(null); else control.setErrors(errs);
    }
    this.inviteStates[index] = { status: 'found', user: { matricule: user.matricule, email: user.email } };
    this.runInviteCrossValidation();
    if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
    this.cd.detectChanges();
  }

  // Clear invite input and its state
  clearInvite(index: number): void {
    const control = this.getInviteControl(index);
    if (!control) return;
    control.setValue(null);
    this.inviteStates[index] = { status: 'idle' };
    control.markAsUntouched();
    this.cd.detectChanges();
  }

  // Called when user clicks the "Inviter ?" button for a not-found email.
  // For now we just log; later this will open the signup/user-form prefilled with the email.
  inviteUser(index: number): void {
    const control = this.getInviteControl(index);
    if (!control) return;
    const email = control.value ? String(control.value).trim() : '';
    // open embedded UserFormComponent overlay with prefilled email and mark invite-mode
    this.userFormPrefillEmail = email || undefined;
    this.userFormInviteIndex = index;
    this.showUserForm = true;
    this.cd.detectChanges();
  }

  onUserFormClose(): void {
    this.showUserForm = false;
    this.userFormPrefillEmail = null;
    this.userFormInviteIndex = null;
    this.cd.detectChanges();
  }

  // Called when UserFormComponent emits signupCompleted with the created user
  onSignupCompleted(createdUser: any): void {
    const idx = this.userFormInviteIndex;
    if (idx === null || idx === undefined) {
      this.onUserFormClose();
      return;
    }
    // set the invite input to the created user's email and mark as found
    const control = this.getInviteControl(idx);
    if (control) {
      control.setValue(createdUser?.email || control.value);
      control.markAsTouched();
    }
    this.inviteStates[idx] = { status: 'found', user: createdUser };
    // re-run cross-field validation
    this.runInviteCrossValidation();
    this.onUserFormClose();
    this.cd.detectChanges();
  }

  async submit(): Promise<void> {
    // Submit handler: for private matches the flow requires payment first;
    // for public matches we create immediately and show a confirmation message.
    this.error = null;
    this.successMessage = null;
    if (this.form.invalid) {
      this.error = 'Formulaire invalide. Merci de vérifier les champs manquants.';
      return;
    }

    // build DTO
    const organiserVal = this.form.get('organiserId')?.value;
    const dto: any = {
      fieldId: Number(this.form.get('fieldId')?.value),
      type: this.defaultType ?? this.form.get('type')?.value,
      matchDate: this.form.get('matchDate')?.value,
      startTime: this.form.get('startTime')?.value,
      endTime: this.form.get('endTime')?.value,
      organiserId: organiserVal !== null && organiserVal !== undefined ? String(organiserVal) : null,
    };

    // if private, resolve invites and require payment flow
    if (this.isPrivate()) {
      // Convert invite inputs (which may contain emails) into matricules expected by backend.
      const controls = (this.form.get('invites') as FormArray).controls;
      const invitesMat: string[] = [];
      for (let i = 0; i < controls.length; i++) {
        const raw = controls[i].value;
        if (!raw) continue;
        const v = String(raw).trim();
        if (!v) continue;

        const state = this.inviteStates[i];
        if (state && state.status === 'found' && state.user && state.user.matricule) {
          invitesMat.push(state.user.matricule);
          continue;
        }

        // If value looks like an email, resolve it to matricule now
        if (v.includes('@')) {
          try {
            const user = await firstValueFrom(this.userService.getUserByEmail(v));
            if (!user || !user.matricule) {
              this.error = `Utilisateur invité non-trouvé - ${v}`;
              this.cd.detectChanges();
              return;
            }
            invitesMat.push(user.matricule);
          } catch (e) {
            this.error = `Utilisateur invité non-trouvé - ${v}`;
            this.cd.detectChanges();
            return;
          }
        } else {
          // assume the user entered a matricule directly
          invitesMat.push(v);
        }
      }

      dto.invites = invitesMat;

      // set the amount: 60 / 4 = 15 (hardcoded as requested)
      this.payAmount = 60 / 4;
      this.pendingDto = dto;
      // show the pay form overlay for private matches
      this.showPayForm = true;
      this.cd.detectChanges();
      return;
    }

    // Public match: create immediately without payment and show confirmation
    try {
      this.loading = true;
      // ensure Authorization header from AuthService token if available
      const token = this.authService.getToken();
      if (token) {
        this.matchCreationService.defaultHeaders = this.matchCreationService.defaultHeaders.set('Authorization', `Bearer ${token}`);
      }
      this.matchCreationService.create(dto).subscribe({
        next: (resp: any) => {
          this.loading = false;
          const rawDate = this.form.get('matchDate')?.value || '';
          const rawStart = this.form.get('startTime')?.value || '';
          const rawEnd = this.form.get('endTime')?.value || '';
          const dateFr = this.formatDateForDisplay(rawDate);
          // Simple confirmation message for public matches (no payment instruction)
          this.popupMessage = `Votre match public du ${dateFr} de ${rawStart} à ${rawEnd} a été créé.`;
          this.showSuccessDialog = true;
          this.cd.detectChanges();
        },
        error: (err) => {
          console.error('Echec de la création du match', err);
          this.loading = false;
          this.error = err?.message || 'Echec de la création du match';
          this.cd.detectChanges();
        }
      });
    } catch (e) {
      console.error('submit error', e);
      this.loading = false;
      this.error = 'Erreur lors de la soumission';
    }
  }

  // Handler called when PayFormComponent emits a successful payment
  onPaymentCompleted(payload: { amount: number; cardLast4?: string }): void {
    if (!this.pendingDto) return;
    // attach payment info to DTO; backend may ignore unknown fields but keep in case
    this.pendingDto.paidAmount = payload.amount;
    if (payload.cardLast4) this.pendingDto.cardLast4 = payload.cardLast4;

    // Set Authorization header from AuthService token (if available)
    const token = this.authService.getToken();
    if (token) {
      this.matchCreationService.defaultHeaders = this.matchCreationService.defaultHeaders.set('Authorization', `Bearer ${token}`);
    }

    // hide pay form while creating
    this.showPayForm = false;
    this.loading = true;
    this.matchCreationService.create(this.pendingDto).subscribe({
      next: (resp: any) => {
        this.loading = false;
        const id = resp && resp['matchId'];
        // Determine organiser and invites from pendingDto
        const organiserMat = this.pendingDto?.organiserId || this.organiserId || null;
        const invites = this.pendingDto?.invites || [];
        const pricing = (this.payAmount && this.payAmount > 0) ? this.payAmount * 4 : 60; // default 60

        // Create payments via PayService: organiser cleared (CARD) and invites pending
        if (id && organiserMat) {
          this.payService.createPaymentsForMatch(Number(id), organiserMat, invites, pricing).subscribe({
            next: (results) => {
              // Build French confirmation message using values directly from the form
              const rawDate = this.form.get('matchDate')?.value || '';
              const rawStart = this.form.get('startTime')?.value || '';
              const rawEnd = this.form.get('endTime')?.value || '';
              const dateFr = this.formatDateForDisplay(rawDate);
              this.popupMessage = `Votre match du ${dateFr} de ${rawStart} à ${rawEnd} est réservé. Le paiement du créateur a été enregistré (CARD). Les ${invites.length} invités ont une demande de paiement en attente.`;
              this.pendingDto = null;
              this.showSuccessDialog = true;
              this.cd.detectChanges();
            },
            error: (err) => {
              console.error('Payments creation failed', err);
              // Still show confirmation of match creation but warn about payments
              const rawDate = this.form.get('matchDate')?.value || '';
              const rawStart = this.form.get('startTime')?.value || '';
              const rawEnd = this.form.get('endTime')?.value || '';
              const dateFr = this.formatDateForDisplay(rawDate);
              this.popupMessage = `Votre match du ${dateFr} de ${rawStart} à ${rawEnd} est réservé. Attention: la création des paiements a échoué; contactez l'administrateur.`;
              this.pendingDto = null;
              this.showSuccessDialog = true;
              this.cd.detectChanges();
            }
          });
        } else {
          // Fallback: match created but no organiser found; just show generic confirmation
          const rawDate = this.form.get('matchDate')?.value || '';
          const rawStart = this.form.get('startTime')?.value || '';
          const rawEnd = this.form.get('endTime')?.value || '';
          const dateFr = this.formatDateForDisplay(rawDate);
          this.popupMessage = `Votre match du ${dateFr} de ${rawStart} à ${rawEnd} a été créé.`;
          this.pendingDto = null;
          this.showSuccessDialog = true;
          this.cd.detectChanges();
        }
      },
      error: (err) => {
        console.error('Echec de la création du match', err);
        this.loading = false;
        this.error = err?.message || 'Echec de la création du match';
        this.pendingDto = null;
        this.cd.detectChanges();
      }
    });
  }

  // Handler when user cancels payment
  onPaymentCancelled(): void {
    this.showPayForm = false;
    this.pendingDto = null;
    this.cd.detectChanges();
  }

  // Called when the user clicks OK on the confirmation popup
  acknowledgeSuccess(): void {
    this.showSuccessDialog = false;

    if (this.stayOnPageAfterSuccess) {
      // Clear form and reload sessions/fields as needed, but stay on page
      this.sessionService.clearCaches();

      this.form.reset({
        siteId: this.form.get('siteId')?.value, // Keep the site if selected
        type: this.defaultType ?? this.form.get('type')?.value,
        organiserId: this.form.get('organiserId')?.value
      });

      // Clear internal state
      this.sessionsForField = [];
      this.tempSelectedDate = null;
      this.dateReadOnly = false;
      this.error = null;
      this.popupMessage = null;
      this.successMessage = null;

      // Re-enable/disable controls based on state
      const fid = this.form.get('fieldId')?.value;
      if (!fid) {
        this.form.get('matchDate')?.disable();
        this.form.get('startTime')?.disable();
        this.form.get('endTime')?.disable();
      }

      if (this.matchCalComponent) {
        this.matchCalComponent.refresh();
      }

      this.cd.detectChanges();
      return;
    }

    const organiser = this.organiserId;
    const navigateTo = organiser ? ['/home', organiser] : ['/home'];
    // clear popup message and legacy inline message, then navigate
    this.popupMessage = null;
    this.successMessage = null;
    try {
      this.router.navigate(navigateTo);
    } catch (e) {
      console.error('Navigation after acknowledgement failed', e);
    }
    this.cd.detectChanges();
  }
}

