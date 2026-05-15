import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {MessageService} from 'primeng/api';

import {CreateEditProductComponent} from './create-edit-product.component';
import {ProductService} from '../../../services/product.service';
import {CategoryService} from '../../../services/category.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {Product} from '../../../models/product.model';
import {Category} from '../../../models/category.model';
import {ImageInfo} from '../../../models/imageInfo.model';

// ── Mock data ──────────────────────────────────────────────────────────────────

const mockImageInfo: ImageInfo = {
  id: 'img-1', imageUrl: 'http://img.jpg', s3Key: 'k1', fileName: 'f.jpg'
};

const mockCategory: Category = {
  id: 'cat-1', name: 'Electrónica', icon: '', bannerText: '',
  shortDescription: '', longDescription: '', imageInfo: { ...mockImageInfo },
  timesUsed: 0, parentId: '', children: []
};

const mockCategoryOtros: Category = {
  id: 'cat-otros', name: 'Otros', icon: '', bannerText: '',
  shortDescription: '', longDescription: '', imageInfo: { ...mockImageInfo },
  timesUsed: 0, parentId: '', children: []
};

const mockProduct: Product = {
  id: 'prod-1',
  referenceCode: 'PRD-001',
  name: 'Producto Test',
  imagesInfo: [{ ...mockImageInfo }],
  description: 'Descripción de prueba',
  supplyPrice: 10,
  previousPrice: 0,
  currentPrice: 15,
  active: true,
  discount: '0%',
  categories: [{ ...mockCategory }],
  totalUnits: 100,
  availableUnits: 50,
  shopsWithStock: 3,
  averageRating: 4.5,
  totalReviews: 10,
  specifications: [],
  createdAt: '2025-01-01'
};

// ── Helper ─────────────────────────────────────────────────────────────────────

