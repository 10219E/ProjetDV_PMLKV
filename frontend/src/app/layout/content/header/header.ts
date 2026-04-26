import { Component, Input, LOCALE_ID } from '@angular/core';
import { CommonModule, registerLocaleData } from '@angular/common';
import { NavService } from '../../../services/nav.service';
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
  constructor(private navService: NavService) {}

  // Toggle the shared nav menu via service so header doesn't need to know where the nav component is rendered.
  toggleNav(): void {
    this.navService.toggle();
  }
}





