import { Component, OnInit, OnDestroy, Input, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
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
  private sub?: Subscription;
  isAdmin = false;
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
            this.cdr.detectChanges();
          },
          error: () => {
            this.isAdmin = false;
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
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
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
    // Try current user first
    this.userService.getCurrentUser().pipe(take(1)).subscribe({
      next: (u: any) => {
        const id = u && u.matricule;
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

}
