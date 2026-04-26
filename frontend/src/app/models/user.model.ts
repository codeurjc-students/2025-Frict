import {Address} from './address.model';
import {PaymentCard} from './paymentCard.model';
import {ImageInfo} from './imageInfo.model';
import {Connection} from './connection.model';

export interface User {
  id: string;
  name: string;
  username: string;
  roles: string[];
  email: string;
  phone: string;
  addresses: Address[];
  cards: PaymentCard[];
  imageInfo: ImageInfo;
  banned: boolean;
  deleted: boolean;
  selectedShopId: string | null;
  ordersCount: number;
  favouriteProductsCount: number;

  connection: Connection | null;
}
