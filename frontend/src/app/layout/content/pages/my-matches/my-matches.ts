import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, firstValueFrom } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { ReactiveFormsModule, FormGroup, FormControl, Validators, FormArray } from '@angular/forms';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';
import { MatchService } from '../../../../services/match.service';
import { InviteService } from '../../../../services/invite.service';
import { UserService } from '../../../../services/user.service';
import { PayService } from '../../../../services/pay.service';
import { MatchPlayerSiteFieldDto } from '../../../../api/model/matchPlayerSiteFieldDto';
import { DeclinedPlayersDto } from '../../../../api/model/declinedPlayersDto';
import { SimpleInviteDto } from '../../../../api/model/simpleInviteDto';
import { MatchPaymentDto } from '../../../../api/model/matchPaymentDto';
import { UserFormComponent } from '../../user-form/user-form';
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-my-matches',
  standalone: true,
  imports: [CommonModule, NavMenu, HomeAccountHeader, ReactiveFormsModule, UserFormComponent],
  templateUrl: './my-matches.html'
})
export class MyMatches implements OnInit {
  matchPlayers: MatchPlayerSiteFieldDto[] = [];
  declinedPlayers: DeclinedPlayersDto[] = [];
  currentMatchEmails: string[] = [];
  loading = false;
  error: string | null = null;
  userId: string | null = null;
  showInviteForm = false;
  showSuccessPopup = false;
  selectedMatchId: number | null = null;

  // Form handling
  inviteForm = new FormGroup({
    invites: new FormArray<FormControl<string | null>>([])
  });
  inviteStates: Array<{ status: 'idle' | 'checking' | 'found' | 'not_found' | 'error', user?: any }> = [];
  private inviteTimeouts: Array<any> = [];
  currentUserEmail?: string | null;

  showUserForm = false;
  userFormPrefillEmail?: string | null = null;
  userFormInviteIndex: number | null = null;

  // Status translations
  private statusTranslations: Record<string, string> = {
    approved: 'Confirmé',
    pending: 'En attente',
    declined: 'Rejeté',
    invited: 'Invité'
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private matchService: MatchService,
    private inviteService: InviteService,
    private userService: UserService,
    private payService: PayService,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.userId = this.route.snapshot.paramMap.get('userId');
    if (this.userId) {
      this.loadMatches(this.userId);
    }

    // Check if we should show success popup after refresh
    if (sessionStorage.getItem('showSuccessInvite') === 'true') {
      this.showSuccessPopup = true;
      sessionStorage.removeItem('showSuccessInvite');
    }
  }

  private loadMatches(matricule: string): void {
    this.loading = true;
    this.error = null;

    // Use forkJoin to wait for both API calls to complete
    forkJoin([
      this.matchService.getMyMatches(matricule),
      this.matchService.getOrganiserMatchesWithDeclinedPlayers(matricule)
    ]).subscribe({
      next: ([matchesData, declinedData]: [MatchPlayerSiteFieldDto[], DeclinedPlayersDto[]]) => {
        // Process matches data
        this.matchPlayers = Array.isArray(matchesData) ? matchesData : [];

        // Sort matches by date and time (closest first)
        this.matchPlayers.sort((a, b) => {
          const dateA = a.match?.matchDate ? new Date(a.match.matchDate) : new Date(8640000000000000);
          const dateB = b.match?.matchDate ? new Date(b.match.matchDate) : new Date(8640000000000000);

          const applyTime = (d: Date, t: any) => {
            if (!t) return;
            if (typeof t === 'string') {
              const parts = t.split(':');
              if (parts.length >= 2) d.setHours(Number(parts[0]), Number(parts[1]), 0, 0);
            } else {
              const h = t.hour ?? t.Hour ?? 0;
              const m = t.minute ?? t.Minute ?? 0;
              d.setHours(Number(h), Number(m), 0, 0);
            }
          };

          applyTime(dateA, a.match?.startTime);
          applyTime(dateB, b.match?.startTime);

          return dateA.getTime() - dateB.getTime();
        });

        // Process declined players data
        this.declinedPlayers = Array.isArray(declinedData) ? declinedData : [];

        this.userService.getCurrentUser().subscribe({
          next: (profile) => {
            this.currentUserEmail = profile?.email;
          }
        });

        this.loading = false;
        this.cd.detectChanges();
      },
      error: (err: any) => {
        console.error('Error loading matches or declined players:', err);
        this.error = err?.message || 'Erreur lors du chargement de vos matchs.';
        this.matchPlayers = [];
        this.declinedPlayers = [];
        this.loading = false;
        this.cd.detectChanges();
      }
    });
  }

