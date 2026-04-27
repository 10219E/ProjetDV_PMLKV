import { Component, Input, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators, FormArray } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MatchCreationControllerService } from '../../../api/api/matchCreationController.service';
import { FieldControllerService } from '../../../api/api/fieldController.service';
import { SiteControllerService } from '../../../api/api/siteController.service';
import { AuthService } from '../../../services/auth.service';
import { UserService } from '../../../services/user.service';
import { SessionService } from '../../../services/session.service';
import { AvailabilityService } from '../../../services/availability.service';
import { Router } from '@angular/router';
import { MatchCal } from '../match-cal/match-cal';

@Component({
  selector: 'app-match-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatchCal],
  templateUrl: './match-form.html',
  styleUrls: ['./match-form.css']
})
export class MatchForm implements OnInit {
  @Input() organiserId?: string | null;
  @Input() organiserName?: string | null;
  @Input() defaultType?: string | null; // e.g. 'private' or 'public'

  fields: any[] = [];
  // all fields loaded from server (unfiltered). `fields` is the currently displayed list after site filtering.
  fieldsAll: any[] = [];
  // list of sites the user can choose from
  sites: any[] = [];
  // allowed site ids derived from profile or /api/sites
  allowedSiteIds?: number[] | undefined;
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;

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

  // sessions for the currently selected site (normalized for UI: _start/_end labels)
  sessionsForSite: any[] = [];
  private updatingFromSession = false;
  // Fully booked dates for the calendar (Set of YYYY-MM-DD)
  fullyBookedDates: Set<string> = new Set();

  // per-invite validation state (idle, checking, found, not_found, error)
  inviteStates: Array<{ status: 'idle' | 'checking' | 'found' | 'not_found' | 'error', user?: any }> = [
    { status: 'idle' },
    { status: 'idle' },
    { status: 'idle' }
  ];

  constructor(private matchCreationService: MatchCreationControllerService, private fieldService: FieldControllerService, private siteController: SiteControllerService, private authService: AuthService, private userService: UserService, private sessionService: SessionService, private availabilityService: AvailabilityService, private router: Router, private cd: ChangeDetectorRef) {}

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

    // prefill type if provided and disable changing it
    if (this.defaultType) {
      this.form.get('type')?.setValue(this.defaultType);
      this.form.get('type')?.disable();
    }

