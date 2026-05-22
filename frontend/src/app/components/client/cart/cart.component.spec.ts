import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {CartComponent} from './cart.component';
import {OrderService} from '../../../services/order.service';
import {ProductService} from '../../../services/product.service';
import {AuthService} from '../../../services/auth.service';
import {ShopService} from '../../../services/shop.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {ImageInfo} from '../../../models/imageInfo.model';
import {Product} from '../../../models/product.model';
import {OrderItem} from '../../../models/orderItem.model';
import {CartSummary} from '../../../models/cartSummary.model';
import {PageResponse} from '../../../models/pageResponse.model';

describe('CartComponent', () => {
  let component: CartComponent;
  let fixture: ComponentFixture<CartComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let authServiceMock: { selectedShopId: jasmine.Spy; isAdmin: jasmine.Spy; isManager: jasmine.Spy; isDriver: jasmine.Spy };
  let shopServiceSpy: jasmine.SpyObj<ShopService>;
  let breadcrumbServiceSpy: jasmine.SpyObj<BreadcrumbService>;
  let routerSpy: jasmine.SpyObj<Router>;

  // ─── Mock data ────────────────────────────────────────────────────────────────

  const mockImageInfo: ImageInfo = {
    id: 'img-1', imageUrl: 'http://img.test/p.png', s3Key: 'key', fileName: 'p.png'
  };

  const mockProduct: Product = {
    id: 'prod-1', referenceCode: 'REF-001', name: 'Ratón Gaming Pro',
    description: 'Descripción', imagesInfo: [mockImageInfo],
    supplyPrice: 20, previousPrice: 80, currentPrice: 59, active: true, discount: '-25%',
    categories: [{ id: 'cat-1', name: 'Periféricos', icon: '', bannerText: '', shortDescription: '',
                   longDescription: '', imageInfo: mockImageInfo, timesUsed: 1, parentId: '', children: [] }],
    totalUnits: 100, availableUnits: 50, shopsWithStock: 3, averageRating: 4.5, totalReviews: 12, specifications: [], capacity: 1, createdAt: '2026-05-08'
  };

  const mockOrderItem: OrderItem = {
    id: 'item-1', orderId: 'order-1', product: mockProduct,
    productName: 'Ratón Gaming Pro', productImageUrl: 'http://img.test/p.png',
    productPrice: 59, userId: 'user-1', quantity: 2, itemsCost: 118
  };

  const mockFavProduct: Product = {
    id: 'fav-1', referenceCode: 'FAV-001', name: 'Teclado Mecánico',
    description: 'Descripción', imagesInfo: [mockImageInfo],
    supplyPrice: 30, previousPrice: 120, currentPrice: 89, active: true, discount: '-26%',
    categories: [], totalUnits: 60, availableUnits: 30,
    shopsWithStock: 2, averageRating: 4.8, totalReviews: 20, specifications: [], capacity: 1, createdAt: '2026-05-08'
  };

  const mockCartItemsPage: PageResponse<OrderItem> = {
    items: [mockOrderItem], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5
  };

  const mockFavProductsPage: PageResponse<Product> = {
    items: [mockFavProduct], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5
  };

  const mockCartSummary: CartSummary = {
    totalItems: 2, subtotalCost: 118, totalDiscount: 0, shippingCost: 5, totalCost: 118
  };

  const mockShop = { id: 'shop-1', name: 'Tienda Madrid' } as any;

  const emptyPage = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0 };

  // ─── Setup ────────────────────────────────────────────────────────────────────

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', [
      'getUserCartItemsPage', 'getUserCartSummary', 'updateItemQuantity',
      'setItemsCount', 'clearUserCartItems', 'deleteItem', 'addItemToCart'
    ]);
    // Use callFake so each call returns a fresh object — the component mutates
    // foundItems in-place (clearCart, removeItem), which would corrupt the shared
    // const if we used returnValue(of(mockCartItemsPage)) directly.
    orderServiceSpy.getUserCartItemsPage.and.callFake(() =>
      of({ items: [{ ...mockOrderItem }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 })
    );
    orderServiceSpy.getUserCartSummary.and.returnValue(of(mockCartSummary));
    orderServiceSpy.updateItemQuantity.and.returnValue(of(mockCartSummary));
    orderServiceSpy.clearUserCartItems.and.returnValue(of({ ...mockCartSummary, totalItems: 0, subtotalCost: 0, totalCost: 0 }));
    orderServiceSpy.deleteItem.and.returnValue(of({ ...mockCartSummary, totalItems: 1 }));
    orderServiceSpy.addItemToCart.and.returnValue(of(mockOrderItem));

    productServiceSpy = jasmine.createSpyObj('ProductService', [
      'getUserFavouriteProductsPage', 'deleteProductFromFavourites', 'searchScope'
    ]);
    productServiceSpy.searchScope.and.returnValue('GLOBAL');
    // Use callFake so each call returns a fresh object — the component mutates
    // foundProducts in-place (removeFavorite), which would corrupt the shared
    // const if we used returnValue(of(mockFavProductsPage)) directly.
    productServiceSpy.getUserFavouriteProductsPage.and.callFake(() =>
      of({ items: [{ ...mockFavProduct }], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5 })
    );
    productServiceSpy.deleteProductFromFavourites.and.returnValue(of(void 0));

    authServiceMock = {
      selectedShopId: jasmine.createSpy('selectedShopId').and.returnValue(null),
      isAdmin:        jasmine.createSpy('isAdmin').and.returnValue(false),
      isManager:      jasmine.createSpy('isManager').and.returnValue(false),
      isDriver:       jasmine.createSpy('isDriver').and.returnValue(false)
    };

    shopServiceSpy = jasmine.createSpyObj('ShopService', ['getShopById']);
    shopServiceSpy.getShopById.and.returnValue(of(mockShop));

    breadcrumbServiceSpy = jasmine.createSpyObj('BreadcrumbService', [
      'setBaseBreadcrumbs', 'insertPenultimateNodesForUrl', 'breadcrumbs'
    ]);
    breadcrumbServiceSpy.breadcrumbs.and.returnValue([]);

    routerSpy = jasmine.createSpyObj(
      'Router',
      ['navigate', 'navigateByUrl', 'createUrlTree', 'serializeUrl'],
      { url: '/cart', events: new Subject<any>(), navigated: false }
    );
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');
    routerSpy.navigateByUrl.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [CartComponent, BrowserAnimationsModule],
      providers: [
        { provide: OrderService,      useValue: orderServiceSpy },
        { provide: ProductService,    useValue: productServiceSpy },
        { provide: AuthService,       useValue: authServiceMock },
        { provide: ShopService,       useValue: shopServiceSpy },
        { provide: BreadcrumbService, useValue: breadcrumbServiceSpy },
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

    fixture = TestBed.createComponent(CartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ─── Creation ─────────────────────────────────────────────────────────────────

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // ─── ngOnInit calls ───────────────────────────────────────────────────────────

  it('should call getUserCartItemsPage on init', () => {
    expect(orderServiceSpy.getUserCartItemsPage).toHaveBeenCalledWith(0, 5);
  });

  it('should call getUserCartSummary on init', () => {
    expect(orderServiceSpy.getUserCartSummary).toHaveBeenCalled();
  });

  it('should call getUserFavouriteProductsPage on init', () => {
    expect(productServiceSpy.getUserFavouriteProductsPage).toHaveBeenCalledWith(0, 5);
  });

  it('should NOT call getShopById when selectedShopId is null', () => {
    expect(shopServiceSpy.getShopById).not.toHaveBeenCalled();
  });

  // ─── State after happy-path load ──────────────────────────────────────────────

  it('should set loading=false after successful load', () => {
    expect(component.loading).toBeFalse();
  });

  it('should set error=false after successful load', () => {
    expect(component.error).toBeFalse();
  });

  it('should populate foundItems after successful load', () => {
    expect(component.foundItems).toEqual(mockCartItemsPage);
  });

  it('should populate cartSummary after successful load', () => {
    expect(component.cartSummary).toEqual(mockCartSummary);
  });

  it('should populate foundProducts after successful load', () => {
    expect(component.foundProducts).toEqual(mockFavProductsPage);
  });

  // ─── Error states ─────────────────────────────────────────────────────────────

  it('should set error=true and loading=false when getUserCartItemsPage fails', () => {
    orderServiceSpy.getUserCartItemsPage.and.returnValue(throwError(() => new Error('500')));
    (component as any).getUserCartItemsPage();
    expect(component.error).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  it('should set error=true and loading=false when getUserCartSummary fails', () => {
    orderServiceSpy.getUserCartSummary.and.returnValue(throwError(() => new Error('500')));
    (component as any).getUserCartSummary();
    expect(component.error).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  it('should set error=true and loading=false when getUserFavouriteProductsPage fails', () => {
    productServiceSpy.getUserFavouriteProductsPage.and.returnValue(throwError(() => new Error('500')));
    (component as any).getUserFavouriteProducts();
    expect(component.error).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  // ─── getSelectedShop ──────────────────────────────────────────────────────────

  it('should call getShopById and set selectedShop when selectedShopId is truthy', () => {
    authServiceMock.selectedShopId.and.returnValue('shop-1');
    component.getSelectedShop();
    expect(shopServiceSpy.getShopById).toHaveBeenCalledWith('shop-1');
    expect((component as any).selectedShop).toEqual(mockShop);
  });

  it('should leave selectedShop null when selectedShopId returns null', () => {
    component.getSelectedShop();
    expect(shopServiceSpy.getShopById).not.toHaveBeenCalled();
    expect((component as any).selectedShop).toBeNull();
  });

  // ─── updateItemQuantity ───────────────────────────────────────────────────────

  it('should return immediately for null quantity', () => {
    const item = { ...mockOrderItem, quantity: 3 };
    (component as any).updateItemQuantity(item, null);
    expect(item.quantity).toBe(3); // unchanged
  });

  it('should set quantity directly when within valid range', () => {
    const item = { ...mockOrderItem, quantity: 2 };
    (component as any).updateItemQuantity(item, 5);
    expect(item.quantity).toBe(5);
  });

  it('should clamp quantity to 1 when newQuantity < 1', fakeAsync(() => {
    const item = { ...mockOrderItem, quantity: 3 };
    (component as any).updateItemQuantity(item, 0);
    tick(0); // flush the setTimeout
    expect(item.quantity).toBe(1);
  }));

  it('should clamp quantity to availableUnits when newQuantity exceeds it', fakeAsync(() => {
    const item = { ...mockOrderItem, quantity: 2 };
    (component as any).updateItemQuantity(item, 999);
    tick(0);
    expect(item.quantity).toBe(mockProduct.availableUnits);
  }));

  // ─── formatCategories ─────────────────────────────────────────────────────────

  it('should return empty string for an empty categories array', () => {
    expect(component.formatCategories([])).toBe('');
  });

  it('should join category names with a comma', () => {
    const cats = [{ name: 'Periféricos' }, { name: 'Gaming' }];
    expect(component.formatCategories(cats)).toBe('Periféricos, Gaming');
  });

  // ─── isProductInCart ──────────────────────────────────────────────────────────

  it('should return true when the product is already in the cart', () => {
    expect(component.isProductInCart(mockProduct.id)).toBeTrue();
  });

  it('should return false when the product is not in the cart', () => {
    expect(component.isProductInCart('unknown-id')).toBeFalse();
  });

  // ─── clearCart ────────────────────────────────────────────────────────────────

  it('should call clearUserCartItems and reset foundItems', () => {
    (component as any).clearCart();
    expect(orderServiceSpy.clearUserCartItems).toHaveBeenCalled();
    expect(component.foundItems.items).toEqual([]);
    expect(component.foundItems.totalItems).toBe(0);
  });

  it('should call setItemsCount(0) after clearing the cart', () => {
    (component as any).clearCart();
    expect(orderServiceSpy.setItemsCount).toHaveBeenCalledWith(0);
  });

  // ─── removeItem ───────────────────────────────────────────────────────────────

  it('should call deleteItem and remove the item from foundItems', () => {
    (component as any).removeItem('item-1');
    expect(orderServiceSpy.deleteItem).toHaveBeenCalledWith('item-1');
    expect(component.foundItems.items.some((i: OrderItem) => i.id === 'item-1')).toBeFalse();
  });

  it('should update cartSummary and call setItemsCount after removing an item', () => {
    (component as any).removeItem('item-1');
    expect(orderServiceSpy.setItemsCount).toHaveBeenCalled();
    expect(component.cartSummary.totalItems).toBe(1);
  });

  // ─── removeFavorite ───────────────────────────────────────────────────────────

  it('should call deleteProductFromFavourites and remove the product from foundProducts', () => {
    (component as any).removeFavorite('fav-1');
    expect(productServiceSpy.deleteProductFromFavourites).toHaveBeenCalledWith('fav-1');
    expect(component.foundProducts.items.some((p: Product) => p.id === 'fav-1')).toBeFalse();
  });

  it('should decrement foundProducts.totalItems after removing a favourite', () => {
    const before = component.foundProducts.totalItems;
    (component as any).removeFavorite('fav-1');
    expect(component.foundProducts.totalItems).toBe(before - 1);
  });

  // ─── moveToCart ───────────────────────────────────────────────────────────────

  it('should call addItemToCart and then refresh the cart', () => {
    const prevCartCalls = orderServiceSpy.getUserCartItemsPage.calls.count();
    (component as any).moveToCart('fav-1');
    expect(orderServiceSpy.addItemToCart).toHaveBeenCalledWith('fav-1', 1);
    expect(orderServiceSpy.getUserCartItemsPage.calls.count()).toBeGreaterThan(prevCartCalls);
  });

  // ─── Pagination ───────────────────────────────────────────────────────────────

  it('should update firstItem and itemsRows and reload cart items on page change', () => {
    const prev = orderServiceSpy.getUserCartItemsPage.calls.count();
    component.onCartItemsPageChange({ first: 5, rows: 10, page: 1, pageCount: 2 });
    expect(component.firstItem).toBe(5);
    expect(component.itemsRows).toBe(10);
    expect(orderServiceSpy.getUserCartItemsPage.calls.count()).toBeGreaterThan(prev);
  });

  it('should update firstProduct and productsRows and reload favourites on page change', () => {
    const prev = productServiceSpy.getUserFavouriteProductsPage.calls.count();
    component.onFavouriteProductsPageChange({ first: 5, rows: 10, page: 1, pageCount: 2 });
    expect(component.firstProduct).toBe(5);
    expect(component.productsRows).toBe(10);
    expect(productServiceSpy.getUserFavouriteProductsPage.calls.count()).toBeGreaterThan(prev);
  });

  // ─── reload ───────────────────────────────────────────────────────────────────

  it('should set loading=true, error=false and re-fetch everything on reload()', () => {
    const blocker = new Subject<any>();
    orderServiceSpy.getUserCartSummary.and.returnValue(blocker.asObservable());
    component.error = true;
    component.reload();
    expect(component.loading).toBeTrue();
    expect(component.error).toBeFalse();
  });

  // ─── DOM: loading / error screen ──────────────────────────────────────────────

  it('should show the loading screen when loading=true', () => {
    component.loading = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-loading-screen')).not.toBeNull();
  });

  it('should hide the loading screen and show main content when loaded', () => {
    expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Tu Carrito');
  });

  // ─── DOM: cart items ──────────────────────────────────────────────────────────

  it('should display the product name in the cart', () => {
    expect(fixture.nativeElement.textContent).toContain(mockProduct.name);
  });

  it('should display "Tu carrito está vacío" when foundItems.totalItems is 0', () => {
    component.foundItems = { ...emptyPage };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Tu carrito está vacío');
  });

  // ─── DOM: order summary aside ─────────────────────────────────────────────────

  it('should show GRATIS when totalCost >= 50', () => {
    component.cartSummary = { ...mockCartSummary, totalCost: 60 };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('GRATIS');
  });

  it('should show "5,00€" for shipping when totalCost < 50', () => {
    component.cartSummary = { ...mockCartSummary, totalCost: 30 };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('5,00€');
  });

  it('should show the selected shop name when selectedShop is set', () => {
    (component as any).selectedShop = mockShop;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Tienda Madrid');
  });

  it('should show "Ninguna tienda seleccionada" when selectedShop is null', () => {
    expect(fixture.nativeElement.textContent).toContain('Ninguna tienda seleccionada');
  });

  // ─── DOM: favourite products ──────────────────────────────────────────────────

  it('should display favourite product name', () => {
    expect(fixture.nativeElement.textContent).toContain(mockFavProduct.name);
  });

  it('should show "No tienes productos guardados" when foundProducts.totalItems is 0', () => {
    component.foundProducts = { ...emptyPage };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No tienes productos guardados');
  });
});
