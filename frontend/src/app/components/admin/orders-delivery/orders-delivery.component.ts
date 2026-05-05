import { Component, OnInit, OnDestroy, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

// PrimeNG
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { Textarea } from 'primeng/textarea';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';

import * as L from 'leaflet';
import { Html5QrcodeScanner } from 'html5-qrcode';

import { LoadingScreenComponent } from '../../common/loading-screen/loading-screen.component';
import { BreadcrumbReloadComponent } from '../../common/breadcrumb-reload/breadcrumb-reload.component';
import { OrderService } from '../../../services/order.service';
import { TruckService } from '../../../services/truck.service';
import { ShopService } from '../../../services/shop.service';
import { AuthService } from '../../../services/auth.service';
import { formatAddress } from '../../../utils/textFormat.util';
import { PageResponse } from '../../../models/pageResponse.model';
import { Order } from '../../../models/order.model';
import { Truck } from '../../../models/truck.model';
import { Shop } from '../../../models/shop.model';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { Tooltip } from 'primeng/tooltip';

@Component({
  selector: 'app-orders-delivery',
  standalone: true,
  imports: [
    CommonModule, FormsModule, LoadingScreenComponent, BreadcrumbReloadComponent,
    PaginatorModule, Textarea,
    Tabs, TabList, Tab, TabPanels, TabPanel, Button, Select, Tag, TableModule, Dialog, Tooltip
  ],
  templateUrl: './orders-delivery.component.html'
})
export class OrdersDeliveryComponent implements OnInit, OnDestroy {

  private authService = inject(AuthService);
  private orderService = inject(OrderService);
  private truckService = inject(TruckService);
  private shopService = inject(ShopService);
  private messageService = inject(MessageService);

  loading = true;
  error = false;
  hasTruck = true;

  ordersPage = signal<PageResponse<Order>>({ items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0 });
  first = 0;
  rows = 5;

  selectedOrderId = signal<string | null>(null);
  selectedOrder = computed(() => this.ordersPage().items.find(o => o.id === this.selectedOrderId()) || null);

  myTruck = signal<Truck | null>(null);
  myShop = signal<Shop | null>(null);

  displayQrScanner = false;
  scanning = false;
  newComment: string = '';
  activeTab: string = '0';

  // --- VARIABLES PARA LOS MODALES DE RECOGIDA Y CANCELACIÓN ---
  displayCollectDialog = false;
  collectComment = '';

  displayCancelDialog = false;
  cancelComment = '';

  private map: L.Map | undefined;
  protected markers: L.Marker[] = [];

  // Instancia del escáner QR
  private html5QrcodeScanner: Html5QrcodeScanner | null = null;

  ngOnInit() {
    this.loadDeliveryData();
  }

  ngOnDestroy() {
    if (this.map) this.map.remove();
    if (this.html5QrcodeScanner) {
      this.html5QrcodeScanner.clear().catch(e => console.error("Error al limpiar escáner:", e));
    }
  }

  loadDeliveryData() {
    this.loading = true;
    this.error = false;

    this.activeTab = '0';

    if (this.map) {
      this.map.remove();
      this.map = undefined;
      this.markers = [];
    }

    this.fetchDriverInfo();
  }

  private fetchDriverInfo() {
    this.authService.getLoginInfo().subscribe({
      next: (driver) => {
        if (driver && driver.id) {
          this.fetchTruck(driver.id);
        } else {
          this.error = true;
          this.loading = false;
        }
      },
      error: () => {
        this.error = true;
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo obtener la información de tu usuario.' });
      }
    });
  }

  private fetchTruck(driverId: string) {
    this.truckService.getAssignedTruckByDriverId(driverId).subscribe({
      next: (truck) => {
        this.myTruck.set(truck);
        if (truck != null){
          this.hasTruck = true;
          this.fetchShop(truck.id);
        } else {
          this.hasTruck = false;
          this.loading = false;
        }
      },
      error: () => {
        this.hasTruck = false;
        this.loading = false;
      }
    });
  }

  private fetchShop(truckId: string) {
    this.shopService.getShopByAssignedTruckId(truckId).subscribe({
      next: (shop) => {
        this.myShop.set(shop);
        this.fetchOrders();
      },
      error: () => {
        this.error = true;
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo cargar la información de la tienda base.' });
      }
    });
  }

  private fetchOrders() {
    const pageIndex = this.first / this.rows;
    this.orderService.getOrdersByRolePage(pageIndex, this.rows, 'createdAt,desc').subscribe({
      next: (page) => {
        this.ordersPage.set(page);
        if (page.items.length > 0) {
          if (!this.selectedOrderId() || !page.items.find(o => o.id === this.selectedOrderId())) {
            this.selectedOrderId.set(page.items[0].id);
          }
        } else {
          this.selectedOrderId.set(null);
        }
        this.loading = false;
        if (this.activeTab === '2') {
          this.updateMapPositions();
        }
      },
      error: () => {
        this.error = true;
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar tus pedidos.' });
      }
    });
  }

  onPageChange(event: PaginatorState) {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 5;
    this.fetchOrders();
  }

  onTabChange(tabValue: string | number) {
    this.activeTab = tabValue.toString();
    if (this.activeTab === '2') {
      setTimeout(() => {
        if (!this.map) this.initMap();
        this.map?.invalidateSize();
        this.updateMapPositions();
      }, 100);
    }
  }

  onOrderSelectionChange(newOrderId: string) {
    this.selectedOrderId.set(newOrderId);
    if (this.activeTab === '2') {
      this.updateMapPositions();
    }
  }

  private initMap(): void {
    const mapEl = document.getElementById('delivery-map');
    if (!mapEl) return;
    this.map = L.map('delivery-map', { zoomControl: false }).setView([40.4168, -3.7038], 6);
    L.control.zoom({ position: 'topright' }).addTo(this.map);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(this.map);
  }

  private updateMapPositions() {
    if (!this.map) return;
    this.markers.forEach(m => m.remove());
    this.markers = [];
    const bounds = L.latLngBounds([]);

    const addCustomMarker = (lat: number, lng: number, iconUrl: string, popupText: string) => {
      const customIcon = L.icon({
        iconUrl: iconUrl,
        iconSize: [32, 32],
        iconAnchor: [16, 32],
        popupAnchor: [0, -32]
      });

      const marker = L.marker([lat, lng], { icon: customIcon }).addTo(this.map!).bindPopup(`<strong>${popupText}</strong>`);
      this.markers.push(marker);
      bounds.extend([lat, lng]);
    };

    const shop = this.myShop();
    if (shop?.address?.latitude != null && shop?.address?.longitude != null) {
      addCustomMarker(shop.address.latitude, shop.address.longitude, '/shopIcon.png', 'Tienda Base');
    }

    const truck = this.myTruck();
    if (truck?.address?.latitude != null && truck?.address?.longitude != null) {
      addCustomMarker(truck.address.latitude, truck.address.longitude, '/truckIcon.png', 'Mi Camión (Pos. Actual)');
    }

    const order = this.selectedOrder();
    if (order?.sendingAddress?.latitude != null && order?.sendingAddress?.longitude != null) {
      addCustomMarker(order.sendingAddress.latitude, order.sendingAddress.longitude, '/location-pointer.png', 'Destino del Pedido');
    }

    if (this.markers.length > 0) {
      this.map.fitBounds(bounds, { padding: [50, 50], maxZoom: 14 });
    }
  }

  getCurrentStatus(order: Order | null | undefined): string {
    if (!order || !order.history || order.history.length === 0) return 'Desconocido';
    return order.history[order.history.length - 1].status;
  }

  get isCollectDisabled(): boolean {
    return this.getCurrentStatus(this.selectedOrder()) === 'Pedido Realizado';
  }

  // --- ACCIONES CON MODALES (RECOGER / CANCELAR) ---

  openCollectDialog() {
    this.collectComment = 'Pedido recogido para su entrega.';
    this.displayCollectDialog = true;
  }

  collectOrder() {
    const order = this.selectedOrder();
    if (!order) return;

    const finalComment = this.collectComment.trim() || 'Pedido recogido para su entrega.';

    this.orderService.commentAndOrUpdateOrderStatus(order.id, 'En Reparto', finalComment).subscribe({
      next: () => {
        this.displayCollectDialog = false;
        this.messageService.add({ severity: 'success', summary: 'Recogido', detail: 'Pedido en fase de reparto.' });
        this.fetchOrders();
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Fallo al actualizar.' })
    });
  }

  openCancelDialog() {
    this.cancelComment = '';
    this.displayCancelDialog = true;
  }

  cancelOrder() {
    const order = this.selectedOrder();
    if (!order) return;

    this.orderService.commentAndOrUpdateOrderStatus(order.id, 'Cancelado', this.cancelComment.trim()).subscribe({
      next: () => {
        this.displayCancelDialog = false;
        this.messageService.add({ severity: 'success', summary: 'Cancelado', detail: 'El pedido ha sido cancelado exitosamente.' });
        this.fetchOrders();
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Fallo al cancelar el pedido.' })
    });
  }

  sendComment() {
    const order = this.selectedOrder();
    if (!order || !this.newComment.trim()) return;
    const currentStatus = this.getCurrentStatus(order);

    this.orderService.commentAndOrUpdateOrderStatus(order.id, currentStatus, this.newComment.trim()).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Comentario enviado', detail: 'Observación registrada.' });
        this.newComment = '';
        this.fetchOrders();
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Fallo al enviar comentario.' })
    });
  }

  // --- NUEVA ACCIÓN: DESASIGNAR PEDIDO ---
  unassignOrder() {
    const order = this.selectedOrder();
    if (!order) return;

    this.orderService.unassignAsFinished(order.id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Desasignado', detail: 'El pedido ha sido liberado de tu camión.' });
        this.fetchOrders(); // Recargamos para que desaparezca de la lista
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Fallo al desasignar el pedido.' })
    });
  }

  // --- MÉTODOS DEL ESCÁNER QR ---

  openScanner() {
    this.displayQrScanner = true;
    this.scanning = true;

    // setTimeout para dar tiempo a PrimeNG de renderizar el div del dialog
    setTimeout(() => {
      this.html5QrcodeScanner = new Html5QrcodeScanner(
        "qr-reader",
        { fps: 10, qrbox: { width: 250, height: 250 } },
        false
      );

      this.html5QrcodeScanner.render(
        this.onScanSuccess.bind(this),
        this.onScanFailure.bind(this)
      );
    }, 100);
  }

  onScanSuccess(decodedText: string) {
    if (this.html5QrcodeScanner) {
      this.html5QrcodeScanner.clear().catch(e => console.error(e));
      this.html5QrcodeScanner = null;
    }
    this.onQrSuccess(decodedText);
  }

  onScanFailure(error: any) {
    // Ignoramos el error, ya que se dispara por cada frame que no reconoce un QR
  }

  onQrSuccess(result: string) {
    this.scanning = false;
    const order = this.selectedOrder();
    if (!order) return;

    this.orderService.checkOrderQrTokenById(order.id, result).subscribe({
      next: (isValid: boolean) => {
        if (isValid) {
          this.displayQrScanner = false;
          this.messageService.add({ severity: 'success', summary: 'Entregado', detail: 'Entrega confirmada con éxito.' });
          this.fetchOrders();
        } else {
          this.messageService.add({ severity: 'error', summary: 'Error QR', detail: 'Código inválido para este pedido.' });
        }
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Fallo al verificar el código QR con el servidor.' })
    });
  }

  closeScanner() {
    this.displayQrScanner = false;
    this.scanning = false;
    if (this.html5QrcodeScanner) {
      this.html5QrcodeScanner.clear().catch(e => console.error(e));
      this.html5QrcodeScanner = null;
    }
  }

  getStatusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    const s = status?.toLowerCase() || '';
    if (s === 'completado') return 'success';
    if (s === 'en reparto') return 'info';
    if (s === 'enviado' || s === 'pedido realizado') return 'warn';
    if (s === 'cancelado') return 'danger';
    return 'secondary';
  }

  protected readonly formatAddress = formatAddress;
}
