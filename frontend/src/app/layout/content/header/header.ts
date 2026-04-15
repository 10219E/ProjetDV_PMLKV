import { Component as NgComponent, Input as NgInput } from '@angular/core';
// CommonModule not required for this header (no structural directives used)

@NgComponent({
  selector: 'app-home-account-header',
  standalone: true,
  templateUrl: './header.html',
})
export class HomeAccountHeader {
  @NgInput() todayDate: Date | null = new Date();
  get formattedDate(): string {
    if (!this.todayDate) return '';
    try {
      return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(this.todayDate as Date);
    } catch {
      return this.todayDate.toString();
    }
  }
  // Reference the getter to satisfy static analysis (used in template)
  private _formattedDateUsed = this.formattedDate;
}





