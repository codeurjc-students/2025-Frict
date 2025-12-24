import {Order} from './order.model';

export interface OrdersPage {
  orders: Order[];
  totalOrders: number;
  currentPage: number;
  lastPage: number;
  pageSize: number;
}
