import { Component, Input, LOCALE_ID, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule, registerLocaleData } from '@angular/common';
import { NavService } from '../../../services/nav.service';
import localeFr from '@angular/common/locales/fr';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { MatIconModule } from '@angular/material/icon';

// Register French locale data for date pipe when this module loads
registerLocaleData(localeFr);

@Component({
  selector: 'app-home-account-header',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule],
  providers: [{ provide: LOCALE_ID, useValue: 'fr-FR' }],
  templateUrl: './header.html',
})
export class HomeAccountHeader implements OnInit, OnDestroy {
  @Input() todayDate: Date | null = new Date();
  private timer: any;
  showHomeButton = false;

  constructor(private navService: NavService, private cdr: ChangeDetectorRef, private router: Router) {}

  ngOnInit(): void {
    this.updateHomeButtonVisibility();

    // Listen for navigation changes to update button visibility
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe(() => {
      this.updateHomeButtonVisibility();
    });

    // If todayDate was not passed as an override, start a timer to update the clock every minute
    if (!this.todayDate) {
      this.todayDate = new Date();
    }

    this.timer = setInterval(() => {
      this.todayDate = new Date();
      this.cdr.detectChanges();
    }, 60000); // update every minute
  }

  ngOnDestroy(): void {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }

  // Toggle the shared nav menu via service so header doesn't need to know where the nav component is rendered.
  toggleNav(): void {
    this.navService.toggle();
  }

  private updateHomeButtonVisibility(): void {
    const url = this.router.url;
    // Hide button if URL is exactly /home or /home/ (after accounting for matricule)
    // Matches /home/:userId accurately
    const homePattern = /^\/home\/[^\/]+$/;
    this.showHomeButton = !homePattern.test(url) && url !== '/home';
    this.cdr.detectChanges();
  }
}
