import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {ActivatedRoute, Router} from '@angular/router';
import {Subject} from 'rxjs';

import {FooterComponent} from './footer.component';
import {AuthService} from '../../../services/auth.service';

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('FooterComponent', () => {
  let component: FooterComponent;
  let fixture: ComponentFixture<FooterComponent>;
  let authServiceSpy: jasmine.SpyObj<any>;
  let routerEvents$: Subject<any>;

  beforeEach(async () => {
    routerEvents$ = new Subject<any>();

    authServiceSpy = jasmine.createSpyObj('AuthService', ['isAdmin', 'isManager', 'isDriver']);
    authServiceSpy.isAdmin.and.returnValue(false);
    authServiceSpy.isManager.and.returnValue(false);
    authServiceSpy.isDriver.and.returnValue(false);

    const routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: routerEvents$.asObservable(),
      url: '/'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    await TestBed.configureTestingModule({
      imports: [FooterComponent],
      providers: [
        provideNoopAnimations(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => null }, url: [], data: {} },
            root: { children: [] }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FooterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Brand — non-admin mode ────────────────────────────────────────────────────

  describe('non-admin mode (default)', () => {
    it('should show the "TecHub" brand name', () => {
      expect(fixture.nativeElement.textContent).toContain('TecHub');
    });

    it('should show the shop logo image', () => {
      expect(fixture.nativeElement.querySelector('img[src="/shopLogo.png"]')).toBeTruthy();
    });

    it('should NOT show the "Frict" brand span', () => {
      const spans: HTMLSpanElement[] = Array.from(fixture.nativeElement.querySelectorAll('span'));
      const brandSpan = spans.find(el => el.textContent?.trim() === 'Frict');
      expect(brandSpan).toBeFalsy();
    });

    it('should NOT show the Frict logo image', () => {
      expect(fixture.nativeElement.querySelector('img[src="/frictLogo.png"]')).toBeFalsy();
    });
  });

  // ── Brand — admin mode ────────────────────────────────────────────────────────

  describe('admin mode', () => {
    beforeEach(() => {
      authServiceSpy.isAdmin.and.returnValue(true);
      fixture.detectChanges();
    });

    it('should show the "Frict" brand span', () => {
      const spans: HTMLSpanElement[] = Array.from(fixture.nativeElement.querySelectorAll('span'));
      const brandSpan = spans.find(el => el.textContent?.trim() === 'Frict');
      expect(brandSpan).toBeTruthy();
    });

    it('should show the Frict logo image', () => {
      expect(fixture.nativeElement.querySelector('img[src="/frictLogo.png"]')).toBeTruthy();
    });

    it('should NOT show "TecHub"', () => {
      expect(fixture.nativeElement.textContent).not.toContain('TecHub');
    });

    it('should NOT show the shop logo image', () => {
      expect(fixture.nativeElement.querySelector('img[src="/shopLogo.png"]')).toBeFalsy();
    });
  });

  // ── Navigation links ──────────────────────────────────────────────────────────

  describe('navigation links', () => {
    it('should render an "Acerca de" link', () => {
      expect(fixture.nativeElement.textContent).toContain('Acerca de');
    });

    it('should render a "Licencia" link', () => {
      expect(fixture.nativeElement.textContent).toContain('Licencia');
    });

    it('should render a "Contacto" link', () => {
      expect(fixture.nativeElement.textContent).toContain('Contacto');
    });
  });

  // ── Copyright ─────────────────────────────────────────────────────────────────

  describe('copyright', () => {
    it('should display the copyright text "2026 The Frict Project"', () => {
      expect(fixture.nativeElement.textContent).toContain('2026 The Frict Project');
    });
  });

  // ── Home link ─────────────────────────────────────────────────────────────────

  describe('home link', () => {
    it('should have a routerLink to "/" on the brand anchor', () => {
      expect(fixture.nativeElement.querySelector('a[routerLink="/"]')).toBeTruthy();
    });
  });
});
