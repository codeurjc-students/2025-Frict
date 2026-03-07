import {User} from './user.model';
import {Address} from './address.model';

export interface Truck {
  id: string;
  referenceCode: string;
  shopId: string;
  assignedDriver: User;
  address: Address;
  activeOrdersToDeliver: number;
}
