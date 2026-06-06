import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, NavigationEnd, Router} from '@angular/router';
import {Subject} from 'rxjs';
import {MenuItem} from 'primeng/api';

import {BreadcrumbReloadComponent} from './breadcrumb-reload.component';
import {AuthService} from '../../../services/auth.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';

// ── Helpers ───────────────────────────────────────────────────────────────────

function makeRoute(segments: string[], breadcrumb: string | undefined, children: any[] = []): any {
  return {
    snapshot: {
      url: segments.map(s => ({ path: s })),
      data: breadcrumb !== undefined ? { breadcrumb } : {}
    },
    children
  };
}

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('BreadcrumbReloadComponent', () => {
  let component: BreadcrumbReloadComponent;
  let fixture: ComponentFixture<BreadcrumbReloadComponent>;
  let authServiceSpy: jasmine.SpyObj<any>;
  let breadcrumbMock: { breadcrumbs: jasmine.Spy; setBaseBreadcrumbs: jasmine.Spy };
  let routerSpy: jasmine.SpyObj<Router>;
  let routerEvents$: Subject<any>;

  beforeEach(async () => {
    routerEvents$ = new Subject<any>();

    authServiceSpy = jasmine.createSpyObj('AuthService', ['isAdmin', 'isManager', 'isDriver']);
    authServiceSpy.isAdmin.and.returnValue(false);
    authServiceSpy.isManager.and.returnValue(false);
    authServiceSpy.isDriver.and.returnValue(false);

    breadcrumbMock = {
      breadcrumbs: jasmine.createSpy('breadcrumbs').and.returnValue([]),
      setBaseBreadcrumbs: jasmine.createSpy('setBaseBreadcrumbs')
    };

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: routerEvents$.asObservable(),
      url: '/admin/products'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    await TestBed.configureTestingModule({
      imports: [BreadcrumbReloadComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: BreadcrumbService, useValue: breadcrumbMock },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { root: { children: [] } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BreadcrumbReloadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── configureHomeNode ─────────────────────────────────────────────────────────

  describe('configureHomeNode', () => {
    it('should set home.label to "Inicio"', () => {
      expect(component.home.label).toBe('Inicio');
    });

    it('should set home.routerLink to "/" for non-staff users', () => {
      expect(component.home.routerLink).toBe('/');
    });

    it('should set home.routerLink to "/admin" for admin', () => {
      authServiceSpy.isAdmin.and.returnValue(true);
      (component as any)['configureHomeNode']();
      expect(component.home.routerLink).toBe('/admin');
    });

    it('should set home.routerLink to "/admin" for manager', () => {
      authServiceSpy.isManager.and.returnValue(true);
      (component as any)['configureHomeNode']();
      expect(component.home.routerLink).toBe('/admin');
    });

    it('should set home.routerLink to "/admin" for driver', () => {
      authServiceSpy.isDriver.and.returnValue(true);
      (component as any)['configureHomeNode']();
      expect(component.home.routerLink).toBe('/admin');
    });
  });

  // ── updateBreadcrumbs ─────────────────────────────────────────────────────────

  describe('updateBreadcrumbs', () => {
    it('should call setBaseBreadcrumbs on init with the current router URL', () => {
      expect(breadcrumbMock.setBaseBreadcrumbs).toHaveBeenCalledWith(jasmine.any(Array), '/admin/products');
    });

    it('should pass an empty array when route root has no children', () => {
      expect(breadcrumbMock.setBaseBreadcrumbs).toHaveBeenCalledWith([], '/admin/products');
    });
  });

  // ── createBreadcrumbs ─────────────────────────────────────────────────────────

  describe('createBreadcrumbs', () => {
    function crumbs(root: any): MenuItem[] {
      return (component as any)['createBreadcrumbs'](root);
    }

    it('should return empty array when route has no children', () => {
      expect(crumbs({ children: [] })).toEqual([]);
    });

    it('should include a segment with a valid non-Inicio label', () => {
      const root = { children: [makeRoute(['admin'], 'Administración')] };
      const result = crumbs(root);
      expect(result.length).toBe(1);
      expect(result[0].label).toBe('Administración');
      expect(result[0].routerLink).toBe('/admin');
    });

    it('should exclude route segments whose label is "Inicio"', () => {
      const root = { children: [makeRoute(['home'], 'Inicio')] };
      expect(crumbs(root).length).toBe(0);
    });

    it('should exclude route segments with no breadcrumb data', () => {
      const root = { children: [makeRoute(['silent'], undefined)] };
      expect(crumbs(root).length).toBe(0);
    });

    it('should still traverse children of a segment with an empty URL', () => {
      const child = makeRoute(['products'], 'Productos');
      const parent = makeRoute([], 'Admin', [child]);
      const result = crumbs({ children: [parent] });
      expect(result.some((b: any) => b.label === 'Productos')).toBeTrue();
    });

    it('should build the correct routerLink for nested route segments', () => {
      const leaf = makeRoute(['products'], 'Productos');
      const middle = makeRoute(['admin'], 'Admin', [leaf]);
      const result = crumbs({ children: [middle] });
      const productCrumb = result.find((b: any) => b.label === 'Productos');
      expect(productCrumb).toBeDefined();
      expect(productCrumb!.routerLink).toBe('/admin/products');
    });

    it('should accumulate both parent and child breadcrumbs', () => {
      const leaf = makeRoute(['shops'], 'Tiendas');
      const middle = makeRoute(['admin'], 'Admin', [leaf]);
      const result = crumbs({ children: [middle] });
      expect(result.length).toBe(2);
      expect(result[0].label).toBe('Admin');
      expect(result[1].label).toBe('Tiendas');
    });
  });

  // ── router events ─────────────────────────────────────────────────────────────

  describe('router events', () => {
    it('should re-call setBaseBreadcrumbs on NavigationEnd', () => {
      const countBefore = breadcrumbMock.setBaseBreadcrumbs.calls.count();
      routerEvents$.next(new NavigationEnd(1, '/admin/new', '/admin/new'));
      expect(breadcrumbMock.setBaseBreadcrumbs.calls.count()).toBe(countBefore + 1);
    });

    it('should re-run configureHomeNode on NavigationEnd', () => {
      authServiceSpy.isAdmin.and.returnValue(true);
      routerEvents$.next(new NavigationEnd(1, '/admin/new', '/admin/new'));
      expect(component.home.routerLink).toBe('/admin');
    });

    it('should NOT react to non-NavigationEnd events', () => {
      const countBefore = breadcrumbMock.setBaseBreadcrumbs.calls.count();
      routerEvents$.next({ type: 'NavigationStart' });
      expect(breadcrumbMock.setBaseBreadcrumbs.calls.count()).toBe(countBefore);
    });
  });

  // ── triggerReload ─────────────────────────────────────────────────────────────

  describe('triggerReload', () => {
    it('should emit onReload when called directly', () => {
      let emitted = false;
      component.reload.subscribe(() => emitted = true);
      component.triggerReload();
      expect(emitted).toBeTrue();
    });
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should render the p-breadcrumb component', () => {
      expect(fixture.nativeElement.querySelector('p-breadcrumb')).toBeTruthy();
    });

    it('should render the reload button with pi-refresh icon', () => {
      expect(fixture.nativeElement.querySelector('p-button[icon="pi pi-refresh"]')).toBeTruthy();
    });

    it('should emit onReload when the refresh button is clicked', () => {
      let emitted = false;
      component.reload.subscribe(() => emitted = true);
      fixture.nativeElement.querySelector('button').click();
      expect(emitted).toBeTrue();
    });
  });
});
