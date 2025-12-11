import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {ActivatedRoute, RouterModule} from '@angular/router';

// PrimeNG Imports
import { ButtonModule } from 'primeng/button';
import { TimelineModule } from 'primeng/timeline';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { DividerModule } from 'primeng/divider';
import {NavbarComponent} from '../navbar/navbar.component';
import {FooterComponent} from '../footer/footer.component';
import {Order} from '../../../models/order.model';
import {OrderService} from '../../../services/order.service';
import {LoadingScreenComponent} from '../loading-screen/loading-screen.component';
import {formatPrice} from '../../../utils/numberFormat.util';

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
    NavbarComponent,
    FooterComponent,
    LoadingScreenComponent
  ],
  templateUrl: './order-details.component.html',
  styles: [`
    /* Personalización para que el timeline se vea bien sobre fondo oscuro */
    :host ::ng-deep .custom-timeline .p-timeline-event-content,
    :host ::ng-deep .custom-timeline .p-timeline-event-opposite {
      line-height: 1;
    }
  `]
})
export class OrderDetailsComponent implements OnInit {

  order!: Order;
  orderId: string | null = null;

  loading: boolean = true;
  error: boolean = false;

  steps = [
    {
      status: 'Pedido realizado',
      icon: 'pi pi-shopping-cart',
      date: '05/12/2025 10:30',
      description: 'Hemos recibido tu pedido correctamente y estamos verificando los detalles del pago.'
    },
    {
      status: 'Enviado',
      icon: 'pi pi-box',
      date: '06/12/2025 14:20',
      description: 'Tu paquete ha salido de nuestro almacén central en Madrid y está en manos del transportista.'
    },
    {
      status: 'En reparto',
      icon: 'pi pi-truck',
      date: '08/12/2025 08:15',
      description: 'El repartidor tiene tu paquete y realizará la entrega a lo largo del día de hoy.'
    },
    {
      status: 'Entregado',
      icon: 'pi pi-check-circle',
      date: undefined,
      description: 'El paquete ha sido entregado en la dirección indicada.'
    }
  ];

  constructor(private orderService: OrderService,
              private route: ActivatedRoute) {}

  ngOnInit() {
    this.orderId = this.route.snapshot.paramMap.get('id');
    this.loadOrder();
  }

  loadOrder(){
    if(this.orderId){
      this.orderService.getOrderById(this.orderId).subscribe({
        next: (order) => {
          this.order = order;
          this.loading = false;
        }
      })
    }
  }

  initTimeline() {
  }

  protected readonly formatPrice = formatPrice;
}
