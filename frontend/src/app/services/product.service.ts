import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  constructor(private http: HttpClient) {}

  private apiUrl = '/api/v1';

  public getAllProducts(): Observable<any> {
    return this.http.get(this.apiUrl + `/products/`);
  }
}
