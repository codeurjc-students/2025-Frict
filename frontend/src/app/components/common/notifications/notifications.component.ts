import {Component, inject, OnInit, signal} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {filter, skip} from 'rxjs';
import {DatePipe, NgClass, UpperCasePipe} from '@angular/common';
import {LoadingScreenComponent} from '../loading-screen/loading-screen.component';
import {PageResponse} from '../../../models/pageResponse.model';
import {Notification} from '../../../models/notification.model';
import {NotificationService} from '../../../services/notification.service';
import {Button} from 'primeng/button';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {Tag} from 'primeng/tag';
import {MessageService} from 'primeng/api';
import {Tooltip} from 'primeng/tooltip';
import {BreadcrumbReloadComponent} from '../breadcrumb-reload/breadcrumb-reload.component';
import {UiService} from '../../../utils/ui.service';

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
    BreadcrumbReloadComponent,
  ],
  templateUrl: './notifications.component.html'
})
export class NotificationsComponent implements OnInit {

  private messageService = inject(MessageService);
  private notificationService = inject(NotificationService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  protected uiService = inject(UiService);

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

  ngOnInit() {
    const initialNotifId = this.route.snapshot.queryParamMap.get('notifId');

    if (initialNotifId) {
      this.handleDeepLink(initialNotifId);
    } else {
      this.loadNotifications();
    }

    // React to same-route navigations (component already mounted, user clicks a new notification)
    this.route.queryParams.pipe(
      skip(1),                            // skip the initial emission already handled above
      filter(params => !!params['notifId'])
    ).subscribe(params => this.handleDeepLink(params['notifId']));
  }

  private handleDeepLink(notifId: string) {
    // Clean the URL immediately so back-navigation doesn't re-trigger
    this.router.navigate([], { queryParams: {}, replaceUrl: true });

    // Show from router state instantly while the backend resolves
    const stateNotif: Notification | undefined = history.state?.notification;
    if (stateNotif && stateNotif.id === notifId) {
      this.selectNotification(stateNotif);
    }

    this.notificationService.getNotificationLocation(notifId, this.rows).subscribe({
      next: (location) => {
        this.first = location.pageIndex * this.rows;
        this.selectNotification(location.notification);
        this.loadNotifications();
      },
      error: () => this.loadNotifications()
    });
  }

  public reloadAll() {
    this.loading = true;
    this.error = false;

    this.selectedNotification.set(null);
    this.first = 0;

    this.loadNotifications();
  }

  loadNotifications() {

    if (!this.loading) this.listLoading = true;

    this.notificationService.getNotificationsByTypePage(this.first / this.rows, this.rows, '').subscribe({
      next: (page) => {
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

  getUnreadInPageCount(): number {
    return this.notificationsPage.items.filter(n => !n.read).length;
  }
}
