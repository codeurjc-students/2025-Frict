import {User} from './user.model';

export interface Truck {
  id: string;
  referenceCode: string;
  shopId: string;
  assignedDriver: User;
}
