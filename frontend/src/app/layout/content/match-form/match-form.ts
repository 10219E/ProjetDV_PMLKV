import { Component, Input, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators, FormArray } from '@angular/forms';
import { MatchCreationControllerService } from '../../../api/api/matchCreationController.service';
import { FieldControllerService } from '../../../api/api/fieldController.service';
import { SiteControllerService } from '../../../api/api/siteController.service';
import { AuthService } from '../../../services/auth.service';
import { UserService } from '../../../services/user.service';
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
   startTime: new FormControl<string | null>(null, [Validators.required]),
   endTime: new FormControl<string | null>({value: null, disabled: true}, [Validators.required]),
   organiserId: new FormControl<string | null>(null, [Validators.required]),
   invites: new FormArray([
     new FormControl<string | null>(null),
     new FormControl<string | null>(null),
     new FormControl<string | null>(null)
   ])
  });

  // calendar overlay state
  showCalendarOverlay = true;
  tempSelectedDate: Date | null = null;
  dateReadOnly = false;

  // sessions for the currently selected site (normalized for UI: _start/_end labels)
  sessionsForSite: any[] = [];
  private updatingFromSession = false;

  constructor(private matchCreationService: MatchCreationControllerService, private fieldService: FieldControllerService, private siteController: SiteControllerService, private authService: AuthService, private userService: UserService, private router: Router, private cd: ChangeDetectorRef) {}

  ngOnInit(): void {
    // determine whether to show calendar overlay (hide if matchDate already set)
    const existingDate = this.form.get('matchDate')?.value;
    if (existingDate) {
      this.showCalendarOverlay = false;
      this.dateReadOnly = true; // keep the date visually non-editable but allow click to open calendar
      // leave control enabled for validation
    } else {
      this.showCalendarOverlay = true;
      this.dateReadOnly = false;
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
    if (this.isPrivate()) {
      const invitesArray = this.form.get('invites') as FormArray;
      invitesArray.controls.forEach(control => {
        control.setValidators([Validators.required]);
        control.updateValueAndValidity();
      });
    }

    // keep endTime disabled (greyed) and set placeholder via template; we'll still set its value programmatically
    this.form.get('endTime')?.disable();

    // react to site selection changes -> filter displayed fields
          this.form.get('siteId')?.valueChanges.subscribe((siteId) => {
      const id = siteId ? Number(siteId) : null;
      if (!id) {
        this.fields = [];
        return;
      }
        // ensure Authorization header is set on the field service before calling
        this.setAuthHeaderForService(this.fieldService);
        // fetch fields for the selected site using the per-site endpoint
        this.fieldService.getFieldsBySite(id).subscribe({
          next: (data: any[]) => {
            this.fields = data || [];
            // load sessions embedded in the selected site (controller returns sessions per-site)
            const site = (this.sites || []).find((x: any) => Number(x.siteId) === Number(id));
            this.sessionsForSite = (site && site.sessions) ? (site.sessions as any[]).map((ss: any) => this.normalizeSessionForUi(ss)) : [];
            // Clear sessions if no date is selected
            this.updateSessionsBasedOnDate();
            this.cd.detectChanges();
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
        return;
      }
      const session = (this.sessionsForSite || []).find(s => s._start === val);
      if (!session) {
        this.form.get('endTime')?.setValue(null);
        return;
      }
      this.updatingFromSession = true;
      if (session.fieldId) this.form.get('fieldId')?.setValue(session.fieldId);
      if (session.siteId) this.form.get('siteId')?.setValue(session.siteId);
      if (session._end) this.form.get('endTime')?.setValue(session._end);
      if (session.startedAt) {
        const sd = this.parseDateTime(session.startedAt);
        if (sd) this.form.get('matchDate')?.setValue(this.formatDateForInput(sd));
      }
      this.updatingFromSession = false;
    });

    // React to date changes - clear sessions if date is not selected
    this.form.get('matchDate')?.valueChanges.subscribe((date) => {
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
              this.setAuthHeaderForService(this.siteController);
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

  // Helper to set Authorization header on generated API services that expose defaultHeaders
  private setAuthHeaderForService(service: any): void {
    try {
      const token = this.authService.getToken();
      if (token && service && service.defaultHeaders && service.defaultHeaders.set) {
        service.defaultHeaders = service.defaultHeaders.set('Authorization', `Bearer ${token}`);
      }
    } catch (e) {
      // ignore failures setting headers
    }
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
      // this will trigger the valueChanges handler and load fields via getFieldsBySite
    }
    // if multiple allowed sites, do not load fields until the user selects a site (valueChanges will handle it)
  }

  // Load sessions for a preselected site (used when site is prefilled and disabled for normal users)
  private loadSessionsForPreselectedSite(site: any): void {
    const siteId = site?.siteId;
    if (!siteId) return;

    // ensure Authorization header is set on the field service before calling
    this.setAuthHeaderForService(this.fieldService);

    // fetch fields for the selected site using the per-site endpoint
    this.fieldService.getFieldsBySite(siteId).subscribe({
      next: (data: any[]) => {
        this.fields = data || [];
        // For normal users, we need to get the site details with sessions from the API
        // The site object from user profile might not contain sessions data
        this.setAuthHeaderForService(this.siteController);
        this.siteController.getSiteById(siteId).subscribe({
          next: (siteWithSessions: any) => {
            // load sessions embedded in the site (controller returns sessions per-site)
            this.sessionsForSite = (siteWithSessions && siteWithSessions.sessions) ?
                (siteWithSessions.sessions as any[]).map((ss: any) => this.normalizeSessionForUi(ss)) : [];
            // Clear sessions if no date is selected
            this.updateSessionsBasedOnDate();
            this.cd.detectChanges();
          },
          error: (err) => {
            console.error('Failed to load site details for preselected site', siteId, err);
            this.sessionsForSite = [];
          }
        });
      },
      error: (err) => {
        console.error('Failed to load fields for preselected site', siteId, err);
        this.fields = [];
      }
    });
  }

  // Update sessionsForSite based on whether a date is selected
  private updateSessionsBasedOnDate(): void {
    const hasDate = !!this.form.get('matchDate')?.value;
    if (!hasDate && this.sessionsForSite.length > 0) {
      // Store the original sessions temporarily
      if (!this._originalSessions) {
        this._originalSessions = [...this.sessionsForSite];
      }
      this.sessionsForSite = [];
      this.form.get('startTime')?.setValue(null);
      this.form.get('endTime')?.setValue(null);
    } else if (hasDate && this._originalSessions && this._originalSessions.length > 0) {
      // Restore sessions when date is selected
      this.sessionsForSite = [...this._originalSessions];
      this._originalSessions = null;
    }
    this.cd.detectChanges();
  }

  // Temporary storage for original sessions when date is not selected
  private _originalSessions: any[] | null = null;

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

  // format a Date into HH:mm string
  private formatTimeHHMM(d: Date): string {
    const hh = d.getHours().toString().padStart(2, '0');
    const mm = d.getMinutes().toString().padStart(2, '0');
    return `${hh}:${mm}`;
  }

  // normalize a raw session into UI-friendly fields: _start, _end and label
  private normalizeSessionForUi(s: any): any {
    const out: any = { ...s };

    // Handle both formats: startedAt/endedAt (datetime) and start_time/end_time (time strings)
    let startTimeStr = s.start_time || null;
    let endTimeStr = s.end_time || null;

    // If we have datetime fields, convert them to time strings
    if (s.startedAt && !startTimeStr) {
      const startDate = this.parseDateTime(s.startedAt);
      startTimeStr = startDate ? this.formatTimeHHMM(startDate) : null;
    }
    if (s.endedAt && !endTimeStr) {
      const endDate = this.parseDateTime(s.endedAt);
      endTimeStr = endDate ? this.formatTimeHHMM(endDate) : null;
    }

    // If we have time strings but they're in HH:MM:SS format, convert to HH:MM
    if (startTimeStr && startTimeStr.includes(':')) {
      startTimeStr = startTimeStr.split(':').slice(0, 2).join(':');
    }
    if (endTimeStr && endTimeStr.includes(':')) {
      endTimeStr = endTimeStr.split(':').slice(0, 2).join(':');
    }

    out._start = startTimeStr;
    out._end = endTimeStr;
    out.label = out._start ? `${out._start}` : `Slot ${out.match_set_id ?? out.sessionId ?? ''}`;
    return out;
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

  onDateInputClick(): void {
    // open calendar overlay for selecting a new date
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
