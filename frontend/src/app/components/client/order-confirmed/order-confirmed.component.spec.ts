import {ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {OrderConfirmedComponent} from './order-confirmed.component';
import {UserService} from '../../../services/user.service';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {User} from '../../../models/user.model';

describe('OrderConfirmedComponent', () => {
  let component: OrderConfirmedComponent;
  let fixture: ComponentFixture<OrderConfirmedComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let routerSpy: jasmine.SpyObj<Router>;

  // ─── Mock data ────────────────────────────────────────────────────────────────

  const mockUser: User = {
    id: 'user-1', name: 'Test User', username: 'testuser',
    roles: ['ROLE_USER'], email: 'test@example.com', phone: '123456789',
    addresses: [], cards: [],
    imageInfo: { id: 'img-1', imageUrl: '', s3Key: '', fileName: '' },
    banned: false, deleted: false, selectedShopId: null,
    ordersCount: 3, favouriteProductsCount: 1, connection: null
  };

  // ─── Setup ────────────────────────────────────────────────────────────────────

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj('UserService', ['getLoggedUserInfo']);
    userServiceSpy.getLoggedUserInfo.and.returnValue(of(mockUser));

    routerSpy = jasmine.createSpyObj(
      'Router',
      ['navigate', 'navigateByUrl', 'createUrlTree', 'serializeUrl'],
      { url: '/success', events: new Subject<any>(), navigated: false }
    );
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/order/order-456');
    routerSpy.navigateByUrl.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [
        OrderConfirmedComponent,
        BrowserAnimationsModule
      ],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: Router,      useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({ ref: 'REF-123', id: 'order-456' }),
            snapshot: { paramMap: { get: () => null }, url: [], data: {} },
            params: of({}),
            root: { children: [] }
          }
        },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrderConfirmedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ─── Creation ─────────────────────────────────────────────────────────────────

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // ─── queryParams reading ──────────────────────────────────────────────────────

  it('should read orderId from queryParams', () => {
    expect(component.orderId).toBe('order-456');
  });

  it('should read orderRefCode from queryParams', () => {
    expect(component.orderRefCode).toBe('REF-123');
  });

  it('should default orderId to an empty string before queryParams emit', () => {
    // The initial declared value before ngOnInit runs
    const fresh = TestBed.createComponent(OrderConfirmedComponent);
    expect(fresh.componentInstance.orderId).toBe('');
  });

  it('should default orderRefCode to an empty string before queryParams emit', () => {
    const fresh = TestBed.createComponent(OrderConfirmedComponent);
    expect(fresh.componentInstance.orderRefCode).toBe('');
  });

  // ─── Happy-path load ──────────────────────────────────────────────────────────

  it('should call getLoggedUserInfo on init', () => {
    expect(userServiceSpy.getLoggedUserInfo).toHaveBeenCalled();
  });

  it('should populate user after a successful load', () => {
    expect(component.user).toEqual(mockUser);
  });

  it('should set loading=false after a successful load', () => {
    expect(component.loading).toBeFalse();
  });

  it('should set error=false after a successful load', () => {
    expect(component.error).toBeFalse();
  });

  // ─── Error state ──────────────────────────────────────────────────────────────

  it('should set error=true and loading=false when getLoggedUserInfo fails', () => {
    userServiceSpy.getLoggedUserInfo.and.returnValue(throwError(() => new Error('401')));
    component.loadLoggedUserInfo();
    expect(component.error).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  // ─── DOM: loading / error screen ──────────────────────────────────────────────

  it('should show the loading screen when loading=true', () => {
    component.loading = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-loading-screen')).not.toBeNull();
  });

  it('should show the loading screen when error=true', () => {
    component.loading = false;
    component.error = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-loading-screen')).not.toBeNull();
  });

  it('should hide the loading screen and show main content when loaded successfully', () => {
    expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('¡Pedido Confirmado!');
  });

  // ─── DOM: confirmation details ────────────────────────────────────────────────

  it('should display the user email in the confirmation message', () => {
    expect(fixture.nativeElement.textContent).toContain(mockUser.email);
  });

  it('should display the order reference code', () => {
    expect(fixture.nativeElement.textContent).toContain('REF-123');
  });

  // ─── DOM: navigation buttons ──────────────────────────────────────────────────

  it('should render two RouterLink directives for the navigation buttons', () => {
    const links = fixture.debugElement.queryAll(By.directive(RouterLink));
    expect(links.length).toBeGreaterThanOrEqual(2);
  });

  it('should navigate when the "Ver detalles del pedido" button is clicked', () => {
    const links = fixture.debugElement.queryAll(By.directive(RouterLink));
    const detailBtn = links.find(el =>
      el.nativeElement.textContent.includes('Ver detalles del pedido')
    );
    expect(detailBtn).not.toBeNull();
    detailBtn!.triggerEventHandler('click', {
      button: 0, ctrlKey: false, shiftKey: false, altKey: false, metaKey: false
    });
    expect(routerSpy.navigateByUrl).toHaveBeenCalled();
  });

  it('should navigate when the "Volver a la tienda" button is clicked', () => {
    const links = fixture.debugElement.queryAll(By.directive(RouterLink));
    const backBtn = links.find(el =>
      el.nativeElement.textContent.includes('Volver a la tienda')
    );
    expect(backBtn).not.toBeNull();
    backBtn!.triggerEventHandler('click', {
      button: 0, ctrlKey: false, shiftKey: false, altKey: false, metaKey: false
    });
    expect(routerSpy.navigateByUrl).toHaveBeenCalled();
  });
});
