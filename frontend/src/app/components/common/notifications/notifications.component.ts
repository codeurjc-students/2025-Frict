import { Component, OnInit, signal } from '@angular/core';
import {DatePipe, NgClass, NgForOf, NgIf, UpperCasePipe} from '@angular/common';
import { LoadingScreenComponent } from '../loading-screen/loading-screen.component';
import { PageResponse } from '../../../models/pageResponse.model';
import { Notification } from '../../../models/notification.model';
import { NotificationService } from '../../../services/notification.service';
import {Button} from 'primeng/button';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {Tag} from 'primeng/tag';
import {MessageService} from 'primeng/api';
import {Tooltip} from 'primeng/tooltip';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [
    LoadingScreenComponent,
    NgClass,
    Button,
    Paginator,
    Tag,
    Tooltip,
    DatePipe,
    UpperCasePipe,
    NgIf,
    NgForOf,
  ],
  templateUrl: './notifications.component.html'
})
export class NotificationsComponent implements OnInit {

  // Pagination and Data
  notificationsPage: PageResponse<Notification> = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0 };
  first = 0;
  rows = 5;

  // Status
  loading: boolean = true;
  error: boolean = false;
  listLoading: boolean = false;

  // Selection
  selectedNotification = signal<Notification | null>(null);

  constructor(
    private messageService: MessageService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.loadNotifications();
  }

  loadNotifications() {
    if (!this.loading) this.listLoading = true;

    this.notificationService.getNotificationsPage(this.first / this.rows, this.rows).subscribe({
      next: (page) => {
        console.log(page)
        this.notificationsPage = page;
        this.loading = false;
        this.listLoading = false;

        if (!this.selectedNotification() && page.items.length > 0) {
          this.selectNotification(page.items[0]);
        }
      },
      error: () => {
        this.loading = false;
        this.listLoading = false;
        this.error = true;
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar las notificaciones.' });
      }
    });
  }

  onPageChange(event: PaginatorState) {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 5;
    this.loadNotifications();
  }

  selectNotification(notification: Notification) {
    this.selectedNotification.set(notification);

    // If unread, mark as read automatically after opening it
    if (!notification.read) {
      this.markAsRead(notification);
    }
  }

  markAsRead(notification: Notification) {
    this.notificationService.markAsRead(notification.id);
    notification.read = true; // Update the visual state in the list
  }

  markAllAsRead() {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {

        this.loadNotifications();
        this.messageService.add({ severity: 'success', summary: 'Actualizado', detail: 'Todas las notificaciones marcadas como leídas.' });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron marcar las notificaciones.' });
      }
    });
  }

  deleteNotification(id: string) {
    this.notificationService.deleteNotification(id).subscribe({
      next: () => {
        if (this.selectedNotification()?.id === id) {
          this.selectedNotification.set(null);
        }
        this.loadNotifications();
        this.messageService.add({ severity: 'success', summary: 'Eliminada', detail: 'Notificación eliminada correctamente.' });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo eliminar la notificación.' });
      }
    });
  }

  // --- VISUAL HELPERS ---
  getVisualsByType(type: string): { color: string, icon: string, tag: 'info' | 'success' | 'warn' | 'danger' | 'secondary' } {
    switch (type?.toLowerCase()) {
      case 'usuario':
        return { color: 'bg-cyan-50 text-cyan-600 border-cyan-200', icon: 'pi pi-user', tag: 'info' };
      case 'camión':
        return { color: 'bg-slate-50 text-slate-600 border-slate-200', icon: 'pi pi-truck', tag: 'secondary' };
      case 'tienda':
        return { color: 'bg-purple-50 text-purple-600 border-purple-200', icon: 'pi pi-building', tag: 'info' };
      case 'pedido':
        return { color: 'bg-emerald-50 text-emerald-600 border-emerald-200', icon: 'pi pi-shopping-cart', tag: 'success' };
      case 'producto':
        return { color: 'bg-indigo-50 text-indigo-600 border-indigo-200', icon: 'pi pi-box', tag: 'info' };
      case 'reseña':
        return { color: 'bg-amber-50 text-amber-600 border-amber-200', icon: 'pi pi-star', tag: 'warn' };
      default:
        return { color: 'bg-blue-50 text-blue-600 border-blue-200', icon: 'pi pi-bell', tag: 'info' };
    }
  }

  getUnreadInPageCount(): number {
    return this.notificationsPage.items.filter(n => !n.read).length;
  }
}
