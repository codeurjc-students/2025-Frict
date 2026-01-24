import {Category} from './category.model';
import {ImageInfo} from './imageInfo.model';

export interface Product {
  id: string; //It will be automatically converted to long when reaching the backend
  referenceCode: string;
  name: string;
  imagesInfo: ImageInfo[];
  description: string;
  previousPrice: number;
  currentPrice: number;
  active: boolean;
  discount: string;
  categories: Category[];
  totalUnits: number;
  shopsWithStock: number;
  averageRating: number;
  totalReviews: number;
}
