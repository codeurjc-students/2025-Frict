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
import {MessageService} from 'primeng/api';
import {Dialog} from 'primeng/dialog';
import {SelectButton} from 'primeng/selectbutton';

export interface OrderDTO {
  id: number;
  referenceCode: string;
  totalItems: number;
  totalCost: number;
  user: { name: string; email: string; avatarUrl: string };
  assignedTruck: { plate: string; reference: string } | null;
  status: 'ORDER_MADE' | 'SHIPPED' | 'IN_DELIVERY' | 'COMPLETED' | 'CANCELLED';
  address: string;
  date: Date;
  products: { name: string; quantity: number; price: number }[];
  history: { status: string; description: string; date: Date; isDriverComment?: boolean }[];
}

@Component({
  selector: 'app-orders-management',
  standalone: true,
  imports: [
    CommonModule, FormsModule, CdkDropListGroup, CdkDropList, CdkDrag, Avatar, CdkDragPlaceholder, CdkDragPreview, Dialog, SelectButton
  ],
  templateUrl: './orders-management.component.html',
  styleUrl: './orders-management.component.css'
})
export class OrdersManagementComponent implements OnInit {

  displayMode: any[] = [{ label: 'Tablero', value: false }, { label: 'Lista', value: true }];
  listModeSelected: boolean = false;

  ordersMade = signal<OrderDTO[]>([]);
  shippedOrders = signal<OrderDTO[]>([]);
  inDeliveryOrders = signal<OrderDTO[]>([]);
  completedOrders = signal<OrderDTO[]>([]);
  cancelledOrders = signal<OrderDTO[]>([]);

  kanbanColumns = [
    { id: 'ORDER_MADE', title: 'Pedido Realizado', data: this.ordersMade },
    { id: 'SHIPPED', title: 'Enviado', data: this.shippedOrders },
    { id: 'IN_DELIVERY', title: 'En Reparto', data: this.inDeliveryOrders },
    { id: 'COMPLETED', title: 'Completado', data: this.completedOrders }
  ];

  // Estado del Diálogo
  displayOrderDialog = false;
  selectedOrder: OrderDTO | null = null;

  constructor(private messageService: MessageService) {}

  ngOnInit() {
    this.loadMockData();
  }

  openOrderDetails(order: OrderDTO) {
    this.selectedOrder = order;
    this.displayOrderDialog = true;
  }

  drop(event: CdkDragDrop<any[]>, newStatus: string) {
    const draggedOrder = event.previousContainer.data[event.previousIndex];
    this.processOrderDrop(draggedOrder, newStatus);

    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex,
      );

      const movedOrder = event.container.data[event.currentIndex];
      movedOrder.status = newStatus as OrderDTO['status'];

