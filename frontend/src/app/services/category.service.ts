import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {map, Observable, tap} from 'rxjs';
import {Category} from '../models/category.model';
import {CategoryList} from '../models/categoryList.model';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {

  constructor(private http: HttpClient) {
  }

  private apiUrl = '/api/v1/categories';

  public getAllCategories(): Observable<CategoryList> {
    return this.http.get<CategoryList>(this.apiUrl + `/`);
  }

  public getCategoryById(id: string): Observable<Category> {
    return this.http.get<Category>(this.apiUrl + `/${id}`);
  }

  public getCategoryByName(name: string): Observable<Category> {
    return this.http.get<{ categories: Category[] }>(this.apiUrl + `/`).pipe(
      map((resp) => {
        const category = resp.categories.find(c => c.name === name);

        if (!category) {
          throw new Error(`Category '${name}' not found`);
        }

        return category;
      })
    );
  }
}
