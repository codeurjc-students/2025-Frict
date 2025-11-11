import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductService } from '../../../services/product.service';
import { of, throwError } from 'rxjs';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import {ClientHomeComponent} from './client-home.component';

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
    const allMockProducts = [
      { id: 1, referenceCode: 'ABC', name: 'Producto 1', description: 'Desc 1', currentPrice: 100, imageUrl: '', previousPrice: 0.0, discount: "0%", categoriesId: [14], availableUnits: 0, averageRating: 0.0, totalReviews: 0 },
      { id: 2, referenceCode: 'DEF', name: 'Producto 2', description: 'Desc 2', currentPrice: 200, imageUrl: '', previousPrice: 0.0, discount: "0%", categoriesId: [14], availableUnits: 0, averageRating: 0.0, totalReviews: 0 }
    ];

    mockProductService.getAllProducts.and.returnValue(
      of({ products: allMockProducts, totalProducts: 2, currentPage: 0, lastPage: -1, pageSize: 8 })
    );

    mockProductService.getProductsByCategoryName.and.callFake((name: string) => {
      const productsCopy = allMockProducts.map(p => ({ ...p })); // copias independientes para evitar duplicados
      switch (name) {
        case 'Destacado':
          return of({ products: productsCopy.filter(p => p.categoriesId.includes(14)), totalProducts: 2, currentPage: 0, lastPage: 0, pageSize: 8 });
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

    // Solo seleccionamos la sección de productos destacados
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

    // Los arrays deben quedar vacíos
    expect(component.featuredProducts.length).toBe(0);
    expect(component.topSalesProducts.length).toBe(0);
    expect(component.recommendedProducts.length).toBe(0);

    // Los flags de error deben activarse
    expect(component.featuredError).toBeTrue();
    expect(component.topSalesError).toBeTrue();
    expect(component.recommendedError).toBeTrue();

    // El loading debe terminar
    expect(component.featuredLoading).toBeFalse();
    expect(component.topSalesLoading).toBeFalse();
    expect(component.recommendedLoading).toBeFalse();
  });
});
