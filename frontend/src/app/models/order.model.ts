import {OrderItem} from './orderItem.model';

export interface Order {
  id: string;
  referenceCode: string;
  status: string;
  userId: string;
  orderItems: OrderItem[];
  assignedTruckId: string;
  estimatedCompletionTime: number;

  totalItems: number;
  subtotalCost: number;
  totalDiscount: number;
  shippingCost: number;
  totalCost: number;

  cardNumberEnding: string;
  fullSendingAddress: string;

  createdAt: string;
}
