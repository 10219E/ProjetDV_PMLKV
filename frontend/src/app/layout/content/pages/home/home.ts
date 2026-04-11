import { Component, ElementRef, OnInit, ViewChild, ChangeDetectorRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Title } from '@angular/platform-browser';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent implements OnInit {
  @ViewChild('img2Section', { static: true }) img2Section!: ElementRef;
  @ViewChild('aboutSection', { static: true }) aboutSection!: ElementRef;
  @ViewChild('pitchSection', { static: true }) pitchSection!: ElementRef;
  isImg2Visible = false;
  isAboutVisible = false;
  isPitchVisible = false;
  isHeaderOrange = false;

  constructor(private cdr: ChangeDetectorRef, private title: Title) {}


  ngOnInit() {
    this.setupObserver();
    this.title.setTitle('Padel Belgium');
  }

  @HostListener('window:scroll')
  onScroll() {
    if (this.img2Section) {
      const rect = this.img2Section.nativeElement.getBoundingClientRect();
      const headerHeight = 80;
      this.isHeaderOrange = rect.top <= headerHeight && rect.bottom >= headerHeight;
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
          if (entry.target === this.aboutSection?.nativeElement) {
            // show About button only when a majority of the section is visible
            const visible = (entry.intersectionRatio ?? 0) >= 0.6;
            if (visible !== this.isAboutVisible) {
              this.isAboutVisible = visible;
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
    if (this.aboutSection) {
      observer.observe(this.aboutSection.nativeElement);
    }
    if (this.pitchSection) {
      observer.observe(this.pitchSection.nativeElement);
    }
  }
}

export class Home {
}