      movedOrder.history.push({
        status: newStatus,
        description: `El estado ha sido actualizado a ${this.getStatusLabel(newStatus)} manualmente desde el panel.`,
        date: new Date()
      });
    }

    this.ordersMade.set([...this.ordersMade()]);
    this.shippedOrders.set([...this.shippedOrders()]);
    this.inDeliveryOrders.set([...this.inDeliveryOrders()]);
    this.completedOrders.set([...this.completedOrders()]);
    this.cancelledOrders.set([...this.cancelledOrders()]);
  }

  processOrderDrop(order: OrderDTO, newStatus: string) {
    if (order.status === newStatus) return;
    this.messageService.add({ severity: 'success', summary: 'Estado actualizado', detail: `Pedido ${order.referenceCode}` });
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'ORDER_MADE': 'Pedido Realizado', 'SHIPPED': 'Enviado',
      'IN_DELIVERY': 'En Reparto', 'COMPLETED': 'Completado', 'CANCELLED': 'Cancelado'
    };
    return labels[status] || status;
  }

  getStatusColor(status: string): string {
    const colors: Record<string, string> = {
      'ORDER_MADE': 'text-blue-500', 'SHIPPED': 'text-purple-500',
      'IN_DELIVERY': 'text-orange-500', 'COMPLETED': 'text-green-500', 'CANCELLED': 'text-red-500'
    };
    return colors[status] || 'text-slate-400';
  }

  getStatusBgColor(status: string): string {
    const colors: Record<string, string> = {
      'ORDER_MADE': 'bg-blue-100 text-blue-700', 'SHIPPED': 'bg-purple-100 text-purple-700',
      'IN_DELIVERY': 'bg-orange-100 text-orange-700', 'COMPLETED': 'bg-green-100 text-green-700', 'CANCELLED': 'bg-red-100 text-red-700'
    };
    return colors[status] || 'bg-slate-100 text-slate-700';
  }

  // --- MOCK DATA ---
  private loadMockData() {
    const defaultHistory = [
      { status: 'ORDER_MADE', description: 'Pedido recibido correctamente. Esperando preparación.', date: new Date(Date.now() - 86400000 * 2) },
      { status: 'SHIPPED', description: 'El paquete ha salido de los almacenes centrales.', date: new Date(Date.now() - 86400000) }
    ];

    const allOrders: OrderDTO[] = [
      {
        id: 1, referenceCode: 'ORD-9921-A', totalItems: 3, totalCost: 145.50, date: new Date(),
        user: { name: 'Ana García', email: 'ana.g@example.com', avatarUrl: '' }, assignedTruck: null, status: 'ORDER_MADE', address: 'Calle Mayor 12, Madrid',
        products: [{ name: 'Ratón Inalámbrico', quantity: 2, price: 25.00 }, { name: 'Teclado Mecánico', quantity: 1, price: 95.50 }],
        history: [{ status: 'ORDER_MADE', description: 'Pedido recibido. Pago confirmado con tarjeta terminada en 4022.', date: new Date() }]
      },
      {
        id: 4, referenceCode: 'ORD-6654-D', totalItems: 2, totalCost: 120.00, date: new Date(Date.now() - 1000000),
        user: { name: 'Jorge Gil', email: 'jorge.g@example.com', avatarUrl: '' }, assignedTruck: { plate: '9988-XYZ', reference: 'TRK-02' }, status: 'IN_DELIVERY', address: 'Plaza Sol 1, Sevilla',
        products: [{ name: 'Monitor 24"', quantity: 1, price: 120.00 }],
        history: [
          ...defaultHistory,
          { status: 'IN_DELIVERY', description: 'Paquete transferido a camión TRK-02.', date: new Date() },
          { status: 'INFO', description: 'El cliente ha llamado pidiendo que se entregue por la tarde si es posible.', date: new Date(), isDriverComment: true }
        ]
      },
      {
        id: 5, referenceCode: 'ORD-5565-E', totalItems: 12, totalCost: 1540.20, date: new Date(Date.now() - 5000000),
        user: { name: 'Empresa S.L.', email: 'admin@empresa.com', avatarUrl: '' }, assignedTruck: { plate: '9988-XYZ', reference: 'TRK-02' }, status: 'COMPLETED', address: 'Polígono Sur, Naves',
        products: [{ name: 'Lote de Sillas Oficina', quantity: 10, price: 100.00 }, { name: 'Mesa Dirección', quantity: 2, price: 270.10 }],
        history: [
          ...defaultHistory,
          { status: 'IN_DELIVERY', description: 'En reparto por zona industrial.', date: new Date() },
          { status: 'COMPLETED', description: 'Entregado a recepcionista (Marta). Firma recogida.', date: new Date(), isDriverComment: true }
        ]
      },
      {
        id: 6, referenceCode: 'ORD-5565-E', totalItems: 12, totalCost: 1540.20, date: new Date(Date.now() - 5000000),
        user: { name: 'Empresa S.L.', email: 'admin@empresa.com', avatarUrl: '' }, assignedTruck: { plate: '9988-XYZ', reference: 'TRK-02' }, status: 'COMPLETED', address: 'Polígono Sur, Naves',
        products: [{ name: 'Lote de Sillas Oficina', quantity: 10, price: 100.00 }, { name: 'Mesa Dirección', quantity: 2, price: 270.10 }],
        history: [
          ...defaultHistory,
          { status: 'IN_DELIVERY', description: 'En reparto por zona industrial.', date: new Date() },
          { status: 'COMPLETED', description: 'Entregado a recepcionista (Marta). Firma recogida.', date: new Date(), isDriverComment: true }
        ]
      }
    ];

    this.ordersMade.set(allOrders.filter(o => o.status === 'ORDER_MADE'));
    this.shippedOrders.set(allOrders.filter(o => o.status === 'SHIPPED'));
    this.inDeliveryOrders.set(allOrders.filter(o => o.status === 'IN_DELIVERY'));
    this.completedOrders.set(allOrders.filter(o => o.status === 'COMPLETED'));
    this.cancelledOrders.set(allOrders.filter(o => o.status === 'CANCELLED'));
  }
}
