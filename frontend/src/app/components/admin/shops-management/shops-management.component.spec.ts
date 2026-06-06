import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ShopsManagementComponent} from './shops-management.component';
import {ShopService} from '../../../services/shop.service';
import {UserService} from '../../../services/user.service';
import {AuthService} from '../../../services/auth.service';
import {UiService} from '../../../utils/ui.service';
import {NotificationService} from '../../../services/notification.service';
import {MessageService} from 'primeng/api';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {Shop} from '../../../models/shop.model';
import {User} from '../../../models/user.model';
import {Notification} from '../../../models/notification.model';
import {PageResponse} from '../../../models/pageResponse.model';

// BreadcrumbReloadComponent (child) injects Router, ActivatedRoute and BreadcrumbService.
// AuthService.isAdmin / isManager are Angular computed signals (callable functions);
// the mock exposes them as jasmine spies so template calls like authService.isAdmin() work.
// NotificationService opens a WebSocket in its constructor via effect(); must be fully mocked.
// Several component fields are protected; accessed via (component as any).
//
// Leaflet strategy: the component calls L.map() inside a setTimeout(() => {...}, 10) that
// fires asynchronously after loadShops resolves. spyOn(L, 'map') cannot intercept it because
// webpack resolves Leaflet exports by direct reference, bypassing the namespace object.
// To stop the timer from firing at all, we split beforeEach into two phases:
//   - Phase 1 (async): configures TestBed — must be async-clean before jasmine.clock().
//   - Phase 2 (sync): installs jasmine.clock(), creates the component, and calls
//     detectChanges(). The setTimeout(10) inside loadShops is now frozen and never fires.
// jasmine.clock() is uninstalled in afterEach to restore real timers for the next spec.

