import {Address} from './address.model';
import {ImageInfo} from './imageInfo.model';
import {User} from './user.model';

export interface Shop {
  id: string;
  referenceCode: string;
  name: string;
  address: Address;
  assignedBudget: number;
  maxCapacity: number;
  occupiedCapacity: number;
  imageInfo: ImageInfo;
  totalAvailableProducts: number;
  totalAssignedTrucks: number;
  assignedManager?: User;
}
