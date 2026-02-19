import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {map, Observable, tap} from 'rxjs';
import {Category} from '../models/category.model';
import {ListResponse} from '../models/listResponse.model';
import {PageResponse} from '../models/pageResponse.model';
import {Product} from '../models/product.model';
import {Shop} from '../models/shop.model';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {

  constructor(private http: HttpClient) {
  }

  private apiUrl = '/api/v1/categories';

  public getAllCategoriesPage(page: number, size: number): Observable<PageResponse<Category>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<Category>>(this.apiUrl + `/`, { params });
  }

  public getAllCategories(): Observable<Category[]> {
    return this.http.get<ListResponse<Category>>(this.apiUrl + `/list`).pipe(map(response => response.items));
  }

  public getCategoryById(id: string): Observable<Category> {
    return this.http.get<Category>(this.apiUrl + `/${id}`);
  }

  public getCategoryByName(name: string): Observable<Category> {
    return this.http.get<ListResponse<Category>>(this.apiUrl + `/`).pipe(
      map((resp) => {
        const category = resp.items.find(c => c.name === name);
        if (!category) {
          throw new Error(`Category '${name}' not found`);
        }

        return category;
      })
    );
  }

  public createCategory(categoryData: Category): Observable<Category> {
    return this.http.post<Category>(this.apiUrl, categoryData);
  }

  public updateCategory(id: string, categoryData: Category): Observable<Category> {
    return this.http.put<Category>(this.apiUrl + `/${id}`, categoryData);
  }

  public deleteCategory(id: string): Observable<Category> {
    return this.http.delete<Category>(this.apiUrl + `/${id}`);
  }

  public updateCategoryImage(categoryId: string, selectedImage: File): Observable<Category> {
    const formData = new FormData();
    formData.append('image', selectedImage);
    return this.http.post<Category>(this.apiUrl + `/${categoryId}/image`, formData);
  }
}
