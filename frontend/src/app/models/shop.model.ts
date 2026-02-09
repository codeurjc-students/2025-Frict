import {Address} from './address.model';
import {ImageInfo} from './imageInfo.model';
import {User} from './user.model';

export interface Shop {
  id: string;
  referenceCode: string;
  name: string;
  address: Address;
  imageInfo: ImageInfo;
  totalAvailableProducts: number;
  totalAssignedTrucks: number;
  longitude: number;
  latitude: number;
  assignedManager: User;
}
