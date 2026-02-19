import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ClientHomeComponent } from './client-home.component';
import { ProductService } from '../../../services/product.service';
import { CategoryService } from '../../../services/category.service';
import { of, throwError } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Category } from '../../../models/category.model';
import { ImageInfo } from '../../../models/imageInfo.model';
import { PageResponse } from '../../../models/pageResponse.model';
import { Product } from '../../../models/product.model';

describe('ClientHomeComponent', () => {
  let component: ClientHomeComponent;
  let fixture: ComponentFixture<ClientHomeComponent>;

  // Spies for services
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let categoryServiceSpy: jasmine.SpyObj<CategoryService>;

  beforeEach(async () => {
    // 1. Create Mocks
    productServiceSpy = jasmine.createSpyObj('ProductService', ['getProductsByCategoryName']);
    categoryServiceSpy = jasmine.createSpyObj('CategoryService', ['getAllCategories']);

    await TestBed.configureTestingModule({
      imports: [
        ClientHomeComponent,
        BrowserAnimationsModule // Required for PrimeNG/Carousel usage
      ],
      providers: [
        { provide: ProductService, useValue: productServiceSpy },
        { provide: CategoryService, useValue: categoryServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => null } } } // Basic route mock
        },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClientHomeComponent);
    component = fixture.componentInstance;

    // IMPORTANT: Base default configuration to prevent stream blocking
    const mockImageInfo: ImageInfo = { id: '1', imageUrl: '', s3Key: '', fileName: ''};
    categoryServiceSpy.getAllCategories.and.returnValue(of([
      { id: '1', name: 'Ordenadores', imageInfo: mockImageInfo, icon: '', timesUsed: 0, bannerText: '', shortDescription: '', longDescription: '', parentId: '', children: [] },
      { id: '2', name: 'Periféricos', imageInfo: mockImageInfo, icon: '', timesUsed: 0, bannerText: '', shortDescription: '', longDescription: '', parentId: '', children: [] }
    ]));
  });

  it('should display products when service returns data', () => {
    const mockImageInfo: ImageInfo = { id: '1', imageUrl: '', s3Key: '', fileName: ''};
    const mockCategory: Category = { id: '1', name: 'Cat1', imageInfo: mockImageInfo, icon: '', timesUsed: 0, bannerText: '', shortDescription: '', longDescription: '', parentId: '', children: [] };
    const mockProducts : PageResponse<Product> = {
      items: [
        { id: '1', referenceCode: 'A', name: 'Producto A', description: 'Desc', currentPrice: 100, imagesInfo: [mockImageInfo], previousPrice: 0, discount: "0%", categories: [mockCategory], active: true, totalUnits: 30, shopsWithStock: 3, averageRating: 5, totalReviews: 1},
        { id: '2', referenceCode: 'B', name: 'Producto B', description: 'Desc', currentPrice: 200, imagesInfo: [mockImageInfo], previousPrice: 0, discount: "0%", categories: [mockCategory], active: true, totalUnits: 30, shopsWithStock: 3, averageRating: 4, totalReviews: 2}
      ],
      totalItems: 2,
      currentPage: 0,
      lastPage: 0,
      pageSize: 5
    };

    productServiceSpy.getProductsByCategoryName.and.returnValue(of(mockProducts as any));

    fixture.detectChanges();

    // Assert
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Producto A');
    expect(compiled.textContent).toContain('Producto B');
    expect(component.featuredLoading).toBeFalse();
  });

  it('should show message when there are no products (empty array)', () => {
    const emptyProducts = { items: [] };
    productServiceSpy.getProductsByCategoryName.and.returnValue(of(emptyProducts as any));

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(component.featuredLoading).toBeFalse();
    expect(compiled.textContent).toContain('No hay productos destacados disponibles');
  });

  it('should handle service errors', () => {
    // Arrange: Simulate error in product service
    productServiceSpy.getProductsByCategoryName.and.returnValue(throwError(() => new Error('Server Error')));

    // Act
    fixture.detectChanges();

    // Assert
    expect(component.featuredLoading).toBeFalse(); // Loading must be turned off even on error
    expect(component.featuredError).toBeTrue();    // Error flag must be activated
  });
});
