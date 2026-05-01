import { Routes, CanActivateFn, Router } from '@angular/router';
import { HomeComponent } from './layout/content/pages/home/home';
import { HomeAccount } from './layout/content/pages/home-account/home-account';
import { NewPubMatch } from './layout/content/pages/new-pub-match/new-pub-match';
import { inject } from '@angular/core';
import { AuthService } from './services/auth.service';
import { UserService } from './services/user.service';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { NewPrivMatch } from './layout/content/pages/new-priv-match/new-priv-match';
import { InvitePaymentsPage } from './layout/content/pages/payments/invite-payments';
import { JoinPublicMatch } from './layout/content/pages/join-public-match/join-public-match';
import { MyMatches } from './layout/content/pages/my-matches/my-matches';

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
      // If the authenticated user is the requested user, allow access except for
      // specific role-based restrictions on create routes.
      if (currentMatricule === requestedId) {
        const roleId = u?.roleId ?? -1;
        const url = state.url || '';
        const isAdmin = [7, 9].includes(Number(roleId));

        // Protect private match creation from admins
        if (url.includes('create_pmatch') && isAdmin) {
          // redirect admins to their home
          return router.createUrlTree(['/home', currentMatricule]);
        }

        // Protect public match creation from non-admin users
        if (url.includes('create_public') && !isAdmin) {
          return router.createUrlTree(['/home', currentMatricule]);
        }

        // If trying to access create_pmatch manually and the user has a restriction
        // (active penalty or debt), redirect to home and set a query param that will
        // trigger a popup on the UI.
        if (url.includes('create_pmatch') && (String(u?.account?.status || '').toLowerCase() === 'debt'
            || (typeof u?.account?.balance === 'number' && u.account.balance < 0)
            || (Array.isArray(u?.penalties) && u.penalties.some((p: any) => p && p.isActive)))) {
          return router.createUrlTree(['/home', currentMatricule], { queryParams: { blocked: '1' } });
        }

        return true;
      }

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
  { path: 'home/:userId/create_public', component: NewPubMatch, canActivate: [authGuard] },
  { path: 'home/:userId/invites', component: InvitePaymentsPage, canActivate: [authGuard] },
  { path: 'home/:userId/join_public', component: JoinPublicMatch, canActivate: [authGuard] },
  { path: 'home/:userId/my_matches', component: MyMatches, canActivate: [authGuard] },
  { path: 'home/:userId', component: HomeAccount, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
