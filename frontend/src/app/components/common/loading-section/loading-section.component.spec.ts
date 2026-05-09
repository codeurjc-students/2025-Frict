import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';

import {LoadingSectionComponent} from './loading-section.component';

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('LoadingSectionComponent', () => {
  let component: LoadingSectionComponent;
  let fixture: ComponentFixture<LoadingSectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoadingSectionComponent],
      providers: [provideNoopAnimations()]
    }).compileComponents();

    fixture = TestBed.createComponent(LoadingSectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Default inputs ────────────────────────────────────────────────────────────

  describe('default inputs', () => {
    it('should default loading to false', () => {
      expect(component.loading).toBeFalse();
    });

    it('should default error to false', () => {
      expect(component.error).toBeFalse();
    });

    it('should default numElements to 0', () => {
      expect(component.numElements).toBe(0);
    });

    it('should default idType to "elements"', () => {
      expect(component.idType).toBe('elements');
    });

    it('should default elementsType to "elementos"', () => {
      expect(component.elementsType).toBe('elementos');
    });

    it('should default loadingText to "Cargando, por favor espera..."', () => {
      expect(component.loadingText).toBe('Cargando, por favor espera...');
    });

    it('should default errorText to "Ha ocurrido un error inesperado"', () => {
      expect(component.errorText).toBe('Ha ocurrido un error inesperado');
    });
  });

  // ── Loading state ─────────────────────────────────────────────────────────────

  describe('loading state (loading=true)', () => {
    beforeEach(() => {
      component.loading = true;
      fixture.detectChanges();
    });

    it('should show the progress spinner', () => {
      expect(fixture.nativeElement.querySelector('p-progress-spinner')).toBeTruthy();
    });

    it('should show the default loading text', () => {
      expect(fixture.nativeElement.textContent).toContain('Cargando, por favor espera...');
    });

    it('should show a custom loadingText when provided', () => {
      component.loadingText = 'Procesando datos...';
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Procesando datos...');
    });

    it('should NOT show the error panel', () => {
      expect(fixture.nativeElement.querySelector('.bg-red-50')).toBeFalsy();
    });

    it('should NOT show the empty-state panel', () => {
      expect(fixture.nativeElement.querySelector('.bg-blue-50')).toBeFalsy();
    });
  });

  // ── Error state ───────────────────────────────────────────────────────────────

  describe('error state (loading=false, error=true)', () => {
    beforeEach(() => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
    });

    it('should show the error panel', () => {
      expect(fixture.nativeElement.querySelector('.bg-red-50')).toBeTruthy();
    });

    it('should show the exclamation-triangle icon', () => {
      expect(fixture.nativeElement.querySelector('.pi-exclamation-triangle')).toBeTruthy();
    });

    it('should show the default error text', () => {
      expect(fixture.nativeElement.textContent).toContain('Ha ocurrido un error inesperado');
    });

    it('should show a custom errorText when provided', () => {
      component.errorText = 'Servicio no disponible';
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Servicio no disponible');
    });

    it('should set the error span id to idType + "-error"', () => {
      expect(fixture.nativeElement.querySelector('#elements-error')).toBeTruthy();
    });

    it('should reflect a custom idType in the error span id', () => {
      component.idType = 'products';
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('#products-error')).toBeTruthy();
    });

    it('should NOT show the progress spinner', () => {
      expect(fixture.nativeElement.querySelector('p-progress-spinner')).toBeFalsy();
    });

    it('should NOT show the empty-state panel', () => {
      expect(fixture.nativeElement.querySelector('.bg-blue-50')).toBeFalsy();
    });
  });

  // ── Empty state ───────────────────────────────────────────────────────────────

  describe('empty state (loading=false, error=false, numElements=0)', () => {
    it('should show the info panel', () => {
      expect(fixture.nativeElement.querySelector('.bg-blue-50')).toBeTruthy();
    });

    it('should show the info-circle icon', () => {
      expect(fixture.nativeElement.querySelector('.pi-info-circle')).toBeTruthy();
    });

    it('should show "No hay elementos disponibles." with the default elementsType', () => {
      expect(fixture.nativeElement.textContent).toContain('No hay elementos disponibles.');
    });

    it('should reflect a custom elementsType in the empty message', () => {
      component.elementsType = 'productos';
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('No hay productos disponibles.');
    });

    it('should set the empty span id to idType + "-noProducts"', () => {
      expect(fixture.nativeElement.querySelector('#elements-noProducts')).toBeTruthy();
    });

    it('should reflect a custom idType in the empty span id', () => {
      component.idType = 'orders';
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('#orders-noProducts')).toBeTruthy();
    });

    it('should NOT show the progress spinner', () => {
      expect(fixture.nativeElement.querySelector('p-progress-spinner')).toBeFalsy();
    });

    it('should NOT show the error panel', () => {
      expect(fixture.nativeElement.querySelector('.bg-red-50')).toBeFalsy();
    });
  });

  // ── With elements — no panel shown ───────────────────────────────────────────

  describe('with elements (numElements > 0)', () => {
    beforeEach(() => {
      component.loading = false;
      component.error = false;
      component.numElements = 5;
      fixture.detectChanges();
    });

    it('should show none of the three panels', () => {
      expect(fixture.nativeElement.querySelector('p-progress-spinner')).toBeFalsy();
      expect(fixture.nativeElement.querySelector('.bg-red-50')).toBeFalsy();
      expect(fixture.nativeElement.querySelector('.bg-blue-50')).toBeFalsy();
    });
  });

  // ── Priority ──────────────────────────────────────────────────────────────────

  describe('state priority', () => {
    it('should show the spinner when loading=true even if error=true', () => {
      component.loading = true;
      component.error = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('p-progress-spinner')).toBeTruthy();
      expect(fixture.nativeElement.querySelector('.bg-red-50')).toBeFalsy();
    });

    it('should show the spinner when loading=true even if numElements=0', () => {
      component.loading = true;
      component.numElements = 0;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('p-progress-spinner')).toBeTruthy();
      expect(fixture.nativeElement.querySelector('.bg-blue-50')).toBeFalsy();
    });
  });
});
