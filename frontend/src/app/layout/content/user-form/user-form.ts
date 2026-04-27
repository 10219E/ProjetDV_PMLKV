import { Component, Input, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../services/auth.service';
import { SiteInfo } from '../../../api/model/siteInfo';
import { UserRegistrationDto } from '../../../api/model/userRegistrationDto';

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
export class UserFormComponent {
  @Input() sites: SiteInfo[] = [];
  @Output() close = new EventEmitter<void>();
  @Output() openLogin = new EventEmitter<void>();

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

  constructor(private authService: AuthService, private cdr: ChangeDetectorRef) {}

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

	  const userData: UserRegistrationDto = {
		fname: formValue.fname ?? '',
		lname: formValue.lname ?? '',
		email: formValue.email ?? '',
		password: formValue.password ?? '',
		bdate: formValue.bdate ?? '',
		lvl: formValue.lvl ?? '',
		siteId: formValue.siteId ? Number(formValue.siteId) : undefined
	  };

	  this.authService.signup(userData).subscribe({
		next: (response) => {
		  console.log('Signup Successful', response);
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



