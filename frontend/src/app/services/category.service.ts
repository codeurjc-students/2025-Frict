import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {map, Observable} from 'rxjs';
import {Category} from '../models/category.model';
import {ListResponse} from '../models/listResponse.model';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {

  constructor(private http: HttpClient) {
  }

  private apiUrl = '/api/v1/categories';

  public getAllCategories(): Observable<Category[]> {
    return this.http.get<ListResponse<Category>>(this.apiUrl + `/`).pipe(map(response => response.items));
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
