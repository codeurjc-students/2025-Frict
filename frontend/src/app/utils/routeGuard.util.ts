import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {AuthService} from '../services/auth.service';

export type UserRole = 'ADMIN' | 'MANAGER' | 'DRIVER' | 'USER';

export const ROLE_HOME_PATHS: Record<UserRole, string> = {
  USER: '',
  ADMIN: '/admin',
  MANAGER: '/management',
  DRIVER: '/delivery'
};

export const ROLE_PRIORITY: UserRole[] = ['ADMIN', 'MANAGER', 'DRIVER', 'USER'];

export const routeGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Protected route, but not logged -> Redirect to log in
  if (!authService.isLogged()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url }});
  }

  const allowedRoles = (route.data['roles'] as UserRole[]) || [];

  // No roles defined (no data['roles'] information defined) -> Allow access
  if (allowedRoles.length === 0) {
    console.warn('Protected route without defined access roles.');
    return true;
  }

  // Defined roles: access restricted
  const userRoles = authService.userRoles() as UserRole[];
  const hasAccess = userRoles.some(role => allowedRoles.includes(role));

  if (hasAccess) {
    return true;
  }

  //Redirect to the corresponding role home page
  const highestRole = ROLE_PRIORITY.find(role => userRoles.includes(role));
  const redirectPath = highestRole ? ROLE_HOME_PATHS[highestRole] : '/';

  return router.createUrlTree([redirectPath]);
};
