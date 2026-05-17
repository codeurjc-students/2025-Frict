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

  public unreadNotifications = computed(() =>
    this.notificationsSignal().filter(n => !n.read)
  );

  public unreadCount = computed(() => this.unreadNotifications().length);
  public isSocketOpen = computed(() => this.socketOpenSignal());
  // Fires synchronously right before the socket is closed, while it is still OPEN.
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

    // Capture tab/browser close so subscribers can flush a final message via WS
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
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/api/v1/ws/notifications`;

    this.socket = new WebSocket(wsUrl);
    this.socket.onopen = () => this.socketOpenSignal.set(true);
    this.socket.onclose = () => this.socketOpenSignal.set(false);
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

  public getRecentNotifications(type: string, count: number = 5): Observable<Notification[]> {
    return this.getNotificationsByTypePage(0, count, type).pipe(
      map(response => response.items)
    );
  }

  public getNotificationsByTypePage(page: number, size: number, type: string): Observable<PageResponse<Notification>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('type', type.toUpperCase()); // Convertimos a mayúsculas para el Enum de Java

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
    if (this.socket) {
      // Notify subscribers BEFORE closing so they can push a last frame while the socket is OPEN
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
