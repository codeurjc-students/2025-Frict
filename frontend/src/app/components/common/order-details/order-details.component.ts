import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import {forkJoin} from 'rxjs';

import * as L from 'leaflet';

// PrimeNG Imports
import {ButtonModule} from 'primeng/button';
import {TimelineModule} from 'primeng/timeline';
import {CardModule} from 'primeng/card';
import {TagModule} from 'primeng/tag';
import {DividerModule} from 'primeng/divider';

import {Order} from '../../../models/order.model';
import {Shop} from '../../../models/shop.model';
import {Truck} from '../../../models/truck.model';
import {OrderService} from '../../../services/order.service';
import {ShopService} from '../../../services/shop.service';
import {TruckService} from '../../../services/truck.service';
import {LocationService} from '../../../services/location.service';
import {LoadingScreenComponent} from '../loading-screen/loading-screen.component';
import {formatAddress, formatDuration, formatPrice} from '../../../utils/textFormat.util';
import {BreadcrumbReloadComponent} from '../breadcrumb-reload/breadcrumb-reload.component';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {QRCodeComponent} from 'angularx-qrcode';
import {Dialog} from 'primeng/dialog';
import {getOrderStatusTagInfo} from '../../../utils/tagManager.util';

@Component({
  selector: 'app-order-details',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule,
    TimelineModule,
    CardModule,
    TagModule,
    DividerModule,
    LoadingScreenComponent,
    BreadcrumbReloadComponent,
    QRCodeComponent,
    Dialog
  ],
  templateUrl: './order-details.component.html'
})
export class OrderDetailsComponent implements OnInit, OnDestroy {

  private orderService = inject(OrderService);
  private shopService = inject(ShopService);
  private truckService = inject(TruckService);
  private locationService = inject(LocationService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private breadcrumbService = inject(BreadcrumbService);

  order!: Order;
  orderId: string | null = null;

  loading: boolean = true;
  error: boolean = false;

  isDownloadingInvoice: boolean = false;
  displayQrDialog: boolean = false;
  qrToken: string = '';
  qrLoading: boolean = false;
  qrError: boolean = false;

  private orderMap: L.Map | undefined;
  shop: Shop | null = null;
  truck: Truck | null = null;
  routeEta: string | null = null;

  private readonly stepsDefinitions = [
    { status: 'Pedido Realizado', icon: 'pi pi-shopping-cart' },
    { status: 'Enviado', icon: 'pi pi-box' },
    { status: 'En Reparto', icon: 'pi pi-truck' },
    { status: 'Completado', icon: 'pi pi-check' },
    { status: 'Cancelado', icon: 'pi pi-ban' }
  ];

  ngOnInit() {
    this.orderId = this.route.snapshot.paramMap.get('id');
    this.loadOrder();
  }

  ngOnDestroy(): void {
    if (this.orderMap) {
      this.orderMap.remove();
      this.orderMap = undefined;
    }
  }

  isInDelivery(): boolean {
    if (!this.order?.history?.length) return false;
    return this.order.history[this.order.history.length - 1].status === 'En Reparto';
  }

  loadOrder() {
    this.loading = true;
    this.error = false;

    if (this.orderId) {
      this.orderService.getOrderById(this.orderId).subscribe({
        next: (order) => {
          const currentUrl = this.router.url;

          this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, [
            { label: 'Perfil', routerLink: '/profile' },
            { label: 'Pedido ' + order.referenceCode }
          ]);

          this.order = order;
          this.loadIcons();
          this.loading = false;

          if (this.isInDelivery() && order.assignedShopId && order.assignedTruckId) {
            forkJoin({
              shop: this.shopService.getShopById(order.assignedShopId),
              truck: this.truckService.getTruckById(order.assignedTruckId)
            }).subscribe({
              next: ({ shop, truck }) => {
                this.shop = shop;
                this.truck = truck;
                setTimeout(() => this.initOrderMap(), 100);
              }
            });
          }
        },
        error: (err) => {
          console.error(err);
          this.error = true;
          this.loading = false;
        }
      })
    }
  }

