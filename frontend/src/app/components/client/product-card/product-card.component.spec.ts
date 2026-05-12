import {ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {ProductCardComponent} from './product-card.component';
import {ProductService} from '../../../services/product.service';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {of, Subject} from 'rxjs';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {Product} from '../../../models/product.model';
import {ImageInfo} from '../../../models/imageInfo.model';

describe('ProductCardComponent', () => {
  let component: ProductCardComponent;
  let fixture: ComponentFixture<ProductCardComponent>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let routerSpy: jasmine.SpyObj<Router>;

  // ─── Mock data ────────────────────────────────────────────────────────────────

  const mockImageInfo: ImageInfo = {
    id: '1',
    imageUrl: 'http://img.test/product.png',
    s3Key: 'products/product.png',
    fileName: 'product.png'
  };

  const mockProduct: Product = {
    id: '42',
    referenceCode: 'REF-042',
    name: 'Ratón Gaming Pro',
    description: 'Un ratón muy bueno',
    imagesInfo: [mockImageInfo],
    supplyPrice: 20,
    previousPrice: 80,
    currentPrice: 59,
    active: true,
    discount: '-25%',
    categories: [],
    totalUnits: 100,
    availableUnits: 50,
    shopsWithStock: 3,
    averageRating: 4.5,
    totalReviews: 12,
    createdAt: '2026-05-08'
  };

  // ─── Setup ────────────────────────────────────────────────────────────────────

  beforeEach(async () => {
    // ProductService: searchScope is a readonly Signal used by StockTagComponent
    productServiceSpy = jasmine.createSpyObj('ProductService', ['searchScope']);
    productServiceSpy.searchScope.and.returnValue('GLOBAL');

    // Router: events Observable required by RouterLink constructor.
    // navigateByUrl is called by RouterLink on click for non-anchor elements.
    // createUrlTree / serializeUrl are called by RouterLink to build the URL.
    routerSpy = jasmine.createSpyObj(
      'Router',
      ['navigate', 'navigateByUrl', 'createUrlTree', 'serializeUrl'],
      { url: '/', events: new Subject<any>(), navigated: false }
    );
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/product/42');
    routerSpy.navigateByUrl.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [
        ProductCardComponent,
        BrowserAnimationsModule
      ],
      providers: [
        { provide: ProductService, useValue: productServiceSpy },
        { provide: Router,         useValue: routerSpy },
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

    fixture = TestBed.createComponent(ProductCardComponent);
    component = fixture.componentInstance;
    component.product = { ...mockProduct };
    fixture.detectChanges();
  });

  // ─── Creation ─────────────────────────────────────────────────────────────────

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // ─── DOM: product data ────────────────────────────────────────────────────────

  it('should render the product name', () => {
    expect(fixture.nativeElement.textContent).toContain('Ratón Gaming Pro');
  });

  it('should render the formatted integer price (no decimals)', () => {
    component.product = { ...mockProduct, currentPrice: 59 };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('59€');
  });

  it('should render the formatted decimal price with comma separator', () => {
    component.product = { ...mockProduct, currentPrice: 19.99 };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('19,99€');
  });

  it('should render the formatted average rating with one decimal', () => {
    // formatRating uses Spanish locale: 4.5 → '4,5'
    component.product = { ...mockProduct, averageRating: 4.5 };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('4,5');
  });

  it('should render an integer rating without decimals', () => {
    component.product = { ...mockProduct, averageRating: 5 };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('5');
  });

  it('should render the total review count', () => {
    expect(fixture.nativeElement.textContent).toContain('12');
  });

  it('should render the product image with the correct src attribute', () => {
    const img = fixture.debugElement.query(By.css('img'));
    expect(img.nativeElement.getAttribute('src')).toBe(mockImageInfo.imageUrl);
  });

  it('should render the product image with the product name as alt text', () => {
    const img = fixture.debugElement.query(By.css('img'));
    expect(img.nativeElement.getAttribute('alt')).toBe(mockProduct.name);
  });

  // ─── DOM: shops count ─────────────────────────────────────────────────────────

  it('should display the shopsWithStock count', () => {
    expect(fixture.nativeElement.textContent).toContain('3');
  });

  it('should use plural "tiendas" when shopsWithStock > 1', () => {
    component.product = { ...mockProduct, shopsWithStock: 3 };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('tiendas');
  });

  it('should use singular "tienda" when shopsWithStock is 1', () => {
    component.product = { ...mockProduct, shopsWithStock: 1 };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('1 tienda');
    expect(fixture.nativeElement.textContent).not.toContain('tiendas');
  });

  // ─── DOM: discount badge (conditional *ngIf) ──────────────────────────────────

  it('should show the discount badge when discount starts with "-"', () => {
    component.product = { ...mockProduct, discount: '-25%' };
    fixture.detectChanges();
    // The badge span uses class bg-red-100 and displays the discount text
    const badge = fixture.debugElement.query(By.css('.bg-red-100'));
    expect(badge).not.toBeNull();
    expect(badge.nativeElement.textContent.trim()).toBe('-25%');
  });

  it('should NOT show the discount badge when discount does not start with "-"', () => {
    component.product = { ...mockProduct, discount: '0%' };
    fixture.detectChanges();
    const badge = fixture.debugElement.query(By.css('.bg-red-100'));
    expect(badge).toBeNull();
  });

  it('should NOT show the discount badge when discount is an empty string', () => {
    component.product = { ...mockProduct, discount: '' };
    fixture.detectChanges();
    const badge = fixture.debugElement.query(By.css('.bg-red-100'));
    expect(badge).toBeNull();
  });

  // ─── DOM: previous price (conditional *ngIf) ──────────────────────────────────

  it('should show the crossed-out previous price when previousPrice > currentPrice', () => {
    component.product = { ...mockProduct, previousPrice: 80, currentPrice: 59 };
    fixture.detectChanges();
    const strikethrough = fixture.debugElement.query(By.css('.line-through'));
    expect(strikethrough).not.toBeNull();
    expect(strikethrough.nativeElement.textContent).toContain('80€');
  });

  it('should NOT show the crossed-out previous price when previousPrice is 0', () => {
    component.product = { ...mockProduct, previousPrice: 0, currentPrice: 59 };
    fixture.detectChanges();
    const strikethrough = fixture.debugElement.query(By.css('.line-through'));
    expect(strikethrough).toBeNull();
  });

  it('should NOT show the crossed-out previous price when previousPrice equals currentPrice', () => {
    component.product = { ...mockProduct, previousPrice: 59, currentPrice: 59 };
    fixture.detectChanges();
    const strikethrough = fixture.debugElement.query(By.css('.line-through'));
    expect(strikethrough).toBeNull();
  });

  it('should NOT show the crossed-out previous price when previousPrice < currentPrice', () => {
    component.product = { ...mockProduct, previousPrice: 40, currentPrice: 59 };
    fixture.detectChanges();
    const strikethrough = fixture.debugElement.query(By.css('.line-through'));
    expect(strikethrough).toBeNull();
  });

  // ─── Input: elementId ─────────────────────────────────────────────────────────

  it('should apply elementId as the id attribute on the card element', () => {
    component.elementId = 'card-test-99';
    fixture.detectChanges();
    const card = fixture.debugElement.query(By.css('#card-test-99'));
    expect(card).not.toBeNull();
  });

  it('should default elementId to "product"', () => {
    const freshFixture = TestBed.createComponent(ProductCardComponent);
    freshFixture.componentInstance.product = { ...mockProduct };
    freshFixture.detectChanges();
    expect(freshFixture.componentInstance.elementId).toBe('product');
    const card = freshFixture.debugElement.query(By.css('#product'));
    expect(card).not.toBeNull();
  });

  // ─── Input: navState ──────────────────────────────────────────────────────────

  it('should accept a custom navState object as input', () => {
    const state = { from: 'search', categoryId: '5' };
    component.navState = state;
    fixture.detectChanges();
    expect(component.navState).toEqual(state);
  });

  it('should default navState to an empty object', () => {
    const freshFixture = TestBed.createComponent(ProductCardComponent);
    freshFixture.componentInstance.product = { ...mockProduct };
    freshFixture.detectChanges();
    expect(freshFixture.componentInstance.navState).toEqual({});
  });

  // ─── RouterLink: navigation target ───────────────────────────────────────────

  it('should attach the RouterLink directive to the card element', () => {
    // Verifies that the [routerLink] directive is present in the template
    const linkEl = fixture.debugElement.query(By.directive(RouterLink));
    expect(linkEl).not.toBeNull();
  });

  it('should call router.navigateByUrl when the card is clicked', () => {
    // For <div [routerLink]>, Angular's RouterLink uses navigateByUrl on click
    // (not router.navigate, which is for <a> elements computing href)
    const linkEl = fixture.debugElement.query(By.directive(RouterLink));
    linkEl.triggerEventHandler('click', { button: 0, ctrlKey: false, shiftKey: false, altKey: false, metaKey: false });
    expect(routerSpy.navigateByUrl).toHaveBeenCalled();
  });
});
