import {OrderItem} from './orderItem.model';
import {OrderStatusLog} from './orderStatusLog.model';
import {Address} from './address.model';
import {User} from './user.model';

export interface Order {
  id: string;
  referenceCode: string;
  history: OrderStatusLog[];
  user: User;
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
