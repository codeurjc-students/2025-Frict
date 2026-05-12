import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';

import {GoogleAuthComponent} from './google-auth.component';
import {AuthService} from '../../../services/auth.service';

// ── Helpers ───────────────────────────────────────────────────────────────────

function createGoogleMock(): any {
  return {
    accounts: {
      id: {
        initialize: jasmine.createSpy('initialize'),
        renderButton: jasmine.createSpy('renderButton'),
        prompt: jasmine.createSpy('prompt')
      }
    }
  };
}

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('GoogleAuthComponent', () => {
  let component: GoogleAuthComponent;
  let fixture: ComponentFixture<GoogleAuthComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    delete (window as any).google;

    authServiceSpy = jasmine.createSpyObj('AuthService', ['googleLogin']);
    authServiceSpy.googleLogin.and.callFake(() => of({}));

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: new Subject().asObservable(),
      url: '/'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    await TestBed.configureTestingModule({
      imports: [GoogleAuthComponent],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
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

    fixture = TestBed.createComponent(GoogleAuthComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(); // triggers ngAfterViewInit → initGoogleButton (google undefined by default)
  });

  afterEach(() => {
    delete (window as any).google;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Google SDK not loaded ─────────────────────────────────────────────────────

  describe('google SDK not loaded', () => {
    it('should log a warning when the google global is undefined', () => {
      spyOn(console, 'warn');
      (component as any)['initGoogleButton']();
      expect(console.warn).toHaveBeenCalledWith('Google script not loaded');
    });

    it('should not throw when google is unavailable', () => {
      expect(() => (component as any)['initGoogleButton']()).not.toThrow();
    });
  });

  // ── Google SDK loaded ─────────────────────────────────────────────────────────

  describe('google SDK loaded', () => {
    let googleMock: any;

    beforeEach(() => {
      googleMock = createGoogleMock();
      (window as any).google = googleMock;
      (component as any)['initGoogleButton']();
    });

    it('should call google.accounts.id.initialize with a client_id string', () => {
      expect(googleMock.accounts.id.initialize).toHaveBeenCalledWith(
        jasmine.objectContaining({ client_id: jasmine.any(String) })
      );
    });

    it('should pass auto_select: false to initialize', () => {
      const args = googleMock.accounts.id.initialize.calls.mostRecent().args[0];
      expect(args.auto_select).toBeFalse();
    });

    it('should pass cancel_on_tap_outside: true to initialize', () => {
      const args = googleMock.accounts.id.initialize.calls.mostRecent().args[0];
      expect(args.cancel_on_tap_outside).toBeTrue();
    });

    it('should pass a callback function to initialize', () => {
      const args = googleMock.accounts.id.initialize.calls.mostRecent().args[0];
      expect(typeof args.callback).toBe('function');
    });

    it('should call renderButton on the container element', () => {
      expect(googleMock.accounts.id.renderButton).toHaveBeenCalledWith(
        component.googleBtn.nativeElement,
        jasmine.any(Object)
      );
    });

    it('should call google.accounts.id.prompt', () => {
      expect(googleMock.accounts.id.prompt).toHaveBeenCalled();
    });

    it('should catch errors thrown by the google SDK without propagating', () => {
      spyOn(console, 'error');
      googleMock.accounts.id.initialize.and.throwError('SDK error');
      expect(() => (component as any)['initGoogleButton']()).not.toThrow();
      expect(console.error).toHaveBeenCalled();
    });
  });

  // ── handleGoogleLogin ─────────────────────────────────────────────────────────

  describe('handleGoogleLogin', () => {
    it('should call authService.googleLogin with the credential from the response', () => {
      (component as any)['handleGoogleLogin']({ credential: 'google-token-123' });
      expect(authServiceSpy.googleLogin).toHaveBeenCalledWith('google-token-123');
    });

    it('should navigate to "/" after a successful login', () => {
      (component as any)['handleGoogleLogin']({ credential: 'token' });
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should log an error when googleLogin fails', () => {
      spyOn(console, 'error');
      authServiceSpy.googleLogin.and.callFake(() => throwError(() => new Error('login-fail')));
      (component as any)['handleGoogleLogin']({ credential: 'bad-token' });
      expect(console.error).toHaveBeenCalled();
    });

    it('should NOT navigate when googleLogin fails', () => {
      spyOn(console, 'error');
      authServiceSpy.googleLogin.and.callFake(() => throwError(() => new Error('login-fail')));
      (component as any)['handleGoogleLogin']({ credential: 'bad-token' });
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should be reachable via the callback passed to google.accounts.id.initialize', () => {
      const gMock = createGoogleMock();
      (window as any).google = gMock;
      (component as any)['initGoogleButton']();
      const callback = gMock.accounts.id.initialize.calls.mostRecent().args[0].callback;
      callback({ credential: 'callback-token' });
      expect(authServiceSpy.googleLogin).toHaveBeenCalledWith('callback-token');
    });
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should render the google button container div', () => {
      expect(fixture.nativeElement.querySelector('div.w-full')).toBeTruthy();
    });
  });
});
