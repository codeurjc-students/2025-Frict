import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject} from 'rxjs';

import {NavbarComponent} from './navbar.component';
import {AuthService} from '../../../services/auth.service';
import {OrderService} from '../../../services/order.service';
import {CategoryService} from '../../../services/category.service';
import {ProductService} from '../../../services/product.service';
import {NotificationService} from '../../../services/notification.service';
import {LoginInfo} from '../../../models/loginInfo.model';

// ── Helpers ───────────────────────────────────────────────────────────────────

const STUB_LOGIN_INFO: LoginInfo = {
  isLogged: true, imageUrl: '/test.png', id: '1',
  name: 'Test User', username: 'testuser', roles: ['USER'], selectedShopId: null
};

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let fixture: ComponentFixture<NavbarComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let categoryServiceSpy: jasmine.SpyObj<CategoryService>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', [
      'isLogged', 'isUser', 'isAdmin', 'isManager', 'isDriver', 'hasShopSelected', 'getLoginInfo', 'logout'
    ]);
    authServiceSpy.isLogged.and.returnValue(false);
    authServiceSpy.isUser.and.returnValue(false);
    authServiceSpy.isAdmin.and.returnValue(false);
    authServiceSpy.isManager.and.returnValue(false);
    authServiceSpy.isDriver.and.returnValue(false);
    authServiceSpy.hasShopSelected.and.returnValue(false);
    authServiceSpy.getLoginInfo.and.callFake(() => of(STUB_LOGIN_INFO));
    authServiceSpy.logout.and.callFake(() => of({}));

    orderServiceSpy = jasmine.createSpyObj('OrderService', ['itemsCount']);
    orderServiceSpy.itemsCount.and.returnValue(0);

    categoryServiceSpy = jasmine.createSpyObj('CategoryService', ['getAllCategories']);
    categoryServiceSpy.getAllCategories.and.callFake(() => of([]));

    productServiceSpy = jasmine.createSpyObj('ProductService', ['searchScope', 'setSearchScope']);
    productServiceSpy.searchScope.and.returnValue('GLOBAL');

    notificationServiceSpy = jasmine.createSpyObj('NotificationService', [
      'unreadCount', 'unreadNotifications', 'markAsRead', 'triggerTest'
    ]);
    notificationServiceSpy.unreadCount.and.returnValue(0);
    notificationServiceSpy.unreadNotifications.and.returnValue([]);
    notificationServiceSpy.triggerTest.and.callFake(() => of({}));

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: new Subject().asObservable(),
      url: '/'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    await TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: CategoryService, useValue: categoryServiceSpy },
        { provide: ProductService, useValue: productServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
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

    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Default state ─────────────────────────────────────────────────────────────

  describe('default state', () => {
    it('should initialize visible to false', () => {
      expect(component.visible).toBeFalse();
    });

    it('should initialize searchBarInput to empty string', () => {
      expect(component.searchBarInput).toBe('');
    });

    it('should initialize isCategoriesExpanded to true', () => {
      expect(component.isCategoriesExpanded()).toBeTrue();
    });

    it('should initialize isAccountExpanded to true', () => {
      expect(component.isAccountExpanded()).toBeTrue();
    });
  });

  // ── ngOnInit ──────────────────────────────────────────────────────────────────

  describe('ngOnInit', () => {
    it('should call categoryService.getAllCategories on init', () => {
      expect(categoryServiceSpy.getAllCategories).toHaveBeenCalled();
    });

    it('should populate categories from the service response', () => {
      const cats = [{ id: 1, name: 'Tech', icon: null, children: [] }] as any;
      categoryServiceSpy.getAllCategories.and.callFake(() => of(cats));
      component.ngOnInit();
      expect(component.categories).toEqual(cats);
    });

    it('should call authService.getLoginInfo on init', () => {
      expect(authServiceSpy.getLoginInfo).toHaveBeenCalled();
    });

    it('should set loggedUserInfo from the getLoginInfo response', () => {
      expect(component.loggedUserInfo).toEqual(STUB_LOGIN_INFO);
    });
  });

  // ── logout ────────────────────────────────────────────────────────────────────

  describe('logout', () => {
    it('should call authService.logout', () => {
      (component as any)['logout']();
      expect(authServiceSpy.logout).toHaveBeenCalled();
    });

    it('should navigate to /login after logout', () => {
      (component as any)['logout']();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  // ── search ────────────────────────────────────────────────────────────────────

  describe('search', () => {
    it('should navigate to /search with query param when input is non-empty', () => {
      component.searchBarInput = 'laptop';
      (component as any)['search']();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/search'], { queryParams: { query: 'laptop' } });
    });

    it('should clear searchBarInput after navigating', () => {
      component.searchBarInput = 'laptop';
      (component as any)['search']();
      expect(component.searchBarInput).toBe('');
    });

    it('should NOT navigate when searchBarInput is empty', () => {
      component.searchBarInput = '';
      (component as any)['search']();
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
  });

  // ── canViewItem ───────────────────────────────────────────────────────────────

  describe('canViewItem', () => {
    it('should return true for an ADMIN item when user is admin', () => {
      authServiceSpy.isAdmin.and.returnValue(true);
      expect(component.canViewItem({ roles: ['ADMIN'] })).toBeTrue();
    });

    it('should return true for a MANAGER item when user is manager', () => {
      authServiceSpy.isManager.and.returnValue(true);
      expect(component.canViewItem({ roles: ['MANAGER'] })).toBeTrue();
    });

    it('should return true for a DRIVER item when user is driver', () => {
      authServiceSpy.isDriver.and.returnValue(true);
      expect(component.canViewItem({ roles: ['DRIVER'] })).toBeTrue();
    });

    it('should return false when the user has no matching role', () => {
      expect(component.canViewItem({ roles: ['ADMIN'] })).toBeFalse();
    });

    it('should return true for an ADMIN+MANAGER item when user is manager only', () => {
      authServiceSpy.isManager.and.returnValue(true);
      expect(component.canViewItem({ roles: ['ADMIN', 'MANAGER'] })).toBeTrue();
    });
  });

  // ── toggleCategories ──────────────────────────────────────────────────────────

  describe('toggleCategories', () => {
    it('should collapse when currently expanded', () => {
      expect(component.isCategoriesExpanded()).toBeTrue();
      component.toggleCategories();
      expect(component.isCategoriesExpanded()).toBeFalse();
    });

    it('should expand again on a second call', () => {
      component.toggleCategories();
      component.toggleCategories();
      expect(component.isCategoriesExpanded()).toBeTrue();
    });
  });

  // ── toggleAccount ─────────────────────────────────────────────────────────────

  describe('toggleAccount', () => {
    it('should collapse when currently expanded', () => {
      expect(component.isAccountExpanded()).toBeTrue();
      component.toggleAccount();
      expect(component.isAccountExpanded()).toBeFalse();
    });

    it('should expand again on a second call', () => {
      component.toggleAccount();
      component.toggleAccount();
      expect(component.isAccountExpanded()).toBeTrue();
    });
  });

  // ── shouldExpand ──────────────────────────────────────────────────────────────

  describe('shouldExpand', () => {
    it('should return isRouteActive when no manual state is set', () => {
      expect(component.shouldExpand(1, true)).toBeTrue();
      expect(component.shouldExpand(1, false)).toBeFalse();
    });

    it('should return the manual state once set, overriding isRouteActive', () => {
      component.toggleSubmenu(1, false); // inverts false → sets manual state to true
      expect(component.shouldExpand(1, false)).toBeTrue();
    });

    it('should allow manual state to override an active route', () => {
      component.toggleSubmenu(5, true); // inverts true → sets manual state to false
      expect(component.shouldExpand(5, true)).toBeFalse();
    });
  });

  // ── toggleSubmenu ─────────────────────────────────────────────────────────────

  describe('toggleSubmenu', () => {
    it('should set the manual state to the inverse of the current expand state', () => {
      component.toggleSubmenu(10, false); // shouldExpand(10, false) = false → set to true
      expect(component.manualToggleState().get(10)).toBeTrue();
    });

    it('should toggle back to false on a second call', () => {
      component.toggleSubmenu(10, false); // true
      component.toggleSubmenu(10, false); // back to false
      expect(component.manualToggleState().get(10)).toBeFalse();
    });

    it('should track independent state per category id', () => {
      component.toggleSubmenu(1, false);
      component.toggleSubmenu(2, false);
      expect(component.manualToggleState().get(1)).toBeTrue();
      expect(component.manualToggleState().get(2)).toBeTrue();
    });
  });

  // ── onScopeChange ─────────────────────────────────────────────────────────────

  describe('onScopeChange', () => {
    it('should call productService.setSearchScope with the new scope', () => {
      component.onScopeChange('LOCAL');
      expect(productServiceSpy.setSearchScope).toHaveBeenCalledWith('LOCAL');
    });

    it('should call productService.setSearchScope with GLOBAL', () => {
      component.onScopeChange('GLOBAL');
      expect(productServiceSpy.setSearchScope).toHaveBeenCalledWith('GLOBAL');
    });

    it('should NOT call setSearchScope when the value is null', () => {
      component.onScopeChange(null);
      expect(productServiceSpy.setSearchScope).not.toHaveBeenCalled();
    });
  });

  // ── triggerTest ───────────────────────────────────────────────────────────────

  describe('triggerTest', () => {
    it('should call notificationService.triggerTest', () => {
      component.triggerTest();
      expect(notificationServiceSpy.triggerTest).toHaveBeenCalled();
    });
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should render the search input', () => {
      expect(fixture.nativeElement.querySelector('input[placeholder="Buscar productos…"]')).toBeTruthy();
    });

    it('should show the "Menú" label on the drawer toggle button', () => {
      expect(fixture.nativeElement.textContent).toContain('Menú');
    });

    it('should show "TecHub" brand when user is not logged in', () => {
      expect(fixture.nativeElement.textContent).toContain('TecHub');
    });

    it('should show "Frict" brand when logged in as staff (non-user)', () => {
      authServiceSpy.isLogged.and.returnValue(true);
      authServiceSpy.isUser.and.returnValue(false);
      authServiceSpy.isAdmin.and.returnValue(true);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Frict');
    });

    it('should NOT show "Frict" when not logged in', () => {
      expect(fixture.nativeElement.textContent).not.toContain('Frict');
    });

    it('should show the "Acceder" button when not logged in', () => {
      expect(fixture.nativeElement.textContent).toContain('Acceder');
    });

    it('should NOT show the "Acceder" button when logged in', () => {
      authServiceSpy.isLogged.and.returnValue(true);
      authServiceSpy.isUser.and.returnValue(true);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Acceder');
    });

    it('should show the cart button text for a logged-in regular user', () => {
      authServiceSpy.isLogged.and.returnValue(true);
      authServiceSpy.isUser.and.returnValue(true);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Cesta');
    });

    it('should NOT show the cart button for admin users', () => {
      authServiceSpy.isLogged.and.returnValue(true);
      authServiceSpy.isUser.and.returnValue(false);
      authServiceSpy.isAdmin.and.returnValue(true);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Cesta');
    });
  });
});
