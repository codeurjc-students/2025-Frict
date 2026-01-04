import {ApplicationConfig, inject, provideAppInitializer, provideZoneChangeDetection} from '@angular/core';
import {provideRouter, withRouterConfig} from '@angular/router';

import {routes} from './app.routes';

import {provideAnimationsAsync} from '@angular/platform-browser/animations/async';
import {providePrimeNG} from 'primeng/config';
import {MyPreset} from './colorpreset';
import {provideHttpClient} from '@angular/common/http';
import {AuthService} from './services/auth.service';
import {finalize} from 'rxjs';
import {ConfirmationService, MessageService} from 'primeng/api';

export const carouselResponsiveOptions = [
  {
    breakpoint: '1600px',
    numVisible: 5,
    numScroll: 1
  },
  {
    breakpoint: '1400px',
    numVisible: 4,
    numScroll: 1
  },
  {
    breakpoint: '1024px',
    numVisible: 3,
    numScroll: 1
  },
  {
    breakpoint: '768px',
    numVisible: 2,
    numScroll: 1
  },
  {
    breakpoint: '560px',
    numVisible: 1,
    numScroll: 1
  }
];

export const galleryResponsiveOptions = [
  {
    breakpoint: '1300px',
    numVisible: 4
  },
  {
    breakpoint: '575px',
    numVisible: 2
  }
];


function initializeAuth(authService: AuthService): () => Promise<any> {
  return () =>
    new Promise((resolve) => {
      authService.getLoginInfo()
        .pipe(finalize(() => resolve(true)))
        .subscribe();
    });
}

export const appConfig: ApplicationConfig = {
  providers: [provideZoneChangeDetection({ eventCoalescing: true }), provideRouter(routes, withRouterConfig({ onSameUrlNavigation: 'reload' })), provideHttpClient(),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: MyPreset,
        options: {
          darkModeSelector: 'none', //Deactivate Dark Mode at all times
        }
      }
    }),
    provideAppInitializer(() => initializeAuth(inject(AuthService))()),
    MessageService,
    ConfirmationService
  ]
};
