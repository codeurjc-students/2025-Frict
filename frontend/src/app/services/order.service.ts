import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {OrderItemsPage} from '../models/orderItemsPage.model';

@Injectable({
  providedIn: 'root'
})
export class OrderService {

  constructor(private http: HttpClient) {}

  private apiUrl = '/api/v1/orders';

  public getUserCartItemsPage(page: number, size: number): Observable<OrderItemsPage> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<OrderItemsPage>(this.apiUrl + `/cart`, { params });
  }

  public clearUserCartItems(): Observable<void> {
    return this.http.delete<void>(this.apiUrl + `/cart`);
  }

  public deleteItem(id: string): Observable<void> {
    return this.http.delete<void>(this.apiUrl + `/cart/${id}`)
  }
}
