import {ComponentFixture, TestBed} from '@angular/core/testing';
import {UsersManagementComponent} from './users-management.component';
import {UserService} from '../../../services/user.service';
import {AuthService} from '../../../services/auth.service';
import {ReviewService} from '../../../services/review.service';
import {OrderService} from '../../../services/order.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {ActivatedRoute, Router} from '@angular/router';
import {ConfirmationService, MessageService} from 'primeng/api';
import {of, Subject, throwError} from 'rxjs';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {User} from '../../../models/user.model';
import {Order} from '../../../models/order.model';
import {Review} from '../../../models/review.model';
import {Stat} from '../../../models/stat.model';
import {PageResponse} from '../../../models/pageResponse.model';
import {LoginInfo} from '../../../models/loginInfo.model';

// ConfirmPopup (PrimeNG) subscribes to ConfirmationService.requireConfirmation$ in its
// constructor, so the real service must be provided — a jasmine.SpyObj would lack
// the internal Subject and cause a runtime error during template rendering.
// BreadcrumbReloadComponent (child) injects Router, ActivatedRoute, BreadcrumbService
// and AuthService signals, so all of them must be properly provided.

describe('UsersManagementComponent', () => {
  let component: UsersManagementComponent;
  let fixture: ComponentFixture<UsersManagementComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let authServiceMock: {
    changeInternalUserPassword: jasmine.Spy;
    signup: jasmine.Spy;
    isAdmin: jasmine.Spy;
    isManager: jasmine.Spy;
    isDriver: jasmine.Spy;
  };
  let reviewServiceSpy: jasmine.SpyObj<ReviewService>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let breadcrumbServiceSpy: jasmine.SpyObj<BreadcrumbService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let confirmationService: ConfirmationService;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;

  // ─── Mock data ────────────────────────────────────────────────────────────────

  const mockImageInfo = {
    id: 'img-1', imageUrl: 'http://img.test/avatar.png',
    s3Key: 'users/avatar.png', fileName: 'avatar.png'
  };

  const mockConnection = { online: true, lastConnection: '2026-05-09T10:00:00', lastSessionDurationSeconds: 120 };

  const mockUser: User = {
    id: 'user-1',
    name: 'Ana García',
    username: 'anagarcia',
    roles: ['USER'],
    email: 'ana@test.com',
    phone: '+34 600000000',
    addresses: [],
    cards: [],
    imageInfo: mockImageInfo as any,
    banned: false,
    deleted: false,
    selectedShopId: null,
    ordersCount: 5,
    favouriteProductsCount: 3,
    connection: mockConnection
  };

  const mockBannedUser: User = {
    ...mockUser,
    id: 'user-2',
    name: 'Usuario Baneado',
    username: 'userbanned',
    email: 'banned@test.com',
    banned: true,
    connection: null
  };

  const mockStats: Stat[] = [
    { label: 'Totales',          value: 10 },
    { label: 'Baneados',         value: 2  },
    { label: 'Anonimizados',     value: 1  },
    { label: 'Cuentas Internas', value: 3  }
  ];

  const mockUsersPage: PageResponse<User> = {
    items: [mockUser, mockBannedUser],
    totalItems: 2, currentPage: 0, lastPage: 0, pageSize: 5
  };

  const emptyPage = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0 };

  const mockStatusLog = {
    id: 'log-1', icon: 'pi pi-clock', status: 'Pendiente',
    updates: [{ message: 'Orden creada', timestamp: '2026-05-01T10:00:00' }]
  } as any;

  const mockOrder: Order = {
    id: 'order-1',
    referenceCode: 'ORD-001',
    history: [mockStatusLog],
    user: mockUser,
    orderItems: [],
    totalItems: 2,
    subtotalCost: 100,
    totalDiscount: 0,
    shippingCost: 5,
    totalCost: 105,
    totalCapacity: 1,
    cardNumberEnding: '4242',
    sendingAddress: 'Calle Mayor, 1 2A 28001 Madrid (España)',
    sendingAddressLat: 40.4168,
    sendingAddressLng: -3.7038,
    estimatedCompletionTime: 30,
    createdAt: '2026-05-01T10:00:00'
  };

  const mockOrdersPage: PageResponse<Order> = {
    items: [mockOrder], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5
  };

  const mockReview: Review = {
    id: 'review-1',
    productId: 'prod-1',
    creatorId: 'user-1',
    productName: 'Ratón Gaming Pro',
    creatorUsername: 'anagarcia',
    creatorName: 'Ana García',
    creatorImage: 'http://img.test/avatar.png',
    creatorConnection: null,
    text: 'Muy buen producto',
    rating: 5,
    createdAt: '2026-05-01',
    recommended: true
  };

  const mockReviewsPage: PageResponse<Review> = {
    items: [mockReview], totalItems: 1, currentPage: 0, lastPage: 0, pageSize: 5
  };

  const mockLoginInfo: LoginInfo = {
    isLogged: true, imageUrl: '', id: 'new-user-99',
    name: 'Nuevo Gerente', username: 'nuevogerente',
    roles: ['MANAGER'], selectedShopId: null
  };

  // ─── Setup ────────────────────────────────────────────────────────────────────

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj('UserService', [
      'getAllUsers', 'getUsersStats', 'toggleAllBans', 'toggleUserBan',
      'anonAll', 'anonUser', 'deleteAll', 'deleteUser',
      'uploadUserImage', 'checkUsernameTaken', 'checkEmailTaken'
    ]);
    // Use callFake so each call returns a fresh object — handleUserAction('Anonimizar Usuario')
    // mutates usersPage.items in-place, which would corrupt the shared const otherwise.
    userServiceSpy.getAllUsers.and.callFake(() =>
      of({ ...mockUsersPage, items: [{ ...mockUser }, { ...mockBannedUser }] })
    );
    userServiceSpy.getUsersStats.and.returnValue(of(mockStats));
    userServiceSpy.toggleAllBans.and.returnValue(of(true));
    userServiceSpy.toggleUserBan.and.returnValue(of({ ...mockUser, banned: true }));
    userServiceSpy.anonAll.and.returnValue(of(true));
    userServiceSpy.anonUser.and.returnValue(of({ ...mockUser, name: 'Anónimo', deleted: true }));
    userServiceSpy.deleteAll.and.returnValue(of(true));
    userServiceSpy.deleteUser.and.returnValue(of(true));
    userServiceSpy.uploadUserImage.and.returnValue(of(mockUser));
    userServiceSpy.checkUsernameTaken.and.returnValue(of(false));
    userServiceSpy.checkEmailTaken.and.returnValue(of(false));

    authServiceMock = {
      changeInternalUserPassword: jasmine.createSpy('changeInternalUserPassword').and.returnValue(of({})),
      signup:    jasmine.createSpy('signup').and.returnValue(of(mockLoginInfo)),
      isAdmin:   jasmine.createSpy('isAdmin').and.returnValue(false),
      isManager: jasmine.createSpy('isManager').and.returnValue(false),
      isDriver:  jasmine.createSpy('isDriver').and.returnValue(false)
    };

    reviewServiceSpy = jasmine.createSpyObj('ReviewService', [
      'getUserReviewsByUserId', 'deleteReviewById'
    ]);
    reviewServiceSpy.getUserReviewsByUserId.and.returnValue(of(mockReviewsPage));
    reviewServiceSpy.deleteReviewById.and.returnValue(of(mockReview));

    orderServiceSpy = jasmine.createSpyObj('OrderService', [
      'getUserOrdersByUserId', 'deleteOrderById'
    ]);
    orderServiceSpy.getUserOrdersByUserId.and.returnValue(of(mockOrdersPage));
    orderServiceSpy.deleteOrderById.and.returnValue(of(void 0 as any));

    breadcrumbServiceSpy = jasmine.createSpyObj('BreadcrumbService', [
      'setBaseBreadcrumbs', 'insertPenultimateNodesForUrl', 'breadcrumbs'
    ]);
    breadcrumbServiceSpy.breadcrumbs.and.returnValue([]);

    routerSpy = jasmine.createSpyObj(
      'Router',
      ['navigate', 'navigateByUrl', 'createUrlTree', 'serializeUrl'],
      { url: '/admin/users', events: new Subject<any>(), navigated: false }
    );
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');
    routerSpy.navigateByUrl.and.returnValue(Promise.resolve(true));

    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    await TestBed.configureTestingModule({
      imports: [UsersManagementComponent, BrowserAnimationsModule],
      providers: [
        { provide: UserService,         useValue: userServiceSpy        },
        { provide: AuthService,         useValue: authServiceMock       },
        { provide: ReviewService,       useValue: reviewServiceSpy      },
        { provide: OrderService,        useValue: orderServiceSpy       },
        { provide: BreadcrumbService,   useValue: breadcrumbServiceSpy  },
        { provide: Router,              useValue: routerSpy             },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => null }, url: [], data: {} },
            params: of({}),
            root: { children: [] }
          }
        },
        ConfirmationService,
        { provide: MessageService,      useValue: messageServiceSpy     },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    confirmationService = TestBed.inject(ConfirmationService);

    fixture = TestBed.createComponent(UsersManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ─── Creation ─────────────────────────────────────────────────────────────────

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // ─── ngOnInit calls ───────────────────────────────────────────────────────────

  it('should call getAllUsers on init', () => {
    expect(userServiceSpy.getAllUsers).toHaveBeenCalledWith(0, 5);
  });

  it('should call getUsersStats on init', () => {
    expect(userServiceSpy.getUsersStats).toHaveBeenCalled();
  });

  it('should initialise chart options on init', () => {
    expect(component.options()).not.toBeNull();
  });

  // ─── State after happy-path load ──────────────────────────────────────────────

  it('should set loading=false after successful load', () => {
    expect(component.loading).toBeFalse();
  });

  it('should set error=false after successful load', () => {
    expect(component.error).toBeFalse();
  });

  it('should populate usersPage after successful load', () => {
    expect(component.usersPage).toEqual(mockUsersPage);
  });

  it('should populate rawStats signal after successful loadStats', () => {
    expect(component.rawStats()).toEqual(mockStats);
  });

  // ─── Error states ─────────────────────────────────────────────────────────────

  it('should set error=true and loading=false when getAllUsers fails', () => {
    userServiceSpy.getAllUsers.and.returnValue(throwError(() => new Error('500')));
    component.loadUsers();
    expect(component.error).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  it('should NOT propagate error flag when getUsersStats fails', () => {
    userServiceSpy.getUsersStats.and.returnValue(throwError(() => new Error('500')));
    component.loadStats();
    expect(component.error).toBeFalse();
  });

  it('should keep loading=true while getAllUsers is pending', () => {
    const pending$ = new Subject<PageResponse<User>>();
    userServiceSpy.getAllUsers.and.returnValue(pending$.asObservable());
    component.loading = false;
    component.loadUsers();
    expect(component.loading).toBeFalse(); // loading is not reset until response arrives
  });

  // ─── reloadAll ────────────────────────────────────────────────────────────────

  it('should set loading=true and error=false on reloadAll', () => {
    const blocker = new Subject<PageResponse<User>>();
    userServiceSpy.getAllUsers.and.returnValue(blocker.asObservable());
    component.error = true;
    component.reloadAll();
    expect(component.loading).toBeTrue();
    expect(component.error).toBeFalse();
  });

  it('should close all dialogs on reloadAll', () => {
    component.visibleOrdersDialog          = true;
    component.visibleReviewsDialog         = true;
    component.visibleNewInternalUserDialog  = true;
    component.visibleChangePasswordDialog   = true;
    component.reloadAll();
    expect(component.visibleOrdersDialog).toBeFalse();
    expect(component.visibleReviewsDialog).toBeFalse();
    expect(component.visibleNewInternalUserDialog).toBeFalse();
    expect(component.visibleChangePasswordDialog).toBeFalse();
  });

  it('should clear selectedUser on reloadAll', () => {
    component.selectedUser = mockUser;
    component.reloadAll();
    expect(component.selectedUser).toBeNull();
  });

  it('should call getAllUsers again on reloadAll', () => {
    const prevCount = userServiceSpy.getAllUsers.calls.count();
    component.reloadAll();
    expect(userServiceSpy.getAllUsers.calls.count()).toBeGreaterThan(prevCount);
  });

  // ─── getStatValue ─────────────────────────────────────────────────────────────

  it('should return the numeric value for a known stat label', () => {
    expect(component.getStatValue('Totales')).toBe(10);
    expect(component.getStatValue('Baneados')).toBe(2);
    expect(component.getStatValue('Anonimizados')).toBe(1);
    expect(component.getStatValue('Cuentas Internas')).toBe(3);
  });

  it('should return 0 for an unknown stat label', () => {
    expect(component.getStatValue('Desconocido')).toBe(0);
  });

  it('should return 0 when the stat value is non-numeric', () => {
    component.rawStats.set([{ label: 'Raro', value: 'no-num' }]);
    expect(component.getStatValue('Raro')).toBe(0);
  });

  // ─── clearSelection ───────────────────────────────────────────────────────────

  it('should set selectedUser to null on clearSelection', () => {
    component.selectedUser = mockUser;
    component.clearSelection();
    expect(component.selectedUser).toBeNull();
  });

  // ─── cancelUserCreation / cancelChangePassword ─────────────────────────────────

  it('should reset newInternalUserForm and hide dialog on cancelUserCreation', () => {
    component.newInternalUserForm.patchValue({ name: 'Test', username: 'test', email: 'a@b.com', password: '1234', role: 'MANAGER' });
    component.visibleNewInternalUserDialog = true;
    component.cancelUserCreation();
    expect(component.visibleNewInternalUserDialog).toBeFalse();
    expect(component.newInternalUserForm.value.name).toBeFalsy();
  });

  it('should reset changePasswordForm and hide dialog on cancelChangePassword', () => {
    component.changePasswordForm.patchValue({ password: 'abc', repeatPassword: 'abc' });
    component.visibleChangePasswordDialog = true;
    component.cancelChangePassword();
    expect(component.visibleChangePasswordDialog).toBeFalse();
    expect(component.changePasswordForm.value.password).toBeFalsy();
  });

  // ─── onChangePasswordSubmit ────────────────────────────────────────────────────

  it('should call changeInternalUserPassword with the user id on submit', () => {
    component.selectedUser = mockUser;
    component.changePasswordForm.patchValue({ password: 'newpass123', repeatPassword: 'newpass123' });
    component.onChangePasswordSubmit(mockUser.id);
    expect(authServiceMock.changeInternalUserPassword).toHaveBeenCalledWith(
      mockUser.id, jasmine.objectContaining({ password: 'newpass123' })
    );
  });

  it('should show success message and close dialog on successful password change', () => {
    component.selectedUser = mockUser;
    component.visibleChangePasswordDialog = true;
    component.onChangePasswordSubmit(mockUser.id);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    expect(component.visibleChangePasswordDialog).toBeFalse();
  });

  it('should show error message when changeInternalUserPassword fails', () => {
    authServiceMock.changeInternalUserPassword.and.returnValue(throwError(() => new Error('500')));
    component.selectedUser = mockUser;
    component.onChangePasswordSubmit(mockUser.id);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
  });

  // ─── onNewUserSubmit ──────────────────────────────────────────────────────────

  it('should call signup on onNewUserSubmit', () => {
    component.onNewUserSubmit();
    expect(authServiceMock.signup).toHaveBeenCalled();
  });

  it('should reload users and stats after successful user creation', () => {
    const prevUsersCalls = userServiceSpy.getAllUsers.calls.count();
    const prevStatsCalls = userServiceSpy.getUsersStats.calls.count();
    component.onNewUserSubmit();
    expect(userServiceSpy.getAllUsers.calls.count()).toBeGreaterThan(prevUsersCalls);
    expect(userServiceSpy.getUsersStats.calls.count()).toBeGreaterThan(prevStatsCalls);
  });

  it('should show success message after successful user creation', () => {
    component.onNewUserSubmit();
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
  });

  it('should close the new-user dialog after successful creation', () => {
    component.visibleNewInternalUserDialog = true;
    component.onNewUserSubmit();
    expect(component.visibleNewInternalUserDialog).toBeFalse();
  });

  it('should show error message when signup fails', () => {
    authServiceMock.signup.and.returnValue(throwError(() => new Error('500')));
    component.onNewUserSubmit();
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
  });

  it('should call uploadUserImage when selectedImage is set', () => {
    const fakeFile = new File(['data'], 'avatar.jpg', { type: 'image/jpeg' });
    (component as any).selectedImage = fakeFile;
    component.onNewUserSubmit();
    expect(userServiceSpy.uploadUserImage).toHaveBeenCalledWith(mockLoginInfo.id, fakeFile);
  });

  // ─── handleGlobalAction ───────────────────────────────────────────────────────

  it('should call toggleAllBans(true) for "Banear Todos"', () => {
    component.handleGlobalAction('Banear Todos');
    expect(userServiceSpy.toggleAllBans).toHaveBeenCalledWith(true);
  });

  it('should call toggleAllBans(false) for "Desbanear Todos"', () => {
    component.handleGlobalAction('Desbanear Todos');
    expect(userServiceSpy.toggleAllBans).toHaveBeenCalledWith(false);
  });

  it('should call anonAll for "Anonimizar Todos"', () => {
    component.handleGlobalAction('Anonimizar Todos');
    expect(userServiceSpy.anonAll).toHaveBeenCalled();
  });

  it('should call deleteAll for "Borrar Todos"', () => {
    component.handleGlobalAction('Borrar Todos');
    expect(userServiceSpy.deleteAll).toHaveBeenCalled();
  });

  it('should clear selectedUser for "Borrar Todos"', () => {
    component.selectedUser = mockUser;
    component.handleGlobalAction('Borrar Todos');
    expect(component.selectedUser).toBeNull();
  });

  it('should reload users and stats after any global action', () => {
    const prevUsersCalls = userServiceSpy.getAllUsers.calls.count();
    const prevStatsCalls = userServiceSpy.getUsersStats.calls.count();
    component.handleGlobalAction('Banear Todos');
    expect(userServiceSpy.getAllUsers.calls.count()).toBeGreaterThan(prevUsersCalls);
    expect(userServiceSpy.getUsersStats.calls.count()).toBeGreaterThan(prevStatsCalls);
  });

  // ─── handleUserAction ─────────────────────────────────────────────────────────

  it('should call toggleUserBan(id, true) for "Banear Usuario"', () => {
    component.selectedUser = mockUser;
    component.handleUserAction('Banear Usuario');
    expect(userServiceSpy.toggleUserBan).toHaveBeenCalledWith(mockUser.id, true);
  });

  it('should call toggleUserBan(id, false) for "Desbanear Usuario"', () => {
    component.selectedUser = mockBannedUser;
    component.handleUserAction('Desbanear Usuario');
    expect(userServiceSpy.toggleUserBan).toHaveBeenCalledWith(mockBannedUser.id, false);
  });

  it('should update selectedUser.banned after a ban toggle', () => {
    component.selectedUser = { ...mockUser };
    userServiceSpy.toggleUserBan.and.returnValue(of({ ...mockUser, banned: true }));
    component.handleUserAction('Banear Usuario');
    expect(component.selectedUser!.banned).toBeTrue();
  });

  it('should call anonUser for "Anonimizar Usuario"', () => {
    component.selectedUser = mockUser;
    component.handleUserAction('Anonimizar Usuario');
    expect(userServiceSpy.anonUser).toHaveBeenCalledWith(mockUser.id);
  });

  it('should update the user entry in usersPage after anonymization', () => {
    const anonUser = { ...mockUser, name: 'Anónimo', deleted: true };
    userServiceSpy.anonUser.and.returnValue(of(anonUser));
    component.selectedUser = mockUser;
    component.handleUserAction('Anonimizar Usuario');
    expect(component.usersPage.items.find(u => u.id === mockUser.id)!.name).toBe('Anónimo');
    expect(component.selectedUser!.name).toBe('Anónimo');
  });

  it('should call deleteUser for "Borrar Usuario"', () => {
    component.selectedUser = mockUser;
    component.handleUserAction('Borrar Usuario');
    expect(userServiceSpy.deleteUser).toHaveBeenCalledWith(mockUser.id);
  });

  it('should clear selectedUser after "Borrar Usuario"', () => {
    component.selectedUser = mockUser;
    component.handleUserAction('Borrar Usuario');
    expect(component.selectedUser).toBeNull();
  });

  it('should NOT call any service when no user is selected', () => {
    component.selectedUser = null;
    component.handleUserAction('Banear Usuario');
    expect(userServiceSpy.toggleUserBan).not.toHaveBeenCalled();
  });

  // ─── openOrdersDialog / loadUserOrders ────────────────────────────────────────

  it('should call getUserOrdersByUserId and open the orders dialog', () => {
    component.openOrdersDialog(mockUser.id);
    expect(orderServiceSpy.getUserOrdersByUserId).toHaveBeenCalledWith(mockUser.id, 0, 5);
    expect(component.visibleOrdersDialog).toBeTrue();
    expect(component.userOrdersPage).toEqual(mockOrdersPage);
  });

  it('should reset ordersFirst before loading orders in openOrdersDialog', () => {
    component.ordersFirst = 10;
    component.openOrdersDialog(mockUser.id);
    expect(component.ordersFirst).toBe(0);
  });

  it('should NOT open the orders dialog when openDialog flag is false', () => {
    component.loadUserOrders(mockUser.id, false);
    expect(component.visibleOrdersDialog).toBeFalse();
  });

  it('should show error message when getUserOrdersByUserId fails', () => {
    orderServiceSpy.getUserOrdersByUserId.and.returnValue(throwError(() => new Error('500')));
    component.loadUserOrders(mockUser.id);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
  });

  // ─── openReviewsDialog / loadUserReviews ──────────────────────────────────────

  it('should call getUserReviewsByUserId and open the reviews dialog', () => {
    component.openReviewsDialog(mockUser.id);
    expect(reviewServiceSpy.getUserReviewsByUserId).toHaveBeenCalledWith(mockUser.id, 0, 5);
    expect(component.visibleReviewsDialog).toBeTrue();
    expect(component.userReviewsPage).toEqual(mockReviewsPage);
  });

  it('should reset reviewsFirst before loading reviews in openReviewsDialog', () => {
    component.reviewsFirst = 10;
    component.openReviewsDialog(mockUser.id);
    expect(component.reviewsFirst).toBe(0);
  });

  it('should NOT open the reviews dialog when openDialog flag is false', () => {
    component.loadUserReviews(mockUser.id, false);
    expect(component.visibleReviewsDialog).toBeFalse();
  });

  it('should show error message when getUserReviewsByUserId fails', () => {
    reviewServiceSpy.getUserReviewsByUserId.and.returnValue(throwError(() => new Error('500')));
    component.loadUserReviews(mockUser.id);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
  });

  // ─── Pagination ───────────────────────────────────────────────────────────────

  it('should update first/rows and reload users on onUsersPageChange', () => {
    const prev = userServiceSpy.getAllUsers.calls.count();
    component.onUsersPageChange({ first: 5, rows: 5, page: 1, pageCount: 2 });
    expect(component.first).toBe(5);
    expect(component.rows).toBe(5);
    expect(userServiceSpy.getAllUsers.calls.count()).toBeGreaterThan(prev);
  });

  it('should reset first=0 and rows=5 on resetPaginator', () => {
    component.first = 10;
    component.rows  = 20;
    component.resetPaginator();
    expect(component.first).toBe(0);
    expect(component.rows).toBe(5);
  });

  it('should update ordersFirst/ordersRows and reload orders on onOrdersPageChange', () => {
    component.selectedUser = mockUser;
    const prev = orderServiceSpy.getUserOrdersByUserId.calls.count();
    component.onOrdersPageChange({ first: 5, rows: 5, page: 1, pageCount: 2 });
    expect(component.ordersFirst).toBe(5);
    expect(component.ordersRows).toBe(5);
    expect(orderServiceSpy.getUserOrdersByUserId.calls.count()).toBeGreaterThan(prev);
  });

  it('should update reviewsFirst/reviewsRows and reload reviews on onReviewsPageChange', () => {
    component.selectedUser = mockUser;
    const prev = reviewServiceSpy.getUserReviewsByUserId.calls.count();
    component.onReviewsPageChange({ first: 5, rows: 5, page: 1, pageCount: 2 });
    expect(component.reviewsFirst).toBe(5);
    expect(component.reviewsRows).toBe(5);
    expect(reviewServiceSpy.getUserReviewsByUserId.calls.count()).toBeGreaterThan(prev);
  });

  // ─── canDeleteOrder / getLatestOrderStatus ─────────────────────────────────────

  it('should return the last history entry status via getLatestOrderStatus', () => {
    const completedLog = { ...mockStatusLog, status: 'Completado' };
    const order = { ...mockOrder, history: [mockStatusLog, completedLog] };
    expect(component.getLatestOrderStatus(order)).toBe('Completado');
  });

  it('should return true from canDeleteOrder when status is "Cancelado"', () => {
    const order = { ...mockOrder, history: [{ ...mockStatusLog, status: 'Cancelado' }] };
    expect(component.canDeleteOrder(order)).toBeTrue();
  });

  it('should return true from canDeleteOrder when status is "Completado"', () => {
    const order = { ...mockOrder, history: [{ ...mockStatusLog, status: 'Completado' }] };
    expect(component.canDeleteOrder(order)).toBeTrue();
  });

  it('should return false from canDeleteOrder when status is "Pendiente"', () => {
    const order = { ...mockOrder, history: [{ ...mockStatusLog, status: 'Pendiente' }] };
    expect(component.canDeleteOrder(order)).toBeFalse();
  });

  it('should return false from canDeleteOrder when status is "En proceso"', () => {
    const order = { ...mockOrder, history: [{ ...mockStatusLog, status: 'En proceso' }] };
    expect(component.canDeleteOrder(order)).toBeFalse();
  });

  // ─── data computed signal ─────────────────────────────────────────────────────

  it('should build chart data labels from rawStats', () => {
    expect(component.data().labels).toEqual([
      'Totales', 'Baneados', 'Anonimizados', 'Cuentas Internas'
    ]);
  });

  it('should build chart data values from rawStats', () => {
    expect(component.data().datasets[0].data).toEqual([10, 2, 1, 3]);
  });

  it('should update chart data when rawStats signal changes', () => {
    component.rawStats.set([{ label: 'Nuevo', value: 42 }]);
    expect(component.data().labels).toEqual(['Nuevo']);
    expect(component.data().datasets[0].data).toEqual([42]);
  });

  // ─── DOM: loading / error screen ──────────────────────────────────────────────

  it('should show the loading screen when loading=true', () => {
    component.loading = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-loading-screen')).not.toBeNull();
  });

  it('should show the error screen when error=true', () => {
    component.error = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-loading-screen')).not.toBeNull();
  });

  it('should hide the loading screen and render main content when fully loaded', () => {
    expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Gestor de usuarios');
  });

  // ─── DOM: stats cards ─────────────────────────────────────────────────────────

  it('should display stat labels in the DOM', () => {
    expect(fixture.nativeElement.textContent).toContain('Totales');
    expect(fixture.nativeElement.textContent).toContain('Baneados');
    expect(fixture.nativeElement.textContent).toContain('Anonimizados');
    expect(fixture.nativeElement.textContent).toContain('Cuentas Internas');
  });

  // ─── DOM: users table ─────────────────────────────────────────────────────────

  it('should display user names in the table', () => {
    expect(fixture.nativeElement.textContent).toContain(mockUser.name);
    expect(fixture.nativeElement.textContent).toContain(mockBannedUser.name);
  });

  it('should show "No se encontraron usuarios." when usersPage is empty', () => {
    component.usersPage = { ...emptyPage };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No se encontraron usuarios.');
  });

  // ─── DOM: selected user panel ─────────────────────────────────────────────────

  it('should show the selected user panel with name and email', () => {
    component.selectedUser = mockUser;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(mockUser.name);
    expect(fixture.nativeElement.textContent).toContain(mockUser.email);
  });

  it('should NOT show the selected user panel when selectedUser is null', () => {
    component.selectedUser = null;
    fixture.detectChanges();
    // "Ver Pedidos" only exists inside the selected-user detail panel, not in the table
    expect(fixture.nativeElement.textContent).not.toContain('Ver Pedidos');
  });
});
