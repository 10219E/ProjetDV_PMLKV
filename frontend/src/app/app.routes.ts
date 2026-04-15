import { Routes } from '@angular/router';
import { HomeComponent } from './layout/content/pages/home/home';
import { HomeAccount } from './layout/content/pages/home-account/home-account';

export const routes: Routes = [
  { path: '', component: HomeComponent, pathMatch: 'full' },
  { path: 'home/:userId', component: HomeAccount },
  { path: '**', redirectTo: '' }
];
