import {User} from './user.model';
import {Address} from './address.model';
import {TruckStatusLog} from './truckStatusLog.model';

export interface Truck {
  id: string;
  referenceCode: string;
  plateNumber: string;
  history: TruckStatusLog[];
  shopId: string;
  assignedDriver?: User;
  address: Address;
  activeOrdersToDeliver: number;
  maxOrderCapacity: number;
}
