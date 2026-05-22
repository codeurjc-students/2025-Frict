import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {MessageService} from 'primeng/api';

import {CreateEditCategoryComponent} from './create-edit-category.component';
import {CategoryService} from '../../../services/category.service';
import {UiService} from '../../../utils/ui.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {Category} from '../../../models/category.model';
import {ImageInfo} from '../../../models/imageInfo.model';

// ── Mock data ──────────────────────────────────────────────────────────────────

const mockImageInfo: ImageInfo = {
  id: 'img-1', imageUrl: 'http://img.jpg', s3Key: 'k1', fileName: 'f.jpg'
};

const mockChildCategory: Category = {
  id: 'cat-child', name: 'Subcategoría', icon: 'pi pi-tag', bannerText: '',
  shortDescription: '', longDescription: '', imageInfo: { ...mockImageInfo },
  timesUsed: 0, parentId: 'cat-1', children: []
};

const mockCategory: Category = {
  id: 'cat-1', name: 'Electrónica', icon: 'pi pi-bolt', bannerText: 'Oferta',
  shortDescription: 'Descripción corta', longDescription: '<p>Larga</p>',
  imageInfo: { ...mockImageInfo }, timesUsed: 5,
  parentId: '', children: [{ ...mockChildCategory }]
};

const mockOtherCategory: Category = {
  id: 'cat-2', name: 'Moda', icon: 'pi pi-tag', bannerText: '',
  shortDescription: '', longDescription: '', imageInfo: { ...mockImageInfo },
  timesUsed: 0, parentId: '', children: []
};

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('CreateEditCategoryComponent', () => {
  let component: CreateEditCategoryComponent;
  let fixture: ComponentFixture<CreateEditCategoryComponent>;
  let categoryServiceSpy: jasmine.SpyObj<CategoryService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let routerEvents$: Subject<any>;

  function buildTestBed(categoryId: string | null = null, parentIdParam: string | null = null) {
    return TestBed.configureTestingModule({
      imports: [CreateEditCategoryComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: CategoryService, useValue: categoryServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: (key: string) => key === 'id' ? categoryId : null },
              queryParamMap: { get: (key: string) => key === 'parentId' ? parentIdParam : null },
              url: [], data: {}
            },
            root: { children: [] }
          }
        },
        { provide: UiService, useValue: { AVAILABLE_ICONS: [] } },
        BreadcrumbService
      ]
    }).compileComponents();
  }

  beforeEach(async () => {
    routerEvents$ = new Subject<any>();

    categoryServiceSpy = jasmine.createSpyObj('CategoryService', [
      'getAllCategories', 'getCategoryById',
      'createCategory', 'updateCategory', 'updateCategoryImage'
    ]);
    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: routerEvents$.asObservable(),
      url: '/admin/categories/new'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    categoryServiceSpy.getAllCategories.and.callFake(() =>
      of([{ ...mockCategory, children: [{ ...mockChildCategory }] }, { ...mockOtherCategory }])
    );
    categoryServiceSpy.getCategoryById.and.callFake(() =>
      of({ ...mockCategory, children: [{ ...mockChildCategory }] })
    );
    categoryServiceSpy.createCategory.and.callFake(() => of({ ...mockCategory }));
    categoryServiceSpy.updateCategory.and.callFake(() => of({ ...mockCategory }));
    categoryServiceSpy.updateCategoryImage.and.callFake(() => of({ ...mockCategory }));

    await buildTestBed(null);

    fixture = TestBed.createComponent(CreateEditCategoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Create mode ───────────────────────────────────────────────────────────────

  describe('create mode (no categoryId)', () => {
    it('should have null categoryId signal', () => {
      expect(component.categoryId()).toBeNull();
    });

    it('should NOT call getCategoryById', () => {
      expect(categoryServiceSpy.getCategoryById).not.toHaveBeenCalled();
    });

    it('should call getAllCategories on init', () => {
      expect(categoryServiceSpy.getAllCategories).toHaveBeenCalled();
    });

    it('should set loading=false after data loads', () => {
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    });

    it('should set error=true when forkJoin fails', () => {
      categoryServiceSpy.getAllCategories.and.callFake(() => throwError(() => new Error('fail')));
      component.loadData(null, null);
      expect(component.loading).toBeFalse();
      expect(component.error).toBeTrue();
    });

    it('should show error message when forkJoin fails', () => {
      categoryServiceSpy.getAllCategories.and.callFake(() => throwError(() => new Error('fail')));
      component.loadData(null, null);
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });

    it('should populate flatCategoriesList from getAllCategories', () => {
      expect(component.flatCategoriesList().length).toBeGreaterThan(0);
    });

    it('should initialise form with empty name and default icon', () => {
      expect(component.categoryForm.get('name')?.value).toBe('');
      expect(component.categoryForm.get('icon')?.value).toBe('pi pi-folder');
    });

    it('should have null images initially', () => {
      expect(component.oldImage()).toBeNull();
      expect(component.existingImage()).toBeNull();
      expect(component.newImage()).toBeNull();
    });
  });

  // ── Create mode with urlParentId ──────────────────────────────────────────────

  describe('create mode with urlParentId', () => {
    it('should pre-select parentId when parent exists in list', () => {
      categoryServiceSpy.getAllCategories.and.callFake(() => of([{ ...mockOtherCategory }]));
      component.loadData(null, mockOtherCategory.id);
      expect(component.categoryForm.get('parentId')?.value).not.toBeNull();
    });

    it('should NOT pre-select parentId when parent does not exist in list', () => {
      component.loadData(null, 'unknown-id');
      expect(component.categoryForm.get('parentId')?.value).toBeNull();
    });

    it('should set flatCategoriesList even with a urlParentId', () => {
      component.loadData(null, mockOtherCategory.id);
      expect(component.flatCategoriesList().length).toBeGreaterThan(0);
    });
  });

  // ── Edit mode ─────────────────────────────────────────────────────────────────

  describe('edit mode (with categoryId)', () => {
    beforeEach(() => {
      component.categoryId.set('cat-1');
      categoryServiceSpy.getAllCategories.calls.reset();
      categoryServiceSpy.getCategoryById.calls.reset();
      component.loadData('cat-1', null);
    });

    it('should call getCategoryById with the categoryId', () => {
      expect(categoryServiceSpy.getCategoryById).toHaveBeenCalledWith('cat-1');
    });

    it('should patch form with category data', () => {
      expect(component.categoryForm.get('name')?.value).toBe('Electrónica');
      expect(component.categoryForm.get('icon')?.value).toBe('pi pi-bolt');
      expect(component.categoryForm.get('bannerText')?.value).toBe('Oferta');
      expect(component.categoryForm.get('shortDescription')?.value).toBe('Descripción corta');
    });

    it('should set existingImage and oldImage from imageInfo', () => {
      expect(component.existingImage()).toBe('http://img.jpg');
      expect(component.oldImage()).toBe('http://img.jpg');
    });

    it('should set loading=false after success', () => {
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    });

    it('should set error=true on forkJoin failure', () => {
      categoryServiceSpy.getCategoryById.and.callFake(() => throwError(() => new Error('fail')));
      component.loadData('cat-1', null);
      expect(component.loading).toBeFalse();
      expect(component.error).toBeTrue();
    });

    it('should exclude current category from flatCategoriesList', () => {
      expect(component.flatCategoriesList().some(c => c.id === 'cat-1')).toBeFalse();
    });

    it('should exclude children of current category from flatCategoriesList', () => {
      expect(component.flatCategoriesList().some(c => c.id === 'cat-child')).toBeFalse();
    });

    it('should include unrelated categories in flatCategoriesList', () => {
      expect(component.flatCategoriesList().some(c => c.id === 'cat-2')).toBeTrue();
    });

    it('should handle category with null imageInfo gracefully', () => {
      categoryServiceSpy.getCategoryById.and.callFake(() =>
        of({ ...mockCategory, imageInfo: null as any })
      );
      component.loadData('cat-1', null);
      expect(component.existingImage()).toBeNull();
    });
  });

  // ── reloadAll ─────────────────────────────────────────────────────────────────

  describe('reloadAll', () => {
    it('should reset form in create mode', () => {
      component.categoryForm.patchValue({ name: 'Changed', icon: 'pi pi-star' });
      component.reloadAll();
      expect(component.categoryForm.get('icon')?.value).toBe('pi pi-folder');
    });

    it('should clear images in create mode', () => {
      component.newImage.set({ file: new File(['x'], 'x.jpg'), previewUrl: 'blob:x' });
      component.existingImage.set('http://old.jpg');
      component.oldImage.set('http://old.jpg');
      component.reloadAll();
      expect(component.newImage()).toBeNull();
      expect(component.existingImage()).toBeNull();
      expect(component.oldImage()).toBeNull();
    });

    it('should clear orgChartNodes in create mode', () => {
      component.orgChartNodes.set([{ expanded: true, data: {} }]);
      component.reloadAll();
      expect(component.orgChartNodes()).toEqual([]);
    });

    it('should clear error and call loadData', () => {
      component.error = true;
      categoryServiceSpy.getAllCategories.calls.reset();
      component.reloadAll();
      expect(component.error).toBeFalse();
      expect(categoryServiceSpy.getAllCategories).toHaveBeenCalled();
    });

    it('should NOT reset form in edit mode (re-patches from service)', () => {
      component.categoryId.set('cat-1');
      component.loadData('cat-1', null);
      component.categoryForm.patchValue({ name: 'Modified' });
      component.reloadAll();
      expect(component.categoryForm.get('name')?.value).toBe('Electrónica');
    });

    it('should set loading=true before calling loadData', () => {
      let loadingDuringCall = false;
      categoryServiceSpy.getAllCategories.and.callFake(() => {
        loadingDuringCall = component.loading;
        return of([]);
      });
      component.reloadAll();
      expect(loadingDuringCall).toBeTrue();
    });
  });

  // ── flattenCategories ─────────────────────────────────────────────────────────

  describe('flattenCategories', () => {
    it('should flatten tree including children', () => {
      const result = component.flattenCategories([{ ...mockCategory, children: [{ ...mockChildCategory }] }]);
      expect(result.length).toBe(2);
      expect(result[0].name).toBe('Electrónica');
      expect(result[1].name).toBe('Subcategoría');
    });

    it('should return empty array for empty input', () => {
      expect(component.flattenCategories([])).toEqual([]);
    });
  });

  // ── updateOrgChart ────────────────────────────────────────────────────────────

  describe('updateOrgChart', () => {
    beforeEach(() => {
      component.flatCategoriesList.set([{ ...mockCategory, children: [{ ...mockChildCategory }] }]);
    });

    it('should set orgChartNodes when parent category is found', () => {
      component.updateOrgChart('cat-1');
      expect(component.orgChartNodes().length).toBe(1);
      expect(component.orgChartNodes()[0].data.name).toBe('Electrónica');
    });

    it('should clear orgChartNodes when parentId is null', () => {
      component.updateOrgChart(null);
      expect(component.orgChartNodes()).toEqual([]);
    });

    it('should clear orgChartNodes when parent is not found', () => {
      component.updateOrgChart('unknown-id');
      expect(component.orgChartNodes()).toEqual([]);
    });

    it('should clear orgChartNodes when flatCategoriesList is empty', () => {
      component.flatCategoriesList.set([]);
      component.updateOrgChart('cat-1');
      expect(component.orgChartNodes()).toEqual([]);
    });
  });

  // ── mapToOrgChart ─────────────────────────────────────────────────────────────

  describe('mapToOrgChart', () => {
    it('should map a category to an org-chart TreeNode', () => {
      const node = component.mapToOrgChart({ ...mockCategory, children: [{ ...mockChildCategory }] });
      expect(node.data.id).toBe('cat-1');
      expect(node.data.name).toBe('Electrónica');
      expect(node.data.icon).toBe('pi pi-bolt');
      expect(node.children?.length).toBe(1);
    });

    it('should use default icon when category icon is empty', () => {
      const node = component.mapToOrgChart({ ...mockCategory, icon: '' });
      expect(node.data.icon).toBe('pi pi-folder');
    });
  });

  // ── onFileSelect ──────────────────────────────────────────────────────────────

  describe('onFileSelect', () => {
    beforeEach(() => {
      spyOn(URL, 'createObjectURL').and.returnValue('blob:fake-url');
    });

    it('should set newImage and clear existingImage for valid file', () => {
      component.existingImage.set('http://old.jpg');
      const file = new File(['content'], 'photo.jpg', { type: 'image/jpeg' });
      component.onFileSelect({ files: [file] });
      expect(component.newImage()).not.toBeNull();
      expect(component.newImage()!.file).toBe(file);
      expect(component.existingImage()).toBeNull();
    });

    it('should do nothing when file exceeds MAX_SIZE', () => {
      const oversized = { size: component['MAX_SIZE'] + 1, name: 'big.jpg', type: 'image/jpeg' } as File;
      component.onFileSelect({ files: [oversized] });
      expect(component.newImage()).toBeNull();
    });

    it('should replace any previous newImage', () => {
      const file1 = new File(['a'], 'a.jpg', { type: 'image/jpeg' });
      const file2 = new File(['b'], 'b.jpg', { type: 'image/jpeg' });
      component.onFileSelect({ files: [file1] });
      component.onFileSelect({ files: [file2] });
      expect(component.newImage()!.file).toBe(file2);
    });
  });

  // ── onFileRemove ──────────────────────────────────────────────────────────────

  describe('onFileRemove', () => {
    it('should clear newImage', () => {
      component.newImage.set({ file: new File(['x'], 'x.jpg'), previewUrl: 'blob:x' });
      component.onFileRemove({});
      expect(component.newImage()).toBeNull();
    });
  });

  // ── removeImage ───────────────────────────────────────────────────────────────

  describe('removeImage', () => {
    it('should clear both newImage and existingImage', () => {
      component.newImage.set({ file: new File(['x'], 'x.jpg'), previewUrl: 'blob:x' });
      component.existingImage.set('http://img.jpg');
      component.removeImage();
      expect(component.newImage()).toBeNull();
      expect(component.existingImage()).toBeNull();
    });
  });

  // ── restoreImage ──────────────────────────────────────────────────────────────

  describe('restoreImage', () => {
    it('should restore existingImage from oldImage', () => {
      component.oldImage.set('http://img.jpg');
      component.existingImage.set(null);
      component.restoreImage();
      expect(component.existingImage()).toBe('http://img.jpg');
    });

    it('should set existingImage to null when oldImage is null', () => {
      component.oldImage.set(null);
      component.existingImage.set('http://img.jpg');
      component.restoreImage();
      expect(component.existingImage()).toBeNull();
    });
  });

  // ── onSubmit — invalid form ───────────────────────────────────────────────────

  describe('onSubmit — invalid form', () => {
    it('should show warn message and not call any service', () => {
      component.categoryForm.patchValue({ name: '' });
      component.onSubmit();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'warn' }));
      expect(categoryServiceSpy.createCategory).not.toHaveBeenCalled();
      expect(categoryServiceSpy.updateCategory).not.toHaveBeenCalled();
    });

    it('should mark all form controls as touched', () => {
      component.categoryForm.patchValue({ name: '' });
      component.onSubmit();
      expect(component.categoryForm.touched).toBeTrue();
    });
  });

  // ── onSubmit — create mode ────────────────────────────────────────────────────

  describe('onSubmit — create mode', () => {
    beforeEach(() => {
      component.categoryId.set(null);
      component.categoryForm.patchValue({ name: 'Nueva Categoría' });
    });

    it('should call createCategory (not updateCategory)', () => {
      component.onSubmit();
      expect(categoryServiceSpy.createCategory).toHaveBeenCalled();
      expect(categoryServiceSpy.updateCategory).not.toHaveBeenCalled();
    });

    it('should navigate to /admin/categories on success (no image)', () => {
      component.onSubmit();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/categories']);
    });

    it('should show success message when no image', () => {
      component.onSubmit();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should call updateCategoryImage after createCategory when newImage is set', () => {
      component.newImage.set({ file: new File(['x'], 'x.jpg'), previewUrl: 'blob:x' });
      component.onSubmit();
      expect(categoryServiceSpy.updateCategoryImage).toHaveBeenCalledWith('cat-1', jasmine.any(File));
    });

    it('should show error message and NOT navigate on createCategory failure', () => {
      categoryServiceSpy.createCategory.and.callFake(() => throwError(() => new Error('fail')));
      component.onSubmit();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
  });

  // ── onSubmit — edit mode ──────────────────────────────────────────────────────

  describe('onSubmit — edit mode', () => {
    beforeEach(() => {
      component.categoryId.set('cat-1');
      component.loadData('cat-1', null);
    });

    it('should call updateCategory (not createCategory)', () => {
      component.onSubmit();
      expect(categoryServiceSpy.updateCategory).toHaveBeenCalledWith('cat-1', jasmine.any(Object));
      expect(categoryServiceSpy.createCategory).not.toHaveBeenCalled();
    });

    it('should navigate to /admin/categories on success (no image)', () => {
      component.onSubmit();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/categories']);
    });

    it('should call updateCategoryImage after updateCategory when newImage is set', () => {
      component.newImage.set({ file: new File(['x'], 'x.jpg'), previewUrl: 'blob:x' });
      component.onSubmit();
      expect(categoryServiceSpy.updateCategoryImage).toHaveBeenCalledWith('cat-1', jasmine.any(File));
    });

    it('should NOT call updateCategoryImage when no new image', () => {
      component.newImage.set(null);
      component.onSubmit();
      expect(categoryServiceSpy.updateCategoryImage).not.toHaveBeenCalled();
    });

    it('should show error and NOT navigate on updateCategory failure', () => {
      categoryServiceSpy.updateCategory.and.callFake(() => throwError(() => new Error('fail')));
      component.onSubmit();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
  });

  // ── updateCategoryImage ───────────────────────────────────────────────────────

  describe('updateCategoryImage', () => {
    it('should call service and navigate on success', () => {
      (component as any).updateCategoryImage('cat-1', new File(['x'], 'x.jpg'));
      expect(categoryServiceSpy.updateCategoryImage).toHaveBeenCalledWith('cat-1', jasmine.any(File));
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/categories']);
    });

    it('should still navigate on image upload failure (warn severity)', () => {
      categoryServiceSpy.updateCategoryImage.and.callFake(() => throwError(() => new Error('fail')));
      (component as any).updateCategoryImage('cat-1', new File(['x'], 'x.jpg'));
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'warn' }));
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/categories']);
    });
  });

  // ── Form validation ───────────────────────────────────────────────────────────

  describe('form validation', () => {
    it('should be invalid when name is empty', () => {
      component.categoryForm.patchValue({ name: '' });
      expect(component.categoryForm.get('name')?.invalid).toBeTrue();
    });

    it('should be invalid when name has fewer than 3 chars', () => {
      component.categoryForm.patchValue({ name: 'AB' });
      expect(component.categoryForm.get('name')?.invalid).toBeTrue();
    });

    it('should be valid when name has at least 3 chars', () => {
      component.categoryForm.patchValue({ name: 'ABC' });
      expect(component.categoryForm.get('name')?.valid).toBeTrue();
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

    it('should show "Nueva Categoría" in create mode', () => {
      component.categoryId.set(null);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Nueva Categoría');
    });

    it('should show "Editar Categoría" in edit mode', () => {
      component.categoryId.set('cat-1');
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Editar Categoría');
    });

    it('should render breadcrumb-reload component', () => {
      expect(fixture.nativeElement.querySelector('app-breadcrumb-reload')).toBeTruthy();
    });
  });
});
