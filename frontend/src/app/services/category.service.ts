import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Category} from '../models/category.model';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {

  constructor(private http: HttpClient) {
  }

  private apiUrl = '/api/v1/categories';

  public getCategoryByName(name: string): Observable<Category> {
    let params = new HttpParams().set('name', name);
    return this.http.get<Category>(this.apiUrl + `/`, { params });
  }

}
