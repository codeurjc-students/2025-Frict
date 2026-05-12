import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {ActivatedRoute, Router} from '@angular/router';
import {Subject} from 'rxjs';

import {LoadingScreenComponent} from './loading-screen.component';

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('LoadingScreenComponent', () => {
  let component: LoadingScreenComponent;
  let fixture: ComponentFixture<LoadingScreenComponent>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: new Subject().asObservable(),
      url: '/current-path'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    await TestBed.configureTestingModule({
      imports: [LoadingScreenComponent],
      providers: [
        provideNoopAnimations(),
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => null }, url: [], data: {} },
            root: { children: [] }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoadingScreenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Default inputs ────────────────────────────────────────────────────────────

  describe('default inputs', () => {
    it('should default loading to true', () => {
      expect(component.loading).toBeTrue();
    });

    it('should default error to false', () => {
      expect(component.error).toBeFalse();
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
    it('should show the progress spinner', () => {
      expect(fixture.nativeElement.querySelector('p-progress-spinner')).toBeTruthy();
    });

    it('should show the default loading text', () => {
      expect(fixture.nativeElement.textContent).toContain('Cargando, por favor espera...');
    });

    it('should show a custom loadingText when provided', () => {
      component.loadingText = 'Espera un momento...';
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Espera un momento...');
    });

    it('should NOT show the error panel', () => {
      expect(fixture.nativeElement.textContent).not.toContain('Error');
      expect(fixture.nativeElement.textContent).not.toContain('Volver a intentarlo');
    });
  });

  // ── Error state ───────────────────────────────────────────────────────────────

  describe('error state (loading=false, error=true)', () => {
    beforeEach(() => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
    });

    it('should show the "Error" heading', () => {
      expect(fixture.nativeElement.textContent).toContain('Error');
    });

    it('should show the default error text', () => {
      expect(fixture.nativeElement.textContent).toContain('Ha ocurrido un error inesperado');
    });

    it('should show a custom errorText when provided', () => {
      component.errorText = 'Servicio no disponible';
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Servicio no disponible');
    });

    it('should show the "Volver a intentarlo" link', () => {
      expect(fixture.nativeElement.textContent).toContain('Volver a intentarlo');
    });

    it('should NOT show the progress spinner', () => {
      expect(fixture.nativeElement.querySelector('p-progress-spinner')).toBeFalsy();
    });
  });

  // ── No-content state ──────────────────────────────────────────────────────────

  describe('idle state (loading=false, error=false)', () => {
    beforeEach(() => {
      component.loading = false;
      component.error = false;
      fixture.detectChanges();
    });

    it('should show neither the spinner nor the error panel', () => {
      expect(fixture.nativeElement.querySelector('p-progress-spinner')).toBeFalsy();
      expect(fixture.nativeElement.textContent).not.toContain('Volver a intentarlo');
    });
  });

  // ── Loading takes priority over error ─────────────────────────────────────────

  describe('loading=true AND error=true', () => {
    beforeEach(() => {
      component.loading = true;
      component.error = true;
      fixture.detectChanges();
    });

    it('should show the spinner, not the error panel', () => {
      expect(fixture.nativeElement.querySelector('p-progress-spinner')).toBeTruthy();
      expect(fixture.nativeElement.textContent).not.toContain('Volver a intentarlo');
    });
  });

  // ── reloadPage ────────────────────────────────────────────────────────────────

  describe('reloadPage', () => {
    it('should call router.navigate with the current router URL', () => {
      component.reloadPage();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/current-path']);
    });

    it('should emit tryReload', () => {
      let emitted = false;
      component.tryReload.subscribe(() => emitted = true);
      component.reloadPage();
      expect(emitted).toBeTrue();
    });
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should call reloadPage when "Volver a intentarlo" is clicked', () => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
      spyOn(component, 'reloadPage');
      const link: HTMLElement = fixture.nativeElement.querySelector('a[class*="bg-primary"]');
      link.click();
      expect(component.reloadPage).toHaveBeenCalled();
    });

    it('should render the shop logo in the error panel', () => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('img[src="/shopLogo.png"]')).toBeTruthy();
    });

    it('should show "TecHub" branding in the error panel', () => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('TecHub');
    });
  });
});
