import {Category} from './category.model';

export interface Product {
  id: string; //It will be automatically converted to long when reaching the backend
  referenceCode: string;
  name: string;
  imageUrls: string[];
  description: string;
  previousPrice: number;
  currentPrice: number;
  active: boolean;
  discount: string;
  categories: Category[];
  totalUnits: number;
  availableUnits: number;
  shopsWithStock: number;
  averageRating: number;
  totalReviews: number;
}
