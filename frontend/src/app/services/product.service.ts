import {HttpClient, HttpParams} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {catchError, Observable, switchMap, throwError} from 'rxjs';
import {CategoryService} from './category.service';
import {ProductsPage} from '../models/productsPage.model';
import {Product} from '../models/product.model';
import {ShopStockList} from '../models/shopStockList.model';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  constructor(private http: HttpClient,
              private categoryService: CategoryService) {}

  private apiUrl = '/api/v1/products';

  public getAllProducts(page: number, size: number): Observable<ProductsPage> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<ProductsPage>(this.apiUrl + `/`, { params });
  }

  public getUserFavouriteProductsPage(page: number, size: number): Observable<ProductsPage> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<ProductsPage>(this.apiUrl + `/favourites`, { params });
  }

  public getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(this.apiUrl + `/${id}`);
  }

  public getStockByProductId(id: string): Observable<ShopStockList> {
    return this.http.get<ShopStockList>(this.apiUrl + `/stock/${id}`)
  }

  public getFilteredProducts(
    page: number,
    size: number,
    searchTerm: string,
    categoryIds: number[],
    sort: string
  ): Observable<ProductsPage> {

    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());

    if (searchTerm) {
      params = params.set('query', searchTerm);
    } else {
      params = params.set('query', '');
    }

    if (sort) {
      params = params.append('sort', sort);
    }

    categoryIds.forEach(id => {
      params = params.append('categoryId', id.toString());
    });

    return this.http.get<ProductsPage>(this.apiUrl + `/filter`, { params });
  }

  public getProductsByCategoryId(id: string): Observable<ProductsPage> {
    return this.categoryService.getCategoryById(id).pipe(
      switchMap((category: any) => {
        if (category && category.id) {
          return this.getFilteredProducts(0, 8, '', [category.id], "");
        }
        return throwError(() => new Error(`Category with id ${id} not found.`));
      }),
      catchError((error) => {
        return throwError(() => error);
      })
    );
  }

  public getProductsByCategoryName(name: string): Observable<ProductsPage> { //Gets data for carousels
    return this.categoryService.getCategoryByName(name).pipe(
      switchMap((category: any) => {
        return this.getFilteredProducts(0, 8, '', [category.id], "");
      }),
      catchError((error) => {
        return throwError(() => error);
      })
    );
  }

  public checkInFavourites(id: string): Observable<Product> {
    return this.http.get<Product>(this.apiUrl + `/favourites/${id}`, {});
  }

  public addProductToFavourites(id: string): Observable<Product> {
    return this.http.post<Product>(this.apiUrl + `/favourites/${id}`, {});
  }

  public deleteProductFromFavourites(id: string): Observable<void> {
    return this.http.delete<void>(this.apiUrl + `/favourites/${id}`);
  }
}
