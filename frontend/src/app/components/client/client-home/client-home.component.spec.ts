

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ClientHomeComponent } from './client-home.component';
import { ProductService } from '../../../services/product.service';
import { CategoryService } from '../../../services/category.service';
import { BreadcrumbService } from '../../../utils/breadcrumb.service';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Category } from '../../../models/category.model';
import { Product } from '../../../models/product.model';
import { PageResponse } from '../../../models/pageResponse.model';
import { ImageInfo } from '../../../models/imageInfo.model';

describe('ClientHomeComponent', () => {
  let component: ClientHomeComponent;
  let fixture: ComponentFixture<ClientHomeComponent>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let categoryServiceSpy: jasmine.SpyObj<CategoryService>;
  let breadcrumbServiceSpy: jasmine.SpyObj<BreadcrumbService>;
  let routerSpy: jasmine.SpyObj<Router>;

  // ─── Mock data ────────────────────────────────────────────────────────────────

  const mockImageInfo: ImageInfo = { id: 'img-1', imageUrl: 'http://img.test/p.png', s3Key: 'key', fileName: 'p.png' };

  const mockCategories: Category[] = [
    { id: 'cat-featured',    name: 'Destacado',   icon: 'pi pi-star',      bannerText: 'Top',      shortDescription: '', longDescription: '', imageInfo: mockImageInfo, timesUsed: 5, parentId: '', children: [] },
    { id: 'cat-recommended', name: 'Recomendado', icon: 'pi pi-thumbs-up', bannerText: 'Para ti',  shortDescription: '', longDescription: '', imageInfo: mockImageInfo, timesUsed: 3, parentId: '', children: [] },
    { id: 'cat-topsales',    name: 'Top Ventas',  icon: 'pi pi-chart-bar', bannerText: 'Trending', shortDescription: '', longDescription: '', imageInfo: mockImageInfo, timesUsed: 8, parentId: '', children: [] },
    { id: 'cat-peripherals', name: 'Periféricos', icon: 'pi pi-desktop',   bannerText: 'Gaming',   shortDescription: '', longDescription: '', imageInfo: mockImageInfo, timesUsed: 6, parentId: '', children: [] },
    { id: 'cat-computers',   name: 'Ordenadores', icon: 'pi pi-server',    bannerText: 'Potencia', shortDescription: '', longDescription: '', imageInfo: mockImageInfo, timesUsed: 4, parentId: '', children: [] }
  ];

  const mockProduct: Product = {
    id: 'prod-1', referenceCode: 'REF-001', name: 'Ratón Gaming Pro',
    description: 'Un gran ratón', imagesInfo: [mockImageInfo],
    supplyPrice: 20, previousPrice: 80, currentPrice: 59, active: true, discount: '-25%',
    categories: [], totalUnits: 100, availableUnits: 50, shopsWithStock: 3,
    averageRating: 4.5, totalReviews: 12, createdAt: '2026-05-08'
  };

  const mockProductPage: PageResponse<Product> = {
    items: [mockProduct], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 10
  };

  const emptyProductPage: PageResponse<Product> = {
    items: [], totalItems: 0, currentPage: 0, lastPage: 0, pageSize: 10
  };

  // ─── Setup ────────────────────────────────────────────────────────────────────

  beforeEach(async () => {
    productServiceSpy = jasmine.createSpyObj('ProductService', [
      'getProductsByCategoryName', 'getRecommendedProducts', 'searchScope'
    ]);
    productServiceSpy.searchScope.and.returnValue('GLOBAL');
    productServiceSpy.getProductsByCategoryName.and.returnValue(of(mockProductPage));
    productServiceSpy.getRecommendedProducts.and.returnValue(of(mockProductPage));

    categoryServiceSpy = jasmine.createSpyObj('CategoryService', ['getAllCategories']);
    categoryServiceSpy.getAllCategories.and.returnValue(of(mockCategories));

    breadcrumbServiceSpy = jasmine.createSpyObj('BreadcrumbService', [
      'setBaseBreadcrumbs', 'insertPenultimateNodesForUrl', 'breadcrumbs'
    ]);
    breadcrumbServiceSpy.breadcrumbs.and.returnValue([]);

    routerSpy = jasmine.createSpyObj(
      'Router',
      ['navigate', 'navigateByUrl', 'createUrlTree', 'serializeUrl'],
      { url: '/', events: new Subject<any>(), navigated: false }
    );
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/search');
    routerSpy.navigateByUrl.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [
        ClientHomeComponent,
        BrowserAnimationsModule
      ],
      providers: [
        { provide: ProductService,    useValue: productServiceSpy },
        { provide: CategoryService,   useValue: categoryServiceSpy },
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

    fixture = TestBed.createComponent(ClientHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ─── Creation ─────────────────────────────────────────────────────────────────

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // ─── services[] — static data ─────────────────────────────────────────────────

  it('should expose 4 service items', () => {
    expect(component.services.length).toBe(4);
  });

  it('should include "Envío Gratis" in the services list', () => {
    expect(component.services.some(s => s.title === 'Envío Gratis')).toBeTrue();
  });

  it('should include "Soporte 24/7" in the services list', () => {
    expect(component.services.some(s => s.title === 'Soporte 24/7')).toBeTrue();
  });

  // ─── Load chain on init ───────────────────────────────────────────────────────

  it('should call getAllCategories on init', () => {
    expect(categoryServiceSpy.getAllCategories).toHaveBeenCalled();
  });

  it('should call getProductsByCategoryName with "Destacado"', () => {
    expect(productServiceSpy.getProductsByCategoryName).toHaveBeenCalledWith('Destacado');
  });

  it('should call getRecommendedProducts on init', () => {
    expect(productServiceSpy.getRecommendedProducts).toHaveBeenCalledWith(8);
  });

  it('should call getProductsByCategoryName with "Top Ventas"', () => {
    expect(productServiceSpy.getProductsByCategoryName).toHaveBeenCalledWith('Top Ventas');
  });

  // ─── State after happy-path load ──────────────────────────────────────────────

  it('should populate the categories array after load', () => {
    expect(component.categories).toEqual(mockCategories);
  });

  it('should populate featuredProducts after load', () => {
    expect(component.featuredProducts).toEqual([mockProduct]);
  });

  it('should populate recommendedProducts after load', () => {
    expect(component.recommendedProducts).toEqual([mockProduct]);
  });

  it('should populate topSalesProducts after load', () => {
    expect(component.topSalesProducts).toEqual([mockProduct]);
  });

  it('should set featuredLoading=false after successful load', () => {
    expect(component.featuredLoading).toBeFalse();
  });

  it('should set recommendedLoading=false after successful load', () => {
    expect(component.recommendedLoading).toBeFalse();
  });

  it('should set topSalesLoading=false after successful load', () => {
    expect(component.topSalesLoading).toBeFalse();
  });

  // ─── Category ID resolution ───────────────────────────────────────────────────

  it('should set featuredCategoryId from the "Destacado" category', () => {
    expect(component.featuredCategoryId).toBe('cat-featured');
  });

  it('should set topSalesCategoryId from the "Top Ventas" category', () => {
    expect(component.topSalesCategoryId).toBe('cat-topsales');
  });

  it('should set peripheralsCategoryId from the "Periféricos" category', () => {
    expect(component.peripheralsCategoryId).toBe('cat-peripherals');
  });

  it('should fall back to "0" when a category name is not found', () => {
    categoryServiceSpy.getAllCategories.and.returnValue(of([
      { id: 'cat-other', name: 'Ordenadores', icon: '', bannerText: '', shortDescription: '',
        longDescription: '', imageInfo: mockImageInfo, timesUsed: 1, parentId: '', children: [] }
    ]));
    (component as any).loadCategories();
    expect(component.featuredCategoryId).toBe('0');
    expect(component.topSalesCategoryId).toBe('0');
    expect(component.peripheralsCategoryId).toBe('0');
  });

  // ─── Error states ─────────────────────────────────────────────────────────────

  it('should set featuredError=true and featuredLoading=false when featured products fail', () => {
    productServiceSpy.getProductsByCategoryName.and.callFake((name: string) =>
      name === 'Destacado' ? throwError(() => new Error('500')) : of(mockProductPage)
    );
    (component as any).loadFeaturedProducts();
    expect(component.featuredError).toBeTrue();
    expect(component.featuredLoading).toBeFalse();
  });

  it('should set recommendedError=true and recommendedLoading=false when recommended products fail', () => {
    productServiceSpy.getRecommendedProducts.and.returnValue(throwError(() => new Error('500')));
    (component as any).loadRecommendedProducts();
    expect(component.recommendedError).toBeTrue();
    expect(component.recommendedLoading).toBeFalse();
  });

  it('should set topSalesError=true and topSalesLoading=false when top-sales products fail', () => {
    productServiceSpy.getProductsByCategoryName.and.callFake((name: string) =>
      name === 'Top Ventas' ? throwError(() => new Error('500')) : of(mockProductPage)
    );
    (component as any).loadTopSalesProducts();
    expect(component.topSalesError).toBeTrue();
    expect(component.topSalesLoading).toBeFalse();
  });

  // ─── DOM: static content ──────────────────────────────────────────────────────

  it('should render the hero heading', () => {
    expect(fixture.nativeElement.textContent).toContain('Tecnología que');
  });

  it('should render all 4 service card titles', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Envío Gratis');
    expect(text).toContain('Garantía 3 años');
    expect(text).toContain('Devoluciones');
    expect(text).toContain('Soporte 24/7');
  });

  it('should render the featured products section heading', () => {
    expect(fixture.nativeElement.textContent).toContain('Destacados de la semana');
  });

  it('should render the recommended products section heading', () => {
    expect(fixture.nativeElement.textContent).toContain('Recomendado');
  });

  it('should render the top-sales products section heading', () => {
    expect(fixture.nativeElement.textContent).toContain('Los más vendidos');
  });

  // ─── DOM: categories grid ─────────────────────────────────────────────────────

  it('should render all category names in the grid', () => {
    const text = fixture.nativeElement.textContent;
    mockCategories.forEach(cat => expect(text).toContain(cat.name));
  });

  // ─── DOM: products ────────────────────────────────────────────────────────────

  it('should display the product name when products are loaded', () => {
    expect(fixture.nativeElement.textContent).toContain(mockProduct.name);
  });

  it('should show the empty-state message when featuredProducts is empty', () => {
    component.featuredProducts = [];
    component.featuredLoading = false;
    component.featuredError = false;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No hay productos destacados disponibles');
  });

  it('should show the empty-state message when recommendedProducts is empty', () => {
    component.recommendedProducts = [];
    component.recommendedLoading = false;
    component.recommendedError = false;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No hay productos recomendados disponibles');
  });

  it('should show the empty-state message when topSalesProducts is empty', () => {
    component.topSalesProducts = [];
    component.topSalesLoading = false;
    component.topSalesError = false;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No hay productos top ventas disponibles');
  });

  // ─── reload() ─────────────────────────────────────────────────────────────────

  it('should reset all products, errors and loading flags on reload()', () => {
    // Use a Subject that never emits so the chain pauses after the synchronous
    // reset and before the services re-populate the arrays.
    const blocker = new Subject<Category[]>();
    categoryServiceSpy.getAllCategories.and.returnValue(blocker.asObservable());

    component.featuredError = true;
    component.recommendedError = true;
    component.topSalesError = true;
    component.featuredProducts = [mockProduct];
    component.recommendedProducts = [mockProduct];
    component.topSalesProducts = [mockProduct];

    component.reload();

    // State is now frozen between the reset and the pending observable
    expect(component.featuredError).toBeFalse();
    expect(component.recommendedError).toBeFalse();
    expect(component.topSalesError).toBeFalse();
    expect(component.featuredLoading).toBeTrue();
    expect(component.recommendedLoading).toBeTrue();
    expect(component.topSalesLoading).toBeTrue();
    expect(component.featuredProducts).toEqual([]);
    expect(component.recommendedProducts).toEqual([]);
    expect(component.topSalesProducts).toEqual([]);
  });

  it('should call getAllCategories again on reload()', () => {
    const prevCount = categoryServiceSpy.getAllCategories.calls.count();
    component.reload();
    expect(categoryServiceSpy.getAllCategories.calls.count()).toBeGreaterThan(prevCount);
  });
});
