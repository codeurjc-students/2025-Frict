import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';

import {LoginComponent} from './login.component';
import {AuthService} from '../../../services/auth.service';
import {UserService} from '../../../services/user.service';
import {LoginInfo} from '../../../models/loginInfo.model';

// ── Helpers ───────────────────────────────────────────────────────────────────

const STUB_LOGIN_INFO: LoginInfo = {
  isLogged: true, imageUrl: '', id: '1', name: 'Test', username: 'test', roles: [], selectedShopId: null
};

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login', 'isAdmin', 'isManager', 'isDriver']);
    authServiceSpy.login.and.callFake(() => of(STUB_LOGIN_INFO));
    authServiceSpy.isAdmin.and.returnValue(false);
    authServiceSpy.isManager.and.returnValue(false);
    authServiceSpy.isDriver.and.returnValue(false);

    userServiceSpy = jasmine.createSpyObj('UserService', ['checkUsernameTaken']);
    userServiceSpy.checkUsernameTaken.and.callFake(() => of(true)); // user exists → valid

    routerSpy = jasmine.createSpyObj('Router', ['navigateByUrl', 'navigate', 'createUrlTree', 'serializeUrl'], {
      events: new Subject().asObservable(),
      url: '/'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParams: {}, paramMap: { get: () => null }, url: [], data: {} },
            root: { children: [] }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Default state ─────────────────────────────────────────────────────────────

  describe('default state', () => {
    it('should initialize showPassword to false', () => {
      expect(component.showPassword).toBeFalse();
    });

    it('should initialize showErrorMessage to false', () => {
      expect(component.showErrorMessage).toBeFalse();
    });

    it('should initialize the form with empty username and password', () => {
      expect(component.loginForm.get('username')?.value).toBe('');
      expect(component.loginForm.get('password')?.value).toBe('');
    });

    it('should have the form invalid by default', () => {
      expect(component.loginForm.invalid).toBeTrue();
    });
  });

  // ── togglePassword ────────────────────────────────────────────────────────────

  describe('togglePassword', () => {
    it('should flip showPassword from false to true', () => {
      component.togglePassword();
      expect(component.showPassword).toBeTrue();
    });

    it('should flip showPassword back to false on second call', () => {
      component.togglePassword();
      component.togglePassword();
      expect(component.showPassword).toBeFalse();
    });
  });

  // ── async validator (createUsernameExistsValidator) ───────────────────────────

  describe('username async validator', () => {
    it('should be valid when checkUsernameTaken returns true', fakeAsync(() => {
      userServiceSpy.checkUsernameTaken.and.callFake(() => of(true));
      component.loginForm.get('username')!.setValue('existinguser');
      tick(750);
      expect(component.loginForm.get('username')!.errors).toBeNull();
    }));

    it('should have userNotFound error when checkUsernameTaken returns false', fakeAsync(() => {
      userServiceSpy.checkUsernameTaken.and.callFake(() => of(false));
      component.loginForm.get('username')!.setValue('unknownuser');
      tick(750);
      expect(component.loginForm.get('username')!.hasError('userNotFound')).toBeTrue();
    }));

    it('should not call checkUsernameTaken for empty username', fakeAsync(() => {
      userServiceSpy.checkUsernameTaken.calls.reset();
      component.loginForm.get('username')!.setValue('');
      tick(750);
      expect(userServiceSpy.checkUsernameTaken).not.toHaveBeenCalled();
    }));

    it('should have required error when username is empty and touched', () => {
      component.loginForm.get('username')!.setValue('');
      component.loginForm.get('username')!.markAsTouched();
      expect(component.loginForm.get('username')!.hasError('required')).toBeTrue();
    });
  });

  // ── onSubmit — guard ──────────────────────────────────────────────────────────

  describe('onSubmit — invalid form', () => {
    it('should not call authService.login when the form is invalid', () => {
      component.loginForm.patchValue({ username: '', password: '' });
      component.onSubmit();
      expect(authServiceSpy.login).not.toHaveBeenCalled();
    });

    it('should not navigate when the form is invalid', () => {
      component.loginForm.patchValue({ username: '', password: '' });
      component.onSubmit();
      expect(routerSpy.navigateByUrl).not.toHaveBeenCalled();
    });
  });

  // ── onSubmit — navigation after success ───────────────────────────────────────

  describe('onSubmit — navigation after success', () => {
    it('should navigate to "/" for regular users', fakeAsync(() => {
      component.loginForm.patchValue({ username: 'john', password: 'pass' });
      tick(750);
      component.onSubmit();
      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/');
    }));

    it('should navigate to "/admin" for admin users', fakeAsync(() => {
      authServiceSpy.isAdmin.and.returnValue(true);
      component.loginForm.patchValue({ username: 'admin', password: 'pass' });
      tick(750);
      component.onSubmit();
      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/admin');
    }));

    it('should navigate to "/admin" for manager users', fakeAsync(() => {
      authServiceSpy.isManager.and.returnValue(true);
      component.loginForm.patchValue({ username: 'manager', password: 'pass' });
      tick(750);
      component.onSubmit();
      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/admin');
    }));

    it('should navigate to "/admin" for driver users', fakeAsync(() => {
      authServiceSpy.isDriver.and.returnValue(true);
      component.loginForm.patchValue({ username: 'driver', password: 'pass' });
      tick(750);
      component.onSubmit();
      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/admin');
    }));

    it('should navigate to the custom returnUrl when set', fakeAsync(() => {
      (component as any)['returnUrl'] = '/checkout';
      component.loginForm.patchValue({ username: 'john', password: 'pass' });
      tick(750);
      component.onSubmit();
      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/checkout');
    }));

    it('should skip role-based routing when a custom returnUrl is set', fakeAsync(() => {
      authServiceSpy.isAdmin.and.returnValue(true);
      (component as any)['returnUrl'] = '/orders/42';
      component.loginForm.patchValue({ username: 'admin', password: 'pass' });
      tick(750);
      component.onSubmit();
      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/orders/42');
      expect(routerSpy.navigateByUrl).not.toHaveBeenCalledWith('/admin');
    }));

    it('should call authService.login with the form credentials', fakeAsync(() => {
      component.loginForm.patchValue({ username: 'john', password: 'secret' });
      tick(750);
      component.onSubmit();
      expect(authServiceSpy.login).toHaveBeenCalledWith('john', 'secret');
    }));
  });

  // ── onSubmit — login error ────────────────────────────────────────────────────

  describe('onSubmit — login error', () => {
    it('should set showErrorMessage to true when login fails', fakeAsync(() => {
      authServiceSpy.login.and.callFake(() => throwError(() => new Error('401')));
      component.loginForm.patchValue({ username: 'john', password: 'wrong' });
      tick(750);
      component.onSubmit();
      expect(component.showErrorMessage).toBeTrue();
    }));

    it('should NOT navigate when login fails', fakeAsync(() => {
      authServiceSpy.login.and.callFake(() => throwError(() => new Error('401')));
      component.loginForm.patchValue({ username: 'john', password: 'wrong' });
      tick(750);
      component.onSubmit();
      expect(routerSpy.navigateByUrl).not.toHaveBeenCalled();
    }));
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should show the "Iniciar sesión" heading', () => {
      expect(fixture.nativeElement.textContent).toContain('Iniciar sesión');
    });

    it('should show the TecHub brand', () => {
      expect(fixture.nativeElement.textContent).toContain('TecHub');
    });

    it('should NOT show the error panel by default', () => {
      expect(fixture.nativeElement.querySelector('.bg-red-50')).toBeFalsy();
    });

    it('should show the error panel when showErrorMessage is true', () => {
      component.showErrorMessage = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.bg-red-50')).toBeTruthy();
    });

    it('should show "Error de acceso" text in the error panel', () => {
      component.showErrorMessage = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Error de acceso');
    });

    it('should render the password input with type "password" by default', () => {
      const input: HTMLInputElement = fixture.nativeElement.querySelector('#password');
      expect(input.type).toBe('password');
    });

    it('should change password input to type "text" after togglePassword', () => {
      component.togglePassword();
      fixture.detectChanges();
      const input: HTMLInputElement = fixture.nativeElement.querySelector('#password');
      expect(input.type).toBe('text');
    });

    it('should call togglePassword when the eye button is clicked', () => {
      spyOn(component, 'togglePassword');
      const btn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="button"]');
      btn.click();
      expect(component.togglePassword).toHaveBeenCalled();
    });

    it('should render the submit button with "Iniciar sesión" label', () => {
      const btn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(btn.textContent?.trim()).toBe('Iniciar sesión');
    });

    it('should render a link to /recover for the forgotten password', () => {
      expect(fixture.nativeElement.querySelector('a[routerLink="/recover"]')).toBeTruthy();
    });

    it('should render a link to /signup', () => {
      expect(fixture.nativeElement.querySelector('a[routerLink="/signup"]')).toBeTruthy();
    });

    it('should render a home routerLink to "/"', () => {
      expect(fixture.nativeElement.querySelector('a[routerLink="/"]')).toBeTruthy();
    });
  });
});
