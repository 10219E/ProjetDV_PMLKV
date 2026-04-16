import { Component, OnInit, AfterViewInit, ViewChild, ElementRef, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InfoService } from '../../../services/info.service';
import { SiteInfo } from '../../../api/model/siteInfo';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './footer.html',
  styleUrl: './footer.css',
})
export class Footer implements OnInit, AfterViewInit, OnDestroy {
  sites: SiteInfo[] = [];

  @ViewChild('scroll', { static: false }) scrollEl!: ElementRef<HTMLElement>;

  private marqueeAnimation?: Animation;
  private resizeHandler = () => this.debouncedSetup();
  private debounceTimer = 0 as any;
  private containerEl?: HTMLElement;
  private pauseHandler = () => this.pauseMarquee();
  private resumeHandler = () => this.resumeMarquee();
  private isDestroyed = false;
  private sitesSub?: any;
  private retryAttempts = 0;
  private readonly maxRetryAttempts = 5;
  private readonly retryIntervalMs = 2000;

  constructor(private infoService: InfoService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadSitesOnce();
  }

  private loadSitesOnce() {
    // unsubscribe previous if any
    if (this.sitesSub) {
      try { this.sitesSub.unsubscribe(); } catch (e) {}
      this.sitesSub = undefined;
    }

    this.sitesSub = this.infoService.getSites().subscribe({
      next: (data: SiteInfo[]) => {
        this.sites = data;
        // Let Angular pick up the change immediately (home.ts pattern)
        try { this.cdr.detectChanges(); } catch (e) {}

        if (data && data.length > 0) {
          // Setup marquee after DOM nodes have been rendered
          setTimeout(() => this.setupMarquee(), 300);
        } else {
          // empty: retry a few times (route reloads/back-end transient empty responses)
          if (this.retryAttempts < this.maxRetryAttempts) {
            this.retryAttempts++;
            setTimeout(() => this.loadSitesOnce(), this.retryIntervalMs);
          }
        }
      },
      error: (err) => {
        console.error('Failed to load sites for footer', err);
        this.sites = [];
        try { this.cdr.detectChanges(); } catch (e) {}
        if (this.retryAttempts < this.maxRetryAttempts) {
          this.retryAttempts++;
          setTimeout(() => this.loadSitesOnce(), this.retryIntervalMs);
        }
      }
    });
  }

  ngAfterViewInit(): void {
    // resize handling
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
    this.isDestroyed = true;
    window.removeEventListener('resize', this.resizeHandler);
    if (this.marqueeAnimation) {
      this.marqueeAnimation.cancel();
    }
    if (this.containerEl) {
      this.containerEl.removeEventListener('mouseenter', this.pauseHandler);
      this.containerEl.removeEventListener('mouseleave', this.resumeHandler);
      this.containerEl.removeEventListener('touchstart', this.pauseHandler as any);
      this.containerEl.removeEventListener('touchend', this.resumeHandler as any);
    }
    if (this.sitesSub) {
      try { this.sitesSub.unsubscribe(); } catch (e) {}
      this.sitesSub = undefined;
    }
  }

  private debouncedSetup() {
    clearTimeout(this.debounceTimer);
    this.debounceTimer = setTimeout(() => this.setupMarquee(), 300);
  }

  private setupMarquee() {
    if (this.isDestroyed) return;

    // Only run if we actually have data to show!
    if (!this.sites || this.sites.length === 0) return;

    try {
      const el = this.scrollEl?.nativeElement;
      if (!el) return;

      const firstSet = el.querySelector('.items-set') as HTMLElement | null;
      if (!firstSet) return;

      const width = firstSet.getBoundingClientRect().width;
      if (!width || width <= 0) {
        // Items haven't been painted yet (e.g. DOM updates in progress). Retry shortly.
        setTimeout(() => this.setupMarquee(), 250);
        return;
      }

      // Compute duration so speed is consistent across screen widths
      const pxPerSecond = 40; // pixels per second
      const durationMs = Math.max(8000, Math.round((width / pxPerSecond) * 1000));

      // Cancel the old animation if it exists
      if (this.marqueeAnimation) {
        this.marqueeAnimation.cancel();
      }

      // Native Web Animations API (No messy <style> tag injection!)
      this.marqueeAnimation = el.animate([
        { transform: 'translateX(0px)' },
        { transform: `translateX(-${width}px)` }
      ], {
        duration: durationMs,
        iterations: Infinity,
        easing: 'linear'
      });

    } catch (e) {
      console.error('Failed to setup marquee animation', e);
    }
  }

  private pauseMarquee() {
    try {
      if (this.marqueeAnimation) this.marqueeAnimation.pause();
    } catch (e) {}
  }

  private resumeMarquee() {
    try {
      if (this.marqueeAnimation) this.marqueeAnimation.play();
    } catch (e) {}
  }

}
