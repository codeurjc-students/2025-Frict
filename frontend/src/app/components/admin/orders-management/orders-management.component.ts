import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  CdkDrag, CdkDragDrop,
  CdkDragPlaceholder, CdkDragPreview,
  CdkDropList,
  CdkDropListGroup,
  moveItemInArray,
  transferArrayItem
} from '@angular/cdk/drag-drop';

import {Avatar} from 'primeng/avatar';
import {MessageService, PrimeTemplate} from 'primeng/api';
import {Dialog} from 'primeng/dialog';
import {SelectButton} from 'primeng/selectbutton';
import {Paginator, PaginatorState} from 'primeng/paginator';

// Nuevos módulos de PrimeNG para la tabla, pestañas y formulario
import {TableModule} from 'primeng/table';
import {Button} from 'primeng/button';

import {Order} from '../../../models/order.model';
import {PageResponse} from '../../../models/pageResponse.model';
import {StatusLog} from '../../../models/statusLog.model';
import {OrderService} from '../../../services/order.service';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {Textarea} from 'primeng/textarea';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {formatPrice} from '../../../utils/textFormat.util';
import {Tag} from 'primeng/tag';

@Component({
  selector: 'app-orders-management',
  standalone: true,
  imports: [
    CommonModule, FormsModule, CdkDropListGroup, CdkDropList, CdkDrag, Avatar, CdkDragPlaceholder, CdkDragPreview,
    Dialog, SelectButton, Paginator, LoadingScreenComponent, PrimeTemplate, TabPanel, TableModule, Button, Textarea, TabList, Tab, TabPanels, Tabs, Tag
  ],
  templateUrl: './orders-management.component.html',
  styleUrl: './orders-management.component.css'
})
export class OrdersManagementComponent implements OnInit {

