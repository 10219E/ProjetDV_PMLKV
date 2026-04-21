import { Component, Input, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators, FormArray } from '@angular/forms';
import { MatchCreationControllerService } from '../../../api/api/matchCreationController.service';
import { FieldControllerService } from '../../../api/api/fieldController.service';
import { AuthService } from '../../../services/auth.service';
import { UserService } from '../../../services/user.service';
import { Router } from '@angular/router';
import { MatchCal } from '../match-cal/match-cal';

@Component({
  selector: 'app-match-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatchCal],
  templateUrl: './match-form.html',
  styleUrls: ['./match-form.css']
})
export class MatchForm implements OnInit {
  @Input() organiserId?: string | null;
  @Input() organiserName?: string | null;
  @Input() defaultType?: string | null; // e.g. 'private' or 'public'

  fields: any[] = [];
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;

  form = new FormGroup({
   fieldId: new FormControl<number | null>(null, [Validators.required]),
   type: new FormControl<string | null>(null),
   matchDate: new FormControl<string | null>(null, [Validators.required]),
   startTime: new FormControl<string | null>(null, [Validators.required]),
   endTime: new FormControl<string | null>(null, [Validators.required]),
   organiserId: new FormControl<string | null>(null, [Validators.required]),
   invites: new FormArray([
	 new FormControl<string | null>(null),
	 new FormControl<string | null>(null),
	 new FormControl<string | null>(null)
   ])
  });

  // calendar overlay state
  showCalendarOverlay = true;
  tempSelectedDate: Date | null = null;
  dateReadOnly = false;

  constructor(private matchCreationService: MatchCreationControllerService, private fieldService: FieldControllerService, private authService: AuthService, private userService: UserService, private router: Router, private cd: ChangeDetectorRef) {}

  ngOnInit(): void {
	// determine whether to show calendar overlay (hide if matchDate already set)
	const existingDate = this.form.get('matchDate')?.value;
	if (existingDate) {
	  this.showCalendarOverlay = false;
	  this.dateReadOnly = true; // keep the date visually non-editable but allow click to open calendar
	  // leave control enabled for validation
	} else {
	  this.showCalendarOverlay = true;
	  this.dateReadOnly = false;
	}

	// prefill organiser if provided and fetch organiser name (first + last only)
	if (this.organiserId) {
	  this.form.get('organiserId')?.setValue(this.organiserId);
	  // try to fetch user profile (will set organiserName to "First Last" only)
	  try {
		this.userService.getUserById(this.organiserId).subscribe({
		  next: (u: any) => {
			const fname = (u && u.firstName) || '';
			const lname = (u && u.lastName) || '';
			const full = (fname + ' ' + lname).trim();
			if (full) {
			  this.organiserName = full;
			  this.cd.detectChanges();
			}
		  },
		  error: () => {
			// ignore; organiserName may be provided by parent
		  }
		});
	  } catch (e) {
		// ignore
	  }
	}

	// prefill type if provided and disable changing it
	if (this.defaultType) {
	  this.form.get('type')?.setValue(this.defaultType);
	  this.form.get('type')?.disable();
	}

	// load available fields for selection
	this.fieldService.getAllFields().subscribe({
	  next: (data) => {
		this.fields = data || [];
		this.cd.detectChanges();
	  },
	  error: (err) => {
		console.error('Failed to load fields', err);
		this.fields = [];
	  }
	});
  }

  onDateSelected(date: Date | null): void {
	this.tempSelectedDate = date;
  }

  onCalendarConfirm(date: Date): void {
	if (!date) return;
	this.tempSelectedDate = date;
	this.confirmDate();
  }

  onCalendarCancel(): void {
	// simply hide overlay without setting a date
	this.showCalendarOverlay = false;
  }

  private formatDateForInput(d: Date): string {
	const y = d.getFullYear();
	const m = (d.getMonth() + 1).toString().padStart(2, '0');
	const day = d.getDate().toString().padStart(2, '0');
	return `${y}-${m}-${day}`;
  }

  confirmDate(): void {
	if (!this.tempSelectedDate) return;
	const formatted = this.formatDateForInput(this.tempSelectedDate);
	this.form.get('matchDate')?.setValue(formatted);
	// keep the control enabled but mark readonly so user cannot type directly
	this.dateReadOnly = true;
	this.showCalendarOverlay = false;
	this.cd.detectChanges();
  }

  onDateInputClick(): void {
	// open calendar overlay for selecting a new date
	// set tempSelectedDate from current form value if present
	const v = this.form.get('matchDate')?.value;
	if (v) {
	  // expect yyyy-mm-dd
	  const d = new Date(v);
	  if (!isNaN(d.getTime())) {
		this.tempSelectedDate = d;
	  } else {
		this.tempSelectedDate = null;
	  }
	} else {
	  this.tempSelectedDate = null;
	}
	// clear start and end times when re-selecting date
	this.form.get('startTime')?.setValue(null);
	this.form.get('endTime')?.setValue(null);
	this.showCalendarOverlay = true;
	this.dateReadOnly = false;
  }

  isPrivate(): boolean {
	const t = this.form.get('type')?.value || this.defaultType;
	return t === 'private';
  }

  get invitesControls(): any[] {
	return (this.form.get('invites') as FormArray).controls as any[];
  }

  submit(): void {
	this.error = null;
	this.successMessage = null;
	if (this.form.invalid) {
	  this.error = 'Form is invalid. Please check required fields.';
	  return;
	}

		  // build DTO according to generated MatchCreationDto (omit hidden fields)
		   const dto: any = {
			 fieldId: Number(this.form.get('fieldId')?.value),
			 type: this.defaultType ?? this.form.get('type')?.value,
			 matchDate: this.form.get('matchDate')?.value,
			 startTime: this.form.get('startTime')?.value,
			 endTime: this.form.get('endTime')?.value,
			 organiserId: this.form.get('organiserId')?.value,
		   };

	if (this.isPrivate()) {
	  const invites = (this.form.get('invites') as FormArray).controls.map(c => c.value).filter((v: any) => v && v.toString().trim() !== '');
	  dto.invites = invites;
	}

	// Set Authorization header from AuthService token (if available)
	const token = this.authService.getToken();
	if (token) {
	  this.matchCreationService.defaultHeaders = this.matchCreationService.defaultHeaders.set('Authorization', `Bearer ${token}`);
	}

	this.loading = true;
	this.matchCreationService.create(dto).subscribe({
	  next: (resp: any) => {
		this.loading = false;
		const id = resp && resp['matchId'];
		this.successMessage = id ? `Match created (id=${id})` : 'Match created';
		// optionally navigate to user's home after creating
		if (this.organiserId) {
		  this.router.navigate(['/home', this.organiserId]);
		}
	  },
	  error: (err) => {
		console.error('Create match failed', err);
		this.loading = false;
		this.error = err?.message || 'Failed to create match';
		this.cd.detectChanges();
	  }
	});
  }
}




