import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';

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

import {TableModule} from 'primeng/table';
import {Button} from 'primeng/button';
import {Textarea} from 'primeng/textarea';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {Tag} from 'primeng/tag';
import {Select} from 'primeng/select';

import {Order} from '../../../models/order.model';
import {PageResponse} from '../../../models/pageResponse.model';
import {Shop} from '../../../models/shop.model';
import {Truck} from '../../../models/truck.model';
import {OrderService} from '../../../services/order.service';
import {ShopService} from '../../../services/shop.service';
import {TruckService} from '../../../services/truck.service';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {formatPrice} from '../../../utils/textFormat.util';

@Component({
  selector: 'app-orders-management',
  standalone: true,
  imports: [
    CommonModule, FormsModule, CdkDropListGroup, CdkDropList, CdkDrag, Avatar, CdkDragPlaceholder, CdkDragPreview,
    Dialog, SelectButton, Paginator, LoadingScreenComponent, PrimeTemplate, TabPanel, TableModule, Button, Textarea, TabList, Tab, TabPanels, Tabs, Tag, Select
  ],
  templateUrl: './orders-management.component.html',
  styleUrl: './orders-management.component.css'
})
export class OrdersManagementComponent implements OnInit {

  ordersPage: PageResponse<Order> = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  first = 0;
  rows = 10;
  loading: boolean = true;
  error: boolean = false;

  displayMode: any[] = [{ label: 'Tablero', value: false }, { label: 'Lista', value: true }];
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

  displayOrderDialog = false;
  selectedOrder: Order | null = null;
  newComment: string = '';

  selectedShop: Shop | null = null;
  selectedTruck: Truck | null = null;

  availableTrucks: Truck[] = [];
  loadingTrucks: boolean = false;
  trucksLoaded: boolean = false;

  constructor(
    private messageService: MessageService,
    private orderService: OrderService,
    private shopService: ShopService,
    private truckService: TruckService
  ) {}

  ngOnInit() {
    this.loadOrdersPage();
  }

  getCurrentStatus(order: Order): string {
    if (!order || !order.history || order.history.length === 0) return 'Pedido Realizado';
    return order.history[order.history.length - 1].status;
  }

  openOrderDetails(order: Order) {
    this.selectedOrder = order;
    this.newComment = '';
    this.selectedShop = null;
    this.selectedTruck = null;
    this.availableTrucks = [];
    this.trucksLoaded = false;

    const shopObs = order.assignedShopId ? this.shopService.getShopById(order.assignedShopId) : of(null);
    const truckObs = order.assignedTruckId ? this.truckService.getTruckById(order.assignedTruckId) : of(null);

    forkJoin({
      shop: shopObs,
      truck: truckObs
    }).subscribe({
      next: (results) => {
        this.selectedShop = results.shop;
        this.selectedTruck = results.truck;

        if (this.selectedTruck) {
          this.availableTrucks = [this.selectedTruck];
        }

        this.displayOrderDialog = true;
      },
      error: () => {
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar los datos logísticos.' });
      }
    });
  }

  loadAvailableTrucks() {
    if (!this.selectedOrder?.assignedShopId) {
      this.messageService.add({ severity: 'warn', summary: 'Atención', detail: 'El pedido no tiene tienda asignada.' });
      return;
    }

    if (this.trucksLoaded) return;

    this.loadingTrucks = true;
    this.truckService.getAllShopTrucks(this.selectedOrder.assignedShopId).subscribe({
      next: (trucks) => {
        this.availableTrucks = trucks;
        this.trucksLoaded = true;

        if (this.selectedTruck) {
          const match = this.availableTrucks.find(t => t.id === this.selectedTruck!.id);
          if (match) {
            this.selectedTruck = match;
          } else {
            this.availableTrucks = [this.selectedTruck, ...this.availableTrucks];
          }
        }

        this.loadingTrucks = false;
      },
      error: () => {
        this.loadingTrucks = false;
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error al obtener los camiones disponibles.' });
      }
    });
  }

