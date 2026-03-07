import {Product} from './product.model';

export interface OrderItem {
  id: string;
  orderId: string;

  product: Product; //Always null when the item is part of an order

  productName: string;
  productImageUrl: string;
  productPrice: number;

  userId: string;
  quantity: number;

  itemsCost: number;
}
