import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {PLATFORM_ID} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {MessageService} from 'primeng/api';

import {CreateEditTruckComponent} from './create-edit-truck.component';
import {TruckService} from '../../../services/truck.service';
import {ShopService} from '../../../services/shop.service';
import {LocationService} from '../../../services/location.service';
import {AuthService} from '../../../services/auth.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {Truck} from '../../../models/truck.model';
import {Shop} from '../../../models/shop.model';

// ── Mock data ──────────────────────────────────────────────────────────────────

const mockAddress: any = {
  id: 'addr-1', alias: 'Base', street: 'Calle Mayor', number: '10', floor: '',
  postalCode: '28001', city: 'Madrid', country: 'España',
  latitude: 40.4168, longitude: -3.7038
};

const mockTruck: Truck = {
  id: 'truck-1', referenceCode: 'TRK-001', plateNumber: 'AB12CD',
  history: [], shopId: 'shop-1', address: mockAddress,
  ordersToDeliver: 2, maxCapacity: 10, currentCapacity: 0
};

const mockShop: Shop = {
  id: 'shop-1', referenceCode: 'SHP-001', name: 'Tienda Test',
  address: mockAddress, assignedBudget: 10000, maxCapacity: 0, occupiedCapacity: 0,
  imageInfo: { id: 'si1', imageUrl: 'http://img.jpg', s3Key: 'sk1', fileName: 'f.jpg' },
  totalAvailableProducts: 50, totalAssignedTrucks: 1
};

// ── Helpers ────────────────────────────────────────────────────────────────────

