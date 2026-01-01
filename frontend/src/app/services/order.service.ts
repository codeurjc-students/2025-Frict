import {Injectable, signal} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable, tap} from 'rxjs';
import {OrderItemsPage} from '../models/orderItemsPage.model';
import {OrderItem} from '../models/orderItem.model';
import {CartSummary} from '../models/cartSummary.model';
import {Order} from '../models/order.model';
import {OrdersPage} from '../models/ordersPage.model';

@Injectable({
  providedIn: 'root'
})
export class OrderService {

  constructor(private http: HttpClient) {}

  private apiUrl = '/api/v1/orders';

// Tu signal actual
  itemsCount = signal<number>(0);

  public syncItemsCount() {
    this.http.get<CartSummary>(this.apiUrl + `/cart/summary`, { withCredentials: true })
      .pipe(
        tap(summary => this.itemsCount.set(summary.totalItems))
      ).subscribe();
  }

  incrementItemsCount(n: number) {
    this.itemsCount.update(current => current + n);
  }

  public setItemsCount(n: number) {
    this.itemsCount.set(n);
  }

  public getOrderById(id: string): Observable<Order> {
    return this.http.get<Order>(this.apiUrl + `/${id}`);
  }

  public getLoggedUserOrders(page: number, size: number): Observable<OrdersPage>{
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<OrdersPage>(this.apiUrl, { params });
  }

  public createOrder(addressId: string, cardId: string): Observable<Order> {
    let params = new HttpParams();
    params = params.append('addressId', addressId);
    params = params.append('cardId', cardId);
    return this.http.post<Order>(this.apiUrl, null, { params });
  }

  public cancelOrder(id: string): Observable<Order> {
    return this.http.delete<Order>(this.apiUrl + `/${id}`);
  }

  public getUserCartItemsPage(page: number, size: number): Observable<OrderItemsPage> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<OrderItemsPage>(this.apiUrl + `/cart`, { params });
  }

  public getUserCartSummary(): Observable<CartSummary> {
    return this.http.get<CartSummary>(this.apiUrl + `/cart/summary`);
  }

  public clearUserCartItems(): Observable<CartSummary> {
    return this.http.delete<CartSummary>(this.apiUrl + `/cart`);
  }

  public addItemToCart(productId: string, units: number): Observable<OrderItem> {
    let params = new HttpParams();
    params = params.append('quantity', units);
    return this.http.post<OrderItem>(this.apiUrl + `/cart/${productId}`, null, { params }); //Null body, required query params
  }

  public updateItemQuantity(productId: string, units: number): Observable<CartSummary> {
    let params = new HttpParams();
    params = params.append('quantity', units);
    return this.http.put<CartSummary>(this.apiUrl + `/cart/${productId}`, null, { params }); //Null body, required query params
  }

  public deleteItem(id: string): Observable<CartSummary> {
    return this.http.delete<CartSummary>(this.apiUrl + `/cart/${id}`)
  }

}
