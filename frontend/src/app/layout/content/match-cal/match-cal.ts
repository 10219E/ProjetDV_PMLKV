import { Component, Output, EventEmitter, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-match-cal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './match-cal.html',
  styleUrls: ['./match-cal.css']
})
export class MatchCal {
  @Input() selected: Date | null = null;
  @Output() dateChange = new EventEmitter<Date | null>();
  @Output() confirm = new EventEmitter<Date>();
  @Output() cancel = new EventEmitter<void>();
  // Role-based reservation window. Accepts numeric role codes or role names.
  // Known roles (from backend):
  // 0 = invite (Membre externe invité) -> max 5 days
  // 1 = subscribed (Membre with subscription to at least one site) -> max 14 days
  // 2 = all_site (Membre VIP multi-sites) -> max 21 days
  // 7 = site_admin (site administrator) -> admin: 3 months (~90 days)
  // 9 = as_admin (super administrator) -> admin: 3 months (~90 days)
  // You can also pass strings: 'invite','subscribed','all_site','site_admin','as_admin'.
  @Input() role: number | string = 'subscribed';
  // Optional override to set the number of days allowed for reservation (including today when reservationWindowIncludesToday = true).
  // If provided, this value takes precedence over `role` mapping.
  @Input() reservationWindowDays?: number;
  // If true, the reservation window count includes today. Default: true as requested.
  @Input() reservationWindowIncludesToday = true;
  /**
   * Optional: list or predicate of fully booked dates. Accepts either:
   * - Array of ISO date strings (YYYY-MM-DD) or Date objects
   * - Set of ISO date strings
   * - Predicate function (isoDate: string) => boolean
   */
  private _fullyBookedInput?: string[] | Set<string> | ((iso: string) => boolean);
  private _fullyBookedSet?: Set<string> | undefined;
  private _fullyBookedFn?: ((iso: string) => boolean) | undefined;

  @Input()
  set fullyBooked(v: string[] | Set<string> | ((iso: string) => boolean) | undefined) {
    this._fullyBookedInput = v;
    this._normalizeFullyBooked();
  }
  get fullyBooked() {
    return this._fullyBookedInput;
  }

  /** If true, normalization uses UTC to produce YYYY-MM-DD. Default false to avoid UTC-shifting local dates
   *  (UTC conversion could map a local midnight to the previous day in UTC and cause off-by-one bookings).
   */
  @Input() fullyBookedUseUTC = false;

  /** Emitted when user attempts to select a fully booked date. */
  @Output() disabledSelectAttempt = new EventEmitter<string>();

