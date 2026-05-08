
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { CategoryInfoComponent } from './category-info.component';
import { CategoryService } from '../../../services/category.service';
import { ProductService } from '../../../services/product.service';
import { BreadcrumbService } from '../../../utils/breadcrumb.service';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Category } from '../../../models/category.model';
import { Product } from '../../../models/product.model';
import { ImageInfo } from '../../../models/imageInfo.model';
import { PageResponse } from '../../../models/pageResponse.model';

describe('CategoryInfoComponent', () => {
  let component: CategoryInfoComponent;
  let fixture: ComponentFixture<CategoryInfoComponent>;
  let categoryServiceSpy: jasmine.SpyObj<CategoryService>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let breadcrumbServiceSpy: jasmine.SpyObj<BreadcrumbService>;
  let routerSpy: jasmine.SpyObj<Router>;

  // ─── Mock data ────────────────────────────────────────────────────────────────

  const mockImageInfo: ImageInfo = {
    id: 'img-1', imageUrl: 'http://img.test/cat.png', s3Key: 'key', fileName: 'cat.png'
  };

  const mockSubCat1: Category = {
    id: 'sub-1', name: 'Subcategoría A', icon: 'pi pi-tag', bannerText: '',
    shortDescription: 'Sub A', longDescription: '', imageInfo: mockImageInfo,
    timesUsed: 2, parentId: 'cat-main', children: []
  };
  const mockSubCat2: Category = {
    id: 'sub-2', name: 'Subcategoría B', icon: 'pi pi-tag', bannerText: '',
    shortDescription: 'Sub B', longDescription: '', imageInfo: mockImageInfo,
    timesUsed: 1, parentId: 'cat-main', children: []
  };

  const mockSiblingCat: Category = {
    id: 'sibling-1', name: 'Grabadoras', icon: 'pi pi-video', bannerText: 'Grab',
    shortDescription: 'Descripción Grabadoras', longDescription: '', imageInfo: mockImageInfo,
    timesUsed: 3, parentId: 'cat-parent', children: []
  };

  // Default: mainCategory has a parentId → uses getCategoryById(parentId) for siblings
  const mockMainCategory: Category = {
    id: 'cat-main', name: 'Cámaras', icon: 'pi pi-camera',
    bannerText: 'Vigilancia avanzada', shortDescription: 'Las mejores cámaras del mercado',
    longDescription: 'Descripción larga de cámaras', imageInfo: mockImageInfo,
    timesUsed: 5, parentId: 'cat-parent', children: [mockSubCat1, mockSubCat2]
  };

  const mockParentCategory: Category = {
    id: 'cat-parent', name: 'Seguridad', icon: 'pi pi-shield', bannerText: 'Seg',
    shortDescription: '', longDescription: '', imageInfo: mockImageInfo,
    timesUsed: 8, parentId: '', children: [mockMainCategory, mockSiblingCat]
  };

  // Variant: no parent → uses getAllCategories() for siblings
  const mockMainCategoryNoParent: Category = {
    id: 'cat-nop', name: 'Electrónica', icon: 'pi pi-bolt', bannerText: 'Tech',
    shortDescription: 'Todo sobre electrónica', longDescription: 'Larga descripción',
    imageInfo: mockImageInfo, timesUsed: 7, parentId: '', children: []
  };

  const mockOtherCategoryNoParent: Category = {
    id: 'other-1', name: 'Accesorios', icon: 'pi pi-box', bannerText: '',
    shortDescription: '', longDescription: '', imageInfo: mockImageInfo,
    timesUsed: 2, parentId: '', children: []
  };

  const mockProduct: Product = {
    id: 'prod-1', referenceCode: 'REF-001', name: 'Cámara HD 1080p',
    description: 'Cámara de alta definición', imagesInfo: [mockImageInfo],
    supplyPrice: 30, previousPrice: 100, currentPrice: 79, active: true, discount: '-21%',
    categories: [], totalUnits: 50, availableUnits: 20, shopsWithStock: 2,
    averageRating: 4.3, totalReviews: 8, createdAt: '2026-05-08'
  };

  const mockProductPage: PageResponse<Product> = {
    items: [mockProduct], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 10
  };

  // ─── Setup ────────────────────────────────────────────────────────────────────

  beforeEach(async () => {
    // Ensure history.state is not null to prevent navState.from throwing
    history.replaceState({}, '');

    categoryServiceSpy = jasmine.createSpyObj('CategoryService', [
      'getCategoryById', 'getAllCategories'
    ]);
    categoryServiceSpy.getCategoryById.and.callFake((id: string) => {
      if (id === 'cat-main')   return of(mockMainCategory);
      if (id === 'cat-parent') return of(mockParentCategory);
      return of(mockMainCategory);
    });
    categoryServiceSpy.getAllCategories.and.returnValue(
      of([mockMainCategory, mockSiblingCat])
    );

    productServiceSpy = jasmine.createSpyObj('ProductService', [
      'getProductsByCategoryName', 'searchScope'
    ]);
    productServiceSpy.searchScope.and.returnValue('GLOBAL');
    productServiceSpy.getProductsByCategoryName.and.returnValue(of(mockProductPage));

    breadcrumbServiceSpy = jasmine.createSpyObj('BreadcrumbService', [
      'setNodesForUrl', 'insertPenultimateNodesForUrl', 'setBaseBreadcrumbs', 'breadcrumbs'
    ]);
    breadcrumbServiceSpy.breadcrumbs.and.returnValue([]);

    routerSpy = jasmine.createSpyObj(
      'Router',
      ['navigate', 'navigateByUrl', 'createUrlTree', 'serializeUrl'],
      { url: '/category/cat-main', events: new Subject<any>(), navigated: false }
    );
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/search');
    routerSpy.navigateByUrl.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [
        CategoryInfoComponent,
        BrowserAnimationsModule
      ],
      providers: [
        { provide: CategoryService,   useValue: categoryServiceSpy },
        { provide: ProductService,    useValue: productServiceSpy },
        { provide: BreadcrumbService, useValue: breadcrumbServiceSpy },
        { provide: Router,            useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ id: 'cat-main' }),
            snapshot: { paramMap: { get: (key: string) => key === 'id' ? 'cat-main' : null }, url: [], data: {} },
            root: { children: [] }
          }
        },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CategoryInfoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ─── Creation ─────────────────────────────────────────────────────────────────

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // ─── useCases — static data ───────────────────────────────────────────────────

  it('should expose 4 use-case items', () => {
    expect(component.useCases.length).toBe(4);
  });

  it('should include "Seguridad para tu Negocio" in the use-cases list', () => {
    expect(component.useCases.some(u => u.title === 'Seguridad para tu Negocio')).toBeTrue();
  });

  // ─── Happy-path load ──────────────────────────────────────────────────────────

  it('should call getCategoryById with the route id on init', () => {
    expect(categoryServiceSpy.getCategoryById).toHaveBeenCalledWith('cat-main');
  });

  it('should populate mainCategory after a successful load', () => {
    expect(component.mainCategory).toEqual(mockMainCategory);
  });

  it('should set loading=false after the full load chain completes', () => {
    expect(component.loading).toBeFalse();
  });

  it('should set error=false after a successful load', () => {
    expect(component.error).toBeFalse();
  });

  it('should populate topSalesProducts after a successful load', () => {
    expect(component.topSalesProducts).toEqual([mockProduct]);
  });

  // ─── Breadcrumb calls ─────────────────────────────────────────────────────────

  it('should call setNodesForUrl with the category name', () => {
    expect(breadcrumbServiceSpy.setNodesForUrl).toHaveBeenCalledWith(
      routerSpy.url,
      [jasmine.objectContaining({ label: mockMainCategory.name })]
    );
  });

  it('should call insertPenultimateNodesForUrl with [] when not coming from categories-management', () => {
    // Default history.state is {} → no "from" key
    expect(breadcrumbServiceSpy.insertPenultimateNodesForUrl).toHaveBeenCalledWith(
      routerSpy.url, []
    );
  });

  it('should call insertPenultimateNodesForUrl with the manager breadcrumb when coming from categories-management', () => {
    history.replaceState({ from: 'categories-management' }, '');
    component.loadMainCategory();
    expect(breadcrumbServiceSpy.insertPenultimateNodesForUrl).toHaveBeenCalledWith(
      routerSpy.url,
      [jasmine.objectContaining({ label: 'Gestor de Categorías' })]
    );
  });

  // ─── Error state ──────────────────────────────────────────────────────────────

  it('should set error=true and loading=false when getCategoryById fails', () => {
    categoryServiceSpy.getCategoryById.and.returnValue(throwError(() => new Error('404')));
    component.loadMainCategory();
    expect(component.error).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  // ─── loadSimilarCategories: with parentId ─────────────────────────────────────

  it('should call getCategoryById(parentId) when the mainCategory has a parentId', () => {
    // Already called in beforeEach with mockMainCategory (parentId='cat-parent')
    expect(categoryServiceSpy.getCategoryById).toHaveBeenCalledWith('cat-parent');
  });

  it('should set similarCategories to parent children excluding itself when parentId exists', () => {
    // mockParentCategory.children = [mockMainCategory, mockSiblingCat]
    // After filtering out cat-main → [mockSiblingCat]
    expect(component.similarCategories).toEqual([mockSiblingCat]);
  });

  // ─── loadSimilarCategories: without parentId ──────────────────────────────────

  it('should call getAllCategories when the mainCategory has no parentId', () => {
    categoryServiceSpy.getCategoryById.and.returnValue(of(mockMainCategoryNoParent));
    categoryServiceSpy.getAllCategories.and.returnValue(
      of([mockMainCategoryNoParent, mockOtherCategoryNoParent])
    );
    component.loadMainCategory();
    expect(categoryServiceSpy.getAllCategories).toHaveBeenCalled();
  });

  it('should filter out the mainCategory from getAllCategories result when no parentId', () => {
    categoryServiceSpy.getCategoryById.and.returnValue(of(mockMainCategoryNoParent));
    categoryServiceSpy.getAllCategories.and.returnValue(
      of([mockMainCategoryNoParent, mockOtherCategoryNoParent])
    );
    component.loadMainCategory();
    expect(component.similarCategories).toEqual([mockOtherCategoryNoParent]);
  });

  // ─── loadTopSalesProducts ─────────────────────────────────────────────────────

  it('should call getProductsByCategoryName with the main category name', () => {
    expect(productServiceSpy.getProductsByCategoryName)
      .toHaveBeenCalledWith(mockMainCategory.name);
  });

  // ─── loadMainCategory reset ───────────────────────────────────────────────────

  it('should reset loading=true and error=false at the start of each loadMainCategory call', () => {
    const blocker = new Subject<Category>();
    categoryServiceSpy.getCategoryById.and.returnValue(blocker.asObservable());

    component.error = true;
    component.loadMainCategory();

    // Before the subject emits the state is frozen at the reset point
    expect(component.loading).toBeTrue();
    expect(component.error).toBeFalse();
  });

  // ─── DOM: loading / error screen ──────────────────────────────────────────────

  it('should show the loading screen when loading=true', () => {
    component.loading = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-loading-screen')).not.toBeNull();
  });

  it('should show the loading screen when error=true', () => {
    component.loading = false;
    component.error = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-loading-screen')).not.toBeNull();
  });

  it('should hide the loading screen and show main content when loaded successfully', () => {
    expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain(mockMainCategory.name);
  });

  // ─── DOM: category details ────────────────────────────────────────────────────

  it('should render the main category name in the heading', () => {
    expect(fixture.nativeElement.textContent).toContain('Cámaras');
  });

  it('should render the category shortDescription', () => {
    expect(fixture.nativeElement.textContent).toContain(mockMainCategory.shortDescription);
  });

  it('should render the category bannerText', () => {
    expect(fixture.nativeElement.textContent).toContain(mockMainCategory.bannerText);
  });

  it('should render the category longDescription', () => {
    expect(fixture.nativeElement.textContent).toContain(mockMainCategory.longDescription);
  });

  it('should render "Los más vendidos en Cámaras" heading', () => {
    expect(fixture.nativeElement.textContent).toContain(`Los más vendidos en ${mockMainCategory.name}`);
  });

  // ─── DOM: sub-categories ──────────────────────────────────────────────────────

  it('should render child subcategory names', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain(mockSubCat1.name);
    expect(text).toContain(mockSubCat2.name);
  });

  // ─── DOM: top-sales products ──────────────────────────────────────────────────

  it('should display top-sales product names after load', () => {
    expect(fixture.nativeElement.textContent).toContain(mockProduct.name);
  });

  // ─── DOM: similar categories ──────────────────────────────────────────────────

  it('should render similar category names', () => {
    expect(fixture.nativeElement.textContent).toContain(mockSiblingCat.name);
  });

  // ─── DOM: RouterLink ──────────────────────────────────────────────────────────

  it('should attach RouterLink directives for navigation', () => {
    const links = fixture.debugElement.queryAll(By.directive(RouterLink));
    expect(links.length).toBeGreaterThan(0);
  });
});