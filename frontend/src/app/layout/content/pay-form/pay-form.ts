import { Component, EventEmitter, Output, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';

@Component({
  selector: 'app-pay-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './pay-form.html',
  styleUrls: ['./pay-form.css']
})
export class PayFormComponent {
  @Input() amount = 0;
  @Output() paid = new EventEmitter<{ amount: number; cardLast4?: string }>();
  @Output() cancelled = new EventEmitter<void>();

  cardNumber = new FormControl<string | null>(null, [Validators.required, Validators.pattern(/^\d{16}$/)]);
  loading = false;
  error: string | null = null;

  // Simple mock payment process: validate card number and emit paid after short delay
  pay(): void {
    this.error = null;
    if (this.cardNumber.invalid) {
      this.cardNumber.markAsTouched();
      //this.error = 'Card number must be 16 digits.';
      return;
    }
    this.loading = true;
    const num = (this.cardNumber.value || '').toString();
    const last4 = num.slice(-4);
    // simulate network/payment delay
    setTimeout(() => {
      this.loading = false;
      this.paid.emit({ amount: this.amount, cardLast4: last4 });
    }, 800);
  }

  cancel(): void {
    this.cancelled.emit();
  }
}

