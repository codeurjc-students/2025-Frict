import { inject } from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

export type UserRole = 'ADMIN' | 'MANAGER' | 'DRIVER' | 'USER';

export const ROLE_HOME_PATHS: Record<UserRole, string> = {
  USER: '',
  ADMIN: '/admin',
  MANAGER: '/management',
  DRIVER: '/delivery'
};

export const ROLE_PRIORITY: UserRole[] = ['ADMIN', 'MANAGER', 'DRIVER', 'USER'];

export const routeGuard: CanActivateFn = (route: ActivatedRouteSnapshot, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Check Login
  if (!authService.isLogged()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url }});
  }

  const userRoles = authService.userRoles() as UserRole[];
  const allowedRoles = route.data['roles'] as UserRole[];

  // Check whether the user has any of the allowed roles
  const hasAccess = userRoles.some(role => allowedRoles.includes(role));

  if (hasAccess) {
    return true;
  }

  // If user has more than a role, the redirection will be to the page of the highest privileges page (ADMIN > MANAGER > DRIVER > USER)
  const highestRole = ROLE_PRIORITY.find(role => userRoles.includes(role));

  // Search for the redirection route
  const redirectPath = highestRole ? ROLE_HOME_PATHS[highestRole] : '/';

  return router.createUrlTree([redirectPath]);
};
