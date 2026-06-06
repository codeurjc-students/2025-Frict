import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {OrderSummaryComponent} from './order-summary.component';
import {OrderService} from '../../../services/order.service';
import {UserService} from '../../../services/user.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {LocationService} from '../../../services/location.service';
import {ActivatedRoute, Router} from '@angular/router';
import {MessageService} from 'primeng/api';
import {of, Subject, throwError} from 'rxjs';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {User} from '../../../models/user.model';
import {Address} from '../../../models/address.model';
import {PaymentCard} from '../../../models/paymentCard.model';
import {CartSummary} from '../../../models/cartSummary.model';
import {OrderItem} from '../../../models/orderItem.model';
import {PageResponse} from '../../../models/pageResponse.model';
import {Order} from '../../../models/order.model';

describe('OrderSummaryComponent', () => {
  let component: OrderSummaryComponent;
  let fixture: ComponentFixture<OrderSummaryComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let breadcrumbServiceSpy: jasmine.SpyObj<BreadcrumbService>;
  let locationServiceSpy: jasmine.SpyObj<LocationService>;
  let routerSpy: jasmine.SpyObj<Router>;

  // ─── Mock data ────────────────────────────────────────────────────────────────

  const mockAddress: Address = {
    id: 'addr-1', alias: 'Casa', street: 'Calle Falsa',
    number: '123', floor: '1A', postalCode: '28001', city: 'Madrid', country: 'España'
  };

  const mockCard: PaymentCard = {
    id: 'card-1', alias: 'Visa', cardOwnerName: 'Test User',
    number: '1234 5678 9012 3456', numberEnding: '3456', cvv: '123', dueDate: '12/25'
  };

  const mockUser: User = {
    id: 'user-1', name: 'Test User', username: 'testuser',
    roles: ['ROLE_USER'], email: 'test@test.com', phone: '123456789',
    addresses: [mockAddress], cards: [mockCard],
    imageInfo: { id: 'img-1', imageUrl: '', s3Key: '', fileName: '' },
    banned: false, deleted: false, selectedShopId: null,
    ordersCount: 0, favouriteProductsCount: 0, connection: null
  };

  const mockCartSummary: CartSummary = {
    totalItems: 2, subtotalCost: 118, totalDiscount: 0, shippingCost: 0, totalCost: 118
  };

  const mockOrderItem: OrderItem = {
    id: 'item-1', orderId: 'order-1',
    product: {
      id: 'prod-1', referenceCode: 'REF-001', name: 'Producto Test',
      description: 'Descripción',
      imagesInfo: [{ id: 'img-1', imageUrl: 'http://img.test/prod.png', s3Key: 'key', fileName: 'prod.png' }],
      supplyPrice: 30, previousPrice: 70, currentPrice: 59, active: true, discount: '-15%',
      categories: [], totalUnits: 100, availableUnits: 50, shopsWithStock: 2,
      averageRating: 4, totalReviews: 5, specifications: [], capacity: 1, createdAt: '2026-05-08'
    },
    productName: 'Producto Test', productImageUrl: 'http://img.test/prod.png',
    productPrice: 59, userId: 'user-1', quantity: 2, itemsCost: 118
  };

  const mockCartItemsPage: PageResponse<OrderItem> = {
    items: [mockOrderItem], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5
  };

  const mockOrder: Partial<Order> = { id: 'order-new', referenceCode: 'REF-NEW' };

  // ─── Setup ────────────────────────────────────────────────────────────────────

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', [
      'getUserCartItemsPage', 'getUserCartSummary', 'createOrder', 'setItemsCount'
    ]);
    orderServiceSpy.getUserCartItemsPage.and.returnValue(of(mockCartItemsPage));
    orderServiceSpy.getUserCartSummary.and.returnValue(of(mockCartSummary));
    orderServiceSpy.createOrder.and.returnValue(of(mockOrder as Order));

    userServiceSpy = jasmine.createSpyObj('UserService', [
      'getLoggedUserInfo', 'submitAddress', 'submitPaymentCard'
    ]);
    userServiceSpy.getLoggedUserInfo.and.returnValue(of(mockUser));
    userServiceSpy.submitAddress.and.returnValue(of(mockUser));
    userServiceSpy.submitPaymentCard.and.returnValue(of(mockUser));

    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    locationServiceSpy = jasmine.createSpyObj('LocationService', ['getCoordinatesFromAddress', 'getAddressFromCoordinates']);
    locationServiceSpy.getCoordinatesFromAddress.and.returnValue(of({ latitude: 40.4168, longitude: -3.7038 }));
    locationServiceSpy.getAddressFromCoordinates.and.returnValue(of({ street: 'Calle', number: '1', city: 'Madrid', postalCode: '28001', country: 'España' }));

    breadcrumbServiceSpy = jasmine.createSpyObj('BreadcrumbService', [
      'insertPenultimateNodesForUrl', 'setBaseBreadcrumbs', 'breadcrumbs'
    ]);
    breadcrumbServiceSpy.breadcrumbs.and.returnValue([]);

    routerSpy = jasmine.createSpyObj(
      'Router',
      ['navigate', 'navigateByUrl', 'createUrlTree', 'serializeUrl'],
      { url: '/order-summary', events: new Subject<any>(), navigated: false }
    );
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/cart');
    routerSpy.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [
        OrderSummaryComponent,
        BrowserAnimationsModule
      ],
      providers: [
        { provide: OrderService,      useValue: orderServiceSpy },
        { provide: UserService,       useValue: userServiceSpy },
        { provide: MessageService,    useValue: messageServiceSpy },
        { provide: BreadcrumbService, useValue: breadcrumbServiceSpy },
        { provide: LocationService, useValue: locationServiceSpy },
        { provide: Router,            useValue: routerSpy },
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

    fixture = TestBed.createComponent(OrderSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ─── Creation ─────────────────────────────────────────────────────────────────

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // ─── Initial state ────────────────────────────────────────────────────────────

  it('should start with activeStep = 1', () => {
    expect(component.activeStep).toBe(1);
  });

  it('should start with no selectedAddress', () => {
    expect(component.selectedAddress).toBeUndefined();
  });

  it('should start with no selectedPaymentCard', () => {
    expect(component.selectedPaymentCard).toBeUndefined();
  });

  it('should start with showNewAddressForm = false', () => {
    expect(component.showNewAddressForm).toBeFalse();
  });

  it('should start with showNewPaymentForm = false', () => {
    expect(component.showNewPaymentForm).toBeFalse();
  });

  // ─── Happy-path load chain ────────────────────────────────────────────────────

  it('should call getLoggedUserInfo on init', () => {
    expect(userServiceSpy.getLoggedUserInfo).toHaveBeenCalled();
  });

  it('should call getUserCartItemsPage after loading user info', () => {
    expect(orderServiceSpy.getUserCartItemsPage).toHaveBeenCalledWith(0, 5);
  });

  it('should call getUserCartSummary after loading cart items', () => {
    expect(orderServiceSpy.getUserCartSummary).toHaveBeenCalled();
  });

  it('should set loading=false after the full load chain completes', () => {
    expect(component.loading).toBeFalse();
  });

  it('should set error=false after a successful load', () => {
    expect(component.error).toBeFalse();
  });

  it('should populate user after a successful load', () => {
    expect(component.user).toEqual(mockUser);
  });

  it('should populate cartSummary after a successful load', () => {
    expect(component.cartSummary).toEqual(mockCartSummary);
  });

  it('should populate cartItemsPage after a successful load', () => {
    expect(component.cartItemsPage).toEqual(mockCartItemsPage);
  });

  // ─── Error states ─────────────────────────────────────────────────────────────

  it('should set error=true and loading=false when getLoggedUserInfo fails', () => {
    userServiceSpy.getLoggedUserInfo.and.returnValue(throwError(() => new Error('500')));
    (component as any).getUserInfo();
    expect(component.error).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  it('should set error=true and loading=false when getUserCartItemsPage fails', () => {
    orderServiceSpy.getUserCartItemsPage.and.returnValue(throwError(() => new Error('500')));
    (component as any).getUserInfo();
    expect(component.error).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  // ─── DOM: loading / main content ──────────────────────────────────────────────

  it('should show the loading screen when loading=true', () => {
    component.loading = true;
    fixture.detectChanges();
    const loadingEl = fixture.nativeElement.querySelector('app-loading-screen');
    expect(loadingEl).not.toBeNull();
  });

  it('should hide the loading screen and show main content when loading=false and error=false', () => {
    expect(component.loading).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Finalizar compra');
  });

  // ─── DOM: order summary aside ─────────────────────────────────────────────────

  it('should show GRATIS badge when shippingCost is 0', () => {
    expect(fixture.nativeElement.textContent).toContain('GRATIS');
  });

  it('should NOT show GRATIS badge when shippingCost > 0', () => {
    component.cartSummary = { ...mockCartSummary, shippingCost: 5 };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('GRATIS');
  });

  it('should display the formatted total in the aside', () => {
    // totalCost(118) + shippingCost(0) = 118 → '118€'
    expect(fixture.nativeElement.textContent).toContain('118€');
  });

  // ─── Interactions: address ────────────────────────────────────────────────────

  it('should set selectedAddress when changeAddress() is called', () => {
    (component as any).changeAddress(mockAddress);
    expect(component.selectedAddress).toEqual(mockAddress);
  });

  it('should reset newAddress fields and hide form on cancelNewAddress()', () => {
    component.showNewAddressForm = true;
    component.newAddress = { ...mockAddress };
    (component as any).cancelNewAddress();
    expect(component.showNewAddressForm).toBeFalse();
    expect(component.newAddress.alias).toBe('');
    expect(component.newAddress.street).toBe('');
  });

  it('should call submitAddress, update user, and hide form on saveNewAddress()', () => {
    const updatedUser = { ...mockUser };
    userServiceSpy.submitAddress.and.returnValue(of(updatedUser));
    component.showNewAddressForm = true;
    component.newAddress = { ...mockAddress };
    (component as any).saveNewAddress();
    expect(userServiceSpy.submitAddress).toHaveBeenCalled();
    expect(component.user).toEqual(updatedUser);
    expect(component.loadingAddresses).toBeFalse();
    expect(component.showNewAddressForm).toBeFalse();
  });

  // ─── Interactions: payment card ───────────────────────────────────────────────

  it('should set selectedPaymentCard when changePaymentCard() is called', () => {
    (component as any).changePaymentCard(mockCard);
    expect(component.selectedPaymentCard).toEqual(mockCard);
  });

  it('should reset newCard fields and hide form on cancelNewCard()', () => {
    component.showNewPaymentForm = true;
    component.newCard = { ...mockCard };
    (component as any).cancelNewCard();
    expect(component.showNewPaymentForm).toBeFalse();
    expect(component.newCard.alias).toBe('');
    expect(component.newCard.cvv).toBe('');
  });

  // ─── isValidDueDate ───────────────────────────────────────────────────────────

  it('should return true for valid MM/YY due dates', () => {
    expect((component as any).isValidDueDate('01/25')).toBeTrue();
    expect((component as any).isValidDueDate('12/30')).toBeTrue();
    expect((component as any).isValidDueDate('09/99')).toBeTrue();
  });

  it('should return false for invalid due dates', () => {
    expect((component as any).isValidDueDate('13/25')).toBeFalse();
    expect((component as any).isValidDueDate('00/25')).toBeFalse();
    expect((component as any).isValidDueDate('1/25')).toBeFalse();
    expect((component as any).isValidDueDate('')).toBeFalse();
  });

  // ─── saveNewCard ──────────────────────────────────────────────────────────────

  it('should call submitPaymentCard and update user when due date is valid', () => {
    const updatedUser = { ...mockUser };
    userServiceSpy.submitPaymentCard.and.returnValue(of(updatedUser));
    component.newCard = { ...mockCard, dueDate: '06/27' };
    (component as any).saveNewCard();
    expect(userServiceSpy.submitPaymentCard).toHaveBeenCalled();
    expect(component.user).toEqual(updatedUser);
  });

  it('should show an error message and NOT call submitPaymentCard when due date is invalid', () => {
    component.newCard = { ...mockCard, dueDate: '13/27' };
    (component as any).saveNewCard();
    expect(messageServiceSpy.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'error' })
    );
    expect(userServiceSpy.submitPaymentCard).not.toHaveBeenCalled();
  });

  // ─── confirmOrder ─────────────────────────────────────────────────────────────

  it('should call createOrder with the selected address and card ids', () => {
    component.selectedAddress = mockAddress;
    component.selectedPaymentCard = mockCard;
    (component as any).confirmOrder();
    expect(orderServiceSpy.createOrder).toHaveBeenCalledWith(mockAddress.id, mockCard.id);
  });

  it('should call setItemsCount(0) and navigate to /success on successful order', () => {
    component.selectedAddress = mockAddress;
    component.selectedPaymentCard = mockCard;
    (component as any).confirmOrder();
    expect(orderServiceSpy.setItemsCount).toHaveBeenCalledWith(0);
    expect(routerSpy.navigate).toHaveBeenCalledWith(
      ['/success'],
      jasmine.objectContaining({ queryParams: { id: 'order-new', ref: 'REF-NEW' } })
    );
  });

  it('should show an error message when no address is selected', () => {
    component.selectedAddress = undefined;
    component.selectedPaymentCard = mockCard;
    (component as any).confirmOrder();
    expect(messageServiceSpy.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'error' })
    );
    expect(orderServiceSpy.createOrder).not.toHaveBeenCalled();
  });

  it('should show an error message when no payment card is selected', () => {
    component.selectedAddress = mockAddress;
    component.selectedPaymentCard = undefined;
    (component as any).confirmOrder();
    expect(messageServiceSpy.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'error' })
    );
    expect(orderServiceSpy.createOrder).not.toHaveBeenCalled();
  });

  // ─── reloadAll ────────────────────────────────────────────────────────────────

  it('should reset selectedAddress, selectedPaymentCard, activeStep and reload on reloadAll()', () => {
    component.selectedAddress = mockAddress;
    component.selectedPaymentCard = mockCard;
    component.activeStep = 3;
    const prevCallCount = userServiceSpy.getLoggedUserInfo.calls.count();
    (component as any).reloadAll();
    expect(component.selectedAddress).toBeUndefined();
    expect(component.selectedPaymentCard).toBeUndefined();
    expect(component.activeStep).toBe(1);
    expect(userServiceSpy.getLoggedUserInfo.calls.count()).toBeGreaterThan(prevCallCount);
  });

  // ─── onCartItemsPageChange ────────────────────────────────────────────────────

  it('should update firstItem and itemsRows and reload when page changes', () => {
    const prevCallCount = userServiceSpy.getLoggedUserInfo.calls.count();
    component.onCartItemsPageChange({ first: 10, rows: 10, page: 1, pageCount: 2 });
    expect(component.firstItem).toBe(10);
    expect(component.itemsRows).toBe(10);
    expect(userServiceSpy.getLoggedUserInfo.calls.count()).toBeGreaterThan(prevCallCount);
  });

  it('should call getUserCartItemsPage with correct page index after page change', () => {
    component.onCartItemsPageChange({ first: 10, rows: 10, page: 1, pageCount: 2 });
    // page = first(10) / rows(10) = 1
    expect(orderServiceSpy.getUserCartItemsPage).toHaveBeenCalledWith(1, 10);
  });

  // ─── breadcrumbService integration ────────────────────────────────────────────

  it('should call breadcrumbService.insertPenultimateNodesForUrl on each getUserInfo call', () => {
    const prevCallCount = breadcrumbServiceSpy.insertPenultimateNodesForUrl.calls.count();
    (component as any).getUserInfo();
    expect(breadcrumbServiceSpy.insertPenultimateNodesForUrl.calls.count()).toBeGreaterThan(prevCallCount);
  });

  // ─── toggleAddressForm ────────────────────────────────────────────────────────

  describe('toggleAddressForm', () => {
    it('should set showNewAddressForm to true', () => {
      component.showNewAddressForm = false;
      component.toggleAddressForm();
      expect(component.showNewAddressForm).toBeTrue();
    });

    it('should set newAddress.alias to "Nueva dirección de envío"', () => {
      component.toggleAddressForm();
      expect(component.newAddress.alias).toBe('Nueva dirección de envío');
    });
  });

  // ─── cancelNewAddress (with map) ──────────────────────────────────────────────

  describe('cancelNewAddress when addressMap exists', () => {
    let mockMap: any;

    beforeEach(() => {
      mockMap = { remove: jasmine.createSpy('remove') };
      (component as any).addressMap = mockMap;
      (component as any).addressMarker = {};
    });

    it('should call remove() on the map', () => {
      (component as any).cancelNewAddress();
      expect(mockMap.remove).toHaveBeenCalled();
    });

    it('should set addressMap and addressMarker to undefined', () => {
      (component as any).cancelNewAddress();
      expect((component as any).addressMap).toBeUndefined();
      expect((component as any).addressMarker).toBeUndefined();
    });

    it('should reset geocoding flags and hide the form', () => {
      (component as any).isGeocodingActive = true;
      (component as any).lastAddressCheck = 'street|1|city|28001|España';
      component.submittedAddress = true;
      (component as any).cancelNewAddress();
      expect((component as any).isGeocodingActive).toBeFalse();
      expect((component as any).lastAddressCheck).toBe('');
      expect(component.submittedAddress).toBeFalse();
      expect(component.showNewAddressForm).toBeFalse();
    });
  });

  // ─── togglePaymentForm ────────────────────────────────────────────────────────

  describe('togglePaymentForm', () => {
    it('should show the payment form and set alias when form is hidden', () => {
      component.showNewPaymentForm = false;
      component.togglePaymentForm();
      expect(component.showNewPaymentForm).toBeTrue();
      expect(component.newCard.alias).toBe('Nueva tarjeta');
    });

    it('should call cancelNewCard and hide the form when form is already visible', () => {
      component.showNewPaymentForm = true;
      component.newCard = { ...mockCard };
      component.togglePaymentForm();
      expect(component.showNewPaymentForm).toBeFalse();
      expect(component.newCard.alias).toBe('');
    });
  });

  // ─── isCardFieldInvalid ───────────────────────────────────────────────────────

  describe('isCardFieldInvalid', () => {
    it('should return false when submittedCard is false regardless of value', () => {
      component.submittedCard = false;
      expect(component.isCardFieldInvalid('')).toBeFalse();
    });

    it('should return true when submittedCard is true and value is blank', () => {
      component.submittedCard = true;
      expect(component.isCardFieldInvalid('   ')).toBeTrue();
      expect(component.isCardFieldInvalid('')).toBeTrue();
    });

    it('should return false when submittedCard is true and value is non-blank', () => {
      component.submittedCard = true;
      expect(component.isCardFieldInvalid('VISA')).toBeFalse();
    });
  });

  // ─── isCardDueDateInvalid ─────────────────────────────────────────────────────

  describe('isCardDueDateInvalid', () => {
    it('should return false when submittedCard is false', () => {
      component.submittedCard = false;
      component.newCard.dueDate = '13/99';
      expect(component.isCardDueDateInvalid()).toBeFalse();
    });

    it('should return true when submittedCard is true and dueDate is invalid', () => {
      component.submittedCard = true;
      component.newCard.dueDate = '13/99';
      expect(component.isCardDueDateInvalid()).toBeTrue();
    });

    it('should return false when submittedCard is true and dueDate is valid', () => {
      component.submittedCard = true;
      component.newCard.dueDate = '06/27';
      expect(component.isCardDueDateInvalid()).toBeFalse();
    });
  });

  // ─── isAddressFieldInvalid ────────────────────────────────────────────────────

  describe('isAddressFieldInvalid', () => {
    it('should return false when submittedAddress is false', () => {
      component.submittedAddress = false;
      expect(component.isAddressFieldInvalid('')).toBeFalse();
    });

    it('should return true when submittedAddress is true and value is blank', () => {
      component.submittedAddress = true;
      expect(component.isAddressFieldInvalid('')).toBeTrue();
      expect(component.isAddressFieldInvalid('  ')).toBeTrue();
    });

    it('should return false when submittedAddress is true and value is non-blank', () => {
      component.submittedAddress = true;
      expect(component.isAddressFieldInvalid('Madrid')).toBeFalse();
    });
  });

  // ─── hasAddressValidationErrors ───────────────────────────────────────────────

  describe('hasAddressValidationErrors', () => {
    it('should return false when all required address fields are filled', () => {
      component.newAddress = { ...mockAddress };
      expect((component as any).hasAddressValidationErrors()).toBeFalse();
    });

    it('should return true when street is blank', () => {
      component.newAddress = { ...mockAddress, street: '' };
      expect((component as any).hasAddressValidationErrors()).toBeTrue();
    });

    it('should return true when city is blank', () => {
      component.newAddress = { ...mockAddress, city: '' };
      expect((component as any).hasAddressValidationErrors()).toBeTrue();
    });

    it('should return true when country is blank', () => {
      component.newAddress = { ...mockAddress, country: '' };
      expect((component as any).hasAddressValidationErrors()).toBeTrue();
    });
  });

  // ─── saveNewAddress (validation error) ───────────────────────────────────────

  describe('saveNewAddress validation errors', () => {
    it('should show error message and set submittedAddress=true when fields are blank', () => {
      component.newAddress = { id: '', alias: '', street: '', number: '', floor: '', postalCode: '', city: '', country: '' };
      (component as any).saveNewAddress();
      expect(component.submittedAddress).toBeTrue();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(userServiceSpy.submitAddress).not.toHaveBeenCalled();
    });
  });

  // ─── onAddressFieldChange ─────────────────────────────────────────────────────

  describe('onAddressFieldChange', () => {
    it('should not throw when called', () => {
      expect(() => component.onAddressFieldChange()).not.toThrow();
    });
  });

  // ─── getAddressFromCoordinates ────────────────────────────────────────────────

  describe('getAddressFromCoordinates', () => {
    it('should call locationService.getAddressFromCoordinates', () => {
      (component as any).getAddressFromCoordinates(40.4, -3.7);
      expect(locationServiceSpy.getAddressFromCoordinates).toHaveBeenCalledWith(40.4, -3.7);
    });

    it('should update newAddress fields and show "Dirección Exacta" when number is returned', () => {
      locationServiceSpy.getAddressFromCoordinates.and.returnValue(
        of({ street: 'Gran Vía', number: '5', city: 'Madrid', postalCode: '28001', country: 'España' } as any)
      );
      (component as any).getAddressFromCoordinates(40.4, -3.7);
      expect(component.newAddress.street).toBe('Gran Vía');
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ summary: 'Dirección Exacta' }));
    });

    it('should show "Zona detectada" when addressData has no number', () => {
      locationServiceSpy.getAddressFromCoordinates.and.returnValue(
        of({ street: 'Gran Vía', number: '', city: 'Madrid', postalCode: '28001', country: 'España' } as any)
      );
      (component as any).getAddressFromCoordinates(40.4, -3.7);
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ summary: 'Zona detectada' }));
    });

    it('should show "Dirección no encontrada" when addressData is null', () => {
      locationServiceSpy.getAddressFromCoordinates.and.returnValue(of(null));
      (component as any).getAddressFromCoordinates(40.4, -3.7);
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ summary: 'Dirección no encontrada' }));
    });

    it('should show "Aviso" and re-enable geocoding on error', () => {
      locationServiceSpy.getAddressFromCoordinates.and.returnValue(throwError(() => new Error('500')));
      (component as any).isGeocodingActive = false;
      (component as any).getAddressFromCoordinates(40.4, -3.7);
      expect((component as any).isGeocodingActive).toBeTrue();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ summary: 'Aviso' }));
    });
  });

  // ─── setupAddressListener pipeline ───────────────────────────────────────────

  describe('setupAddressListener pipeline', () => {
    beforeEach(() => {
      component.newAddress = { id: '', alias: '', street: 'Calle Mayor', number: '1', floor: '', postalCode: '28001', city: 'Madrid', country: 'España' };
      (component as any).isGeocodingActive = true;
      (component as any).lastAddressCheck = '';
      (component as any).addressMap = {
        addLayer: jasmine.createSpy('addLayer').and.returnValue({}),
        setView: jasmine.createSpy('setView'),
        remove: jasmine.createSpy('remove')
      };
    });

    it('should call getCoordinatesFromAddress after debounce when geocoding is active', fakeAsync(() => {
      locationServiceSpy.getCoordinatesFromAddress.and.returnValue(of({ latitude: 40.4, longitude: -3.7 }));
      component.onAddressFieldChange();
      tick(1000);
      expect(locationServiceSpy.getCoordinatesFromAddress).toHaveBeenCalled();
    }));

    it('should update newAddress lat/lng and show "Ubicación actualizada" on coords returned', fakeAsync(() => {
      locationServiceSpy.getCoordinatesFromAddress.and.returnValue(of({ latitude: 40.4, longitude: -3.7 }));
      component.onAddressFieldChange();
      tick(1000);
      expect(component.newAddress.latitude).toBe(40.4);
      expect(component.newAddress.longitude).toBe(-3.7);
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ summary: 'Ubicación actualizada' }));
    }));

    it('should show "Dirección no encontrada" when getCoordinatesFromAddress returns null', fakeAsync(() => {
      locationServiceSpy.getCoordinatesFromAddress.and.returnValue(of(null));
      component.onAddressFieldChange();
      tick(1000);
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ summary: 'Dirección no encontrada' }));
    }));

    it('should not call geocoding service when isGeocodingActive is false', fakeAsync(() => {
      (component as any).isGeocodingActive = false;
      locationServiceSpy.getCoordinatesFromAddress.calls.reset();
      component.onAddressFieldChange();
      tick(1000);
      expect(locationServiceSpy.getCoordinatesFromAddress).not.toHaveBeenCalled();
    }));

    it('should not call geocoding service when address has not changed since last check', fakeAsync(() => {
      const addr = component.newAddress;
      (component as any).lastAddressCheck = `${addr.street}|${addr.number}|${addr.city}|${addr.postalCode}|${addr.country}`;
      locationServiceSpy.getCoordinatesFromAddress.calls.reset();
      component.onAddressFieldChange();
      tick(1000);
      expect(locationServiceSpy.getCoordinatesFromAddress).not.toHaveBeenCalled();
    }));

    it('should not call geocoding service when street is blank', fakeAsync(() => {
      component.newAddress.street = '';
      locationServiceSpy.getCoordinatesFromAddress.calls.reset();
      component.onAddressFieldChange();
      tick(1000);
      expect(locationServiceSpy.getCoordinatesFromAddress).not.toHaveBeenCalled();
    }));
  });
});