  private initOrderMap(): void {
    const container = document.getElementById('order-tracking-map');
    if (!container || !this.order) return;

    if (this.orderMap) {
      this.orderMap.remove();
    }
    this.orderMap = L.map('order-tracking-map').setView([40.4168, -3.7038], 6);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(this.orderMap);
    this.orderMap.attributionControl.setPrefix('Leaflet');

    const markersGroup = L.featureGroup().addTo(this.orderMap);

    const destIcon = L.icon({ iconUrl: './location-pointer.png', iconSize: [32, 32], iconAnchor: [16, 32], popupAnchor: [0, -32] });
    const shopIcon = L.icon({ iconUrl: './shopIcon.png', iconSize: [35, 35], iconAnchor: [17, 35], popupAnchor: [0, -35] });
    const truckIcon = L.icon({ iconUrl: './truckIcon.png', iconSize: [40, 40], iconAnchor: [20, 20], popupAnchor: [0, -20] });

    if (this.order.sendingAddress?.latitude && this.order.sendingAddress?.longitude) {
      L.marker([this.order.sendingAddress.latitude, this.order.sendingAddress.longitude], { icon: destIcon })
        .bindPopup('<b>Tu dirección de entrega</b>')
        .addTo(markersGroup);
    }

    if (this.shop?.address?.latitude && this.shop?.address?.longitude) {
      L.marker([this.shop.address.latitude, this.shop.address.longitude], { icon: shopIcon })
        .bindPopup('<b>Tienda de origen</b><br>' + this.shop.name)
        .addTo(markersGroup);
    }

    let truckLat: number | undefined;
    let truckLng: number | undefined;
    if (this.truck) {
      if (this.truck.assignedDriver && this.truck.driverLocation?.address?.latitude && this.truck.driverLocation?.address?.longitude) {
        truckLat = this.truck.driverLocation.address.latitude;
        truckLng = this.truck.driverLocation.address.longitude;
      } else if (this.truck.address?.latitude && this.truck.address?.longitude) {
        truckLat = this.truck.address.latitude;
        truckLng = this.truck.address.longitude;
      }
    }

    if (truckLat !== undefined && truckLng !== undefined) {
      L.marker([truckLat, truckLng], { icon: truckIcon })
        .bindPopup('<b>Tu camión de reparto</b><br>' + this.truck?.plateNumber)
        .addTo(markersGroup);
    }

    if (markersGroup.getLayers().length > 0) {
      this.orderMap.fitBounds(markersGroup.getBounds(), { padding: [40, 40], maxZoom: 15 });
    }

    this.routeEta = null;

    if (truckLat === undefined || truckLng === undefined) return;

    const truckStatus = this.truck?.history?.length
      ? this.truck.history[this.truck.history.length - 1].status
      : '';
    const isActiveOrder = this.truck?.selectedOrderId === this.order?.id;

    if (truckStatus === 'Descanso' || truckStatus === 'Fuera de servicio') return;

    if (truckStatus === 'En ruta a la tienda'
        && this.shop?.address?.latitude && this.shop?.address?.longitude
        && this.order?.sendingAddress?.latitude && this.order?.sendingAddress?.longitude) {

      const shopLat = this.shop.address.latitude;
      const shopLng = this.shop.address.longitude;
      const destLat = this.order.sendingAddress.latitude;
      const destLng = this.order.sendingAddress.longitude;

      forkJoin({
        leg1: this.locationService.getRoute(truckLat, truckLng, shopLat, shopLng),
        leg2: this.locationService.getRoute(shopLat, shopLng, destLat, destLng)
      }).subscribe(({ leg1, leg2 }) => {
        if (!this.orderMap) return;
        if (leg1) {
          const ll1: L.LatLngTuple[] = leg1.coordinates.map(([lng, lat]) => [lat, lng]);
          L.polyline(ll1, { color: '#3b82f6', weight: 5, opacity: 0.75 }).addTo(this.orderMap!);
        }
        if (leg2) {
          const ll2: L.LatLngTuple[] = leg2.coordinates.map(([lng, lat]) => [lat, lng]);
          const dashArray = isActiveOrder ? undefined : '8, 8';
          L.polyline(ll2, { color: '#8b5cf6', weight: 5, opacity: 0.75, dashArray }).addTo(this.orderMap!);
        }
        if (leg1 && leg2) {
          const eta = formatDuration(leg1.durationSeconds + leg2.durationSeconds);
          this.routeEta = isActiveOrder ? eta : '>= ' + eta;
        }
      });

    } else if (truckStatus === 'En Reparto'
        && this.order?.sendingAddress?.latitude && this.order?.sendingAddress?.longitude) {

      this.locationService.getRoute(truckLat, truckLng, this.order.sendingAddress.latitude, this.order.sendingAddress.longitude)
        .subscribe(route => {
          if (!route || !this.orderMap) return;
          const latlngs: L.LatLngTuple[] = route.coordinates.map(([lng, lat]) => [lat, lng]);
          const dashArray = isActiveOrder ? undefined : '8, 8';
          L.polyline(latlngs, { color: '#8b5cf6', weight: 5, opacity: 0.8, dashArray }).addTo(this.orderMap!);
          const eta = formatDuration(route.durationSeconds);
          this.routeEta = isActiveOrder ? eta : '>= ' + eta;
        });
    }
  }

  loadIcons() {
    if (!this.order || !this.order.history) return;

    this.order.history.forEach(log => {
      const matchingStep = this.stepsDefinitions.find(step => step.status === log.status);

      if (matchingStep) {
        log.icon = matchingStep.icon;
      }
    });
  }

  protected cancelOrder() {
    this.orderService.cancelOrder(this.order.id).subscribe({
      next: (order) =>  {
        this.order = order;
        this.loadIcons();
        this.loading = false;
      }
    })
  }

  openQrDialog() {
    this.displayQrDialog = true;

    // Si ya lo hemos cargado antes, no repetimos la llamada
    if (!this.qrToken && this.orderId) {
      this.qrLoading = true;
      this.qrError = false;

      this.orderService.getOrderQrTokenById(this.orderId).subscribe({
        next: (token) => {
          this.qrToken = token;
          this.qrLoading = false;
        },
        error: (err) => {
          console.error("Error obteniendo el token QR:", err);
          this.qrError = true;
          this.qrLoading = false;
        }
      });
    }
  }

  downloadInvoice() {
    if (!this.orderId || !this.order) return;

    this.isDownloadingInvoice = true;

    this.orderService.downloadOrderInvoice(this.orderId).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;

        anchor.download = `Factura_${this.order.referenceCode}.pdf`;
        document.body.appendChild(anchor);
        anchor.click();

        document.body.removeChild(anchor);
        window.URL.revokeObjectURL(url);

        this.isDownloadingInvoice = false;
      },
      error: (err) => {
        console.error('Error al descargar la factura:', err);
        this.isDownloadingInvoice = false;
      }
    });
  }

  protected readonly formatAddress = formatAddress;
  protected readonly formatPrice = formatPrice;
  protected readonly formatDuration = formatDuration;
  protected readonly getOrderStatusTagInfo = getOrderStatusTagInfo;
}
