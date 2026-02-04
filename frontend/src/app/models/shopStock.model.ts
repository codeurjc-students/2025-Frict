export interface ShopStock {
  id: string;
  shopId: string;
  shopName: string;
  shopAddress: string;
  productId: string;
  productName: string;
  productReferenceCode: string;
  units: number;
  active: boolean;
}
