import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

// PrimeNG
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';

import { LoadingScreenComponent } from '../../common/loading-screen/loading-screen.component';
import { PageResponse } from '../../../models/pageResponse.model';
import { Notification } from '../../../models/notification.model';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [
    CommonModule, FormsModule, LoadingScreenComponent,
    ButtonModule, TagModule, PaginatorModule, TooltipModule
  ],
  templateUrl: './notifications.component.html'
})
export class NotificationsComponent implements OnInit {

  // Paginación y Datos
  notificationsPage: PageResponse<Notification> = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0 };
  first = 0;
  rows = 5;

  // Estados
  loading: boolean = true;
  error: boolean = false;
  listLoading: boolean = false;

  // Selección
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
    this.notificationService.markAllAsReadRest().subscribe({
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
    this.notificationService.deleteNotificationReq(id).subscribe({
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
  getSeverityBySubject(subject: string): { color: string, icon: string, tag: 'info' | 'success' | 'warn' | 'danger' } {
    const s = subject.toLowerCase();

    if (s.includes('error') || s.includes('fallo') || s.includes('crítico') || s.includes('cancelado')) {
      return { color: 'bg-red-50 text-red-600 border-red-200', icon: 'pi pi-exclamation-circle', tag: 'danger' };
    }
    if (s.includes('éxito') || s.includes('completado') || s.includes('asignado')) {
      return { color: 'bg-green-50 text-green-600 border-green-200', icon: 'pi pi-check-circle', tag: 'success' };
    }
    if (s.includes('alerta') || s.includes('aviso') || s.includes('retraso') || s.includes('mantenimiento')) {
      return { color: 'bg-yellow-50 text-yellow-600 border-yellow-200', icon: 'pi pi-exclamation-triangle', tag: 'warn' };
    }

    // Por defecto (Info)
    return { color: 'bg-blue-50 text-blue-600 border-blue-200', icon: 'pi pi-info-circle', tag: 'info' };
  }

  getUnreadInPageCount(): number {
    return this.notificationsPage.items.filter(n => !n.read).length;
  }
}
