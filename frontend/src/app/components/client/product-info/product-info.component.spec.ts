import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductInfoComponent } from './product-info.component';
import { ProductService } from '../../../services/product.service';
import { CategoryService } from '../../../services/category.service';
import { ReviewService } from '../../../services/review.service';
import { AuthService } from '../../../services/auth.service';
import { OrderService } from '../../../services/order.service';
import { ShopService } from '../../../services/shop.service';
import { BreadcrumbService } from '../../../utils/breadcrumb.service';
import { RegistryService } from '../../../services/registry.service';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { of, Subject, throwError } from 'rxjs';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { LOCALE_ID } from '@angular/core';
import { Product } from '../../../models/product.model';
import { Category } from '../../../models/category.model';
import { Review } from '../../../models/review.model';
import { ImageInfo } from '../../../models/imageInfo.model';
import { LoginInfo } from '../../../models/loginInfo.model';

describe('ProductInfoComponent', () => {
  let component: ProductInfoComponent;
  let fixture: ComponentFixture<ProductInfoComponent>;

  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let categoryServiceSpy: jasmine.SpyObj<CategoryService>;
  let reviewServiceSpy: jasmine.SpyObj<ReviewService>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let shopServiceSpy: jasmine.SpyObj<ShopService>;
  let authServiceMock: {
    isLogged: jasmine.Spy;
    isUser: jasmine.Spy;
    isAdmin: jasmine.Spy;
    isManager: jasmine.Spy;
    isDriver: jasmine.Spy;
    selectedShopId: jasmine.Spy;
    hasShopSelected: jasmine.Spy;
    getLoginInfo: jasmine.Spy;
  };
  let routerSpy: jasmine.SpyObj<Router>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  // Typed as any: BreadcrumbService exposes a computed signal (breadcrumbs)
  // that must be mocked as a callable spy, incompatible with SpyObj typing
  let breadcrumbServiceSpy: any;
  let registryServiceSpy: jasmine.SpyObj<RegistryService>;

  // ─── Mock data ───────────────────────────────────────────────────────────────

  const mockImageInfo: ImageInfo = {
    id: '1',
    imageUrl: 'http://img.test/img.png',
    s3Key: 'products/img.png',
    fileName: 'img.png'
  };

  const mockCategory: Category = {
    id: '10',
    name: 'Electrónica',
    icon: 'pi pi-bolt',
    bannerText: '',
    shortDescription: '',
    longDescription: '',
    imageInfo: mockImageInfo,
    timesUsed: 5,
    parentId: '',
    children: []
  };

  const mockProduct: Product = {
    id: '1',
    referenceCode: 'REF-001',
    name: 'Portátil Test',
    description: '<p>Descripción del producto</p>',
    imagesInfo: [mockImageInfo],
    supplyPrice: 50,
    previousPrice: 120,
    currentPrice: 99,
    active: true,
    discount: '10%',
    categories: [mockCategory],
    totalUnits: 50,
    availableUnits: 10,
    shopsWithStock: 3,
    averageRating: 4.5,
    totalReviews: 0,
    createdAt: '2026-05-08'
  };

  const mockLoginInfo: LoginInfo = {
    isLogged: true,
    imageUrl: 'http://user.img',
    id: 'user-1',
    name: 'Test User',
    username: 'testuser',
    roles: ['USER'],
    selectedShopId: null
  };

  const mockReview: Review = {
    id: 'rev-1',
    productId: '1',
    creatorId: 'user-2',
    productName: 'Portátil Test',
    creatorUsername: 'otheruser',
    creatorName: 'Other User',
    creatorImage: 'http://other.img',
    creatorConnection: null,
    text: 'Muy buen producto',
    rating: 5,
    createdAt: '2025-01-01',
    recommended: true
  };

  // ─── Setup ───────────────────────────────────────────────────────────────────

  beforeEach(async () => {
    // Ensure history.state is a plain object so loadProduct does not throw
    history.replaceState({}, '');

    productServiceSpy = jasmine.createSpyObj('ProductService', [
      'getProductById',
      'getProductsByCategoryName',
      'getRecommendedProducts',
      'getStockByProductId',
      'checkInFavourites',
      'addProductToFavourites',
      'deleteProductFromFavourites',
      // searchScope is a readonly Signal used by StockTagComponent — mock as a callable spy
      'searchScope'
    ]);

    categoryServiceSpy = jasmine.createSpyObj('CategoryService', ['getCategoryById']);

    reviewServiceSpy = jasmine.createSpyObj('ReviewService', [
      'getReviewsByProductId',
      'submitReview',
      'deleteReviewById'
    ]);

    orderServiceSpy = jasmine.createSpyObj('OrderService', [
      'getCartItemByProductId',
      'addItemToCart',
      'incrementItemsCount'
    ]);

    shopServiceSpy = jasmine.createSpyObj('ShopService', ['getShopById']);

    // Router spy: must expose `events` as an Observable so that RouterLink
    // (used inside LoadingScreenComponent) can subscribe in its constructor.
    // `createUrlTree` / `serializeUrl` are called by RouterLink's href getter.
    routerSpy = jasmine.createSpyObj(
      'Router',
      ['navigate', 'createUrlTree', 'serializeUrl'],
      { url: '/product/1', events: new Subject<any>(), navigated: false }
    );
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    // BreadcrumbService mock: the template of BreadcrumbReloadComponent calls
    // `breadcrumbService.breadcrumbs()` (computed signal) and the component calls
    // `setBaseBreadcrumbs` in ngOnInit, so both must be present.
    breadcrumbServiceSpy = jasmine.createSpyObj('BreadcrumbService', [
      'setNodesForUrl',
      'insertPenultimateNodesForUrl',
      'setBaseBreadcrumbs',
      'breadcrumbs'
    ]);
    breadcrumbServiceSpy.breadcrumbs.and.returnValue([]);
    registryServiceSpy = jasmine.createSpyObj('RegistryService', ['loadPublicRegistry']);

    // AuthService: computed signals are mocked as plain jasmine spy functions
    authServiceMock = {
      isLogged:       jasmine.createSpy('isLogged').and.returnValue(false),
      isUser:         jasmine.createSpy('isUser').and.returnValue(false),
      isAdmin:        jasmine.createSpy('isAdmin').and.returnValue(false),
      isManager:      jasmine.createSpy('isManager').and.returnValue(false),
      isDriver:       jasmine.createSpy('isDriver').and.returnValue(false),
      selectedShopId: jasmine.createSpy('selectedShopId').and.returnValue(null),
      hasShopSelected:jasmine.createSpy('hasShopSelected').and.returnValue(false),
      getLoginInfo:   jasmine.createSpy('getLoginInfo').and.returnValue(of(mockLoginInfo))
    };

    // Default happy-path responses shared across all tests
    productServiceSpy.searchScope.and.returnValue('GLOBAL');
    productServiceSpy.getProductById.and.returnValue(of({ ...mockProduct }));
    categoryServiceSpy.getCategoryById.and.returnValue(of({ ...mockCategory }));
    productServiceSpy.getProductsByCategoryName.and.returnValue(
      of({ items: [], totalItems: 0, currentPage: 0, lastPage: 0, pageSize: 8 })
    );
    productServiceSpy.getRecommendedProducts.and.returnValue(
      of({ items: [], totalItems: 0, currentPage: 0, lastPage: 0, pageSize: 10 })
    );
    productServiceSpy.getStockByProductId.and.returnValue(of([]));
    productServiceSpy.checkInFavourites.and.returnValue(of(false));
    orderServiceSpy.getCartItemByProductId.and.returnValue(of(null));
    orderServiceSpy.addItemToCart.and.returnValue(of({} as any));
    reviewServiceSpy.getReviewsByProductId.and.returnValue(of([]));
    registryServiceSpy.loadPublicRegistry.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [
        ProductInfoComponent,
        BrowserAnimationsModule
      ],
      providers: [
        { provide: ProductService,    useValue: productServiceSpy },
        { provide: CategoryService,   useValue: categoryServiceSpy },
        { provide: ReviewService,     useValue: reviewServiceSpy },
        { provide: OrderService,      useValue: orderServiceSpy },
        { provide: ShopService,       useValue: shopServiceSpy },
        { provide: AuthService,       useValue: authServiceMock },
        { provide: Router,            useValue: routerSpy },
        { provide: MessageService,    useValue: messageServiceSpy },
        { provide: BreadcrumbService, useValue: breadcrumbServiceSpy },
        { provide: RegistryService,   useValue: registryServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ id: '1' }),
            snapshot: {
              paramMap: { get: (key: string) => key === 'id' ? '1' : null },
              // Required by BreadcrumbReloadComponent.createBreadcrumbs
              url: [],
              data: {}
            },
            // BreadcrumbReloadComponent traverses route.root.children;
            // an empty array makes it return immediately without crashing.
            root: { children: [] }
          }
        },
        { provide: LOCALE_ID, useValue: 'en-US' },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductInfoComponent);
    component = fixture.componentInstance;
  });

  // ─── Creation & initial state ─────────────────────────────────────────────────

  it('should create the component', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should have loading=true and error=false before the first change-detection cycle', () => {
    expect((component as any).loading).toBeTrue();
    expect((component as any).error).toBeFalse();
  });

  it('should keep loading=true while the product request is pending', () => {
    const pending$ = new Subject<Product>();
    productServiceSpy.getProductById.and.returnValue(pending$);

    fixture.detectChanges();

    expect((component as any).loading).toBeTrue();
  });

  // ─── Happy-path: product loading ─────────────────────────────────────────────

  it('should set loading=false after a successful product load', () => {
    fixture.detectChanges();
    expect((component as any).loading).toBeFalse();
  });

  it('should render the product name and reference code in the DOM', () => {
    fixture.detectChanges();

    const html = (fixture.nativeElement as HTMLElement).textContent!;
    expect(html).toContain('Portátil Test');
    expect(html).toContain('REF-001');
  });

  it('should render the current price in the DOM', () => {
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('99');
  });

  it('should map imagesInfo to the gallery array', () => {
    fixture.detectChanges();

    const images: any[] = (component as any).images;
    expect(images.length).toBe(1);
    expect(images[0].itemImageSrc).toBe(mockImageInfo.imageUrl);
  });

  it('should produce an empty gallery array when product has no images', () => {
    productServiceSpy.getProductById.and.returnValue(
      of({ ...mockProduct, imagesInfo: [] })
    );

    fixture.detectChanges();

    expect((component as any).images.length).toBe(0);
  });

  it('should update the breadcrumb with the product name', () => {
    fixture.detectChanges();

    expect(breadcrumbServiceSpy.setNodesForUrl).toHaveBeenCalledWith(
      '/product/1',
      [{ label: 'Portátil Test' }]
    );
  });

  it('should call getStockByProductId with the product id', () => {
    fixture.detectChanges();
    expect(productServiceSpy.getStockByProductId).toHaveBeenCalledWith('1');
  });

  it('should call checkInFavourites with the product id', () => {
    fixture.detectChanges();
    expect(productServiceSpy.checkInFavourites).toHaveBeenCalledWith('1');
  });

  it('should call loadPublicRegistry twice (today views + chart data)', () => {
    fixture.detectChanges();
    expect(registryServiceSpy.loadPublicRegistry).toHaveBeenCalledTimes(2);
  });

  // ─── Error states ─────────────────────────────────────────────────────────────

  it('should set error=true and loading=false when getProductById fails', () => {
    productServiceSpy.getProductById.and.returnValue(
      throwError(() => new Error('500 Server Error'))
    );

    fixture.detectChanges();

    expect((component as any).loading).toBeFalse();
    expect((component as any).error).toBeTrue();
  });

  it('should set error=true and loading=false when getCategoryById fails', () => {
    categoryServiceSpy.getCategoryById.and.returnValue(
      throwError(() => new Error('404 Not Found'))
    );

    fixture.detectChanges();

    expect((component as any).loading).toBeFalse();
    expect((component as any).error).toBeTrue();
  });

  it('should set relatedError=true and relatedLoading=false when getRecommendedProducts fails', () => {
    productServiceSpy.getRecommendedProducts.and.returnValue(
      throwError(() => new Error('Network Error'))
    );

    (component as any).loadRelatedProducts();

    expect((component as any).relatedLoading).toBeFalse();
    expect((component as any).relatedError).toBeTrue();
  });

  // ─── Related products ─────────────────────────────────────────────────────────

  it('should exclude the current product from the related products list', () => {
    productServiceSpy.getRecommendedProducts.and.returnValue(of({
      items: [
        { ...mockProduct, id: '2', name: 'Portátil B' },
        { ...mockProduct, id: '3', name: 'Portátil C' },
        { ...mockProduct, id: '1', name: 'Portátil Test' }  // must be filtered out
      ],
      totalItems: 3, currentPage: 0, lastPage: 0, pageSize: 10
    }));

    (component as any).loadRelatedProducts();

    const related: Product[] = (component as any).relatedProducts;
    expect(related.length).toBe(2);
    expect(related.find(p => p.id === '1')).toBeUndefined();
  });

  // ─── Review statistics ────────────────────────────────────────────────────────

  it('should compute totalReviews, average rating and star distribution correctly', () => {
    const reviews: Review[] = [
      { ...mockReview, id: 'r1', creatorId: 'u2', rating: 5, recommended: true },
      { ...mockReview, id: 'r2', creatorId: 'u3', rating: 4, recommended: true },
      { ...mockReview, id: 'r3', creatorId: 'u4', rating: 3, recommended: false }
    ];
    reviewServiceSpy.getReviewsByProductId.and.returnValue(of(reviews));

    fixture.detectChanges();

    expect((component as any).product.totalReviews).toBe(3);
    expect((component as any).product.averageRating).toBeCloseTo(4, 1);
    expect((component as any).recommendedCount).toBe(2);
    expect((component as any).recommendationPercentage).toBeCloseTo(66.67, 1);

    const star5 = (component as any).stars.find((s: any) => s.value === 5);
    const star3 = (component as any).stars.find((s: any) => s.value === 3);
    expect(star5.count).toBe(1);
    expect(star3.count).toBe(1);
  });

  it('should set userReviewed=true when the logged user already has a review', () => {
    reviewServiceSpy.getReviewsByProductId.and.returnValue(
      of([{ ...mockReview, creatorId: 'user-1' }])  // same id as mockLoginInfo.id
    );

    fixture.detectChanges();

    expect((component as any).userReviewed).toBeTrue();
  });

  it('should leave userReviewed=false when none of the reviews belong to the logged user', () => {
    reviewServiceSpy.getReviewsByProductId.and.returnValue(
      of([{ ...mockReview, creatorId: 'another-user' }])
    );

    fixture.detectChanges();

    expect((component as any).userReviewed).toBeFalse();
  });

  // ─── Favourites ───────────────────────────────────────────────────────────────

  it('should set inFavourites=true when checkInFavourites returns true', () => {
    productServiceSpy.checkInFavourites.and.returnValue(of(true));

    fixture.detectChanges();

    expect((component as any).inFavourites).toBeTrue();
  });

  it('should add the product to favourites and set inFavourites=true', () => {
    authServiceMock.isLogged.and.returnValue(true);
    productServiceSpy.addProductToFavourites.and.returnValue(of({ ...mockProduct }));

    fixture.detectChanges();
    (component as any).inFavourites = false;
    (component as any).toggleInFavourites();

    expect(productServiceSpy.addProductToFavourites).toHaveBeenCalledWith('1');
    expect((component as any).inFavourites).toBeTrue();
    expect(messageServiceSpy.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'info' })
    );
  });

  it('should remove the product from favourites and set inFavourites=false', () => {
    authServiceMock.isLogged.and.returnValue(true);
    productServiceSpy.checkInFavourites.and.returnValue(of(true));
    productServiceSpy.deleteProductFromFavourites.and.returnValue(of(undefined as any));

    fixture.detectChanges();
    expect((component as any).inFavourites).toBeTrue();

    (component as any).toggleInFavourites();

    expect(productServiceSpy.deleteProductFromFavourites).toHaveBeenCalledWith('1');
    expect((component as any).inFavourites).toBeFalse();
  });

  it('should redirect to /login when toggleInFavourites is called without being logged in', () => {
    authServiceMock.isLogged.and.returnValue(false);
    productServiceSpy.addProductToFavourites.and.returnValue(of({ ...mockProduct }));

    fixture.detectChanges();
    (component as any).inFavourites = false;
    (component as any).toggleInFavourites();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  // ─── Add to cart ──────────────────────────────────────────────────────────────

  it('should redirect to /login when addItemToCart is called without being logged in', () => {
    authServiceMock.isLogged.and.returnValue(false);

    fixture.detectChanges();
    (component as any).addItemToCart();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should show an info message when addItemToCart is called with quantity 0', () => {
    authServiceMock.isLogged.and.returnValue(true);

    fixture.detectChanges();
    (component as any).quantity = 0;
    (component as any).addItemToCart();

    expect(orderServiceSpy.addItemToCart).not.toHaveBeenCalled();
    expect(messageServiceSpy.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'info', summary: 'Cantidad no válida' })
    );
  });

  it('should call addItemToCart service and show a success message on success', () => {
    authServiceMock.isLogged.and.returnValue(true);
    authServiceMock.isUser.and.returnValue(true);
    orderServiceSpy.addItemToCart.and.returnValue(of({ id: 'item-1', quantity: 2 } as any));

    fixture.detectChanges();
    (component as any).quantity = 2;
    (component as any).addItemToCart();

    expect(orderServiceSpy.addItemToCart).toHaveBeenCalledWith('1', 2);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'success' })
    );
  });

  it('should show a "no stock" error message when addItemToCart returns HTTP 405', () => {
    authServiceMock.isLogged.and.returnValue(true);
    authServiceMock.isUser.and.returnValue(true);
    orderServiceSpy.addItemToCart.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 405 }))
    );

    fixture.detectChanges();
    (component as any).quantity = 1;
    (component as any).addItemToCart();

    expect(messageServiceSpy.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'error', detail: 'No hay suficiente stock disponible' })
    );
  });

  it('should show a generic error message when addItemToCart fails with a non-405 status', () => {
    authServiceMock.isLogged.and.returnValue(true);
    authServiceMock.isUser.and.returnValue(true);
    orderServiceSpy.addItemToCart.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );

    fixture.detectChanges();
    (component as any).quantity = 1;
    (component as any).addItemToCart();

    expect(messageServiceSpy.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'error', detail: 'Error añadiendo el producto al carrito' })
    );
  });

  // ─── Submit / edit / delete reviews ──────────────────────────────────────────

  it('should call submitReview service with productId and creatorId, then set userReviewed=true', () => {
    const submittedReview = { ...mockReview, id: 'rev-new', creatorId: 'user-1' };
    reviewServiceSpy.submitReview.and.returnValue(of(submittedReview));

    fixture.detectChanges(); // initial load — getReviewsByProductId returns of([]) by default

    // submitReview triggers loadReviews internally, which recomputes userReviewed from the
    // reviews list. Reconfigure the spy to return the submitted review so the recomputation
    // sees a review owned by the logged user (creatorId === 'user-1').
    reviewServiceSpy.getReviewsByProductId.and.returnValue(of([submittedReview]));

    (component as any).loggedUserInfo = mockLoginInfo;
    (component as any).newReview = { rating: 5, recommended: true, text: 'Excelente' };
    (component as any).submitReview();

    expect(reviewServiceSpy.submitReview).toHaveBeenCalledWith(
      jasmine.objectContaining({ productId: '1', creatorId: 'user-1' })
    );
    expect((component as any).userReviewed).toBeTrue();
  });

  it('should call deleteReviewById service, reset newReview and set userReviewed=false', () => {
    reviewServiceSpy.deleteReviewById.and.returnValue(of(mockReview));

    fixture.detectChanges();
    (component as any).deleteReview('rev-1');

    expect(reviewServiceSpy.deleteReviewById).toHaveBeenCalledWith('rev-1');
    expect((component as any).newReview).toEqual({});
    expect((component as any).userReviewed).toBeFalse();
  });

  it('should move the selected review into newReview and set userReviewed=false on editReview', () => {
    const editableReview: Review = { ...mockReview, id: 'rev-edit', creatorId: 'user-1' };

    fixture.detectChanges();
    (component as any).productReviews = [editableReview];
    (component as any).userReviewed = true;
    (component as any).editReview('rev-edit');

    expect((component as any).userReviewed).toBeFalse();
    expect((component as any).newReview).toEqual(jasmine.objectContaining({ id: 'rev-edit' }));
    expect(
      (component as any).productReviews.find((r: Review) => r.id === 'rev-edit')
    ).toBeUndefined();
  });

  // ─── setRecommendation ────────────────────────────────────────────────────────

  it('should set newReview.recommended=true when setRecommendation(true) is called', () => {
    fixture.detectChanges();
    (component as any).newReview = { rating: 4, recommended: false };
    (component as any).setRecommendation(true);

    expect((component as any).newReview.recommended).toBeTrue();
  });

  it('should set newReview.recommended=false when setRecommendation(false) is called', () => {
    fixture.detectChanges();
    (component as any).newReview = { rating: 4, recommended: true };
    (component as any).setRecommendation(false);

    expect((component as any).newReview.recommended).toBeFalse();
  });
});
