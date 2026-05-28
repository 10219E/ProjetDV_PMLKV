import { Component, Input, Output, EventEmitter, ChangeDetectorRef, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../services/auth.service';
import { SiteInfo } from '../../../api/model/siteInfo';

// validators copied from previous Home component
export function passwordsMatchValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
	const password = control.get('password');
	const confirmPassword = control.get('confirmPassword');

	if (password && confirmPassword && password.value !== confirmPassword.value) {
	  return { passwordsMismatch: true };
	}
	return null;
  };
}

export function passwordStrengthValidator(): ValidatorFn {
  const pattern = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@!\-\+&\$€])[A-Za-z0-9@!\-\+&\$€]{8,}$/;
  return (control: AbstractControl): ValidationErrors | null => {
	const v = control.value as string | null | undefined;
	if (!v) return { weakPassword: true };
	return pattern.test(v) ? null : { weakPassword: true };
  };
}

export const NAME_REGEX: RegExp = /^[A-Za-zÀ-ÖØ-öø-ÿ' -]+$/;

export function ageValidator(minAge: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
	const v = control.value;
	if (!v) return null;
	const date = (v instanceof Date) ? v : new Date(v);
	if (isNaN(date.getTime())) return { invalidDate: true };

	const today = new Date();
	let age = today.getFullYear() - date.getFullYear();
	const m = today.getMonth() - date.getMonth();
	if (m < 0 || (m === 0 && today.getDate() < date.getDate())) {
	  age--;
	}
	return age >= minAge ? null : { underAge: { requiredAge: minAge, actualAge: age } };
  };
}

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule],
  templateUrl: './user-form.html',
  styleUrls: ['./user-form.css']
})
export class UserFormComponent implements AfterViewInit, OnDestroy {
  // track if the host element has been moved to document.body
  private _movedToBody: boolean = false;
  @Input() sites: SiteInfo[] = [];
  @Input() prefillEmail?: string | null;
  // optional: prefill the selected site when the form is opened from another component
  @Input() prefillSiteId?: number | string | null;
  @Input() prefillSiteName?: string | null;
  @Input() inviteMode: boolean = false; // when true register as invite (matricule 'L')
  @Output() close = new EventEmitter<void>();
  @Output() openLogin = new EventEmitter<void>();
  @Output() signupCompleted = new EventEmitter<any>();

  showForm: boolean = true;
  signupSuccess: boolean = false;
  signupError: string | null = null;
  showPassword: boolean = false;

  signupForm = new FormGroup({
	fname: new FormControl('', [Validators.required, Validators.minLength(2), Validators.pattern(NAME_REGEX)]),
	lname: new FormControl('', [Validators.required, Validators.minLength(2), Validators.pattern(NAME_REGEX)]),
	email: new FormControl('', [Validators.required, Validators.email, Validators.pattern(/^[^\s@]+@[^\s@]+\.[a-zA-Z]{2,6}$/)]),
	password: new FormControl('', [Validators.required, Validators.minLength(8), passwordStrengthValidator()]),
	confirmPassword: new FormControl('', Validators.required),
	bdate: new FormControl('', [Validators.required, ageValidator(16)]),
	lvl: new FormControl('', Validators.required),
	siteId: new FormControl('', Validators.required)
  }, { validators: passwordsMatchValidator() });

  constructor(private authService: AuthService, private cdr: ChangeDetectorRef, private el: ElementRef) {}

  ngAfterViewInit(): void {
	// move the component host to document.body to avoid stacking-context issues
	// (similar approach used elsewhere in the project for mobile overlays)
	try {
	  // small timeout to ensure view is rendered
	  setTimeout(() => this.moveHostToBody(), 0);
	} catch (e) {
	  console.warn('Failed to move user-form host to body', e);
	}
  }

  ngOnDestroy(): void {
	// cleanup moved host if still present
	try {
	  if (this._movedToBody && this.el && this.el.nativeElement && this.el.nativeElement.parentElement === document.body) {
		// remove from body — Angular will clean up component node
		document.body.removeChild(this.el.nativeElement);
	  }
	} catch (e) {
	  // ignore
	}
  }

  private moveHostToBody(): void {
	try {
	  const host = this.el?.nativeElement as HTMLElement | null;
	  if (!host) return;
	  if (host.parentElement !== document.body) {
		document.body.appendChild(host);
		this._movedToBody = true;
	  }
	} catch (e) {
	  console.warn('moveHostToBody error', e);
	}
  }

