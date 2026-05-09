import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {FormGroup} from '@angular/forms';

import {ResetPasswordComponent} from './reset-password.component';
import {AuthService} from '../../../services/auth.service';

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('ResetPasswordComponent', () => {
  let component: ResetPasswordComponent;
  let fixture: ComponentFixture<ResetPasswordComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  function configureTestBed(queryParams: Record<string, string> = { username: 'testuser' }) {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['verifyOtp', 'resetPassword']);
    authServiceSpy.verifyOtp.and.callFake(() => of(true));
    authServiceSpy.resetPassword.and.callFake(() => of(undefined as void));

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: new Subject().asObservable(),
      url: '/reset-password'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    return TestBed.configureTestingModule({
      imports: [ResetPasswordComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of(queryParams),
            snapshot: { paramMap: { get: () => null }, url: [], data: {} },
            root: { children: [] }
          }
        }
      ]
    }).compileComponents();
  }

  beforeEach(async () => {
    await configureTestBed({ username: 'testuser' });
    fixture = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Default state ─────────────────────────────────────────────────────────────

  describe('default state', () => {
    it('should initialize currentStep to 1', () => {
      expect(component.currentStep).toBe(1);
    });

    it('should initialize otpCode to empty string', () => {
      expect(component.otpCode).toBe('');
    });

    it('should initialize loading to false', () => {
      expect(component.loading).toBeFalse();
    });

    it('should initialize errorMessage to empty string', () => {
      expect(component.errorMessage).toBe('');
    });

    it('should initialize showPassword to false', () => {
      expect(component.showPassword).toBeFalse();
    });

    it('should initialize passwordForm with empty password and confirmPassword', () => {
      expect(component.passwordForm.get('password')?.value).toBe('');
      expect(component.passwordForm.get('confirmPassword')?.value).toBe('');
    });
  });

  // ── ngOnInit ──────────────────────────────────────────────────────────────────

  describe('ngOnInit', () => {
    it('should set username from queryParams', () => {
      expect(component.username).toBe('testuser');
    });

    it('should set username to empty string when param is absent', async () => {
      TestBed.resetTestingModule();
      await configureTestBed({});
      fixture = TestBed.createComponent(ResetPasswordComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      expect(component.username).toBe('');
    });
  });

  // ── verifyOtp ─────────────────────────────────────────────────────────────────

  describe('verifyOtp', () => {
    it('should NOT call authService.verifyOtp when otpCode length is less than 6', () => {
      component.otpCode = '12345';
      component.verifyOtp();
      expect(authServiceSpy.verifyOtp).not.toHaveBeenCalled();
    });

    it('should NOT call authService.verifyOtp when otpCode is empty', () => {
      component.otpCode = '';
      component.verifyOtp();
      expect(authServiceSpy.verifyOtp).not.toHaveBeenCalled();
    });

    it('should call authService.verifyOtp with username and otpCode', () => {
      component.otpCode = '123456';
      component.verifyOtp();
      expect(authServiceSpy.verifyOtp).toHaveBeenCalledWith('testuser', '123456');
    });

    it('should set loading=true before the response arrives', () => {
      const pending$ = new Subject<boolean>();
      authServiceSpy.verifyOtp.and.callFake(() => pending$.asObservable());
      component.otpCode = '123456';
      component.verifyOtp();
      expect(component.loading).toBeTrue();
    });

    it('should clear errorMessage before sending the request', () => {
      component.errorMessage = 'old error';
      component.otpCode = '123456';
      component.verifyOtp();
      expect(component.errorMessage).toBe('');
    });

    it('should advance to step 2 when OTP is valid', () => {
      authServiceSpy.verifyOtp.and.callFake(() => of(true));
      component.otpCode = '123456';
      component.verifyOtp();
      expect(component.currentStep).toBe(2);
    });

    it('should set loading=false after a valid OTP response', () => {
      component.otpCode = '123456';
      component.verifyOtp();
      expect(component.loading).toBeFalse();
    });

    it('should set errorMessage and stay on step 1 when OTP is invalid', () => {
      authServiceSpy.verifyOtp.and.callFake(() => of(false));
      component.otpCode = '000000';
      component.verifyOtp();
      expect(component.currentStep).toBe(1);
      expect(component.errorMessage).not.toBe('');
    });

    it('should set loading=false when OTP is invalid', () => {
      authServiceSpy.verifyOtp.and.callFake(() => of(false));
      component.otpCode = '000000';
      component.verifyOtp();
      expect(component.loading).toBeFalse();
    });

    it('should set errorMessage and loading=false on verifyOtp error', () => {
      authServiceSpy.verifyOtp.and.callFake(() => throwError(() => new Error('500')));
      component.otpCode = '123456';
      component.verifyOtp();
      expect(component.loading).toBeFalse();
      expect(component.errorMessage).not.toBe('');
    });
  });

  // ── onSubmitPassword ──────────────────────────────────────────────────────────

  describe('onSubmitPassword', () => {
    beforeEach(() => {
      component.currentStep = 2;
      component.otpCode = '123456';
    });

    it('should NOT call resetPassword when the form is invalid', () => {
      component.passwordForm.patchValue({ password: '', confirmPassword: '' });
      component.onSubmitPassword();
      expect(authServiceSpy.resetPassword).not.toHaveBeenCalled();
    });

    it('should NOT call resetPassword when passwords do not match', () => {
      component.passwordForm.patchValue({ password: 'Password1', confirmPassword: 'Password2' });
      component.onSubmitPassword();
      expect(authServiceSpy.resetPassword).not.toHaveBeenCalled();
    });

    it('should call resetPassword with username, otpCode and new password', () => {
      component.passwordForm.patchValue({ password: 'Password1', confirmPassword: 'Password1' });
      component.onSubmitPassword();
      expect(authServiceSpy.resetPassword).toHaveBeenCalledWith('testuser', '123456', 'Password1');
    });

    it('should set loading=true before the response arrives', () => {
      const pending$ = new Subject<void>();
      authServiceSpy.resetPassword.and.callFake(() => pending$.asObservable());
      component.passwordForm.patchValue({ password: 'Password1', confirmPassword: 'Password1' });
      component.onSubmitPassword();
      expect(component.loading).toBeTrue();
    });

    it('should navigate to "/login" on success', () => {
      component.passwordForm.patchValue({ password: 'Password1', confirmPassword: 'Password1' });
      component.onSubmitPassword();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should set loading=false and errorMessage on failure', () => {
      authServiceSpy.resetPassword.and.callFake(() => throwError(() => new Error('500')));
      component.passwordForm.patchValue({ password: 'Password1', confirmPassword: 'Password1' });
      component.onSubmitPassword();
      expect(component.loading).toBeFalse();
      expect(component.errorMessage).not.toBe('');
    });

    it('should NOT navigate on failure', () => {
      authServiceSpy.resetPassword.and.callFake(() => throwError(() => new Error('500')));
      component.passwordForm.patchValue({ password: 'Password1', confirmPassword: 'Password1' });
      component.onSubmitPassword();
      expect(routerSpy.navigate).not.toHaveBeenCalled();
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

  // ── passwordsMatchValidator ───────────────────────────────────────────────────

  describe('passwordsMatchValidator', () => {
    it('should return null when passwords match', () => {
      component.passwordForm.patchValue({ password: 'abc12345', confirmPassword: 'abc12345' });
      const result = component.passwordsMatchValidator(component.passwordForm);
      expect(result).toBeNull();
    });

    it('should return { notMatching: true } when passwords differ', () => {
      component.passwordForm.patchValue({ password: 'abc12345', confirmPassword: 'different' });
      const result = component.passwordsMatchValidator(component.passwordForm);
      expect(result).toEqual({ notMatching: true });
    });

    it('should apply the validator to the form group', () => {
      component.passwordForm.patchValue({ password: 'abc12345', confirmPassword: 'other1234' });
      expect(component.passwordForm.hasError('notMatching')).toBeTrue();
    });

    it('should clear the notMatching error when passwords match', () => {
      component.passwordForm.patchValue({ password: 'abc12345', confirmPassword: 'abc12345' });
      expect(component.passwordForm.hasError('notMatching')).toBeFalse();
    });
  });

  // ── passwordForm validation ───────────────────────────────────────────────────

  describe('passwordForm validation', () => {
    it('should be invalid when password is empty', () => {
      component.passwordForm.patchValue({ password: '', confirmPassword: '' });
      expect(component.passwordForm.invalid).toBeTrue();
    });

    it('should be invalid when password is shorter than 8 characters', () => {
      component.passwordForm.patchValue({ password: 'abc123', confirmPassword: 'abc123' });
      expect(component.passwordForm.get('password')!.hasError('minlength')).toBeTrue();
    });

    it('should be valid when both passwords are equal and meet requirements', () => {
      component.passwordForm.patchValue({ password: 'ValidPass1', confirmPassword: 'ValidPass1' });
      expect(component.passwordForm.valid).toBeTrue();
    });
  });

  // ── DOM — step 1 ──────────────────────────────────────────────────────────────

  describe('DOM — step 1', () => {
    it('should show the TecHub brand', () => {
      expect(fixture.nativeElement.textContent).toContain('TecHub');
    });

    it('should show the "Verificación de seguridad" heading', () => {
      expect(fixture.nativeElement.textContent).toContain('Verificación de seguridad');
    });

    it('should show the "Verificar código" button', () => {
      expect(fixture.nativeElement.textContent).toContain('Verificar código');
    });

    it('should show the "¿No recibiste el código?" resend link', () => {
      expect(fixture.nativeElement.textContent).toContain('¿No recibiste el código?');
    });

    it('should show the "Cancelar" back link to /login', () => {
      expect(fixture.nativeElement.querySelector('a[routerLink="/login"]')).toBeTruthy();
    });

    it('should render a link to "/"', () => {
      expect(fixture.nativeElement.querySelector('a[routerLink="/"]')).toBeTruthy();
    });

    it('should NOT show the step 2 form', () => {
      expect(fixture.nativeElement.textContent).not.toContain('Actualizar contraseña');
    });

    it('should NOT show the error panel when errorMessage is empty', () => {
      expect(fixture.nativeElement.querySelector('.bg-red-50')).toBeFalsy();
    });

    it('should show the error panel when errorMessage is set', () => {
      component.errorMessage = 'El código es incorrecto o ha expirado.';
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.bg-red-50')).toBeTruthy();
    });

    it('should display the errorMessage in the error panel', () => {
      component.errorMessage = 'Error al verificar el código.';
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Error al verificar el código.');
    });

    it('should disable the verify button while otpCode is shorter than 6 digits', () => {
      component.otpCode = '123';
      fixture.detectChanges();
      const btn: HTMLButtonElement = fixture.nativeElement.querySelector('button[disabled]');
      expect(btn).toBeTruthy();
    });
  });

  // ── DOM — step 2 ──────────────────────────────────────────────────────────────

  describe('DOM — step 2', () => {
    beforeEach(() => {
      component.currentStep = 2;
      fixture.detectChanges();
    });

    it('should show the "Código correcto" heading', () => {
      expect(fixture.nativeElement.textContent).toContain('Código correcto');
    });

    it('should show the "Actualizar contraseña" submit button', () => {
      expect(fixture.nativeElement.textContent).toContain('Actualizar contraseña');
    });

    it('should NOT show the step 1 heading', () => {
      expect(fixture.nativeElement.textContent).not.toContain('Verificación de seguridad');
    });

    it('should NOT show the "Cancelar" footer link', () => {
      expect(fixture.nativeElement.querySelector('a[routerLink="/login"]')).toBeFalsy();
    });

    it('should render password inputs with type "password" by default', () => {
      const inputs: NodeListOf<HTMLInputElement> = fixture.nativeElement.querySelectorAll('input[formControlName="password"], input[formControlName="confirmPassword"]');
      inputs.forEach(input => expect(input.type).toBe('password'));
    });

    it('should change password inputs to type "text" after togglePassword', () => {
      component.togglePassword();
      fixture.detectChanges();
      const inputs: NodeListOf<HTMLInputElement> = fixture.nativeElement.querySelectorAll('input[formControlName="password"], input[formControlName="confirmPassword"]');
      inputs.forEach(input => expect(input.type).toBe('text'));
    });

    it('should show the password eye toggle button', () => {
      const btn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="button"]');
      expect(btn).toBeTruthy();
    });

    it('should call togglePassword when eye button is clicked', () => {
      spyOn(component, 'togglePassword');
      const btn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="button"]');
      btn.click();
      expect(component.togglePassword).toHaveBeenCalled();
    });

    it('should disable the submit button when the form is invalid', () => {
      const btn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(btn.disabled).toBeTrue();
    });

    it('should enable the submit button when the form is valid', () => {
      component.passwordForm.patchValue({ password: 'ValidPass1', confirmPassword: 'ValidPass1' });
      fixture.detectChanges();
      const btn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(btn.disabled).toBeFalse();
    });

    it('should show "Las contraseñas no coinciden" when passwords differ and confirmPassword is touched', () => {
      component.passwordForm.patchValue({ password: 'ValidPass1', confirmPassword: 'Other1234' });
      component.passwordForm.get('confirmPassword')!.markAsTouched();
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Las contraseñas no coinciden');
    });

    it('should NOT show "Las contraseñas no coinciden" when passwords match', () => {
      component.passwordForm.patchValue({ password: 'ValidPass1', confirmPassword: 'ValidPass1' });
      component.passwordForm.get('confirmPassword')!.markAsTouched();
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Las contraseñas no coinciden');
    });
  });
});
