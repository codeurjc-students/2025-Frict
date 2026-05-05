import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';

// PrimeNG Imports
import {ButtonModule} from 'primeng/button';
import {TimelineModule} from 'primeng/timeline';
import {CardModule} from 'primeng/card';
import {TagModule} from 'primeng/tag';
import {DividerModule} from 'primeng/divider';

import {Order} from '../../../models/order.model';
import {OrderService} from '../../../services/order.service';
import {LoadingScreenComponent} from '../loading-screen/loading-screen.component';
import {formatAddress, formatPrice} from '../../../utils/textFormat.util';
import {BreadcrumbReloadComponent} from '../breadcrumb-reload/breadcrumb-reload.component';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {QRCodeComponent} from 'angularx-qrcode';
import {Dialog} from 'primeng/dialog';

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
export class OrderDetailsComponent implements OnInit {

  order!: Order;
  orderId: string | null = null;

  loading: boolean = true;
  error: boolean = false;

  isDownloadingInvoice: boolean = false;
  displayQrDialog: boolean = false;
  qrToken: string = '';
  qrLoading: boolean = false;
  qrError: boolean = false;

  private readonly stepsDefinitions = [
    { status: 'Pedido Realizado', icon: 'pi pi-shopping-cart' },
    { status: 'Enviado', icon: 'pi pi-box' },
    { status: 'En Reparto', icon: 'pi pi-truck' },
    { status: 'Completado', icon: 'pi pi-check' },
    { status: 'Cancelado', icon: 'pi pi-ban' }
  ];

  constructor(private orderService: OrderService,
              private route: ActivatedRoute,
              private router: Router,
              private breadcrumbService: BreadcrumbService) {}

  ngOnInit() {
    this.orderId = this.route.snapshot.paramMap.get('id');
    this.loadOrder();
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
        },
        error: (err) => {
          console.error(err);
          this.error = true;
          this.loading = false;
        }
      })
    }
  }

  protected readonly formatPrice = formatPrice;

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
}
