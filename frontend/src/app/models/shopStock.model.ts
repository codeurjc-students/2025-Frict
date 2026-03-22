export interface ShopStock {
  id: string;
  shopId: string;
  shopName: string;
  shopAddress: string;

  productId: string;
  productName: string;
  productReferenceCode: string;
  productSupplyPrice: number;
  productCurrentPrice: number;

  units: number;
  active: boolean;
}
