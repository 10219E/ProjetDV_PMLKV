import { Component, Input, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators, FormArray } from '@angular/forms';
import { MatchCreationControllerService } from '../../../api/api/matchCreationController.service';
import { FieldControllerService } from '../../../api/api/fieldController.service';
import { AuthService } from '../../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-match-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
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
	type: new FormControl<string | null>(null, [Validators.required]),
	pubStatus: new FormControl<string | null>(null),
	privStatus: new FormControl<string | null>(null),
	matchDate: new FormControl<string | null>(null, [Validators.required]),
	startTime: new FormControl<string | null>(null, [Validators.required]),
	endTime: new FormControl<string | null>(null, [Validators.required]),
	pricing: new FormControl<number | null>(null),
	organiserId: new FormControl<string | null>(null, [Validators.required]),
	invites: new FormArray([
	  new FormControl<string | null>(null),
	  new FormControl<string | null>(null),
	  new FormControl<string | null>(null)
	])
  });

  constructor(private matchCreationService: MatchCreationControllerService, private fieldService: FieldControllerService, private authService: AuthService, private router: Router, private cd: ChangeDetectorRef) {}

  ngOnInit(): void {
	// prefill organiser if provided
	if (this.organiserId) {
	  this.form.get('organiserId')?.setValue(this.organiserId);
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

	// build DTO according to generated MatchCreationDto
	const dto: any = {
	  fieldId: Number(this.form.get('fieldId')?.value),
	  type: this.defaultType ?? this.form.get('type')?.value,
	  pubStatus: this.form.get('pubStatus')?.value,
	  privStatus: this.form.get('privStatus')?.value,
	  matchDate: this.form.get('matchDate')?.value,
	  startTime: this.form.get('startTime')?.value,
	  endTime: this.form.get('endTime')?.value,
	  pricing: this.form.get('pricing')?.value ? Number(this.form.get('pricing')?.value) : undefined,
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




