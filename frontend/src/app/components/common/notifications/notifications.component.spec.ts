import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {MessageService} from 'primeng/api';

import {NotificationsComponent} from './notifications.component';
import {NotificationService} from '../../../services/notification.service';
import {UiService} from '../../../utils/ui.service';
import {AuthService} from '../../../services/auth.service';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {Notification} from '../../../models/notification.model';
import {PageResponse} from '../../../models/pageResponse.model';

// ── Helpers ───────────────────────────────────────────────────────────────────

function makeNotif(id: string, read = false): Notification {
  return { id, subject: `Subject ${id}`, description: `Desc ${id}`, timestamp: '2025-01-01T10:00:00', read, type: 'sistema' };
}

function makePage(items: Notification[], total = items.length): PageResponse<Notification> {
  return { items, totalItems: total, currentPage: 0, lastPage: 0, pageSize: 5 };
}

const EMPTY_PAGE = makePage([]);

const STUB_VISUALS = { color: 'bg-blue-50 text-blue-600 border-blue-200', icon: 'pi pi-bell', tag: 'info' as const };

// ── Spec ───────────────────────────────────────────────────────────────────────

describe('NotificationsComponent', () => {
  let component: NotificationsComponent;
  let fixture: ComponentFixture<NotificationsComponent>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let uiServiceMock: { getVisualsByType: jasmine.Spy };
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    notificationServiceSpy = jasmine.createSpyObj('NotificationService', [
      'getNotificationsByTypePage', 'markAsRead', 'markAllAsRead', 'deleteNotification'
    ]);
    notificationServiceSpy.getNotificationsByTypePage.and.callFake(() => of(EMPTY_PAGE));
    notificationServiceSpy.markAllAsRead.and.callFake(() => of(true));
    notificationServiceSpy.deleteNotification.and.callFake(() => of(true));

    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    uiServiceMock = { getVisualsByType: jasmine.createSpy('getVisualsByType').and.returnValue(STUB_VISUALS) };

    authServiceSpy = jasmine.createSpyObj('AuthService', ['isAdmin', 'isManager', 'isDriver']);
    authServiceSpy.isAdmin.and.returnValue(false);
    authServiceSpy.isManager.and.returnValue(false);
    authServiceSpy.isDriver.and.returnValue(false);

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: new Subject().asObservable(),
      url: '/notifications'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    await TestBed.configureTestingModule({
      imports: [NotificationsComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: UiService, useValue: uiServiceMock },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: BreadcrumbService, useValue: { breadcrumbs: jasmine.createSpy().and.returnValue([]), setBaseBreadcrumbs: jasmine.createSpy() } },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            root: { children: [] },
            snapshot: { paramMap: { get: () => null }, queryParamMap: { get: () => null }, url: [], data: {} },
            queryParams: of({})
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(); // triggers ngOnInit → loadNotifications → loading=false
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Default state ─────────────────────────────────────────────────────────────

  describe('default state', () => {
    it('should have loading=false after ngOnInit resolves', () => {
      expect(component.loading).toBeFalse();
    });

    it('should have error=false initially', () => {
      expect(component.error).toBeFalse();
    });

    it('should default first to 0', () => {
      expect(component.first).toBe(0);
    });

    it('should default rows to 5', () => {
      expect(component.rows).toBe(5);
    });

    it('should have no selected notification', () => {
      expect(component.selectedNotification()).toBeNull();
    });
  });

  // ── ngOnInit ──────────────────────────────────────────────────────────────────

  describe('ngOnInit', () => {
    it('should call getNotificationsByTypePage with (0, 5, "") on init', () => {
      expect(notificationServiceSpy.getNotificationsByTypePage).toHaveBeenCalledWith(0, 5, '');
    });

    it('should populate notificationsPage from the service response', () => {
      const notif = makeNotif('1');
      notificationServiceSpy.getNotificationsByTypePage.and.callFake(() => of(makePage([notif])));
      component.ngOnInit();
      expect(component.notificationsPage.items).toEqual([notif]);
    });

    it('should auto-select the first notification when none is selected', () => {
      const notif = makeNotif('1', true);
      notificationServiceSpy.getNotificationsByTypePage.and.callFake(() => of(makePage([notif])));
      component.ngOnInit();
      expect(component.selectedNotification()).toBe(notif);
    });

    it('should NOT change the selection when a notification is already selected', () => {
      const existing = makeNotif('existing', true);
      component.selectedNotification.set(existing);
      const newNotif = makeNotif('new', true);
      notificationServiceSpy.getNotificationsByTypePage.and.callFake(() => of(makePage([newNotif])));
      component.loadNotifications();
      expect(component.selectedNotification()).toBe(existing);
    });
  });

  // ── loadNotifications ─────────────────────────────────────────────────────────

  describe('loadNotifications', () => {
    it('should set listLoading=true when called after the initial load', () => {
      const pending$ = new Subject<PageResponse<Notification>>();
      notificationServiceSpy.getNotificationsByTypePage.and.callFake(() => pending$.asObservable());
      component.loadNotifications(); // loading is already false → listLoading should be set
      expect(component.listLoading).toBeTrue();
    });

    it('should set loading=false and listLoading=false on success', () => {
      expect(component.loading).toBeFalse();
      expect(component.listLoading).toBeFalse();
    });

    it('should set loading=false and error=true on failure', () => {
      notificationServiceSpy.getNotificationsByTypePage.and.callFake(() => throwError(() => new Error()));
      component.loadNotifications();
      expect(component.loading).toBeFalse();
      expect(component.error).toBeTrue();
    });

    it('should show an error toast when loading fails', () => {
      notificationServiceSpy.getNotificationsByTypePage.and.callFake(() => throwError(() => new Error()));
      component.loadNotifications();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── reloadAll ─────────────────────────────────────────────────────────────────

  describe('reloadAll', () => {
    it('should set loading=true before the response arrives', () => {
      const pending$ = new Subject<PageResponse<Notification>>();
      notificationServiceSpy.getNotificationsByTypePage.and.callFake(() => pending$.asObservable());
      component.reloadAll();
      expect(component.loading).toBeTrue();
    });

    it('should reset error to false', () => {
      component.error = true;
      component.reloadAll();
      expect(component.error).toBeFalse();
    });

    it('should clear selectedNotification', () => {
      component.selectedNotification.set(makeNotif('1', true));
      component.reloadAll();
      // After sync response, auto-select may kick in; confirm it reset to null first (checked via empty page)
      notificationServiceSpy.getNotificationsByTypePage.and.callFake(() => of(EMPTY_PAGE));
      component.reloadAll();
      expect(component.selectedNotification()).toBeNull();
    });

    it('should reset first to 0 and reload from the first page', () => {
      component.first = 10;
      notificationServiceSpy.getNotificationsByTypePage.calls.reset();
      component.reloadAll();
      expect(notificationServiceSpy.getNotificationsByTypePage).toHaveBeenCalledWith(0, 5, '');
    });
  });

  // ── onPageChange ──────────────────────────────────────────────────────────────

  describe('onPageChange', () => {
    it('should update first and rows from the event', () => {
      component.onPageChange({ first: 5, rows: 10 });
      expect(component.first).toBe(5);
      expect(component.rows).toBe(10);
    });

    it('should reload notifications after a page change', () => {
      notificationServiceSpy.getNotificationsByTypePage.calls.reset();
      component.onPageChange({ first: 5, rows: 5 });
      expect(notificationServiceSpy.getNotificationsByTypePage).toHaveBeenCalledWith(1, 5, '');
    });

    it('should default first to 0 and rows to 5 when event values are undefined', () => {
      component.onPageChange({});
      expect(component.first).toBe(0);
      expect(component.rows).toBe(5);
    });
  });

  // ── selectNotification ────────────────────────────────────────────────────────

  describe('selectNotification', () => {
    it('should set selectedNotification to the given notification', () => {
      const notif = makeNotif('1', true);
      component.selectNotification(notif);
      expect(component.selectedNotification()).toBe(notif);
    });

    it('should call markAsRead when the notification is unread', () => {
      spyOn(component, 'markAsRead');
      const notif = makeNotif('1', false);
      component.selectNotification(notif);
      expect(component.markAsRead).toHaveBeenCalledWith(notif);
    });

    it('should NOT call markAsRead when the notification is already read', () => {
      spyOn(component, 'markAsRead');
      const notif = makeNotif('1', true);
      component.selectNotification(notif);
      expect(component.markAsRead).not.toHaveBeenCalled();
    });
  });

  // ── markAsRead ────────────────────────────────────────────────────────────────

  describe('markAsRead', () => {
    it('should call notificationService.markAsRead with the notification id', () => {
      const notif = makeNotif('42', false);
      component.markAsRead(notif);
      expect(notificationServiceSpy.markAsRead).toHaveBeenCalledWith('42');
    });

    it('should set notification.read to true', () => {
      const notif = makeNotif('1', false);
      component.markAsRead(notif);
      expect(notif.read).toBeTrue();
    });
  });

  // ── markAllAsRead ─────────────────────────────────────────────────────────────

  describe('markAllAsRead', () => {
    it('should call notificationService.markAllAsRead', () => {
      component.markAllAsRead();
      expect(notificationServiceSpy.markAllAsRead).toHaveBeenCalled();
    });

    it('should reload notifications after all are marked as read', () => {
      notificationServiceSpy.getNotificationsByTypePage.calls.reset();
      component.markAllAsRead();
      expect(notificationServiceSpy.getNotificationsByTypePage).toHaveBeenCalled();
    });

    it('should show a success toast on success', () => {
      component.markAllAsRead();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should show an error toast on failure', () => {
      notificationServiceSpy.markAllAsRead.and.callFake(() => throwError(() => new Error()));
      component.markAllAsRead();
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── deleteNotification ────────────────────────────────────────────────────────

  describe('deleteNotification', () => {
    it('should call notificationService.deleteNotification with the id', () => {
      component.deleteNotification('abc');
      expect(notificationServiceSpy.deleteNotification).toHaveBeenCalledWith('abc');
    });

    it('should clear selectedNotification when the deleted notification was selected', () => {
      const notif = makeNotif('abc', true);
      component.selectedNotification.set(notif);
      component.deleteNotification('abc');
      expect(component.selectedNotification()).toBeNull();
    });

    it('should NOT clear selectedNotification when a different notification is deleted', () => {
      const notif = makeNotif('abc', true);
      component.selectedNotification.set(notif);
      component.deleteNotification('xyz');
      expect(component.selectedNotification()).toBe(notif);
    });

    it('should reload notifications on success', () => {
      notificationServiceSpy.getNotificationsByTypePage.calls.reset();
      component.deleteNotification('abc');
      expect(notificationServiceSpy.getNotificationsByTypePage).toHaveBeenCalled();
    });

    it('should show a success toast on success', () => {
      component.deleteNotification('abc');
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should show an error toast on failure', () => {
      notificationServiceSpy.deleteNotification.and.callFake(() => throwError(() => new Error()));
      component.deleteNotification('abc');
      expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    });
  });

  // ── getUnreadInPageCount ──────────────────────────────────────────────────────

  describe('getUnreadInPageCount', () => {
    it('should return 0 when there are no items', () => {
      expect(component.getUnreadInPageCount()).toBe(0);
    });

    it('should return the number of unread notifications in the page', () => {
      component.notificationsPage = makePage([makeNotif('1', false), makeNotif('2', true), makeNotif('3', false)]);
      expect(component.getUnreadInPageCount()).toBe(2);
    });

    it('should return 0 when all notifications in the page are read', () => {
      component.notificationsPage = makePage([makeNotif('1', true), makeNotif('2', true)]);
      expect(component.getUnreadInPageCount()).toBe(0);
    });
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  describe('DOM', () => {
    it('should show the loading screen when loading=true', () => {
      component.loading = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });

    it('should show the loading screen when error=true', () => {
      component.loading = false;
      component.error = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });

    it('should show the main content when not loading and not error', () => {
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeFalsy();
      expect(fixture.nativeElement.textContent).toContain('Centro de Notificaciones');
    });

    it('should show the empty inbox message when there are no notifications', () => {
      expect(fixture.nativeElement.textContent).toContain('No tienes notificaciones en este momento.');
    });

    it('should render a notification row for each item in the page', () => {
      component.notificationsPage = makePage([makeNotif('1', true), makeNotif('2', true)]);
      fixture.detectChanges();
      // Subject text should appear in the list
      expect(fixture.nativeElement.textContent).toContain('Subject 1');
      expect(fixture.nativeElement.textContent).toContain('Subject 2');
    });

    it('should show "Ningún mensaje seleccionado" when no notification is selected', () => {
      expect(fixture.nativeElement.textContent).toContain('Ningún mensaje seleccionado');
    });

    it('should show the selected notification detail when a notification is selected', () => {
      const notif = makeNotif('1', true);
      component.notificationsPage = makePage([notif]);
      component.selectedNotification.set(notif);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Subject 1');
      expect(fixture.nativeElement.textContent).toContain('Desc 1');
    });

    it('should show the unread count badge when getUnreadInPageCount() > 0', () => {
      component.notificationsPage = makePage([makeNotif('1', false)]);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('sin leer en esta página');
    });

    it('should NOT show the unread count badge when all notifications are read', () => {
      component.notificationsPage = makePage([makeNotif('1', true)]);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('sin leer en esta página');
    });

    it('should render the "Marcar todas como leídas" button', () => {
      expect(fixture.nativeElement.textContent).toContain('Marcar todas como leídas');
    });
  });
});
