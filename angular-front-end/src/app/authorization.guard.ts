import { ActivatedRouteSnapshot, CanActivateFn, Router, UrlTree } from '@angular/router';
import { inject } from '@angular/core';
import { UserService } from './utils/user.service';

export const authorizationGuard: CanActivateFn = (route: ActivatedRouteSnapshot): boolean | UrlTree => {
  const userService = inject(UserService);
  const router = inject(Router);
  const requiredRoles = route.data?.roles as string[] | undefined;
  const token = window.sessionStorage.getItem('token');
  const currentRole = userService.getCurrentRole();

  if (!token) {
    return router.parseUrl('/');
  }

  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }

  if (currentRole && requiredRoles.includes(currentRole)) {
    return true;
  }

  return router.parseUrl('/');
};
