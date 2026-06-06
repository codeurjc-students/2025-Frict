import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';

import {OrderDetailsComponent} from './order-details.component';
import {OrderService} from '../../../services/order.service';
import {ShopService} from '../../../services/shop.service';
import {TruckService} from '../../../services/truck.service';
import {LocationService} from '../../../services/location.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {AuthService} from '../../../services/auth.service';
import {Order} from '../../../models/order.model';
import {Shop} from '../../../models/shop.model';
import {Truck} from '../../../models/truck.model';

// ── Helpers ───────────────────────────────────────────────────────────────────

const STUB_ORDER: Order = {
  id: 'order-123',
  referenceCode: 'REF-001',
  history: [
    { id: 'h1', icon: '', status: 'Pedido Realizado', updates: [{ date: '01/01/2025', description: 'Pedido confirmado' }] }
  ],
  user: {} as any,
  orderItems: [
    { id: 'i1', orderId: 'order-123', product: null as any, productName: 'Laptop', productImageUrl: '/laptop.jpg', productPrice: 100, userId: 'u1', quantity: 2, itemsCost: 200 }
  ],
  assignedShopId: null,
  assignedTruckId: null,
  estimatedCompletionTime: 3,
  totalItems: 2,
  subtotalCost: 200,
  totalDiscount: 10,
  shippingCost: 5,
  totalCost: 190,
  totalCapacity: 1,
  cardNumberEnding: '1234',
  sendingAddress: 'Calle Mayor, 1 2A 28001 Madrid (España)',
  sendingAddressLat: 40.4168,
  sendingAddressLng: -3.7038,
  createdAt: '2025-01-01T12:00:00'
};

const STUB_SHOP: Shop = {
  id: 'shop-1', referenceCode: 'SHP-001', name: 'Tienda Test',
  address: { id: 'a1', alias: 'A', street: 'Calle', number: '1', floor: '', postalCode: '28001', city: 'Madrid', country: 'España', latitude: 40.4168, longitude: -3.7038 },
  assignedBudget: 0, maxCapacity: 0, occupiedCapacity: 0,
  imageInfo: { id: '', imageUrl: '', s3Key: '', fileName: '' },
  totalAvailableProducts: 0, totalAssignedTrucks: 0
};

const STUB_TRUCK: Truck = {
  id: 'truck-1', referenceCode: 'TRK-001', plateNumber: 'AB-1234',
  history: [], shopId: 'shop-1',
  address: { id: 'a2', alias: 'B', street: 'Calle', number: '2', floor: '', postalCode: '28001', city: 'Madrid', country: 'España', latitude: 40.42, longitude: -3.71 },
  ordersToDeliver: 1, maxCapacity: 10, currentCapacity: 0
};