  goToInvitation(): void {
    if (this.userId) {
      this.router.navigate(['/home', this.userId, 'invites']);
    }
  }

  // Check if current user is the organiser of this match
  isOrganiser(match: any): boolean {
    if (!match || !this.userId) return false;
    return match.match?.organiserId === this.userId;
  }

  // Check if current user is a player in this match
  isCurrentUserPlayer(match: any): boolean {
    if (!match || !this.userId) return false;
    return match.player?.userMatricule === this.userId;
  }

  // Check if current match has declined players AND current user is the organiser
  hasDeclinedPlayers(match: any): boolean {
    if (!match || !this.userId) return false;

    // Get the actual match ID - it might be in match.match?.matchId, not match.matchId
    const actualMatchId = match.match?.matchId || match.matchId;
    if (!actualMatchId) return false;

    // Check if current user is the organiser of this match
    const isOrganiser = this.isOrganiser(match);

    // Check if this match has declined players
    const hasDeclined = this.declinedPlayers.some(dp => dp.matchId === actualMatchId);

    return isOrganiser && hasDeclined;
  }

  // Show invite form for replacing declined players
  showReplaceInviteForm(matchId: number): void {
    this.selectedMatchId = matchId;
    const declinedCount = this.getDeclinedCount(matchId);

    // Fetch all players for this match to prevent inviting someone already in
    this.inviteService.getPlayersForMatch(matchId).subscribe({
      next: (emails) => {
        this.currentMatchEmails = emails || [];
      },
      error: (err) => console.error('Error fetching match players', err)
    });

    // Setup form array based on declined count
    const invitesArray = this.inviteForm.get('invites') as FormArray;
    invitesArray.clear();
    this.inviteStates = [];
    this.inviteTimeouts = [];

    const emailPattern = Validators.pattern(/^[^\s@]+@[^\s@]+\.[a-zA-Z]{2,6}$/);
    for (let i = 0; i < declinedCount; i++) {
      const control = new FormControl<string | null>(null, [Validators.required, Validators.email, emailPattern]);
      invitesArray.push(control);
      this.inviteStates.push({ status: 'idle' });
      this.inviteTimeouts.push(null);

      control.valueChanges.subscribe(() => {
        if (this.inviteStates[i]?.status !== 'checking') {
          this.inviteStates[i] = { status: 'idle' };
        }
        if (!control.value) {
          control.markAsUntouched();
        }
        this.runInviteCrossValidation();
        this.cd.detectChanges();
      });
    }

    this.showInviteForm = true;
  }

  // Hide invite form
  hideInviteForm(): void {
    this.showInviteForm = false;
    this.selectedMatchId = null;
  }

  get invitesControls(): any[] {
    return (this.inviteForm.get('invites') as FormArray).controls;
  }

  allInvitesFound(): boolean {
    const invites = this.inviteForm.get('invites') as FormArray;
    if (!invites || invites.length === 0) return true;
    for (let i = 0; i < invites.length; i++) {
      const val = invites.at(i).value;
      if (!val) continue; // if field is empty, it might be allowed depending on requirements, but here we usually have 4 players total.
      if (this.inviteStates[i]?.status !== 'found') return false;
    }
    return true;
  }

  getInviteControl(index: number): any {
    return (this.inviteForm.get('invites') as FormArray).at(index);
  }

