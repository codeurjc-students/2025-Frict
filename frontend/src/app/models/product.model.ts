export interface Product {
  id: string; //It will be automatically converted to long when reaching the backend
  referenceCode: string;
  name: string;
  imageUrl: string;
  thumbnailUrl: string;
  description: string;
  previousPrice: number;
  currentPrice: number;
  discount: string;
  categoriesId: number[];
  availableUnits: number;
  averageRating: number;
  totalReviews: number;
}
