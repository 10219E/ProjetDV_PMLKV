import { Component as NgComponent, Input as NgInput } from '@angular/core';
import { CommonModule } from '@angular/common';

@NgComponent({
  selector: 'app-home-account-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.html',
})
export class HomeAccountHeader {
  @NgInput() todayDate: Date | null = new Date();

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