    // Add validators to invite fields for private matches
    // always ensure email syntax validator is present; for private matches also require the field
    const invitesArray = this.form.get('invites') as FormArray;
    const emailPattern = Validators.pattern(/^[^\s@]+@[^\s@]+\.[a-zA-Z]{2,6}$/);
    if (this.isPrivate()) {
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
        this.cd.detectChanges();
      });
    });

    // keep endTime disabled (greyed) and set placeholder via template; we'll still set its value programmatically
    this.form.get('endTime')?.disable();

    // react to site selection changes -> fetch fields for the selected site
    this.form.get('siteId')?.valueChanges.subscribe((siteId) => {
      const id = siteId ? Number(siteId) : null;
      if (!id) {
        this.fields = [];
        this.sessionsForSite = [];
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
      this.sessionsForSite = [];
      this.tempSelectedDate = null;
      this.dateReadOnly = false;
      // fetch fields only; sessions will be loaded when a field is selected
      this.sessionService.fetchFieldsBySite(id).subscribe({
        next: (data: any[]) => {
          this.fields = data || [];
          this.cd.detectChanges();
          // recompute fully booked dates for newly selected site (no specific field)
          this.updateFullyBookedDates(null);
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
      const session = (this.sessionsForSite || []).find(s => s._start === val);
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
        this.sessionsForSite = [];
        // recompute fully booked dates for site-level (no specific field)
        this.updateFullyBookedDates(null);
        return;
      }
      // try to find the field in the currently loaded fields to get its siteId
      const fld = (this.fields || []).find((f: any) => Number(f.fieldId) === Number(fid));
      const siteId = fld?.siteId ? Number(fld.siteId) : Number(this.form.get('siteId')?.value) || null;
      if (!siteId) {
        this.sessionsForSite = [];
        return;
      }
      this.sessionService.loadSessionsForSite(siteId, this.form.get('matchDate')?.value).subscribe((sessions) => {
        // filter sessions to the selected field when possible
        let filtered = (sessions || []).filter(s => !s.fieldId || Number(s.fieldId) === Number(fid));
        const matchDate = this.form.get('matchDate')?.value;
        if (matchDate) {
          this.availabilityService.filterSessionsByAvailability(siteId, filtered, matchDate, fid).subscribe({
            next: (res) => {
              this.sessionsForSite = res;
              this.cd.detectChanges();
            },
            error: () => {
              this.sessionsForSite = filtered;
              this.cd.detectChanges();
            }
          });
        } else {
          this.sessionsForSite = filtered;
          this.cd.detectChanges();
        }
        // recompute fully booked dates for the selected field
        this.updateFullyBookedDates(fid);
      });
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
            // role ids that grant access to all sites: ALL_SITE_ACCESS(2), SITE_ADMIN(7), ADMIN(9)
            const isAllSites = [2, 7, 9].includes(Number(roleId)) || (profile?.sites && profile.sites.some((s: any) => s.isVip));
            if (isAllSites) {
              // fetch all sites
              // ensure Authorization header is set on the site controller
              this.sessionService.setAuthHeader(this.siteController);
              this.siteController.getAllSites(true).subscribe({
                next: (sites: any[]) => {
                  this.sites = sites || [];
                  const allowed = (this.sites || []).map(s => s.siteId).filter((id: any) => id !== undefined && id !== null);
                  this.loadFieldsForAllowedSites(allowed);

                  // if only one site available, preselect and disable the control
                  if (this.sites.length === 1) {
                    const only = this.sites[0];
                    this.form.get('siteId')?.setValue(only.siteId);
                    this.form.get('siteId')?.disable();
                  } else {
                    this.form.get('siteId')?.enable();
                  }
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
              const userSites = (this.sites || []).map((s: any) => s.siteId).filter((id: any) => id !== undefined && id !== null);
              this.loadFieldsForAllowedSites(userSites);

              if (this.sites.length === 1) {
                const onlySite = this.sites[0];
                this.form.get('siteId')?.setValue(onlySite.siteId);
                this.form.get('siteId')?.disable();
                // Simulate site selection to load start times for normal users
                this.loadSessionsForPreselectedSite(onlySite);
              } else {
                this.form.get('siteId')?.enable();
              }
            }
          },
          error: (err) => {
            console.error('Failed to load current user profile', err);
            // fallback: load all fields unfiltered
            this.loadFieldsForAllowedSites(undefined);
          }
        });


  }

  // ...existing code...

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
      // this will trigger the valueChanges handler and load fields via getFieldsBySite
    }
    // if multiple allowed sites, do not load fields until the user selects a site (valueChanges will handle it)
  }

  // Load sessions for a preselected site (used when site is prefilled and disabled for normal users)
  private loadSessionsForPreselectedSite(site: any): void {
    const siteId = site?.siteId;
    if (!siteId) return;

    this.sessionService.fetchFieldsBySite(siteId).subscribe({
      next: (data: any[]) => {
        // only fetch fields for the preselected site; do not load sessions until a field is selected
        this.fields = data || [];
        this.cd.detectChanges();
        // compute fully booked dates for preselected site (no field selected yet)
        this.updateFullyBookedDates(null);
      },
      error: (err) => {
        console.error('Failed to load fields for preselected site', siteId, err);
        this.fields = [];
      }
    });
  }

  // Update sessionsForSite based on whether a date is selected
  private updateSessionsBasedOnDate(): void {
    const matchDate = this.form.get('matchDate')?.value;
    const fid = Number(this.form.get('fieldId')?.value) || null;
    // If no field selected, clear sessions (we load sessions when a field is selected)
    if (!fid) {
      this.sessionsForSite = [];
      if (!matchDate) {
        this.form.get('startTime')?.setValue(null);
        this.form.get('endTime')?.setValue(null);
      }
      this.cd.detectChanges();
      return;
    }
    // find site's id from loaded fields if possible
    const fld = (this.fields || []).find((f: any) => Number(f.fieldId) === Number(fid));
    const siteId = fld?.siteId ? Number(fld.siteId) : Number(this.form.get('siteId')?.value) || null;
    if (!siteId) {
      this.sessionsForSite = [];
      this.cd.detectChanges();
      return;
    }
    this.sessionService.onDateChange(siteId, matchDate).subscribe((sessions) => {
      let filtered = (sessions || []).filter(s => !s.fieldId || Number(s.fieldId) === Number(fid));
      if (matchDate) {
        this.availabilityService.filterSessionsByAvailability(siteId, filtered, matchDate, fid).subscribe({
          next: (res) => {
            this.sessionsForSite = res;
            if (!matchDate) {
              this.form.get('startTime')?.setValue(null);
              this.form.get('endTime')?.setValue(null);
            }
            this.cd.detectChanges();
          },
          error: () => {
            this.sessionsForSite = filtered;
            if (!matchDate) {
              this.form.get('startTime')?.setValue(null);
              this.form.get('endTime')?.setValue(null);
            }
            this.cd.detectChanges();
          }
        });
      } else {
        this.sessionsForSite = filtered;
        if (!matchDate) {
          this.form.get('startTime')?.setValue(null);
          this.form.get('endTime')?.setValue(null);
        }
        this.cd.detectChanges();
      }
    });
  }

  /**
   * Compute fully booked dates for the calendar. For the next N days, load sessions for the site
   * and use AvailabilityService to determine if any session slots remain for the selected field.
   * This is async and updates `fullyBookedDates` when complete.
   */
  private async updateFullyBookedDates(fieldId: number | null): Promise<void> {
    this.fullyBookedDates = new Set();
    const siteId = Number(this.form.get('siteId')?.value) || null;
    if (!siteId) {
      this.cd.detectChanges();
      return;
    }

    const days = 14; // check next 14 days
    const today = new Date();
    for (let i = 0; i < days; i++) {
      const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() + i);
      const iso = this.formatDateForInput(d);
      try {
        // load normalized sessions for this site/date
        const sessions: any[] = await firstValueFrom(this.sessionService.loadSessionsForSite(siteId, iso));
        let sessionsForField = sessions || [];
        if (fieldId) sessionsForField = (sessionsForField || []).filter(s => !s.fieldId || Number(s.fieldId) === Number(fieldId));
        const available: any[] = await firstValueFrom(this.availabilityService.filterSessionsByAvailability(siteId, sessionsForField, iso, fieldId ?? null));
        if (!available || available.length === 0) {
          this.fullyBookedDates.add(iso);
        }
      } catch (e) {
        // ignore single-day failures; do not block overall computation
        console.warn('updateFullyBookedDates error for', iso, e);
      }
    }
    this.cd.detectChanges();
  }

  // ...existing code...

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

  // parse ISO datetime or timestamp-ish strings into Date or return null
  private parseDateTime(v: any): Date | null {
    if (!v) return null;
    const d = new Date(v);
    if (!isNaN(d.getTime())) return d;
    return null;
  }

  // ...existing code...

  // ...existing code...

  // ...existing code...

  confirmDate(): void {
    if (!this.tempSelectedDate) return;
    const formatted = this.formatDateForInput(this.tempSelectedDate);
    this.form.get('matchDate')?.setValue(formatted);
    // keep the control enabled but mark readonly so user cannot type directly
    this.dateReadOnly = true;
    this.showCalendarOverlay = false;
    this.cd.detectChanges();
  }

  onDateInputClick(): void {
    // open calendar overlay for selecting a new date
    // do not allow opening the calendar until a field is selected
    const fid = this.form.get('fieldId')?.value ? Number(this.form.get('fieldId')?.value) : null;
    if (!fid) {
      this.error = 'Please select a field before choosing a date.';
      return;
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

  // Helper method to get individual invite control for validation
  getInviteControl(index: number): any {
    return (this.form.get('invites') as FormArray).at(index);
  }

  // Validate invite email at given index: call backend to see if user exists
  validateInvite(index: number): void {
    const control = this.getInviteControl(index);
    if (!control) return;
    const email = control.value ? String(control.value).trim() : '';
    // mark touched so validation messages show
    control.markAsTouched();
    // do not proceed if the control is invalid (either empty when required or bad email syntax)
    if (control.invalid) {
      // ensure the template disables the button, but protect here as well
      return;
    }

    // set checking state
    this.inviteStates[index] = { status: 'checking' };
    this.cd.detectChanges();

    // call UserService to lookup by email
    try {
      this.userService.getUserByEmail(email).subscribe({
        next: (user) => {
          // user found -> keep email in control but store user (matricule) for later use
          this.inviteStates[index] = { status: 'found', user };
          this.cd.detectChanges();
        },
        error: (err) => {
          // if backend returns 404 or similar, mark as not_found
          console.warn('Invite validation error for', email, err);
          this.inviteStates[index] = { status: 'not_found' };
          this.cd.detectChanges();
        }
      });
    } catch (e) {
      console.error('validateInvite caught', e);
      this.inviteStates[index] = { status: 'error' };
      this.cd.detectChanges();
    }
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
    console.log('Invite user flow should start for', email);
    // TODO: open UserFormComponent with prefilled email (handled in later step)
  }

  submit(): void {
    this.error = null;
    this.successMessage = null;
    if (this.form.invalid) {
      this.error = 'Form is invalid. Please check required fields.';
      return;
    }

      // build DTO according to generated MatchCreationDto (omit hidden fields)
       const dto: any = {
         fieldId: Number(this.form.get('fieldId')?.value),
         type: this.defaultType ?? this.form.get('type')?.value,
         matchDate: this.form.get('matchDate')?.value,
         startTime: this.form.get('startTime')?.value,
         endTime: this.form.get('endTime')?.value,
         organiserId: this.form.get('organiserId')?.value,
       };

    if (this.isPrivate()) {
      const invites = (this.form.get('invites') as FormArray).controls.map(c => c.value).filter((v: any) => v && v.toString().trim() !== '');
      dto.invites = invites;
    }

    // Set Authorization header from AuthService token (if available)
    const token = this.authService.getToken();
    if (token) {
      this.matchCreationService.defaultHeaders = this.matchCreationService.defaultHeaders.set('Authorization', `Bearer ${token}`);
    }

    this.loading = true;
    this.matchCreationService.create(dto).subscribe({
      next: (resp: any) => {
        this.loading = false;
        const id = resp && resp['matchId'];
        this.successMessage = id ? `Match created (id=${id})` : 'Match created';
        // optionally navigate to user's home after creating
        if (this.organiserId) {
          this.router.navigate(['/home', this.organiserId]);
        }
      },
      error: (err) => {
        console.error('Create match failed', err);
        this.loading = false;
        this.error = err?.message || 'Failed to create match';
        this.cd.detectChanges();
      }
    });
  }
}