  ngOnInit() {
	// if parent prefilled an email (e.g. from match-form invite flow), set it
	if (this.prefillEmail) {
	  this.signupForm.get('email')?.setValue(this.prefillEmail);
	  // keep the field touched so validation messages may show if invalid
	  this.signupForm.get('email')?.markAsTouched();
	}

	// if parent prefilled a site id (e.g. from match-form), set it on the form
	if (this.prefillSiteId !== undefined && this.prefillSiteId !== null) {
	  // control stores string values for binding; ensure we set a string
	  this.signupForm.get('siteId')?.setValue(String(this.prefillSiteId));
	  this.signupForm.get('siteId')?.markAsTouched();
	}

				// If only one site is available (e.g. for Site Admin role 7), auto-select and lock it
				if (this.sites && this.sites.length === 1) {
				  const singleSite = this.sites[0];
				  const sid = singleSite.siteId;
				  if (sid !== undefined && sid !== null) {
					this.signupForm.get('siteId')?.setValue(String(sid));
					this.signupForm.get('siteId')?.disable({ onlySelf: true });
					// Ensure prefillSiteName is set for the readonly display in template
					if (!this.prefillSiteName) {
					  this.prefillSiteName = (singleSite as any).name || (singleSite as any).siteName;
					}
				  }
				}

				// If inviteMode is set, preselect defaults and make the email and site fields non-editable
				if (this.inviteMode) {

				  // prefill email already set above; lock it
				  this.signupForm.get('email')?.disable({ onlySelf: true });

				  // If parent passed a specific site to prefill (match-form selected site), prefer that and lock it.
				  if (this.prefillSiteId !== undefined && this.prefillSiteId !== null && String(this.prefillSiteId).trim() !== '') {
					this.signupForm.get('siteId')?.setValue(String(this.prefillSiteId));
					this.signupForm.get('siteId')?.disable({ onlySelf: true });
				  } else if (this.sites && this.sites.length === 1) {
					// already handled by general logic above, but kept for clarity in inviteMode
					this.signupForm.get('siteId')?.setValue(String(this.sites[0].siteId));
					this.signupForm.get('siteId')?.disable({ onlySelf: true });
				  } else {
					// multiple sites available and no explicit prefill -> allow choosing if not already disabled
					if (this.sites && this.sites.length > 1 && (!this.prefillSiteId)) {
						this.signupForm.get('siteId')?.enable();
					}
				  }
				}
  }

  togglePasswordVisibility() {
	this.showPassword = !this.showPassword;
	this.cdr.detectChanges();
  }

  selectLevel(level: string) {
	this.signupForm.get('lvl')?.setValue(level);
	this.cdr.detectChanges();
  }

  submitSignup() {
	if (this.signupForm.valid) {
	  this.signupError = null;
	  this.signupSuccess = false;
	  const formValue = this.signupForm.value;
	  // Disabled controls (email when inviteMode) are not included in form.value,
	  // so read the email from the control directly to ensure it's submitted.
	  const emailValue = this.signupForm.get('email')?.value ?? formValue.email ?? '';

			  // siteId may be disabled (inviteMode) so read from control first
			  const siteIdControlValue = this.signupForm.get('siteId')?.value ?? formValue.siteId;
			  const userData: any = {
				fname: formValue.fname ?? '',
				lname: formValue.lname ?? '',
				email: emailValue,
				password: formValue.password ?? '',
				bdate: formValue.bdate ?? '',
				lvl: formValue.lvl ?? '',
				siteId: siteIdControlValue ? Number(siteIdControlValue) : undefined
			  };
	  // If we're in invite mode, set the roleId for INVITE users so backend will
	  // generate a matricule with the 'L' prefix server-side (EnumUserRolesType.INVITE => id 0)
	  // (backend generates matricule from roleId via MatriculeHandler.generateMatricule)
	  if (this.inviteMode) {
		userData['roleId'] = 0; // INVITE role -> prefix 'L'
	  }

	  this.authService.signup(userData).subscribe({
		next: (response) => {
		  console.log('Signup Successful', response);
		  // emit event so parent (match-form) can register the created user as an invite
		  try { this.signupCompleted.emit(response); } catch {}
		  // Hide the form and show the success popup inside this component
		  this.showForm = false;
		  this.signupSuccess = true;
		  this.cdr.detectChanges();
		  return response;
		},
		error: (err) => {
		  console.error('Signup Failed', err);
		  this.signupError = "Une erreur est survenue lors de l'inscription.";
		  this.cdr.detectChanges();
		}
	  });
	}
  }

  // Close the whole signup component (parent will remove it)
  doClose() {
	this.close.emit();
  }

  // Called when user acknowledges success and wants to open Login
  ackSignupOpenLogin() {
	this.openLogin.emit();
	// ask parent to close this component
	this.close.emit();
  }

  // Close success popup only and keep form hidden; parent remains responsible for removing component
  closeSignupSuccess() {
	this.signupSuccess = false;
	this.cdr.detectChanges();
  }
}



