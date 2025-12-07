import {Address} from './address.model';
import {PaymentCard} from './paymentCard.model';

export interface User {
  id: string;
  name: string;
  username: string;
  roles: string[];
  email: string;
  addresses: Address[];
  cards: PaymentCard[];
  imageUrl: string;
  banned: boolean;
}
