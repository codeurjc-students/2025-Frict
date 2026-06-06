import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {PLATFORM_ID} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {MessageService} from 'primeng/api';
import {PaginatorState} from 'primeng/paginator';
import {getOrderStatusTagInfo, getOrderStatusColorClass, getOrderStatusBgColorClass} from '../../../utils/tagManager.util';
import {provideHttpClient} from '@angular/common/http';

import {OrdersManagementComponent} from './orders-management.component';
import {OrderService} from '../../../services/order.service';
import {ShopService} from '../../../services/shop.service';
import {TruckService} from '../../../services/truck.service';
import {AuthService} from '../../../services/auth.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {Order} from '../../../models/order.model';
import {Shop} from '../../../models/shop.model';
import {Truck} from '../../../models/truck.model';
import {PageResponse} from '../../../models/pageResponse.model';

// ── Mock data ──────────────────────────────────────────────────────────────────

const mockAddress = {
  id: 'addr-1', alias: 'Casa', street: 'Calle Mayor', number: '1', floor: '2A',
  postalCode: '28001', city: 'Madrid', country: 'España',
  latitude: 40.4168, longitude: -3.7038
};

const mockUser: any = {
  id: 'user-1', name: 'Test User', username: 'testuser',
  roles: ['ROLE_USER'], email: 'test@test.com', phone: '123456789',
  addresses: [], cards: [],
  imageInfo: { id: 'img-1', imageUrl: 'http://example.com/img.jpg', s3Key: 'k1', fileName: 'img.jpg' },
  banned: false, deleted: false, selectedShopId: null,
  ordersCount: 3, favouriteProductsCount: 1, connection: null
};

const mockOrder: Order = {
  id: 'order-1',
  referenceCode: 'ORD-001',
  history: [{ id: 'log-1', icon: 'pi pi-shopping-cart', status: 'Pedido Realizado', updates: [] }],
  user: mockUser,
  orderItems: [],
  assignedShopId: 'shop-1',
  assignedTruckId: 'truck-1',
  estimatedCompletionTime: 60,
  totalItems: 3, subtotalCost: 30, totalDiscount: 0, shippingCost: 5, totalCost: 35, totalCapacity: 1,
  cardNumberEnding: '1234',
  sendingAddress: 'Calle Mayor, 1 2A 28001 Madrid (España)',
  sendingAddressLat: 40.4168,
  sendingAddressLng: -3.7038,
  createdAt: '2025-01-01'
};

const mockShop: Shop = {
  id: 'shop-1', referenceCode: 'SHP-001', name: 'Tienda Test',
  address: mockAddress as any, assignedBudget: 10000, maxCapacity: 0, occupiedCapacity: 0,
  imageInfo: { id: 'si1', imageUrl: 'http://img.jpg', s3Key: 'sk1', fileName: 'f.jpg' },
  totalAvailableProducts: 100, totalAssignedTrucks: 2
};

const mockTruck: Truck = {
  id: 'truck-1', referenceCode: 'TRK-001', plateNumber: 'AB-1234',
  history: [], shopId: 'shop-1', address: mockAddress as any,
  ordersToDeliver: 2, maxCapacity: 10, currentCapacity: 0
};

