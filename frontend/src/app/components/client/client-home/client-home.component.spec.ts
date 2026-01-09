import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ClientHomeComponent } from './client-home.component';
import { ProductService } from '../../../services/product.service';
import { CategoryService } from '../../../services/category.service';
import { of, throwError } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {Category} from '../../../models/category.model';

describe('ClientHomeComponent', () => {
  let component: ClientHomeComponent;
  let fixture: ComponentFixture<ClientHomeComponent>;

  // Spies para los servicios
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let categoryServiceSpy: jasmine.SpyObj<CategoryService>;

  beforeEach(async () => {
    // 1. Crear los Mocks
    productServiceSpy = jasmine.createSpyObj('ProductService', ['getProductsByCategoryName']);
    categoryServiceSpy = jasmine.createSpyObj('CategoryService', ['getAllCategories']);

    await TestBed.configureTestingModule({
      imports: [
        ClientHomeComponent,
        BrowserAnimationsModule // Necesario porque usas PrimeNG/Carousel
      ],
      providers: [
        { provide: ProductService, useValue: productServiceSpy },
        { provide: CategoryService, useValue: categoryServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => null } } } // Mock básico de ruta
        },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClientHomeComponent);
    component = fixture.componentInstance;

    // IMPORTANTE: Configuración base por defecto para que no se bloquee la cadena
    categoryServiceSpy.getAllCategories.and.returnValue(of([
      { id: '1', name: 'Ordenadores', imageUrl: '', icon: '', bannerText: '', shortDescription: '', longDescription: '', parentId: '', children: [] },
      { id: '2', name: 'Periféricos', imageUrl: '', icon: '', bannerText: '', shortDescription: '', longDescription: '', parentId: '', children: [] }
    ]));
  });

  it('debe mostrar los productos cuando el servicio devuelve datos', () => {
    const mockCategory: Category = { id: '1', name: 'Cat1', imageUrl: '', icon: '', bannerText: '', shortDescription: '', longDescription: '', parentId: '', children: [] };
    const mockProducts = {
      items: [
        { id: '1', referenceCode: 'A', name: 'Producto A', description: 'Desc', currentPrice: 100, imageUrls: [], previousPrice: 0, discount: "0%", categories: [mockCategory], availableUnits: 10, averageRating: 5, totalReviews: 1},
        { id: '2', referenceCode: 'B', name: 'Producto B', description: 'Desc', currentPrice: 200, imageUrls: [], previousPrice: 0, discount: "0%", categories: [mockCategory], availableUnits: 5, averageRating: 4, totalReviews: 2}
      ]
    };

    productServiceSpy.getProductsByCategoryName.and.returnValue(of(mockProducts as any));

    fixture.detectChanges();

    // Assert
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Producto A');
    expect(compiled.textContent).toContain('Producto B');
    expect(component.featuredLoading).toBeFalse();
  });

  it('debe mostrar mensaje cuando no hay productos (array vacío)', () => {
    const emptyProducts = { items: [] };
    productServiceSpy.getProductsByCategoryName.and.returnValue(of(emptyProducts as any));

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(component.featuredLoading).toBeFalse();
    expect(compiled.textContent).toContain('No hay productos destacados disponibles');
  });

  it('debe manejar error en el servicio', () => {
    // Arrange: Simulamos error en el servicio de productos
    productServiceSpy.getProductsByCategoryName.and.returnValue(throwError(() => new Error('Error de servidor')));

    // Act
    fixture.detectChanges();

    // Assert
    expect(component.featuredLoading).toBeFalse(); // El loading debe apagarse incluso en error
    expect(component.featuredError).toBeTrue();    // El flag de error debe activarse
  });
});
