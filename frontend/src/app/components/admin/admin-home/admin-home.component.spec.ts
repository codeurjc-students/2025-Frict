import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';

import {AdminHomeComponent} from './admin-home.component';
import {AuthService} from '../../../services/auth.service';
import {StatService} from '../../../services/stat.service';
import {OrderService} from '../../../services/order.service';
import {ShopService} from '../../../services/shop.service';
import {TruckService} from '../../../services/truck.service';
import {NotificationService} from '../../../services/notification.service';
import {UiService} from '../../../utils/ui.service';
import {RegistryService} from '../../../services/registry.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {LoginInfo} from '../../../models/loginInfo.model';
import {Notification} from '../../../models/notification.model';

// ── Mock data ──────────────────────────────────────────────────────────────────

const mockAdminInfo: LoginInfo = {
  isLogged: true, imageUrl: '', id: 'user-1', name: 'Admin', username: 'admin',
  roles: ['ADMIN'], selectedShopId: null
};

const mockManagerInfo: LoginInfo = {
  isLogged: true, imageUrl: '', id: 'user-2', name: 'Manager', username: 'manager',
  roles: ['MANAGER'], selectedShopId: null
};

const mockDriverInfo: LoginInfo = {
  isLogged: true, imageUrl: '', id: 'user-3', name: 'Driver', username: 'driver',
  roles: ['DRIVER'], selectedShopId: null
};

const mockOrderStats: any[] = [
  { label: 'Realizados', value: 3 },
  { label: 'Enviados', value: 2 },
  { label: 'En Reparto', value: 1 },
  { label: 'Completados', value: 5 }
];

const mockShopStats: any[] = [
  { label: 'Presupuesto Total', value: 10000 },
  { label: 'Tiendas', value: 4 }
];

const mockTruckStats: any[] = [
  { label: 'Disponibles', value: 3 },
  { label: 'En Ruta', value: 2 },
  { label: 'En mantenimiento', value: 1 },
  { label: 'Fuera de servicio', value: 0 }
];

const mockOrder: any = {
  id: 'order-1', referenceCode: 'ORD-001', createdAt: '2025-01-01',
  user: { name: 'Cliente', imageInfo: { imageUrl: '' }, connection: { online: true } },
  history: [{ status: 'CREATED' }], totalCost: 100,
  sendingAddress: { street: 'Main St', city: 'Madrid', zipCode: '28001' }
};

const mockNotification: Notification = {
  id: 'notif-1', subject: 'Aviso', description: 'Test notification',
  timestamp: '2025-01-01', read: false, type: 'pedido'
};

const mockTruck: any = {
  id: 'truck-1', referenceCode: 'ABC-123',
  history: [{ status: 'ACTIVE' }]
};

const mockShop: any = {
  id: 'shop-1', name: 'Tienda Central', phone: '123456789'
};

const mockRegistryData: any[] = [
  { _id: '2025-01-01', totalValue: 100 },
  { _id: '2025-01-02', totalValue: 200 }
];

