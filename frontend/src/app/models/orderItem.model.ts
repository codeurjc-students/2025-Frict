import {Product} from './product.model';

export interface OrderItem {
  id: string;
  orderId: string;

  product: Product;

  productName: string;
  productImageUrl: string;
  productPrice: number;

  userId: string;
  quantity: number;

  itemsCost: number;
}