const STUB_ORDER_IN_DELIVERY: Order = {
  ...STUB_ORDER,
  history: [{ id: 'h1', icon: '', status: 'En Reparto', updates: [] }],
  assignedShopId: 'shop-1',
  assignedTruckId: 'truck-1'
};

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('OrderDetailsComponent', () => {
  let component: OrderDetailsComponent;
  let fixture: ComponentFixture<OrderDetailsComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let shopServiceSpy: jasmine.SpyObj<ShopService>;
  let truckServiceSpy: jasmine.SpyObj<TruckService>;
  let locationServiceSpy: jasmine.SpyObj<LocationService>;
  let breadcrumbSpy: jasmine.SpyObj<BreadcrumbService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', [
      'getOrderById', 'cancelOrder', 'getOrderQrTokenById', 'downloadOrderInvoice'
    ]);
    orderServiceSpy.getOrderById.and.callFake(() => of({ ...STUB_ORDER, history: [{ ...STUB_ORDER.history[0] }] }));
    orderServiceSpy.cancelOrder.and.callFake(() => of({ ...STUB_ORDER }));
    orderServiceSpy.getOrderQrTokenById.and.callFake(() => of('qr-token-abc'));
    orderServiceSpy.downloadOrderInvoice.and.callFake(() => of(new Blob(['pdf'], { type: 'application/pdf' })));

    shopServiceSpy = jasmine.createSpyObj('ShopService', ['getShopById']);
    shopServiceSpy.getShopById.and.callFake(() => of({ ...STUB_SHOP }));

    truckServiceSpy = jasmine.createSpyObj('TruckService', ['getTruckById']);
    truckServiceSpy.getTruckById.and.callFake(() => of({ ...STUB_TRUCK }));

    locationServiceSpy = jasmine.createSpyObj('LocationService', ['getRoute']);
    locationServiceSpy.getRoute.and.callFake(() => of(null));

    breadcrumbSpy = jasmine.createSpyObj('BreadcrumbService', [
      'insertPenultimateNodesForUrl', 'setBaseBreadcrumbs', 'breadcrumbs'
    ]);
    breadcrumbSpy.breadcrumbs.and.returnValue([]);

    authServiceSpy = jasmine.createSpyObj('AuthService', ['isAdmin', 'isManager', 'isDriver']);
    authServiceSpy.isAdmin.and.returnValue(false);
    authServiceSpy.isManager.and.returnValue(false);
    authServiceSpy.isDriver.and.returnValue(false);

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: new Subject().asObservable(),
      url: '/profile/orders/order-123'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    await TestBed.configureTestingModule({
      imports: [OrderDetailsComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: ShopService, useValue: shopServiceSpy },
        { provide: TruckService, useValue: truckServiceSpy },
        { provide: LocationService, useValue: locationServiceSpy },
        { provide: BreadcrumbService, useValue: breadcrumbSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: (key: string) => key === 'id' ? 'order-123' : null },
              url: [],
              data: {}
            },
            root: { children: [] }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrderDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(); // triggers ngOnInit → loadOrder → order loaded synchronously
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── State after init ──────────────────────────────────────────────────────────

  describe('state after init', () => {
    it('should set orderId from route param', () => {
      expect(component.orderId).toBe('order-123');
    });

    it('should have loading=false after the order loads', () => {
      expect(component.loading).toBeFalse();
    });

    it('should have error=false after successful load', () => {
      expect(component.error).toBeFalse();
    });

    it('should have the order populated', () => {
      expect(component.order.referenceCode).toBe('REF-001');
    });

    it('should initialize displayQrDialog to false', () => {
      expect(component.displayQrDialog).toBeFalse();
    });

    it('should initialize qrToken to empty string', () => {
      expect(component.qrToken).toBe('');
    });

    it('should initialize isDownloadingInvoice to false', () => {
      expect(component.isDownloadingInvoice).toBeFalse();
    });
  });

  // ── loadOrder ─────────────────────────────────────────────────────────────────

  describe('loadOrder', () => {
    it('should call getOrderById with the orderId', () => {
      expect(orderServiceSpy.getOrderById).toHaveBeenCalledWith('order-123');
    });

    it('should set loading=true before the response arrives', () => {
      const pending$ = new Subject<Order>();
      orderServiceSpy.getOrderById.and.callFake(() => pending$.asObservable());
      component.loadOrder();
      expect(component.loading).toBeTrue();
    });

    it('should set loading=false and populate order on success', () => {
      expect(component.loading).toBeFalse();
      expect(component.order).toBeTruthy();
    });

    it('should call breadcrumbService.insertPenultimateNodesForUrl on success', () => {
      expect(breadcrumbSpy.insertPenultimateNodesForUrl).toHaveBeenCalled();
    });

    it('should include the referenceCode in the breadcrumb label', () => {
      const [, items] = breadcrumbSpy.insertPenultimateNodesForUrl.calls.mostRecent().args;
      const labels = (items as any[]).map((i: any) => i.label);
      expect(labels.some((l: string) => l.includes('REF-001'))).toBeTrue();
    });

    it('should set error=true and loading=false on failure', () => {
      orderServiceSpy.getOrderById.and.callFake(() => throwError(() => new Error('404')));
      component.loadOrder();
      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
    });

    it('should reset error to false when reloading after an error', () => {
      component.error = true;
      orderServiceSpy.getOrderById.and.callFake(() => of({ ...STUB_ORDER }));
      component.loadOrder();
      expect(component.error).toBeFalse();
    });

    it('should NOT call getOrderById when orderId is null', () => {
      component.orderId = null;
      orderServiceSpy.getOrderById.calls.reset();
      component.loadOrder();
      expect(orderServiceSpy.getOrderById).not.toHaveBeenCalled();
    });
  });

  // ── loadIcons ─────────────────────────────────────────────────────────────────

  describe('loadIcons', () => {
    it('should assign the correct icon to a matching status', () => {
      expect(component.order.history[0].icon).toBe('pi pi-shopping-cart');
    });

    it('should assign pi pi-box icon for "Enviado" status', () => {
      component.order.history = [{ id: 'h2', icon: '', status: 'Enviado', updates: [] }];
      component.loadIcons();
      expect(component.order.history[0].icon).toBe('pi pi-box');
    });

    it('should assign pi pi-check icon for "Completado" status', () => {
      component.order.history = [{ id: 'h3', icon: '', status: 'Completado', updates: [] }];
      component.loadIcons();
      expect(component.order.history[0].icon).toBe('pi pi-check');
    });

    it('should assign pi pi-ban icon for "Cancelado" status', () => {
      component.order.history = [{ id: 'h4', icon: '', status: 'Cancelado', updates: [] }];
      component.loadIcons();
      expect(component.order.history[0].icon).toBe('pi pi-ban');
    });

    it('should not modify the icon for an unknown status', () => {
      component.order.history = [{ id: 'h5', icon: 'pi pi-star', status: 'Unknown', updates: [] }];
      component.loadIcons();
      expect(component.order.history[0].icon).toBe('pi pi-star');
    });

    it('should return early when order is undefined', () => {
      (component as any)['order'] = undefined;
      expect(() => component.loadIcons()).not.toThrow();
    });
  });

  // ── cancelOrder ───────────────────────────────────────────────────────────────

  describe('cancelOrder', () => {
    it('should call orderService.cancelOrder with the order id', () => {
      (component as any)['cancelOrder']();
      expect(orderServiceSpy.cancelOrder).toHaveBeenCalledWith('order-123');
    });

    it('should update the order with the response', () => {
      const cancelled: Order = { ...STUB_ORDER, referenceCode: 'REF-CANCELLED' };
      orderServiceSpy.cancelOrder.and.callFake(() => of(cancelled));
      (component as any)['cancelOrder']();
      expect(component.order.referenceCode).toBe('REF-CANCELLED');
    });

    it('should call loadIcons after cancelling', () => {
      spyOn(component, 'loadIcons');
      (component as any)['cancelOrder']();
      expect(component.loadIcons).toHaveBeenCalled();
    });

    it('should set loading=false after cancelling', () => {
      (component as any)['cancelOrder']();
      expect(component.loading).toBeFalse();
    });
  });

  // ── openQrDialog ──────────────────────────────────────────────────────────────

  describe('openQrDialog', () => {
    it('should set displayQrDialog to true', () => {
      component.openQrDialog();
      expect(component.displayQrDialog).toBeTrue();
    });

    it('should call getOrderQrTokenById when qrToken is empty', () => {
      component.openQrDialog();
      expect(orderServiceSpy.getOrderQrTokenById).toHaveBeenCalledWith('order-123');
    });

    it('should set qrToken on success', () => {
      component.openQrDialog();
      expect(component.qrToken).toBe('qr-token-abc');
    });

    it('should set qrLoading=true before the token arrives', () => {
      const pending$ = new Subject<string>();
      orderServiceSpy.getOrderQrTokenById.and.callFake(() => pending$.asObservable());
      component.openQrDialog();
      expect(component.qrLoading).toBeTrue();
    });

    it('should set qrLoading=false after token is received', () => {
      component.openQrDialog();
      expect(component.qrLoading).toBeFalse();
    });

    it('should NOT call getOrderQrTokenById again when qrToken is already loaded', () => {
      component.qrToken = 'existing-token';
      orderServiceSpy.getOrderQrTokenById.calls.reset();
      component.openQrDialog();
      expect(orderServiceSpy.getOrderQrTokenById).not.toHaveBeenCalled();
    });

    it('should set qrError=true and qrLoading=false on failure', () => {
      orderServiceSpy.getOrderQrTokenById.and.callFake(() => throwError(() => new Error('qr-fail')));
      component.openQrDialog();
      expect(component.qrError).toBeTrue();
      expect(component.qrLoading).toBeFalse();
    });
  });

  // ── downloadInvoice ───────────────────────────────────────────────────────────

  describe('downloadInvoice', () => {
    beforeEach(() => {
      spyOn(window.URL, 'createObjectURL').and.returnValue('blob:test-url');
      spyOn(window.URL, 'revokeObjectURL');
    });

    it('should call orderService.downloadOrderInvoice with the orderId', () => {
      component.downloadInvoice();
      expect(orderServiceSpy.downloadOrderInvoice).toHaveBeenCalledWith('order-123');
    });

    it('should set isDownloadingInvoice=true before the response', () => {
      const pending$ = new Subject<Blob>();
      orderServiceSpy.downloadOrderInvoice.and.callFake(() => pending$.asObservable());
      component.downloadInvoice();
      expect(component.isDownloadingInvoice).toBeTrue();
    });

    it('should reset isDownloadingInvoice=false after success', () => {
      component.downloadInvoice();
      expect(component.isDownloadingInvoice).toBeFalse();
    });

    it('should reset isDownloadingInvoice=false on error', () => {
      orderServiceSpy.downloadOrderInvoice.and.callFake(() => throwError(() => new Error('pdf-fail')));
      component.downloadInvoice();
      expect(component.isDownloadingInvoice).toBeFalse();
    });

    it('should NOT call the service when orderId is null', () => {
      component.orderId = null;
      component.downloadInvoice();
      expect(orderServiceSpy.downloadOrderInvoice).not.toHaveBeenCalled();
    });

    it('should create an object URL from the blob', () => {
      component.downloadInvoice();
      expect(window.URL.createObjectURL).toHaveBeenCalledWith(jasmine.any(Blob));
    });

    it('should revoke the object URL after download', () => {
      component.downloadInvoice();
      expect(window.URL.revokeObjectURL).toHaveBeenCalledWith('blob:test-url');
    });
  });

  // ── isInDelivery ──────────────────────────────────────────────────────────────

  describe('isInDelivery', () => {
    it('should return true when last history status is "En Reparto"', () => {
      component.order = { ...STUB_ORDER_IN_DELIVERY };
      expect(component.isInDelivery()).toBeTrue();
    });

    it('should return false when last status is "Pedido Realizado"', () => {
      component.order = { ...STUB_ORDER };
      expect(component.isInDelivery()).toBeFalse();
    });

    it('should return false when history is empty', () => {
      component.order = { ...STUB_ORDER, history: [] };
      expect(component.isInDelivery()).toBeFalse();
    });

    it('should return false when order is undefined', () => {
      (component as any).order = undefined;
      expect(component.isInDelivery()).toBeFalse();
    });
  });

  // ── ngOnDestroy ───────────────────────────────────────────────────────────────

  describe('ngOnDestroy', () => {
    it('should call remove() on the map when orderMap is set', () => {
      const mockMap = { remove: jasmine.createSpy('remove') };
      (component as any).orderMap = mockMap;
      component.ngOnDestroy();
      expect(mockMap.remove).toHaveBeenCalled();
    });

    it('should set orderMap to undefined after destroy', () => {
      (component as any).orderMap = { remove: jasmine.createSpy('remove') };
      component.ngOnDestroy();
      expect((component as any).orderMap).toBeUndefined();
    });

    it('should not throw when orderMap is undefined', () => {
      (component as any).orderMap = undefined;
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  // ── loadOrder — En Reparto (forkJoin) path ────────────────────────────────────

  describe('loadOrder when order is in delivery', () => {
    beforeEach(() => {
      orderServiceSpy.getOrderById.and.callFake(() => of({ ...STUB_ORDER_IN_DELIVERY }));
      component.loadOrder();
    });

    it('should call shopService.getShopById with the assigned shop id', () => {
      expect(shopServiceSpy.getShopById).toHaveBeenCalledWith('shop-1');
    });

    it('should call truckService.getTruckById with the assigned truck id', () => {
      expect(truckServiceSpy.getTruckById).toHaveBeenCalledWith('truck-1');
    });

    it('should set component.shop after forkJoin resolves', () => {
      expect(component.shop).not.toBeNull();
      expect(component.shop?.id).toBe('shop-1');
    });

    it('should set component.truck after forkJoin resolves', () => {
      expect(component.truck).not.toBeNull();
      expect(component.truck?.id).toBe('truck-1');
    });
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should show the loading screen when loading=true', () => {
      component.loading = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });

    it('should show the loading screen when error=true', () => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });

    it('should show the main content when not loading and not error', () => {
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeFalsy();
      expect(fixture.nativeElement.textContent).toContain('REF-001');
    });

    it('should render the order referenceCode', () => {
      expect(fixture.nativeElement.textContent).toContain('REF-001');
    });

    it('should render the latest order status', () => {
      expect(fixture.nativeElement.textContent).toContain('Pedido Realizado');
    });

    it('should render the order item product name', () => {
      expect(fixture.nativeElement.textContent).toContain('Laptop');
    });

    it('should render the card number ending', () => {
      expect(fixture.nativeElement.textContent).toContain('1234');
    });

    it('should render the "Volver a mis pedidos" back link', () => {
      expect(fixture.nativeElement.querySelector('a[routerLink="/profile"]')).toBeTruthy();
    });

    it('should show the cancel button for a non-terminal status', () => {
      expect(fixture.nativeElement.textContent).toContain('Cancelar pedido');
    });

    it('should hide the cancel button when status is "Completado"', () => {
      component.order = {
        ...STUB_ORDER,
        history: [{ id: 'h1', icon: 'pi pi-check', status: 'Completado', updates: [] }]
      };
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Cancelar pedido');
    });

    it('should hide the cancel button when status is "Cancelado"', () => {
      component.order = {
        ...STUB_ORDER,
        history: [{ id: 'h1', icon: 'pi pi-ban', status: 'Cancelado', updates: [] }]
      };
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Cancelar pedido');
    });

    it('should render the "Código de Entrega" QR button', () => {
      expect(fixture.nativeElement.textContent).toContain('Código de Entrega');
    });

    it('should render the "Descargar Factura" button', () => {
      expect(fixture.nativeElement.textContent).toContain('Descargar Factura');
    });
  });
});