  private runInviteCrossValidation(): void {
    const arr = this.inviteForm.get('invites') as FormArray;
    if (!arr || !arr.controls) return;
    const seen = new Map<string, number[]>();
    arr.controls.forEach((c: any, idx: number) => {
      const v = (c.value || '').toString().trim().toLowerCase();
      if (!v) return;
      if (!seen.has(v)) seen.set(v, []);
      seen.get(v)!.push(idx);
    });

    arr.controls.forEach((c: any) => {
      if (!c) return;
      const errors = c.errors || {};
      delete errors['duplicate'];
      delete errors['selfInvite'];
      delete errors['adminNotAllowed'];
      delete errors['inviteNotAllowed'];
      delete errors['userDeclined'];
      delete errors['alreadyPlayer'];
      delete errors['timeout'];
      if (Object.keys(errors).length === 0) c.setErrors(null, { emitEvent: false });
      else c.setErrors(errors, { emitEvent: false });
    });

    seen.forEach((indices, email) => {
      if (indices.length > 1) {
        indices.forEach(i => {
          const c = arr.at(i);
          const err = c.errors || {};
          err['duplicate'] = true;
          c.setErrors(err, { emitEvent: false });
        });
      }
    });

    if (this.currentUserEmail) {
      const norm = this.currentUserEmail.toString().trim().toLowerCase();
      const matches = seen.get(norm);
      if (matches && matches.length > 0) {
        matches.forEach(i => {
          const c = arr.at(i);
          const err = c.errors || {};
          err['selfInvite'] = true;
          c.setErrors(err, { emitEvent: false });
        });
      }
    }
  }

  validateInvite(index: number): void {
    const control = this.getInviteControl(index);
    if (!control) return;
    const email = control.value ? String(control.value).trim() : '';
    control.markAsTouched();
    if (control.disabled || !email || control.invalid) return;

    this.inviteStates[index] = { status: 'checking' };
    this.cd.detectChanges();

    try {
      if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
      let sub: any = null;
      const watchdog = () => setTimeout(() => {
        if (this.inviteStates[index]?.status === 'checking') {
          this.inviteStates[index] = { status: 'not_found' };
          const c = this.getInviteControl(index);
          if (c) {
            const errs = c.errors || {};
            errs['timeout'] = true;
            c.setErrors(errs, { emitEvent: false });
          }
          try { sub?.unsubscribe?.(); } catch {}
          this.cd.detectChanges();
        }
      }, 3000);
      this.inviteTimeouts[index] = watchdog();

      sub = this.inviteService.getInviteByEmail(email).pipe(finalize(() => {
        if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
        if (this.inviteStates[index]?.status === 'checking') {
          this.inviteStates[index] = { status: 'not_found' };
          const c = this.getInviteControl(index);
          if (c) { const errs = c.errors || {}; errs['timeout'] = true; c.setErrors(errs, { emitEvent: false }); }
          this.cd.detectChanges();
        }
      })).subscribe({
        next: (user: SimpleInviteDto) => {
          const roleId = user?.roleId ?? null;
          if (roleId === 7 || roleId === 9) {
            const c = this.getInviteControl(index);
            if (c) { const err = c.errors || {}; err['adminNotAllowed'] = true; c.setErrors(err, { emitEvent: false }); }
            this.inviteStates[index] = { status: 'error', user: { matricule: user.matricule, email: user.email } };
            if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
            this.cd.detectChanges();
            return;
          }

          if ((user as any).hasActivePenalties) {
            const c = this.getInviteControl(index);
            if (c) { const err = c.errors || {}; err['inviteNotAllowed'] = true; c.setErrors(err, { emitEvent: false }); }
            this.inviteStates[index] = { status: 'error', user: { matricule: user.matricule, email: user.email } };
            if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
            this.cd.detectChanges();
            return;
          }

          const hasDeclined = this.declinedPlayers.some(dp => dp.matchId === this.selectedMatchId && dp.playerId === user.matricule);
          if (hasDeclined) {
            const c = this.getInviteControl(index);
            if (c) { const err = c.errors || {}; err['userDeclined'] = true; c.setErrors(err, { emitEvent: false }); }
            this.inviteStates[index] = { status: 'error', user: { matricule: user.matricule, email: user.email } };
            if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
            this.cd.detectChanges();
            return;
          }

          const isAlreadyPlayer = this.currentMatchEmails.includes(user.email!);
          if (isAlreadyPlayer) {
            const c = this.getInviteControl(index);
            if (c) { const err = c.errors || {}; err['alreadyPlayer'] = true; c.setErrors(err, { emitEvent: false }); }
            this.inviteStates[index] = { status: 'error', user: { matricule: user.matricule, email: user.email } };
            if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
            this.cd.detectChanges();
            return;
          }

          const targetMatch = this.matchPlayers.find(m => m.match?.matchId === this.selectedMatchId);
          if (targetMatch && targetMatch.match?.matchDate && targetMatch.match?.startTime && user.matricule) {
            const matchDateStr = typeof targetMatch.match.matchDate === 'string' ? targetMatch.match.matchDate.split('T')[0] : targetMatch.match.matchDate;
            const startTimeStr = typeof targetMatch.match.startTime === 'string' ? targetMatch.match.startTime : (targetMatch.match.startTime as any)?.hour !== undefined ? `${(targetMatch.match.startTime as any).hour}:${(targetMatch.match.startTime as any).minute}` : String(targetMatch.match.startTime);

            this.matchService.getCollidingMatches(user.matricule, matchDateStr, startTimeStr).subscribe({
              next: (isColliding: boolean) => {
                if (isColliding) {
                  const c = this.getInviteControl(index);
                  if (c) { const err = c.errors || {}; err['collidingMatch'] = true; c.setErrors(err, { emitEvent: false }); }
                  this.inviteStates[index] = { status: 'error', user: { matricule: user.matricule, email: user.email } };
                  if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
                  this.cd.detectChanges();
                } else {
                  this.finalizeInviteSuccess(index, user);
                }
              },
              error: () => {
                // proceed anyway if the check fails
                this.finalizeInviteSuccess(index, user);
              }
            });
            return;
          }

          this.finalizeInviteSuccess(index, user);
        },
        error: (err) => {
          this.inviteStates[index] = { status: 'not_found' };
          if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
          this.cd.detectChanges();
        }
      });
    } catch (e) {
      this.inviteStates[index] = { status: 'error' };
      this.cd.detectChanges();
    }
  }

