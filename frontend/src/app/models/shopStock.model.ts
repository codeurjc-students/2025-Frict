export interface ShopStock {
  id: string;
  shopId: string;
  shopName: string;
  shopAddress: string;
  shopImageUrl: string;

  productId: string;
  productName: string;
  productReferenceCode: string;
  productSupplyPrice: number;
  productCurrentPrice: number;
  productCapacity: number;

  units: number;
  active: boolean;
}
