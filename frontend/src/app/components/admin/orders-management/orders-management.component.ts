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
import {formatAddress, formatPrice} from '../../../utils/textFormat.util';

import * as L from 'leaflet';

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

  displayStatusDialog: boolean = false;
  pendingStatusComment: string = '';
  pendingDropData: {
    order: Order;
    newStatus: string;
    previousContainer: CdkDropList<Order[]>;
    container: CdkDropList<Order[]>;
    previousIndex: number;
    currentIndex: number;
  } | null = null;

  // Map and dialog tabs management
  activeTab: string = '0';
  private orderMap: L.Map | undefined;
  private markersGroup: L.FeatureGroup | undefined;

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
    this.activeTab = '0';

    const shopObs = order.assignedShopId ? this.shopService.getShopById(order.assignedShopId) : of(null);
    const truckObs = order.assignedTruckId ? this.truckService.getTruckById(order.assignedTruckId) : of(null);

    forkJoin({
      shop: shopObs,
      truck: truckObs
    }).subscribe({
      next: (results) => {
        this.selectedShop = results.shop;
        this.selectedTruck = results.truck;
        console.log(this.selectedTruck?.address);

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

  onDialogHide() {
    if (this.orderMap) {
      this.orderMap.remove();
      this.orderMap = undefined;
    }
  }

  // Intercept the tabs change to manage map redimensioning
  onTabChange(tabValue: string | number) {
    if (String(tabValue) === '2') {
      setTimeout(() => {
        if (!this.orderMap) {
          this.initMap();
        } else {
          this.orderMap.invalidateSize();
          if (this.markersGroup && this.markersGroup.getLayers().length > 0) {
            this.orderMap.fitBounds(this.markersGroup.getBounds(), { padding: [40, 40], maxZoom: 16 });
          }
        }
      }, 100);
    }
  }

  private initMap() {
    const container = document.getElementById('order-map');
    if (!container) return;

    // Destroy the previous map if there were already an initialized map
    if (this.orderMap) {
      this.orderMap.remove();
    }

    this.orderMap = L.map('order-map').setView([40.4168, -3.7038], 6);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap'
    }).addTo(this.orderMap);

    const orderIcon = L.icon({
      iconUrl: './location-pointer.png',
      iconSize: [32, 32],
      iconAnchor: [16, 32],
      popupAnchor: [0, -32]
    });

    const shopIcon = L.icon({
      iconUrl: './shopIcon.png',
      iconSize: [35, 35],
      iconAnchor: [17, 35],
      popupAnchor: [0, -35]
    });

    const truckIcon = L.icon({
      iconUrl: './truckIcon.png',
      iconSize: [40, 40],
      iconAnchor: [20, 20],
      popupAnchor: [0, -20]
    });

    this.markersGroup = L.featureGroup().addTo(this.orderMap);

    // Destinations pointer
    if (this.selectedOrder?.sendingAddress?.latitude && this.selectedOrder?.sendingAddress?.longitude) {
      L.marker([this.selectedOrder.sendingAddress.latitude, this.selectedOrder.sendingAddress.longitude], { icon: orderIcon })
        .bindPopup('<b>Destino</b><br>' + this.selectedOrder.userName)
        .addTo(this.markersGroup);
    }

    // Shops pointer
    if (this.selectedShop?.address?.latitude && this.selectedShop?.address?.longitude) {
      L.marker([this.selectedShop.address.latitude, this.selectedShop.address.longitude], { icon: shopIcon })
        .bindPopup('<b>Tienda Origen</b><br>' + this.selectedShop.name)
        .addTo(this.markersGroup);
    }

    // Trucks pointer
    if (this.selectedTruck?.address?.latitude && this.selectedTruck?.address?.longitude) {
      L.marker([this.selectedTruck.address.latitude, this.selectedTruck.address.longitude], { icon: truckIcon })
        .bindPopup('<b>Camión</b><br>' + this.selectedTruck?.referenceCode)
        .addTo(this.markersGroup);
    }

    // Autocenter the map containing all added pointers
    if (this.markersGroup.getLayers().length > 0) {
      this.orderMap.fitBounds(this.markersGroup.getBounds(), { padding: [40, 40], maxZoom: 15 });
    }
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
        this.syncOrderInListData(updatedOrder);
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

    transferArrayItem(event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);

    // Save the data locally to use them when cancelling or accepting the change status operation
    this.pendingDropData = {
      order: event.container.data[event.currentIndex],
      newStatus: newStatus,
      previousContainer: event.previousContainer,
      container: event.container,
      previousIndex: event.previousIndex,
      currentIndex: event.currentIndex
    };

    this.pendingStatusComment = '';
    this.displayStatusDialog = true;
  }

  // Confirm change status
  confirmStatusChange() {
    if (!this.pendingDropData) return;

    const { order, newStatus, previousContainer, container, previousIndex, currentIndex } = this.pendingDropData;
    this.displayStatusDialog = false;

    // Send the comment to backend (empty or not)
    this.orderService.commentAndOrUpdateOrderStatus(order.id, newStatus, this.pendingStatusComment).subscribe({
      next: (updatedOrder) => {
        this.syncOrderInListData(updatedOrder);
        this.messageService.add({ severity: 'success', summary: 'Estado actualizado', detail: `Pedido movido a ${newStatus}` });
        this.pendingDropData = null;
      },
      error: () => {
        // Revert if backend fails
        transferArrayItem(container.data, previousContainer.data, currentIndex, previousIndex);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo actualizar el estado.' });
        this.pendingDropData = null;
      }
    });
  }

  //Cancel change status
  cancelStatusChange() {
    if (!this.pendingDropData) return;

    const { previousContainer, container, previousIndex, currentIndex } = this.pendingDropData;

    // Revert visual movement
    transferArrayItem(container.data, previousContainer.data, currentIndex, previousIndex);

    this.displayStatusDialog = false;
    this.pendingDropData = null;
  }

  private syncOrderInListData(updatedOrder: Order) {
    // Update order in list mode
    if (this.ordersPage && this.ordersPage.items) {
      const index = this.ordersPage.items.findIndex(o => o.id === updatedOrder.id);
      if (index !== -1) {
        this.ordersPage.items[index] = updatedOrder;
      }
    }

    // Update order in Kanban
    this.removeOrderFromAllSignals(updatedOrder.id);
    this.addOrderToCorrectSignal(updatedOrder);

    // Update selected order if dialog is open
    if (this.selectedOrder?.id === updatedOrder.id) {
      this.selectedOrder = updatedOrder;
    }
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
  protected readonly formatAddress = formatAddress;
}
