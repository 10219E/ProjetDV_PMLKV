import { Component, Input, LOCALE_ID, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
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
export class HomeAccountHeader implements OnInit, OnDestroy {
  @Input() todayDate: Date | null = new Date();
  private timer: any;

  constructor(private navService: NavService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
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
}





