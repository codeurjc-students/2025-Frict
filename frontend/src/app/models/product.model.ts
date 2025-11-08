export interface Product {
  id: number;
  referenceCode: string;
  name: string;
  imageUrl: string;
  description: string;
  previousPrice: number;
  currentPrice: number;
  discount: string;
  availableUnits: number;
  categoriesId: number[];
}
