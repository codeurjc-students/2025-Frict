import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ClientHomeComponent } from './client-home.component';
import { ProductService } from '../../../services/product.service';
import { of, throwError } from 'rxjs';
import { By } from '@angular/platform-browser';

//CLIENT SIDE UNIT TESTS
describe('ClientHomeComponent', () => {
  let fixture: ComponentFixture<ClientHomeComponent>;
  let component: ClientHomeComponent;
  let mockProductService: jasmine.SpyObj<ProductService>;

  beforeEach(async () => {
    // Create a ProductService mock using Jasmine
    mockProductService = jasmine.createSpyObj('ProductService', ['getAllProducts']);

    await TestBed.configureTestingModule({
      imports: [ClientHomeComponent],
      providers: [
        { provide: ProductService, useValue: mockProductService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClientHomeComponent);
    component = fixture.componentInstance;
  });

  it('debe mostrar mensaje cuando no hay productos', () => {
    // Empty mock should return an empty array
    mockProductService.getAllProducts.and.returnValue(of([]));

    fixture.detectChanges(); // runs ngOnInit and renders

    const nativeElement: HTMLElement = fixture.nativeElement;
    expect(nativeElement.textContent).toContain('No hay productos disponibles para mostrar.');
  });

  it('debe mostrar los productos cuando el servicio devuelve datos', () => {
    const mockProducts = [
      { id: 1, referenceCode: 'ABC', name: 'Producto 1', description: 'Desc 1', price: 100 },
      { id: 2, referenceCode: 'DEF', name: 'Producto 2', description: 'Desc 2', price: 200 }
    ];

    mockProductService.getAllProducts.and.returnValue(of(mockProducts));

    fixture.detectChanges();

    const items = fixture.debugElement.queryAll(By.css('.m-4'));
    expect(items.length).toBe(2);

    const nativeElement: HTMLElement = fixture.nativeElement;
    expect(nativeElement.textContent).toContain('Producto 1');
    expect(nativeElement.textContent).toContain('Producto 2');
  });

  it('debe manejar error en el servicio', () => {
    mockProductService.getAllProducts.and.returnValue(throwError(() => new Error('Error backend')));

    fixture.detectChanges();

    expect(component.availableProducts).toBeFalse();
    expect(component.products.length).toBe(0);
  });
});
