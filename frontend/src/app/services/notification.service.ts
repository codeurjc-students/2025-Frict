import { Injectable, signal, computed, effect, inject, OnDestroy } from '@angular/core';
import { AuthService } from './auth.service';
import { Notification } from '../models/notification.model';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PageResponse } from '../models/pageResponse.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {
  private http = inject(HttpClient);
  private authService = inject(AuthService);

  private socket: WebSocket | null = null;
  private notificationsSignal = signal<Notification[]>([]);

  public unreadNotifications = computed(() =>
    this.notificationsSignal().filter(n => !n.read)
  );

  public unreadCount = computed(() => this.unreadNotifications().length);

  constructor() {
    effect(() => {
      const isLogged = this.authService.isLogged();

      if (isLogged && !this.socket) {
        this.connect();
      } else if (!isLogged && this.socket) {
        this.disconnect();
      }
    });
  }

  ngOnDestroy() {
    this.disconnect();
  }

  private loadInitialHistory() {
    this.http.get<Notification[]>('/api/v1/notifications/unread').subscribe({
      next: (history) => {
        console.log("Hola");
        this.notificationsSignal.set(history);
      },
      error: (err) => console.error('Error loading notifications history', err)
    });
  }

  private connect() {
    const wsUrl = 'wss://localhost/api/v1/ws/notifications';

    this.socket = new WebSocket(wsUrl);
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

  public getNotificationsPage(page: number, size: number): Observable<PageResponse<Notification>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Notification>>('/api/v1/notifications/', { params });
  }

  public markAllAsReadRest(): Observable<boolean> {
    return this.http.put<boolean>('/api/v1/notifications/read-all', {});
  }

  public deleteNotificationReq(id: string): Observable<boolean> {
    return this.http.delete<boolean>(`/api/v1/notifications/${id}`);
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
      this.socket.close();
      this.socket = null;
    }
    this.notificationsSignal.set([]);
  }
}
