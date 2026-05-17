import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {PLATFORM_ID} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {MessageService} from 'primeng/api';
import {PaginatorState} from 'primeng/paginator';
import {getOrderStatusTagInfo} from '../../../utils/tagManager.util';

import {OrdersDeliveryComponent} from './orders-delivery.component';
import {AuthService} from '../../../services/auth.service';
import {OrderService} from '../../../services/order.service';
import {ShopService} from '../../../services/shop.service';
import {TruckService} from '../../../services/truck.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {Order} from '../../../models/order.model';
import {PageResponse} from '../../../models/pageResponse.model';
import {LoginInfo} from '../../../models/loginInfo.model';

// ── Mock data ──────────────────────────────────────────────────────────────────

const mockAddress: any = {
  id: 'addr-1', alias: 'Casa', street: 'Calle Mayor', number: '1', floor: '2A',
  postalCode: '28001', city: 'Madrid', country: 'España',
  latitude: 40.4168, longitude: -3.7038
};

const mockUser: any = {
  id: 'user-1', name: 'Test User', username: 'testuser', roles: ['ROLE_USER'],
  email: 'test@test.com', phone: '123', addresses: [], cards: [],
  imageInfo: { id: 'img-1', imageUrl: 'http://example.com/img.jpg', s3Key: 'k1', fileName: 'img.jpg' },
  banned: false, deleted: false, selectedShopId: null,
  ordersCount: 1, favouriteProductsCount: 0, connection: null
};

const mockOrder: Order = {
  id: 'order-1', referenceCode: 'ORD-001',
  history: [{ id: 'log-1', icon: 'pi pi-truck', status: 'En Reparto', updates: [] }],
  user: mockUser, orderItems: [],
  assignedShopId: 'shop-1', assignedTruckId: 'truck-1',
  estimatedCompletionTime: 60,
  totalItems: 2, subtotalCost: 20, totalDiscount: 0, shippingCost: 3, totalCost: 23,
  cardNumberEnding: '1234', sendingAddress: mockAddress, createdAt: '2025-01-01'
};

const mockPage: PageResponse<Order> = {
  items: [{ ...mockOrder }],
  totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5
};

const mockTruck: any = {
  id: 'truck-1', referenceCode: 'TRK-001', plateNumber: 'AB-1234',
  history: [], shopId: 'shop-1', address: mockAddress,
  ordersToDeliver: 2, maxCapacity: 10
};

const mockShop: any = {
  id: 'shop-1', referenceCode: 'SHP-001', name: 'Tienda Test',
  address: mockAddress, assignedBudget: 10000,
  imageInfo: { id: 'si1', imageUrl: 'http://img.jpg', s3Key: 'sk1', fileName: 'f.jpg' },
  totalAvailableProducts: 50, totalAssignedTrucks: 1
};

