export interface LoginInfo {
  isLogged: boolean;
  imageUrl: string;
  id: string;
  name: string;
  username: string;
  roles: string[];
  selectedShopId: string | null;
}
