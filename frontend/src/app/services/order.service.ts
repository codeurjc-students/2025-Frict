import {Injectable, signal} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {OrderItemsPage} from '../models/orderItemsPage.model';
import {OrderItem} from '../models/orderItem.model';

@Injectable({
  providedIn: 'root'
})
export class OrderService {

  constructor(private http: HttpClient) {}

  private apiUrl = '/api/v1/orders';

  itemsCount = signal<number>(0);

  setItemsCount(n: number) {
    this.itemsCount.set(n);
  }

  incrementItemsCount(n: number) {
    this.itemsCount.update(current => current + n);
  }

  decrementItemsCount(n: number) {
    this.itemsCount.update(current => current - n);
  }

  public getUserCartItemsCount(): Observable<number> {
    return this.http.get<number>(this.apiUrl + `/cart/count`);
  }

  public getUserCartItemsPage(page: number, size: number): Observable<OrderItemsPage> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<OrderItemsPage>(this.apiUrl + `/cart`, { params });
  }

  public clearUserCartItems(): Observable<void> {
    return this.http.delete<void>(this.apiUrl + `/cart`);
  }

  public addItemToCart(productId: string, units: number): Observable<OrderItem> {
    let params = new HttpParams();
    params = params.append('quantity', units);
    return this.http.post<OrderItem>(this.apiUrl + `/cart/${productId}`, null, { params }); //Null body, required query params
  }

  public updateItemQuantity(productId: string, units: number): Observable<OrderItem> {
    let params = new HttpParams();
    params = params.append('quantity', units);
    return this.http.put<OrderItem>(this.apiUrl + `/cart/${productId}`, null, { params }); //Null body, required query params
  }

  public deleteItem(id: string): Observable<void> {
    return this.http.delete<void>(this.apiUrl + `/cart/${id}`)
  }
}
