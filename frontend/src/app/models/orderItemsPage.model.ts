import {OrderItem} from './orderItem.model';

export interface OrderItemsPage {
  orderItems: OrderItem[];
  totalItems: number;
  currentPage: number;
  lastPage: number;
  pageSize: number;
}
