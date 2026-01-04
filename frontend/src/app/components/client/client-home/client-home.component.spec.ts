import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ClientHomeComponent } from './client-home.component';
import { ProductService } from '../../../services/product.service';

import { MessageService, ConfirmationService } from 'primeng/api';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { Product } from '../../../models/product.model';
import { Category } from '../../../models/category.model';

describe('ClientHomeComponent', () => {
  let fixture: ComponentFixture<ClientHomeComponent>;
  let component: ClientHomeComponent;

  let mockProductService: jasmine.SpyObj<ProductService>;

  beforeEach(async () => {
    mockProductService = jasmine.createSpyObj('ProductService', [
      'getAllProducts',
      'getProductsByCategoryName'
    ]);

    await TestBed.configureTestingModule({
      imports: [ClientHomeComponent],
      providers: [
        // Mocks
        { provide: ProductService, useValue: mockProductService },

        // Real dependencies
        MessageService,
        ConfirmationService,
        provideRouter([]),
        provideHttpClient(),       // Included in case any component requests real HTTP interaction
        provideHttpClientTesting(),
        provideAnimations() // Necessary for PrimeNG
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClientHomeComponent);
    component = fixture.componentInstance;

    mockProductService.getAllProducts.calls.reset();
    mockProductService.getProductsByCategoryName.calls.reset();
  });

  // Tests
  it('debe mostrar mensaje cuando no hay productos', async () => {
    const emptyResponse = { products: [], totalProducts: 0, currentPage: 0, lastPage: -1, pageSize: 0 };
    mockProductService.getAllProducts.and.returnValue(of(emptyResponse));
    mockProductService.getProductsByCategoryName.and.returnValue(of(emptyResponse));

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const nativeElement: HTMLElement = fixture.nativeElement;
    expect(nativeElement.textContent).toContain('No hay productos destacados disponibles');
  });

  it('debe mostrar los productos cuando el servicio devuelve datos', async () => {
    const mockCategory: Category = { id: '1', name: 'Cat1', imageUrl: '', icon: '', bannerText: '', shortDescription: '', longDescription: '', parentId: '', children: [] };

    const mockProducts: Product[] = [
      { id: '1', referenceCode: 'A', name: 'Producto A', description: 'Desc', currentPrice: 100, imageUrls: [], previousPrice: 0, discount: "0%", categories: [mockCategory], availableUnits: 10, averageRating: 5, totalReviews: 1},
      { id: '2', referenceCode: 'B', name: 'Producto B', description: 'Desc', currentPrice: 200, imageUrls: [], previousPrice: 0, discount: "0%", categories: [mockCategory], availableUnits: 5, averageRating: 4, totalReviews: 2}
    ];

    mockProductService.getAllProducts.and.returnValue(of({ products: mockProducts, totalProducts: 2, currentPage: 0, lastPage: 0, pageSize: 10 }));

    mockProductService.getProductsByCategoryName.and.callFake((name) => {
      if (name === 'Destacado') return of({ products: mockProducts, totalProducts: 2, currentPage: 0, lastPage: 0, pageSize: 10 });
      return of({ products: [], totalProducts: 0, currentPage: 0, lastPage: 0, pageSize: 0 });
    });

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const textContent = fixture.nativeElement.textContent;
    expect(textContent).toContain('Producto A');
    expect(textContent).toContain('Producto B');
  });

  it('debe manejar error en el servicio', async () => {
    mockProductService.getAllProducts.and.returnValue(of({ products: [], totalProducts: 0, currentPage: 0, lastPage: 0, pageSize: 0 }));
    mockProductService.getProductsByCategoryName.and.returnValue(
      throwError(() => new Error('Error de red'))
    );

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(component.featuredError).toBeTrue();
    expect(component.featuredLoading).toBeFalse();
    expect(component.featuredProducts.length).toBe(0);
  });
});
