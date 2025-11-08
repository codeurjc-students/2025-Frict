import {HttpClient, HttpParams} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {catchError, Observable, of, switchMap, throwError} from 'rxjs';
import {ProductsPage} from './productsPage.model';
import {CategoryService} from './category.service';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  constructor(private http: HttpClient,
              private categoryService: CategoryService) {}

  private apiUrl = '/api/v1/products';

  private defaultProductPage = {products: [], totalProducts: 0, currentPage: 0, lastPage: -1, pageSize: 0};

  public getAllProducts(page: number): Observable<ProductsPage> {
    let params = new HttpParams().set('page', page.toString());
    return this.http.get<ProductsPage>(this.apiUrl + `/`, { params });
  }

  public getFilteredProducts(
    page: number,
    searchTerm: string,
    categoryIds: number[]
  ): Observable<ProductsPage> {

    let params = new HttpParams().set('page', page.toString());

    if (searchTerm) {
      params = params.set('query', searchTerm);
    } else {
      params = params.set('query', '');
    }

    categoryIds.forEach(id => {
      params = params.append('categoryId', id.toString());
    });

    return this.http.get<ProductsPage>(this.apiUrl + `/filter`, { params });
  }

  public getProductsByCategoryName(name: string): Observable<ProductsPage> {
    return this.categoryService.getCategoryByName(name).pipe(
      switchMap((category: any) => {
        if (category && category.id) {
          return this.getFilteredProducts(0,'',[category.id])
        }
        return throwError(() => new Error(name + 'category not found.'));
      }),

      catchError((error) => {
        console.error('Error retrieving products with category "' + name + '"', error);
        return of(this.defaultProductPage);
      })
    );
  }
}
