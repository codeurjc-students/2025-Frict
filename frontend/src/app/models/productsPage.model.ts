import {Product} from './product.model';

export interface ProductsPage {
  products: Product[];
  totalProducts: number;
  currentPage: number;
  lastPage: number;
  pageSize: number;
}
