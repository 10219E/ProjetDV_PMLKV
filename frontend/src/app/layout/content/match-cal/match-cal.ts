import { Component, Output, EventEmitter, Input, OnChanges, SimpleChanges, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClosuresService } from '../../../services/closures.service';
import { SessionService } from '../../../services/session.service';
import { Subscription } from 'rxjs';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-match-cal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './match-cal.html',
  styleUrls: ['./match-cal.css']
})
export class MatchCal implements OnChanges, OnDestroy {
  @Input() selected: Date | null = null;
  @Output() dateChange = new EventEmitter<Date | null>();
  @Output() confirm = new EventEmitter<Date>();
  @Output() cancel = new EventEmitter<void>();
  @Input() role: number | string = 'subscribed';
  @Input() siteId?: number | null;
  @Input() fieldId?: number | null;

  private _availableDates: Set<string> = new Set();
  private _isLoading = false;
  private _availableDatesSub?: Subscription;

  viewDate = new Date();
  weekDays = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];
  monthNames = [
    'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
    'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'
  ];

  get calendarDays() {
    const days: { date: Date; isPast: boolean; isFullyBooked: boolean; isAvailable: boolean }[] = [];
    const year = this.viewDate.getFullYear();
    const month = this.viewDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const startDay = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1; // Monday start

    // Fill blanks
    for (let i = 0; i < startDay; i++) {
      const d = new Date(year, month, 1 - startDay + i);
      days.push({
        date: d,
        isPast: this.isPast(d),
        isFullyBooked: this.isFullyBooked(d),
        isAvailable: false
      });
    }

    // Fill days
    for (let d = 1; d <= lastDay.getDate(); d++) {
      const date = new Date(year, month, d);
      const isPast = this.isPast(date);
      const isFullyBooked = this.isFullyBooked(date);
      const isAvailable = !isPast && !isFullyBooked && this._availableDates.has(this._toIsoYMD(date));

      days.push({
        date,
        isPast,
        isFullyBooked,
        isAvailable
      });
    }

    return days;
  }

  isSelected(date: Date) {
    return (
      this.selected &&
      date.getFullYear() === this.selected.getFullYear() &&
      date.getMonth() === this.selected.getMonth() &&
      date.getDate() === this.selected.getDate()
    );
  }

  selectDate(date: Date) {
    if (this.isSelected(date)) return;
    if (this.isPast(date)) return;
    if (this.isFullyBooked(date)) return;
    if (!this._availableDates.has(this._toIsoYMD(date))) return;

    this.selected = date;
    this.dateChange.emit(date);
  }

  private _blockedSub?: Subscription;
  private _remoteBlockedSet?: Set<string>;

  isFullyBooked(date: Date) {
    const iso = this._toIsoYMD(date);
    return this._remoteBlockedSet?.has(iso) || false;
  }

  isPast(date: Date) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    // Let backend handle business logic for available dates
    // Frontend only checks if date is literally in the past (before today)
    return date < today;
  }

  prevMonth() {
    this.viewDate = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth() - 1, 1);
    this.loadAvailableDates();
  }

  nextMonth() {
    this.viewDate = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth() + 1, 1);
    this.loadAvailableDates();
  }

  onCancel() {
    this.cancel.emit();
  }

  onConfirm() {
    if (this.selected) this.confirm.emit(this.selected);
  }

  constructor(
    private closuresService: ClosuresService,
    private sessionService: SessionService,
    private cd: ChangeDetectorRef
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['siteId'] || changes['fieldId'] || changes['role']) {
      this.loadAvailableDates();
      this.updateBlockedDates();
    }
  }

  refresh(): void {
    this.loadAvailableDates();
    this.updateBlockedDates();
  }

  ngOnDestroy(): void {
    this._blockedSub?.unsubscribe();
    this._availableDatesSub?.unsubscribe();
  }

  private loadAvailableDates() {
    if (!this.siteId || !this.fieldId) return;

    // Always use current date for availability calculation, not the calendar view date
    // The backend will calculate the available range based on user role from today
    const startDate = this._toIsoYMD(new Date());
    const roleId = typeof this.role === 'string' ? this._getRoleId(this.role) : this.role;

    this._isLoading = true;
    this._availableDatesSub?.unsubscribe();

    this._availableDatesSub = this.sessionService.getAvailableDates(
      this.siteId,
      this.fieldId,
      startDate,
      roleId
    ).subscribe(dates => {
      this._availableDates = new Set(dates);
      this._isLoading = false;
      this.cd.detectChanges();
    });
  }

  private _getRoleId(role: string): number {
    switch (role.toLowerCase()) {
      case 'invite': return 0;
      case 'subscribed': return 1;
      case 'all_site': return 2;
      case 'site_admin': return 7;
      case 'as_admin': return 9;
      default: return 1; // default to subscribed
    }
  }

  private updateBlockedDates() {
    if (!this.siteId && !this.fieldId) {
      return;
    }

    this._blockedSub?.unsubscribe();
    this._blockedSub = this.closuresService.getBlockedDates(this.siteId ?? null, this.fieldId ?? null).subscribe(s => {
      this._remoteBlockedSet = s;
      this.cd.detectChanges();
    });
  }

  private _toIsoYMD(d: Date | string) {
    if (typeof d === 'string') {
      if (/^\d{4}-\d{2}-\d{2}$/.test(d)) return d;
      const parsed = new Date(d);
      return parsed.toISOString().slice(0, 10);
    }
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${dd}`;
  }
}
