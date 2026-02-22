import {Address} from './address.model';
import {PaymentCard} from './paymentCard.model';
import {ImageInfo} from './imageInfo.model';

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
  logged: boolean;
  lastConnection: string;
  selectedShopId: number | null;

  ordersCount: number;
  favouriteProductsCount: number;
}