function fillValidForm(component: CreateEditProductComponent) {
  component.productForm.patchValue({
    name: 'Producto Test',
    supplyPrice: 10.00,
    currentPrice: 15.00,
    active: true
  });
}

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('CreateEditProductComponent', () => {
  let component: CreateEditProductComponent;
  let fixture: ComponentFixture<CreateEditProductComponent>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let categoryServiceSpy: jasmine.SpyObj<CategoryService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let routerEvents$: Subject<any>;

  function buildTestBed(productId: string | null = null) {
    return TestBed.configureTestingModule({
      imports: [CreateEditProductComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: ProductService, useValue: productServiceSpy },
        { provide: CategoryService, useValue: categoryServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: (key: string) => key === 'id' ? productId : null },
              url: [], data: {}
            },
            root: { children: [] }
          }
        },
        BreadcrumbService
      ]
    }).compileComponents();
  }

  beforeEach(async () => {
    routerEvents$ = new Subject<any>();

    productServiceSpy = jasmine.createSpyObj('ProductService', [
      'getProductById', 'createProduct', 'updateProduct', 'updateProductImages', 'getSpecsCatalog'
    ]);
    categoryServiceSpy = jasmine.createSpyObj('CategoryService', ['getAllCategories']);
    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: routerEvents$.asObservable(),
      url: '/admin/products/new'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    categoryServiceSpy.getAllCategories.and.callFake(() => of([{ ...mockCategory }]));
    productServiceSpy.getProductById.and.callFake(() =>
      of({ ...mockProduct, imagesInfo: [{ ...mockImageInfo }], categories: [{ ...mockCategory }] })
    );
    productServiceSpy.createProduct.and.callFake(() => of({ ...mockProduct }));
    productServiceSpy.updateProduct.and.callFake(() => of({ ...mockProduct }));
    productServiceSpy.updateProductImages.and.callFake(() => of({ ...mockProduct }));
    productServiceSpy.getSpecsCatalog.and.callFake(() => of({}));

    await buildTestBed(null);

    fixture = TestBed.createComponent(CreateEditProductComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Create mode ───────────────────────────────────────────────────────────────

  describe('create mode (no productId)', () => {
    it('should have null productId signal', () => {
      expect(component.productId()).toBeNull();
    });

    it('should NOT call getProductById in create mode', () => {
      expect(productServiceSpy.getProductById).not.toHaveBeenCalled();
    });

    it('should call getAllCategories on init', () => {
      expect(categoryServiceSpy.getAllCategories).toHaveBeenCalled();
    });

    it('should populate categories from service', () => {
      expect(component.categories.length).toBe(1);
      expect(component.categories[0].label).toBe('Electrónica');
    });

    it('should filter out "Otros" category from the tree', () => {
      categoryServiceSpy.getAllCategories.and.callFake(() =>
        of([{ ...mockCategory }, { ...mockCategoryOtros }])
      );
      component.loadData();
      expect(component.categories.some(c => c.label?.toLowerCase() === 'otros')).toBeFalse();
      expect(component.categories.some(c => c.label === 'Electrónica')).toBeTrue();
    });

    it('should set loading=false after categories load', () => {
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    });

    it('should set error=true when getAllCategories fails', () => {
      categoryServiceSpy.getAllCategories.and.callFake(() => throwError(() => new Error('fail')));
      component.loadData();
      expect(component.loading).toBeFalse();
      expect(component.error).toBeTrue();
    });

    it('should initialise form with empty name, zero prices and active=true', () => {
      expect(component.productForm.get('name')?.value).toBe('');
      expect(component.productForm.get('supplyPrice')?.value).toBe(0);
      expect(component.productForm.get('currentPrice')?.value).toBe(0);
      expect(component.productForm.get('active')?.value).toBeTrue();
    });

    it('referenceCode should be disabled', () => {
      expect(component.productForm.get('referenceCode')?.disabled).toBeTrue();
    });

    it('should have empty existingImages and newImages initially', () => {
      expect(component.existingImages()).toEqual([]);
      expect(component.newImages()).toEqual([]);
    });
  });

  // ── Edit mode ─────────────────────────────────────────────────────────────────

  describe('edit mode (with productId)', () => {
    beforeEach(() => {
      component.productId.set('prod-1');
      categoryServiceSpy.getAllCategories.calls.reset();
      productServiceSpy.getProductById.calls.reset();
      component.loadData();
    });

    it('should call getProductById with productId', () => {
      expect(productServiceSpy.getProductById).toHaveBeenCalledWith('prod-1');
    });

    it('should call getAllCategories via forkJoin', () => {
      expect(categoryServiceSpy.getAllCategories).toHaveBeenCalled();
    });

    it('should set the product signal', () => {
      expect(component.product()).toEqual(jasmine.objectContaining({ id: 'prod-1' }));
    });

    it('should patch form with product name and prices', () => {
      expect(component.productForm.get('name')?.value).toBe('Producto Test');
      expect(component.productForm.get('supplyPrice')?.value).toBe(10);
      expect(component.productForm.get('currentPrice')?.value).toBe(15);
    });

    it('should set existingImages from product imagesInfo', () => {
      expect(component.existingImages().length).toBe(1);
      expect(component.existingImages()[0].imageUrl).toBe('http://img.jpg');
    });

    it('should set loading=false and no error after success', () => {
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    });

    it('should set error=true when forkJoin fails', () => {
      productServiceSpy.getProductById.and.callFake(() => throwError(() => new Error('fail')));
      component.loadData();
      expect(component.loading).toBeFalse();
      expect(component.error).toBeTrue();
    });

    it('should populate selectedCategories from product categories', () => {
      expect(component.selectedCategories.length).toBe(1);
      expect(component.selectedCategories[0].label).toBe('Electrónica');
    });

    it('should filter "Otros" out of product categories', () => {
      productServiceSpy.getProductById.and.callFake(() =>
        of({ ...mockProduct, categories: [{ ...mockCategory }, { ...mockCategoryOtros }] })
      );
      component.loadData();
      expect(component.selectedCategories.some(c => c.label?.toLowerCase() === 'otros')).toBeFalse();
    });
  });

  // ── reloadAll ─────────────────────────────────────────────────────────────────

  describe('reloadAll', () => {
    it('should reset form fields in create mode', () => {
      component.productForm.patchValue({ name: 'Modified', supplyPrice: 99 });
      component.reloadAll();
      expect(component.productForm.get('supplyPrice')?.value).toBe(0);
    });

    it('should clear existingImages and newImages in create mode', () => {
      component.existingImages.set([{ ...mockImageInfo }]);
      component.newImages.set([{ file: new File(['x'], 'x.jpg'), previewUrl: 'blob:x' }]);
      component.reloadAll();
      expect(component.existingImages()).toEqual([]);
      expect(component.newImages()).toEqual([]);
    });

    it('should clear selectedCategories in create mode', () => {
      component.selectedCategories = [{ key: '0', label: 'Test' }];
      component.reloadAll();
      expect(component.selectedCategories).toEqual([]);
    });

    it('should call loadData after reset in create mode', () => {
      categoryServiceSpy.getAllCategories.calls.reset();
      component.reloadAll();
      expect(categoryServiceSpy.getAllCategories).toHaveBeenCalled();
    });

    it('should clear error flag', () => {
      component.error = true;
      component.reloadAll();
      expect(component.error).toBeFalse();
    });

    it('should re-patch form from service in edit mode (not reset)', () => {
      component.productId.set('prod-1');
      component.loadData();
      component.productForm.patchValue({ name: 'Modified' });
      component.reloadAll();
      expect(component.productForm.get('name')?.value).toBe('Producto Test');
    });
  });

  // ── onFileSelect ──────────────────────────────────────────────────────────────

  describe('onFileSelect', () => {
    beforeEach(() => {
      spyOn(URL, 'createObjectURL').and.returnValue('blob:fake-url');
    });

    it('should add a valid file to newImages', () => {
      const file = new File(['content'], 'photo.jpg', { type: 'image/jpeg' });
      component.onFileSelect({ files: [file] });
      expect(component.newImages().length).toBe(1);
      expect(component.newImages()[0].file).toBe(file);
    });

    it('should ignore files that exceed MAX_SIZE', () => {
      const oversized = { size: component['MAX_SIZE'] + 1, name: 'big.jpg' } as File;
      component.onFileSelect({ files: [oversized] });
      expect(component.newImages().length).toBe(0);
    });

    it('should add multiple valid files at once', () => {
      const file1 = new File(['a'], 'a.jpg');
      const file2 = new File(['b'], 'b.jpg');
      component.onFileSelect({ files: [file1, file2] });
      expect(component.newImages().length).toBe(2);
    });

    it('should accumulate images across multiple calls', () => {
      const file1 = new File(['a'], 'a.jpg');
      const file2 = new File(['b'], 'b.jpg');
      component.onFileSelect({ files: [file1] });
      component.onFileSelect({ files: [file2] });
      expect(component.newImages().length).toBe(2);
    });

    it('should only add valid files when mixed with oversized ones', () => {
      const valid = new File(['a'], 'a.jpg');
      const oversized = { size: component['MAX_SIZE'] + 1, name: 'big.jpg' } as File;
      component.onFileSelect({ files: [valid, oversized] });
      expect(component.newImages().length).toBe(1);
    });
  });

  // ── removeNewImage ────────────────────────────────────────────────────────────

  describe('removeNewImage', () => {
    it('should remove the image at the given index', () => {
      component.newImages.set([
        { file: new File(['a'], 'a.jpg'), previewUrl: 'blob:a' },
        { file: new File(['b'], 'b.jpg'), previewUrl: 'blob:b' }
      ]);
      component.removeNewImage(0);
      expect(component.newImages().length).toBe(1);
      expect(component.newImages()[0].previewUrl).toBe('blob:b');
    });

    it('should handle removing the last image', () => {
      component.newImages.set([{ file: new File(['x'], 'x.jpg'), previewUrl: 'blob:x' }]);
      component.removeNewImage(0);
      expect(component.newImages()).toEqual([]);
    });
  });

  // ── removeExistingImage ───────────────────────────────────────────────────────

  describe('removeExistingImage', () => {
    it('should remove the existing image at the given index', () => {
      const img1: ImageInfo = { id: 'i1', imageUrl: 'http://1.jpg', s3Key: 'k1', fileName: 'f1.jpg' };
      const img2: ImageInfo = { id: 'i2', imageUrl: 'http://2.jpg', s3Key: 'k2', fileName: 'f2.jpg' };
      component.existingImages.set([img1, img2]);
      component.removeExistingImage(0);
      expect(component.existingImages().length).toBe(1);
      expect(component.existingImages()[0].id).toBe('i2');
    });

    it('should handle removing the last existing image', () => {
      component.existingImages.set([{ ...mockImageInfo }]);
      component.removeExistingImage(0);
      expect(component.existingImages()).toEqual([]);
    });
  });

  // ── onSubmit — invalid form ───────────────────────────────────────────────────

  describe('onSubmit — invalid form', () => {
    it('should show error and not call any service when form is invalid', () => {
      component.productForm.patchValue({ name: '', supplyPrice: 0, currentPrice: 0 });
      component.onSubmit();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(productServiceSpy.createProduct).not.toHaveBeenCalled();
      expect(productServiceSpy.updateProduct).not.toHaveBeenCalled();
    });
  });

  // ── onSubmit — create mode ────────────────────────────────────────────────────

  describe('onSubmit — create mode', () => {
    beforeEach(() => {
      component.productId.set(null);
      fillValidForm(component);
    });

    it('should call createProduct with form data', () => {
      component.onSubmit();
      expect(productServiceSpy.createProduct).toHaveBeenCalled();
    });

    it('should show success message after createProduct', () => {
      component.onSubmit();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should call updateProductImages after createProduct success', () => {
      component.onSubmit();
      expect(productServiceSpy.updateProductImages).toHaveBeenCalledWith(
        'prod-1', jasmine.any(Array), jasmine.any(Array)
      );
    });

    it('should navigate to /admin/products after updateProductImages', () => {
      component.onSubmit();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/products']);
    });

    it('should show error on createProduct failure', () => {
      productServiceSpy.createProduct.and.callFake(() => throwError(() => new Error('fail')));
      component.onSubmit();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
  });

  // ── onSubmit — edit mode ──────────────────────────────────────────────────────

  describe('onSubmit — edit mode', () => {
    beforeEach(() => {
      component.productId.set('prod-1');
      component.loadData();
      fillValidForm(component);
    });

    it('should call updateProduct with productId', () => {
      component.onSubmit();
      expect(productServiceSpy.updateProduct).toHaveBeenCalledWith('prod-1', jasmine.any(Object));
    });

    it('should show success message after updateProduct', () => {
      component.onSubmit();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should call updateProductImages after updateProduct success', () => {
      component.onSubmit();
      expect(productServiceSpy.updateProductImages).toHaveBeenCalledWith(
        'prod-1', jasmine.any(Array), jasmine.any(Array)
      );
    });

    it('should navigate to /admin/products after updateProductImages', () => {
      component.onSubmit();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/products']);
    });

    it('should show error on updateProduct failure', () => {
      productServiceSpy.updateProduct.and.callFake(() => throwError(() => new Error('fail')));
      component.onSubmit();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should NOT call updateProduct if product signal is null', () => {
      component.product.set(null);
      component.onSubmit();
      expect(productServiceSpy.updateProduct).not.toHaveBeenCalled();
    });
  });

  // ── updateProductImages ───────────────────────────────────────────────────────

  describe('updateProductImages', () => {
    it('should call productService.updateProductImages with id, existingImages and mapped files', () => {
      component.existingImages.set([{ ...mockImageInfo }]);
      component.newImages.set([{ file: new File(['x'], 'x.jpg'), previewUrl: 'blob:x' }]);
      component.updateProductImages('prod-1');
      expect(productServiceSpy.updateProductImages).toHaveBeenCalledWith(
        'prod-1',
        jasmine.arrayContaining([jasmine.objectContaining({ id: 'img-1' })]),
        jasmine.any(Array)
      );
    });

    it('should navigate to /admin/products on success', () => {
      component.updateProductImages('prod-1');
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/products']);
    });

    it('should show "Imágenes actualizadas" in create mode (productId null)', () => {
      component.productId.set(null);
      component.updateProductImages('prod-1');
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({
        detail: jasmine.stringContaining('Imágenes actualizadas')
      }));
    });

    it('should show "Imágenes del producto actualizadas correctamente" in edit mode', () => {
      component.productId.set('prod-1');
      component.updateProductImages('prod-1');
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({
        detail: jasmine.stringContaining('Imágenes del producto actualizadas correctamente')
      }));
    });
  });

  // ── Form validation ───────────────────────────────────────────────────────────

  describe('form validation', () => {
    it('should be invalid when name is empty', () => {
      component.productForm.patchValue({ name: '' });
      expect(component.productForm.get('name')?.invalid).toBeTrue();
    });

    it('should be invalid when name has fewer than 3 chars', () => {
      component.productForm.patchValue({ name: 'AB' });
      expect(component.productForm.get('name')?.invalid).toBeTrue();
    });

    it('should be valid when name has at least 3 chars', () => {
      component.productForm.patchValue({ name: 'ABC' });
      expect(component.productForm.get('name')?.valid).toBeTrue();
    });

    it('should be invalid when supplyPrice is 0', () => {
      component.productForm.patchValue({ supplyPrice: 0 });
      expect(component.productForm.get('supplyPrice')?.invalid).toBeTrue();
    });

    it('should be valid when supplyPrice > 0', () => {
      component.productForm.patchValue({ supplyPrice: 0.01 });
      expect(component.productForm.get('supplyPrice')?.valid).toBeTrue();
    });

    it('should be invalid when currentPrice is 0', () => {
      component.productForm.patchValue({ currentPrice: 0 });
      expect(component.productForm.get('currentPrice')?.invalid).toBeTrue();
    });

    it('should be valid when currentPrice > 0', () => {
      component.productForm.patchValue({ currentPrice: 0.01 });
      expect(component.productForm.get('currentPrice')?.valid).toBeTrue();
    });

    it('getRawValue should include disabled referenceCode', () => {
      const raw = component.productForm.getRawValue();
      expect('referenceCode' in raw).toBeTrue();
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

    it('should show "Nuevo Producto" in create mode', () => {
      component.productId.set(null);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Nuevo Producto');
    });

    it('should show "Editar Producto" in edit mode', () => {
      component.productId.set('prod-1');
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Editar Producto');
    });

    it('should render breadcrumb-reload component', () => {
      expect(fixture.nativeElement.querySelector('app-breadcrumb-reload')).toBeTruthy();
    });
  });
});
