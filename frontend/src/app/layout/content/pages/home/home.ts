import { Component, ElementRef, OnInit, ViewChild, ChangeDetectorRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormGroup, FormControl, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../../services/auth.service';
import { InfoService } from '../../../../services/info.service';

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
  // at least one uppercase, one lowercase, one digit, one special from set @ ! - + & $ €,
  // only allow letters, digits and those special chars, minimum 8 chars
  const pattern = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@!\-\+&\$€])[A-Za-z0-9@!\-\+&\$€]{8,}$/;
  return (control: AbstractControl): ValidationErrors | null => {
    const v = control.value as string | null | undefined;
    if (!v) return { weakPassword: true };
    return pattern.test(v) ? null : { weakPassword: true };
  };
}

export function ageValidator(minAge: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const v = control.value;
    if (!v) return null; // required will handle empty
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
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent implements OnInit {
  @ViewChild('img2Section', { static: true }) img2Section!: ElementRef;
  @ViewChild('aboutSection', { static: true }) aboutSection!: ElementRef;
  @ViewChild('aboutEnd', { static: true }) aboutEnd!: ElementRef;
  @ViewChild('pitchSection', { static: true }) pitchSection!: ElementRef;
  isImg2Visible = false;
  isAboutVisible = false;
  isPitchVisible = false;
  isHeaderGreen = false;
  isSignupOpen = false;
  isLoginOpen = false;
  showPassword = false;

  signupForm = new FormGroup({
    fname: new FormControl('', Validators.required),
    lname: new FormControl('', Validators.required),
    // email: require RFC-like email and common TLD (2-6 letters)
    email: new FormControl('', [Validators.required, Validators.email, Validators.pattern(/^[^\s@]+@[^\s@]+\.[a-zA-Z]{2,6}$/)]),
    password: new FormControl('', [Validators.required, Validators.minLength(8), passwordStrengthValidator()]),
    confirmPassword: new FormControl('', Validators.required),
    bdate: new FormControl('', [Validators.required, ageValidator(16)]),
    lvl: new FormControl('', Validators.required)
  }, { validators: passwordsMatchValidator() });

  loginForm = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email, Validators.pattern(/^[^\s@]+@[^\s@]+\.[a-zA-Z]{2,6}$/)]),
    password: new FormControl('', Validators.required)
  });

  loginError: string | null = null;
  signupError: string | null = null;
  signupSuccess: boolean = false;

  sitesTotal: number = 0;
  fieldsTotal: number = 0;

  constructor(private cdr: ChangeDetectorRef, private title: Title, private authService: AuthService, private router: Router, private infoService: InfoService) {}


  ngOnInit() {
    this.setupObserver();
    this.title.setTitle('Padel Belgium');
    this.loadCounts();
  }

  loadCounts() {
    this.infoService.getCounts().subscribe({
      next: dto => {
        this.sitesTotal = dto.sites;
        this.fieldsTotal = dto.fields;
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('Failed to load counts', err);
      }
    });
  }

  @HostListener('window:scroll')
  onScroll() {
    if (this.img2Section) {
      const rect = this.img2Section.nativeElement.getBoundingClientRect();
      const headerHeight = 80;
      this.isHeaderGreen = rect.top <= headerHeight && rect.bottom >= headerHeight;
    }
  }

  setupObserver() {
    const observer = new IntersectionObserver(
      (entries) => {
        let changed = false;
        entries.forEach((entry) => {
          // check which section intersected
          if (entry.target === this.img2Section?.nativeElement) {
            if (entry.isIntersecting) {
              this.isImg2Visible = true;
              changed = true;
            }
          }
          if (entry.target === this.aboutEnd?.nativeElement) {
            // show About button only when the END of the about section enters the viewport
            // we rely on the sentinel being intersecting to show the button (appears later)
            if ((entry.isIntersecting) !== this.isAboutVisible) {
              this.isAboutVisible = !!entry.isIntersecting;
              changed = true;
            }
          }
          if (entry.target === this.pitchSection?.nativeElement) {
            // show Pitch (Pourquoi venir chez nous) button when a majority is visible
            const visible = (entry.intersectionRatio ?? 0) >= 0.6;
            if (visible !== this.isPitchVisible) {
              this.isPitchVisible = visible;
              changed = true;
            }
          }
        });
        if (changed) {
          this.cdr.detectChanges();
        }
      },
      { threshold: [0, 0.6, 1] }
    );

    if (this.img2Section) {
      observer.observe(this.img2Section.nativeElement);
    }
    // observe the sentinel at the end of the about section to delay the button
    if (this.aboutEnd) {
      observer.observe(this.aboutEnd.nativeElement);
    }
    if (this.pitchSection) {
      observer.observe(this.pitchSection.nativeElement);
    }
  }

  openSignup() {
    this.isSignupOpen = true;
    this.cdr.detectChanges();
  }

  closeSignup() {
    this.isSignupOpen = false;
    this.signupForm.reset();
    this.cdr.detectChanges();
  }

  openLogin() {
    this.isLoginOpen = true;
    this.cdr.detectChanges();
  }

  closeLogin() {
    this.isLoginOpen = false;
    this.loginForm.reset();
    this.cdr.detectChanges();
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
      const userData = this.signupForm.value;

      this.authService.signup(userData).subscribe({
        next: (response) => {
          console.log('Signup Successful', response);
          this.signupSuccess = true;
          this.closeSignup();
        },
        error: (err) => {
          console.error('Signup Failed', err);
          this.signupError = "Une erreur est survenue lors de l'inscription.";
          this.cdr.detectChanges();
        }
      });
    }
  }

  submitLogin() {
    if (this.loginForm.valid) {
      this.loginError = null;
      const { email, password } = this.loginForm.value;
      if (email && password) {
        this.authService.login(email, password).subscribe({
          next: (response) => {
            console.log('Login Successful', response);
            this.closeLogin();
            this.cdr.detectChanges();
            this.router.navigate(['/home-account']);
          },
          error: (err) => {
            console.error('Login Failed', err);
            this.loginError = "Email ou mot de passe incorrect.";
            this.cdr.detectChanges();
          }
        });
      }
    }
  }

  /**
   * Return responsive classes for the header button.
   * - small screens: translucent background and icon-only appearance
   * - md+ screens: add text
   */
  getHeaderClasses(): string {
    const base = 'text-white font-bold text-sm md:text-base py-1.5 md:py-2 px-3 md:px-6 rounded-full shadow-lg transition-colors duration-300 transform hover:scale-105 inline-flex items-center';
    // always translucent and change coulour when over IMAGE 2 (player)
    if (this.isHeaderGreen) {
      return `${base} bg-green-700/80 hover:bg-green-700`;
    }
    return `${base} bg-blue-600/80 hover:bg-blue-600`;
  }
}

export class Home {
}