  ordersPage: PageResponse<Order> = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};

  // Pagination
  first = 0;
  rows = 10;

  loading: boolean = true;
  error: boolean = false;

  displayMode: any[] = [
    { label: 'Tablero', value: false },
    { label: 'Lista', value: true }
  ];
  listModeSelected: boolean = false;

  ordersMade = signal<Order[]>([]);
  shippedOrders = signal<Order[]>([]);
  inDeliveryOrders = signal<Order[]>([]);
  completedOrders = signal<Order[]>([]);
  cancelledOrders = signal<Order[]>([]);

  kanbanColumns = [
    { id: 'Pedido Realizado', title: 'Pedido Realizado', data: this.ordersMade },
    { id: 'Enviado', title: 'Enviado', data: this.shippedOrders },
    { id: 'En Reparto', title: 'En Reparto', data: this.inDeliveryOrders },
    { id: 'Completado', title: 'Completado', data: this.completedOrders }
  ];

  // Estado del Diálogo
  displayOrderDialog = false;
  selectedOrder: Order | null = null;
  newComment: string = '';

  constructor(private messageService: MessageService,
              private orderService: OrderService) {}

  ngOnInit() {
    this.loadOrdersPage();
  }

  // --- OBTENCIÓN DE ESTADO ACTUAL ---
  getCurrentStatus(order: Order): string {
    return order.history[order.history.length - 1].status;
  }

  openOrderDetails(order: Order) {
    this.selectedOrder = order;
    this.newComment = '';
    this.displayOrderDialog = true;
  }

  // --- MOCK DATA PARA CONDUCTOR Y TIENDA ---
  getMockDriver(truckId: string) {
    if (!truckId) return null;
    return { name: 'Carlos Rodríguez', username: '@carlos.driver', photo: 'https://i.pravatar.cc/150?u=' + truckId };
  }

  getMockStore() {
    return { name: 'Tienda Central Madrid', address: 'Av. Gran Vía 45, 28013, Madrid', image: 'https://images.unsplash.com/photo-1534452203293-494d7ddbf7e0?q=80&w=200&auto=format&fit=crop' };
  }

  // Comment
  addComment() {
    if (!this.newComment.trim() || !this.selectedOrder) return;

    // 1. Obtenemos el estado actual para la petición
    const currentHistoryIndex = this.selectedOrder.history.length - 1;
    const currentStatus = this.selectedOrder.history[currentHistoryIndex].status;

    this.orderService.commentAndOrUpdateOrderStatus(
      this.selectedOrder.id,
      currentStatus,
      this.newComment.trim()
    ).subscribe({
      next: (updatedOrder) => {
        // A) LIMPIEZA: Eliminamos el pedido de TODAS las señales de estado.
        // Esto garantiza que si el estado cambió, desaparezca de la "columna" antigua.
        this.removeOrderFromAllSignals(updatedOrder.id);

        // B) INSERCIÓN: Lo añadimos a la señal que le corresponde ahora.
        this.addOrderToCorrectSignal(updatedOrder);

        // C) ACTUALIZAR SELECCIONADO: Para que el modal/detalle se refresque
        this.selectedOrder = updatedOrder;

        // D) FEEDBACK Y LIMPIEZA
        this.newComment = '';
        this.messageService.add({
          severity: 'success',
          summary: 'Éxito',
          detail: 'Actualización registrada.'
        });
      }
    });
  }

  private removeOrderFromAllSignals(orderId: string) {
    const signals = [
      this.ordersMade, this.shippedOrders, this.inDeliveryOrders,
      this.completedOrders, this.cancelledOrders
    ];
    signals.forEach(s => s.update(list => list.filter(o => o.id !== orderId)));
  }

  private addOrderToCorrectSignal(order: Order) {
    switch (order.history[order.history.length - 1].status) {
      case 'Pedido Realizado': this.ordersMade.update(l => [...l, order]); break;
      case 'Enviado':          this.shippedOrders.update(l => [...l, order]); break;
      case 'En Reparto':       this.inDeliveryOrders.update(l => [...l, order]); break;
      case 'Completado':       this.completedOrders.update(l => [...l, order]); break;
      case 'Cancelado':        this.cancelledOrders.update(l => [...l, order]); break;
    }
  }

  drop(event: CdkDragDrop<Order[]>, newStatus: string) {
    // 1. Si se suelta en la misma columna, solo reordenamos visualmente
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }

    // 2. MOVIMIENTO OPTIMISTA: Lo movemos en el UI antes de que responda el servidor
    const movedOrder = event.previousContainer.data[event.previousIndex];
    transferArrayItem(
      event.previousContainer.data,
      event.container.data,
      event.previousIndex,
      event.currentIndex
    );

    // 3. LLAMADA AL SERVICIO
    this.orderService.commentAndOrUpdateOrderStatus(
      movedOrder.id,
      newStatus,
      `Cambio de estado a "${newStatus}" desde el tablero.`
    ).subscribe({
      next: (updatedOrder) => {
        // ÉXITO: Sincronizamos con el objeto real de la BDD (que ya trae el historial nuevo)
        this.removeOrderFromAllSignals(updatedOrder.id);
        this.addOrderToCorrectSignal(updatedOrder);

        // Actualizamos el seleccionado si es el mismo
        if (this.selectedOrder?.id === updatedOrder.id) {
          this.selectedOrder = updatedOrder;
        }

        this.messageService.add({
          severity: 'success',
          summary: 'Estado actualizado',
          detail: `Pedido movido a ${newStatus}`
        });
      },
      error: () => {
        // ERROR: Revertimos el movimiento visual
        transferArrayItem(
          event.container.data,
          event.previousContainer.data,
          event.currentIndex,
          event.previousIndex
        );

        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo actualizar el estado en el servidor.'
        });
      }
    });
  }

  processOrderDrop(order: Order, newStatus: string) {
    const currentStatus = this.getCurrentStatus(order);
    if (currentStatus === newStatus) return;
    this.messageService.add({ severity: 'success', summary: 'Estado actualizado', detail: `Pedido ${order.referenceCode}` });
  }

  // --- HELPERS VISUALES ---

  getIconForStatus(status: string): string {
    const icons: Record<string, string> = {
      'Pedido Realizado': 'pi pi-shopping-cart', 'Enviado': 'pi pi-box',
      'En Reparto': 'pi pi-truck', 'Completado': 'pi pi-check', 'Cancelado': 'pi pi-times'
    };
    return icons[status] || 'pi pi-info-circle';
  }

  getStatusColor(status: string): string {
    const colors: Record<string, string> = {
      'Pedido Realizado': 'text-blue-500', 'Enviado': 'text-purple-500',
      'En Reparto': 'text-orange-500', 'Completado': 'text-green-500', 'Cancelado': 'text-red-500'
    };
    return colors[status] || 'text-slate-400';
  }

  getStatusBgColor(status: string): string {
    const colors: Record<string, string> = {
      'Pedido Realizado': 'bg-blue-100 text-blue-700', 'Enviado': 'bg-purple-100 text-purple-700',
      'En Reparto': 'bg-orange-100 text-orange-700', 'Completado': 'bg-green-100 text-green-700', 'Cancelado': 'bg-red-100 text-red-700'
    };
    return colors[status] || 'bg-slate-100 text-slate-700';
  }

  getUserLabel(userId: any): string {
    const idStr = String(userId || '');
    return (idStr.charAt(0) || 'U').toUpperCase();
  }

  // --- CARGA DE DATOS ---
  private loadOrdersPage() {
    this.orderService.getAllOrdersPage(this.first/this.rows, this.rows).subscribe({
      next: (page: PageResponse<Order>) => {
        this.ordersPage = page;
        this.ordersMade.set(page.items.filter(o => this.getCurrentStatus(o) === 'Pedido Realizado'));
        this.shippedOrders.set(page.items.filter(o => this.getCurrentStatus(o) === 'Enviado'));
        this.inDeliveryOrders.set(page.items.filter(o => this.getCurrentStatus(o) === 'En Reparto'));
        this.completedOrders.set(page.items.filter(o => this.getCurrentStatus(o) === 'Completado'));
        this.cancelledOrders.set(page.items.filter(o => this.getCurrentStatus(o) === 'Cancelado'));
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }

  onOrdersPageChange(event: PaginatorState) {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    this.loadOrdersPage();
  }

  protected readonly formatPrice = formatPrice;
}
