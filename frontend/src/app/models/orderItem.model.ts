import {Product} from './product.model';

export interface OrderItem {
  id: string;
  orderId: string;

  product: Product;

  productName: string;
  productImageUrl: string;
  productPrice: number;

  userId: string;

  maxQuantity: number;
  quantity: number;

  itemsCost: number;
}