describe('ShopsManagementComponent', () => {
  let component: ShopsManagementComponent;
  let fixture: ComponentFixture<ShopsManagementComponent>;
  let shopServiceSpy: jasmine.SpyObj<ShopService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let authServiceMock: {
    isAdmin: jasmine.Spy;
    isManager: jasmine.Spy;
    isLogged: jasmine.Spy;
    isDriver: jasmine.Spy;
    isUser: jasmine.Spy;
  };
  let uiServiceMock: { getVisualsByType: jasmine.Spy };
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;
  let breadcrumbServiceSpy: jasmine.SpyObj<BreadcrumbService>;
  let routerSpy: jasmine.SpyObj<Router>;

  // Shorthand to bypass protected access in state assertions
  const c = () => component as any;

  // ─── Mock data ────────────────────────────────────────────────────────────────

  const mockAddress = {
    id: 'addr-1', alias: 'Almacén Central', street: 'Gran Vía', number: '10',
    floor: '', postalCode: '28013', city: 'Madrid', country: 'España',
    latitude: 40.4168, longitude: -3.7038
  };

  const mockImageInfo = {
    id: 'img-1', imageUrl: 'http://img.test/shop.png',
    s3Key: 'shops/shop.png', fileName: 'shop.png'
  };

  const mockManager: User = {
    id: 'manager-1',
    name: 'Laura Martínez',
    username: 'lauramartinez',
    roles: ['MANAGER'],
    email: 'laura@test.com',
    phone: '+34 600 111 222',
    addresses: [],
    cards: [],
    imageInfo: { id: 'img-m', imageUrl: 'http://img.test/manager.png', s3Key: 'users/m.png', fileName: 'm.png' } as any,
    banned: false,
    deleted: false,
    selectedShopId: null,
    ordersCount: 0,
    favouriteProductsCount: 0,
    connection: { online: true, lastConnection: '2026-05-09T10:00:00', lastSessionDurationSeconds: 60 } as any
  };

  const mockManager2: User = {
    ...mockManager,
    id: 'manager-2',
    name: 'Carlos Ruiz',
    username: 'carlosruiz',
    email: 'carlos@test.com',
    connection: null
  };

  const mockShopWithManager: Shop = {
    id: 'shop-1',
    referenceCode: 'SHP-001',
    name: 'Tienda Central Madrid',
    address: mockAddress,
    assignedBudget: 50000,
    maxCapacity: 0,
    occupiedCapacity: 0,
    imageInfo: mockImageInfo as any,
    totalAvailableProducts: 120,
    totalAssignedTrucks: 3,
    assignedManager: mockManager
  };

  const mockShopNoManager: Shop = {
    id: 'shop-2',
    referenceCode: 'SHP-002',
    name: 'Tienda Norte Barcelona',
    address: { ...mockAddress, id: 'addr-2', latitude: 41.3851, longitude: 2.1734 },
    assignedBudget: 30000,
    maxCapacity: 0,
    occupiedCapacity: 0,
    imageInfo: mockImageInfo as any,
    totalAvailableProducts: 85,
    totalAssignedTrucks: 1,
    assignedManager: undefined
  };

  const mockShopsPage: PageResponse<Shop> = {
    items: [mockShopWithManager, mockShopNoManager],
    totalItems: 2,
    currentPage: 0,
    lastPage: 0,
    pageSize: 10
  };

  const emptyPage: PageResponse<Shop> = {
    items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0
  };

  const mockNotification: Notification = {
    id: 'notif-1',
    subject: 'Stock bajo en Tienda Central',
    description: 'El stock de producto X está por debajo del umbral.',
    timestamp: '2026-05-09T09:00:00',
    read: false,
    type: 'SHOP'
  };

  // ─── Phase 1: async TestBed configuration ─────────────────────────────────────
  // Runs before jasmine.clock() is installed to keep the async/await chain clean.

  beforeEach(async () => {
    shopServiceSpy = jasmine.createSpyObj('ShopService', [
      'getAllShopsPage', 'getAssignedShopsPage', 'deleteShop', 'assignManager'
    ]);
    // callFake returns a fresh copy to prevent test pollution when the component
    // mutates shopsPage.items in place (e.g. after assignManager succeeds).
    shopServiceSpy.getAllShopsPage.and.callFake(() =>
      of({
        ...mockShopsPage,
        items: [
          { ...mockShopWithManager, assignedManager: { ...mockManager } },
          { ...mockShopNoManager }
        ]
      })
    );
    shopServiceSpy.getAssignedShopsPage.and.callFake(() =>
      of({ ...mockShopsPage, items: [{ ...mockShopWithManager, assignedManager: { ...mockManager } }] })
    );
    shopServiceSpy.deleteShop.and.returnValue(of({ ...mockShopWithManager }));
    shopServiceSpy.assignManager.and.returnValue(of({ ...mockShopNoManager, assignedManager: mockManager2 }));

    userServiceSpy = jasmine.createSpyObj('UserService', ['getAllUsersByRole']);
    userServiceSpy.getAllUsersByRole.and.returnValue(of([mockManager, mockManager2]));

    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    // isAdmin / isManager are computed signals in the real service — callable functions.
    // Jasmine spies are also callable, so authService.isAdmin() works in templates.
    authServiceMock = {
      isAdmin:   jasmine.createSpy('isAdmin').and.returnValue(true),
      isManager: jasmine.createSpy('isManager').and.returnValue(false),
      isLogged:  jasmine.createSpy('isLogged').and.returnValue(false),
      isDriver:  jasmine.createSpy('isDriver').and.returnValue(false),
      isUser:    jasmine.createSpy('isUser').and.returnValue(false)
    };

    uiServiceMock = {
      getVisualsByType: jasmine.createSpy('getVisualsByType').and.returnValue({
        color: 'bg-blue-50 text-blue-600 border-blue-200',
        icon: 'pi pi-bell',
        tag: 'info'
      })
    };

    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['getRecentNotifications']);
    notificationServiceSpy.getRecentNotifications.and.returnValue(of([]));

    breadcrumbServiceSpy = jasmine.createSpyObj('BreadcrumbService', [
      'setBaseBreadcrumbs', 'insertPenultimateNodesForUrl', 'breadcrumbs'
    ]);
    breadcrumbServiceSpy.breadcrumbs.and.returnValue([]);

    routerSpy = jasmine.createSpyObj(
      'Router',
      ['navigate', 'navigateByUrl', 'createUrlTree', 'serializeUrl'],
      { url: '/admin/shops', events: new Subject<any>(), navigated: false }
    );
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');
    routerSpy.navigateByUrl.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [ShopsManagementComponent, BrowserAnimationsModule],
      providers: [
        { provide: ShopService,          useValue: shopServiceSpy         },
        { provide: UserService,          useValue: userServiceSpy         },
        { provide: MessageService,       useValue: messageServiceSpy      },
        { provide: AuthService,          useValue: authServiceMock        },
        { provide: UiService,            useValue: uiServiceMock          },
        { provide: NotificationService,  useValue: notificationServiceSpy },
        { provide: BreadcrumbService,    useValue: breadcrumbServiceSpy   },
        { provide: Router,               useValue: routerSpy              },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => null }, url: [], data: {} },
            params: of({}),
            root: { children: [] }
          }
        },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();
  });

  // ─── Phase 2: sync component creation with frozen clock ───────────────────────
  // jasmine.clock() is installed HERE (after async) to freeze the setTimeout(10) in
  // loadShops before it can call L.map() against jsdom's unrendered container.

  beforeEach(() => {
    jasmine.clock().install();
    fixture   = TestBed.createComponent(ShopsManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    jasmine.clock().uninstall();
  });

  // ─── Creation ─────────────────────────────────────────────────────────────────

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // ─── ngOnInit calls ───────────────────────────────────────────────────────────

  it('should call getAllShopsPage on init when user is admin', () => {
    expect(shopServiceSpy.getAllShopsPage).toHaveBeenCalledWith(0, 10);
  });

  it('should call getRecentNotifications after loading shops', () => {
    expect(notificationServiceSpy.getRecentNotifications).toHaveBeenCalledWith('SHOP', 3);
  });

  // ─── State after happy-path load ──────────────────────────────────────────────

  it('should set loading=false after successful load', () => {
    expect(c().loading).toBeFalse();
  });

  it('should set error=false after successful load', () => {
    expect(c().error).toBeFalse();
  });

  it('should populate shopsPage after successful load', () => {
    expect(component.shopsPage.items.length).toBe(2);
    expect(component.shopsPage.totalItems).toBe(2);
  });

  // ─── loadShops: admin vs manager branches ─────────────────────────────────────

  it('should call getAssignedShopsPage when user is NOT admin', () => {
    authServiceMock.isAdmin.and.returnValue(false);
    component.loadShops();
    expect(shopServiceSpy.getAssignedShopsPage).toHaveBeenCalledWith(0, 10);
  });

  it('should NOT call getAssignedShopsPage when user is admin', () => {
    component.loadShops();
    expect(shopServiceSpy.getAssignedShopsPage).not.toHaveBeenCalled();
  });

  it('should use correct page and size calculation from first/rows', () => {
    component.first = 20;
    component.rows  = 10;
    component.loadShops();
    expect(shopServiceSpy.getAllShopsPage).toHaveBeenCalledWith(2, 10);
  });

  // ─── Error states ─────────────────────────────────────────────────────────────

  it('should set error=true and loading=false when getAllShopsPage fails', () => {
    shopServiceSpy.getAllShopsPage.and.returnValue(throwError(() => new Error('500')));
    component.loadShops();
    expect(c().error).toBeTrue();
    expect(c().loading).toBeFalse();
  });

  it('should set error=true when getAssignedShopsPage fails', () => {
    authServiceMock.isAdmin.and.returnValue(false);
    shopServiceSpy.getAssignedShopsPage.and.returnValue(throwError(() => new Error('500')));
    component.loadShops();
    expect(c().error).toBeTrue();
  });

  it('should not change loading before the response arrives in non-initial loads', () => {
    const pending$ = new Subject<PageResponse<Shop>>();
    shopServiceSpy.getAllShopsPage.and.returnValue(pending$.asObservable());
    c().loading = false;
    component.loadShops();
    expect(c().loading).toBeFalse();
  });

  // ─── reloadAll ────────────────────────────────────────────────────────────────

  it('should set loading=true and error=false on reloadAll', () => {
    const blocker = new Subject<PageResponse<Shop>>();
    shopServiceSpy.getAllShopsPage.and.returnValue(blocker.asObservable());
    c().error = true;
    component.reloadAll();
    expect(c().loading).toBeTrue();
    expect(c().error).toBeFalse();
  });

  it('should close the assignment dialog on reloadAll', () => {
    c().visibleAssignmentDialog = true;
    c().currentShop             = mockShopWithManager;
    c().selectedManager         = mockManager;
    component.reloadAll();
    expect(c().visibleAssignmentDialog).toBeFalse();
    expect(c().currentShop).toBeUndefined();
    expect(c().selectedManager).toBeUndefined();
  });

  it('should call loadShops again on reloadAll', () => {
    const prevCount = shopServiceSpy.getAllShopsPage.calls.count();
    component.reloadAll();
    expect(shopServiceSpy.getAllShopsPage.calls.count()).toBeGreaterThan(prevCount);
  });

  // ─── deleteShop ───────────────────────────────────────────────────────────────

  it('should call shopService.deleteShop with the correct id', () => {
    component.deleteShop(mockShopWithManager.id);
    expect(shopServiceSpy.deleteShop).toHaveBeenCalledWith(mockShopWithManager.id);
  });

  it('should show success message and reload after deleteShop succeeds', () => {
    const prevCount = shopServiceSpy.getAllShopsPage.calls.count();
    component.deleteShop(mockShopWithManager.id);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    expect(shopServiceSpy.getAllShopsPage.calls.count()).toBeGreaterThan(prevCount);
  });

  it('should include the shop name in the success message detail', () => {
    shopServiceSpy.deleteShop.and.returnValue(of({ ...mockShopWithManager }));
    component.deleteShop(mockShopWithManager.id);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ detail: jasmine.stringContaining(mockShopWithManager.name) })
    );
  });

  it('should show error message when deleteShop fails', () => {
    shopServiceSpy.deleteShop.and.returnValue(throwError(() => new Error('500')));
    component.deleteShop(mockShopWithManager.id);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
  });

  // ─── onPageChange ─────────────────────────────────────────────────────────────

  it('should update first and rows, then reload shops on onPageChange', () => {
    const prevCount = shopServiceSpy.getAllShopsPage.calls.count();
    component.onPageChange({ first: 10, rows: 10, page: 1, pageCount: 3 });
    expect(component.first).toBe(10);
    expect(component.rows).toBe(10);
    expect(shopServiceSpy.getAllShopsPage.calls.count()).toBeGreaterThan(prevCount);
  });

  // ─── getShopRecentNotifications ───────────────────────────────────────────────

  it('should populate recentShopsNotifications signal from service', () => {
    notificationServiceSpy.getRecentNotifications.and.returnValue(of([mockNotification]));
    component.getShopRecentNotifications();
    expect(component.recentShopsNotifications()).toEqual([mockNotification]);
  });

  it('should keep recentShopsNotifications empty when service returns []', () => {
    notificationServiceSpy.getRecentNotifications.and.returnValue(of([]));
    component.getShopRecentNotifications();
    expect(component.recentShopsNotifications().length).toBe(0);
  });

  // ─── showAssignmentDialog ─────────────────────────────────────────────────────

  it('should call getAllUsersByRole with "MANAGER" on showAssignmentDialog', () => {
    component.showAssignmentDialog(mockShopWithManager.id);
    expect(userServiceSpy.getAllUsersByRole).toHaveBeenCalledWith('MANAGER');
  });

  it('should set managers list from service response', () => {
    component.showAssignmentDialog(mockShopWithManager.id);
    expect(c().managers).toEqual([mockManager, mockManager2]);
  });

  it('should set currentShop to the matching shop', () => {
    component.showAssignmentDialog(mockShopWithManager.id);
    expect(c().currentShop?.id).toBe(mockShopWithManager.id);
  });

  it('should preselect the existing assigned manager', () => {
    component.showAssignmentDialog(mockShopWithManager.id);
    expect(c().selectedManager?.id).toBe(mockManager.id);
  });

  it('should set visibleUnassignButton=true when shop has an assigned manager', () => {
    component.showAssignmentDialog(mockShopWithManager.id);
    expect(c().visibleUnassignButton).toBeTrue();
  });

  it('should set visibleUnassignButton=false when shop has no assigned manager', () => {
    component.showAssignmentDialog(mockShopNoManager.id);
    expect(c().visibleUnassignButton).toBeFalse();
  });

  it('should set selectedManager to undefined when shop has no manager', () => {
    component.showAssignmentDialog(mockShopNoManager.id);
    expect(c().selectedManager).toBeUndefined();
  });

  it('should open the assignment dialog', () => {
    component.showAssignmentDialog(mockShopWithManager.id);
    expect(c().visibleAssignmentDialog).toBeTrue();
  });

  it('should show error message when getAllUsersByRole fails', () => {
    userServiceSpy.getAllUsersByRole.and.returnValue(throwError(() => new Error('500')));
    component.showAssignmentDialog(mockShopWithManager.id);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
  });

  it('should NOT open the dialog when getAllUsersByRole fails', () => {
    userServiceSpy.getAllUsersByRole.and.returnValue(throwError(() => new Error('500')));
    component.showAssignmentDialog(mockShopWithManager.id);
    expect(c().visibleAssignmentDialog).toBeFalse();
  });

  it('should NOT set currentShop when shopId does not match any item', () => {
    component.showAssignmentDialog('non-existent-id');
    expect(c().currentShop).toBeUndefined();
  });

  // ─── cancelAssignment ─────────────────────────────────────────────────────────

  it('should close dialog and clear all state on cancelAssignment', () => {
    c().visibleAssignmentDialog = true;
    c().visibleUnassignButton   = true;
    c().currentShop             = mockShopWithManager;
    c().selectedManager         = mockManager;
    component.cancelAssignment();
    expect(c().visibleAssignmentDialog).toBeFalse();
    expect(c().visibleUnassignButton).toBeFalse();
    expect(c().currentShop).toBeUndefined();
    expect(c().selectedManager).toBeUndefined();
  });

  // ─── setManagerAssignment ─────────────────────────────────────────────────────

  it('should call shopService.assignManager with correct args when assigning', () => {
    c().currentShop = mockShopNoManager;
    component.setManagerAssignment(mockManager.id, true);
    expect(shopServiceSpy.assignManager).toHaveBeenCalledWith(
      mockShopNoManager.id, mockManager.id, true
    );
  });

  it('should call shopService.assignManager with state=false when unassigning', () => {
    c().currentShop = mockShopWithManager;
    component.setManagerAssignment(mockManager.id, false);
    expect(shopServiceSpy.assignManager).toHaveBeenCalledWith(
      mockShopWithManager.id, mockManager.id, false
    );
  });

  it('should NOT call shopService.assignManager when userId is undefined', () => {
    c().currentShop = mockShopWithManager;
    component.setManagerAssignment(undefined, true);
    expect(shopServiceSpy.assignManager).not.toHaveBeenCalled();
  });

  it('should NOT call shopService.assignManager when currentShop is undefined', () => {
    c().currentShop = undefined;
    component.setManagerAssignment(mockManager.id, true);
    expect(shopServiceSpy.assignManager).not.toHaveBeenCalled();
  });

  it('should update the matching item in shopsPage after successful assignment', () => {
    const updatedShop = { ...mockShopNoManager, assignedManager: mockManager2 };
    shopServiceSpy.assignManager.and.returnValue(of(updatedShop));
    c().currentShop = mockShopNoManager;
    component.setManagerAssignment(mockManager2.id, true);
    const found = component.shopsPage.items.find(s => s.id === mockShopNoManager.id);
    expect(found?.assignedManager?.id).toBe(mockManager2.id);
  });

  it('should close the assignment dialog after successful assignment', () => {
    c().currentShop            = mockShopNoManager;
    c().visibleAssignmentDialog = true;
    c().selectedManager         = mockManager;
    shopServiceSpy.assignManager.and.returnValue(of({ ...mockShopNoManager, assignedManager: mockManager }));
    component.setManagerAssignment(mockManager.id, true);
    expect(c().visibleAssignmentDialog).toBeFalse();
  });

  it('should show error message when shopService.assignManager fails', () => {
    c().currentShop = mockShopWithManager;
    shopServiceSpy.assignManager.and.returnValue(throwError(() => new Error('500')));
    component.setManagerAssignment(mockManager.id, false);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
  });

  // ─── DOM: loading / error screen ──────────────────────────────────────────────

  it('should show the loading screen when loading=true', () => {
    c().loading = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-loading-screen')).not.toBeNull();
  });

  it('should show the error screen when error=true', () => {
    c().error = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-loading-screen')).not.toBeNull();
  });

  it('should hide the loading screen when fully loaded', () => {
    expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeNull();
  });

  // ─── DOM: main content ────────────────────────────────────────────────────────

  it('should render the page title "Gestor de Tiendas"', () => {
    expect(fixture.nativeElement.textContent).toContain('Gestor de Tiendas');
  });

  it('should render the "Nueva Tienda" button when user is admin', () => {
    expect(fixture.nativeElement.textContent).toContain('Nueva Tienda');
  });

  it('should NOT render the "Nueva Tienda" button when user is not admin', () => {
    authServiceMock.isAdmin.and.returnValue(false);
    authServiceMock.isManager.and.returnValue(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Nueva Tienda');
  });

  it('should display total shops count in the stats card', () => {
    expect(fixture.nativeElement.textContent).toContain(String(mockShopsPage.totalItems));
  });

  // ─── DOM: shops table ─────────────────────────────────────────────────────────

  it('should display shop names in the table', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain(mockShopWithManager.name);
    expect(text).toContain(mockShopNoManager.name);
  });

  it('should display reference codes in the table', () => {
    expect(fixture.nativeElement.textContent).toContain(mockShopWithManager.referenceCode);
  });

  it('should display the assigned manager name when user is admin', () => {
    expect(fixture.nativeElement.textContent).toContain(mockManager.name);
  });

  it('should show "No asignado" for shops without a manager', () => {
    expect(fixture.nativeElement.textContent).toContain('No asignado');
  });

  it('should show the empty-state message when shopsPage is empty', () => {
    component.shopsPage = { ...emptyPage };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No hay tiendas disponibles.');
  });

  // ─── DOM: notifications panel ─────────────────────────────────────────────────

  it('should show "No hay notificaciones recientes." when list is empty', () => {
    expect(fixture.nativeElement.textContent).toContain('No hay notificaciones recientes.');
  });

  it('should display notification subjects when list has items', () => {
    component.recentShopsNotifications.set([mockNotification]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(mockNotification.subject);
  });

  // ─── locateShopOnMap ──────────────────────────────────────────────────────────

  describe('locateShopOnMap', () => {
    it('should call flyTo on the map when map and coordinates are present', () => {
      const mockMap = { flyTo: jasmine.createSpy('flyTo'), remove: jasmine.createSpy('remove') };
      (component as any).map = mockMap;
      component.locateShopOnMap(mockShopWithManager);
      expect(mockMap.flyTo).toHaveBeenCalledWith(
        [mockAddress.latitude, mockAddress.longitude], 14, jasmine.any(Object)
      );
    });

    it('should do nothing when map is undefined', () => {
      (component as any).map = undefined;
      expect(() => component.locateShopOnMap(mockShopWithManager)).not.toThrow();
    });

    it('should do nothing when shop has no coordinates', () => {
      const mockMap = { flyTo: jasmine.createSpy('flyTo'), remove: jasmine.createSpy('remove') };
      (component as any).map = mockMap;
      const shopNoCoords: any = {
        ...mockShopWithManager,
        address: { ...mockAddress, latitude: null, longitude: null }
      };
      component.locateShopOnMap(shopNoCoords);
      expect(mockMap.flyTo).not.toHaveBeenCalled();
    });
  });
});