  private finalizeInviteSuccess(index: number, user: SimpleInviteDto): void {
    const c = this.getInviteControl(index);
    if (c) {
      const errs = c.errors || {};
      delete errs['adminNotAllowed']; delete errs['inviteNotAllowed']; delete errs['userDeclined']; delete errs['collidingMatch'];
      if (Object.keys(errs).length === 0) c.setErrors(null, { emitEvent: false }); else c.setErrors(errs, { emitEvent: false });
    }
    this.inviteStates[index] = { status: 'found', user: { matricule: user.matricule, email: user.email } };
    this.runInviteCrossValidation();
    if (this.inviteTimeouts[index]) { clearTimeout(this.inviteTimeouts[index]); this.inviteTimeouts[index] = null; }
    this.cd.detectChanges();
  }

  clearInvite(index: number): void {
    const control = this.getInviteControl(index);
    if (!control) return;
    control.setValue(null);
    this.inviteStates[index] = { status: 'idle' };
    control.markAsUntouched();
    this.cd.detectChanges();
  }

  inviteUser(index: number): void {
    const control = this.getInviteControl(index);
    if (!control) return;
    const email = control.value ? String(control.value).trim() : '';
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

  onSignupCompleted(createdUser: any): void {
    const idx = this.userFormInviteIndex;
    if (idx === null || idx === undefined) {
      this.onUserFormClose();
      return;
    }
    const control = this.getInviteControl(idx);
    if (control) {
      control.setValue(createdUser?.email || control.value);
      control.markAsTouched();
    }
    this.inviteStates[idx] = { status: 'found', user: createdUser };
    this.runInviteCrossValidation();
    this.onUserFormClose();
    this.cd.detectChanges();
  }

  async submitReinvites(): Promise<void> {
    if (this.inviteForm.invalid || !this.selectedMatchId) return;
    const controls = (this.inviteForm.get('invites') as FormArray).controls;
    const invitesMat: string[] = [];
    this.loading = true;

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
        if (v.includes('@')) {
          try {
            const user = await firstValueFrom(this.userService.getUserByEmail(v));
            if (user && user.matricule) {
              invitesMat.push(user.matricule);
              continue;
            }
          } catch(e) {}
        }
        invitesMat.push(v);
    }
    this.loading = false;
    this.inviteReplacementPlayers(invitesMat);
  }

