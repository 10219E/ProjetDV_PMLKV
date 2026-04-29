import { Component, OnInit, OnDestroy, Input, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { UserService } from '../../../services/user.service';
import { take } from 'rxjs/operators';
import { NavService } from '../../../services/nav.service';
import { Subscription } from 'rxjs';
@Component({
  selector: 'app-nav-menu',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './nav-menu.html',
})
export class NavMenu implements OnInit, OnDestroy {
  visible = false;
  // popup shown when user is blocked from creating matches (penalty or debt)
  showRestrictionPopup = false;
  restrictionMessage = 'Vous avez une pénalité active ou une dette impayée ; vous ne pouvez pas réserver de matchs tant que la situation n\'est pas réglée.';
  // details extracted from the user JSON to display in the popup
  restrictionPenalties: any[] = []; // can hold multiple active penalties
  restrictionDebtAmount: number | null = null;
  private sub?: Subscription;
  private routerSub?: Subscription;
  isAdmin = false;
  currentMatricule: string | null = null;
  // track whether we attempted to move the mobile overlay to body
  private _movedOverlay = false;
  // When true the component will not render/move the mobile overlay (useful for page-level placements)
  @Input() preventMobileOverlay = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private userService: UserService,
    private navService: NavService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Subscribe to the shared NavService so header can toggle the menu from anywhere.
    this.sub = this.navService.visible$.subscribe(v => {
      this.visible = !!v;
      // when menu becomes visible, move the mobile overlay element to document.body so
      // it escapes any stacking-context from parent containers and can appear above the floating header
      if (this.visible && !this.preventMobileOverlay) {
        // delay to allow the template to render the overlay element
        setTimeout(() => this.moveMobileOverlayToBody(), 0);
      } else if (!this.visible && this._movedOverlay) {
        // menu was closed — if we previously moved the overlay into body, remove it so
        // a new NavMenu instance can render its own overlay cleanly (prevents orphaned overlay)
        this.cleanupMovedOverlay();
      }
    });

    // determine if current user is an admin (roleId 7 or 9)
    // Avoid calling the protected endpoint when the user is not authenticated
    // (prevents noisy 403s on public pages like the root landing).
    try {
      if (this.authService.isAuthenticated()) {
        this.userService.getCurrentUser().pipe(take(1)).subscribe({
          next: (u: any) => {
            const rid = Number(u?.roleId ?? -1);
            this.isAdmin = [7, 9].includes(rid);
            this.currentMatricule = u?.matricule ?? null;
            this.cdr.detectChanges();
          },
          error: () => {
            this.isAdmin = false;
            this.currentMatricule = null;
            this.cdr.detectChanges();
          }
        });
      } else {
        // not authenticated -> definitely not admin
        this.isAdmin = false;
      }
    } catch (e) {
      this.isAdmin = false;
    }

    // Listen for navigation end to show restriction popup when redirected by guard
    try {
      this.routerSub = this.router.events.subscribe(evt => {
        if (evt instanceof NavigationEnd) {
          try {
            const q = this.router.parseUrl(this.router.url).queryParams || {};
            if (q['blocked']) {
              // persist a marker so popup survives intermediate redirects
              try { localStorage.setItem('blockedPopup', '1'); } catch(e) {}
              // fetch current user details to show specifics in the popup
              try {
                this.userService.getCurrentUser().pipe(take(1)).subscribe({
                  next: (u: any) => {
                    this.setRestrictionFromUser(u);
                    this.showRestrictionPopup = true;
                    this.cdr.detectChanges();
                  },
                  error: () => {
                    // fallback: just show generic message
                    this.restrictionPenalties = [];
                    this.restrictionDebtAmount = null;
                    this.showRestrictionPopup = true;
                    this.cdr.detectChanges();
                  }
                });
              } catch (errInner) {
                this.showRestrictionPopup = true;
                this.cdr.detectChanges();
              }
            }
          } catch (err) {
            // ignore
          }
        }
      });
    } catch (e) {
      // noop
    }

    // If we have a persisted blocked marker (guard redirected), ensure popup is shown
    try {
      if (localStorage.getItem('blockedPopup')) {
        this.userService.getCurrentUser().pipe(take(1)).subscribe({
          next: (u: any) => {
            this.setRestrictionFromUser(u);
            this.showRestrictionPopup = true;
            this.cdr.detectChanges();
          },
          error: () => {
            this.showRestrictionPopup = true;
            this.cdr.detectChanges();
          }
        });
      }
    } catch (e) {
      // ignore
    }
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.routerSub?.unsubscribe();
    // ensure any overlay left in body by this component is cleaned up when it is destroyed
    this.cleanupMovedOverlay();
  }

  toggle(): void {
    this.navService.toggle();
  }

  close(): void {
    this.navService.close();
  }

  onLogout(): void {
	this.authService.logout();
	this.router.navigate(['/']).then(() => window.location.reload());
  }

  // Navigate to the create private match page for the current user.
  // Attempts to fetch the current user's matricule (id) from the API; if that fails
  // it falls back to trying to extract a userId from the current URL (/home/:userId/...).
  goCreatePmatch(): void {
    // close the nav menu immediately on click (mobile UX)
    this.close();
    // Try current user first
    this.userService.getCurrentUser().pipe(take(1)).subscribe({
      next: (u: any) => {
        const id = u && u.matricule;
        // If user has an active penalty or debt, set details and show inline popup and DO NOT navigate
        if (this.userHasRestriction(u)) {
          this.setRestrictionFromUser(u);
          this.showRestrictionPopup = true;
          try { localStorage.setItem('blockedPopup', '1'); } catch(e) {}
          this.cdr.detectChanges();
          return;
        }
        if (id) {
          const segment = this.isAdmin ? 'create_public' : 'create_pmatch';
          this.router.navigate([`/home`, id, segment]);
          this.close();
          return;
        }
        // fallback to parse from URL
        this.navigateFromUrlFallback();
      },
      error: (_err) => this.navigateFromUrlFallback()
    });
  }

  // Navigate to the page that lists/join public matches for the current user.
  goJoinPublic(): void {
    // close the nav menu immediately on click (mobile UX)
    this.close();
    this.userService.getCurrentUser().pipe(take(1)).subscribe({
      next: (u: any) => {
        // If user has an active penalty or debt, show inline popup and DO NOT navigate
        if (this.userHasRestriction(u)) {
          this.setRestrictionFromUser(u);
          this.showRestrictionPopup = true;
          try { localStorage.setItem('blockedPopup', '1'); } catch(e) {}
          this.cdr.detectChanges();
          return;
        }
        const id = u && u.matricule;
        if (id) {
          this.router.navigate([`/home`, id, 'join_public']);
          this.close();
          return;
        }
        // fallback to parse from URL
        this.navigateFromUrlFallbackJoinPublic();
      },
      error: (_err) => this.navigateFromUrlFallbackJoinPublic()
    });
  }

  private navigateFromUrlFallbackJoinPublic(): void {
    const url = this.router.url || '';
    const m = url.match(/\/home\/(\w+)/);
    if (m && m[1]) {
      this.router.navigate([`/home`, m[1], 'join_public']);
    } else {
      this.router.navigate(['/']);
    }
    this.close();
  }

  // Determine whether the given user object should be blocked from creating matches.
  // Block when account.status === 'debt' or account.balance < 0, or when there is any
  // active penalty (penalty.isActive === true and optionally within date range).
  private userHasRestriction(u: any): boolean {
    if (!u) return false;
    try {
      const acc = u.account || {};
      if ((acc.status || '').toString().toLowerCase() === 'debt') return true;
      if (typeof acc.balance === 'number' && acc.balance < 0) return true;

      const penalties = Array.isArray(u.penalties) ? u.penalties : [];
      const now = new Date();
      for (const p of penalties) {
        if (!p) continue;
        // prefer explicit isActive flag
        if (p.isActive) {
          // if start/end dates are present, ensure current date falls within range
          if (p.startDate && p.endDate) {
            const s = new Date(p.startDate);
            const e = new Date(p.endDate);
            if (!isNaN(s.getTime()) && !isNaN(e.getTime()) && now >= s && now <= e) return true;
          } else {
            return true;
          }
        }
      }
    } catch (e) {
      // if anything goes wrong, default to not blocking
      console.warn('userHasRestriction check failed', e);
    }
    return false;
  }

  // Close the restriction popup
  closeRestrictionPopup(): void {
    // remove the query param so popup does not reappear on reload
    try {
      this.router.navigate([], { queryParams: { blocked: null }, replaceUrl: true });
    } catch (e) {
      // ignore navigation errors
    }
    try { localStorage.removeItem('blockedPopup'); } catch(e) {}
    this.showRestrictionPopup = false;
    this.cdr.detectChanges();
  }

  // NOTE: navigation to invites uses `currentMatricule` property so template can build routerLink

  private navigateFromUrlFallback(): void {
    // router.url might be something like /home/123 or /home/123/other
    const url = this.router.url || '';
    const m = url.match(/\/home\/(\w+)/);
    const segment = this.isAdmin ? 'create_public' : 'create_pmatch';
    if (m && m[1]) {
      this.router.navigate([`/home`, m[1], segment]);
    } else {
      // no user id available; navigate to a safe default (home root)
      this.router.navigate(['/']);
    }
    this.close();
  }

  private moveMobileOverlayToBody(): void {
    try {
      const el = document.querySelector('.nav-mobile-overlay') as HTMLElement | null;
      if (!el) return;
      if (el.parentElement === document.body) return;
      // append to body so overlay is top-level for stacking
      document.body.appendChild(el);
      this._movedOverlay = true;
    } catch (e) {
      // ignore DOM errors
      console.warn('Failed to move nav mobile overlay to body', e);
    }
  }

  /**
   * Remove any nav mobile overlay that was previously moved into document.body.
   * Called when the menu is closed or the component is destroyed so a new instance
   * can re-render the overlay without conflicts.
   */
  private cleanupMovedOverlay(): void {
    try {
      const el = document.querySelector('.nav-mobile-overlay') as HTMLElement | null;
      if (!el) {
        this._movedOverlay = false;
        return;
      }
      if (el.parentElement === document.body) {
        // Smoothly fade/slide out instead of immediately removing to avoid a hard visual cut.
        const panel = el.querySelector('.nav-mobile-panel') as HTMLElement | null;
        try {
          // add classes to animate out (Tailwind utility classes assumed present in build)
          el.classList.add('opacity-0');
          if (panel) panel.classList.add('translate-x-full');
          // wait for the transition to finish (duration matches classes above: 200ms)
          const removeDelay = 220;
          setTimeout(() => {
            // remove from DOM after animation
            if (el.parentElement === document.body) el.remove();
          }, removeDelay);
        } catch (innerErr) {
          // fallback: if animation fails, remove immediately
          if (el.parentElement === document.body) el.remove();
        }
      }
    } catch (e) {
      console.warn('Failed to cleanup nav mobile overlay from body', e);
    } finally {
      this._movedOverlay = false;
    }
  }

  // Populate restrictionPenalties / restrictionDebtAmount from the user object
  private setRestrictionFromUser(u: any): void {
    try {
      this.restrictionPenalties = [];
      this.restrictionDebtAmount = null;
      const acc = u?.account || {};
      if (typeof acc.balance === 'number' && acc.balance < 0) {
        this.restrictionDebtAmount = acc.balance;
      } else if ((acc.status || '').toString().toLowerCase() === 'debt') {
        this.restrictionDebtAmount = typeof acc.balance === 'number' ? acc.balance : null;
      }

      const penalties = Array.isArray(u?.penalties) ? u.penalties : [];
      const now = new Date();
      for (const p of penalties) {
        if (!p) continue;
        if (p.isActive) {
          if (p.startDate && p.endDate) {
            const s = new Date(p.startDate);
            const e = new Date(p.endDate);
            if (!isNaN(s.getTime()) && !isNaN(e.getTime()) && now >= s && now <= e) {
              this.restrictionPenalties.push(p);
            }
          } else {
            this.restrictionPenalties.push(p);
          }
        }
      }
      // fallback: if no active penalties found, but there are penalties, include any with isActive===true
      if (this.restrictionPenalties.length === 0 && penalties.length > 0) {
        const actives = penalties.filter((pp: any) => pp && pp.isActive);
        if (actives.length > 0) this.restrictionPenalties = actives;
      }
    } catch (e) {
      console.warn('setRestrictionFromUser failed', e);
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

  private formatDate(d: string | Date): string {
    try {
      const dt = d instanceof Date ? d : new Date(d);
      if (isNaN(dt.getTime())) return String(d);
      const day = String(dt.getDate()).padStart(2, '0');
      const month = String(dt.getMonth() + 1).padStart(2, '0');
      const year = dt.getFullYear();
      return `${day}/${month}/${year}`;
    } catch (e) {
      return String(d);
    }
  }
}
