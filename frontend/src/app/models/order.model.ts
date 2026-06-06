import {OrderItem} from './orderItem.model';
import {OrderStatusLog} from './orderStatusLog.model';
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
  totalCapacity: number;

  cardNumberEnding: string;
  sendingAddress: string;
  sendingAddressLat?: number;
  sendingAddressLng?: number;

  createdAt: string;
}