function fillValidForm(component: CreateEditTruckComponent) {
  component.truckForm.patchValue({
    plateNumber: 'AB12CD',
    maxCapacity: 10,
    address: {
      street: 'Calle Mayor', number: '10', postalCode: '28001',
      city: 'Madrid', country: 'España', latitude: 40.4168, longitude: -3.7038
    }
  });
}

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('CreateEditTruckComponent', () => {
  let component: CreateEditTruckComponent;
  let fixture: ComponentFixture<CreateEditTruckComponent>;
  let truckServiceSpy: jasmine.SpyObj<TruckService>;
  let shopServiceSpy: jasmine.SpyObj<ShopService>;
  let locationServiceSpy: jasmine.SpyObj<LocationService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let routerEvents$: Subject<any>;

  function buildTestBed(truckId: string | null = null) {
    return TestBed.configureTestingModule({
      imports: [CreateEditTruckComponent],
      providers: [
        provideNoopAnimations(),
        { provide: PLATFORM_ID, useValue: 'server' },
        { provide: TruckService, useValue: truckServiceSpy },
        { provide: ShopService, useValue: shopServiceSpy },
        { provide: LocationService, useValue: locationServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: (key: string) => key === 'id' ? truckId : null },
              url: [], data: {}
            },
            root: { children: [] }
          }
        },
        {
          provide: AuthService,
          useValue: {
            isAdmin: jasmine.createSpy().and.returnValue(false),
            isManager: jasmine.createSpy().and.returnValue(false),
            isDriver: jasmine.createSpy().and.returnValue(false),
            isLogged: jasmine.createSpy().and.returnValue(false)
          }
        },
        BreadcrumbService
      ]
    }).compileComponents();
  }

  beforeEach(async () => {
    routerEvents$ = new Subject<any>();

    truckServiceSpy = jasmine.createSpyObj('TruckService', [
      'getTruckById', 'createTruck', 'updateTruck', 'checkPlateNumberTaken'
    ]);
    truckServiceSpy.checkPlateNumberTaken.and.callFake(() => of(false));
    shopServiceSpy = jasmine.createSpyObj('ShopService', [
      'getShopById', 'getAllShopsList'
    ]);
    locationServiceSpy = jasmine.createSpyObj('LocationService', [
      'getCoordinatesFromAddress', 'getAddressFromCoordinates'
    ]);
    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: routerEvents$.asObservable(),
      url: '/admin/trucks/new'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    locationServiceSpy.getCoordinatesFromAddress.and.callFake(() => of(null));
    locationServiceSpy.getAddressFromCoordinates.and.callFake(() => of(null));
    shopServiceSpy.getAllShopsList.and.callFake(() => of([{ ...mockShop }]));
    shopServiceSpy.getShopById.and.callFake(() => of({ ...mockShop }));
    truckServiceSpy.getTruckById.and.callFake(() => of({ ...mockTruck }));

    // Default: create mode (no truckId)
    await buildTestBed(null);

    fixture = TestBed.createComponent(CreateEditTruckComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Create mode ───────────────────────────────────────────────────────────────

  describe('create mode (no truckId)', () => {
    it('should have null truckId signal', () => {
      expect(component.truckId()).toBeNull();
    });

    it('should set loading=false without calling getTruckById', () => {
      expect(truckServiceSpy.getTruckById).not.toHaveBeenCalled();
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    });

    it('should initialise form with default values', () => {
      expect(component.truckForm.get('maxCapacity')?.value).toBe(10);
      expect(component.truckForm.get('address.country')?.value).toBe('España');
    });

    it('referenceCode control should be disabled', () => {
      expect(component.truckForm.get('referenceCode')?.disabled).toBeTrue();
    });

    it('reloadAll should reset form fields', () => {
      component.truckForm.patchValue({ plateNumber: 'XY99ZZ' });
      component.reloadAll();

      expect(component.truckForm.get('plateNumber')?.value).toBeFalsy();
    });
  });

  // ── Edit mode ─────────────────────────────────────────────────────────────────

  describe('edit mode (with truckId)', () => {
    beforeEach(() => {
      component.truckId.set('truck-1');
      truckServiceSpy.getTruckById.calls.reset();
      component.loadData();
    });

    it('should call getTruckById with the route truckId', () => {
      expect(truckServiceSpy.getTruckById).toHaveBeenCalledWith('truck-1');
    });

    it('should set the truck signal', () => {
      expect(component.truck()).toEqual(jasmine.objectContaining({ id: 'truck-1' }));
    });

    it('should patch form with truck data', () => {
      expect(component.truckForm.get('plateNumber')?.value).toBe('AB12CD');
      expect(component.truckForm.get('maxCapacity')?.value).toBe(10);
    });

    it('should set loading=false after success', () => {
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    });

    it('should load shop when truck has shopId', () => {
      expect(shopServiceSpy.getShopById).toHaveBeenCalledWith('shop-1');
      expect(component.availableShops.length).toBe(1);
    });

    it('should not load shop when truck has no shopId', () => {
      shopServiceSpy.getShopById.calls.reset();
      truckServiceSpy.getTruckById.and.callFake(() => of({ ...mockTruck, shopId: undefined }));
      component.loadData();

      expect(shopServiceSpy.getShopById).not.toHaveBeenCalled();
    });

    it('should set error=true on getTruckById failure', () => {
      truckServiceSpy.getTruckById.and.callFake(() => throwError(() => new Error('fail')));
      component.loadData();

      expect(component.loading).toBeFalse();
      expect(component.error).toBeTrue();
    });

    it('reloadAll should clear error and reload data', () => {
      component.error = true;
      truckServiceSpy.getTruckById.and.callFake(() => of({ ...mockTruck }));
      component.reloadAll();

      expect(component.error).toBeFalse();
      expect(component.loading).toBeFalse();
    });
  });

  // ── loadAvailableShops ────────────────────────────────────────────────────────

  describe('loadAvailableShops', () => {
    it('should skip loading if already loaded', () => {
      component.shopsLoaded = true;
      component.loadAvailableShops();
      expect(shopServiceSpy.getAllShopsList).not.toHaveBeenCalled();
    });

    it('should load shops and set shopsLoaded=true on success', () => {
      component.shopsLoaded = false;
      shopServiceSpy.getAllShopsList.and.callFake(() => of([{ ...mockShop }]));
      component.loadAvailableShops();

      expect(component.availableShops.length).toBe(1);
      expect(component.shopsLoaded).toBeTrue();
      expect(component.loadingShops).toBeFalse();
    });

    it('should prepend current shop when not found in the loaded list', () => {
      const currentShop: Shop = { ...mockShop, id: 'shop-current' };
      component.availableShops = [currentShop];
      shopServiceSpy.getAllShopsList.and.callFake(() => of([{ ...mockShop, id: 'shop-other' }]));
      component.loadAvailableShops();

      expect(component.availableShops[0].id).toBe('shop-current');
      expect(component.availableShops.length).toBe(2);
    });

    it('should not duplicate current shop when it is already in the list', () => {
      component.availableShops = [{ ...mockShop }];
      shopServiceSpy.getAllShopsList.and.callFake(() => of([{ ...mockShop }]));
      component.loadAvailableShops();

      expect(component.availableShops.filter(s => s.id === 'shop-1').length).toBe(1);
    });

    it('should show error and reset loadingShops on failure', () => {
      shopServiceSpy.getAllShopsList.and.callFake(() => throwError(() => new Error('fail')));
      component.loadAvailableShops();

      expect(component.loadingShops).toBeFalse();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── onSubmit ──────────────────────────────────────────────────────────────────

  describe('onSubmit — invalid form', () => {
    it('should mark all touched and show error when form is invalid', () => {
      component.truckForm.patchValue({ plateNumber: '' });
      component.onSubmit();

      expect(component.truckForm.touched).toBeTrue();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(truckServiceSpy.createTruck).not.toHaveBeenCalled();
      expect(truckServiceSpy.updateTruck).not.toHaveBeenCalled();
    });
  });

  describe('onSubmit — create mode', () => {
    beforeEach(() => {
      component.truckId.set(null);
      fillValidForm(component);
    });

    it('should call createTruck and navigate on success', () => {
      truckServiceSpy.createTruck.and.callFake(() => of({ ...mockTruck }));
      component.onSubmit();

      expect(truckServiceSpy.createTruck).toHaveBeenCalled();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/trucks']);
    });

    it('should show error on createTruck failure', () => {
      truckServiceSpy.createTruck.and.callFake(() => throwError(() => new Error('fail')));
      component.onSubmit();

      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
  });

  describe('onSubmit — edit mode', () => {
    beforeEach(() => {
      component.truckId.set('truck-1');
      truckServiceSpy.getTruckById.and.callFake(() => of({ ...mockTruck }));
      component.loadData();
      fillValidForm(component);
    });

    it('should call updateTruck and navigate on success', () => {
      truckServiceSpy.updateTruck.and.callFake(() => of({ ...mockTruck }));
      component.onSubmit();

      expect(truckServiceSpy.updateTruck).toHaveBeenCalledWith('truck-1', jasmine.any(Object));
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/trucks']);
    });

    it('should show error on updateTruck failure', () => {
      truckServiceSpy.updateTruck.and.callFake(() => throwError(() => new Error('fail')));
      component.onSubmit();

      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
  });

  // ── Form validation ───────────────────────────────────────────────────────────

  describe('form validation', () => {
    it('should be invalid when plateNumber is empty', () => {
      component.truckForm.patchValue({ plateNumber: '' });
      expect(component.truckForm.get('plateNumber')?.invalid).toBeTrue();
    });

    it('should be invalid when plateNumber is shorter than 4 chars', () => {
      component.truckForm.patchValue({ plateNumber: 'AB' });
      expect(component.truckForm.get('plateNumber')?.invalid).toBeTrue();
    });

    it('should be valid when plateNumber meets minimum length', fakeAsync(() => {
      fillValidForm(component);
      tick(300); // drain the 250ms async validator debounce
      expect(component.truckForm.get('plateNumber')?.valid).toBeTrue();
    }));

    it('should be invalid when maxCapacity is 0', () => {
      component.truckForm.patchValue({ maxCapacity: 0 });
      expect(component.truckForm.get('maxCapacity')?.invalid).toBeTrue();
    });

    it('should be valid when maxCapacity >= 1', () => {
      component.truckForm.patchValue({ maxCapacity: 5 });
      expect(component.truckForm.get('maxCapacity')?.valid).toBeTrue();
    });

    it('address street field should be required', () => {
      component.truckForm.get('address')?.patchValue({ street: '' });
      expect(component.truckForm.get('address.street')?.invalid).toBeTrue();
    });

    it('address city field should be required', () => {
      component.truckForm.get('address')?.patchValue({ city: '' });
      expect(component.truckForm.get('address.city')?.invalid).toBeTrue();
    });

    it('getRawValue should include disabled referenceCode field', () => {
      component.truckForm.patchValue({ plateNumber: 'AB12CD' });
      const raw = component.truckForm.getRawValue();
      expect('referenceCode' in raw).toBeTrue();
    });
  });

  // ── reloadAll ─────────────────────────────────────────────────────────────────

  describe('reloadAll', () => {
    it('should set loading=true temporarily and clear error', () => {
      component.error = true;
      component.reloadAll();

      // Synchronous of() completes immediately; loading ends false after chain
      expect(component.error).toBeFalse();
    });

    it('should call loadData again', () => {
      const prevCount = truckServiceSpy.getTruckById.calls.count();
      component.truckId.set('truck-1');
      component.reloadAll();

      expect(truckServiceSpy.getTruckById.calls.count()).toBeGreaterThan(prevCount);
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

    it('should show loading-screen on error state', () => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });

    it('should show "Nuevo Camión" title in create mode', () => {
      component.truckId.set(null);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Nuevo Camión');
    });

    it('should show "Editar Camión" title in edit mode', () => {
      component.truckId.set('truck-1');
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Editar Camión');
    });

    it('should render breadcrumb-reload component', () => {
      expect(fixture.nativeElement.querySelector('app-breadcrumb-reload')).toBeTruthy();
    });
  });
});