  confirmTruckAssignment() {
    if (!this.selectedOrder || !this.selectedTruck) return;
    if (this.selectedTruck.id === this.selectedOrder.assignedTruckId) return;

    this.orderService.setAssignedTruck(this.selectedOrder.id, this.selectedTruck.id, true).subscribe({
      next: (updatedOrder) => {
        this.removeOrderFromAllSignals(updatedOrder.id);
        this.addOrderToCorrectSignal(updatedOrder);
        this.selectedOrder = updatedOrder;
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Camión asignado correctamente al pedido.' });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo asignar el camión.' });
      }
    });
  }

  unassignTruck() {
    if (!this.selectedOrder || !this.selectedOrder.assignedTruckId) return;

    this.orderService.setAssignedTruck(this.selectedOrder.id, this.selectedOrder.assignedTruckId, false).subscribe({
      next: (updatedOrder) => {
        this.removeOrderFromAllSignals(updatedOrder.id);
        this.addOrderToCorrectSignal(updatedOrder);
        this.selectedOrder = updatedOrder;
        this.selectedTruck = null;
        this.messageService.add({ severity: 'success', summary: 'Desasignado', detail: 'Camión retirado correctamente.' });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo desasignar el camión.' });
      }
    });
  }

  addComment() {
    if (!this.newComment.trim() || !this.selectedOrder) return;
    const currentStatus = this.getCurrentStatus(this.selectedOrder);

    this.orderService.commentAndOrUpdateOrderStatus(this.selectedOrder.id, currentStatus, this.newComment.trim()).subscribe({
      next: (updatedOrder) => {
        this.removeOrderFromAllSignals(updatedOrder.id);
        this.addOrderToCorrectSignal(updatedOrder);
        this.selectedOrder = updatedOrder;
        this.newComment = '';
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Actualización registrada.' });
      }
    });
  }

  private removeOrderFromAllSignals(orderId: string) {
    const signals = [this.ordersMade, this.shippedOrders, this.inDeliveryOrders, this.completedOrders, this.cancelledOrders];
    signals.forEach(s => s.update(list => list.filter(o => o.id !== orderId)));
  }

  private addOrderToCorrectSignal(order: Order) {
    const status = this.getCurrentStatus(order);
    switch (status) {
      case 'Pedido Realizado': this.ordersMade.update(l => [...l, order]); break;
      case 'Enviado':          this.shippedOrders.update(l => [...l, order]); break;
      case 'En Reparto':       this.inDeliveryOrders.update(l => [...l, order]); break;
      case 'Completado':       this.completedOrders.update(l => [...l, order]); break;
      case 'Cancelado':        this.cancelledOrders.update(l => [...l, order]); break;
    }
  }

  drop(event: CdkDragDrop<Order[]>, newStatus: string) {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }
    const movedOrder = event.previousContainer.data[event.previousIndex];
    transferArrayItem(event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);

    this.orderService.commentAndOrUpdateOrderStatus(movedOrder.id, newStatus, `Cambio de estado a "${newStatus}" desde el tablero.`).subscribe({
      next: (updatedOrder) => {
        this.removeOrderFromAllSignals(updatedOrder.id);
        this.addOrderToCorrectSignal(updatedOrder);
        if (this.selectedOrder?.id === updatedOrder.id) this.selectedOrder = updatedOrder;
        this.messageService.add({ severity: 'success', summary: 'Estado actualizado', detail: `Pedido movido a ${newStatus}` });
      },
      error: () => {
        transferArrayItem(event.container.data, event.previousContainer.data, event.currentIndex, event.previousIndex);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo actualizar el estado.' });
      }
    });
  }

  getIconForStatus(status: string): string {
    const icons: Record<string, string> = { 'Pedido Realizado': 'pi pi-shopping-cart', 'Enviado': 'pi pi-box', 'En Reparto': 'pi pi-truck', 'Completado': 'pi pi-check', 'Cancelado': 'pi pi-times' };
    return icons[status] || 'pi pi-info-circle';
  }

  getStatusColor(status: string): string {
    const colors: Record<string, string> = { 'Pedido Realizado': 'text-blue-500', 'Enviado': 'text-purple-500', 'En Reparto': 'text-orange-500', 'Completado': 'text-green-500', 'Cancelado': 'text-red-500' };
    return colors[status] || 'text-slate-400';
  }

  getStatusBgColor(status: string): string {
    const colors: Record<string, string> = { 'Pedido Realizado': 'bg-blue-100 text-blue-700', 'Enviado': 'bg-purple-100 text-purple-700', 'En Reparto': 'bg-orange-100 text-orange-700', 'Completado': 'bg-green-100 text-green-700', 'Cancelado': 'bg-red-100 text-red-700' };
    return colors[status] || 'bg-slate-100 text-slate-700';
  }

  getUserLabel(userName: string | undefined): string {
    const idStr = String(userName || '');
    return (idStr.charAt(0) || 'U').toUpperCase();
  }

  getItemName(item: any): string {
    return item.productName || item.product?.name || 'Producto ID: ' + (item.productId || 'N/A');
  }

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
      error: () => { this.loading = false; this.error = true; }
    });
  }

  onOrdersPageChange(event: PaginatorState) {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    this.loadOrdersPage();
  }

  protected readonly formatPrice = formatPrice;
}
