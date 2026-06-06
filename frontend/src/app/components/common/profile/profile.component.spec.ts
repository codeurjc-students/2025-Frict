import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient, HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {signal} from '@angular/core';
import {getOrderStatusTagInfo} from '../../../utils/tagManager.util';

import {ProfileComponent} from './profile.component';
import {UserService} from '../../../services/user.service';
import {OrderService} from '../../../services/order.service';
import {ReviewService} from '../../../services/review.service';
import {ShopService} from '../../../services/shop.service';
import {TruckService} from '../../../services/truck.service';
import {AuthService} from '../../../services/auth.service';
import {ConfirmationService, MessageService} from 'primeng/api';
import {ThemeColor, UiService} from '../../../utils/ui.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {User} from '../../../models/user.model';
import {Address} from '../../../models/address.model';
import {PaymentCard} from '../../../models/paymentCard.model';
import {Order} from '../../../models/order.model';
import {Review} from '../../../models/review.model';
import {PageResponse} from '../../../models/pageResponse.model';

// ── Helpers ───────────────────────────────────────────────────────────────────

const STUB_ADDRESS: Address = {
  id: 'a1', alias: 'Casa', street: 'Calle Mayor', number: '1', floor: '2A',
  postalCode: '28001', city: 'Madrid', country: 'España'
};

const STUB_CARD: PaymentCard = {
  id: 'c1', alias: 'Visa', cardOwnerName: 'Test User',
  number: '1234567890123456', numberEnding: '3456', cvv: '123', dueDate: '12/25'
};

const STUB_USER: User = {
  id: 'u1', name: 'Test User', username: 'testuser', email: 'test@test.com',
  phone: '123456789', roles: ['USER'],
  addresses: [STUB_ADDRESS],
  cards: [STUB_CARD],
  imageInfo: { id: 'i1', imageUrl: '/user.jpg', s3Key: '', fileName: '' },
  banned: false, deleted: false, selectedShopId: null,
  ordersCount: 5, favouriteProductsCount: 3, connection: null
};

const STUB_ORDER: Order = {
  id: 'o1', referenceCode: 'REF-001', totalCost: 100,
  createdAt: '2025-01-01T12:00:00',
  history: [{ id: 'h1', status: 'Pedido realizado', icon: 'pi pi-shopping-cart', updates: [] }],
  user: {} as any, orderItems: [], assignedShopId: null, assignedTruckId: null,
  estimatedCompletionTime: 3, totalItems: 1, subtotalCost: 100, totalDiscount: 0,
  shippingCost: 0, totalCapacity: 1, cardNumberEnding: '1234',
  sendingAddress: 'Calle Mayor, 1 2A 28001 Madrid (España)',
  sendingAddressLat: 40.4168, sendingAddressLng: -3.7038
};

const STUB_REVIEW: Review = {
  id: 'r1', productId: 'p1', creatorId: 'u1',
  creatorUsername: 'testuser', creatorName: 'Test User',
  creatorImage: '/user.jpg', creatorConnection: null,
  productName: 'Laptop', text: 'Great product',
  rating: 5, createdAt: '2025-01-01', recommended: true
};

