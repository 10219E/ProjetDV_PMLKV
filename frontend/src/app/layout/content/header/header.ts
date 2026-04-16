import { Component, Input, LOCALE_ID } from '@angular/core';
import { CommonModule, registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';

// Register French locale data for date pipe when this module loads.
registerLocaleData(localeFr);

@Component({
  selector: 'app-home-account-header',
  standalone: true,
  imports: [CommonModule],
  providers: [{ provide: LOCALE_ID, useValue: 'fr-FR' }],
  templateUrl: './header.html',
})
export class HomeAccountHeader {
  @Input() todayDate: Date | null = new Date();

  // Dispatch a global event that `NavMenu` listens to in order to toggle the mobile overlay.
  toggleNav(): void {
    try {
      window.dispatchEvent(new CustomEvent('toggleNavMenu'));
    } catch (e) {
      // fallback for older environments
      try { window.dispatchEvent(new Event('toggleNavMenu')); } catch {}
    }
  }
}





