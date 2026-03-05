import {OrderItem} from './orderItem.model';
import {StatusLog} from './statusLog.model';
import {Address} from './address.model';

export interface Order {
  id: string;
  referenceCode: string;
  history: StatusLog[];
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
