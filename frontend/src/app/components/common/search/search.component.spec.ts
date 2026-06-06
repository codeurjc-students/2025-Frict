import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, convertToParamMap, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {TreeNode} from 'primeng/api';

import {SearchComponent} from './search.component';
import {ProductService} from '../../../services/product.service';
import {CategoryService} from '../../../services/category.service';
import {AuthService} from '../../../services/auth.service';
import {Product} from '../../../models/product.model';
import {PageResponse} from '../../../models/pageResponse.model';
import {Category} from '../../../models/category.model';

// ── Stubs ─────────────────────────────────────────────────────────────────────

const STUB_PRODUCT: Product = {
  id: '1', referenceCode: 'REF-001', name: 'Laptop',
  imagesInfo: [{ id: '1', imageUrl: '/laptop.jpg', s3Key: '', fileName: '' }],
  description: 'A laptop', supplyPrice: 100, previousPrice: 120, currentPrice: 100,
  active: true, discount: '0%', categories: [], totalUnits: 10, availableUnits: 10,
  shopsWithStock: 1, averageRating: 4.5, totalReviews: 10, specifications: [], capacity: 1, createdAt: '2025-01-01'
};

const STUB_PAGE: PageResponse<Product> = {
  items: [STUB_PRODUCT], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 10
};

const EMPTY_PAGE: PageResponse<Product> = {
  items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0
};

const STUB_CATEGORY: Category = {
  id: '10', name: 'Electronics', icon: '', bannerText: '', shortDescription: '',
  longDescription: '', imageInfo: { id: '1', imageUrl: '', s3Key: '', fileName: '' },
  timesUsed: 5, parentId: '', children: []
};

// ── Spec ──────────────────────────────────────────────────────────────────────