  viewDate = new Date();
  weekDays = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];
  monthNames = [
    'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
    'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'
  ];

  get calendarDays() {
    // Each entry includes date, whether it's past and whether it's fully booked. This avoids
    // repeated timezone-sensitive conversions in the template and centralizes the logic here.
    const days: { date: Date; isPast: boolean; isFullyBooked: boolean; isBeyondWindow: boolean }[] = [];
    const year = this.viewDate.getFullYear();
    const month = this.viewDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const startDay = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1; // Monday start
    // Fill blanks
    for (let i = 0; i < startDay; i++) {
      const d = new Date(year, month, 1 - startDay + i);
      days.push({ date: d, isPast: this.isPast(d), isFullyBooked: this.isFullyBooked(d), isBeyondWindow: this.isBeyondReservationWindow(d) });
    }
    // Fill days
    for (let d = 1; d <= lastDay.getDate(); d++) {
      const date = new Date(year, month, d);
      const isPast = this.isPast(date);
      days.push({ date, isPast, isFullyBooked: this.isFullyBooked(date), isBeyondWindow: this.isBeyondReservationWindow(date) });
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
    if (this.isFullyBooked(date)) {
      // emit iso string for caller to react (analytics / toast / etc.)
      this.disabledSelectAttempt.emit(this._toIsoYMD(date));
      return;
    }
    if (this.isBeyondReservationWindow(date)) {
      // emit reason that date is beyond allowed reservation window
      this.disabledSelectAttempt.emit(`beyond-window:${this._toIsoYMD(date)}`);
      return;
    }
    this.selected = date;
    this.dateChange.emit(date);
  }

  /** Combined check used by UI: not past and not fully booked */
  isSelectable(date: Date) {
    return !this.isPast(date) && !this.isFullyBooked(date) && !this.isBeyondReservationWindow(date);
  }

  /** Returns true when the given date is beyond the configured reservation window. */
  isBeyondReservationWindow(date: Date) {
    const last = this._computeLastAllowedDate();
    if (!last) return false; // no limit configured
    // Normalize times to midnight local for comparison
    const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    return d > last;
  }

  private _computeLastAllowedDate(): Date | null {
    const days = this._getMaxReservationDays();
    if (!days || days <= 0) return null;
    const today = new Date();
    const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    // If includesToday is true, count includes today. So lastAllowed = start + (days - 1)
    const offset = this.reservationWindowIncludesToday ? days - 1 : days;
    return new Date(start.getFullYear(), start.getMonth(), start.getDate() + offset);
  }

  private _getMaxReservationDays(): number {
    if (typeof this.reservationWindowDays === 'number') return this.reservationWindowDays;
    const r = (this.role ?? '').toString().toLowerCase();
    switch (r) {
      case '0':
      case 'invite':
      case 'l':
        return 5; // invite / visiteur: max 5 days (including today)
      case '1':
      case 'subscribed':
      case 's':
        return 14; // subscribed / member single-site: 2 weeks = 14 days
      case '2':
      case 'all_site':
      case 'g':
        return 21; // VIP / multi-site: 3 weeks = 21 days
      case 'site_admin':
      case '7':
      case '9':
      case 'as_admin':
        return 90; // admin roles: ~3 months = 90 days
      default:
        return 14; // sensible default: 2 weeks
    }
  }

  isFullyBooked(date: Date) {
    if (!this._fullyBookedFn && !this._fullyBookedSet) return false;
    const iso = this._toIsoYMD(date);
    if (this._fullyBookedFn) return this._fullyBookedFn(iso);
    return !!this._fullyBookedSet?.has(iso);
  }

  private _normalizeFullyBooked() {
    this._fullyBookedFn = undefined;
    this._fullyBookedSet = undefined;
    const v = this._fullyBookedInput as any;
    if (!v) return;
    if (typeof v === 'function') {
      this._fullyBookedFn = v;
      return;
    }
    // it's a Set or Array - normalize to Set<string> of YYYY-MM-DD
    const set = new Set<string>();
    if (v instanceof Set) {
      for (const el of v) {
        if (typeof el === 'string') set.add(el);
        else if (el instanceof Date) set.add(this._toIsoYMD(el));
        else set.add(String(el));
      }
    } else if (Array.isArray(v)) {
      for (const el of v) {
        if (typeof el === 'string') set.add(el);
        else if (el instanceof Date) set.add(this._toIsoYMD(el));
        else set.add(String(el));
      }
    }
    this._fullyBookedSet = set;
  }

  private _toIsoYMD(d: Date | string) {
    if (typeof d === 'string') {
      // assume user passed YYYY-MM-DD or similar; try to normalize
      // if it's already in YYYY-MM-DD we return as is
      if (/^\d{4}-\d{2}-\d{2}$/.test(d)) return d;
      const parsed = new Date(d);
      return parsed.toISOString().slice(0, 10);
    }
    if (this.fullyBookedUseUTC) return d.toISOString().slice(0, 10);
    // local YYYY-MM-DD
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${dd}`;
  }

  isPast(date: Date) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return date < today;
  }

  prevMonth() {
    this.viewDate = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth() - 1, 1);
  }

  nextMonth() {
    this.viewDate = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth() + 1, 1);
  }

  onCancel() {
    this.cancel.emit();
  }

  onConfirm() {
    if (this.selected) this.confirm.emit(this.selected);
  }
}
