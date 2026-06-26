import {computed, effect, inject, Injectable, OnDestroy, signal} from '@angular/core';
import {Notification, NotificationLocation} from '../models/notification.model';
import {HttpClient, HttpParams} from '@angular/common/http';
import {map, Observable, Subject, throwError} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {PageResponse} from '../models/pageResponse.model';
import {AuthService} from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {

  private http = inject(HttpClient);
  private authService = inject(AuthService);

  private socket: WebSocket | null = null;
  private notificationsSignal = signal<Notification[]>([]);
  private socketOpenSignal = signal(false);
  private beforeDisconnectSubject = new Subject<void>();

  private reconnectAttempt = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private intentionalClose = false;

  private static readonly BASE_DELAY_MS = 1000;
  private static readonly MAX_DELAY_MS = 30000;

  public unreadNotifications = computed(() =>
    this.notificationsSignal().filter(n => !n.read)
  );

  public unreadCount = computed(() => this.unreadNotifications().length);
  public isSocketOpen = computed(() => this.socketOpenSignal());
  public beforeDisconnect$ = this.beforeDisconnectSubject.asObservable();

  constructor() {
    effect(() => {
      const isLogged = this.authService.isLogged();

      if (isLogged && !this.socket) {
        this.connect();
      } else if (!isLogged && this.socket) {
        this.disconnect();
      }
    });

    if (typeof window !== 'undefined') {
      window.addEventListener('beforeunload', () => this.disconnect());
    }
  }

  ngOnDestroy() {
    this.disconnect();
  }

  private loadInitialHistory() {
    this.http.get<Notification[]>('/api/v1/notifications/unread').subscribe({
      next: (history) => {
        this.notificationsSignal.set(history);
      },
      error: (err) => console.error('Error loading notifications history', err)
    });
  }

  private connect() {
    this.intentionalClose = false;
    this.clearReconnectTimer();

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/api/v1/ws/notifications`;

    this.socket = new WebSocket(wsUrl);

    this.socket.onopen = () => {
      this.reconnectAttempt = 0;
      this.socketOpenSignal.set(true);
    };

    this.socket.onclose = () => {
      this.socketOpenSignal.set(false);
      this.socket = null;
      if (!this.intentionalClose && this.authService.isLogged()) {
        this.scheduleReconnect();
      }
    };

    this.loadInitialHistory();

    this.socket.onmessage = (event) => {
      const data = JSON.parse(event.data);

      if (data.topic === 'NOTIFICATIONS' && data.action === 'NEW') {
        const newNotification: Notification = data.payload;
        this.notificationsSignal.update(list => [newNotification, ...list]);
      }
    };

    this.socket.onerror = (error) => {
      console.error('Error in notifications websocket:', error);
    };
  }

  private scheduleReconnect() {
    const jitter = Math.random() * 0.5 + 0.75;
    const delay = Math.min(
      NotificationService.BASE_DELAY_MS * Math.pow(2, this.reconnectAttempt) * jitter,
      NotificationService.MAX_DELAY_MS
    );
    this.reconnectAttempt++;
    this.reconnectTimer = setTimeout(() => this.connect(), delay);
  }

  private clearReconnectTimer() {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  public getRecentNotifications(type: string, count: number = 5): Observable<Notification[]> {
    return this.getNotificationsByTypePage(0, count, type).pipe(
      map(response => response.items)
    );
  }

  public getNotificationsByTypePage(page: number, size: number, type: string): Observable<PageResponse<Notification>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('type', type.toUpperCase()); // Convert to uppercase to match the Java enum naming convention

    return this.http.get<PageResponse<Notification>>('/api/v1/notifications/', { params });
  }

  public markAllAsRead(): Observable<boolean> {
    const previousState = this.notificationsSignal();

    this.notificationsSignal.update(list =>
      list.map(n => ({ ...n, read: true }))
    );

    return this.http.put<boolean>('/api/v1/notifications/read-all', {}).pipe(
      catchError((err) => {
        this.notificationsSignal.set(previousState);
        return throwError(() => err);
      })
    );
  }

  public deleteNotification(id: string): Observable<boolean> {
    return this.http.delete<boolean>(`/api/v1/notifications/${id}`);
  }

  public getNotificationLocation(id: string, size: number): Observable<NotificationLocation> {
    const params = new HttpParams().set('size', size.toString());
    return this.http.get<NotificationLocation>(`/api/v1/notifications/${id}`, { params });
  }

  public markAsRead(id: string) {
    const previousState = this.notificationsSignal().find(n => n.id === id);
    if (!previousState) return;

    // 1. Optimistic Interface updating
    this.notificationsSignal.update(list =>
      list.map(n => n.id === id ? { ...n, read: true } : n)
    );

    // 2. Persist changes in MongoDB
    this.http.put(`/api/v1/notifications/${id}/read`, {}).subscribe({
      error: (err) => {
        console.error('Error marking the notification as read in DB', err);
        //Revert not read state if error
        this.notificationsSignal.update(list =>
          list.map(n => n.id === id ? { ...n, read: previousState.read } : n)
        );
      }
    });
  }

  private disconnect() {
    this.intentionalClose = true;
    this.clearReconnectTimer();
    if (this.socket) {
      if (this.socket.readyState === WebSocket.OPEN) {
        this.beforeDisconnectSubject.next();
      }
      this.socket.close();
      this.socket = null;
    }
    this.socketOpenSignal.set(false);
    this.notificationsSignal.set([]);
  }

  public triggerTest(): Observable<any> {
    return this.http.post('/api/v1/notifications/test', {});
  }

  public send(payload: object): boolean {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(payload));
      return true;
    }
    return false;
  }
}
