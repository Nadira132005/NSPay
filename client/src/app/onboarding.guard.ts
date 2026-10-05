import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { catchError, map, of } from 'rxjs';

export const onboardingGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(HttpClient).get('/api/finance/onboarding').pipe(
    map(() => true),
    catchError(() => of(router.parseUrl('/dashboard/overview')))
  );
};
