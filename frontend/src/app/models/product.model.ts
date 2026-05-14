import {Category} from './category.model';
import {ImageInfo} from './imageInfo.model';
import {ProductSpec} from './product-spec.model';

export interface Product {
  id: string; //It will be automatically converted to long when reaching the backend
  referenceCode: string;
  name: string;
  imagesInfo: ImageInfo[];
  description: string;
  supplyPrice: number;
  previousPrice: number;
  currentPrice: number;
  active: boolean;
  discount: string;
  categories: Category[];
  specifications: ProductSpec[];
  totalUnits: number;
  availableUnits: number;
  shopsWithStock: number;
  averageRating: number;
  totalReviews: number;

  createdAt: string;
}
