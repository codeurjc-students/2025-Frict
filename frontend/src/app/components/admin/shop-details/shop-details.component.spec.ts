import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ShopDetailsComponent } from './shop-details.component';
import { of, throwError, Subject } from 'rxjs';
import { PLATFORM_ID } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { ShopService } from '../../../services/shop.service';
import { TruckService } from '../../../services/truck.service';
import { ProductService } from '../../../services/product.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Shop } from '../../../models/shop.model';
import { Truck } from '../../../models/truck.model';
import { ShopStock } from '../../../models/shopStock.model';
import { Product } from '../../../models/product.model';
import { PageResponse } from '../../../models/pageResponse.model';
import { BreadcrumbService } from '../../../utils/breadcrumb.service';
import { AuthService } from '../../../services/auth.service';

const mockAddress = {
  id: 'addr-1',
  alias: 'Principal',
  street: 'Calle Mayor',
  number: '10',
  floor: '1A',
  postalCode: '28001',
  city: 'Madrid',
  country: 'España',
  latitude: 40.4168,
  longitude: -3.7038
};

const mockShop: Shop = {
  id: 'shop-1',
  referenceCode: 'SH-001',
  name: 'Tienda Central',
  address: { ...mockAddress },
  assignedBudget: 1000,
  imageInfo: { id: 'img-1', imageUrl: 'http://example.com/img.jpg', s3Key: 'k1', fileName: 'img.jpg' },
  totalAvailableProducts: 5,
  totalAssignedTrucks: 2
};

const mockTruck: Truck = {
  id: 'truck-1',
  referenceCode: 'TR-001',
  plateNumber: '1234-ABC',
  history: [],
  shopId: 'shop-1',
  address: { ...mockAddress },
  ordersToDeliver: 2,
  maxOrderCapacity: 5
};

const mockStock: ShopStock = {
  id: 'stock-1',
  shopId: 'shop-1',
  shopName: 'Tienda Central',
  shopAddress: 'Calle Mayor 10',
  shopImageUrl: 'http://example.com/img.jpg',
  productId: 'prod-1',
  productName: 'Producto A',
  productReferenceCode: 'PR-001',
  productSupplyPrice: 10,
  productCurrentPrice: 15,
  units: 20,
  active: true
};

const mockProduct: Product = {
  id: 'prod-2',
  referenceCode: 'PR-002',
  name: 'Producto B',
  imagesInfo: [{ id: 'img-2', imageUrl: 'http://example.com/prod.jpg', s3Key: 'k2', fileName: 'prod.jpg' }],
  description: 'Descripción B',
  supplyPrice: 8,
  previousPrice: 12,
  currentPrice: 14,
  active: true,
  discount: '0',
  categories: [],
  totalUnits: 100,
  availableUnits: 80,
  shopsWithStock: 3,
  averageRating: 4.5,
  totalReviews: 10,
  specifications: [],
  createdAt: '2025-01-01'
};

const mockTrucksPage: PageResponse<Truck> = {
  items: [{ ...mockTruck }],
  totalItems: 1,
  currentPage: 0,
  lastPage: 0,
  pageSize: 5
};

const mockStocksPage: PageResponse<ShopStock> = {
  items: [{ ...mockStock }],
  totalItems: 1,
  currentPage: 0,
  lastPage: 0,
  pageSize: 5
};

