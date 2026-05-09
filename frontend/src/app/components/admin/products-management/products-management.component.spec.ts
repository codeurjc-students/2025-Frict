import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductsManagementComponent } from './products-management.component';
import { of, throwError, Subject } from 'rxjs';
import { LOCALE_ID } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ProductService } from '../../../services/product.service';
import { RegistryService } from '../../../services/registry.service';
import { BreadcrumbService } from '../../../utils/breadcrumb.service';
import { AuthService } from '../../../services/auth.service';
import { Product } from '../../../models/product.model';
import { PageResponse } from '../../../models/pageResponse.model';

const mockProduct: Product = {
  id: 'prod-1',
  referenceCode: 'PR-001',
  name: 'Producto Test',
  imagesInfo: [{ id: 'img-1', imageUrl: 'http://example.com/img.jpg', s3Key: 'k1', fileName: 'img.jpg' }],
  description: 'Test product',
  supplyPrice: 8,
  previousPrice: 12,
  currentPrice: 10,
  active: true,
  discount: '0',
  categories: [],
  totalUnits: 50,
  availableUnits: 40,
  shopsWithStock: 3,
  averageRating: 4.0,
  totalReviews: 5,
  createdAt: '2025-01-01'
};

const mockProduct2: Product = {
  id: 'prod-2',
  referenceCode: 'PR-002',
  name: 'Producto B',
  imagesInfo: [{ id: 'img-2', imageUrl: 'http://example.com/img2.jpg', s3Key: 'k2', fileName: 'img2.jpg' }],
  description: 'Test product B',
  supplyPrice: 5,
  previousPrice: 9,
  currentPrice: 8,
  active: false,
  discount: '0',
  categories: [],
  totalUnits: 10,
  availableUnits: 10,
  shopsWithStock: 1,
  averageRating: 3.5,
  totalReviews: 2,
  createdAt: '2025-01-02'
};

const mockProductsPage: PageResponse<Product> = {
  items: [{ ...mockProduct }],
  totalItems: 1,
  currentPage: 0,
  lastPage: 0,
  pageSize: 10
};

