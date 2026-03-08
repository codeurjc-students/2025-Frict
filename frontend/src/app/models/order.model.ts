import {OrderItem} from './orderItem.model';
import {OrderStatusLog} from './orderStatusLog.model';
import {Address} from './address.model';

export interface Order {
  id: string;
  referenceCode: string;
  history: OrderStatusLog[];
  userName: string;
  orderItems: OrderItem[];
  assignedShopId?: string | null;
  assignedTruckId?: string | null;
  estimatedCompletionTime: number;

  totalItems: number;
  subtotalCost: number;
  totalDiscount: number;
  shippingCost: number;
  totalCost: number;

  cardNumberEnding: string;
  sendingAddress: Address;

  createdAt: string;
}