  // Handle inviting replacement players
  inviteReplacementPlayers(newPlayerIds: string[]): void {
    if (!this.selectedMatchId || !this.userId) return;
    this.loading = true;

    // Find the pricing for this match
    const targetMatch = this.matchPlayers.find(m => m.match?.matchId === this.selectedMatchId);
    // Assume shared pay means pricing / 4 for the newly invited replacement player
    const amount = targetMatch && targetMatch.match?.pricing != null ? targetMatch.match.pricing / 4 : 0;

    const requests = newPlayerIds.map(newPlayerId => {
      return this.matchService.joinPublicMatchOrUpdatePrivate(this.selectedMatchId!, newPlayerId, 'pending').pipe(
        switchMap(() => {
          const dto: MatchPaymentDto = {
            matchId: this.selectedMatchId!,
            userMatricule: newPlayerId,
            amount: amount,
            status: 'pending'
          };
          return this.payService.createPayment(dto);
        })
      );
    });

    if (requests.length === 0) {
      this.loading = false;
      this.hideInviteForm();
      return;
    }

    forkJoin(requests).subscribe({
      next: () => {
        sessionStorage.setItem('showSuccessInvite', 'true');
        window.location.reload();
      },
      error: (err) => {
        console.error(`Error inviting replacement players:`, err);
        this.error = 'Certaines invitations n\'ont pas pu être envoyées.';
        this.loading = false;
        this.cd.detectChanges();
      }
    });
  }

  // Show success popup - not used as we're triggering a reload then show popup
  displaySuccessPopup(): void {
    this.showSuccessPopup = true;
    this.cd.detectChanges();
  }

  closeSuccessPopup(): void {
    this.showSuccessPopup = false;
    this.cd.detectChanges();
  }

  formatTime(t?: any): string {
    if (!t) return '';
    const hour = (t && (t.hour ?? t.Hour)) ?? null;
    const minute = (t && (t.minute ?? t.Minute)) ?? 0;
    if (hour == null) {
      try { return String(t); } catch { return ''; }
    }
    return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
  }

  matchType(mp: MatchPlayerSiteFieldDto): string {
    const type = mp.match?.type ?? '';
    if (type === 'public') return 'Public';
    if (type === 'private') return 'Privé';
    return type || '—';
  }

  isPending(mp: MatchPlayerSiteFieldDto): boolean {
    return (mp.player?.status ?? '').toLowerCase() === 'pending';
  }

  // Method to get status translation from the constructor
  getStatusTranslation(status?: string): string {
    if (!status) return '—';

    const lowerStatus = status.toLowerCase();
    return this.statusTranslations[lowerStatus] ||
           status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
  }

  // Method to format match date
  formatMatchDate(date?: string): string {
    if (!date) return '—';
    const d = new Date(date);
    return d.toLocaleDateString('fr-FR', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  // Method to get match time range
  getMatchTimeRange(match: MatchPlayerSiteFieldDto['match']): string {
    const start = this.formatTime(match?.startTime);
    const end = this.formatTime(match?.endTime);
    return `${start} - ${end}`;
  }

  // Method to get field type
  getFieldType(field: MatchPlayerSiteFieldDto['field']): string {
    return field?.isIndoor ? 'Intérieur' : 'Extérieur';
  }

  // Method to get site name
  getSiteName(site: MatchPlayerSiteFieldDto['site']): string {
    return site?.name || '—';
  }

  // Method to get declined players count for a specific match
  getDeclinedCount(matchId: number | undefined): number {
    if (!matchId) return 0;
    return this.declinedPlayers.filter(dp => dp.matchId === matchId).length;
  }
}
