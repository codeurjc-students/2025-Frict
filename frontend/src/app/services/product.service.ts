import {HttpClient, HttpParams} from '@angular/common/http';
import {Injectable, signal} from '@angular/core';
import {catchError, Observable, switchMap, throwError} from 'rxjs';
import {CategoryService} from './category.service';
import {Product} from '../models/product.model';
import {ProductSpec} from '../models/product-spec.model';
import {PageResponse} from '../models/pageResponse.model';
import {ShopStock} from '../models/shopStock.model';

export type SearchScope = 'GLOBAL' | 'LOCAL';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  constructor(private http: HttpClient,
              private categoryService: CategoryService) {}

  private apiUrl = '/api/v1/products';

  private scopeSignal = signal<SearchScope>('GLOBAL');
  public readonly searchScope = this.scopeSignal.asReadonly();

  public setSearchScope(scope: SearchScope): void {
    this.scopeSignal.set(scope);
    localStorage.setItem('search_scope', scope);
  }

  public resetSearchScope(): void {
    this.scopeSignal.set('GLOBAL');
    localStorage.removeItem('search_scope');
  }

  public getAllProducts(page: number, size: number): Observable<PageResponse<Product>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<Product>>(this.apiUrl + `/`, { params });
  }

  public getUserFavouriteProductsPage(page: number, size: number): Observable<PageResponse<Product>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<Product>>(this.apiUrl + `/favourites`, { params });
  }

  public getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(this.apiUrl + `/${id}`);
  }

  public getStockByProductId(id: string): Observable<ShopStock[]> {
    return this.http.get<ShopStock[]>(this.apiUrl + `/stock/${id}`);
  }

  //Shop details component: Search for the products that do not have a stock assigned in a shop
  public getEligibleProducts(shopId: string): Observable<Product[]> {
    return this.http.get<Product[]>(this.apiUrl + `/available/${shopId}`);
  }

  //Using use-data from MongoDB
  public getRecommendedProducts(size: number = 8): Observable<PageResponse<Product>> {
    return this.http.get<PageResponse<Product>>(`${this.apiUrl}/recommendations?size=${size}`);
  }

  public getFilteredProducts(
    page: number,
    size: number,
    searchTerm: string,
    categoryIds: number[],
    sort: string,
    specFilters: ProductSpec[] = []
  ): Observable<PageResponse<Product>> {

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

    specFilters.forEach(f => {
      params = params.append('specFilter', `${f.name}:${f.values.join(',')}`);
    });

    return this.http.get<PageResponse<Product>>(this.apiUrl + `/filter`, { params });
  }

  public getSpecsCatalog(): Observable<Record<string, string[]>> {
    return this.http.get<Record<string, string[]>>(this.apiUrl + `/specs`);
  }

  public getProductsByCategoryId(id: string): Observable<PageResponse<Product>> {
    return this.categoryService.getCategoryById(id).pipe(
      switchMap((category: any) => {
        if (category && category.id) {
          return this.getFilteredProducts(0, 8, '', [category.id], "name,asc");
        }
        return throwError(() => new Error(`Category with id ${id} not found.`));
      }),
      catchError((error) => {
        return throwError(() => error);
      })
    );
  }

  public getProductsByCategoryName(name: string): Observable<PageResponse<Product>> { //Gets data for carousels
    return this.categoryService.getCategoryByName(name).pipe(
      switchMap((category: any) => {
        return this.getFilteredProducts(0, 8, '', [category.id], "name,asc");
      }),
      catchError((error) => {
        return throwError(() => error);
      })
    );
  }

  public checkInFavourites(id: string): Observable<boolean> {
    return this.http.get<boolean>(this.apiUrl + `/favourites/${id}`, {});
  }

  public addProductToFavourites(id: string): Observable<Product> {
    return this.http.post<Product>(this.apiUrl + `/favourites/${id}`, {});
  }

  public deleteProductFromFavourites(id: string): Observable<void> {
    return this.http.delete<void>(this.apiUrl + `/favourites/${id}`);
  }

  //Product management endpoints
  public toggleGlobalActivation(id: string, state: boolean): Observable<Product> {
    let params = new HttpParams();
    params = params.append('state', state);
    return this.http.put<Product>(this.apiUrl + `/active/${id}`, null, { params });
  }

  public toggleAllGlobalActivations(state: boolean): Observable<Boolean> {
    let params = new HttpParams();
    params = params.append('state', state);
    return this.http.put<Boolean>(this.apiUrl + `/active/`, null, { params });
  }

  public createProduct(productData: Product): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, productData);
  }

  public updateProduct(id: string, productData: Product): Observable<Product> {
    return this.http.put<Product>(this.apiUrl + `/${id}`, productData);
  }

  public deleteProduct(id: string): Observable<Product> {
    return this.http.delete<Product>(this.apiUrl + `/${id}`);
  }

  public updateProductImages(id: string, existingImages: any[], newImages: File[]): Observable<Product> {
    const formData = new FormData();
    const jsonPart = new Blob([JSON.stringify(existingImages)], { type: 'application/json' });
    formData.append('existingImages', jsonPart);

    if (newImages && newImages.length > 0) {
      newImages.forEach(file => {
        formData.append('newImages', file);
      });
    }
    return this.http.put<Product>(this.apiUrl + `/${id}/images`, formData);
  }


  public getCategoryTopSales(categoryId: string, size: number = 10): Observable<PageResponse<Product>> {
    let params = new HttpParams()
      .set('size', size.toString());

    return this.http.get<PageResponse<Product>>(`${this.apiUrl}/category/${categoryId}/top-sales`, { params });
  }


  public getCategoryTimeline(categoryId: string, dataType: string, days: number): Observable<any[]> {
    let params = new HttpParams()
      .set('dataType', dataType)
      .set('days', days.toString());

    return this.http.get<any[]>(`${this.apiUrl}/category/${categoryId}/timeline`, { params });
  }


  public getCategoryMetrics(categoryId: string): Observable<{totalShops: number, totalViews: number, totalSales: number}> {
    return this.http.get<any>(`${this.apiUrl}/category/${categoryId}/metrics`);
  }

}
