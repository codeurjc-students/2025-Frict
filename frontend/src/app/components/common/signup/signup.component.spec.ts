import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';

import {SignupComponent} from './signup.component';
import {AuthService} from '../../../services/auth.service';
import {UserService} from '../../../services/user.service';
import {LoginInfo} from '../../../models/loginInfo.model';

// ── Helpers ───────────────────────────────────────────────────────────────────

const STUB_LOGIN_INFO: LoginInfo = {
  isLogged: true, imageUrl: '/test.png', id: 'user-1',
  name: 'Test User', username: 'testuser', roles: ['USER'], selectedShopId: null
};

function makeFileEvent(file: File): Event {
  const fileList = { 0: file, length: 1, item: (i: number) => file } as unknown as FileList;
  const input = { files: fileList } as unknown as HTMLInputElement;
  return { target: input } as unknown as Event;
}

// ── Spec ──────────────────────────────────────────────────────────────────────

describe('SignupComponent', () => {
  let component: SignupComponent;
  let fixture: ComponentFixture<SignupComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['signup', 'googleLogin']);
    authServiceSpy.signup.and.callFake(() => of({ ...STUB_LOGIN_INFO }));
    authServiceSpy.googleLogin.and.callFake(() => of({}));

    userServiceSpy = jasmine.createSpyObj('UserService', [
      'checkUsernameTaken', 'checkEmailTaken', 'uploadUserImage'
    ]);
    userServiceSpy.checkUsernameTaken.and.callFake(() => of(false));
    userServiceSpy.checkEmailTaken.and.callFake(() => of(false));
    userServiceSpy.uploadUserImage.and.callFake(() => of({} as any));

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: new Subject().asObservable(),
      url: '/signup'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    await TestBed.configureTestingModule({
      imports: [SignupComponent],
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

    fixture = TestBed.createComponent(SignupComponent);
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

    it('should initialize selectedImage to null', () => {
      expect(component.selectedImage).toBeNull();
    });

    it('should initialize registerForm with empty name', () => {
      expect(component.registerForm.get('name')?.value).toBe('');
    });

    it('should initialize registerForm with empty username', () => {
      expect(component.registerForm.get('username')?.value).toBe('');
    });

    it('should initialize registerForm with empty email', () => {
      expect(component.registerForm.get('email')?.value).toBe('');
    });

    it('should initialize registerForm with empty password', () => {
      expect(component.registerForm.get('password')?.value).toBe('');
    });

    it('should have the registerForm invalid by default', () => {
      expect(component.registerForm.invalid).toBeTrue();
    });
  });

  // ── Getters ───────────────────────────────────────────────────────────────────

  describe('usernameControl getter', () => {
    it('should return the username form control', () => {
      expect(component.usernameControl).toBe(component.registerForm.get('username'));
    });
  });

  describe('emailControl getter', () => {
    it('should return the email form control', () => {
      expect(component.emailControl).toBe(component.registerForm.get('email'));
    });
  });

  // ── Username async validator ───────────────────────────────────────────────────

  describe('username async validator', () => {
    it('should have usernameTaken error when username is already taken', fakeAsync(() => {
      userServiceSpy.checkUsernameTaken.and.callFake(() => of(true));
      component.registerForm.get('username')!.setValue('takenuser');
      tick(750);
      expect(component.registerForm.get('username')!.hasError('usernameTaken')).toBeTrue();
    }));

    it('should have no error when username is available', fakeAsync(() => {
      userServiceSpy.checkUsernameTaken.and.callFake(() => of(false));
      component.registerForm.get('username')!.setValue('freeuser');
      tick(750);
      expect(component.registerForm.get('username')!.errors).toBeNull();
    }));

    it('should NOT call checkUsernameTaken when username is empty', fakeAsync(() => {
      userServiceSpy.checkUsernameTaken.calls.reset();
      component.registerForm.get('username')!.setValue('');
      tick(750);
      expect(userServiceSpy.checkUsernameTaken).not.toHaveBeenCalled();
    }));

    it('should have required error when username is empty and touched', () => {
      component.registerForm.get('username')!.markAsTouched();
      expect(component.registerForm.get('username')!.hasError('required')).toBeTrue();
    });
  });

  // ── Email async validator ─────────────────────────────────────────────────────

  describe('email async validator', () => {
    it('should have emailTaken error when email is already taken', fakeAsync(() => {
      userServiceSpy.checkEmailTaken.and.callFake(() => of(true));
      component.registerForm.get('email')!.setValue('taken@test.com');
      tick(750);
      expect(component.registerForm.get('email')!.hasError('emailTaken')).toBeTrue();
    }));

    it('should have no error when email is available', fakeAsync(() => {
      userServiceSpy.checkEmailTaken.and.callFake(() => of(false));
      component.registerForm.get('email')!.setValue('free@test.com');
      tick(750);
      expect(component.registerForm.get('email')!.errors).toBeNull();
    }));

    it('should set email format error for an invalid email address', fakeAsync(() => {
      component.registerForm.get('email')!.setValue('not-an-email');
      tick(750);
      expect(component.registerForm.get('email')!.hasError('email')).toBeTrue();
    }));

    it('should NOT call checkEmailTaken when email is empty', fakeAsync(() => {
      userServiceSpy.checkEmailTaken.calls.reset();
      component.registerForm.get('email')!.setValue('');
      tick(750);
      expect(userServiceSpy.checkEmailTaken).not.toHaveBeenCalled();
    }));
  });

  // ── togglePassword ────────────────────────────────────────────────────────────

  describe('togglePassword', () => {
    it('should flip showPassword from false to true', () => {
      component.togglePassword();
      expect(component.showPassword).toBeTrue();
    });

    it('should flip showPassword back to false on a second call', () => {
      component.togglePassword();
      component.togglePassword();
      expect(component.showPassword).toBeFalse();
    });
  });

  // ── changeSelectedImage ───────────────────────────────────────────────────────

  describe('changeSelectedImage', () => {
    it('should set selectedImage from a valid file input event', () => {
      const file = new File(['data'], 'photo.jpg', { type: 'image/jpeg' });
      component.changeSelectedImage(makeFileEvent(file));
      expect(component.selectedImage).toBe(file);
    });

    it('should NOT update selectedImage when input has no files', () => {
      component.selectedImage = null;
      const input = { files: null } as unknown as HTMLInputElement;
      component.changeSelectedImage({ target: input } as unknown as Event);
      expect(component.selectedImage).toBeNull();
    });

    it('should NOT update selectedImage when files list is empty', () => {
      component.selectedImage = null;
      const input = { files: { length: 0 } } as unknown as HTMLInputElement;
      component.changeSelectedImage({ target: input } as unknown as Event);
      expect(component.selectedImage).toBeNull();
    });
  });

  // ── onSubmit ──────────────────────────────────────────────────────────────────

  describe('onSubmit', () => {
    it('should call authService.signup with the current form value', () => {
      component.registerForm.patchValue({
        name: 'Test User', username: 'testuser',
        email: 'test@test.com', password: 'pass123'
      });
      component.onSubmit();
      expect(authServiceSpy.signup).toHaveBeenCalledWith(
        jasmine.objectContaining({ name: 'Test User', username: 'testuser' })
      );
    });

    it('should navigate to /login on success when no image is selected', () => {
      component.selectedImage = null;
      component.onSubmit();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should NOT call uploadUserImage when selectedImage is null', () => {
      component.selectedImage = null;
      component.onSubmit();
      expect(userServiceSpy.uploadUserImage).not.toHaveBeenCalled();
    });

    it('should call uploadUserImage with the loginInfo id and selected file on success', () => {
      const file = new File([''], 'photo.jpg');
      component.selectedImage = file;
      component.onSubmit();
      expect(userServiceSpy.uploadUserImage).toHaveBeenCalledWith('user-1', file);
    });

    it('should navigate to /login after a successful image upload', () => {
      component.selectedImage = new File([''], 'photo.jpg');
      userServiceSpy.uploadUserImage.and.callFake(() => of({} as any));
      component.onSubmit();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should navigate to /error on signup failure', () => {
      authServiceSpy.signup.and.callFake(() => throwError(() => new Error('500')));
      component.onSubmit();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/error']);
    });

    it('should set loading=true (call signup) before the response arrives', () => {
      const pending$ = new Subject<LoginInfo>();
      authServiceSpy.signup.and.callFake(() => pending$.asObservable());
      component.onSubmit();
      expect(authServiceSpy.signup).toHaveBeenCalled();
    });
  });

  // ── uploadUserImage (private) ─────────────────────────────────────────────────

  describe('uploadUserImage (private)', () => {
    it('should call userService.uploadUserImage with the correct userId and file', () => {
      const file = new File([''], 'photo.jpg');
      (component as any)['uploadUserImage']('abc-123', file);
      expect(userServiceSpy.uploadUserImage).toHaveBeenCalledWith('abc-123', file);
    });

    it('should navigate to /login on successful upload', () => {
      userServiceSpy.uploadUserImage.and.callFake(() => of({} as any));
      (component as any)['uploadUserImage']('abc-123', new File([''], 'photo.jpg'));
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should call window.alert on upload failure', () => {
      spyOn(window, 'alert');
      userServiceSpy.uploadUserImage.and.callFake(() => throwError(() => new Error('upload fail')));
      (component as any)['uploadUserImage']('abc-123', new File([''], 'photo.jpg'));
      expect(window.alert).toHaveBeenCalled();
    });
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should show the TecHub brand', () => {
      expect(fixture.nativeElement.textContent).toContain('TecHub');
    });

    it('should show the "Crea tu cuenta gratis" heading', () => {
      expect(fixture.nativeElement.textContent).toContain('Crea tu cuenta gratis');
    });

    it('should show the "O completa el formulario" divider', () => {
      expect(fixture.nativeElement.textContent).toContain('O completa el formulario');
    });

    it('should render the name input', () => {
      expect(fixture.nativeElement.querySelector('#name')).toBeTruthy();
    });

    it('should render the username input', () => {
      expect(fixture.nativeElement.querySelector('#username')).toBeTruthy();
    });

    it('should render the email input', () => {
      expect(fixture.nativeElement.querySelector('#email')).toBeTruthy();
    });

    it('should render the password input', () => {
      expect(fixture.nativeElement.querySelector('#password')).toBeTruthy();
    });

    it('should render the image file input', () => {
      expect(fixture.nativeElement.querySelector('#image')).toBeTruthy();
    });

    it('should render the "Crear cuenta" submit button', () => {
      expect(fixture.nativeElement.textContent).toContain('Crear cuenta');
    });

    it('should render a link to /login', () => {
      expect(fixture.nativeElement.querySelector('a[routerLink="/login"]')).toBeTruthy();
    });

    it('should render a link to /', () => {
      expect(fixture.nativeElement.querySelector('a[routerLink="/"]')).toBeTruthy();
    });

    it('should have the submit button disabled when form is invalid', () => {
      const btn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(btn.disabled).toBeTrue();
    });

    it('should enable the submit button when form is valid', fakeAsync(() => {
      component.registerForm.patchValue({
        name: 'Test User', username: 'freeuser',
        email: 'free@test.com', password: 'password123'
      });
      tick(750);
      fixture.detectChanges();
      const btn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(btn.disabled).toBeFalse();
    }));

    it('should show password input as type "password" by default', () => {
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

    it('should show "El usuario ya existe." when username is taken and field is touched', fakeAsync(() => {
      userServiceSpy.checkUsernameTaken.and.callFake(() => of(true));
      const ctrl = component.registerForm.get('username')!;
      ctrl.setValue('takenuser');
      ctrl.markAsTouched();
      tick(750);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('El usuario ya existe.');
    }));

    it('should show "Requerido." for username when empty and touched', () => {
      component.registerForm.get('username')!.markAsTouched();
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Requerido.');
    });

    it('should show "El correo ya existe." when email is taken and field is touched', fakeAsync(() => {
      userServiceSpy.checkEmailTaken.and.callFake(() => of(true));
      const ctrl = component.registerForm.get('email')!;
      ctrl.setValue('taken@test.com');
      ctrl.markAsTouched();
      tick(750);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('El correo ya existe.');
    }));

    it('should show "Correo inválido." when email format is wrong and field is touched', fakeAsync(() => {
      const ctrl = component.registerForm.get('email')!;
      ctrl.setValue('not-an-email');
      ctrl.markAsTouched();
      tick(750);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Correo inválido.');
    }));
  });
});
