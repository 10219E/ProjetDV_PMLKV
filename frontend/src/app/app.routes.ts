import { Routes } from '@angular/router';
import { HomeComponent } from './layout/content/pages/home/home';
import { HomeAccountComponent } from './layout/content/pages/home-account/home-account.component';

export const routes: Routes = [
  { path: '', component: HomeComponent, pathMatch: 'full' },
  { path: 'home/:userId', component: HomeAccountComponent },
  { path: '**', redirectTo: '' }
];
