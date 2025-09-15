import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  constructor(private http: HttpClient) {}

  private apiUrl = 'https://localhost:443/api';

  getAllProducts(): Observable<any> {
    return this.http.get(this.apiUrl + `/products/all`);
  }
}