describe('ProductsManagementComponent', () => {
  let component: ProductsManagementComponent;
  let fixture: ComponentFixture<ProductsManagementComponent>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let registryServiceSpy: jasmine.SpyObj<RegistryService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let confirmationService: ConfirmationService;
  let routerSpy: jasmine.SpyObj<Router>;
  let routerEvents$: Subject<any>;

  const c = () => component as any;

  beforeEach(async () => {
    routerEvents$ = new Subject<any>();

    productServiceSpy = jasmine.createSpyObj('ProductService', [
      'getAllProducts', 'toggleGlobalActivation', 'toggleAllGlobalActivations', 'deleteProduct'
    ]);
    registryServiceSpy = jasmine.createSpyObj('RegistryService', ['loadInternalRegistry']);
    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: routerEvents$.asObservable(),
      url: '/admin/products'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    productServiceSpy.getAllProducts.and.callFake(() =>
      of({ ...mockProductsPage, items: [{ ...mockProduct }] })
    );
    productServiceSpy.toggleGlobalActivation.and.callFake(() => of({ ...mockProduct }));
    productServiceSpy.toggleAllGlobalActivations.and.callFake(() => of(true));
    productServiceSpy.deleteProduct.and.callFake(() => of({ ...mockProduct }));
    registryServiceSpy.loadInternalRegistry.and.callFake(() =>
      of([{ _id: '2025-01-01', totalValue: 5 }, { _id: '2025-01-02', totalValue: 8 }] as any)
    );

    await TestBed.configureTestingModule({
      imports: [ProductsManagementComponent],
      providers: [
        provideNoopAnimations(),
        { provide: ProductService, useValue: productServiceSpy },
        { provide: RegistryService, useValue: registryServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => null }, url: [], data: {} },
            root: { children: [] }
          }
        },
        ConfirmationService,
        BreadcrumbService,
        {
          provide: AuthService,
          useValue: {
            isAdmin: jasmine.createSpy('isAdmin').and.returnValue(false),
            isManager: jasmine.createSpy('isManager').and.returnValue(false),
            isDriver: jasmine.createSpy('isDriver').and.returnValue(false),
            isLogged: jasmine.createSpy('isLogged').and.returnValue(false)
          }
        },
        { provide: LOCALE_ID, useValue: 'en-US' }
      ]
    }).compileComponents();

    confirmationService = TestBed.inject(ConfirmationService);
    fixture = TestBed.createComponent(ProductsManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // --- ngOnInit / loadProducts ---

  describe('ngOnInit / loadProducts', () => {
    it('should call getAllProducts on init', () => {
      expect(productServiceSpy.getAllProducts).toHaveBeenCalled();
    });

    it('should call getAllProducts with page=0 and size=10 initially', () => {
      expect(productServiceSpy.getAllProducts).toHaveBeenCalledWith(0, 10);
    });

    it('should set productsPage after successful load', () => {
      expect(component.productsPage.items.length).toBe(1);
      expect(component.productsPage.items[0].id).toBe('prod-1');
    });

    it('should set totalItems correctly', () => {
      expect(component.productsPage.totalItems).toBe(1);
    });

    it('should set loading to false after successful load', () => {
      expect(component.loading).toBeFalse();
    });

    it('should set chartProductSelector to the first product', () => {
      expect(component.chartProductSelector()?.id).toBe('prod-1');
    });

    it('should initialize pieData after load', () => {
      expect(component.pieData()).not.toBeNull();
      expect(component.pieData().labels).toContain('Producto Test');
    });

    it('should initialize pieOptions after load', () => {
      expect(component.pieOptions()).not.toBeNull();
    });

    it('should initialize lineOptions after load', () => {
      expect(component.lineOptions()).not.toBeNull();
    });

    it('should call loadInternalRegistry to build line chart data', () => {
      expect(registryServiceSpy.loadInternalRegistry).toHaveBeenCalledWith(
        jasmine.objectContaining({
          entityType: 'PRODUCT',
          dataType: 'PRODUCT_UNITS_SOLD',
          viewType: 'GRAPH'
        })
      );
    });

    it('should set lineData after loadInternalRegistry succeeds', () => {
      expect(component.lineData()).not.toBeNull();
      expect(component.lineData().datasets[0].data).toEqual([5, 8]);
    });

    it('should use product referenceCode in loadInternalRegistry params', () => {
      const callArgs = registryServiceSpy.loadInternalRegistry.calls.mostRecent().args[0] as any;
      expect(callArgs.productIds).toContain('PR-001');
    });

    it('should set error=true and loading=false when getAllProducts fails', () => {
      productServiceSpy.getAllProducts.and.returnValue(throwError(() => new Error('fail')));
      c().loadProducts();
      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
    });

    it('should not throw when loadInternalRegistry fails in line chart update', () => {
      registryServiceSpy.loadInternalRegistry.and.returnValue(throwError(() => new Error('fail')));
      expect(() => c().loadProducts()).not.toThrow();
    });
  });

  // --- confirmDelete ---

  describe('confirmDelete', () => {
    it('should call confirmationService.confirm', () => {
      spyOn(confirmationService, 'confirm');
      component.confirmDelete(new MouseEvent('click'), 'prod-1');
      expect(confirmationService.confirm).toHaveBeenCalled();
    });

    it('should call deleteProduct when confirmation is accepted', () => {
      spyOn(confirmationService, 'confirm').and.callFake((cfg: any) => cfg.accept());
      component.confirmDelete(new MouseEvent('click'), 'prod-1');
      expect(productServiceSpy.deleteProduct).toHaveBeenCalledWith('prod-1');
    });

    it('should show success message after successful delete', () => {
      spyOn(confirmationService, 'confirm').and.callFake((cfg: any) => cfg.accept());
      component.confirmDelete(new MouseEvent('click'), 'prod-1');
      expect(messageServiceSpy.add).toHaveBeenCalledWith(
        jasmine.objectContaining({ severity: 'success' })
      );
    });

    it('should reload products after successful delete', () => {
      spyOn(confirmationService, 'confirm').and.callFake((cfg: any) => cfg.accept());
      productServiceSpy.getAllProducts.calls.reset();
      component.confirmDelete(new MouseEvent('click'), 'prod-1');
      expect(productServiceSpy.getAllProducts).toHaveBeenCalled();
    });

    it('should show error message when delete fails', () => {
      spyOn(confirmationService, 'confirm').and.callFake((cfg: any) => cfg.accept());
      productServiceSpy.deleteProduct.and.returnValue(throwError(() => new Error('fail')));
      component.confirmDelete(new MouseEvent('click'), 'prod-1');
      expect(messageServiceSpy.add).toHaveBeenCalledWith(
        jasmine.objectContaining({ severity: 'error' })
      );
    });
  });

  // --- onGlobalAction ---

  describe('onGlobalAction', () => {
    it('should call toggleAllGlobalActivations(true) for activate_all', () => {
      component.onGlobalAction('activate_all');
      expect(productServiceSpy.toggleAllGlobalActivations).toHaveBeenCalledWith(true);
    });

    it('should set all product items active=true on activate_all success', () => {
      component.productsPage = {
        ...mockProductsPage,
        items: [{ ...mockProduct, active: false }, { ...mockProduct2, active: false }]
      };
      component.onGlobalAction('activate_all');
      expect(component.productsPage.items.every(p => p.active)).toBeTrue();
    });

    it('should call toggleAllGlobalActivations(false) for deactivate_all', () => {
      component.onGlobalAction('deactivate_all');
      expect(productServiceSpy.toggleAllGlobalActivations).toHaveBeenCalledWith(false);
    });

    it('should set all product items active=false on deactivate_all success', () => {
      component.productsPage = {
        ...mockProductsPage,
        items: [{ ...mockProduct, active: true }, { ...mockProduct2, active: true }]
      };
      component.onGlobalAction('deactivate_all');
      expect(component.productsPage.items.every(p => !p.active)).toBeTrue();
    });

    it('should not call any service for unknown action', () => {
      productServiceSpy.toggleAllGlobalActivations.calls.reset();
      component.onGlobalAction('unknown_action');
      expect(productServiceSpy.toggleAllGlobalActivations).not.toHaveBeenCalled();
    });
  });

  // --- onToggleActive ---

  describe('onToggleActive', () => {
    it('should call toggleGlobalActivation with the new value', () => {
      const product = { ...mockProduct, active: false };
      component.onToggleActive(product, { checked: true });
      expect(productServiceSpy.toggleGlobalActivation).toHaveBeenCalledWith('prod-1', true);
    });

    it('should set product.active to new value', () => {
      const product = { ...mockProduct, active: false };
      component.onToggleActive(product, { checked: true });
      expect(product.active).toBeTrue();
    });

    it('should show success message on success', () => {
      const product = { ...mockProduct };
      component.onToggleActive(product, { checked: false });
      expect(messageServiceSpy.add).toHaveBeenCalledWith(
        jasmine.objectContaining({ severity: 'success' })
      );
    });

    it('should revert product.active to original value on error', () => {
      productServiceSpy.toggleGlobalActivation.and.returnValue(throwError(() => new Error('fail')));
      const product = { ...mockProduct, active: true };
      component.onToggleActive(product, { checked: false });
      expect(product.active).toBeTrue();
    });

    it('should show error message on failure', () => {
      productServiceSpy.toggleGlobalActivation.and.returnValue(throwError(() => new Error('fail')));
      const product = { ...mockProduct };
      component.onToggleActive(product, { checked: false });
      expect(messageServiceSpy.add).toHaveBeenCalledWith(
        jasmine.objectContaining({ severity: 'error' })
      );
    });
  });

  // --- onChartProductChange ---

  describe('onChartProductChange', () => {
    it('should update chartProductSelector signal', () => {
      registryServiceSpy.loadInternalRegistry.calls.reset();
      component.onChartProductChange({ value: { ...mockProduct2 } });
      expect(component.chartProductSelector()?.id).toBe('prod-2');
    });

    it('should trigger line chart update via loadInternalRegistry', () => {
      registryServiceSpy.loadInternalRegistry.calls.reset();
      component.onChartProductChange({ value: { ...mockProduct2 } });
      expect(registryServiceSpy.loadInternalRegistry).toHaveBeenCalledWith(
        jasmine.objectContaining({ productIds: ['PR-002'] })
      );
    });

    it('should update lineData with response data', () => {
      registryServiceSpy.loadInternalRegistry.and.callFake(() =>
        of([{ _id: '2025-01-05', totalValue: 20 }] as any)
      );
      component.onChartProductChange({ value: { ...mockProduct2 } });
      expect(component.lineData().datasets[0].data).toEqual([20]);
    });

    it('should use product name in lineData dataset label', () => {
      registryServiceSpy.loadInternalRegistry.and.callFake(() =>
        of([{ _id: '2025-01-05', totalValue: 20 }] as any)
      );
      component.onChartProductChange({ value: { ...mockProduct2 } });
      expect(component.lineData().datasets[0].label).toContain('Producto B');
    });
  });

  // --- onProductsPageChange ---

  describe('onProductsPageChange', () => {
    it('should update first and rows from event', () => {
      component.onProductsPageChange({ first: 10, rows: 10 });
      expect(component.first).toBe(10);
      expect(component.rows).toBe(10);
    });

    it('should call getAllProducts with correct page and size', () => {
      productServiceSpy.getAllProducts.calls.reset();
      component.onProductsPageChange({ first: 10, rows: 10 });
      expect(productServiceSpy.getAllProducts).toHaveBeenCalledWith(1, 10);
    });

    it('should default first to 0 when event.first is undefined', () => {
      component.onProductsPageChange({ first: undefined, rows: 10 });
      expect(component.first).toBe(0);
    });

    it('should default rows to 10 when event.rows is undefined', () => {
      component.onProductsPageChange({ first: 0, rows: undefined });
      expect(component.rows).toBe(10);
    });

    it('should set loading=true while loading page', () => {
      let loadingDuringCall = false;
      productServiceSpy.getAllProducts.and.callFake(() => {
        loadingDuringCall = component.loading;
        return of({ ...mockProductsPage, items: [{ ...mockProduct }] });
      });
      component.onProductsPageChange({ first: 0, rows: 10 });
      expect(loadingDuringCall).toBeTrue();
    });
  });

  // --- pieData and pieOptions (via initCharts) ---

  describe('pieData / pieOptions (via loadProducts)', () => {
    it('should include product names as labels in pieData', () => {
      expect(component.pieData().labels).toEqual(['Producto Test']);
    });

    it('should include totalUnits as dataset data in pieData', () => {
      expect(component.pieData().datasets[0].data).toEqual([50]);
    });

    it('should hide legend when there are more than 10 products', () => {
      const manyItems = Array.from({ length: 11 }, (_, i) => ({
        ...mockProduct,
        id: `prod-${i}`,
        name: `Producto ${i}`
      }));
      productServiceSpy.getAllProducts.and.callFake(() =>
        of({ ...mockProductsPage, items: manyItems, totalItems: 11 })
      );
      c().loadProducts();
      expect(component.pieOptions().plugins.legend.display).toBeFalse();
    });

    it('should show legend when there are 10 or fewer products', () => {
      expect(component.pieOptions().plugins.legend.display).toBeTrue();
    });
  });

  // --- lineData (via updateLineChartData) ---

  describe('lineData (via updateLineChartData)', () => {
    it('should not update lineData when chartProductSelector is null', () => {
      component.chartProductSelector.set(null);
      component.lineData.set(null);
      component.onChartProductChange({ value: null });
      expect(component.lineData()).toBeNull();
    });

    it('should handle res.items format from registry response', () => {
      registryServiceSpy.loadInternalRegistry.and.callFake(() =>
        of({ items: [{ _id: '2025-01-01', totalValue: 99 }] } as any)
      );
      component.onChartProductChange({ value: { ...mockProduct } });
      expect(component.lineData().datasets[0].data).toEqual([99]);
    });

    it('should handle plain array format from registry response', () => {
      registryServiceSpy.loadInternalRegistry.and.callFake(() =>
        of([{ _id: '2025-01-01', totalValue: 77 }] as any)
      );
      component.onChartProductChange({ value: { ...mockProduct } });
      expect(component.lineData().datasets[0].data).toEqual([77]);
    });

    it('should not throw when loadInternalRegistry fails', () => {
      registryServiceSpy.loadInternalRegistry.and.returnValue(throwError(() => new Error('fail')));
      expect(() => component.onChartProductChange({ value: { ...mockProduct } })).not.toThrow();
    });
  });

  // --- DOM ---

  describe('DOM', () => {
    it('should render "Gestor de Productos" heading', () => {
      expect(fixture.nativeElement.textContent).toContain('Gestor de Productos');
    });

    it('should render the product name in the table', () => {
      expect(fixture.nativeElement.textContent).toContain('Producto Test');
    });

    it('should show the total items count in the table header', () => {
      expect(fixture.nativeElement.textContent).toContain('1 productos');
    });

    it('should show empty-products message when items list is empty', () => {
      component.productsPage = { items: [], totalItems: 0, currentPage: 0, lastPage: 0, pageSize: 10 };
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('No se encontraron productos en el catálogo.');
    });

    it('should show loading screen when loading is true', () => {
      component.loading = true;
      component.error = false;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });

    it('should hide main content when loading is true', () => {
      component.loading = true;
      fixture.detectChanges();
      const allText = fixture.nativeElement.textContent as string;
      expect(allText).not.toContain('Gestor de Productos');
    });

    it('should show loading screen when error is true', () => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });
  });
});
