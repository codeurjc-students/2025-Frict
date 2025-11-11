import {Product} from '../models/product.model';

export interface ProductsPage {
  products: Product[];
  totalProducts: number;
  currentPage: number;
  lastPage: number;
  pageSize: number;
}
