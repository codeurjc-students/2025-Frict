import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {signal, WritableSignal} from '@angular/core';

import {StockTagComponent} from './stock-tag.component';
import {ProductService} from '../../../services/product.service';
import {SearchScope} from '../../../services/product.service';
import {Product} from '../../../models/product.model';

// ── Helpers ───────────────────────────────────────────────────────────────────

function makeProduct(totalUnits: number, availableUnits: number): Product {
  return {
    id: '1', referenceCode: 'REF-001', name: 'Laptop',
    imagesInfo: [{ id: '1', imageUrl: '/laptop.jpg', s3Key: '', fileName: '' }],
    description: 'A laptop', supplyPrice: 100, previousPrice: 120, currentPrice: 100,
    active: true, discount: '0%', categories: [], shopsWithStock: 1,
    averageRating: 4.5, totalReviews: 10, specifications: [], createdAt: '2025-01-01',
    totalUnits, availableUnits
  };
}

// ── Spec ──────────────────────────────────────────────────────────────────────

describe('StockTagComponent', () => {
  let component: StockTagComponent;
  let fixture: ComponentFixture<StockTagComponent>;
  let scopeSignal: WritableSignal<SearchScope>;

  beforeEach(async () => {
    scopeSignal = signal<SearchScope>('GLOBAL');
    const productServiceMock = { searchScope: scopeSignal.asReadonly() };

    await TestBed.configureTestingModule({
      imports: [StockTagComponent],
      providers: [
        provideNoopAnimations(),
        { provide: ProductService, useValue: productServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StockTagComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('product', makeProduct(5, 5));
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── tagInfo — GLOBAL scope ────────────────────────────────────────────────────

  describe('tagInfo — GLOBAL scope', () => {
    it('should return null when totalUnits > 10', () => {
      fixture.componentRef.setInput('product', makeProduct(15, 15));
      expect(component.tagInfo()).toBeNull();
    });

    it('should return null when totalUnits is exactly 11', () => {
      fixture.componentRef.setInput('product', makeProduct(11, 11));
      expect(component.tagInfo()).toBeNull();
    });

    it('should return null when totalUnits is exactly 10 boundary — info not null', () => {
      fixture.componentRef.setInput('product', makeProduct(10, 10));
      expect(component.tagInfo()).not.toBeNull();
    });

    it('should return info severity when totalUnits is 10', () => {
      fixture.componentRef.setInput('product', makeProduct(10, 10));
      expect(component.tagInfo()?.severity).toBe('info');
    });

    it('should return info severity when totalUnits is between 6 and 10', () => {
      fixture.componentRef.setInput('product', makeProduct(7, 7));
      expect(component.tagInfo()?.severity).toBe('info');
    });

    it('should return info severity when totalUnits is exactly 6', () => {
      fixture.componentRef.setInput('product', makeProduct(6, 6));
      expect(component.tagInfo()?.severity).toBe('info');
    });

    it('should include "Global" in the info message', () => {
      fixture.componentRef.setInput('product', makeProduct(7, 7));
      expect(component.tagInfo()?.message).toContain('Global');
    });

    it('should include the unit count in the info message', () => {
      fixture.componentRef.setInput('product', makeProduct(7, 7));
      expect(component.tagInfo()?.message).toContain('7');
    });

    it('should return warn severity when totalUnits is between 1 and 5', () => {
      fixture.componentRef.setInput('product', makeProduct(3, 3));
      expect(component.tagInfo()?.severity).toBe('warn');
    });

    it('should return warn severity when totalUnits is exactly 5', () => {
      fixture.componentRef.setInput('product', makeProduct(5, 5));
      expect(component.tagInfo()?.severity).toBe('warn');
    });

    it('should return warn severity when totalUnits is exactly 1', () => {
      fixture.componentRef.setInput('product', makeProduct(1, 1));
      expect(component.tagInfo()?.severity).toBe('warn');
    });

    it('should include the unit count in the warn message', () => {
      fixture.componentRef.setInput('product', makeProduct(3, 3));
      expect(component.tagInfo()?.message).toContain('3');
    });

    it('should return danger severity when totalUnits is 0', () => {
      fixture.componentRef.setInput('product', makeProduct(0, 0));
      expect(component.tagInfo()?.severity).toBe('danger');
    });

    it('should include "Agotado" in the danger message', () => {
      fixture.componentRef.setInput('product', makeProduct(0, 0));
      expect(component.tagInfo()?.message).toContain('Agotado');
    });

    it('should include "Global" in the danger message', () => {
      fixture.componentRef.setInput('product', makeProduct(0, 0));
      expect(component.tagInfo()?.message).toContain('Global');
    });
  });

  // ── tagInfo — LOCAL scope ─────────────────────────────────────────────────────

  describe('tagInfo — LOCAL scope', () => {
    beforeEach(() => {
      scopeSignal.set('LOCAL');
    });

    it('should use availableUnits instead of totalUnits', () => {
      // totalUnits=15 would be null in GLOBAL; availableUnits=5 → warn in LOCAL
      fixture.componentRef.setInput('product', makeProduct(15, 5));
      expect(component.tagInfo()?.severity).toBe('warn');
    });

    it('should include "Local" in the message', () => {
      fixture.componentRef.setInput('product', makeProduct(15, 5));
      expect(component.tagInfo()?.message).toContain('Local');
    });

    it('should NOT include "Global" in the message', () => {
      fixture.componentRef.setInput('product', makeProduct(15, 5));
      expect(component.tagInfo()?.message).not.toContain('Global');
    });

    it('should return null when availableUnits > 10', () => {
      fixture.componentRef.setInput('product', makeProduct(20, 15));
      expect(component.tagInfo()).toBeNull();
    });

    it('should return info severity when availableUnits is between 6 and 10', () => {
      fixture.componentRef.setInput('product', makeProduct(20, 8));
      expect(component.tagInfo()?.severity).toBe('info');
    });

    it('should return warn severity when availableUnits is between 1 and 5', () => {
      fixture.componentRef.setInput('product', makeProduct(20, 2));
      expect(component.tagInfo()?.severity).toBe('warn');
    });

    it('should return danger severity when availableUnits is 0', () => {
      fixture.componentRef.setInput('product', makeProduct(20, 0));
      expect(component.tagInfo()?.severity).toBe('danger');
    });

    it('should include "Agotado" in the local danger message', () => {
      fixture.componentRef.setInput('product', makeProduct(20, 0));
      expect(component.tagInfo()?.message).toContain('Agotado');
    });
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should NOT render p-tag when totalUnits > 10 in GLOBAL scope', () => {
      fixture.componentRef.setInput('product', makeProduct(15, 15));
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('p-tag')).toBeFalsy();
    });

    it('should render p-tag when totalUnits is in warn range', () => {
      fixture.componentRef.setInput('product', makeProduct(5, 5));
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('p-tag')).toBeTruthy();
    });

    it('should render p-tag when totalUnits is in info range', () => {
      fixture.componentRef.setInput('product', makeProduct(8, 8));
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('p-tag')).toBeTruthy();
    });

    it('should render p-tag when totalUnits is 0', () => {
      fixture.componentRef.setInput('product', makeProduct(0, 0));
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('p-tag')).toBeTruthy();
    });

    it('should display the unit count in the rendered tag', () => {
      fixture.componentRef.setInput('product', makeProduct(3, 3));
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('3');
    });

    it('should display "Agotado" in the rendered tag when units are 0', () => {
      fixture.componentRef.setInput('product', makeProduct(0, 0));
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Agotado');
    });

    it('should NOT render p-tag when availableUnits > 10 in LOCAL scope', () => {
      scopeSignal.set('LOCAL');
      fixture.componentRef.setInput('product', makeProduct(20, 15));
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('p-tag')).toBeFalsy();
    });

    it('should display "Local" label in LOCAL scope', () => {
      scopeSignal.set('LOCAL');
      fixture.componentRef.setInput('product', makeProduct(20, 3));
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Local');
    });

    it('should display "Global" label in GLOBAL scope', () => {
      fixture.componentRef.setInput('product', makeProduct(3, 3));
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Global');
    });
  });
});
