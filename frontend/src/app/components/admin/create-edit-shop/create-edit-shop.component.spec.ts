import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {PLATFORM_ID} from '@angular/core';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {MessageService} from 'primeng/api';

import {CreateEditShopComponent} from './create-edit-shop.component';
import {ShopService} from '../../../services/shop.service';
import {LocationService} from '../../../services/location.service';
import {AuthService} from '../../../services/auth.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {Shop} from '../../../models/shop.model';

// ── Mock data ──────────────────────────────────────────────────────────────────

const mockAddress: any = {
  id: 'addr-1', alias: 'Sede', street: 'Gran Vía', number: '1', floor: '',
  postalCode: '28013', city: 'Madrid', country: 'España',
  latitude: 40.4200, longitude: -3.7025
};

const mockShop: Shop = {
  id: 'shop-1', referenceCode: 'SHP-001', name: 'Tienda Test',
  address: mockAddress, assignedBudget: 5000,
  imageInfo: { id: 'img-1', imageUrl: 'http://img.jpg', s3Key: 'k1', fileName: 'f.jpg' },
  totalAvailableProducts: 30, totalAssignedTrucks: 2
};

// ── Helper ─────────────────────────────────────────────────────────────────────

function fillValidForm(component: CreateEditShopComponent) {
  component.shopForm.patchValue({
    name: 'Tienda Test',
    assignedBudget: 1000,
    address: {
      street: 'Gran Vía', number: '1', postalCode: '28013',
      city: 'Madrid', country: 'España', latitude: 40.42, longitude: -3.70
    }
  });
}

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('CreateEditShopComponent', () => {
  let component: CreateEditShopComponent;
  let fixture: ComponentFixture<CreateEditShopComponent>;
  let shopServiceSpy: jasmine.SpyObj<ShopService>;
  let locationServiceSpy: jasmine.SpyObj<LocationService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let routerEvents$: Subject<any>;

  function buildTestBed(shopId: string | null = null) {
    return TestBed.configureTestingModule({
      imports: [CreateEditShopComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: PLATFORM_ID, useValue: 'server' },
        { provide: ShopService, useValue: shopServiceSpy },
        { provide: LocationService, useValue: locationServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: (key: string) => key === 'id' ? shopId : null },
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

    shopServiceSpy = jasmine.createSpyObj('ShopService', [
      'getShopById', 'createShop', 'updateShop', 'updateShopImage'
    ]);
    locationServiceSpy = jasmine.createSpyObj('LocationService', [
      'getCoordinatesFromAddress', 'getAddressFromCoordinates'
    ]);
    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: routerEvents$.asObservable(),
      url: '/admin/shops/new'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    locationServiceSpy.getCoordinatesFromAddress.and.callFake(() => of(null));
    locationServiceSpy.getAddressFromCoordinates.and.callFake(() => of(null));
    shopServiceSpy.getShopById.and.callFake(() => of({ ...mockShop }));
    shopServiceSpy.updateShopImage.and.callFake(() => of({ ...mockShop }));

    // Default: create mode
    await buildTestBed(null);

    fixture = TestBed.createComponent(CreateEditShopComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Create mode ───────────────────────────────────────────────────────────────

  describe('create mode (no shopId)', () => {
    it('should have null shopId signal', () => {
      expect(component.shopId()).toBeNull();
    });

    it('should set loading=false without calling getShopById', () => {
      expect(shopServiceSpy.getShopById).not.toHaveBeenCalled();
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    });

    it('should initialise form with empty name and zero budget', () => {
      expect(component.shopForm.get('name')?.value).toBe('');
      expect(component.shopForm.get('assignedBudget')?.value).toBe(0);
    });

    it('referenceCode control should be disabled', () => {
      expect(component.shopForm.get('referenceCode')?.disabled).toBeTrue();
    });

    it('should have null existingImage and newImage initially', () => {
      expect(component.existingImage()).toBeNull();
      expect(component.newImage()).toBeNull();
    });

    it('reloadAll should reset form and clear images in create mode', () => {
      component.shopForm.patchValue({ name: 'Changed' });
      component.newImage.set({ file: new File(['x'], 'x.jpg'), previewUrl: 'blob:x' });
      component.existingImage.set('http://old.jpg');

      component.reloadAll();

      expect(component.shopForm.get('name')?.value).toBeFalsy();
      expect(component.newImage()).toBeNull();
      expect(component.existingImage()).toBeNull();
    });
  });

  // ── Edit mode ─────────────────────────────────────────────────────────────────

  describe('edit mode (with shopId)', () => {
    beforeEach(() => {
      component.shopId.set('shop-1');
      shopServiceSpy.getShopById.calls.reset();
      component.loadData();
    });

    it('should call getShopById with the route shopId', () => {
      expect(shopServiceSpy.getShopById).toHaveBeenCalledWith('shop-1');
    });

    it('should set the shop signal', () => {
      expect(component.shop()).toEqual(jasmine.objectContaining({ id: 'shop-1' }));
    });

    it('should patch form with shop data', () => {
      expect(component.shopForm.get('name')?.value).toBe('Tienda Test');
      expect(component.shopForm.get('assignedBudget')?.value).toBe(5000);
    });

    it('should set existingImage from shop imageInfo', () => {
      expect(component.existingImage()).toBe('http://img.jpg');
    });

    it('should set loading=false and no error after success', () => {
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    });

    it('should set error=true on getShopById failure', () => {
      shopServiceSpy.getShopById.and.callFake(() => throwError(() => new Error('fail')));
      component.loadData();

      expect(component.loading).toBeFalse();
      expect(component.error).toBeTrue();
    });

    it('reloadAll should clear error and reload data in edit mode', () => {
      component.error = true;
      shopServiceSpy.getShopById.and.callFake(() => of({ ...mockShop }));
      component.reloadAll();

      expect(component.error).toBeFalse();
      expect(component.loading).toBeFalse();
    });

    it('reloadAll should NOT reset form in edit mode', () => {
      component.shopForm.patchValue({ name: 'Modified' });
      shopServiceSpy.getShopById.and.callFake(() => of({ ...mockShop }));
      component.reloadAll();

      // Form gets re-patched from the service response, not reset
      expect(component.shopForm.get('name')?.value).toBe('Tienda Test');
    });
  });

  // ── Image management ──────────────────────────────────────────────────────────

  describe('onFileSelect', () => {
    it('should set newImage and clear existingImage when file size is valid', () => {
      component.existingImage.set('http://old.jpg');
      spyOn(URL, 'createObjectURL').and.returnValue('blob:fake-url');

      const fakeFile = new File(['content'], 'photo.jpg', { type: 'image/jpeg' });
      component.onFileSelect({ files: [fakeFile] });

      expect(component.newImage()).not.toBeNull();
      expect(component.newImage()!.file).toBe(fakeFile);
      expect(component.existingImage()).toBeNull();
    });

    it('should do nothing when file exceeds MAX_SIZE', () => {
      const oversizedFile = { size: component['MAX_SIZE'] + 1, name: 'big.jpg' } as File;
      component.onFileSelect({ files: [oversizedFile] });

      expect(component.newImage()).toBeNull();
    });
  });

  describe('onFileRemove', () => {
    it('should clear newImage', () => {
      component.newImage.set({ file: new File(['x'], 'x.jpg'), previewUrl: 'blob:x' });
      component.onFileRemove();

      expect(component.newImage()).toBeNull();
    });
  });

  describe('removeImage', () => {
    it('should clear both newImage and existingImage', () => {
      component.newImage.set({ file: new File(['x'], 'x.jpg'), previewUrl: 'blob:x' });
      component.existingImage.set('http://img.jpg');

      component.removeImage();

      expect(component.newImage()).toBeNull();
      expect(component.existingImage()).toBeNull();
    });
  });

  describe('restoreImage', () => {
    it('should restore existingImage from shop imageInfo', () => {
      component.shop.set({ ...mockShop });
      component.existingImage.set(null);

      component.restoreImage();

      expect(component.existingImage()).toBe('http://img.jpg');
    });

    it('should do nothing when shop is null', () => {
      component.shop.set(null);
      component.existingImage.set(null);

      component.restoreImage();

      expect(component.existingImage()).toBeNull();
    });
  });

  // ── updateShopImage ───────────────────────────────────────────────────────────

  describe('updateShopImage', () => {
    it('should call shopService.updateShopImage and show success', () => {
      shopServiceSpy.updateShopImage.and.callFake(() => of({ ...mockShop }));
      (component as any).updateShopImage('shop-1', undefined);

      expect(shopServiceSpy.updateShopImage).toHaveBeenCalledWith('shop-1', undefined);
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should show error when updateShopImage fails', () => {
      shopServiceSpy.updateShopImage.and.callFake(() => throwError(() => new Error('fail')));
      (component as any).updateShopImage('shop-1', undefined);

      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── onSubmit — invalid form ───────────────────────────────────────────────────

  describe('onSubmit — invalid form', () => {
    it('should mark all touched and show error without calling service', () => {
      component.shopForm.patchValue({ name: '' });
      component.onSubmit();

      expect(component.shopForm.touched).toBeTrue();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(shopServiceSpy.createShop).not.toHaveBeenCalled();
      expect(shopServiceSpy.updateShop).not.toHaveBeenCalled();
    });
  });

  // ── onSubmit — create mode ────────────────────────────────────────────────────

  describe('onSubmit — create mode', () => {
    beforeEach(() => {
      component.shopId.set(null);
      fillValidForm(component);
    });

    it('should call createShop and navigate on success', () => {
      shopServiceSpy.createShop.and.callFake(() => of({ ...mockShop }));
      component.onSubmit();

      expect(shopServiceSpy.createShop).toHaveBeenCalled();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/shops']);
    });

    it('should call updateShopImage after createShop with no new image', () => {
      shopServiceSpy.createShop.and.callFake(() => of({ ...mockShop }));
      component.onSubmit();

      expect(shopServiceSpy.updateShopImage).toHaveBeenCalledWith('shop-1', undefined);
    });

    it('should call updateShopImage with file when newImage is set', () => {
      const fakeFile = new File(['x'], 'photo.jpg');
      component.newImage.set({ file: fakeFile, previewUrl: 'blob:x' });
      shopServiceSpy.createShop.and.callFake(() => of({ ...mockShop }));
      component.onSubmit();

      expect(shopServiceSpy.updateShopImage).toHaveBeenCalledWith('shop-1', fakeFile);
    });

    it('should show error on createShop failure', () => {
      shopServiceSpy.createShop.and.callFake(() => throwError(() => new Error('fail')));
      component.onSubmit();

      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
  });

  // ── onSubmit — edit mode ──────────────────────────────────────────────────────

  describe('onSubmit — edit mode', () => {
    beforeEach(() => {
      component.shopId.set('shop-1');
      shopServiceSpy.getShopById.and.callFake(() => of({ ...mockShop }));
      component.loadData();
      fillValidForm(component);
    });

    it('should call updateShop and navigate on success', () => {
      shopServiceSpy.updateShop.and.callFake(() => of({ ...mockShop }));
      component.onSubmit();

      expect(shopServiceSpy.updateShop).toHaveBeenCalledWith('shop-1', jasmine.any(Object));
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/shops']);
    });

    it('should call updateShopImage after updateShop', () => {
      shopServiceSpy.updateShop.and.callFake(() => of({ ...mockShop }));
      component.onSubmit();

      expect(shopServiceSpy.updateShopImage).toHaveBeenCalledWith('shop-1', undefined);
    });

    it('should pass the new file to updateShopImage when set', () => {
      const fakeFile = new File(['x'], 'new.jpg');
      component.newImage.set({ file: fakeFile, previewUrl: 'blob:x' });
      shopServiceSpy.updateShop.and.callFake(() => of({ ...mockShop }));
      component.onSubmit();

      expect(shopServiceSpy.updateShopImage).toHaveBeenCalledWith('shop-1', fakeFile);
    });

    it('should show error on updateShop failure', () => {
      shopServiceSpy.updateShop.and.callFake(() => throwError(() => new Error('fail')));
      component.onSubmit();

      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
  });

  // ── Form validation ───────────────────────────────────────────────────────────

  describe('form validation', () => {
    it('should be invalid when name is empty', () => {
      component.shopForm.patchValue({ name: '' });
      expect(component.shopForm.get('name')?.invalid).toBeTrue();
    });

    it('should be invalid when name is shorter than 3 chars', () => {
      component.shopForm.patchValue({ name: 'AB' });
      expect(component.shopForm.get('name')?.invalid).toBeTrue();
    });

    it('should be valid when name meets minimum length', () => {
      component.shopForm.patchValue({ name: 'ABC' });
      expect(component.shopForm.get('name')?.valid).toBeTrue();
    });

    it('should be invalid when assignedBudget is 0', () => {
      component.shopForm.patchValue({ assignedBudget: 0 });
      expect(component.shopForm.get('assignedBudget')?.invalid).toBeTrue();
    });

    it('should be valid when assignedBudget > 0', () => {
      component.shopForm.patchValue({ assignedBudget: 100 });
      expect(component.shopForm.get('assignedBudget')?.valid).toBeTrue();
    });

    it('address street should be required', () => {
      component.shopForm.get('address')?.patchValue({ street: '' });
      expect(component.shopForm.get('address.street')?.invalid).toBeTrue();
    });

    it('address city should be required', () => {
      component.shopForm.get('address')?.patchValue({ city: '' });
      expect(component.shopForm.get('address.city')?.invalid).toBeTrue();
    });

    it('getRawValue should include disabled referenceCode', () => {
      const raw = component.shopForm.getRawValue();
      expect('referenceCode' in raw).toBeTrue();
    });
  });

  // ── reloadAll ─────────────────────────────────────────────────────────────────

  describe('reloadAll', () => {
    it('should clear error flag', () => {
      component.error = true;
      component.reloadAll();
      expect(component.error).toBeFalse();
    });

    it('should call loadData again (edit mode)', () => {
      component.shopId.set('shop-1');
      shopServiceSpy.getShopById.calls.reset();
      component.reloadAll();
      expect(shopServiceSpy.getShopById).toHaveBeenCalled();
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

    it('should show "Nueva Tienda" title in create mode', () => {
      component.shopId.set(null);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Nueva Tienda');
    });

    it('should show "Editar Tienda" title in edit mode', () => {
      component.shopId.set('shop-1');
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Editar Tienda');
    });

    it('should render breadcrumb-reload component', () => {
      expect(fixture.nativeElement.querySelector('app-breadcrumb-reload')).toBeTruthy();
    });

    it('should show "Sin imagen asignada" when no images are set', () => {
      component.existingImage.set(null);
      component.newImage.set(null);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Sin imagen asignada');
    });
  });
});
