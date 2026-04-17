import { Component, signal, OnInit } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { Footer } from './layout/content/footer/footer';
import { NavMenu } from './layout/content/nav-menu/nav-menu';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Footer, NavMenu],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly title = signal('frontend');
  showGlobalNav = true;

  constructor(private router: Router) {}

  ngOnInit(): void {
    // Hide global nav on routes under /home/:userId because those pages render a page-level NavMenu
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe((ev: any) => {
      const url = ev.urlAfterRedirects || ev.url || '';
      // Hide global nav on any /home route (including /home and /home/:userId/*)
      this.showGlobalNav = !url.startsWith('/');
    });
  }
}
