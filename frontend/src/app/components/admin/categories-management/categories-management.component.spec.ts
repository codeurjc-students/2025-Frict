import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';

import {CategoriesManagementComponent} from './categories-management.component';
import {CategoryService} from '../../../services/category.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {Category} from '../../../models/category.model';
import {PageResponse} from '../../../models/pageResponse.model';

// ── Mock data ──────────────────────────────────────────────────────────────────

const mockImageInfo: any = { id: 'img-1', imageUrl: '', s3Key: '', fileName: '' };

const mockCategory: Category = {
  id: 'cat-1', name: 'Electrónica', icon: 'pi pi-bolt', bannerText: 'Oferta',
  shortDescription: '', longDescription: '', imageInfo: mockImageInfo,
  timesUsed: 5, parentId: '', children: []
};

const mockCategory2: Category = {
  id: 'cat-2', name: 'Moda', icon: '', bannerText: '',
  shortDescription: '', longDescription: '', imageInfo: mockImageInfo,
  timesUsed: 0, parentId: '', children: []
};

const mockPage: PageResponse<Category> = {
  items: [{ ...mockCategory }, { ...mockCategory2 }],
  totalItems: 2, currentPage: 0, lastPage: 0, pageSize: 5
};

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('CategoriesManagementComponent', () => {
  let component: CategoriesManagementComponent;
  let fixture: ComponentFixture<CategoriesManagementComponent>;
  let categoryServiceSpy: jasmine.SpyObj<CategoryService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let routerEvents$: Subject<any>;

  beforeEach(async () => {
    routerEvents$ = new Subject<any>();

    categoryServiceSpy = jasmine.createSpyObj('CategoryService', [
      'getAllCategories', 'getAllCategoriesPage', 'deleteCategory'
    ]);

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: routerEvents$.asObservable(),
      url: '/admin/categories'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    categoryServiceSpy.getAllCategoriesPage.and.callFake(() => of({ ...mockPage, items: [{ ...mockCategory }, { ...mockCategory2 }] }));
    categoryServiceSpy.getAllCategories.and.callFake(() => of([{ ...mockCategory }, { ...mockCategory2 }]));
    categoryServiceSpy.deleteCategory.and.callFake(() => of({ ...mockCategory }));

    await TestBed.configureTestingModule({
      imports: [CategoriesManagementComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: CategoryService, useValue: categoryServiceSpy },
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

    fixture = TestBed.createComponent(CategoriesManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── loadCategories — paginated mode ───────────────────────────────────────────

  describe('loadCategories — paginated mode (default)', () => {
    it('should call getAllCategoriesPage with page 0 and default rows on init', () => {
      expect(categoryServiceSpy.getAllCategoriesPage).toHaveBeenCalledWith(0, 5);
    });

    it('should NOT call getAllCategories in paginated mode', () => {
      expect(categoryServiceSpy.getAllCategories).not.toHaveBeenCalled();
    });

    it('should set loading=false after page loads', () => {
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    });

    it('should populate categoriesPage from service response', () => {
      expect(component.categoriesPage.totalItems).toBe(2);
      expect(component.categoriesPage.items.length).toBe(2);
    });

    it('should populate treeTableNodes from page items', () => {
      expect(component.treeTableNodes().length).toBe(2);
    });

    it('should populate orgChartNodes with a virtual root node', () => {
      expect(component.orgChartNodes().length).toBe(1);
      expect(component.orgChartNodes()[0].data.id).toBe(-1);
      expect(component.orgChartNodes()[0].data.name).toBe('Vista Paginada');
    });

    it('should set loading=false on service error (error flag stays false)', () => {
      categoryServiceSpy.getAllCategoriesPage.and.callFake(() => throwError(() => new Error('fail')));
      component.loadCategories();
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    });

    it('should reset error=false and loading=true at start of each call', () => {
      component.error = true;
      let capturedLoading = false;
      categoryServiceSpy.getAllCategoriesPage.and.callFake(() => {
        capturedLoading = component.loading;
        return of({ ...mockPage });
      });
      component.loadCategories();
      expect(capturedLoading).toBeTrue();
      expect(component.error).toBeFalse();
    });
  });

  // ── loadCategories — full list mode ───────────────────────────────────────────

  describe('loadCategories — full list mode (listModeSelected=true)', () => {
    beforeEach(() => {
      component.listModeSelected = true;
      categoryServiceSpy.getAllCategories.calls.reset();
      categoryServiceSpy.getAllCategoriesPage.calls.reset();
      component.loadCategories();
    });

    it('should call getAllCategories (not getAllCategoriesPage)', () => {
      expect(categoryServiceSpy.getAllCategories).toHaveBeenCalled();
      expect(categoryServiceSpy.getAllCategoriesPage).not.toHaveBeenCalled();
    });

    it('should set loading=false after full list loads', () => {
      expect(component.loading).toBeFalse();
    });

    it('should populate treeTableNodes from full list', () => {
      expect(component.treeTableNodes().length).toBe(2);
    });

    it('should set virtual root node name to "Catálogo Completo"', () => {
      expect(component.orgChartNodes()[0].data.name).toBe('Catálogo Completo');
    });

    it('should set loading=false on getAllCategories error', () => {
      categoryServiceSpy.getAllCategories.and.callFake(() => throwError(() => new Error('fail')));
      component.loadCategories();
      expect(component.loading).toBeFalse();
    });
  });

  // ── onCategoryPageChange ──────────────────────────────────────────────────────

  describe('onCategoryPageChange', () => {
    it('should update first from event', () => {
      component.onCategoryPageChange({ first: 10, rows: 5 });
      expect(component.first).toBe(10);
    });

    it('should update rows from event', () => {
      component.onCategoryPageChange({ first: 0, rows: 10 });
      expect(component.rows).toBe(10);
    });

    it('should call getAllCategoriesPage with computed page index', () => {
      categoryServiceSpy.getAllCategoriesPage.calls.reset();
      component.onCategoryPageChange({ first: 10, rows: 5 });
      expect(categoryServiceSpy.getAllCategoriesPage).toHaveBeenCalledWith(2, 5);
    });

    it('should default first and rows when event fields are undefined', () => {
      component.onCategoryPageChange({});
      expect(component.first).toBe(0);
      expect(component.rows).toBe(10);
    });
  });

  // ── deleteCategory ────────────────────────────────────────────────────────────

  describe('deleteCategory', () => {
    it('should call categoryService.deleteCategory with the given id', () => {
      component.deleteCategory('cat-1');
      expect(categoryServiceSpy.deleteCategory).toHaveBeenCalledWith('cat-1');
    });

    it('should reload categories after successful deletion', () => {
      categoryServiceSpy.getAllCategoriesPage.calls.reset();
      component.deleteCategory('cat-1');
      expect(categoryServiceSpy.getAllCategoriesPage).toHaveBeenCalled();
    });
  });

  // ── mapToOrgChart ─────────────────────────────────────────────────────────────

  describe('mapToOrgChart', () => {
    it('should map a category to an org-chart TreeNode with correct data', () => {
      const node = component.mapToOrgChart({ ...mockCategory });
      expect(node.data.id).toBe('cat-1');
      expect(node.data.name).toBe('Electrónica');
      expect(node.data.icon).toBe('pi pi-bolt');
      expect(node.data.timesUsed).toBe(5);
      expect(node.expanded).toBeTrue();
    });

    it('should fall back to "pi pi-folder" when icon is empty', () => {
      const node = component.mapToOrgChart({ ...mockCategory2 });
      expect(node.data.icon).toBe('pi pi-folder');
    });

    it('should recursively map children', () => {
      const child: Category = { ...mockCategory2, id: 'child-1', children: [] };
      const parent: Category = { ...mockCategory, children: [child] };
      const node = component.mapToOrgChart(parent);
      expect(node.children?.length).toBe(1);
      expect(node.children![0].data.id).toBe('child-1');
    });
  });

  // ── mapToTreeTable ────────────────────────────────────────────────────────────

  describe('mapToTreeTable', () => {
    it('should map a category to a tree-table TreeNode with correct data', () => {
      const node = component.mapToTreeTable({ ...mockCategory });
      expect(node.data.id).toBe('cat-1');
      expect(node.data.name).toBe('Electrónica');
      expect(node.data.description).toBe('Oferta');
      expect(node.data.timesUsed).toBe(5);
      expect(node.data.active).toBeTrue();
      expect(node.expanded).toBeFalse();
    });

    it('should fall back to "pi pi-folder" when icon is empty', () => {
      const node = component.mapToTreeTable({ ...mockCategory2 });
      expect(node.data.icon).toBe('pi pi-folder');
    });

    it('should recursively map children', () => {
      const child: Category = { ...mockCategory2, id: 'child-1', children: [] };
      const parent: Category = { ...mockCategory, children: [child] };
      const node = component.mapToTreeTable(parent);
      expect(node.children?.length).toBe(1);
      expect(node.children![0].data.id).toBe('child-1');
    });
  });

  // ── Statistics — flat categories ──────────────────────────────────────────────

  describe('statistics with flat categories (catA:5, catB:0)', () => {
    it('should count 2 total categories', () => {
      expect(component.totalCategories()).toBe(2);
    });

    it('should compute maxDepth=1 for leaf-only categories', () => {
      expect(component.maxDepth()).toBe(1);
    });

    it('should set totalVolume to sum of top-level timesUsed', () => {
      expect(component.totalVolume()).toBe(5);
    });

    it('should set usagePercentage to 50.00 (1 active out of 2)', () => {
      expect(component.usagePercentage()).toBe(50);
    });
  });

  // ── Statistics — nested categories ────────────────────────────────────────────

  describe('statistics with nested categories', () => {
    beforeEach(() => {
      const child: Category = { ...mockCategory, id: 'child-1', timesUsed: 2, children: [] };
      const parent: Category = { ...mockCategory, children: [child] };
      categoryServiceSpy.getAllCategoriesPage.and.callFake(() =>
        of({ ...mockPage, items: [parent, { ...mockCategory2 }] })
      );
      component.loadCategories();
    });

    it('should count 3 total nodes (parent + child + cat2)', () => {
      expect(component.totalCategories()).toBe(3);
    });

    it('should compute maxDepth=2 for one nesting level', () => {
      expect(component.maxDepth()).toBe(2);
    });

    it('should compute usagePercentage based on active nodes', () => {
      // parent(5>0) + child(2>0) = 2 active, cat2(0) inactive → 2/3 * 100 = 66.67
      expect(component.usagePercentage()).toBeCloseTo(66.67, 1);
    });
  });

  // ── Chart data (via effect) ────────────────────────────────────────────────────

  describe('chart data via effect', () => {
    it('should set chartData with category names as labels', () => {
      fixture.detectChanges();
      expect(component.chartData).toBeDefined();
      expect(component.chartData.labels).toContain('Electrónica');
      expect(component.chartData.labels).toContain('Moda');
    });

    it('should set chartData dataset with timesUsed values', () => {
      fixture.detectChanges();
      expect(component.chartData.datasets[0].data).toEqual(jasmine.arrayContaining([5, 0]));
    });

    it('should set barData with active/inactive counts', () => {
      fixture.detectChanges();
      expect(component.barData).toBeDefined();
      // catA active (5>0), catB inactive (0) → [1 inactive, 1 active]
      expect(component.barData.datasets[0].data).toEqual([1, 1]);
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

    it('should show loading-screen on error', () => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });

    it('should show "Gestor de Categorías" heading', () => {
      expect(fixture.nativeElement.textContent).toContain('Gestor de Categorías');
    });

    it('should render breadcrumb-reload component', () => {
      expect(fixture.nativeElement.querySelector('app-breadcrumb-reload')).toBeTruthy();
    });
  });
});