describe('SearchComponent', () => {
  let component: SearchComponent;
  let fixture: ComponentFixture<SearchComponent>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let categoryServiceSpy: jasmine.SpyObj<CategoryService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    productServiceSpy = jasmine.createSpyObj('ProductService', ['getFilteredProducts', 'getSpecsCatalog']);
    productServiceSpy.getFilteredProducts.and.callFake(() => of({ ...STUB_PAGE, items: [{ ...STUB_PRODUCT }] }));
    productServiceSpy.getSpecsCatalog.and.callFake(() => of({}));
    (productServiceSpy as any).searchScope = jasmine.createSpy('searchScope').and.returnValue('GLOBAL');

    categoryServiceSpy = jasmine.createSpyObj('CategoryService', ['getAllCategories']);
    categoryServiceSpy.getAllCategories.and.callFake(() => of([]));

    authServiceSpy = jasmine.createSpyObj('AuthService', ['isAdmin', 'isManager', 'isDriver']);
    authServiceSpy.isAdmin.and.returnValue(false);
    authServiceSpy.isManager.and.returnValue(false);
    authServiceSpy.isDriver.and.returnValue(false);

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: new Subject().asObservable(),
      url: '/search'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    await TestBed.configureTestingModule({
      imports: [SearchComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: ProductService, useValue: productServiceSpy },
        { provide: CategoryService, useValue: categoryServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: of(convertToParamMap({})),
            snapshot: {
              queryParamMap: convertToParamMap({}),
              paramMap: { get: () => null },
              url: [],
              data: {}
            },
            root: { children: [] }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Default state ─────────────────────────────────────────────────────────────

  describe('default state', () => {
    it('should set loading to false after init', () => {
      expect(component.loading).toBeFalse();
    });

    it('should set error to false', () => {
      expect(component.error).toBeFalse();
    });

    it('should set searchQuery to null after empty params', () => {
      expect(component.searchQuery).toBeNull();
    });

    it('should initialize visibleDrawer to false', () => {
      expect(component.visibleDrawer).toBeFalse();
    });

    it('should initialize first to 0', () => {
      expect(component.first).toBe(0);
    });

    it('should initialize rows to 10', () => {
      expect(component.rows).toBe(10);
    });

    it('should expose 9 sort options', () => {
      expect(component.sortOptions.length).toBe(9);
    });

    it('should set selectedSortOption to the first option by default', () => {
      expect(component.selectedSortOption).toEqual(component.sortOptions[0]);
    });

    it('should initialize selectedCategories to an empty array', () => {
      expect(component.selectedCategories).toEqual([]);
    });

    it('should populate foundProducts after init', () => {
      expect(component.foundProducts.totalItems).toBe(1);
    });
  });

  // ── ngOnInit ──────────────────────────────────────────────────────────────────

  describe('ngOnInit', () => {
    it('should call categoryService.getAllCategories', () => {
      expect(categoryServiceSpy.getAllCategories).toHaveBeenCalled();
    });

    it('should call productService.getFilteredProducts after categories load', () => {
      expect(productServiceSpy.getFilteredProducts).toHaveBeenCalled();
    });

    it('should leave loading=false after successful init', () => {
      expect(component.loading).toBeFalse();
    });
  });

  // ── reloadAll ─────────────────────────────────────────────────────────────────

  describe('reloadAll', () => {
    it('should set loading=true', () => {
      const pending$ = new Subject<any>();
      categoryServiceSpy.getAllCategories.and.callFake(() => pending$.asObservable());
      component.reloadAll();
      expect(component.loading).toBeTrue();
    });

    it('should reset error to false', () => {
      component.error = true;
      component.reloadAll();
      expect(component.error).toBeFalse();
    });

    it('should clear the categories array', () => {
      component.categories = [{ key: '0', label: 'Test', data: '1' }];
      component.reloadAll();
      expect(component.categories).toEqual([]);
    });

    it('should clear foundProducts.items', () => {
      const pending$ = new Subject<PageResponse<Product>>();
      productServiceSpy.getFilteredProducts.and.callFake(() => pending$.asObservable());
      component.foundProducts.items = [STUB_PRODUCT];
      component.reloadAll();
      expect(component.foundProducts.items).toEqual([]);
    });

    it('should call getAllCategories again', () => {
      categoryServiceSpy.getAllCategories.calls.reset();
      component.reloadAll();
      expect(categoryServiceSpy.getAllCategories).toHaveBeenCalled();
    });
  });

  // ── getAllCategories ───────────────────────────────────────────────────────────

  describe('getAllCategories', () => {
    it('should map category list to tree nodes on success', () => {
      categoryServiceSpy.getAllCategories.and.callFake(() => of([STUB_CATEGORY]));
      (component as any)['getAllCategories'](false);
      expect(component.categories.length).toBe(1);
      expect(component.categories[0].label).toBe('Electronics');
    });

    it('should call loadProducts after categories load', () => {
      spyOn(component, 'loadProducts');
      categoryServiceSpy.getAllCategories.and.callFake(() => of([]));
      (component as any)['getAllCategories'](false);
      expect(component.loadProducts).toHaveBeenCalled();
    });

    it('should set error=true and loading=false on failure when isInitialLoad=false', () => {
      categoryServiceSpy.getAllCategories.and.callFake(() => throwError(() => new Error('fail')));
      (component as any)['getAllCategories'](false);
      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
    });

    it('should still call loadProducts on error when isInitialLoad=true', () => {
      spyOn(component, 'loadProducts');
      categoryServiceSpy.getAllCategories.and.callFake(() => throwError(() => new Error('fail')));
      (component as any)['getAllCategories'](true);
      expect(component.loadProducts).toHaveBeenCalled();
    });

    it('should handle a null category response without throwing', () => {
      categoryServiceSpy.getAllCategories.and.callFake(() => of(null as any));
      expect(() => (component as any)['getAllCategories'](false)).not.toThrow();
    });
  });

  // ── loadProducts ──────────────────────────────────────────────────────────────

  describe('loadProducts', () => {
    it('should call getFilteredProducts with default args', () => {
      component.loadProducts();
      expect(productServiceSpy.getFilteredProducts).toHaveBeenCalledWith(0, 10, '', [], 'name,asc', []);
    });

    it('should pass searchQuery as the query arg', () => {
      component.searchQuery = 'laptop';
      component.loadProducts();
      expect(productServiceSpy.getFilteredProducts).toHaveBeenCalledWith(0, 10, 'laptop', [], 'name,asc', []);
    });

    it('should pass selected category data ids', () => {
      component.selectedCategories = [{ key: '0', label: 'Electronics', data: 10 }];
      component.loadProducts();
      expect(productServiceSpy.getFilteredProducts).toHaveBeenCalledWith(0, 10, '', [10], 'name,asc', []);
    });

    it('should set loading=true before the response arrives', () => {
      const pending$ = new Subject<PageResponse<Product>>();
      productServiceSpy.getFilteredProducts.and.callFake(() => pending$.asObservable());
      component.loadProducts();
      expect(component.loading).toBeTrue();
    });

    it('should set foundProducts and loading=false on success', () => {
      productServiceSpy.getFilteredProducts.and.callFake(() => of({ ...STUB_PAGE }));
      component.loadProducts();
      expect(component.foundProducts.totalItems).toBe(1);
      expect(component.loading).toBeFalse();
    });

    it('should set loading=false and error=true on failure', () => {
      productServiceSpy.getFilteredProducts.and.callFake(() => throwError(() => new Error('fail')));
      component.loadProducts();
      expect(component.loading).toBeFalse();
      expect(component.error).toBeTrue();
    });
  });

  // ── onCategoryChange ──────────────────────────────────────────────────────────

  describe('onCategoryChange', () => {
    it('should reset first to 0', () => {
      component.first = 20;
      component.onCategoryChange([]);
      expect(component.first).toBe(0);
    });

    it('should call router.navigate with page=0', () => {
      component.onCategoryChange([]);
      expect(routerSpy.navigate).toHaveBeenCalledWith(
        [], jasmine.objectContaining({ queryParams: jasmine.objectContaining({ page: 0 }) })
      );
    });

    it('should include event category ids in queryParams', () => {
      const node: TreeNode = { key: '0', label: 'Electronics', data: 10 };
      component.onCategoryChange([node]);
      expect(routerSpy.navigate).toHaveBeenCalledWith(
        [], jasmine.objectContaining({ queryParams: jasmine.objectContaining({ categories: '10' }) })
      );
    });

    it('should set categories to null in queryParams when event is empty', () => {
      component.onCategoryChange([]);
      expect(routerSpy.navigate).toHaveBeenCalledWith(
        [], jasmine.objectContaining({ queryParams: jasmine.objectContaining({ categories: null }) })
      );
    });
  });

  // ── onSortChange ──────────────────────────────────────────────────────────────

  describe('onSortChange', () => {
    it('should call router.navigate with page=0', () => {
      component.onSortChange();
      expect(routerSpy.navigate).toHaveBeenCalledWith(
        [], jasmine.objectContaining({ queryParams: jasmine.objectContaining({ page: 0 }) })
      );
    });

    it('should include the selected sort value in queryParams', () => {
      component.selectedSortOption = component.sortOptions[2]; // 'currentPrice,asc' is now at index 2
      component.onSortChange();
      expect(routerSpy.navigate).toHaveBeenCalledWith(
        [], jasmine.objectContaining({ queryParams: jasmine.objectContaining({ sort: 'currentPrice,asc' }) })
      );
    });
  });

  // ── onPageChange ──────────────────────────────────────────────────────────────

  describe('onPageChange', () => {
    it('should update first from event', () => {
      component.onPageChange({ first: 10, rows: 10 });
      expect(component.first).toBe(10);
    });

    it('should update rows from event', () => {
      component.onPageChange({ first: 0, rows: 20 });
      expect(component.rows).toBe(20);
    });

    it('should call router.navigate with correct page and size', () => {
      component.onPageChange({ first: 20, rows: 10 });
      expect(routerSpy.navigate).toHaveBeenCalledWith(
        [], jasmine.objectContaining({ queryParams: jasmine.objectContaining({ page: 2, size: 10 }) })
      );
    });

    it('should default first to 0 when event.first is undefined', () => {
      component.onPageChange({ rows: 10 });
      expect(component.first).toBe(0);
    });

    it('should default rows to 10 when event.rows is undefined', () => {
      component.onPageChange({ first: 0 });
      expect(component.rows).toBe(10);
    });
  });

  // ── syncStateWithUrl ──────────────────────────────────────────────────────────

  describe('syncStateWithUrl', () => {
    it('should set searchQuery from query param', () => {
      (component as any)['syncStateWithUrl'](convertToParamMap({ query: 'mouse' }));
      expect(component.searchQuery).toBe('mouse');
    });

    it('should set searchQuery to null when query param is absent', () => {
      (component as any)['syncStateWithUrl'](convertToParamMap({}));
      expect(component.searchQuery).toBeNull();
    });

    it('should set selectedSortOption from sort param', () => {
      (component as any)['syncStateWithUrl'](convertToParamMap({ sort: 'currentPrice,desc' }));
      expect(component.selectedSortOption.value).toBe('currentPrice,desc');
    });

    it('should default to first sort option when sort param is absent', () => {
      (component as any)['syncStateWithUrl'](convertToParamMap({}));
      expect(component.selectedSortOption).toEqual(component.sortOptions[0]);
    });

    it('should default to first sort option for an unrecognized sort value', () => {
      (component as any)['syncStateWithUrl'](convertToParamMap({ sort: 'unknown,value' }));
      expect(component.selectedSortOption).toEqual(component.sortOptions[0]);
    });

    it('should set first and rows from page and size params', () => {
      (component as any)['syncStateWithUrl'](convertToParamMap({ page: '2', size: '20' }));
      expect(component.first).toBe(40);
      expect(component.rows).toBe(20);
    });

    it('should resolve selectedCategories from category ids when categories are loaded', () => {
      component.categories = [{ key: '0', label: 'Electronics', data: '10' }];
      (component as any)['syncStateWithUrl'](convertToParamMap({ categories: '10' }));
      expect(component.selectedCategories.length).toBe(1);
      expect(component.selectedCategories[0].label).toBe('Electronics');
    });

    it('should set selectedCategories to empty array when no category param', () => {
      (component as any)['syncStateWithUrl'](convertToParamMap({}));
      expect(component.selectedCategories).toEqual([]);
    });

    it('should call loadProducts', () => {
      spyOn(component, 'loadProducts');
      (component as any)['syncStateWithUrl'](convertToParamMap({}));
      expect(component.loadProducts).toHaveBeenCalled();
    });
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should render the total items count', () => {
      expect(fixture.nativeElement.textContent).toContain('1');
    });

    it('should show the "resultados" label', () => {
      expect(fixture.nativeElement.textContent).toContain('resultados');
    });

    it('should show the search query in the heading when searchQuery is set', () => {
      component.searchQuery = 'laptop';
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('"laptop"');
    });

    it('should NOT show "búsqueda:" when searchQuery is null', () => {
      component.searchQuery = null;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('búsqueda:');
    });

    it('should render a product card for each item in foundProducts', () => {
      expect(fixture.nativeElement.querySelectorAll('app-product-card').length).toBe(1);
    });

    it('should show "No hemos encontrado nada" when items array is empty and not loading', () => {
      component.foundProducts = { ...EMPTY_PAGE };
      component.loading = false;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('No hemos encontrado nada');
    });

    it('should NOT show "No hemos encontrado nada" while loading', () => {
      component.foundProducts = { ...EMPTY_PAGE };
      component.loading = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('No hemos encontrado nada');
    });

    it('should show the paginator when totalItems > 0', () => {
      expect(fixture.nativeElement.querySelector('p-paginator')).toBeTruthy();
    });

    it('should NOT show the paginator when totalItems is 0', () => {
      component.foundProducts = { ...EMPTY_PAGE };
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('p-paginator')).toBeFalsy();
    });

    it('should render the "Ordenar:" label', () => {
      expect(fixture.nativeElement.textContent).toContain('Ordenar:');
    });

    it('should render the "Filtrar y Ordenar" mobile drawer button', () => {
      expect(fixture.nativeElement.textContent).toContain('Filtrar y Ordenar');
    });

    it('should show "No hay filtros disponibles." when the category list is empty', () => {
      expect(fixture.nativeElement.textContent).toContain('No hay filtros disponibles.');
    });
  });

  // ── getAllCategories (isInitialLoad=true error path) ───────────────────────────

  describe('getAllCategories with isInitialLoad=true on error', () => {
    it('should NOT set error=true when isInitialLoad is true and categories fail', () => {
      categoryServiceSpy.getAllCategories.and.returnValue(throwError(() => new Error('500')));
      component.error = false;
      (component as any).getAllCategories(true);
      expect(component.error).toBeFalse();
    });
  });
});
