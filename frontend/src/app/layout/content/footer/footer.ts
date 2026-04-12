import { Component, OnInit, AfterViewInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InfoService } from '../../../services/info.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './footer.html',
  styleUrl: './footer.css',
})
export class Footer implements OnInit, AfterViewInit, OnDestroy {
  sites: Array<{ siteId: number; name: string; address: string }> = [];

  @ViewChild('scroll', { static: false }) scrollEl!: ElementRef<HTMLElement>;

  private styleId = 'footer-marquee-keyframes';
  private resizeHandler = () => this.debouncedSetup();
  private debounceTimer = 0 as any;
  private containerEl?: HTMLElement;
  private pauseHandler = () => this.pauseMarquee();
  private resumeHandler = () => this.resumeMarquee();

  constructor(private infoService: InfoService) {}

  ngOnInit(): void {
    this.infoService.getSites().subscribe({
      next: (data: any) => {
        if (data && Array.isArray(data.siteInfoList)) {
          this.sites = data.siteInfoList;
        } else if (Array.isArray(data)) {
          // fallback if service returns array directly
          this.sites = data;
        } else {
          this.sites = [];
        }
        // schedule marquee setup after content rendered
        setTimeout(() => this.setupMarquee(), 120);
      },
      error: (err) => {
        console.error('Failed to load sites for footer', err);
        this.sites = [];
      }
    });
  }

  ngAfterViewInit(): void {
    // initial setup and resize handling
    setTimeout(() => this.setupMarquee(), 200);
    window.addEventListener('resize', this.resizeHandler);
    // attach pause/resume handlers once view is ready
    setTimeout(() => {
      try {
        const el = this.scrollEl?.nativeElement;
        if (!el) return;
        this.containerEl = el.parentElement as HTMLElement | undefined;
        if (!this.containerEl) return;
        this.containerEl.addEventListener('mouseenter', this.pauseHandler);
        this.containerEl.addEventListener('mouseleave', this.resumeHandler);
        this.containerEl.addEventListener('touchstart', this.pauseHandler, { passive: true } as any);
        this.containerEl.addEventListener('touchend', this.resumeHandler as any);
      } catch (e) {
        // ignore
      }
    }, 300);
  }

  ngOnDestroy(): void {
    window.removeEventListener('resize', this.resizeHandler);
    const existing = document.getElementById(this.styleId);
    if (existing) existing.remove();
    if (this.containerEl) {
      this.containerEl.removeEventListener('mouseenter', this.pauseHandler);
      this.containerEl.removeEventListener('mouseleave', this.resumeHandler);
      this.containerEl.removeEventListener('touchstart', this.pauseHandler as any);
      this.containerEl.removeEventListener('touchend', this.resumeHandler as any);
    }
  }

  private debouncedSetup() {
    clearTimeout(this.debounceTimer);
    this.debounceTimer = setTimeout(() => this.setupMarquee(), 150);
  }

  private setupMarquee() {
    try {
      const el = this.scrollEl?.nativeElement;
      if (!el) return;

      const firstSet = el.querySelector('.items-set') as HTMLElement | null;
      if (!firstSet) return;

      const width = firstSet.getBoundingClientRect().width;
      if (!width || width <= 0) return;

      // compute duration so speed is consistent across widths
      const pxPerSecond = 40; // pixels per second (lower => slower)
      const duration = Math.max(8, Math.round(width / pxPerSecond));

      // remove old keyframes if any
      const existing = document.getElementById(this.styleId);
      if (existing) existing.remove();

      const style = document.createElement('style');
      style.id = this.styleId;
      style.innerHTML = `@keyframes ${this.styleId} { from { transform: translateX(0); } to { transform: translateX(-${width}px); } }`;
      document.head.appendChild(style);

      // apply the animation to the scroll element (inline style overrides CSS)
      el.style.animation = `${this.styleId} ${duration}s linear infinite`;
      el.style.willChange = 'transform';

      // ensure sets are displayed as flex (should already be via CSS)
      el.querySelectorAll('.items-set').forEach((s) => (s as HTMLElement).style.display = 'flex');
    } catch (e) {
      console.error('Failed to setup marquee animation', e);
    }
  }

  private pauseMarquee() {
    try {
      const el = this.scrollEl?.nativeElement;
      if (!el) return;
      (el as HTMLElement).style.animationPlayState = 'paused';
    } catch (e) {
      // ignore
    }
  }

  private resumeMarquee() {
    try {
      const el = this.scrollEl?.nativeElement;
      if (!el) return;
      (el as HTMLElement).style.animationPlayState = 'running';
    } catch (e) {
      // ignore
    }
  }

}
