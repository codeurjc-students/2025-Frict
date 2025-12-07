import {Product} from './product.model';

export interface OrderItem {
  id: string;
  orderId: string;

  product: Product;

  userId: string;

  maxQuantity: number;
  quantity: number;
}
