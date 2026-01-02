import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, RouterModule} from '@angular/router';

// PrimeNG Imports
import {ButtonModule} from 'primeng/button';
import {TimelineModule} from 'primeng/timeline';
import {CardModule} from 'primeng/card';
import {TagModule} from 'primeng/tag';
import {DividerModule} from 'primeng/divider';

import {NavbarComponent} from '../navbar/navbar.component';
import {FooterComponent} from '../footer/footer.component';
import {Order} from '../../../models/order.model'; // Asegúrate que tu modelo Order tenga el campo history: StatusLog[]
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

  private readonly stepsDefinitions = [
    { status: 'Pedido realizado', icon: 'pi pi-shopping-cart' },
    { status: 'Enviado', icon: 'pi pi-box' },
    { status: 'En reparto', icon: 'pi pi-truck' },
    { status: 'Completado', icon: 'pi pi-check' },
    { status: 'Cancelado', icon: 'pi pi-ban' }
  ];

  constructor(private orderService: OrderService,
              private route: ActivatedRoute) {}

  ngOnInit() {
    this.orderId = this.route.snapshot.paramMap.get('id');
    this.loadOrder();
  }

  loadOrder() {
    if (this.orderId) {
      this.orderService.getOrderById(this.orderId).subscribe({
        next: (order) => {
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
}
