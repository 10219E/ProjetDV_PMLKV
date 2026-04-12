import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../../services/auth.service';

@Component({
  selector: 'app-home-account',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="w-full min-h-screen flex flex-col items-center justify-center bg-gray-50 p-6">
      <div class="bg-white rounded-2xl shadow-xl p-10 max-w-md w-full text-center">

        <div class="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
          </svg>
        </div>

        <h1 class="text-3xl font-bold text-gray-800 mb-2">Connexion réussie !</h1>
        <p class="text-gray-600 mb-6">Vous avez été identifié avec succès sur le portail Padel Belgium.</p>

        <div class="py-4 border-t border-b border-gray-100 mb-8">
          <p class="text-sm text-gray-500 uppercase font-semibold tracking-wider">Type de compte</p>
          <p class="text-2xl font-bold mt-1"
             [ngClass]="role === 'Admin' ? 'text-purple-600' : 'text-blue-600'">
            {{ role }}
          </p>
        </div>

        <button (click)="logout()" class="text-white font-bold py-3 px-8 rounded-full shadow-lg bg-orange-500 hover:bg-orange-600 transition-colors w-full">
          SE DÉCONNECTER
        </button>
      </div>
    </div>
  `
})
export class HomeAccountComponent implements OnInit {
  role: string = 'Inconnu';

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/']);
    } else {
      this.role = this.authService.getUserRole();

      // Simplify logic for demo display
      if (this.role === 'Normal User') {
         this.role = 'Utilisateur Standard';
      }
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}