const mockLoginInfo: LoginInfo = {
  isLogged: true, imageUrl: '', id: 'driver-1', name: 'Driver Test',
  username: 'driver', roles: ['ROLE_DRIVER'], selectedShopId: null
};

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('OrdersDeliveryComponent', () => {
  let component: OrdersDeliveryComponent;
  let fixture: ComponentFixture<OrdersDeliveryComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let shopServiceSpy: jasmine.SpyObj<ShopService>;
  let truckServiceSpy: jasmine.SpyObj<TruckService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let routerEvents$: Subject<any>;

  beforeEach(async () => {
    routerEvents$ = new Subject<any>();

    authServiceSpy = jasmine.createSpyObj('AuthService', [
      'getLoginInfo', 'isAdmin', 'isManager', 'isDriver', 'isLogged'
    ]);
    orderServiceSpy = jasmine.createSpyObj('OrderService', [
      'getOrdersByRolePage', 'commentAndOrUpdateOrderStatus', 'checkOrderQrTokenById', 'unassignAsFinished'
    ]);
    shopServiceSpy = jasmine.createSpyObj('ShopService', ['getShopByAssignedTruckId']);
    truckServiceSpy = jasmine.createSpyObj('TruckService', ['getAssignedTruckByDriverId']);
    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: routerEvents$.asObservable(),
      url: '/admin/delivery'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    authServiceSpy.getLoginInfo.and.callFake(() => of({ ...mockLoginInfo }));
    authServiceSpy.isAdmin.and.returnValue(false);
    authServiceSpy.isManager.and.returnValue(false);
    authServiceSpy.isDriver.and.returnValue(true);
    authServiceSpy.isLogged.and.returnValue(true);

    truckServiceSpy.getAssignedTruckByDriverId.and.callFake(() => of({ ...mockTruck }));
    shopServiceSpy.getShopByAssignedTruckId.and.callFake(() => of({ ...mockShop }));
    orderServiceSpy.getOrdersByRolePage.and.callFake(
      () => of({ ...mockPage, items: [{ ...mockOrder }] })
    );

    await TestBed.configureTestingModule({
      imports: [OrdersDeliveryComponent],
      providers: [
        provideNoopAnimations(),
        { provide: PLATFORM_ID, useValue: 'server' },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: ShopService, useValue: shopServiceSpy },
        { provide: TruckService, useValue: truckServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
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

    fixture = TestBed.createComponent(OrdersDeliveryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── ngOnInit / loadDeliveryData (chain) ───────────────────────────────────────

  describe('ngOnInit / loadDeliveryData', () => {
    it('should execute the full data chain on init', () => {
      expect(authServiceSpy.getLoginInfo).toHaveBeenCalled();
      expect(truckServiceSpy.getAssignedTruckByDriverId).toHaveBeenCalledWith('driver-1');
      expect(shopServiceSpy.getShopByAssignedTruckId).toHaveBeenCalledWith('truck-1');
      expect(orderServiceSpy.getOrdersByRolePage).toHaveBeenCalled();
    });

    it('should set loading=false and populate signals after full chain', () => {
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
      expect(component.hasTruck).toBeTrue();
      expect(component.myTruck()).toEqual(jasmine.objectContaining({ id: 'truck-1' }));
      expect(component.myShop()).toEqual(jasmine.objectContaining({ id: 'shop-1' }));
    });

    it('should auto-select first order after fetching', () => {
      expect(component.selectedOrderId()).toBe('order-1');
      expect(component.selectedOrder()).toEqual(jasmine.objectContaining({ id: 'order-1' }));
    });

    it('should set error=true when getLoginInfo fails', () => {
      authServiceSpy.getLoginInfo.and.callFake(() => throwError(() => new Error('fail')));
      component.loadDeliveryData();

      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });

    it('should set error=true when driver info has no id', () => {
      authServiceSpy.getLoginInfo.and.callFake(() => of({ ...mockLoginInfo, id: '' } as any));
      component.loadDeliveryData();

      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
    });

    it('should set hasTruck=false when truck is null', () => {
      truckServiceSpy.getAssignedTruckByDriverId.and.callFake(() => of(null as any));
      component.loadDeliveryData();

      expect(component.hasTruck).toBeFalse();
      expect(component.loading).toBeFalse();
    });

    it('should set hasTruck=false (no error) when truck fetch errors', () => {
      truckServiceSpy.getAssignedTruckByDriverId.and.callFake(() => throwError(() => new Error('fail')));
      component.loadDeliveryData();

      expect(component.hasTruck).toBeFalse();
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    });

    it('should set error=true when shop fetch fails', () => {
      shopServiceSpy.getShopByAssignedTruckId.and.callFake(() => throwError(() => new Error('fail')));
      component.loadDeliveryData();

      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });

    it('should set error=true when orders fetch fails', () => {
      orderServiceSpy.getOrdersByRolePage.and.callFake(() => throwError(() => new Error('fail')));
      component.loadDeliveryData();

      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });

    it('should set selectedOrderId=null when page has no items', () => {
      orderServiceSpy.getOrdersByRolePage.and.callFake(() =>
        of({ items: [], totalItems: 0, currentPage: 0, lastPage: 0, pageSize: 5 })
      );
      component.loadDeliveryData();

      expect(component.selectedOrderId()).toBeNull();
    });

    it('should reset activeTab and complete loading on reload', () => {
      component.error = true;
      component.displayCollectDialog = true;
      component.displayCancelDialog = true;
      component.loadDeliveryData();

      // Synchronous of() observables complete immediately, so loading ends up false
      expect(component.activeTab).toBe('0');
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    });

    it('should keep current selectedOrderId when it exists in the new page', () => {
      component.selectedOrderId.set('order-1');
      orderServiceSpy.getOrdersByRolePage.and.callFake(() =>
        of({ items: [{ ...mockOrder, id: 'order-1' }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 })
      );
      component.loadDeliveryData();

      expect(component.selectedOrderId()).toBe('order-1');
    });

    it('should switch selectedOrderId to first item when current id not in new page', () => {
      component.selectedOrderId.set('order-old');
      orderServiceSpy.getOrdersByRolePage.and.callFake(() =>
        of({ items: [{ ...mockOrder, id: 'order-new' }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 })
      );
      component.loadDeliveryData();

      expect(component.selectedOrderId()).toBe('order-new');
    });
  });

  // ── selectedOrder computed ────────────────────────────────────────────────────

  describe('selectedOrder computed', () => {
    it('should return the order matching selectedOrderId', () => {
      component.ordersPage.set({ items: [{ ...mockOrder }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 });
      component.selectedOrderId.set('order-1');
      expect(component.selectedOrder()?.id).toBe('order-1');
    });

    it('should return null when selectedOrderId is null', () => {
      component.selectedOrderId.set(null);
      expect(component.selectedOrder()).toBeNull();
    });

    it('should return null when no order matches selectedOrderId', () => {
      component.ordersPage.set({ items: [{ ...mockOrder }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 });
      component.selectedOrderId.set('non-existent');
      expect(component.selectedOrder()).toBeNull();
    });
  });

  // ── getCurrentStatus ──────────────────────────────────────────────────────────

  describe('getCurrentStatus', () => {
    it('should return the last status from history', () => {
      const order: Order = {
        ...mockOrder,
        history: [
          { id: 'l1', icon: '', status: 'Pedido Realizado', updates: [] },
          { id: 'l2', icon: '', status: 'Enviado', updates: [] }
        ]
      };
      expect(component.getCurrentStatus(order)).toBe('Enviado');
    });

    it('should return "Desconocido" for empty history', () => {
      expect(component.getCurrentStatus({ ...mockOrder, history: [] })).toBe('Desconocido');
    });

    it('should return "Desconocido" for null order', () => {
      expect(component.getCurrentStatus(null)).toBe('Desconocido');
    });

    it('should return "Desconocido" for null history', () => {
      expect(component.getCurrentStatus({ ...mockOrder, history: null as any })).toBe('Desconocido');
    });
  });

  // ── isCollectDisabled getter ──────────────────────────────────────────────────

  describe('isCollectDisabled', () => {
    it('should be true when status is "Pedido Realizado"', () => {
      component.ordersPage.set({ items: [{ ...mockOrder, history: [{ id: 'l1', icon: '', status: 'Pedido Realizado', updates: [] }] }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 });
      component.selectedOrderId.set('order-1');
      expect(component.isCollectDisabled).toBeTrue();
    });

    it('should be false when status is "Enviado"', () => {
      component.ordersPage.set({ items: [{ ...mockOrder }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 });
      component.selectedOrderId.set('order-1');
      expect(component.isCollectDisabled).toBeFalse();
    });

    it('should be false when selectedOrder is null', () => {
      component.selectedOrderId.set(null);
      expect(component.isCollectDisabled).toBeFalse();
    });
  });

  // ── onPageChange ──────────────────────────────────────────────────────────────

  describe('onPageChange', () => {
    it('should update first/rows and reload orders', () => {
      const prevCount = orderServiceSpy.getOrdersByRolePage.calls.count();
      const event: PaginatorState = { first: 10, rows: 10, page: 2, pageCount: 5 };
      component.onPageChange(event);

      expect(component.first).toBe(10);
      expect(component.rows).toBe(10);
      expect(orderServiceSpy.getOrdersByRolePage.calls.count()).toBe(prevCount + 1);
      expect(orderServiceSpy.getOrdersByRolePage).toHaveBeenCalledWith(1, 10, 'createdAt,desc');
    });

    it('should default first=0 and rows=5 when event values are undefined', () => {
      component.first = 99;
      component.rows = 99;
      component.onPageChange({ first: undefined, rows: undefined });

      expect(component.first).toBe(0);
      expect(component.rows).toBe(5);
    });
  });

  // ── onTabChange ───────────────────────────────────────────────────────────────

  describe('onTabChange', () => {
    it('should update activeTab', () => {
      component.onTabChange('1');
      expect(component.activeTab).toBe('1');
    });

    it('should set activeTab to "2" when tab 2 is selected', () => {
      component.onTabChange(2);
      expect(component.activeTab).toBe('2');
    });
  });

  // ── onOrderSelectionChange ────────────────────────────────────────────────────

  describe('onOrderSelectionChange', () => {
    it('should update selectedOrderId', () => {
      component.onOrderSelectionChange('order-2');
      expect(component.selectedOrderId()).toBe('order-2');
    });
  });

  // ── openCollectDialog ─────────────────────────────────────────────────────────

  describe('openCollectDialog', () => {
    it('should set default comment and open dialog', () => {
      component.collectComment = '';
      component.openCollectDialog();

      expect(component.collectComment).toBe('Pedido recogido para su entrega.');
      expect(component.displayCollectDialog).toBeTrue();
    });
  });

  // ── collectOrder ──────────────────────────────────────────────────────────────

  describe('collectOrder', () => {
    beforeEach(() => {
      component.ordersPage.set({ items: [{ ...mockOrder }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 });
      component.selectedOrderId.set('order-1');
      component.collectComment = 'Recogido';
      component.displayCollectDialog = true;
    });

    it('should do nothing when no selectedOrder', () => {
      component.selectedOrderId.set(null);
      component.collectOrder();
      expect(orderServiceSpy.commentAndOrUpdateOrderStatus).not.toHaveBeenCalled();
    });

    it('should call service with "En Reparto" and close dialog on success', () => {
      orderServiceSpy.commentAndOrUpdateOrderStatus.and.callFake(() => of({ ...mockOrder }));
      component.collectOrder();

      expect(orderServiceSpy.commentAndOrUpdateOrderStatus).toHaveBeenCalledWith('order-1', 'En Reparto', 'Recogido');
      expect(component.displayCollectDialog).toBeFalse();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should use default comment when collectComment is blank', () => {
      component.collectComment = '   ';
      orderServiceSpy.commentAndOrUpdateOrderStatus.and.callFake(() => of({ ...mockOrder }));
      component.collectOrder();

      expect(orderServiceSpy.commentAndOrUpdateOrderStatus).toHaveBeenCalledWith(
        'order-1', 'En Reparto', 'Pedido recogido para su entrega.'
      );
    });

    it('should show error on service failure', () => {
      orderServiceSpy.commentAndOrUpdateOrderStatus.and.callFake(() => throwError(() => new Error('fail')));
      component.collectOrder();

      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── openCancelDialog ──────────────────────────────────────────────────────────

  describe('openCancelDialog', () => {
    it('should reset cancelComment and open dialog', () => {
      component.cancelComment = 'old reason';
      component.openCancelDialog();

      expect(component.cancelComment).toBe('');
      expect(component.displayCancelDialog).toBeTrue();
    });
  });

  // ── cancelOrder ───────────────────────────────────────────────────────────────

  describe('cancelOrder', () => {
    beforeEach(() => {
      component.ordersPage.set({ items: [{ ...mockOrder }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 });
      component.selectedOrderId.set('order-1');
      component.cancelComment = 'Cliente ausente';
      component.displayCancelDialog = true;
    });

    it('should do nothing when no selectedOrder', () => {
      component.selectedOrderId.set(null);
      component.cancelOrder();
      expect(orderServiceSpy.commentAndOrUpdateOrderStatus).not.toHaveBeenCalled();
    });

    it('should call service with "Cancelado" and close dialog on success', () => {
      orderServiceSpy.commentAndOrUpdateOrderStatus.and.callFake(() => of({ ...mockOrder }));
      component.cancelOrder();

      expect(orderServiceSpy.commentAndOrUpdateOrderStatus).toHaveBeenCalledWith('order-1', 'Cancelado', 'Cliente ausente');
      expect(component.displayCancelDialog).toBeFalse();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should show error on service failure', () => {
      orderServiceSpy.commentAndOrUpdateOrderStatus.and.callFake(() => throwError(() => new Error('fail')));
      component.cancelOrder();

      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── sendComment ───────────────────────────────────────────────────────────────

  describe('sendComment', () => {
    beforeEach(() => {
      component.ordersPage.set({ items: [{ ...mockOrder }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 });
      component.selectedOrderId.set('order-1');
      component.newComment = 'Observación de prueba';
    });

    it('should do nothing when comment is blank', () => {
      component.newComment = '   ';
      component.sendComment();
      expect(orderServiceSpy.commentAndOrUpdateOrderStatus).not.toHaveBeenCalled();
    });

    it('should do nothing when selectedOrder is null', () => {
      component.selectedOrderId.set(null);
      component.sendComment();
      expect(orderServiceSpy.commentAndOrUpdateOrderStatus).not.toHaveBeenCalled();
    });

    it('should call service with current status and clear comment on success', () => {
      orderServiceSpy.commentAndOrUpdateOrderStatus.and.callFake(() => of({ ...mockOrder }));
      component.sendComment();

      expect(orderServiceSpy.commentAndOrUpdateOrderStatus).toHaveBeenCalledWith(
        'order-1', 'En Reparto', 'Observación de prueba'
      );
      expect(component.newComment).toBe('');
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should show error on service failure', () => {
      orderServiceSpy.commentAndOrUpdateOrderStatus.and.callFake(() => throwError(() => new Error('fail')));
      component.sendComment();

      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── unassignOrder ─────────────────────────────────────────────────────────────

  describe('unassignOrder', () => {
    beforeEach(() => {
      component.ordersPage.set({ items: [{ ...mockOrder }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 });
      component.selectedOrderId.set('order-1');
    });

    it('should do nothing when no selectedOrder', () => {
      component.selectedOrderId.set(null);
      component.unassignOrder();
      expect(orderServiceSpy.unassignAsFinished).not.toHaveBeenCalled();
    });

    it('should call unassignAsFinished and show success', () => {
      orderServiceSpy.unassignAsFinished.and.callFake(() => of({ ...mockOrder }));
      component.unassignOrder();

      expect(orderServiceSpy.unassignAsFinished).toHaveBeenCalledWith('order-1');
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should reload orders after successful unassign', () => {
      orderServiceSpy.unassignAsFinished.and.callFake(() => of({ ...mockOrder }));
      const prevCount = orderServiceSpy.getOrdersByRolePage.calls.count();
      component.unassignOrder();

      expect(orderServiceSpy.getOrdersByRolePage.calls.count()).toBeGreaterThan(prevCount);
    });

    it('should show error on unassign failure', () => {
      orderServiceSpy.unassignAsFinished.and.callFake(() => throwError(() => new Error('fail')));
      component.unassignOrder();

      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── openScanner / closeScanner ────────────────────────────────────────────────

  describe('openScanner', () => {
    it('should set displayQrScanner=true and scanning=true synchronously', () => {
      // Install fake clock so the inner setTimeout never fires Html5QrcodeScanner
      jasmine.clock().install();
      try {
        component.openScanner();
        expect(component.displayQrScanner).toBeTrue();
        expect(component.scanning).toBeTrue();
      } finally {
        jasmine.clock().uninstall();
      }
    });
  });

  describe('closeScanner', () => {
    it('should reset displayQrScanner and scanning flags', () => {
      component.displayQrScanner = true;
      component.scanning = true;
      component.closeScanner();

      expect(component.displayQrScanner).toBeFalse();
      expect(component.scanning).toBeFalse();
    });

    it('should clear and nullify html5QrcodeScanner if set', () => {
      const mockScanner = jasmine.createSpyObj('Html5QrcodeScanner', ['clear']);
      mockScanner.clear.and.returnValue(Promise.resolve());
      (component as any).html5QrcodeScanner = mockScanner;

      component.closeScanner();

      expect(mockScanner.clear).toHaveBeenCalled();
      expect((component as any).html5QrcodeScanner).toBeNull();
    });
  });

  // ── onScanSuccess ─────────────────────────────────────────────────────────────

  describe('onScanSuccess', () => {
    it('should clear scanner, set it to null, and trigger onQrSuccess', () => {
      const mockScanner = jasmine.createSpyObj('Html5QrcodeScanner', ['clear']);
      mockScanner.clear.and.returnValue(Promise.resolve());
      (component as any).html5QrcodeScanner = mockScanner;

      spyOn(component, 'onQrSuccess');
      component.onScanSuccess('decoded-token');

      expect(mockScanner.clear).toHaveBeenCalled();
      expect((component as any).html5QrcodeScanner).toBeNull();
      expect(component.onQrSuccess).toHaveBeenCalledWith('decoded-token');
    });
  });

  // ── onQrSuccess ───────────────────────────────────────────────────────────────

  describe('onQrSuccess', () => {
    beforeEach(() => {
      component.ordersPage.set({ items: [{ ...mockOrder }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 });
      component.selectedOrderId.set('order-1');
      component.displayQrScanner = true;
      component.scanning = true;
    });

    it('should do nothing when no selectedOrder', () => {
      component.selectedOrderId.set(null);
      component.onQrSuccess('some-token');
      expect(orderServiceSpy.checkOrderQrTokenById).not.toHaveBeenCalled();
    });

    it('should set scanning=false before checking token', () => {
      orderServiceSpy.checkOrderQrTokenById.and.callFake(() => of(true));
      component.onQrSuccess('valid-token');
      expect(component.scanning).toBeFalse();
    });

    it('should close scanner and show success for valid token', () => {
      orderServiceSpy.checkOrderQrTokenById.and.callFake(() => of(true));
      component.onQrSuccess('valid-token');

      expect(orderServiceSpy.checkOrderQrTokenById).toHaveBeenCalledWith('order-1', 'valid-token');
      expect(component.displayQrScanner).toBeFalse();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should reload orders after valid delivery', () => {
      orderServiceSpy.checkOrderQrTokenById.and.callFake(() => of(true));
      const prevCount = orderServiceSpy.getOrdersByRolePage.calls.count();
      component.onQrSuccess('valid-token');

      expect(orderServiceSpy.getOrdersByRolePage.calls.count()).toBeGreaterThan(prevCount);
    });

    it('should show error for invalid token without closing scanner', () => {
      orderServiceSpy.checkOrderQrTokenById.and.callFake(() => of(false));
      component.onQrSuccess('bad-token');

      expect(component.displayQrScanner).toBeTrue();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });

    it('should show error when service call fails', () => {
      orderServiceSpy.checkOrderQrTokenById.and.callFake(() => throwError(() => new Error('fail')));
      component.onQrSuccess('any-token');

      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── ngOnDestroy ───────────────────────────────────────────────────────────────

  describe('ngOnDestroy', () => {
    it('should clear html5QrcodeScanner on destroy', () => {
      const mockScanner = jasmine.createSpyObj('Html5QrcodeScanner', ['clear']);
      mockScanner.clear.and.returnValue(Promise.resolve());
      (component as any).html5QrcodeScanner = mockScanner;

      component.ngOnDestroy();

      expect(mockScanner.clear).toHaveBeenCalled();
    });

    it('should not throw if html5QrcodeScanner is null', () => {
      (component as any).html5QrcodeScanner = null;
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  // ── getOrderStatusTagInfo (tagManager) ───────────────────────────────────────

  describe('getOrderStatusTagInfo', () => {
    it('should return success severity for Completado', () => {
      expect(getOrderStatusTagInfo('Completado').severity).toBe('success');
    });

    it('should return warn severity for En Reparto', () => {
      expect(getOrderStatusTagInfo('En Reparto').severity).toBe('warn');
    });

    it('should return info severity for Enviado', () => {
      expect(getOrderStatusTagInfo('Enviado').severity).toBe('info');
    });

    it('should return info severity for Pedido Realizado', () => {
      expect(getOrderStatusTagInfo('Pedido Realizado').severity).toBe('info');
    });

    it('should return danger severity for Cancelado', () => {
      expect(getOrderStatusTagInfo('Cancelado').severity).toBe('danger');
    });

    it('should return secondary severity for unknown status', () => {
      expect(getOrderStatusTagInfo('Unknown').severity).toBe('secondary');
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

    it('should show loading-screen on error state', () => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });

    it('should render breadcrumb-reload component', () => {
      expect(fixture.nativeElement.querySelector('app-breadcrumb-reload')).toBeTruthy();
    });

    it('should show "Sin vehículo asignado" message when hasTruck is false', () => {
      component.loading = false;
      component.error = false;
      component.hasTruck = false;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Sin vehículo asignado');
    });

    it('should show main content when loaded with truck', () => {
      expect(fixture.nativeElement.textContent).toContain('Mi Ruta de Entrega');
    });
  });
});
