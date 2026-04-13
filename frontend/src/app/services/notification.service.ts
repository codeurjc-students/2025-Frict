import { Injectable, signal, computed, effect, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { Notification } from '../models/notification.model';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);

  private socket: WebSocket | null = null;
  private notificationsSignal = signal<Notification[]>([]);

  public unreadNotifications = computed(() =>
    this.notificationsSignal().filter(n => !n.isRead)
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

  public markAsRead(id: string) {
    const previousState = this.notificationsSignal().find(n => n.id === id);
    if (!previousState) return;

    // 1. Optimistic Interface updating
    this.notificationsSignal.update(list =>
      list.map(n => n.id === id ? { ...n, isRead: true } : n)
    );

    // 2. Persist changes in MongoDB
    this.http.put(`/api/v1/notifications/${id}/read`, {}).subscribe({
      error: (err) => {
        console.error('Error marking the notification as read in DB', err);
        //Revert not read state if error
        this.notificationsSignal.update(list =>
          list.map(n => n.id === id ? { ...n, isRead: previousState.isRead } : n)
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
