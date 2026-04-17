import { Component, OnInit, OnDestroy, Input } from '@angular/core';
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
  // track whether we attempted to move the mobile overlay to body
  private _movedOverlay = false;
  // When true the component will not render/move the mobile overlay (useful for page-level placements)
  @Input() preventMobileOverlay = false;

  constructor(private authService: AuthService, private router: Router, private userService: UserService, private navService: NavService) {}

  ngOnInit(): void {
    // Subscribe to the shared NavService so header can toggle the menu from anywhere.
    this.sub = this.navService.visible$.subscribe(v => {
      this.visible = !!v;
      // when menu becomes visible, move the mobile overlay element to document.body so
      // it escapes any stacking-context from parent containers and can appear above the floating header
      if (this.visible && !this.preventMobileOverlay) {
        // delay to allow the template to render the overlay element
        setTimeout(() => this.moveMobileOverlayToBody(), 0);
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
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
          this.router.navigate([`/home`, id, `create_pmatch`]);
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
    if (m && m[1]) {
      this.router.navigate([`/home`, m[1], `create_pmatch`]);
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

}
