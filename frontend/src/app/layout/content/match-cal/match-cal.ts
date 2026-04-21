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

  viewDate = new Date();
  weekDays = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];
  monthNames = [
    'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
    'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'
  ];

  get calendarDays() {
    const days: { date: Date; isPast: boolean }[] = [];
    const year = this.viewDate.getFullYear();
    const month = this.viewDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const startDay = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1; // Monday start
    // Fill blanks
    for (let i = 0; i < startDay; i++) {
      days.push({ date: new Date(year, month, 1 - startDay + i), isPast: true });
    }
    // Fill days
    for (let d = 1; d <= lastDay.getDate(); d++) {
      const date = new Date(year, month, d);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const isPast = date < today;
      days.push({ date, isPast });
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
    if (!this.isSelected(date) && !this.isPast(date)) {
      this.selected = date;
      this.dateChange.emit(date);
    }
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
