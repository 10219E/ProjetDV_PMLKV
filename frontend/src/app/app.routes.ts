import { Routes, CanActivateFn, Router } from '@angular/router';
import { HomeComponent } from './layout/content/pages/home/home';
import { HomeAccount } from './layout/content/pages/home-account/home-account';
import { NewPrivMatch } from './layout/content/pages/new-priv-match/new-priv-match';
import { inject } from '@angular/core';
import { AuthService } from './services/auth.service';
import { UserService } from './services/user.service';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const userService = inject(UserService);

  // not logged in -> redirect to login with redirectUrl
  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login'], { queryParams: { redirectUrl: state.url } });
  }

  // if there's no userId in the route, allow access
  const requestedId = route.paramMap.get('userId');
  if (!requestedId) {
    // no userId requested: redirect authenticated users to their own home
    // if we cannot determine current matricule, fall back to root
    return userService.getCurrentUser().pipe(
      map(u => {
        const currentMatricule = u?.matricule;
        if (currentMatricule) {
          return router.createUrlTree(['/home', currentMatricule]);
        }
        return router.createUrlTree(['/']);
      }),
      catchError(() => of(router.createUrlTree(['/'])))
    );
  }

  // verify current user matches requestedId
  return userService.getCurrentUser().pipe(
    map(u => {
      const currentMatricule = u?.matricule;
      if (currentMatricule === requestedId) return true;
      // mismatch -> redirect to the authenticated user's home
      if (currentMatricule) {
        return router.createUrlTree(['/home', currentMatricule]);
      }
      return router.createUrlTree(['/']);
    }),
    catchError(() => of(router.createUrlTree(['/'])))
  );
};

export const routes: Routes = [
  { path: '', component: HomeComponent, pathMatch: 'full' },
  // Visiting /home (no userId) should redirect the authenticated user to their own /home/:userId
  { path: 'home', component: HomeComponent, canActivate: [authGuard] },
  { path: 'home/:userId/create_pmatch', component: NewPrivMatch, canActivate: [authGuard] },
  { path: 'home/:userId', component: HomeAccount, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
