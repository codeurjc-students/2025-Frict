import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

// PrimeNG Imports (v18 structure)
import { StepperModule } from 'primeng/stepper';
import { ButtonModule } from 'primeng/button';
import { RadioButtonModule } from 'primeng/radiobutton';
import { TagModule } from 'primeng/tag';
import { DividerModule } from 'primeng/divider';
import { InputTextModule } from 'primeng/inputtext';
import { CheckboxModule } from 'primeng/checkbox';
import {NavbarComponent} from '../../common/navbar/navbar.component';
import {FooterComponent} from '../../common/footer/footer.component';
import {formatPrice} from '../../../utils/numberFormat.util';

// Interfaces
interface Address {
  id: number;
  alias: string;
  name: string;
  street: string;
  city: string;
  zip: string;
  phone: string;
}

interface PaymentMethod {
  id: number;
  type: 'card' | 'paypal';
  alias: string;
  number?: string;
  expiry?: string;
  icon: string;
}

@Component({
  selector: 'app-order-summary',
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    StepperModule, // This imports p-stepper, p-step-list, p-step, p-step-panels, p-step-panel
    ButtonModule,
    RadioButtonModule,
    TagModule,
    DividerModule,
    InputTextModule,
    CheckboxModule,
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './order-summary.component.html',
  standalone: true,
  styleUrl: './order-summary.component.css'
})
export class OrderSummaryComponent implements OnInit {

  // Sample data
  orderItems = [
    { id: 1, name: 'Smartphone Plegable X', thumbnailUrl: 'https://via.placeholder.com/150', price: 750, quantity: 1, variant: 'Negro Espacial' },
    { id: 2, name: 'Funda Protectora', thumbnailUrl: 'https://via.placeholder.com/150', price: 25, quantity: 1, variant: 'Transparente' }
  ];

  addresses: Address[] = [
    { id: 1, alias: 'Casa', name: 'Juan Pérez', street: 'Calle Mayor 123, 4ºA', city: 'Madrid', zip: '28001', phone: '+34 600 000 000' },
    { id: 2, alias: 'Oficina', name: 'Juan Pérez', street: 'Av. de la Innovación 5', city: 'Getafe', zip: '28901', phone: '+34 600 111 222' }
  ];

  paymentMethods: PaymentMethod[] = [
    { id: 1, type: 'card', alias: 'Visa Personal', number: '**** **** **** 4242', expiry: '12/28', icon: 'pi pi-credit-card' },
    { id: 2, type: 'card', alias: 'Mastercard Trabajo', number: '**** **** **** 8888', expiry: '09/26', icon: 'pi pi-credit-card' },
    { id: 3, type: 'paypal', alias: 'PayPal', icon: 'pi pi-paypal' }
  ];

  selectedAddress: Address | null = this.addresses[0];
  selectedPayment: PaymentMethod | null = this.paymentMethods[0];

  showNewAddressForm = false;
  showNewPaymentForm = false;

  constructor() {}

  ngOnInit(): void {}

  // Main functions
  getSubtotal(): number {
    return this.orderItems.reduce((acc, item) => acc + (item.price * item.quantity), 0);
  }

  getShippingCost(): number {
    return this.getSubtotal() >= 50 ? 0 : 5;
  }

  getTotal(): number {
    return this.getSubtotal() + this.getShippingCost();
  }

  confirmOrder() {
    console.log('Pedido confirmado');
  }

  protected readonly formatPrice = formatPrice;
}
