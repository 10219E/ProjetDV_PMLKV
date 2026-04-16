import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-nav-menu',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './nav-menu.html',
})
export class NavMenu implements OnInit, OnDestroy {
  visible = false;
  private toggleHandler = () => this.toggle();

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
	window.addEventListener('toggleNavMenu', this.toggleHandler);
  }

  ngOnDestroy(): void {
	window.removeEventListener('toggleNavMenu', this.toggleHandler);
  }

  toggle(): void {
	this.visible = !this.visible;
  }

  close(): void {
	this.visible = false;
  }

  onLogout(): void {
	this.authService.logout();
	this.router.navigate(['/']).then(() => window.location.reload());
  }
}