const mockPage: PageResponse<Order> = {
  items: [{ ...mockOrder }],
  totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 10
};

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('OrdersManagementComponent', () => {
  let component: OrdersManagementComponent;
  let fixture: ComponentFixture<OrdersManagementComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let shopServiceSpy: jasmine.SpyObj<ShopService>;
  let truckServiceSpy: jasmine.SpyObj<TruckService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let routerEvents$: Subject<any>;

  beforeEach(async () => {
    routerEvents$ = new Subject<any>();

    orderServiceSpy = jasmine.createSpyObj('OrderService', [
      'getOrdersByRolePage', 'commentAndOrUpdateOrderStatus', 'setAssignedTruck'
    ]);
    shopServiceSpy = jasmine.createSpyObj('ShopService', ['getShopById']);
    truckServiceSpy = jasmine.createSpyObj('TruckService', ['getTruckById', 'getAllShopTrucks']);
    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: routerEvents$.asObservable(),
      url: '/admin/orders'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    orderServiceSpy.getOrdersByRolePage.and.callFake(
      () => of({ ...mockPage, items: [{ ...mockOrder }] })
    );
    shopServiceSpy.getShopById.and.callFake(() => of({ ...mockShop }));
    truckServiceSpy.getTruckById.and.callFake(() => of({ ...mockTruck }));
    truckServiceSpy.getAllShopTrucks.and.callFake(() => of([{ ...mockTruck }]));

    await TestBed.configureTestingModule({
      imports: [OrdersManagementComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: PLATFORM_ID, useValue: 'server' },
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
        {
          provide: AuthService,
          useValue: {
            isAdmin: jasmine.createSpy().and.returnValue(false),
            isManager: jasmine.createSpy().and.returnValue(false),
            isDriver: jasmine.createSpy().and.returnValue(false),
            isLogged: jasmine.createSpy().and.returnValue(false)
          }
        },
        BreadcrumbService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrdersManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── ngOnInit / loadOrdersPage ─────────────────────────────────────────────────

  describe('ngOnInit / loadOrdersPage', () => {
    it('should call getOrdersByRolePage on init', () => {
      expect(orderServiceSpy.getOrdersByRolePage).toHaveBeenCalledWith(0, 10, 'createdAt,desc');
    });

    it('should populate ordersPage and set loading=false on success', () => {
      expect(component.ordersPage.totalItems).toBe(1);
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    });

    it('should place orders into the correct kanban signal by status', () => {
      const shipped: Order = { ...mockOrder, id: 'o2', history: [{ id: 'l2', icon: '', status: 'Enviado', updates: [] }] };
      const inDelivery: Order = { ...mockOrder, id: 'o3', history: [{ id: 'l3', icon: '', status: 'En Reparto', updates: [] }] };
      const completed: Order = { ...mockOrder, id: 'o4', history: [{ id: 'l4', icon: '', status: 'Completado', updates: [] }] };
      const cancelled: Order = { ...mockOrder, id: 'o5', history: [{ id: 'l5', icon: '', status: 'Cancelado', updates: [] }] };

      orderServiceSpy.getOrdersByRolePage.and.callFake(() =>
        of({ items: [shipped, inDelivery, completed, cancelled], totalItems: 4, currentPage: 0, lastPage: 0, pageSize: 10 })
      );
      component.ngOnInit();

      expect(component.ordersMade().length).toBe(0);
      expect(component.shippedOrders().length).toBe(1);
      expect(component.inDeliveryOrders().length).toBe(1);
      expect(component.completedOrders().length).toBe(1);
      expect(component.cancelledOrders().length).toBe(1);
    });

    it('should set error=true and loading=false on service error', () => {
      orderServiceSpy.getOrdersByRolePage.and.callFake(() => throwError(() => new Error('fail')));
      component.ngOnInit();

      expect(component.loading).toBeFalse();
      expect(component.error).toBeTrue();
    });

    it('should place order with no history into ordersMade', () => {
      const noHistory: Order = { ...mockOrder, id: 'o-nh', history: [] };
      orderServiceSpy.getOrdersByRolePage.and.callFake(() =>
        of({ items: [noHistory], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 10 })
      );
      component.ngOnInit();

      expect(component.ordersMade().length).toBe(1);
    });
  });

  // ── reloadAll ─────────────────────────────────────────────────────────────────

  describe('reloadAll', () => {
    it('should reset dialog state and reload orders', () => {
      component.error = true;
      component.displayOrderDialog = true;
      component.displayStatusDialog = true;
      component.selectedOrder = { ...mockOrder };
      component.pendingDropData = { order: mockOrder, newStatus: 'Enviado', previousContainer: {} as any, container: {} as any, previousIndex: 0, currentIndex: 0 };
      const prevCount = orderServiceSpy.getOrdersByRolePage.calls.count();

      component.reloadAll();

      expect(component.displayOrderDialog).toBeFalse();
      expect(component.displayStatusDialog).toBeFalse();
      expect(component.selectedOrder).toBeNull();
      expect(component.pendingDropData).toBeNull();
      expect(orderServiceSpy.getOrdersByRolePage.calls.count()).toBe(prevCount + 1);
    });
  });

  // ── getCurrentStatus ──────────────────────────────────────────────────────────

  describe('getCurrentStatus', () => {
    it('should return the last history status', () => {
      const order: Order = {
        ...mockOrder,
        history: [
          { id: 'l1', icon: '', status: 'Pedido Realizado', updates: [] },
          { id: 'l2', icon: '', status: 'Enviado', updates: [] }
        ]
      };
      expect(component.getCurrentStatus(order)).toBe('Enviado');
    });

    it('should return "Pedido Realizado" when history is empty', () => {
      expect(component.getCurrentStatus({ ...mockOrder, history: [] })).toBe('Pedido Realizado');
    });

    it('should return "Pedido Realizado" when history is null', () => {
      expect(component.getCurrentStatus({ ...mockOrder, history: null as any })).toBe('Pedido Realizado');
    });
  });

  // ── openOrderDetails ──────────────────────────────────────────────────────────

  describe('openOrderDetails', () => {
    it('should open dialog with shop and truck on forkJoin success', () => {
      component.openOrderDetails({ ...mockOrder });
      fixture.detectChanges();

      expect(component.selectedOrder?.id).toBe('order-1');
      expect(component.selectedShop?.id).toBe('shop-1');
      expect(component.selectedTruck?.id).toBe('truck-1');
      expect(component.displayOrderDialog).toBeTrue();
    });

    it('should add assigned truck to availableTrucks', () => {
      component.openOrderDetails({ ...mockOrder });
      fixture.detectChanges();

      expect(component.availableTrucks.length).toBeGreaterThan(0);
      expect(component.availableTrucks[0].id).toBe('truck-1');
    });

    it('should skip getShopById when no shop assigned', () => {
      component.openOrderDetails({ ...mockOrder, assignedShopId: null });
      fixture.detectChanges();

      expect(shopServiceSpy.getShopById).not.toHaveBeenCalled();
      expect(component.selectedShop).toBeNull();
      expect(component.displayOrderDialog).toBeTrue();
    });

    it('should skip getTruckById when no truck assigned', () => {
      component.openOrderDetails({ ...mockOrder, assignedTruckId: null });
      fixture.detectChanges();

      expect(truckServiceSpy.getTruckById).not.toHaveBeenCalled();
      expect(component.selectedTruck).toBeNull();
    });

    it('should show error message and not open dialog on forkJoin error', () => {
      shopServiceSpy.getShopById.and.callFake(() => throwError(() => new Error('fail')));
      component.openOrderDetails({ ...mockOrder });
      fixture.detectChanges();

      expect(component.displayOrderDialog).toBeFalse();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });

    it('should reset comment, truck list and activeTab before opening', () => {
      component.newComment = 'old';
      component.trucksLoaded = true;
      component.activeTab = '2';
      component.openOrderDetails({ ...mockOrder });

      expect(component.newComment).toBe('');
      expect(component.trucksLoaded).toBeFalse();
      expect(component.activeTab).toBe('0');
    });
  });

  // ── loadAvailableTrucks ───────────────────────────────────────────────────────

  describe('loadAvailableTrucks', () => {
    beforeEach(() => {
      component.selectedOrder = { ...mockOrder };
    });

    it('should warn and abort when no assignedShopId', () => {
      component.selectedOrder = { ...mockOrder, assignedShopId: null };
      component.loadAvailableTrucks();

      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'warn' }));
      expect(truckServiceSpy.getAllShopTrucks).not.toHaveBeenCalled();
    });

    it('should skip load if trucksLoaded and forceReload is false', () => {
      component.trucksLoaded = true;
      component.loadAvailableTrucks();

      expect(truckServiceSpy.getAllShopTrucks).not.toHaveBeenCalled();
    });

    it('should force reload even when trucksLoaded is true', () => {
      component.trucksLoaded = true;
      component.loadAvailableTrucks(true);

      expect(truckServiceSpy.getAllShopTrucks).toHaveBeenCalled();
    });

    it('should populate availableTrucks and set trucksLoaded=true on success', () => {
      component.trucksLoaded = false;
      component.loadAvailableTrucks();

      expect(component.availableTrucks.length).toBe(1);
      expect(component.trucksLoaded).toBeTrue();
      expect(component.loadingTrucks).toBeFalse();
    });

    it('should update selectedTruck with matched truck from list', () => {
      component.selectedTruck = { ...mockTruck };
      truckServiceSpy.getAllShopTrucks.and.callFake(() => of([{ ...mockTruck, referenceCode: 'UPDATED' }]));
      component.loadAvailableTrucks();

      expect(component.selectedTruck?.referenceCode).toBe('UPDATED');
    });

    it('should prepend selectedTruck when not found in loaded list', () => {
      component.selectedTruck = { ...mockTruck, id: 'truck-other' };
      truckServiceSpy.getAllShopTrucks.and.callFake(() => of([{ ...mockTruck }]));
      component.loadAvailableTrucks();

      expect(component.availableTrucks[0].id).toBe('truck-other');
      expect(component.availableTrucks.length).toBe(2);
    });

    it('should show error and reset loadingTrucks on failure', () => {
      truckServiceSpy.getAllShopTrucks.and.callFake(() => throwError(() => new Error('fail')));
      component.loadAvailableTrucks();

      expect(component.loadingTrucks).toBeFalse();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── confirmTruckAssignment ────────────────────────────────────────────────────

  describe('confirmTruckAssignment', () => {
    beforeEach(() => {
      component.selectedOrder = { ...mockOrder };
      component.selectedTruck = { ...mockTruck, id: 'truck-new' };
    });

    it('should do nothing when selectedOrder is null', () => {
      component.selectedOrder = null;
      component.confirmTruckAssignment();
      expect(orderServiceSpy.setAssignedTruck).not.toHaveBeenCalled();
    });

    it('should do nothing when selectedTruck is null', () => {
      component.selectedTruck = null;
      component.confirmTruckAssignment();
      expect(orderServiceSpy.setAssignedTruck).not.toHaveBeenCalled();
    });

    it('should do nothing when selectedTruck is already the assigned truck', () => {
      component.selectedTruck = { ...mockTruck, id: 'truck-1' };
      component.confirmTruckAssignment();
      expect(orderServiceSpy.setAssignedTruck).not.toHaveBeenCalled();
    });

    it('should call setAssignedTruck with state=true and show success', () => {
      const updatedOrder: Order = { ...mockOrder, assignedTruckId: 'truck-new' };
      orderServiceSpy.setAssignedTruck.and.callFake(() => of(updatedOrder));

      component.confirmTruckAssignment();

      expect(orderServiceSpy.setAssignedTruck).toHaveBeenCalledWith('order-1', 'truck-new', true);
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should show error on setAssignedTruck failure', () => {
      orderServiceSpy.setAssignedTruck.and.callFake(() => throwError(() => new Error('fail')));
      component.confirmTruckAssignment();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── unassignTruck ─────────────────────────────────────────────────────────────

  describe('unassignTruck', () => {
    beforeEach(() => {
      component.selectedOrder = { ...mockOrder };
    });

    it('should do nothing when selectedOrder is null', () => {
      component.selectedOrder = null;
      component.unassignTruck();
      expect(orderServiceSpy.setAssignedTruck).not.toHaveBeenCalled();
    });

    it('should do nothing when assignedTruckId is null', () => {
      component.selectedOrder = { ...mockOrder, assignedTruckId: null };
      component.unassignTruck();
      expect(orderServiceSpy.setAssignedTruck).not.toHaveBeenCalled();
    });

    it('should call setAssignedTruck with state=false and show success', () => {
      const updatedOrder: Order = { ...mockOrder, assignedTruckId: null };
      orderServiceSpy.setAssignedTruck.and.callFake(() => of(updatedOrder));
      truckServiceSpy.getAllShopTrucks.and.callFake(() => of([]));

      component.unassignTruck();

      expect(orderServiceSpy.setAssignedTruck).toHaveBeenCalledWith('order-1', 'truck-1', false);
      expect(component.selectedTruck).toBeNull();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should update selectedOrder after successful unassignment', () => {
      const updatedOrder: Order = { ...mockOrder, assignedTruckId: null };
      orderServiceSpy.setAssignedTruck.and.callFake(() => of(updatedOrder));
      truckServiceSpy.getAllShopTrucks.and.callFake(() => of([]));

      component.unassignTruck();

      expect(component.selectedOrder?.assignedTruckId).toBeNull();
    });

    it('should show error on unassign failure', () => {
      orderServiceSpy.setAssignedTruck.and.callFake(() => throwError(() => new Error('fail')));
      component.unassignTruck();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── addComment ────────────────────────────────────────────────────────────────

  describe('addComment', () => {
    beforeEach(() => {
      component.selectedOrder = { ...mockOrder };
      component.newComment = 'Test comment';
    });

    it('should do nothing when comment is blank', () => {
      component.newComment = '   ';
      component.addComment();
      expect(orderServiceSpy.commentAndOrUpdateOrderStatus).not.toHaveBeenCalled();
    });

    it('should do nothing when selectedOrder is null', () => {
      component.selectedOrder = null;
      component.addComment();
      expect(orderServiceSpy.commentAndOrUpdateOrderStatus).not.toHaveBeenCalled();
    });

    it('should call commentAndOrUpdateOrderStatus with trimmed comment', () => {
      orderServiceSpy.commentAndOrUpdateOrderStatus.and.callFake(() => of({ ...mockOrder }));
      component.addComment();

      expect(orderServiceSpy.commentAndOrUpdateOrderStatus).toHaveBeenCalledWith(
        'order-1', 'Pedido Realizado', 'Test comment'
      );
    });

    it('should clear newComment and update selectedOrder on success', () => {
      const updated: Order = { ...mockOrder, totalItems: 99 };
      orderServiceSpy.commentAndOrUpdateOrderStatus.and.callFake(() => of(updated));
      component.addComment();

      expect(component.newComment).toBe('');
      expect(component.selectedOrder?.totalItems).toBe(99);
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });
  });

  // ── drop ──────────────────────────────────────────────────────────────────────

  describe('drop', () => {
    it('should reorder array and not show dialog for same-container drop', () => {
      const data = [{ ...mockOrder }, { ...mockOrder, id: 'order-2' }];
      const container = { data } as any;
      const event: any = {
        previousContainer: container, container,
        previousIndex: 0, currentIndex: 1, item: {}
      };

      component.drop(event, 'Pedido Realizado');

      expect(component.displayStatusDialog).toBeFalse();
      expect(component.pendingDropData).toBeNull();
      // order was reordered
      expect(data[0].id).toBe('order-2');
    });

    it('should transferItem, set pendingDropData, and show dialog for cross-container drop', () => {
      const order1 = { ...mockOrder };
      const prevData = [order1];
      const currData: Order[] = [];
      const event: any = {
        previousContainer: { data: prevData } as any,
        container: { data: currData } as any,
        previousIndex: 0, currentIndex: 0, item: {}
      };

      component.drop(event, 'Enviado');

      expect(component.displayStatusDialog).toBeTrue();
      expect(component.pendingDropData?.newStatus).toBe('Enviado');
      expect(currData.length).toBe(1);
      expect(prevData.length).toBe(0);
    });

    it('should reset pendingStatusComment on drop', () => {
      component.pendingStatusComment = 'old comment';
      const data: Order[] = [{ ...mockOrder }];
      const prev = { data } as any;
      const curr = { data: [] } as any;
      component.drop({ previousContainer: prev, container: curr, previousIndex: 0, currentIndex: 0, item: {} } as any, 'Completado');

      expect(component.pendingStatusComment).toBe('');
    });
  });

  // ── confirmStatusChange ───────────────────────────────────────────────────────

  describe('confirmStatusChange', () => {
    it('should do nothing when pendingDropData is null', () => {
      component.pendingDropData = null;
      component.confirmStatusChange();
      expect(orderServiceSpy.commentAndOrUpdateOrderStatus).not.toHaveBeenCalled();
    });

    it('should call service, hide dialog, clear pendingDropData, and show success', () => {
      const updatedOrder: Order = { ...mockOrder, history: [{ id: 'l2', icon: '', status: 'Enviado', updates: [] }] };
      orderServiceSpy.commentAndOrUpdateOrderStatus.and.callFake(() => of(updatedOrder));

      const currData = [{ ...mockOrder }];
      const prevData: Order[] = [];
      component.pendingDropData = {
        order: currData[0], newStatus: 'Enviado',
        previousContainer: { data: prevData } as any,
        container: { data: currData } as any,
        previousIndex: 0, currentIndex: 0
      };
      component.pendingStatusComment = 'Status comment';

      component.confirmStatusChange();

      expect(orderServiceSpy.commentAndOrUpdateOrderStatus).toHaveBeenCalledWith('order-1', 'Enviado', 'Status comment');
      expect(component.displayStatusDialog).toBeFalse();
      expect(component.pendingDropData).toBeNull();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should revert item transfer and show error on service failure', () => {
      orderServiceSpy.commentAndOrUpdateOrderStatus.and.callFake(() => throwError(() => new Error('fail')));

      const order = { ...mockOrder };
      const currData = [order];
      const prevData: Order[] = [];
      component.pendingDropData = {
        order, newStatus: 'Enviado',
        previousContainer: { data: prevData } as any,
        container: { data: currData } as any,
        previousIndex: 0, currentIndex: 0
      };

      component.confirmStatusChange();

      expect(prevData.length).toBe(1);
      expect(currData.length).toBe(0);
      expect(component.pendingDropData).toBeNull();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── cancelStatusChange ────────────────────────────────────────────────────────

  describe('cancelStatusChange', () => {
    it('should do nothing when pendingDropData is null', () => {
      component.pendingDropData = null;
      component.displayStatusDialog = false;
      component.cancelStatusChange();
      expect(component.displayStatusDialog).toBeFalse();
    });

    it('should revert item, close dialog, and clear pendingDropData', () => {
      const order = { ...mockOrder };
      const currData = [order];
      const prevData: Order[] = [];
      component.pendingDropData = {
        order, newStatus: 'Enviado',
        previousContainer: { data: prevData } as any,
        container: { data: currData } as any,
        previousIndex: 0, currentIndex: 0
      };
      component.displayStatusDialog = true;

      component.cancelStatusChange();

      expect(prevData.length).toBe(1);
      expect(currData.length).toBe(0);
      expect(component.displayStatusDialog).toBeFalse();
      expect(component.pendingDropData).toBeNull();
    });
  });

  // ── syncOrderInListData (via addComment) ──────────────────────────────────────

  describe('syncOrderInListData', () => {
    it('should update ordersPage item, kanban signal, and selectedOrder', () => {
      const updated: Order = { ...mockOrder, totalItems: 77 };
      orderServiceSpy.commentAndOrUpdateOrderStatus.and.callFake(() => of(updated));

      component.selectedOrder = { ...mockOrder };
      component.ordersPage = { items: [{ ...mockOrder }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 10 };
      component.ordersMade.set([{ ...mockOrder }]);

      // confirmStatusChange calls syncOrderInListData which updates ordersPage.items
      component.pendingDropData = {
        order: { ...mockOrder }, newStatus: 'Pedido Realizado',
        previousContainer: { data: [] } as any,
        container: { data: [{ ...mockOrder }] } as any,
        previousIndex: 0, currentIndex: 0
      };
      component.confirmStatusChange();

      expect(component.ordersPage.items[0].totalItems).toBe(77);
      expect(component.selectedOrder?.totalItems).toBe(77);
    });

    it('should move order to correct signal when status changes', () => {
      component.selectedOrder = { ...mockOrder };
      component.ordersMade.set([{ ...mockOrder }]);
      component.newComment = 'Update';

      const updatedShipped: Order = { ...mockOrder, history: [{ id: 'l2', icon: '', status: 'Enviado', updates: [] }] };
      orderServiceSpy.commentAndOrUpdateOrderStatus.and.callFake(() => of(updatedShipped));
      component.addComment();

      expect(component.ordersMade().length).toBe(0);
      expect(component.shippedOrders().length).toBe(1);
    });
  });

  // ── tagManager helpers (getOrderStatusTagInfo, getOrderStatusColorClass, getOrderStatusBgColorClass) ──

  describe('getOrderStatusTagInfo', () => {
    it('should return correct icons for all known statuses', () => {
      expect(getOrderStatusTagInfo('Pedido Realizado').icon).toBe('pi pi-shopping-cart');
      expect(getOrderStatusTagInfo('Enviado').icon).toBe('pi pi-box');
      expect(getOrderStatusTagInfo('En Reparto').icon).toBe('pi pi-truck');
      expect(getOrderStatusTagInfo('Completado').icon).toBe('pi pi-check');
      expect(getOrderStatusTagInfo('Cancelado').icon).toBe('pi pi-times');
    });

    it('should return fallback icon for unknown status', () => {
      expect(getOrderStatusTagInfo('Unknown').icon).toBe('pi pi-info-circle');
    });
  });

  describe('getOrderStatusColorClass', () => {
    it('should return correct CSS text color classes', () => {
      expect(getOrderStatusColorClass('Pedido Realizado')).toBe('text-blue-500');
      expect(getOrderStatusColorClass('Enviado')).toBe('text-purple-500');
      expect(getOrderStatusColorClass('En Reparto')).toBe('text-orange-500');
      expect(getOrderStatusColorClass('Completado')).toBe('text-green-500');
      expect(getOrderStatusColorClass('Cancelado')).toBe('text-red-500');
    });

    it('should return fallback color for unknown status', () => {
      expect(getOrderStatusColorClass('Unknown')).toBe('text-slate-400');
    });
  });

  describe('getOrderStatusBgColorClass', () => {
    it('should return correct bg+text color classes', () => {
      expect(getOrderStatusBgColorClass('Pedido Realizado')).toBe('bg-blue-100 text-blue-700');
      expect(getOrderStatusBgColorClass('Enviado')).toBe('bg-purple-100 text-purple-700');
      expect(getOrderStatusBgColorClass('En Reparto')).toBe('bg-orange-100 text-orange-700');
      expect(getOrderStatusBgColorClass('Completado')).toBe('bg-green-100 text-green-700');
      expect(getOrderStatusBgColorClass('Cancelado')).toBe('bg-red-100 text-red-700');
    });

    it('should return fallback for unknown status', () => {
      expect(getOrderStatusBgColorClass('Unknown')).toBe('bg-slate-100 text-slate-700');
    });
  });

  describe('getItemName', () => {
    it('should return productName when present', () => {
      expect(component.getItemName({ productName: 'Prod A' })).toBe('Prod A');
    });

    it('should return product.name when productName is absent', () => {
      expect(component.getItemName({ product: { name: 'Prod B' } })).toBe('Prod B');
    });

    it('should return fallback with productId when no name available', () => {
      expect(component.getItemName({ productId: 'p-123' })).toBe('Producto ID: p-123');
    });

    it('should return fallback N/A when no identifying info', () => {
      expect(component.getItemName({})).toBe('Producto ID: N/A');
    });
  });

  describe('getLoadPercentage', () => {
    it('should return 0 when maxCapacity is 0', () => {
      expect(component.getLoadPercentage(5, 0)).toBe(0);
    });

    it('should return correct percentage rounded', () => {
      expect(component.getLoadPercentage(1, 3)).toBe(33);
    });

    it('should return 100 when fully loaded', () => {
      expect(component.getLoadPercentage(10, 10)).toBe(100);
    });
  });

  // ── onOrdersPageChange ────────────────────────────────────────────────────────

  describe('onOrdersPageChange', () => {
    it('should update first/rows and reload with new page params', () => {
      const prevCount = orderServiceSpy.getOrdersByRolePage.calls.count();
      const event: PaginatorState = { first: 20, rows: 20, page: 1, pageCount: 5 };
      component.onOrdersPageChange(event);

      expect(component.first).toBe(20);
      expect(component.rows).toBe(20);
      expect(orderServiceSpy.getOrdersByRolePage.calls.count()).toBe(prevCount + 1);
      expect(orderServiceSpy.getOrdersByRolePage).toHaveBeenCalledWith(1, 20, 'createdAt,desc');
    });

    it('should default first=0 and rows=10 when event values are undefined', () => {
      component.first = 50;
      component.rows = 50;
      component.onOrdersPageChange({ first: undefined, rows: undefined });

      expect(component.first).toBe(0);
      expect(component.rows).toBe(10);
    });
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should show loading-screen component while loading', () => {
      component.loading = true;
      component.error = false;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });

    it('should show loading-screen when in error state', () => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });

    it('should render the page title when loaded', () => {
      expect(fixture.nativeElement.textContent).toContain('Gestor de Pedidos');
    });

    it('should render breadcrumb-reload component when loaded', () => {
      expect(fixture.nativeElement.querySelector('app-breadcrumb-reload')).toBeTruthy();
    });
  });
});