describe('ShopDetailsComponent', () => {
  let component: ShopDetailsComponent;
  let fixture: ComponentFixture<ShopDetailsComponent>;
  let shopServiceSpy: jasmine.SpyObj<ShopService>;
  let truckServiceSpy: jasmine.SpyObj<TruckService>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let confirmationService: ConfirmationService;
  let routerSpy: jasmine.SpyObj<Router>;
  let routerEvents$: Subject<any>;

  const c = () => component as any;

  beforeEach(async () => {
    routerEvents$ = new Subject<any>();

    shopServiceSpy = jasmine.createSpyObj('ShopService', [
      'getShopById', 'getStocksPageByShopId', 'assignTruck', 'assignStock',
      'toggleAllLocalActivations', 'toggleLocalActivation', 'restockProduct'
    ]);
    truckServiceSpy = jasmine.createSpyObj('TruckService', [
      'getTrucksPageByShopId', 'getUnassignedTrucks'
    ]);
    productServiceSpy = jasmine.createSpyObj('ProductService', ['getEligibleProducts']);
    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: routerEvents$.asObservable(),
      url: '/admin/shops/shop-1'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    shopServiceSpy.getShopById.and.callFake(() =>
      of({ ...mockShop, address: { ...mockAddress } })
    );
    shopServiceSpy.getStocksPageByShopId.and.callFake(() =>
      of({ ...mockStocksPage, items: [{ ...mockStock }] })
    );
    truckServiceSpy.getTrucksPageByShopId.and.callFake(() =>
      of({ ...mockTrucksPage, items: [{ ...mockTruck, address: { ...mockAddress } }] })
    );

    await TestBed.configureTestingModule({
      imports: [ShopDetailsComponent],
      providers: [
        provideNoopAnimations(),
        { provide: PLATFORM_ID, useValue: 'server' },
        { provide: ShopService, useValue: shopServiceSpy },
        { provide: TruckService, useValue: truckServiceSpy },
        { provide: ProductService, useValue: productServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: (key: string) => (key === 'id' ? 'shop-1' : null) },
              url: [],
              data: {}
            },
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
        }
      ]
    }).compileComponents();

    confirmationService = TestBed.inject(ConfirmationService);
    fixture = TestBed.createComponent(ShopDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // --- ngOnInit / loadData ---

  describe('ngOnInit / loadData', () => {
    it('should call getShopById with the route id on init', () => {
      expect(shopServiceSpy.getShopById).toHaveBeenCalledWith('shop-1');
    });

    it('should set shop after successful load', () => {
      expect(component.shop).toEqual(jasmine.objectContaining({ id: 'shop-1', name: 'Tienda Central' }));
    });

    it('should load stocks and trucks pages after shop is loaded', () => {
      expect(shopServiceSpy.getStocksPageByShopId).toHaveBeenCalledWith('shop-1', 0, 5);
      expect(truckServiceSpy.getTrucksPageByShopId).toHaveBeenCalledWith('shop-1', 0, 5);
    });

    it('should set loading to false after trucks page loads', () => {
      expect(component.loading).toBeFalse();
    });

    it('should populate trucksPage items', () => {
      expect(component.trucksPage.items.length).toBe(1);
      expect(component.trucksPage.items[0].id).toBe('truck-1');
    });

    it('should populate stocksPage items', () => {
      expect(component.stocksPage.items.length).toBe(1);
      expect(component.stocksPage.items[0].id).toBe('stock-1');
    });

    it('should set error true and loading false when getShopById fails', () => {
      shopServiceSpy.getShopById.and.returnValue(throwError(() => new Error('fail')));
      component.loadData();
      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
    });

    it('should show error message when stocks page fails', () => {
      shopServiceSpy.getStocksPageByShopId.and.returnValue(throwError(() => new Error('fail')));
      component.loadStocksPage();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });

    it('should show error message and set loading false when trucks page fails', () => {
      shopServiceSpy.getShopById.and.callFake(() =>
        of({ ...mockShop, address: { ...mockAddress } })
      );
      truckServiceSpy.getTrucksPageByShopId.and.returnValue(throwError(() => new Error('fail')));
      component.loadData();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(component.loading).toBeFalse();
    });

    it('should do nothing when route id is missing', () => {
      shopServiceSpy.getShopById.calls.reset();
      const activatedRoute = TestBed.inject(ActivatedRoute);
      spyOn(activatedRoute.snapshot.paramMap, 'get').and.returnValue(null);
      component.loadData();
      expect(shopServiceSpy.getShopById).not.toHaveBeenCalled();
    });

    it('should not call loadStocksPage when shop is missing', () => {
      (component as any).shop = undefined;
      shopServiceSpy.getStocksPageByShopId.calls.reset();
      component.loadStocksPage();
      expect(shopServiceSpy.getStocksPageByShopId).not.toHaveBeenCalled();
    });
  });

  // --- ngOnDestroy ---

  describe('ngOnDestroy', () => {
    it('should not throw when called without a map', () => {
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  // --- reloadAll ---

  describe('reloadAll', () => {
    it('should reset state and trigger loadData', () => {
      component.selectedStock = { ...mockStock };
      component.restockQuantity = 5;
      shopServiceSpy.getShopById.calls.reset();
      shopServiceSpy.getShopById.and.callFake(() =>
        of({ ...mockShop, address: { ...mockAddress } })
      );

      component.reloadAll();

      expect(component.error).toBeFalse();
      expect(component.selectedStock).toBeUndefined();
      expect(component.restockQuantity).toBe(0);
      expect(shopServiceSpy.getShopById).toHaveBeenCalled();
    });

    it('should reset selectedTruck on reload', () => {
      c().selectedTruck = { ...mockTruck };
      component.reloadAll();
      expect(c().selectedTruck).toBeUndefined();
    });
  });

  // --- showAddTruckDialog ---

  describe('showAddTruckDialog', () => {
    it('should load unassigned trucks and open dialog', () => {
      const trucks = [{ ...mockTruck, id: 'truck-free' }];
      truckServiceSpy.getUnassignedTrucks.and.returnValue(of(trucks));
      component.showAddTruckDialog();
      expect(c().unassignedTrucks).toEqual(trucks);
      expect(c().visibleAddTruckDialog).toBeTrue();
    });

    it('should show error when getUnassignedTrucks fails', () => {
      truckServiceSpy.getUnassignedTrucks.and.returnValue(throwError(() => new Error('fail')));
      component.showAddTruckDialog();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // --- cancelAddTruck ---

  describe('cancelAddTruck', () => {
    it('should clear selectedTruck and hide the dialog', () => {
      c().selectedTruck = { ...mockTruck };
      c().visibleAddTruckDialog = true;
      component.cancelAddTruck();
      expect(c().selectedTruck).toBeUndefined();
      expect(c().visibleAddTruckDialog).toBeFalse();
    });
  });

  // --- manageTruckAssignment ---

  describe('manageTruckAssignment', () => {
    it('should call assignTruck directly when state is true', () => {
      shopServiceSpy.assignTruck.and.returnValue(of({ ...mockShop }));
      truckServiceSpy.getTrucksPageByShopId.and.callFake(() =>
        of({ ...mockTrucksPage, items: [{ ...mockTruck, address: { ...mockAddress } }] })
      );
      component.manageTruckAssignment('truck-1', true);
      expect(shopServiceSpy.assignTruck).toHaveBeenCalledWith('shop-1', 'truck-1', true);
    });

    it('should open confirmation dialog when state is false', () => {
      spyOn(confirmationService, 'confirm');
      component.manageTruckAssignment('truck-1', false);
      expect(confirmationService.confirm).toHaveBeenCalled();
    });

    it('should call assignTruck with false when confirmation is accepted', () => {
      shopServiceSpy.assignTruck.and.returnValue(of({ ...mockShop }));
      truckServiceSpy.getTrucksPageByShopId.and.callFake(() =>
        of({ ...mockTrucksPage, items: [{ ...mockTruck, address: { ...mockAddress } }] })
      );
      spyOn(confirmationService, 'confirm').and.callFake((cfg: any) => cfg.accept());
      component.manageTruckAssignment('truck-1', false);
      expect(shopServiceSpy.assignTruck).toHaveBeenCalledWith('shop-1', 'truck-1', false);
    });

    it('should do nothing when truckId is undefined', () => {
      component.manageTruckAssignment(undefined, true);
      expect(shopServiceSpy.assignTruck).not.toHaveBeenCalled();
    });
  });

  // --- confirmTruckAssignment ---

  describe('confirmTruckAssignment', () => {
    it('should reload trucks and close dialog on success', () => {
      shopServiceSpy.assignTruck.and.returnValue(of({ ...mockShop }));
      truckServiceSpy.getTrucksPageByShopId.and.callFake(() =>
        of({ ...mockTrucksPage, items: [{ ...mockTruck, address: { ...mockAddress } }] })
      );
      c().selectedTruck = { ...mockTruck };
      c().visibleAddTruckDialog = true;

      component.confirmTruckAssignment('shop-1', 'truck-1', true);

      expect(shopServiceSpy.assignTruck).toHaveBeenCalledWith('shop-1', 'truck-1', true);
      expect(c().visibleAddTruckDialog).toBeFalse();
    });

    it('should show error message on failure', () => {
      shopServiceSpy.assignTruck.and.returnValue(throwError(() => new Error('fail')));
      component.confirmTruckAssignment('shop-1', 'truck-1', true);
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // --- showAddStockDialog ---

  describe('showAddStockDialog', () => {
    it('should load eligible products and open dialog', () => {
      const products = [{ ...mockProduct }];
      productServiceSpy.getEligibleProducts.and.returnValue(of(products));
      component.showAddStockDialog();
      expect(c().eligibleProducts).toEqual(products);
      expect(c().visibleAddProductStockDialog).toBeTrue();
    });

    it('should show error when getEligibleProducts fails', () => {
      productServiceSpy.getEligibleProducts.and.returnValue(throwError(() => new Error('fail')));
      component.showAddStockDialog();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // --- cancelAddStock ---

  describe('cancelAddStock', () => {
    it('should clear selectedProduct and hide the dialog', () => {
      c().selectedProduct = { ...mockProduct };
      c().visibleAddProductStockDialog = true;
      component.cancelAddStock();
      expect(c().selectedProduct).toBeUndefined();
      expect(c().visibleAddProductStockDialog).toBeFalse();
    });
  });

  // --- manageStockAssignment ---

  describe('manageStockAssignment', () => {
    it('should call assignStock directly when state is true', () => {
      shopServiceSpy.assignStock.and.returnValue(of({ ...mockShop }));
      shopServiceSpy.getStocksPageByShopId.and.callFake(() =>
        of({ ...mockStocksPage, items: [{ ...mockStock }] })
      );
      component.manageStockAssignment('prod-2', true);
      expect(shopServiceSpy.assignStock).toHaveBeenCalledWith('shop-1', 'prod-2', true);
    });

    it('should open confirmation dialog when state is false', () => {
      spyOn(confirmationService, 'confirm');
      component.manageStockAssignment('stock-1', false);
      expect(confirmationService.confirm).toHaveBeenCalled();
    });

    it('should call assignStock with false when confirmation is accepted', () => {
      shopServiceSpy.assignStock.and.returnValue(of({ ...mockShop }));
      shopServiceSpy.getStocksPageByShopId.and.callFake(() =>
        of({ ...mockStocksPage, items: [{ ...mockStock }] })
      );
      spyOn(confirmationService, 'confirm').and.callFake((cfg: any) => cfg.accept());
      component.manageStockAssignment('stock-1', false);
      expect(shopServiceSpy.assignStock).toHaveBeenCalledWith('shop-1', 'stock-1', false);
    });

    it('should do nothing when stockId is undefined', () => {
      component.manageStockAssignment(undefined, true);
      expect(shopServiceSpy.assignStock).not.toHaveBeenCalled();
    });
  });

  // --- confirmStockAssignment ---

  describe('confirmStockAssignment', () => {
    it('should reload stocks and close dialog on success', () => {
      shopServiceSpy.assignStock.and.returnValue(of({ ...mockShop }));
      shopServiceSpy.getStocksPageByShopId.and.callFake(() =>
        of({ ...mockStocksPage, items: [{ ...mockStock }] })
      );
      c().selectedProduct = { ...mockProduct };
      c().visibleAddProductStockDialog = true;

      component.confirmStockAssignment('shop-1', 'stock-1', false);

      expect(shopServiceSpy.assignStock).toHaveBeenCalledWith('shop-1', 'stock-1', false);
      expect(c().visibleAddProductStockDialog).toBeFalse();
    });

    it('should show error message on failure', () => {
      shopServiceSpy.assignStock.and.returnValue(throwError(() => new Error('fail')));
      component.confirmStockAssignment('shop-1', 'stock-1', false);
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // --- getMaxRestockAllowed ---

  describe('getMaxRestockAllowed', () => {
    it('should return 0 when selectedStock is undefined', () => {
      component.selectedStock = undefined;
      expect(component.getMaxRestockAllowed()).toBe(0);
    });

    it('should return 0 when productSupplyPrice is 0', () => {
      component.selectedStock = { ...mockStock, productSupplyPrice: 0 };
      expect(component.getMaxRestockAllowed()).toBe(0);
    });

    it('should return floor of budget divided by supply price', () => {
      component.shop = { ...mockShop, assignedBudget: 105 };
      component.selectedStock = { ...mockStock, productSupplyPrice: 10 };
      expect(component.getMaxRestockAllowed()).toBe(10);
    });

    it('should return 0 when budget is less than supply price', () => {
      component.shop = { ...mockShop, assignedBudget: 5 };
      component.selectedStock = { ...mockStock, productSupplyPrice: 10 };
      expect(component.getMaxRestockAllowed()).toBe(0);
    });
  });

  // --- selectProductForReplenish ---

  describe('selectProductForReplenish', () => {
    it('should set selectedStock and reset restockQuantity', () => {
      component.restockQuantity = 7;
      component.selectProductForReplenish({ ...mockStock });
      expect(component.selectedStock).toEqual(jasmine.objectContaining({ id: 'stock-1' }));
      expect(component.restockQuantity).toBe(0);
    });
  });

  // --- confirmReplenish ---

  describe('confirmReplenish', () => {
    it('should not call restockProduct when restockQuantity is 0', () => {
      component.selectedStock = { ...mockStock };
      component.restockQuantity = 0;
      component.confirmReplenish();
      expect(shopServiceSpy.restockProduct).not.toHaveBeenCalled();
    });

    it('should not call restockProduct when selectedStock is undefined', () => {
      component.selectedStock = undefined;
      component.restockQuantity = 5;
      component.confirmReplenish();
      expect(shopServiceSpy.restockProduct).not.toHaveBeenCalled();
    });

    it('should call restockProduct with correct stock id and quantity', () => {
      shopServiceSpy.restockProduct.and.returnValue(of({ ...mockStock, units: 25 }));
      component.selectedStock = { ...mockStock };
      component.restockQuantity = 5;
      component.confirmReplenish();
      expect(shopServiceSpy.restockProduct).toHaveBeenCalledWith('stock-1', 5);
    });

    it('should add qty to stock units on success', () => {
      shopServiceSpy.restockProduct.and.returnValue(of({ ...mockStock, units: 25 }));
      component.stocksPage = { ...mockStocksPage, items: [{ ...mockStock }] };
      component.selectedStock = { ...mockStock };
      component.restockQuantity = 5;

      component.confirmReplenish();

      const updated = component.stocksPage.items.find(i => i.id === 'stock-1');
      expect(updated?.units).toBe(25);
    });

    it('should decrement assignedBudget on success', () => {
      shopServiceSpy.restockProduct.and.returnValue(of({ ...mockStock, units: 25 }));
      component.selectedStock = { ...mockStock };
      component.restockQuantity = 5;
      const budgetBefore = component.shop.assignedBudget;

      component.confirmReplenish();

      expect(component.shop.assignedBudget).toBe(budgetBefore - 10 * 5);
    });

    it('should reset restockQuantity to 0 on success', () => {
      shopServiceSpy.restockProduct.and.returnValue(of({ ...mockStock, units: 25 }));
      component.selectedStock = { ...mockStock };
      component.restockQuantity = 5;
      component.confirmReplenish();
      expect(component.restockQuantity).toBe(0);
    });

    it('should show success message on success', () => {
      shopServiceSpy.restockProduct.and.returnValue(of({ ...mockStock, units: 25 }));
      component.selectedStock = { ...mockStock };
      component.restockQuantity = 5;
      component.confirmReplenish();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should show error message on failure', () => {
      shopServiceSpy.restockProduct.and.returnValue(throwError(() => new Error('fail')));
      component.selectedStock = { ...mockStock };
      component.restockQuantity = 5;
      component.confirmReplenish();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // --- onGlobalAction ---

  describe('onGlobalAction', () => {
    it('should call toggleAllLocalActivations(shop.id, true) for activate_all', () => {
      shopServiceSpy.toggleAllLocalActivations.and.returnValue(of(true));
      component.onGlobalAction('activate_all');
      expect(shopServiceSpy.toggleAllLocalActivations).toHaveBeenCalledWith('shop-1', true);
    });

    it('should set all stock items active=true on activate_all success', () => {
      shopServiceSpy.toggleAllLocalActivations.and.returnValue(of(true));
      component.stocksPage = { ...mockStocksPage, items: [{ ...mockStock, active: false }] };
      component.onGlobalAction('activate_all');
      expect(component.stocksPage.items.every(p => p.active)).toBeTrue();
    });

    it('should call toggleAllLocalActivations(shop.id, false) for deactivate_all', () => {
      shopServiceSpy.toggleAllLocalActivations.and.returnValue(of(true));
      component.onGlobalAction('deactivate_all');
      expect(shopServiceSpy.toggleAllLocalActivations).toHaveBeenCalledWith('shop-1', false);
    });

    it('should set all stock items active=false on deactivate_all success', () => {
      shopServiceSpy.toggleAllLocalActivations.and.returnValue(of(true));
      component.stocksPage = { ...mockStocksPage, items: [{ ...mockStock, active: true }] };
      component.onGlobalAction('deactivate_all');
      expect(component.stocksPage.items.every(p => !p.active)).toBeTrue();
    });
  });

  // --- onToggleActive ---

  describe('onToggleActive', () => {
    it('should call toggleLocalActivation with the new value', () => {
      shopServiceSpy.toggleLocalActivation.and.returnValue(of({ ...mockProduct }));
      const stock = { ...mockStock, active: false };
      component.onToggleActive(stock, { checked: true });
      expect(shopServiceSpy.toggleLocalActivation).toHaveBeenCalledWith('stock-1', true);
    });

    it('should keep new active value on success', () => {
      shopServiceSpy.toggleLocalActivation.and.returnValue(of({ ...mockProduct }));
      const stock = { ...mockStock, active: false };
      component.onToggleActive(stock, { checked: true });
      expect(stock.active).toBeTrue();
    });

    it('should revert active value on error', () => {
      shopServiceSpy.toggleLocalActivation.and.returnValue(throwError(() => new Error('fail')));
      const stock = { ...mockStock, active: true };
      component.onToggleActive(stock, { checked: false });
      expect(stock.active).toBeTrue();
    });

    it('should show success message on success', () => {
      shopServiceSpy.toggleLocalActivation.and.returnValue(of({ ...mockProduct }));
      const stock = { ...mockStock };
      component.onToggleActive(stock, { checked: false });
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should show error message on failure', () => {
      shopServiceSpy.toggleLocalActivation.and.returnValue(throwError(() => new Error('fail')));
      const stock = { ...mockStock };
      component.onToggleActive(stock, { checked: false });
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // --- onStocksPageChange ---

  describe('onStocksPageChange', () => {
    it('should update firstStock and stocksRows and reload', () => {
      shopServiceSpy.getStocksPageByShopId.calls.reset();
      component.onStocksPageChange({ first: 5, rows: 5 });
      expect(component.firstStock).toBe(5);
      expect(component.stocksRows).toBe(5);
      expect(shopServiceSpy.getStocksPageByShopId).toHaveBeenCalledWith('shop-1', 1, 5);
    });

    it('should default to first=0 and rows=5 when event values are undefined', () => {
      component.onStocksPageChange({ first: undefined, rows: undefined });
      expect(component.firstStock).toBe(0);
      expect(component.stocksRows).toBe(5);
    });
  });

  // --- onTrucksPageChange ---

  describe('onTrucksPageChange', () => {
    it('should update firstTruck and trucksRows and reload', () => {
      truckServiceSpy.getTrucksPageByShopId.calls.reset();
      component.onTrucksPageChange({ first: 5, rows: 5 });
      expect(component.firstTruck).toBe(5);
      expect(component.trucksRows).toBe(5);
      expect(truckServiceSpy.getTrucksPageByShopId).toHaveBeenCalledWith('shop-1', 1, 5);
    });

    it('should default to first=0 and rows=5 when event values are undefined', () => {
      component.onTrucksPageChange({ first: undefined, rows: undefined });
      expect(component.firstTruck).toBe(0);
      expect(component.trucksRows).toBe(5);
    });
  });

  // --- focusTruckOnMap ---

  describe('focusTruckOnMap', () => {
    it('should not throw when map is not initialized', () => {
      expect(() => component.focusTruckOnMap({ ...mockTruck, address: { ...mockAddress } })).not.toThrow();
    });
  });

  // --- goBack ---

  describe('goBack', () => {
    it('should navigate to /admin/shops', () => {
      component.goBack();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/shops']);
    });
  });

  // --- DOM ---

  describe('DOM', () => {
    it('should render the shop name', () => {
      expect(fixture.nativeElement.textContent).toContain('Tienda Central');
    });

    it('should render the shop reference code', () => {
      expect(fixture.nativeElement.textContent).toContain('SH-001');
    });

    it('should render the truck reference code in the trucks table', () => {
      expect(fixture.nativeElement.textContent).toContain('TR-001');
    });

    it('should render the product name in the stock table', () => {
      expect(fixture.nativeElement.textContent).toContain('Producto A');
    });

    it('should show empty-trucks message when trucks list is empty', () => {
      component.trucksPage = { items: [], totalItems: 0, currentPage: 0, lastPage: 0, pageSize: 5 };
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('No hay camiones asignados a esta tienda.');
    });

    it('should show empty-stock message when stocks list is empty', () => {
      component.stocksPage = { items: [], totalItems: 0, currentPage: 0, lastPage: 0, pageSize: 5 };
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('No hay stock disponible en esta tienda.');
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
      expect(fixture.nativeElement.querySelector('h1')).toBeFalsy();
    });

    it('should show restock panel when selectedStock is set', () => {
      component.selectedStock = { ...mockStock };
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Reponer Stock');
    });

    it('should hide restock panel when selectedStock is undefined', () => {
      component.selectedStock = undefined;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Reponer Stock');
    });
  });
});
