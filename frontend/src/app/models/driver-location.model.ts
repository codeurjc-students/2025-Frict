export interface AddressSnapshot {
  street: string;
  number: string;
  postalCode: string;
  city: string;
  country: string;
  latitude: number;
  longitude: number;
}

export interface DriverLocation {
  driverUsername: string;
  driverName: string;
  pingDateTime: string;
  address: AddressSnapshot;
}
