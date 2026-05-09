import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';

import {RecoverAccountComponent} from './recover-account.component';
import {AuthService} from '../../../services/auth.service';
import {UserService} from '../../../services/user.service';

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('RecoverAccountComponent', () => {
  let component: RecoverAccountComponent;
  let fixture: ComponentFixture<RecoverAccountComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['initPasswordRecovery']);
    authServiceSpy.initPasswordRecovery.and.callFake(() => of({}));

    userServiceSpy = jasmine.createSpyObj('UserService', ['checkUsernameTaken']);
    userServiceSpy.checkUsernameTaken.and.callFake(() => of(true));

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: new Subject().asObservable(),
      url: '/recover'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    await TestBed.configureTestingModule({
      imports: [RecoverAccountComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
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

    fixture = TestBed.createComponent(RecoverAccountComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Default state ─────────────────────────────────────────────────────────────

  describe('default state', () => {
    it('should initialize loading to false', () => {
      expect(component.loading).toBeFalse();
    });

    it('should initialize isEmailSent to false', () => {
      expect(component.isEmailSent).toBeFalse();
    });

    it('should initialize errorMessage to empty string', () => {
      expect(component.errorMessage).toBe('');
    });

    it('should initialize recoverForm with empty username', () => {
      expect(component.recoverForm.get('username')?.value).toBe('');
    });

    it('should have the form invalid by default', () => {
      expect(component.recoverForm.invalid).toBeTrue();
    });
  });

  // ── usernameControl getter ────────────────────────────────────────────────────

  describe('usernameControl getter', () => {
    it('should return the username form control', () => {
      expect(component.usernameControl).toBe(component.recoverForm.get('username'));
    });
  });

  // ── async validator (username exists) ────────────────────────────────────────

  describe('username async validator', () => {
    it('should be valid when checkUsernameTaken returns true', fakeAsync(() => {
      userServiceSpy.checkUsernameTaken.and.callFake(() => of(true));
      component.recoverForm.get('username')!.setValue('existinguser');
      tick(750);
      expect(component.recoverForm.get('username')!.errors).toBeNull();
    }));

    it('should have userNotFound error when checkUsernameTaken returns false', fakeAsync(() => {
      userServiceSpy.checkUsernameTaken.and.callFake(() => of(false));
      component.recoverForm.get('username')!.setValue('unknownuser');
      tick(750);
      expect(component.recoverForm.get('username')!.hasError('userNotFound')).toBeTrue();
    }));

    it('should have required error when username is empty and touched', () => {
      component.recoverForm.get('username')!.setValue('');
      component.recoverForm.get('username')!.markAsTouched();
      expect(component.recoverForm.get('username')!.hasError('required')).toBeTrue();
    });

    it('should NOT call checkUsernameTaken for empty username', fakeAsync(() => {
      userServiceSpy.checkUsernameTaken.calls.reset();
      component.recoverForm.get('username')!.setValue('');
      tick(750);
      expect(userServiceSpy.checkUsernameTaken).not.toHaveBeenCalled();
    }));
  });

  // ── onSubmit — invalid form guard ─────────────────────────────────────────────

  describe('onSubmit — invalid form', () => {
    it('should not call authService.initPasswordRecovery when form is invalid', () => {
      component.recoverForm.patchValue({ username: '' });
      component.onSubmit();
      expect(authServiceSpy.initPasswordRecovery).not.toHaveBeenCalled();
    });

    it('should not set loading=true when form is invalid', () => {
      component.recoverForm.patchValue({ username: '' });
      component.onSubmit();
      expect(component.loading).toBeFalse();
    });
  });

  // ── onSubmit — success ────────────────────────────────────────────────────────

  describe('onSubmit — success', () => {
    it('should call initPasswordRecovery with the entered username', fakeAsync(() => {
      component.recoverForm.patchValue({ username: 'john' });
      tick(750);
      component.onSubmit();
      expect(authServiceSpy.initPasswordRecovery).toHaveBeenCalledWith('john');
    }));

    it('should set loading=true before the response arrives', fakeAsync(() => {
      const pending$ = new Subject<Object>();
      authServiceSpy.initPasswordRecovery.and.callFake(() => pending$.asObservable());
      component.recoverForm.patchValue({ username: 'john' });
      tick(750);
      component.onSubmit();
      expect(component.loading).toBeTrue();
    }));

    it('should set loading=false after success', fakeAsync(() => {
      component.recoverForm.patchValue({ username: 'john' });
      tick(750);
      component.onSubmit();
      expect(component.loading).toBeFalse();
    }));

    it('should set isEmailSent=true after success', fakeAsync(() => {
      component.recoverForm.patchValue({ username: 'john' });
      tick(750);
      component.onSubmit();
      expect(component.isEmailSent).toBeTrue();
    }));

    it('should clear errorMessage before submitting', fakeAsync(() => {
      component.errorMessage = 'previous error';
      component.recoverForm.patchValue({ username: 'john' });
      tick(750);
      component.onSubmit();
      expect(component.errorMessage).toBe('');
    }));
  });

  // ── onSubmit — error ──────────────────────────────────────────────────────────

  describe('onSubmit — error', () => {
    it('should set loading=false on failure', fakeAsync(() => {
      authServiceSpy.initPasswordRecovery.and.callFake(() => throwError(() => new Error('500')));
      component.recoverForm.patchValue({ username: 'john' });
      tick(750);
      component.onSubmit();
      expect(component.loading).toBeFalse();
    }));

    it('should set errorMessage on failure', fakeAsync(() => {
      authServiceSpy.initPasswordRecovery.and.callFake(() => throwError(() => new Error('500')));
      component.recoverForm.patchValue({ username: 'john' });
      tick(750);
      component.onSubmit();
      expect(component.errorMessage).not.toBe('');
    }));

    it('should NOT set isEmailSent on failure', fakeAsync(() => {
      authServiceSpy.initPasswordRecovery.and.callFake(() => throwError(() => new Error('500')));
      component.recoverForm.patchValue({ username: 'john' });
      tick(750);
      component.onSubmit();
      expect(component.isEmailSent).toBeFalse();
    }));
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should show the "Recuperar contraseña" heading', () => {
      expect(fixture.nativeElement.textContent).toContain('Recuperar contraseña');
    });

    it('should show the TecHub brand', () => {
      expect(fixture.nativeElement.textContent).toContain('TecHub');
    });

    it('should render the username input', () => {
      expect(fixture.nativeElement.querySelector('#username')).toBeTruthy();
    });

    it('should render the "Enviar instrucciones" submit button', () => {
      expect(fixture.nativeElement.textContent).toContain('Enviar instrucciones');
    });

    it('should render a link to /login', () => {
      expect(fixture.nativeElement.querySelector('a[routerLink="/login"]')).toBeTruthy();
    });

    it('should render a link to /', () => {
      expect(fixture.nativeElement.querySelector('a[routerLink="/"]')).toBeTruthy();
    });

    it('should NOT show the error panel when errorMessage is empty', () => {
      expect(fixture.nativeElement.querySelector('.bg-red-50')).toBeFalsy();
    });

    it('should show the error panel when errorMessage is set', () => {
      component.errorMessage = 'No se ha podido procesar la solicitud.';
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.bg-red-50')).toBeTruthy();
    });

    it('should display the errorMessage text in the error panel', () => {
      component.errorMessage = 'No se ha podido procesar la solicitud.';
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('No se ha podido procesar la solicitud.');
    });

    it('should show "Procesando..." label when loading=true', () => {
      component.loading = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Procesando...');
    });

    it('should NOT show "Procesando..." when loading=false', () => {
      component.loading = false;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Procesando...');
    });

    it('should hide the form view and show success view when isEmailSent=true', () => {
      component.isEmailSent = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('¡Correo enviado!');
      expect(fixture.nativeElement.textContent).not.toContain('Recuperar contraseña');
    });

    it('should show "Probar con otro usuario" button in success view', () => {
      component.isEmailSent = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Probar con otro usuario');
    });

    it('should reset isEmailSent to false when "Probar con otro usuario" is clicked', () => {
      component.isEmailSent = true;
      fixture.detectChanges();
      const btn = Array.from<HTMLButtonElement>(
        fixture.nativeElement.querySelectorAll('button')
      ).find(b => b.textContent?.includes('Probar con otro usuario'));
      btn?.click();
      expect(component.isEmailSent).toBeFalse();
    });

    it('should show "userNotFound" validation error when username does not exist', fakeAsync(() => {
      userServiceSpy.checkUsernameTaken.and.callFake(() => of(false));
      const ctrl = component.recoverForm.get('username')!;
      ctrl.setValue('ghost');
      ctrl.markAsTouched();
      tick(750);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('El usuario no existe.');
    }));

    it('should show "Requerido." validation error when username is empty and touched', () => {
      const ctrl = component.recoverForm.get('username')!;
      ctrl.setValue('');
      ctrl.markAsTouched();
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Requerido.');
    });

    it('should disable the submit button while the form is invalid', () => {
      const btn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(btn.disabled).toBeTrue();
    });

    it('should disable the submit button while loading=true', fakeAsync(() => {
      const pending$ = new Subject<Object>();
      authServiceSpy.initPasswordRecovery.and.callFake(() => pending$.asObservable());
      component.recoverForm.patchValue({ username: 'john' });
      tick(750);
      component.onSubmit();
      fixture.detectChanges();
      const btn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(btn.disabled).toBeTrue();
    }));
  });
});
