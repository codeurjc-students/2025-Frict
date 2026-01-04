import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ProductService} from '../../../services/product.service';
import {of, throwError} from 'rxjs';
import {By} from '@angular/platform-browser';
import {provideRouter} from '@angular/router';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {provideHttpClient} from '@angular/common/http';
import {ClientHomeComponent} from './client-home.component';
import {Product} from '../../../models/product.model';
import {Category} from '../../../models/category.model';

describe('ClientHomeComponent', () => {
  let fixture: ComponentFixture<ClientHomeComponent>;
  let component: ClientHomeComponent;
  let mockProductService: jasmine.SpyObj<ProductService>;

  beforeEach(async () => {
    mockProductService = jasmine.createSpyObj('ProductService', [
      'getAllProducts',
      'getProductsByCategoryName'
    ]);

    mockProductService.getAllProducts.calls.reset();
    mockProductService.getProductsByCategoryName.calls.reset();

    await TestBed.configureTestingModule({
      imports: [ClientHomeComponent],
      providers: [
        { provide: ProductService, useValue: mockProductService },
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClientHomeComponent);
    component = fixture.componentInstance;
  });

  it('debe mostrar mensaje cuando no hay productos', async () => {
    mockProductService.getAllProducts.and.returnValue(
      of({ products: [], totalProducts: 0, currentPage: 0, lastPage: -1, pageSize: 0 })
    );

    mockProductService.getProductsByCategoryName.and.returnValue(
      of({ products: [], totalProducts: 0, currentPage: 0, lastPage: -1, pageSize: 0 })
    );

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const nativeElement: HTMLElement = fixture.nativeElement;
    expect(nativeElement.textContent).toContain('No hay productos destacados disponibles.');
  });

  it('debe mostrar los productos cuando el servicio devuelve datos', async () => {

    const allMockCategories: Category[] = [
      { id: '1', name: 'Categoría 1', imageUrl: '/api/v1/categories/image/1', icon: '', bannerText: '', shortDescription: '', longDescription: '', parentId: '', children: [] }
    ];

    const allMockProducts: Product[] = [
      { id: '1', referenceCode: 'ABC', name: 'Producto 1', description: 'Desc 1', currentPrice: 100, imageUrls: [], previousPrice: 0.0, discount: "0%", categories: [allMockCategories[0]], availableUnits: 0, averageRating: 0.0, totalReviews: 0},
      { id: '2', referenceCode: 'DEF', name: 'Producto 2', description: 'Desc 2', currentPrice: 200, imageUrls: [], previousPrice: 0.0, discount: "0%", categories: [allMockCategories[0]], availableUnits: 0, averageRating: 0.0, totalReviews: 0}
    ];

    mockProductService.getAllProducts.and.returnValue(
      of({ products: allMockProducts, totalProducts: 2, currentPage: 0, lastPage: -1, pageSize: 8 })
    );

    mockProductService.getProductsByCategoryName.and.callFake((name: string) => {
      const productsCopy = allMockProducts.map(p => ({ ...p })); // Deepcopy to avoid duplicates
      switch (name) {
        case 'Destacado':
          return of({ products: productsCopy.filter(p => p.categories.includes(allMockCategories[0])), totalProducts: 2, currentPage: 0, lastPage: 0, pageSize: 8 });
        case 'Top ventas':
          return of({ products: [], totalProducts: 0, currentPage: 0, lastPage: -1, pageSize: 8 });
        case 'Recomendado':
          return of({ products: [], totalProducts: 0, currentPage: 0, lastPage: -1, pageSize: 8 });
        default:
          return of({ products: [], totalProducts: 0, currentPage: 0, lastPage: -1, pageSize: 8 });
      }
    });

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    // Only featured products is selected
    const items = fixture.debugElement.queryAll(
      By.css('#featured-content [id^="featuredProduct-"]')
    );
    const nativeElement: HTMLElement = fixture.nativeElement;
    expect(nativeElement.textContent).toContain('Producto 1');
    expect(nativeElement.textContent).toContain('Producto 2');
  });

  it('debe manejar error en el servicio para cada categoría', async () => {
    mockProductService.getProductsByCategoryName.and.callFake((name: string) =>
      throwError(() => new Error(name + ' category not found.'))
    );

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    // The arrays should be empty
    expect(component.featuredProducts.length).toBe(0);
    expect(component.topSalesProducts.length).toBe(0);
    expect(component.recommendedProducts.length).toBe(0);

    // The error flags should be activated
    expect(component.featuredError).toBeTrue();
    expect(component.topSalesError).toBeTrue();
    expect(component.recommendedError).toBeTrue();

    // The loading process should end
    expect(component.featuredLoading).toBeFalse();
    expect(component.topSalesLoading).toBeFalse();
    expect(component.recommendedLoading).toBeFalse();
  });
});
