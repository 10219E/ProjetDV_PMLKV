import { Component, ElementRef, OnInit, ViewChild, ChangeDetectorRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormGroup, FormControl, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../../services/auth.service';
import { InfoService } from '../../../../services/info.service';
import { SiteInfo } from '../../../../api/model/siteInfo';
import { UserRegistrationDto } from '../../../../api/model/userRegistrationDto';

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

// allow letters (including common accented ranges), spaces, apostrophe and hyphen
export const NAME_REGEX: RegExp = /^[A-Za-zÀ-ÖØ-öø-ÿ' -]+$/;

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
    // first and last name: at least 2 chars, no symbols (allow accents, spaces, apostrophe and hyphen)
    fname: new FormControl('', [Validators.required, Validators.minLength(2), Validators.pattern(NAME_REGEX)]),
    lname: new FormControl('', [Validators.required, Validators.minLength(2), Validators.pattern(NAME_REGEX)]),
    // email: require RFC-like email and common TLD (2-6 letters)
    email: new FormControl('', [Validators.required, Validators.email, Validators.pattern(/^[^\s@]+@[^\s@]+\.[a-zA-Z]{2,6}$/)]),
    password: new FormControl('', [Validators.required, Validators.minLength(8), passwordStrengthValidator()]),
    confirmPassword: new FormControl('', Validators.required),
    bdate: new FormControl('', [Validators.required, ageValidator(16)]),
    lvl: new FormControl('', Validators.required),
    siteId: new FormControl('', Validators.required)
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
  sites: SiteInfo[] = [];

  constructor(private cdr: ChangeDetectorRef, private title: Title, private authService: AuthService, private router: Router, private infoService: InfoService) {}


  ngOnInit() {
    this.setupObserver();
    this.title.setTitle('Padel Belgium');
    this.loadCounts();
    this.loadSites();
  }

  loadSites() {
    this.infoService.getSites().subscribe({
      next: (data: SiteInfo[]) => {
        this.sites = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load sites', err);
        this.sites = [];
        this.cdr.detectChanges();
      }
    });
  }

  loadCounts() {
    this.infoService.getCounts().subscribe({
      next: dto => {
        this.sitesTotal = dto.sites || 0;
        this.fieldsTotal = dto.fields || 0;
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
      const formValue = this.signupForm.value;

      // Ensure the form value conforms to the UserRegistrationDto
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
          this.signupSuccess = true;
          // keep the signup overlay closed but show the success popup
          this.closeSignup();
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

  /** Close the signup-success popup without opening login */
  closeSignupSuccess() {
    this.signupSuccess = false;
    this.cdr.detectChanges();
  }

  /** Close popup and open the login overlay */
  ackSignupSuccessOpenLogin() {
    this.signupSuccess = false;
    this.openLogin();
    this.cdr.detectChanges();
  }

  submitLogin() {
    if (this.loginForm.valid) {
      this.loginError = null;
      const { email, password } = this.loginForm.value;
      if (email && password) {
        // Pass an AuthLoginDto object as required by the updated service
        this.authService.login({ login: email, password: password }).subscribe({
          next: (response) => {
            console.log('Login Successful', response);
            this.closeLogin();
            this.cdr.detectChanges();
            // after successful login, fetch the user's matricule and navigate to /home/:userId
            this.authService.getMatriculeByEmail(email).subscribe({
              next: (matricule) => {
                if (matricule) {
                  this.router.navigate(['/home', matricule]);
                } else {
                  // fallback to root as requested
                  this.router.navigate(['/']);
                }
              },
              error: (err) => {
                console.error('Failed to retrieve matricule', err);
                // On error, fallback to root
                this.router.navigate(['/']);
              }
            });
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