function makePage<T>(items: T[], total = items.length): PageResponse<T> {
  return { items, totalItems: total, currentPage: 0, lastPage: 0, pageSize: 5 };
}

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let orderServiceSpy: any;
  let reviewServiceSpy: jasmine.SpyObj<ReviewService>;
  let shopServiceSpy: jasmine.SpyObj<ShopService>;
  let truckServiceSpy: jasmine.SpyObj<TruckService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let confirmationServiceSpy: jasmine.SpyObj<ConfirmationService>;
  let breadcrumbSpy: jasmine.SpyObj<BreadcrumbService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj('UserService', [
      'getLoggedUserInfo', 'uploadUserImage', 'submitUserData', 'submitAddress',
      'submitPaymentCard', 'deleteAddress', 'deletePaymentCard', 'anonLoggedUser',
      'setSelectedShopId', 'checkBackendConnection'
    ]);
    userServiceSpy.getLoggedUserInfo.and.callFake(() =>
      of({ ...STUB_USER, addresses: [...STUB_USER.addresses], cards: [...STUB_USER.cards] })
    );
    userServiceSpy.uploadUserImage.and.callFake(() =>
      of({ ...STUB_USER, imageInfo: { id: 'i2', imageUrl: '/new.jpg', s3Key: '', fileName: '' } })
    );
    userServiceSpy.submitUserData.and.callFake(() => of({ ...STUB_USER }));
    userServiceSpy.submitAddress.and.callFake(() =>
      of({ ...STUB_USER, addresses: [...STUB_USER.addresses] })
    );
    userServiceSpy.submitPaymentCard.and.callFake(() =>
      of({ ...STUB_USER, cards: [...STUB_USER.cards] })
    );
    userServiceSpy.deleteAddress.and.callFake(() => of({ ...STUB_USER, addresses: [] }));
    userServiceSpy.deletePaymentCard.and.callFake(() => of({ ...STUB_USER, cards: [] }));
    userServiceSpy.anonLoggedUser.and.callFake(() => of({ ...STUB_USER }));
    userServiceSpy.setSelectedShopId.and.callFake(() => of(true));
    userServiceSpy.checkBackendConnection.and.callFake(() => of({ status: 'UP' }));

    orderServiceSpy = jasmine.createSpyObj('OrderService', ['getLoggedUserOrders']);
    orderServiceSpy.itemsCount = signal(0);
    orderServiceSpy.getLoggedUserOrders.and.callFake(() => of(makePage([STUB_ORDER])));

    reviewServiceSpy = jasmine.createSpyObj('ReviewService', ['getLoggedUserReviews']);
    reviewServiceSpy.getLoggedUserReviews.and.callFake(() => of(makePage([STUB_REVIEW])));

    shopServiceSpy = jasmine.createSpyObj('ShopService', ['getShopById', 'getAssignedShopsPage', 'getAllShopsList']);
    shopServiceSpy.getShopById.and.callFake(() =>
      of({ id: 's1', name: 'Tienda 1', referenceCode: 'S-001', address: STUB_ADDRESS,
           assignedBudget: 1000, imageInfo: { id: '', imageUrl: '', s3Key: '', fileName: '' },
           totalAvailableProducts: 10, totalAssignedTrucks: 1, maxCapacity: 0, occupiedCapacity: 0 })
    );
    shopServiceSpy.getAssignedShopsPage.and.callFake(() => of(makePage([])));
    shopServiceSpy.getAllShopsList.and.callFake(() => of([]));

    truckServiceSpy = jasmine.createSpyObj('TruckService', ['getAssignedTruckByDriverId']);
    truckServiceSpy.getAssignedTruckByDriverId.and.callFake(() =>
      of({ id: 't1', referenceCode: 'T-001', plateNumber: '1234-ABC',
           history: [], address: STUB_ADDRESS, ordersToDeliver: 5, maxCapacity: 10, currentCapacity: 0 })
    );

    authServiceSpy = jasmine.createSpyObj('AuthService', [
      'isUser', 'isAdmin', 'isManager', 'isDriver', 'selectedShopId', 'setSelectedShopId', 'logout'
    ]);
    authServiceSpy.isUser.and.returnValue(true);
    authServiceSpy.isAdmin.and.returnValue(false);
    authServiceSpy.isManager.and.returnValue(false);
    authServiceSpy.isDriver.and.returnValue(false);
    authServiceSpy.selectedShopId.and.returnValue(null);
    authServiceSpy.setSelectedShopId.and.stub();
    authServiceSpy.logout.and.callFake(() => of({}));

    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);
    confirmationServiceSpy = jasmine.createSpyObj('ConfirmationService', ['confirm']);

    breadcrumbSpy = jasmine.createSpyObj('BreadcrumbService', [
      'setBaseBreadcrumbs', 'insertPenultimateNodesForUrl', 'breadcrumbs'
    ]);
    breadcrumbSpy.breadcrumbs.and.returnValue([]);

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: new Subject().asObservable(),
      url: '/profile'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: UserService, useValue: userServiceSpy },
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: ReviewService, useValue: reviewServiceSpy },
        { provide: ShopService, useValue: shopServiceSpy },
        { provide: TruckService, useValue: truckServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: ConfirmationService, useValue: confirmationServiceSpy },
        { provide: BreadcrumbService, useValue: breadcrumbSpy },
        {
          provide: UiService,
          useValue: {
            THEME_COLORS: [{ name: 'Amarillo', value: 'yellow' }],
            selectedColor: { name: 'Amarillo', value: 'yellow' },
            changeThemeColor: jasmine.createSpy('changeThemeColor')
          }
        },
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

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── State after init ──────────────────────────────────────────────────────────

  describe('state after init (isUser)', () => {
    it('should have loading=false', () => {
      expect(component.loading).toBeFalse();
    });

    it('should have error=false', () => {
      expect(component.error).toBeFalse();
    });

    it('should have user populated', () => {
      expect(component.user.username).toBe('testuser');
    });

    it('should have foundOrders populated', () => {
      expect(component.foundOrders.items.length).toBe(1);
    });

    it('should have foundReviews populated', () => {
      expect(component.foundReviews.items.length).toBe(1);
    });

    it('should initialize visibleDataDialog to false', () => {
      expect(component.visibleDataDialog).toBeFalse();
    });

    it('should initialize visibleAddressDialog to false', () => {
      expect(component.visibleAddressDialog).toBeFalse();
    });

    it('should initialize visibleCardDialog to false', () => {
      expect(component.visibleCardDialog).toBeFalse();
    });

    it('should initialize visibleImageDialog to false', () => {
      expect(component.visibleImageDialog).toBeFalse();
    });
  });

  // ── loadUser ─────────────────────────────────────────────────────────────────

  describe('loadUser', () => {
    it('should call userService.getLoggedUserInfo', () => {
      expect(userServiceSpy.getLoggedUserInfo).toHaveBeenCalled();
    });

    it('should set loading=true before the response arrives', () => {
      const pending$ = new Subject<User>();
      userServiceSpy.getLoggedUserInfo.and.callFake(() => pending$.asObservable());
      (component as any)['loadUser']();
      expect(component.loading).toBeTrue();
    });

    it('should reset error to false when reloading', () => {
      component.error = true;
      (component as any)['loadUser']();
      expect(component.error).toBeFalse();
    });

    it('should set error=true and loading=false on getLoggedUserInfo failure', () => {
      userServiceSpy.getLoggedUserInfo.and.callFake(() => throwError(() => new Error('500')));
      (component as any)['loadUser']();
      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
    });

    it('should call getLoggedUserOrders when isUser', () => {
      orderServiceSpy.getLoggedUserOrders.calls.reset();
      (component as any)['loadUser']();
      expect(orderServiceSpy.getLoggedUserOrders).toHaveBeenCalled();
    });

    it('should call getLoggedUserReviews when isUser', () => {
      reviewServiceSpy.getLoggedUserReviews.calls.reset();
      (component as any)['loadUser']();
      expect(reviewServiceSpy.getLoggedUserReviews).toHaveBeenCalled();
    });

    it('should call getAssignedShopsPage and set loading=false when isManager', () => {
      authServiceSpy.isUser.and.returnValue(false);
      authServiceSpy.isManager.and.returnValue(true);
      shopServiceSpy.getAssignedShopsPage.calls.reset();
      (component as any)['loadUser']();
      expect(shopServiceSpy.getAssignedShopsPage).toHaveBeenCalled();
      expect(component.loading).toBeFalse();
    });

    it('should call getAssignedTruckByDriverId and set loading=false when isDriver', () => {
      authServiceSpy.isUser.and.returnValue(false);
      authServiceSpy.isDriver.and.returnValue(true);
      truckServiceSpy.getAssignedTruckByDriverId.calls.reset();
      (component as any)['loadUser']();
      expect(truckServiceSpy.getAssignedTruckByDriverId).toHaveBeenCalled();
      expect(component.loading).toBeFalse();
    });

    it('should set loading=false when no role branch matches', () => {
      authServiceSpy.isUser.and.returnValue(false);
      (component as any)['loadUser']();
      expect(component.loading).toBeFalse();
    });
  });

  // ── loadSelectedShop ──────────────────────────────────────────────────────────

  describe('loadSelectedShop', () => {
    it('should NOT call getShopById when selectedShopId is null', () => {
      component.user = { ...STUB_USER, selectedShopId: null };
      shopServiceSpy.getShopById.calls.reset();
      component.loadSelectedShop();
      expect(shopServiceSpy.getShopById).not.toHaveBeenCalled();
    });

    it('should call getShopById with the selectedShopId', () => {
      component.user = { ...STUB_USER, selectedShopId: 's1' };
      shopServiceSpy.getShopById.calls.reset();
      component.loadSelectedShop();
      expect(shopServiceSpy.getShopById).toHaveBeenCalledWith('s1');
    });

    it('should set selectedShop and shops on success', () => {
      const shop = { id: 's1', name: 'Test Shop' } as any;
      component.user = { ...STUB_USER, selectedShopId: 's1' };
      shopServiceSpy.getShopById.and.callFake(() => of(shop));
      component.loadSelectedShop();
      expect(component.selectedShop()).toEqual(shop);
      expect(component.shops()).toContain(shop);
    });

    it('should set error=true on getShopById failure', () => {
      component.user = { ...STUB_USER, selectedShopId: 's1' };
      shopServiceSpy.getShopById.and.callFake(() => throwError(() => new Error('404')));
      component.loadSelectedShop();
      expect(component.error).toBeTrue();
    });
  });

  // ── onDropdownOpen ────────────────────────────────────────────────────────────

  describe('onDropdownOpen', () => {
    it('should call getAllShopsList when list is not loaded', () => {
      component.isListLoaded = false;
      shopServiceSpy.getAllShopsList.calls.reset();
      component.onDropdownOpen();
      expect(shopServiceSpy.getAllShopsList).toHaveBeenCalled();
    });

    it('should set isListLoaded=true after fetching', () => {
      component.isListLoaded = false;
      shopServiceSpy.getAllShopsList.and.callFake(() => of([{ id: 's1' } as any]));
      component.onDropdownOpen();
      expect(component.isListLoaded).toBeTrue();
    });

    it('should update shops with the fetched list', () => {
      const allShops = [{ id: 's1', name: 'Shop 1' } as any];
      component.isListLoaded = false;
      shopServiceSpy.getAllShopsList.and.callFake(() => of(allShops));
      component.onDropdownOpen();
      expect(component.shops()).toEqual(allShops);
    });

    it('should NOT call getAllShopsList when already loaded', () => {
      component.isListLoaded = true;
      shopServiceSpy.getAllShopsList.calls.reset();
      component.onDropdownOpen();
      expect(shopServiceSpy.getAllShopsList).not.toHaveBeenCalled();
    });
  });

  // ── onSaveStore ───────────────────────────────────────────────────────────────

  describe('onSaveStore', () => {
    it('should call confirmationService.confirm', () => {
      component.onSaveStore();
      expect(confirmationServiceSpy.confirm).toHaveBeenCalled();
    });

    it('should call userService.setSelectedShopId in the accept callback', () => {
      component.onSaveStore();
      (confirmationServiceSpy.confirm.calls.mostRecent().args[0] as any).accept();
      expect(userServiceSpy.setSelectedShopId).toHaveBeenCalled();
    });

    it('should call authService.setSelectedShopId on success', () => {
      component.onSaveStore();
      (confirmationServiceSpy.confirm.calls.mostRecent().args[0] as any).accept();
      expect(authServiceSpy.setSelectedShopId).toHaveBeenCalled();
    });

    it('should call messageService.add with success severity on accept', () => {
      component.onSaveStore();
      (confirmationServiceSpy.confirm.calls.mostRecent().args[0] as any).accept();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should reset orderService.itemsCount to 0 on success', () => {
      orderServiceSpy.itemsCount.set(5);
      component.onSaveStore();
      (confirmationServiceSpy.confirm.calls.mostRecent().args[0] as any).accept();
      expect(orderServiceSpy.itemsCount()).toBe(0);
    });

    it('should call messageService.add with error severity when setSelectedShopId fails', () => {
      userServiceSpy.setSelectedShopId.and.callFake(() => throwError(() => new Error('500')));
      component.onSaveStore();
      (confirmationServiceSpy.confirm.calls.mostRecent().args[0] as any).accept();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── confirm (account deletion) ────────────────────────────────────────────────

  describe('confirm (account deletion)', () => {
    const fakeEvent = { target: document.createElement('button') } as unknown as Event;

    it('should call confirmationService.confirm', () => {
      component.confirm(fakeEvent);
      expect(confirmationServiceSpy.confirm).toHaveBeenCalled();
    });

    it('should set loading=true inside accept before anonLoggedUser response', () => {
      const pending$ = new Subject<User>();
      userServiceSpy.anonLoggedUser.and.callFake(() => pending$.asObservable());
      component.confirm(fakeEvent);
      (confirmationServiceSpy.confirm.calls.mostRecent().args[0] as any).accept();
      expect(component.loading).toBeTrue();
    });

    it('should call anonLoggedUser in the accept callback', () => {
      component.confirm(fakeEvent);
      (confirmationServiceSpy.confirm.calls.mostRecent().args[0] as any).accept();
      expect(userServiceSpy.anonLoggedUser).toHaveBeenCalled();
    });

    it('should navigate to "/" after successful account deletion', () => {
      component.confirm(fakeEvent);
      (confirmationServiceSpy.confirm.calls.mostRecent().args[0] as any).accept();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should call messageService.add with error and reset loading when anonLoggedUser fails', () => {
      userServiceSpy.anonLoggedUser.and.callFake(() => throwError(() => new Error('500')));
      component.confirm(fakeEvent);
      (confirmationServiceSpy.confirm.calls.mostRecent().args[0] as any).accept();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(component.loading).toBeFalse();
    });
  });

  // ── logoutOnDelete ────────────────────────────────────────────────────────────

  describe('logoutOnDelete', () => {
    it('should call authService.logout', () => {
      (component as any)['logoutOnDelete']();
      expect(authServiceSpy.logout).toHaveBeenCalled();
    });

    it('should navigate to "/" on successful logout', () => {
      (component as any)['logoutOnDelete']();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should set loading=false on successful logout', () => {
      (component as any)['logoutOnDelete']();
      expect(component.loading).toBeFalse();
    });

    it('should set loading=false and error=true when logout fails', () => {
      authServiceSpy.logout.and.callFake(() => throwError(() => new Error('500')));
      (component as any)['logoutOnDelete']();
      expect(component.loading).toBeFalse();
      expect(component.error).toBeTrue();
    });
  });

  // ── Dialog visibility ─────────────────────────────────────────────────────────

  describe('showDataDialog', () => {
    it('should set visibleDataDialog to true', () => {
      component.showDataDialog();
      expect(component.visibleDataDialog).toBeTrue();
    });

    it('should deep-clone user into newUserData', () => {
      component.showDataDialog();
      expect(component.newUserData).toEqual(component.user);
      expect(component.newUserData).not.toBe(component.user);
    });
  });

  describe('showAddressCreationDialog', () => {
    it('should set visibleAddressDialog to true', () => {
      component.showAddressCreationDialog();
      expect(component.visibleAddressDialog).toBeTrue();
    });
  });

  describe('showCardCreationDialog', () => {
    it('should set visibleCardDialog to true', () => {
      component.showCardCreationDialog();
      expect(component.visibleCardDialog).toBeTrue();
    });
  });

  describe('showEditAddressDialog', () => {
    it('should open dialog and clone the found address', () => {
      component.user = { ...STUB_USER, addresses: [STUB_ADDRESS] };
      component.showEditAddressDialog('a1');
      expect(component.visibleAddressDialog).toBeTrue();
      expect(component.newAddress).toEqual(STUB_ADDRESS);
      expect(component.newAddress).not.toBe(STUB_ADDRESS);
    });

    it('should NOT open dialog when address id is not found', () => {
      component.user = { ...STUB_USER, addresses: [STUB_ADDRESS] };
      component.showEditAddressDialog('nonexistent');
      expect(component.visibleAddressDialog).toBeFalse();
    });
  });

  describe('showEditCardDialog', () => {
    it('should open dialog and clone the found card', () => {
      component.user = { ...STUB_USER, cards: [STUB_CARD] };
      component.showEditCardDialog('c1');
      expect(component.visibleCardDialog).toBeTrue();
      expect(component.newCard).toEqual(STUB_CARD);
      expect(component.newCard).not.toBe(STUB_CARD);
    });

    it('should NOT open dialog when card id is not found', () => {
      component.user = { ...STUB_USER, cards: [STUB_CARD] };
      component.showEditCardDialog('nonexistent');
      expect(component.visibleCardDialog).toBeFalse();
    });
  });

  // ── Cancel operations ─────────────────────────────────────────────────────────

  describe('cancelEditData', () => {
    it('should set visibleDataDialog to false', () => {
      component.visibleDataDialog = true;
      (component as any)['cancelEditData']();
      expect(component.visibleDataDialog).toBeFalse();
    });
  });

  describe('cancelNewAddress', () => {
    it('should reset newAddress fields to empty', () => {
      component.newAddress = { ...STUB_ADDRESS };
      (component as any)['cancelNewAddress']();
      expect(component.newAddress.id).toBe('');
      expect(component.newAddress.alias).toBe('');
    });

    it('should set visibleAddressDialog to false', () => {
      component.visibleAddressDialog = true;
      (component as any)['cancelNewAddress']();
      expect(component.visibleAddressDialog).toBeFalse();
    });
  });

  describe('cancelNewCard', () => {
    it('should reset newCard fields to empty', () => {
      component.newCard = { ...STUB_CARD };
      (component as any)['cancelNewCard']();
      expect(component.newCard.id).toBe('');
      expect(component.newCard.alias).toBe('');
    });

    it('should set visibleCardDialog to false', () => {
      component.visibleCardDialog = true;
      (component as any)['cancelNewCard']();
      expect(component.visibleCardDialog).toBeFalse();
    });
  });

  // ── CRUD operations ───────────────────────────────────────────────────────────

  describe('submitAddress', () => {
    it('should call userService.submitAddress with newAddress', () => {
      component.newAddress = { ...STUB_ADDRESS };
      (component as any)['submitAddress']();
      expect(userServiceSpy.submitAddress).toHaveBeenCalledWith(
        jasmine.objectContaining({ id: 'a1', alias: 'Casa' })
      );
    });

    it('should update user.addresses from the response', () => {
      const updated = [{ ...STUB_ADDRESS, alias: 'Trabajo' }];
      userServiceSpy.submitAddress.and.callFake(() => of({ ...STUB_USER, addresses: updated }));
      component.newAddress = { ...STUB_ADDRESS };
      (component as any)['submitAddress']();
      expect(component.user.addresses).toEqual(updated);
    });

    it('should call cancelNewAddress after success', () => {
      spyOn(component as any, 'cancelNewAddress');
      component.newAddress = { ...STUB_ADDRESS };
      (component as any)['submitAddress']();
      expect((component as any)['cancelNewAddress']).toHaveBeenCalled();
    });
  });

  describe('submitCard', () => {
    it('should call userService.submitPaymentCard when dueDate is valid', () => {
      component.newCard = { ...STUB_CARD, dueDate: '12/25' };
      (component as any)['submitCard']();
      expect(userServiceSpy.submitPaymentCard).toHaveBeenCalledWith(
        jasmine.objectContaining({ id: 'c1', dueDate: '12/25' })
      );
    });

    it('should update user.cards on success', () => {
      const updated = [{ ...STUB_CARD, alias: 'Mastercard' }];
      component.newCard = { ...STUB_CARD, dueDate: '12/25' };
      userServiceSpy.submitPaymentCard.and.callFake(() => of({ ...STUB_USER, cards: updated }));
      (component as any)['submitCard']();
      expect(component.user.cards).toEqual(updated);
    });

    it('should NOT call the service and show an error for invalid dueDate', () => {
      component.newCard = { ...STUB_CARD, dueDate: '13/25' };
      (component as any)['submitCard']();
      expect(userServiceSpy.submitPaymentCard).not.toHaveBeenCalled();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  describe('saveEditData', () => {
    it('should call userService.submitUserData with newUserData', () => {
      component.newUserData = { ...STUB_USER };
      (component as any)['saveEditData']();
      expect(userServiceSpy.submitUserData).toHaveBeenCalledWith(component.newUserData);
    });

    it('should update user and call cancelEditData on success', () => {
      const updated = { ...STUB_USER, name: 'Updated Name' };
      userServiceSpy.submitUserData.and.callFake(() => of(updated));
      component.newUserData = { ...STUB_USER };
      spyOn(component as any, 'cancelEditData');
      (component as any)['saveEditData']();
      expect(component.user.name).toBe('Updated Name');
      expect((component as any)['cancelEditData']).toHaveBeenCalled();
    });

    it('should show a 403-specific error message when the username is taken', () => {
      userServiceSpy.submitUserData.and.callFake(() =>
        throwError(() => new HttpErrorResponse({ status: 403 }))
      );
      component.newUserData = { ...STUB_USER };
      (component as any)['saveEditData']();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(
        jasmine.objectContaining({ summary: 'Error de registro' })
      );
    });

    it('should show a generic error message for non-403 errors', () => {
      userServiceSpy.submitUserData.and.callFake(() =>
        throwError(() => new HttpErrorResponse({ status: 500 }))
      );
      component.newUserData = { ...STUB_USER };
      (component as any)['saveEditData']();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(
        jasmine.objectContaining({ severity: 'error', summary: 'Error' })
      );
    });
  });

  describe('deleteAddress', () => {
    it('should call userService.deleteAddress with the address id', () => {
      (component as any)['deleteAddress']('a1');
      expect(userServiceSpy.deleteAddress).toHaveBeenCalledWith('a1');
    });

    it('should update user.addresses from the response', () => {
      userServiceSpy.deleteAddress.and.callFake(() => of({ ...STUB_USER, addresses: [] }));
      (component as any)['deleteAddress']('a1');
      expect(component.user.addresses).toEqual([]);
    });
  });

  describe('deleteCard', () => {
    it('should call userService.deletePaymentCard with the card id', () => {
      (component as any)['deleteCard']('c1');
      expect(userServiceSpy.deletePaymentCard).toHaveBeenCalledWith('c1');
    });

    it('should update user.cards from the response', () => {
      userServiceSpy.deletePaymentCard.and.callFake(() => of({ ...STUB_USER, cards: [] }));
      (component as any)['deleteCard']('c1');
      expect(component.user.cards).toEqual([]);
    });
  });

  // ── processFile / submitUploadImage ───────────────────────────────────────────

  describe('processFile', () => {
    it('should set selectedImage when the file is JPEG', () => {
      const file = new File(['data'], 'photo.jpg', { type: 'image/jpeg' });
      component.processFile(file);
      expect(component.selectedImage).toBe(file);
    });

    it('should NOT set selectedImage and call messageService.add for non-JPEG', () => {
      const file = new File(['data'], 'photo.png', { type: 'image/png' });
      component.processFile(file);
      expect(component.selectedImage).toBeNull();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });

    it('should call messageService.add when file is undefined', () => {
      component.processFile(undefined);
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  describe('submitUploadImage', () => {
    it('should call uploadUserImage when selectedImage is set', () => {
      component.selectedImage = new File(['data'], 'photo.jpg', { type: 'image/jpeg' });
      (component as any)['submitUploadImage']();
      expect(userServiceSpy.uploadUserImage).toHaveBeenCalledWith('u1', component.selectedImage);
    });

    it('should update user.imageInfo on success', () => {
      component.selectedImage = new File(['data'], 'photo.jpg', { type: 'image/jpeg' });
      (component as any)['submitUploadImage']();
      expect(component.user.imageInfo.imageUrl).toBe('/new.jpg');
    });

    it('should call messageService.add with success on upload success', () => {
      component.selectedImage = new File(['data'], 'photo.jpg', { type: 'image/jpeg' });
      (component as any)['submitUploadImage']();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should call messageService.add with error on upload failure', () => {
      component.selectedImage = new File(['data'], 'photo.jpg', { type: 'image/jpeg' });
      userServiceSpy.uploadUserImage.and.callFake(() => throwError(() => new Error('upload-fail')));
      (component as any)['submitUploadImage']();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });

    it('should NOT call uploadUserImage when selectedImage is null', () => {
      component.selectedImage = null;
      userServiceSpy.uploadUserImage.calls.reset();
      (component as any)['submitUploadImage']();
      expect(userServiceSpy.uploadUserImage).not.toHaveBeenCalled();
    });
  });

  // ── checkBackendConnection ────────────────────────────────────────────────────

  describe('checkBackendConnection', () => {
    it('should set isOnline=true when status is UP', () => {
      userServiceSpy.checkBackendConnection.and.callFake(() => of({ status: 'UP' }));
      component.checkBackendConnection();
      expect(component.isOnline()).toBeTrue();
    });

    it('should set lastCheckTime when called', () => {
      component.checkBackendConnection();
      expect(component.lastCheckTime()).not.toBeNull();
    });

    it('should set isOnline=false and show a warn message when status is not UP', () => {
      userServiceSpy.checkBackendConnection.and.callFake(() => of({ status: 'DOWN' }));
      component.checkBackendConnection();
      expect(component.isOnline()).toBeFalse();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'warn' }));
    });

    it('should set isOnline=false and show an error message on failure', () => {
      userServiceSpy.checkBackendConnection.and.callFake(() => throwError(() => new Error('net-fail')));
      component.checkBackendConnection();
      expect(component.isOnline()).toBeFalse();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── getOrderStatusTagInfo (tagManager) ───────────────────────────────────────

  describe('getOrderStatusTagInfo', () => {
    it('should return info severity for Pedido Realizado', () => {
      expect(getOrderStatusTagInfo('Pedido Realizado').severity).toBe('info');
    });

    it('should return info severity for Enviado', () => {
      expect(getOrderStatusTagInfo('Enviado').severity).toBe('info');
    });

    it('should return warn severity for En Reparto', () => {
      expect(getOrderStatusTagInfo('En Reparto').severity).toBe('warn');
    });

    it('should return success severity for Completado', () => {
      expect(getOrderStatusTagInfo('Completado').severity).toBe('success');
    });

    it('should return danger severity for Cancelado', () => {
      expect(getOrderStatusTagInfo('Cancelado').severity).toBe('danger');
    });

    it('should return secondary severity for unknown status', () => {
      expect(getOrderStatusTagInfo('Unknown').severity).toBe('secondary');
    });
  });

  // ── getRoleLabel ──────────────────────────────────────────────────────────────

  describe('getRoleLabel', () => {
    it('should return "Usuario registrado" for USER', () => {
      expect((component as any)['getRoleLabel']('USER')).toBe('Usuario registrado');
    });

    it('should return "Gerente" for MANAGER', () => {
      expect((component as any)['getRoleLabel']('MANAGER')).toBe('Gerente');
    });

    it('should return "Repartidor" for DRIVER', () => {
      expect((component as any)['getRoleLabel']('DRIVER')).toBe('Repartidor');
    });

    it('should return "Administrador" for ADMIN', () => {
      expect((component as any)['getRoleLabel']('ADMIN')).toBe('Administrador');
    });

    it('should return the raw value for unknown roles', () => {
      expect((component as any)['getRoleLabel']('SUPERUSER')).toBe('SUPERUSER');
    });
  });

  // ── isValidDueDate ────────────────────────────────────────────────────────────

  describe('isValidDueDate', () => {
    it('should return true for "12/25"', () => {
      expect((component as any)['isValidDueDate']('12/25')).toBeTrue();
    });

    it('should return true for "01/30"', () => {
      expect((component as any)['isValidDueDate']('01/30')).toBeTrue();
    });

    it('should return false for "13/25" (month > 12)', () => {
      expect((component as any)['isValidDueDate']('13/25')).toBeFalse();
    });

    it('should return false for "00/25" (month < 01)', () => {
      expect((component as any)['isValidDueDate']('00/25')).toBeFalse();
    });

    it('should return false for empty string', () => {
      expect((component as any)['isValidDueDate']('')).toBeFalse();
    });

    it('should return false for "12/2" (wrong year format)', () => {
      expect((component as any)['isValidDueDate']('12/2')).toBeFalse();
    });
  });

  // ── onColorChange ─────────────────────────────────────────────────────────────

  describe('onColorChange', () => {
    it('should call uiService.changeThemeColor with the given color', () => {
      const color: ThemeColor = { name: 'Azul', value: 'blue' };
      const uiSvc = (component as any)['uiService'];
      component.onColorChange(color);
      expect(uiSvc.changeThemeColor).toHaveBeenCalledWith(color);
    });

    it('should NOT call changeThemeColor when color is falsy', () => {
      const uiSvc = (component as any)['uiService'];
      uiSvc.changeThemeColor.calls.reset();
      component.onColorChange(null as any);
      expect(uiSvc.changeThemeColor).not.toHaveBeenCalled();
    });
  });

  // ── loadManagerShops ──────────────────────────────────────────────────────────

  describe('loadManagerShops', () => {
    beforeEach(() => {
      authServiceSpy.isUser.and.returnValue(false);
      authServiceSpy.isManager.and.returnValue(true);
    });

    it('should call shopService.getAssignedShopsPage and set assignedShopsPage', () => {
      const page = makePage([]);
      shopServiceSpy.getAssignedShopsPage.and.callFake(() => of(page));
      (component as any).loadManagerShops();
      expect(shopServiceSpy.getAssignedShopsPage).toHaveBeenCalled();
      expect(component.assignedShopsPage).toEqual(page);
    });

    it('should set assignedShopsPage to null on error', () => {
      shopServiceSpy.getAssignedShopsPage.and.returnValue(throwError(() => new Error('500')));
      (component as any).loadManagerShops();
      expect(component.assignedShopsPage).toBeNull();
    });
  });

  // ── loadDriverTruck ───────────────────────────────────────────────────────────

  describe('loadDriverTruck', () => {
    beforeEach(() => {
      authServiceSpy.isUser.and.returnValue(false);
      authServiceSpy.isDriver.and.returnValue(true);
    });

    it('should call getAssignedTruckByDriverId with user id and set assignedTruck', () => {
      (component as any).loadDriverTruck();
      expect(truckServiceSpy.getAssignedTruckByDriverId).toHaveBeenCalledWith(STUB_USER.id);
      expect(component.assignedTruck).not.toBeNull();
    });

    it('should set assignedTruck to null on error', () => {
      truckServiceSpy.getAssignedTruckByDriverId.and.returnValue(throwError(() => new Error('500')));
      (component as any).loadDriverTruck();
      expect(component.assignedTruck).toBeNull();
    });
  });

  // ── loadUserOrders ────────────────────────────────────────────────────────────

  describe('loadUserOrders', () => {
    it('should call getLoggedUserOrders and populate foundOrders', () => {
      const page = makePage([STUB_ORDER]);
      orderServiceSpy.getLoggedUserOrders.and.callFake(() => of(page));
      (component as any).loadUserOrders();
      expect(orderServiceSpy.getLoggedUserOrders).toHaveBeenCalled();
      expect(component.foundOrders).toEqual(page);
    });

    it('should set error=true and loading=false on failure', () => {
      orderServiceSpy.getLoggedUserOrders.and.returnValue(throwError(() => new Error('500')));
      (component as any).loadUserOrders();
      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
    });
  });

  // ── loadUserReviews ───────────────────────────────────────────────────────────

  describe('loadUserReviews', () => {
    it('should call getLoggedUserReviews and populate foundReviews', () => {
      const page = makePage([STUB_REVIEW]);
      reviewServiceSpy.getLoggedUserReviews.and.callFake(() => of(page));
      (component as any).loadUserReviews();
      expect(reviewServiceSpy.getLoggedUserReviews).toHaveBeenCalled();
      expect(component.foundReviews).toEqual(page);
    });

    it('should set error=true and loading=false on failure', () => {
      reviewServiceSpy.getLoggedUserReviews.and.returnValue(throwError(() => new Error('500')));
      (component as any).loadUserReviews();
      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
    });
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should hide main content when loading=true', () => {
      component.loading = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Test User');
    });

    it('should hide main content when error=true', () => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Test User');
    });

    it('should show user name when not loading and not error', () => {
      expect(fixture.nativeElement.textContent).toContain('Test User');
    });

    it('should show username in the personal data section', () => {
      expect(fixture.nativeElement.textContent).toContain('testuser');
    });

    it('should show "Eliminar cuenta" button when not admin', () => {
      expect(fixture.nativeElement.textContent).toContain('Eliminar cuenta');
    });

    it('should hide "Eliminar cuenta" button when admin', () => {
      authServiceSpy.isAdmin.and.returnValue(true);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Eliminar cuenta');
    });

    it('should show "Direcciones" section for user', () => {
      expect(fixture.nativeElement.textContent).toContain('Direcciones');
    });

    it('should NOT show "Direcciones" section for non-user', () => {
      authServiceSpy.isUser.and.returnValue(false);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Direcciones');
    });

    it('should show "Mis Pedidos" section for user', () => {
      expect(fixture.nativeElement.textContent).toContain('Mis Pedidos');
    });

    it('should NOT show "Mis Pedidos" for non-user', () => {
      authServiceSpy.isUser.and.returnValue(false);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Mis Pedidos');
    });

    it('should show "Mis Reseñas" section for user', () => {
      expect(fixture.nativeElement.textContent).toContain('Mis Reseñas');
    });

    it('should show order reference code in the orders list', () => {
      expect(fixture.nativeElement.textContent).toContain('REF-001');
    });

    it('should show "Lugares de Trabajo" section for manager', () => {
      authServiceSpy.isUser.and.returnValue(false);
      authServiceSpy.isManager.and.returnValue(true);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Lugares de Trabajo');
    });

    it('should NOT show "Lugares de Trabajo" for non-manager', () => {
      expect(fixture.nativeElement.textContent).not.toContain('Lugares de Trabajo');
    });

    it('should show "Vehículo Asignado" section for driver', () => {
      authServiceSpy.isUser.and.returnValue(false);
      authServiceSpy.isDriver.and.returnValue(true);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Vehículo Asignado');
    });

    it('should NOT show "Vehículo Asignado" for non-driver', () => {
      expect(fixture.nativeElement.textContent).not.toContain('Vehículo Asignado');
    });

    it('should show "Estado de la Cuenta" section for admin', () => {
      authServiceSpy.isAdmin.and.returnValue(true);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Estado de la Cuenta');
    });

    it('should NOT show "Estado de la Cuenta" for non-admin', () => {
      expect(fixture.nativeElement.textContent).not.toContain('Estado de la Cuenta');
    });

    it('should always show the "Personalización" section', () => {
      expect(fixture.nativeElement.textContent).toContain('Personalización');
    });

    it('should always show the "Datos personales" section', () => {
      expect(fixture.nativeElement.textContent).toContain('Datos personales');
    });
  });
});