const mockShopReferences: any[] = [
  { referenceCode: 'SHOP-001', name: 'Tienda A' },
  { referenceCode: 'SHOP-002', name: 'Tienda B' }
];

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('AdminHomeComponent', () => {
  let component: AdminHomeComponent;
  let fixture: ComponentFixture<AdminHomeComponent>;
  let authServiceSpy: jasmine.SpyObj<any>;
  let statServiceSpy: jasmine.SpyObj<StatService>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let shopServiceSpy: jasmine.SpyObj<ShopService>;
  let truckServiceSpy: jasmine.SpyObj<TruckService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;
  let registryServiceSpy: jasmine.SpyObj<RegistryService>;
  let routerEvents$: Subject<any>;

  function setupRole(role: 'ADMIN' | 'MANAGER' | 'DRIVER') {
    const info = role === 'ADMIN' ? mockAdminInfo : role === 'MANAGER' ? mockManagerInfo : mockDriverInfo;
    authServiceSpy.isAdmin.and.returnValue(role === 'ADMIN');
    authServiceSpy.isManager.and.returnValue(role === 'MANAGER');
    authServiceSpy.isDriver.and.returnValue(role === 'DRIVER');
    authServiceSpy.getLoginInfo.and.callFake(() => of({ ...info }));
  }

  beforeEach(async () => {
    routerEvents$ = new Subject<any>();

    authServiceSpy = jasmine.createSpyObj('AuthService', ['getLoginInfo', 'isAdmin', 'isManager', 'isDriver']);
    statServiceSpy = jasmine.createSpyObj('StatService', ['getOrdersStatsByRole', 'getShopsStatsByRole', 'getTrucksStatsByRole']);
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['getOrdersByRolePage']);
    shopServiceSpy = jasmine.createSpyObj('ShopService', ['getManagedShopReferences', 'getShopByAssignedTruckId']);
    truckServiceSpy = jasmine.createSpyObj('TruckService', ['getAssignedTruckByDriverId']);
    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['getRecentNotifications']);
    registryServiceSpy = jasmine.createSpyObj('RegistryService', ['loadInternalRegistry']);

    setupRole('ADMIN');
    statServiceSpy.getOrdersStatsByRole.and.callFake(() => of([...mockOrderStats]));
    statServiceSpy.getShopsStatsByRole.and.callFake(() => of([...mockShopStats]));
    statServiceSpy.getTrucksStatsByRole.and.callFake(() => of([...mockTruckStats]));
    orderServiceSpy.getOrdersByRolePage.and.callFake(() =>
      of({ items: [{ ...mockOrder }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 })
    );
    notificationServiceSpy.getRecentNotifications.and.callFake(() => of([{ ...mockNotification }]));
    registryServiceSpy.loadInternalRegistry.and.callFake(() => of([...mockRegistryData]));
    shopServiceSpy.getManagedShopReferences.and.callFake(() => of([...mockShopReferences]));
    shopServiceSpy.getShopByAssignedTruckId.and.callFake(() => of({ ...mockShop }));
    truckServiceSpy.getAssignedTruckByDriverId.and.callFake(() => of({ ...mockTruck }));

    const routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: routerEvents$.asObservable(),
      url: '/admin'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    await TestBed.configureTestingModule({
      imports: [AdminHomeComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: StatService, useValue: statServiceSpy },
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: ShopService, useValue: shopServiceSpy },
        { provide: TruckService, useValue: truckServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: RegistryService, useValue: registryServiceSpy },
        {
          provide: UiService,
          useValue: {
            AVAILABLE_ICONS: [],
            getVisualsByType: jasmine.createSpy('getVisualsByType').and.returnValue({
              color: 'bg-blue-50 text-blue-600 border-blue-200',
              icon: 'pi pi-bell',
              tag: 'info'
            })
          }
        },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => null }, url: [], data: {} },
            root: { children: [] }
          }
        },
        BreadcrumbService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── getLoginInfo ───────────────────────────────────────────────────────────────

  describe('getLoginInfo', () => {
    it('should set loading=true at the start of each call', () => {
      let capturedLoading = false;
      authServiceSpy.getLoginInfo.and.callFake(() => {
        capturedLoading = component.loading;
        return of({ ...mockAdminInfo });
      });
      component.getLoginInfo();
      expect(capturedLoading).toBeTrue();
    });

    it('should set error=true and loading=false on failure', () => {
      authServiceSpy.getLoginInfo.and.callFake(() => throwError(() => new Error('auth-fail')));
      component.getLoginInfo();
      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
    });

    it('should call initData (and hence loadKpis) after success', () => {
      statServiceSpy.getOrdersStatsByRole.calls.reset();
      component.getLoginInfo();
      expect(statServiceSpy.getOrdersStatsByRole).toHaveBeenCalled();
    });
  });

  // ── Admin mode — loadKpis ──────────────────────────────────────────────────────

  describe('admin mode — loadKpis', () => {
    it('should call getOrdersStatsByRole, getShopsStatsByRole and getTrucksStatsByRole', () => {
      expect(statServiceSpy.getOrdersStatsByRole).toHaveBeenCalled();
      expect(statServiceSpy.getShopsStatsByRole).toHaveBeenCalled();
      expect(statServiceSpy.getTrucksStatsByRole).toHaveBeenCalled();
    });

    it('should set globalKpis.totalBudget from shops stats', () => {
      expect(component.globalKpis().totalBudget).toBe(10000);
    });

    it('should set globalKpis.totalShops from shops stats', () => {
      expect(component.globalKpis().totalShops).toBe(4);
    });

    it('should compute globalKpis.activeOrders = orderMade+sent+onDelivery (3+2+1=6)', () => {
      expect(component.globalKpis().activeOrders).toBe(6);
    });

    it('should compute globalKpis.totalOrders = activeOrders+completed (6+5=11)', () => {
      expect(component.globalKpis().totalOrders).toBe(11);
    });

    it('should compute globalKpis.activeTrucks = disponibles+enRuta (3+2=5)', () => {
      expect(component.globalKpis().activeTrucks).toBe(5);
    });

    it('should compute globalKpis.totalTrucks (5+1+0=6)', () => {
      expect(component.globalKpis().totalTrucks).toBe(6);
    });

    it('should set ordersChartData with 4 labels', () => {
      expect(component.ordersChartData().labels.length).toBe(4);
      expect(component.ordersChartData().labels).toContain('Completados');
      expect(component.ordersChartData().labels).toContain('Realizados');
    });

    it('should set ordersChartData dataset with counts in order [completed, onDelivery, sent, orderMade]', () => {
      expect(component.ordersChartData().datasets[0].data).toEqual([5, 1, 2, 3]);
    });

    it('should set loading=false after forkJoin completes', () => {
      expect(component.loading).toBeFalse();
    });

    it('should set error=true and loading=false on forkJoin failure', () => {
      statServiceSpy.getOrdersStatsByRole.and.callFake(() => throwError(() => new Error('kpi-fail')));
      component.getLoginInfo();
      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
    });
  });

  // ── Admin mode — loadSalesChartData ───────────────────────────────────────────

  describe('admin mode — loadSalesChartData', () => {
    it('should call registryService.loadInternalRegistry directly', () => {
      expect(registryServiceSpy.loadInternalRegistry).toHaveBeenCalled();
    });

    it('should NOT call getManagedShopReferences', () => {
      expect(shopServiceSpy.getManagedShopReferences).not.toHaveBeenCalled();
    });

    it('should set salesChartData with one dataset', () => {
      expect(component.salesChartData().datasets.length).toBe(1);
    });

    it('should set salesChartData dataset label to "Presupuesto Global"', () => {
      expect(component.salesChartData().datasets[0].label).toBe('Presupuesto Global');
    });

    it('should set salesChartData data from registry totalValue', () => {
      expect(component.salesChartData().datasets[0].data).toEqual([100, 200]);
    });
  });

  // ── loadRecentOrdersByRole ────────────────────────────────────────────────────

  describe('loadRecentOrdersByRole', () => {
    it('should call getOrdersByRolePage with page 0, size 5 and sort "createdAt,desc"', () => {
      expect(orderServiceSpy.getOrdersByRolePage).toHaveBeenCalledWith(0, 5, 'createdAt,desc');
    });

    it('should populate recentOrders signal from page items', () => {
      expect(component.recentOrders().length).toBe(1);
      expect(component.recentOrders()[0].referenceCode).toBe('ORD-001');
    });
  });

  // ── loadRecentNotifications ───────────────────────────────────────────────────

  describe('loadRecentNotifications', () => {
    it('should call getRecentNotifications with empty type and count 3', () => {
      expect(notificationServiceSpy.getRecentNotifications).toHaveBeenCalledWith('', 3);
    });

    it('should populate recentNotifications signal', () => {
      expect(component.recentNotifications().length).toBe(1);
      expect(component.recentNotifications()[0].subject).toBe('Aviso');
    });

    it('should set loadingNotifications=false after successful load', () => {
      expect(component.loadingNotifications).toBeFalse();
    });

    it('should set loadingNotifications=false on error', () => {
      notificationServiceSpy.getRecentNotifications.and.callFake(() => throwError(() => new Error('notif-fail')));
      (component as any)['loadRecentNotifications']();
      expect(component.loadingNotifications).toBeFalse();
    });
  });

  // ── Manager mode — loadSalesChartData ─────────────────────────────────────────

  describe('manager mode — loadSalesChartData', () => {
    beforeEach(() => {
      setupRole('MANAGER');
      registryServiceSpy.loadInternalRegistry.calls.reset();
      shopServiceSpy.getManagedShopReferences.calls.reset();
      component.getLoginInfo();
    });

    it('should call getManagedShopReferences', () => {
      expect(shopServiceSpy.getManagedShopReferences).toHaveBeenCalled();
    });

    it('should call loadInternalRegistry once per shop (2 shops → 2 calls)', () => {
      expect(registryServiceSpy.loadInternalRegistry.calls.count()).toBe(2);
    });

    it('should set salesChartData with one dataset per shop', () => {
      expect(component.salesChartData().datasets.length).toBe(2);
    });

    it('should use shop names as dataset labels', () => {
      expect(component.salesChartData().datasets[0].label).toBe('Tienda A');
      expect(component.salesChartData().datasets[1].label).toBe('Tienda B');
    });

    it('should NOT call loadInternalRegistry when shops list is empty', () => {
      shopServiceSpy.getManagedShopReferences.and.callFake(() => of([]));
      registryServiceSpy.loadInternalRegistry.calls.reset();
      component.getLoginInfo();
      expect(registryServiceSpy.loadInternalRegistry).not.toHaveBeenCalled();
    });
  });

  // ── Driver mode — no truck ─────────────────────────────────────────────────────

  describe('driver mode — no truck', () => {
    beforeEach(() => {
      setupRole('DRIVER');
      truckServiceSpy.getAssignedTruckByDriverId.and.callFake(() => of(null as any));
      notificationServiceSpy.getRecentNotifications.calls.reset();
      component.getLoginInfo();
    });

    it('should call getAssignedTruckByDriverId with the driver id', () => {
      expect(truckServiceSpy.getAssignedTruckByDriverId).toHaveBeenCalledWith('user-3');
    });

    it('should set driverTruck to null', () => {
      expect(component.driverTruck()).toBeNull();
    });

    it('should set loading=false', () => {
      expect(component.loading).toBeFalse();
    });

    it('should NOT call getRecentNotifications (no truck scenario)', () => {
      expect(notificationServiceSpy.getRecentNotifications).not.toHaveBeenCalled();
    });
  });

  // ── Driver mode — truck but no shop ───────────────────────────────────────────

  describe('driver mode — truck but no shop', () => {
    beforeEach(() => {
      setupRole('DRIVER');
      truckServiceSpy.getAssignedTruckByDriverId.and.callFake(() => of({ ...mockTruck }));
      shopServiceSpy.getShopByAssignedTruckId.and.callFake(() => of(null as any));
      notificationServiceSpy.getRecentNotifications.calls.reset();
      component.getLoginInfo();
    });

    it('should set driverTruck from service', () => {
      expect(component.driverTruck()).not.toBeNull();
      expect(component.driverTruck().id).toBe('truck-1');
    });

    it('should set driverShop to null', () => {
      expect(component.driverShop()).toBeNull();
    });

    it('should call getRecentNotifications (truck present, no shop)', () => {
      expect(notificationServiceSpy.getRecentNotifications).toHaveBeenCalled();
    });

    it('should set loading=false', () => {
      expect(component.loading).toBeFalse();
    });
  });

  // ── Driver mode — truck and shop ──────────────────────────────────────────────

  describe('driver mode — truck and shop', () => {
    beforeEach(() => {
      setupRole('DRIVER');
      truckServiceSpy.getAssignedTruckByDriverId.and.callFake(() => of({ ...mockTruck }));
      shopServiceSpy.getShopByAssignedTruckId.and.callFake(() => of({ ...mockShop }));
      statServiceSpy.getOrdersStatsByRole.calls.reset();
      registryServiceSpy.loadInternalRegistry.calls.reset();
      component.getLoginInfo();
    });

    it('should set driverTruck', () => {
      expect(component.driverTruck()).not.toBeNull();
      expect(component.driverTruck().id).toBe('truck-1');
    });

    it('should set driverShop', () => {
      expect(component.driverShop()).not.toBeNull();
      expect(component.driverShop().name).toBe('Tienda Central');
    });

    it('should call getOrdersStatsByRole for driver KPIs', () => {
      expect(statServiceSpy.getOrdersStatsByRole).toHaveBeenCalled();
    });

    it('should set driverKpis.orderMade from order stats', () => {
      expect(component.driverKpis().orderMade).toBe(3);
    });

    it('should set driverKpis.sent from order stats', () => {
      expect(component.driverKpis().sent).toBe(2);
    });

    it('should set driverKpis.onDelivery from order stats', () => {
      expect(component.driverKpis().onDelivery).toBe(1);
    });

    it('should call loadInternalRegistry twice for driver history (completed + cancelled)', () => {
      expect(registryServiceSpy.loadInternalRegistry.calls.count()).toBe(2);
    });

    it('should set driverHistoryChartData with 2 datasets', () => {
      expect(component.driverHistoryChartData().datasets.length).toBe(2);
      expect(component.driverHistoryChartData().datasets[0].label).toBe('Completados');
      expect(component.driverHistoryChartData().datasets[1].label).toBe('Cancelados');
    });

    it('should set loading=false', () => {
      expect(component.loading).toBeFalse();
    });

    it('should set error=true and loading=false when getOrdersStatsByRole fails', () => {
      statServiceSpy.getOrdersStatsByRole.and.callFake(() => throwError(() => new Error('stats-fail')));
      component.getLoginInfo();
      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
    });
  });

  // ── Driver mode — truck service error ─────────────────────────────────────────

  describe('driver mode — loadContactInformation error', () => {
    beforeEach(() => {
      setupRole('DRIVER');
      truckServiceSpy.getAssignedTruckByDriverId.and.callFake(() => throwError(() => new Error('truck-error')));
      component.getLoginInfo();
    });

    it('should set driverTruck to null on truckService error', () => {
      expect(component.driverTruck()).toBeNull();
    });

    it('should set loading=false on truckService error', () => {
      expect(component.loading).toBeFalse();
    });
  });

  // ── initChartOptions ──────────────────────────────────────────────────────────

  describe('initChartOptions', () => {
    it('should set salesChartOptions with maintainAspectRatio=false', () => {
      expect(component.salesChartOptions().maintainAspectRatio).toBeFalse();
    });

    it('should set ordersChartOptions with cutout "65%"', () => {
      expect(component.ordersChartOptions().cutout).toBe('65%');
    });

    it('should set driverHistoryChartOptions', () => {
      expect(component.driverHistoryChartOptions()).toBeDefined();
      expect(component.driverHistoryChartOptions().maintainAspectRatio).toBeFalse();
    });
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should show loading-screen while loading', () => {
      component.loading = true;
      component.error = false;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });

    it('should show loading-screen on error', () => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });

    it('should show "Panel de Control" heading for admin', () => {
      expect(fixture.nativeElement.textContent).toContain('Panel de Control');
    });

    it('should render breadcrumb-reload component', () => {
      expect(fixture.nativeElement.querySelector('app-breadcrumb-reload')).toBeTruthy();
    });

    it('should show "Hoja de Ruta" heading for driver', () => {
      setupRole('DRIVER');
      truckServiceSpy.getAssignedTruckByDriverId.and.callFake(() => of(null as any));
      component.getLoginInfo();
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Hoja de Ruta');
    });

    it('should show "Vehículo No Asignado" when driver has no truck', () => {
      setupRole('DRIVER');
      truckServiceSpy.getAssignedTruckByDriverId.and.callFake(() => of(null as any));
      component.getLoginInfo();
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Vehículo No Asignado');
    });
  });
});
